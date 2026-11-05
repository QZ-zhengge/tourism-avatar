"""
语音转文字（ASR）工具模块 —— Vosk 真正流式版

Vosk 特点：
  - 离线运行，无需联网
  - 逐帧解码，真正流式输出（延迟 < 500ms）
  - 支持中文/英文/日语/韩语等多语言
  - 模型文件 40MB（small）~ 1.8GB（large）可选

目录结构（模型需提前下载解压到 models/vosk/ 下）：
  models/vosk/
    vospk-model-small-cn-0.22/    ← 中文轻量模型（推荐）
    vospk-model-cn-0.22/          ← 中文大模型（高精度）
    vospk-model-small-en-us-0.15/ ← 英文轻量模型
"""
import os
import io
import sys
import json
import logging
import threading
import asyncio
import queue
import subprocess
import shutil
import tempfile

import numpy as np

logger = logging.getLogger(__name__)

# ── 模型路径配置 ────────────────────────────────────────────────
# 模型默认放在项目根目录的 models/vosk/ 下
# 可通过环境变量 VOSK_MODEL_DIR 覆盖
_DEFAULT_MODEL_DIR = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "models", "vosk"
)
MODEL_DIR = os.environ.get("VOSK_MODEL_DIR", _DEFAULT_MODEL_DIR)

# 语言 → 模型子目录名映射
LANG_MODEL_MAP = {
    "zh": "vosk-model-small-cn-0.22",
    "en": "vospk-model-small-en-us-0.15",
    "ja": "vospk-model-small-ja-0.22",
    "ko": "vospk-model-small-ko-0.22",
    "auto": "vospk-model-small-cn-0.22",   # auto 默认中文，由调用方多语言重试
}

# ── 全局模型实例缓存（懒加载，线程安全）────────────────────────
_model_cache = {}          # lang -> (Model, KaldiRecognizer)
_model_cache_lock = threading.Lock()


# ──────────────────────────────────────────────────────────────────
# 模型管理
# ──────────────────────────────────────────────────────────────────

def _get_model_path(lang: str) -> str:
    """根据语言返回本地模型目录路径，不存在则抛异常"""
    subdir = LANG_MODEL_MAP.get(lang, LANG_MODEL_MAP["zh"])
    path = os.path.join(MODEL_DIR, subdir)
    if not os.path.isdir(path):
        # 尝试直接把 lang 当目录名
        path2 = os.path.join(MODEL_DIR, lang)
        if os.path.isdir(path2):
            return path2
        raise FileNotFoundError(
            f"Vosk 模型未找到：{path}\n"
            f"请先下载模型并解压到 {MODEL_DIR} 目录。\n"
            f"中文模型下载：https://alphacephei.com/vosk/models/vospk-model-small-cn-0.22.zip"
        )
    return path


def get_vosk_model(lang: str = "zh"):
    """
    懒加载 Vosk Model + KaldiRecognizer（线程安全单例，按语言隔离）
    返回 (Model, KaldiRecognizer)
    """
    global _model_cache
    if lang in _model_cache:
        return _model_cache[lang]

    with _model_cache_lock:
        if lang in _model_cache:
            return _model_cache[lang]

        try:
            import vosk
        except ImportError:
            raise RuntimeError(
                "vosk 未安装，请执行：pip install vosk\n"
                "（注意：包名是 vosk，不是 vosk）"
            )

        model_path = _get_model_path(lang)
        logger.info(f"[ASR] 正在加载 Vosk 模型：{model_path}")

        model = vosk.Model(model_path)
        # 创建识别器，指定采样率 16000
        recognizer = vosk.KaldiRecognizer(model, 16000)
        recognizer.SetWords(False)   # 不需要词级时间戳

        _model_cache[lang] = (model, recognizer)
        logger.info(f"[ASR] Vosk 模型加载完成：{lang}")
        return _model_cache[lang]


def is_model_ready(lang: str = "zh") -> bool:
    return lang in _model_cache


def get_model_info() -> dict:
    loaded = list(_model_cache.keys())
    return {
        "backend": "vosk",
        "model_dir": MODEL_DIR,
        "loaded_langs": loaded,
        "available_langs": list(LANG_MODEL_MAP.keys()),
    }


# ──────────────────────────────────────────────────────────────────
# 音频解码工具（任意格式 → int16 PCM，16kHz 单声道）
# ──────────────────────────────────────────────────────────────────

def _find_ffmpeg() -> str:
    """
    查找 ffmpeg 可执行文件路径。
    优先级：环境变量 FFMPEG_PATH > PATH 搜索 > Windows 常见安装目录兜底。
    """
    # 1. 环境变量指定
    env_path = os.environ.get("FFMPEG_PATH")
    if env_path and os.path.isfile(env_path):
        return env_path

    # 2. PATH 搜索
    found = shutil.which("ffmpeg")
    if found:
        return found

    # 3. Windows 常见安装路径兜底
    candidates = [
        r"C:\ffmpeg\bin\ffmpeg.exe",
        r"C:\Program Files\ffmpeg\bin\ffmpeg.exe",
        r"C:\Program Files (x86)\ffmpeg\bin\ffmpeg.exe",
        os.path.join(os.environ.get("USERPROFILE", ""), r"scoop\shims\ffmpeg.exe"),
        os.path.join(os.environ.get("USERPROFILE", ""), r"scoop\apps\ffmpeg\current\bin\ffmpeg.exe"),
    ]
    for c in candidates:
        if os.path.isfile(c):
            logger.info(f"[ASR] 使用兜底 ffmpeg 路径：{c}")
            return c

    raise RuntimeError(
        "系统未找到 ffmpeg，请先安装 ffmpeg 并加入 PATH，或设置环境变量 FFMPEG_PATH：\n"
        "  Windows:  scoop install ffmpeg  或  choco install ffmpeg\n"
        "  Ubuntu:    sudo apt install ffmpeg"
    )


def _decode_audio_to_pcm(raw_bytes: bytes, file_ext: str = ".wav") -> bytes:
    """
    将任意格式音频字节解码为 int16 PCM 原始字节（16kHz 单声道）。
    使用 ffmpeg（subprocess）完成格式转换。
    返回 bytes（int16 little-endian PCM）。
    """
    ffmpeg_bin = _find_ffmpeg()

    suffix = file_ext if file_ext.startswith(".") else f".{file_ext}"
    with tempfile.NamedTemporaryFile(suffix=suffix, delete=False) as f_in:
        f_in.write(raw_bytes)
        in_path = f_in.name

    out_path = in_path + ".pcm"
    try:
        cmd = [
            ffmpeg_bin, "-y",
            "-i", in_path,
            "-ar", "16000",    # 16kHz
            "-ac", "1",        # 单声道
            "-f", "s16le",     # int16 little-endian PCM 原始流
            "-loglevel", "error",
            out_path
        ]
        subprocess.run(cmd, check=True, timeout=30)

        with open(out_path, "rb") as f:
            pcm_bytes = f.read()
        return pcm_bytes

    finally:
        for p in [in_path, out_path]:
            try:
                os.remove(p)
            except Exception:
                pass


# ──────────────────────────────────────────────────────────────────
# Vosk 流式会话（真正逐帧流式识别）
# ──────────────────────────────────────────────────────────────────

class VoskStreamingSession:
    """
    基于 Vosk 的真正流式 ASR 会话。

    工作原理：
      1. 创建时新建一个 KaldiRecognizer（复用 Model，但 Recognizer 需独立）
      2. 外部每收到一块 int16 PCM 音频，调用 feed_pcm(pcm_bytes)
      3. feed_pcm 内部调用 recognizer.AcceptWaveform(pcm_bytes)
         - 若当前语音段结束，AcceptWaveform 返回 True，
           调用 Result() 获取该段完整文字（final）
         - 若未结束，可随时调用 PartialResult() 获取当前中间结果（partial）
      4. 延迟极低：每收到几十 ms 音频即可解码，无需等整段

    与 Whisper 的本质区别：
      - Whisper 必须喂整段音频才能出结果（伪流式，靠滑动窗口模拟）
      - Vosk 是真正的流式：逐帧解码，每帧（10~20ms 音频）都能产出部分结果
    """

    SAMPLE_RATE = 16000

    def __init__(self, lang: str = "zh"):
        self.lang = lang
        self.model, self._template_rec = get_vosk_model(lang)

        # 每个会话必须创建独立的 Recognizer（KaldiRecognizer 非线程安全）
        import vosk
        self.recognizer = vosk.KaldiRecognizer(self.model, self.SAMPLE_RATE)
        self.recognizer.SetWords(False)

        self._full_text_parts: list[str] = []
        self._lock = threading.Lock()

        logger.debug(f"[ASR] VoskStreamingSession 创建，lang={lang}")

    def feed_pcm(self, pcm_bytes: bytes) -> dict:
        """
        喂入一块 int16 PCM 原始字节（16kHz 单声道）。
        返回：
          {"type": "partial", "text": "..."}  — 中间结果（可调用的非常频繁）
          或
          {"type": "final",   "text": "..."}  — 当前语音段结束，最终文字
          或
          None                                    — 暂无结果
        """
        import vosk

        with self._lock:
            # AcceptWaveform 接受 bytes（int16 PCM）
            if self.recognizer.AcceptWaveform(pcm_bytes):
                # 当前语音段结束，读取最终结果
                raw_result = self.recognizer.Result()
                result = json.loads(raw_result)
                text = (result.get("text") or "").strip()
                if text:
                    self._full_text_parts.append(text)
                    logger.debug(f"[ASR] final: '{text}'")
                    return {"type": "final", "text": text}
            else:
                # 尚未结束，读取中间结果
                raw_partial = self.recognizer.PartialResult()
                result = json.loads(raw_partial)
                text = (result.get("partial") or "").strip()
                if text:
                    logger.debug(f"[ASR] partial: '{text}'")
                    return {"type": "partial", "text": text}
        return None

    def feed_float32(self, pcm_float32: np.ndarray) -> list[dict]:
        """
        喂入 float32 numpy 数组（Whisper 格式兼容）。
        自动转换为 int16 PCM 字节后调用 feed_pcm。
        返回所有产生的结果列表（可能有多个 partial + 一个 final）。
        """
        # float32 [-1,1] → int16
        pcm_int16 = (np.clip(pcm_float32, -1.0, 1.0) * 32767.0).astype(np.int16)
        pcm_bytes = pcm_int16.tobytes()
        # 分块喂入（每次 4096 字节 ≈ 128ms），让 Vosk 有机会输出 partial
        results = []
        chunk_size = 4096
        for i in range(0, len(pcm_bytes), chunk_size):
            chunk = pcm_bytes[i:i + chunk_size]
            r = self.feed_pcm(chunk)
            if r:
                results.append(r)
        return results

    def finalize(self) -> str:
        """
        会话结束：调用 recognizer.FinalResult() 获取最后一段文字，
        并返回所有已识别文字的拼接。
        """
        with self._lock:
            raw = self.recognizer.FinalResult()
            result = json.loads(raw)
            text = (result.get("text") or "").strip()
            if text:
                self._full_text_parts.append(text)
            full = " ".join(self._full_text_parts)
            logger.debug(f"[ASR] finalized, full_text='{full}'")
            return full


# ──────────────────────────────────────────────────────────────────
# 一次性识别（保留，供非流式场景使用）
# ──────────────────────────────────────────────────────────────────

def transcribe_audio(
    audio_bytes: bytes,
    file_ext: str = ".wav",
    lang: str = "zh",
) -> dict:
    """
    对完整音频字节进行一次识别（非流式，内部调用 Vosk 完整处理）。
    """
    model, recognizer = get_vosk_model(lang)

    # 解码为 int16 PCM
    pcm_bytes = _decode_audio_to_pcm(audio_bytes, file_ext=file_ext)

    import vosk
    rec = vosk.KaldiRecognizer(model, 16000)
    rec.SetWords(False)

    # 分块喂入
    offset = 0
    step = 4096
    all_text_parts = []
    while offset < len(pcm_bytes):
        chunk = pcm_bytes[offset:offset + step]
        offset += len(chunk)
        if rec.AcceptWaveform(chunk):
            raw = rec.Result()
            t = json.loads(raw).get("text", "").strip()
            if t:
                all_text_parts.append(t)

    # 最后一段
    raw = rec.FinalResult()
    t = json.loads(raw).get("text", "").strip()
    if t:
        all_text_parts.append(t)

    full_text = " ".join(all_text_parts)
    return {"text": full_text, "language": lang, "segments": []}
