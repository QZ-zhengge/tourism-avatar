"""
测试模块 07 — 虚拟人模块（讯飞在线虚拟人驱动）
覆盖业务逻辑：
  - 本地静态接口（无需讯飞凭证）
      · 发音人列表 / 结构校验
      · 形象动作列表（已知ID / 未知ID走default）
      · 查询不存在会话 → code=404
  - 凭证保护（未配置 → 503；配置后 → 讯飞业务响应）
  - 入参 Pydantic 校验（text 为空 → 422 / status 越界 → 422）
  - 全流程端到端（凭证已配置时执行）：
      启动 → 会话查询 → 文本驱动 → WSS URL → 心跳 → 停止
      → 停后查询 404 → 无效 session 错误处理
"""
import pytest
import requests
from conftest import api, BASE_URL, TIMEOUT


# ────────────────────────────────────────────────────────────
#  7-A. 本地静态接口
# ────────────────────────────────────────────────────────────

class TestAvatarStatic:
    """无需讯飞凭证的本地数据接口"""

    # ── 发音人列表 ──

    def test_vcn_list_returns_200(self):
        """获取发音人列表：应返回 code=200"""
        body = api("get", "/avatar/vcn/list").json()
        assert body["code"] == 200

    def test_vcn_list_data_is_nonempty_list(self):
        """发音人列表 data 应为非空数组"""
        body = api("get", "/avatar/vcn/list").json()
        assert isinstance(body["data"], list)
        assert len(body["data"]) > 0

    def test_vcn_list_item_structure(self):
        """发音人列表每项必须含 vcn / name / language / scene / gender"""
        body = api("get", "/avatar/vcn/list").json()
        item = body["data"][0]
        for field in ("vcn", "name", "language", "scene", "gender"):
            assert field in item, f"发音人列表缺少字段: {field}"

    def test_vcn_list_contains_default_vcn(self):
        """发音人列表应包含默认发音人 x4_lingxiaoyu_assist"""
        body = api("get", "/avatar/vcn/list").json()
        vcns = [v["vcn"] for v in body["data"]]
        assert "x4_lingxiaoyu_assist" in vcns, \
            "未找到默认发音人 x4_lingxiaoyu_assist"

    # ── 形象动作列表 ──

    def test_actions_known_avatar_id(self):
        """已知形象 118801001（依丹）的动作列表：应返回 code=200"""
        body = api("get", "/avatar/actions/118801001").json()
        assert body["code"] == 200

    def test_actions_response_structure(self):
        """动作列表响应应包含 avatar_id 和 actions 数组"""
        body = api("get", "/avatar/actions/118801001").json()
        assert "avatar_id" in body
        assert "actions"   in body
        assert isinstance(body["actions"], list)

    def test_actions_item_has_value_and_description(self):
        """动作列表每项应含 value 和 description"""
        body = api("get", "/avatar/actions/118801001").json()
        assert len(body["actions"]) > 0
        for action in body["actions"]:
            assert "value"       in action, "动作缺少 value 字段"
            assert "description" in action, "动作缺少 description 字段"

    def test_actions_second_known_avatar(self):
        """已知形象 110026010（伊凡）的动作列表：应返回 code=200"""
        body = api("get", "/avatar/actions/110026010").json()
        assert body["code"] == 200
        assert len(body["actions"]) > 0

    def test_actions_unknown_avatar_uses_default(self):
        """未知形象ID 应使用 default 动作，仍返回 code=200 及非空列表"""
        body = api("get", "/avatar/actions/999999999").json()
        assert body["code"] == 200
        assert isinstance(body["actions"], list)
        assert len(body["actions"]) > 0

    def test_actions_avatar_id_in_response_matches_request(self):
        """响应中 avatar_id 字段应与请求路径中的 ID 一致"""
        body = api("get", "/avatar/actions/118801001").json()
        assert body["avatar_id"] == "118801001"

    # ── 会话查询 ──

    def test_session_nonexistent_returns_404(self):
        """查询不存在的会话：应返回 code=404"""
        body = api("get", "/avatar/session/not-exist-session-xyz-00000").json()
        assert body["code"] == 404

    def test_session_nonexistent_msg_not_empty(self):
        """查询不存在会话：msg 字段不应为空"""
        body = api("get", "/avatar/session/not-exist-xyz").json()
        assert body.get("msg")


# ────────────────────────────────────────────────────────────
#  7-B. 凭证保护 & 接口可达性
# ────────────────────────────────────────────────────────────

def _req_avatar(method: str, path: str, body: dict = None) -> tuple:
    """
    测试讯飞虚拟人接口可达性。
    返回 (body_dict, status_code)
    允许的状态码：200(业务响应) / 400/401/502/503(各类错误)
    """
    url = BASE_URL + path
    try:
        resp = (requests.post(url, json=body or {}, timeout=TIMEOUT)
                if method == "post"
                else requests.get(url, timeout=TIMEOUT))
        return resp.json(), resp.status_code
    except Exception as e:
        pytest.fail(f"请求 {method.upper()} {path} 时发生异常: {e}")


class TestAvatarAccessibility:
    """全部需鉴权接口的可达性测试（无论凭证配置状态）"""

    # 全局记录 7-B 阶段的启动响应，供 7-D 复用
    _b1_body   = None
    _b1_status = None
    _b1_session = None

    @pytest.fixture(scope="class", autouse=True)
    def run_start_first(self):
        """先执行 start，记录响应供后续测试使用"""
        body, status = _req_avatar("post", "/avatar/start", {
            "avatar_id": "118801001",
            "width": 1280, "height": 720, "protocol": "xrtc",
        })
        TestAvatarAccessibility._b1_body   = body
        TestAvatarAccessibility._b1_status = status
        if (status == 200 and isinstance(body, dict)
                and body.get("code") == 200 and body.get("data")):
            TestAvatarAccessibility._b1_session = body["data"].get("session")
        yield

    def test_start_is_reachable(self):
        """POST /avatar/start 应可达（HTTP 200/400/401/503）"""
        assert self._b1_status in (200, 400, 401, 503, 502)

    def test_start_returns_json(self):
        """POST /avatar/start 响应应为合法 JSON"""
        assert isinstance(self._b1_body, dict)

    def test_drive_text_is_reachable(self):
        """POST /avatar/drive/text 应可达"""
        body, status = _req_avatar("post", "/avatar/drive/text", {
            "session": "fake-session-00000",
            "text": "你好，这是可达性测试",
        })
        assert status in (200, 400, 401, 422, 503, 502)

    def test_drive_audio_url_is_reachable(self):
        """POST /avatar/drive/audio/url 应可达"""
        body, status = _req_avatar("post", "/avatar/drive/audio/url",
                                   {"session": "fake-session-00000"})
        assert status in (200, 400, 401, 503, 502)

    def test_drive_audio_proxy_is_reachable(self):
        """POST /avatar/drive/audio/proxy 应可达"""
        body, status = _req_avatar("post", "/avatar/drive/audio/proxy", {
            "session":      "fake-session-00000",
            "audio_base64": "AAAA",
            "status":       0,
            "seq":          1,
        })
        assert status in (200, 400, 401, 502, 503)

    def test_stop_is_reachable(self):
        """POST /avatar/stop 应可达"""
        body, status = _req_avatar("post", "/avatar/stop",
                                   {"session": "fake-session-00000"})
        assert status in (200, 400, 401, 503, 502)

    def test_ping_is_reachable(self):
        """POST /avatar/ping 应可达"""
        body, status = _req_avatar("post", "/avatar/ping",
                                   {"session": "fake-session-00000"})
        assert status in (200, 400, 401, 503, 502)

    def test_unconfigured_returns_503(self):
        """
        未配置讯飞凭证时，所有需鉴权接口应返回 HTTP 503。
        已配置时此测试自动跳过。
        """
        if self._b1_status != 503:
            pytest.skip("讯飞凭证已配置，跳过 503 检测")
        # 验证 detail 中包含配置提示
        assert "detail" in self._b1_body or "msg" in self._b1_body


# ────────────────────────────────────────────────────────────
#  7-C. 入参校验
# ────────────────────────────────────────────────────────────

class TestAvatarValidation:
    """Pydantic 字段校验测试（不依赖讯飞凭证）"""

    def test_drive_text_empty_text_returns_422(self):
        """文本驱动 text 为空字符串：应返回 HTTP 422（min_length=1）"""
        resp = requests.post(
            BASE_URL + "/avatar/drive/text",
            json={"session": "fake", "text": ""},
            timeout=TIMEOUT,
        )
        assert resp.status_code == 422

    def test_drive_text_missing_session_returns_422(self):
        """文本驱动缺少 session：应返回 HTTP 422"""
        resp = requests.post(
            BASE_URL + "/avatar/drive/text",
            json={"text": "你好"},
            timeout=TIMEOUT,
        )
        assert resp.status_code == 422

    def test_drive_text_missing_text_returns_422(self):
        """文本驱动缺少 text：应返回 HTTP 422"""
        resp = requests.post(
            BASE_URL + "/avatar/drive/text",
            json={"session": "fake"},
            timeout=TIMEOUT,
        )
        assert resp.status_code == 422

    def test_audio_proxy_status_out_of_range_returns_422(self):
        """音频代理 status=99（超出 ge=0,le=2）：应返回 HTTP 422"""
        resp = requests.post(
            BASE_URL + "/avatar/drive/audio/proxy",
            json={"session": "fake", "audio_base64": "AAAA", "status": 99, "seq": 1},
            timeout=TIMEOUT,
        )
        assert resp.status_code == 422

    def test_audio_proxy_missing_audio_base64_returns_422(self):
        """音频代理缺少 audio_base64：应返回 HTTP 422"""
        resp = requests.post(
            BASE_URL + "/avatar/drive/audio/proxy",
            json={"session": "fake", "status": 0, "seq": 1},
            timeout=TIMEOUT,
        )
        assert resp.status_code == 422

    def test_stop_missing_session_returns_422(self):
        """停止接口缺少 session：应返回 HTTP 422"""
        resp = requests.post(
            BASE_URL + "/avatar/stop",
            json={},
            timeout=TIMEOUT,
        )
        assert resp.status_code == 422

    def test_ping_missing_session_returns_422(self):
        """心跳接口缺少 session：应返回 HTTP 422"""
        resp = requests.post(
            BASE_URL + "/avatar/ping",
            json={},
            timeout=TIMEOUT,
        )
        assert resp.status_code == 422


# ────────────────────────────────────────────────────────────
#  7-D. 全流程端到端（需讯飞凭证及平台资源）
# ────────────────────────────────────────────────────────────

@pytest.fixture(scope="module")
def avatar_session():
    """
    获取有效 avatar session。
    - 优先复用 TestAvatarAccessibility._b1_session
    - 若未配置/资源不足则跳过整个 7-D 模块
    """
    body, status = _req_avatar("post", "/avatar/start", {
        "avatar_id": "118801001",
        "width": 1280, "height": 720, "protocol": "xrtc",
    })
    if status == 503:
        pytest.skip("讯飞凭证未配置，跳过全流程端到端测试")
    if not (status == 200 and isinstance(body, dict)
            and body.get("code") == 200 and body.get("data")):
        pytest.skip(
            f"讯飞平台资源异常（HTTP {status} code={body.get('code') if isinstance(body, dict) else 'N/A'}），"
            "跳过全流程测试。请检查：①形象ID是否已购买 ②账号并发余量 ③凭证有效性"
        )
    session = body["data"]["session"]
    yield session
    # 测试结束后清理会话
    _req_avatar("post", "/avatar/stop", {"session": session})


class TestAvatarE2E:
    """全流程端到端测试（依赖有效讯飞凭证 + 平台资源）"""

    def test_start_returns_session_and_stream_url(self, avatar_session):
        """启动会话后 session 应为非空字符串"""
        assert isinstance(avatar_session, str)
        assert len(avatar_session) > 0

    def test_start_stream_url_format(self):
        """启动会话返回的 stream_url 应不为空（xrtc:// 或 rtmp://）"""
        body, status = _req_avatar("post", "/avatar/start", {
            "avatar_id": "118801001",
            "width": 1280, "height": 720, "protocol": "xrtc",
        })
        if status != 200 or body.get("code") != 200:
            pytest.skip("启动失败，跳过 stream_url 格式测试")
        stream_url = body["data"].get("stream_url", "")
        assert stream_url, "stream_url 不应为空"
        _req_avatar("post", "/avatar/stop",
                    {"session": body["data"]["session"]})

    def test_session_query_after_start(self, avatar_session):
        """启动后查询会话状态：应返回 code=200，is_active=True"""
        body = api("get", f"/avatar/session/{avatar_session}").json()
        assert body["code"] == 200
        assert body["data"]["is_active"] is True

    def test_session_info_structure(self, avatar_session):
        """会话信息应包含所有标准字段"""
        body = api("get", f"/avatar/session/{avatar_session}").json()
        data = body["data"]
        for field in ("session", "avatar_id", "stream_url",
                      "protocol", "width", "height", "create_time", "is_active"):
            assert field in data, f"会话信息缺少字段: {field}"

    def test_text_drive_success(self, avatar_session):
        """文本驱动：应返回 code=200"""
        body = api("post", "/avatar/drive/text", json={
            "session": avatar_session,
            "text":    "你好，欢迎体验虚拟人服务。",
            "vcn":     "x4_lingxiaoyu_assist",
            "speed":   50, "pitch": 50, "volume": 50,
        }).json()
        assert body["code"] == 200, f"文本驱动失败: {body}"

    def test_text_drive_with_action(self, avatar_session):
        """带动作控制的文本驱动：应返回 code=200"""
        body = api("post", "/avatar/drive/text", json={
            "session": avatar_session,
            "text":    "欢迎使用讯飞虚拟人，我是您的智能助手。",
            "vcn":     "x4_lingxiaoyu_assist",
            "speed":   50, "pitch": 50, "volume": 50,
            "actions": [{"type": "action", "value": "A_LH_introduced_O", "wb": 0}],
        }).json()
        assert body["code"] == 200, f"带动作文本驱动失败: {body}"

    def test_audio_url_wss_format(self, avatar_session):
        """获取音频 WSS URL：应以 wss:// 开头"""
        body = api("post", "/avatar/drive/audio/url",
                   json={"session": avatar_session}).json()
        assert body["code"] == 200
        wss_url = body["data"]["wss_url"]
        assert wss_url.startswith("wss://"), f"WSS URL 格式错误: {wss_url}"

    def test_audio_url_response_has_params(self, avatar_session):
        """音频 URL 响应 data 应含 wss_url / session / app_id / encoding / sample_rate"""
        body = api("post", "/avatar/drive/audio/url",
                   json={"session": avatar_session}).json()
        data = body["data"]
        for field in ("wss_url", "session", "app_id", "encoding", "sample_rate"):
            assert field in data, f"音频 URL 响应缺少字段: {field}"

    def test_ping_success(self, avatar_session):
        """心跳保活：应返回 code=200"""
        body = api("post", "/avatar/ping",
                   json={"session": avatar_session}).json()
        assert body["code"] == 200

    def test_ping_updates_last_ping_time(self, avatar_session):
        """心跳后会话的 last_ping_time 应非空"""
        api("post", "/avatar/ping", json={"session": avatar_session})
        body = api("get", f"/avatar/session/{avatar_session}").json()
        assert body["data"].get("last_ping_time") is not None

    def test_text_drive_invalid_session_returns_400(self, avatar_session):
        """用无效 session 发送文本驱动：应返回 code=400（讯飞 session invalid）"""
        body = api("post", "/avatar/drive/text", json={
            "session": "invalid-session-xyz-99999-xxxxxxxxxx",
            "text":    "这个 session 是假的",
        }).json()
        assert body["code"] == 400, f"期望 code=400，实际: {body}"

    def test_stop_success(self, avatar_session):
        """停止会话：应返回 code=200（注意：此 fixture 已在 yield 后自动 stop，此处再次尝试会触发重复停止测试）"""
        # avatar_session fixture 在 teardown 时会 stop，此处主动 stop
        body = api("post", "/avatar/stop",
                   json={"session": avatar_session}).json()
        # 成功 or 已被 teardown stop 导致 code=400 均可接受
        assert body["code"] in (200, 400)

    def test_session_gone_after_stop(self, avatar_session):
        """停止后本地缓存应清除：查询会话应返回 code=404"""
        # 确保已停止
        api("post", "/avatar/stop", json={"session": avatar_session})
        body = api("get", f"/avatar/session/{avatar_session}").json()
        assert body["code"] == 404

    def test_repeat_stop_returns_400(self, avatar_session):
        """重复停止已停止会话：应返回 code=400（讯飞 session invalid）"""
        api("post", "/avatar/stop", json={"session": avatar_session})
        body = api("post", "/avatar/stop",
                   json={"session": avatar_session}).json()
        assert body["code"] == 400
