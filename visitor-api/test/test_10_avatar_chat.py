"""
游客端（主项目）：数字人对话历史写入 + 端到端校验
=================================================
测试内容：
  1. 建表（avatar_chat_sessions / avatar_chat_messages，幂等）
  2. 通过 router 内的 _chat_log_* 辅助函数模拟
     /avatar/start → /avatar/drive/text → /avatar/stop 三个埋点的真实写库逻辑
  3. 断言写库结果正确（会话、消息数、角色、结束时间）
  4. 在独立进程中运行管理端读取接口（TestClient），断言管理端能读到相同数据
  5. 清理测试数据（不污染生产库）

运行：
  Z:\\SoftwareCupAI\\.venv\\Scripts\\python.exe test\\test_10_avatar_chat.py
"""
import os
import sys
import subprocess

MAIN_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, MAIN_ROOT)

# ── 0. 基础导入（触发建表） ───────────────────────────────────────
from database.database import SessionLocal, ensure_tables_exist  # noqa: E402
ensure_tables_exist()  # 创建 avatar_chat_* 两张新表（幂等，不动任何原表）

from models.avatar_chat import AvatarChatSession, AvatarChatMessage  # noqa: E402
from routers.avatar import (  # noqa: E402
    _chat_log_session,
    _chat_log_message,
    _chat_end_session,
)

TEST_SESSION = "TEST_AVATARCHAT_0001"
TEST_USER = "u_test_001"
SCENIC = "lingshan"
AVATAR = "118801001"  # 依丹（仍在 avatar_catalog 中，用于验证 avatar_name 关联）

# 对话样本（user=游客提问，assistant=数字人回答）
TURNS = [
    ("user",   "你知道灵山有哪些必看景点吗？", "audio"),
    ("assistant", "灵山胜境必看的有灵山大佛、九龙灌浴和梵宫。", "text"),
    ("user",   "大佛有多高？", "audio"),
    ("assistant", "灵山大佛高 88 米，是举世闻名的青铜释迦牟尼立像。", "text"),
]


def _cleanup(db):
    db.query(AvatarChatMessage).filter(
        AvatarChatMessage.session_id == TEST_SESSION
    ).delete()
    db.query(AvatarChatSession).filter(
        AvatarChatSession.session_id == TEST_SESSION
    ).delete()
    db.commit()


def test_write():
    db = SessionLocal()
    try:
        _cleanup(db)  # 先清掉历史残留

        # 模拟 /avatar/start 成功 → 写会话
        _chat_log_session(db, TEST_SESSION, TEST_USER, SCENIC, AVATAR)

        # 模拟两轮 /avatar/drive/text 成功 → 写 user + assistant 消息
        for role, content, source in TURNS:
            _chat_log_message(db, TEST_SESSION, role, content, source)

        # 模拟 /avatar/stop → 结束会话 + 重算消息数
        _chat_end_session(db, TEST_SESSION)

        # ── 断言 ──
        sess = db.query(AvatarChatSession).filter(
            AvatarChatSession.session_id == TEST_SESSION
        ).first()
        assert sess is not None, "会话未写入"
        assert sess.user_id == TEST_USER, f"user_id 不符：{sess.user_id}"
        assert sess.scenic_id == SCENIC
        assert sess.avatar_id == AVATAR
        assert sess.ended_at is not None, "ended_at 未写入（stop 埋点失效）"
        assert sess.message_count == len(TURNS), (
            f"message_count 应为 {len(TURNS)}，实际 {sess.message_count}"
        )

        msgs = db.query(AvatarChatMessage).filter(
            AvatarChatMessage.session_id == TEST_SESSION
        ).order_by(AvatarChatMessage.seq_no).all()
        assert len(msgs) == len(TURNS), f"消息条数应为 {len(TURNS)}，实际 {len(msgs)}"
        for i, (role, content, source) in enumerate(TURNS):
            m = msgs[i]
            assert m.role == role, f"第{i+1}条 role 应为 {role}"
            assert m.content == content, f"第{i+1}条 content 不符"
            assert m.source == source, f"第{i+1}条 source 应为 {source}"
            assert m.seq_no == i + 1, f"seq_no 应为 {i+1}，实际 {m.seq_no}"

        print(f"[main] 写入校验 OK：会话1 + 消息{len(TURNS)}条，ended_at 已落库")
    finally:
        db.close()


def test_admin_read():
    """独立进程运行管理端读取测试，避免与主项目同名模块冲突"""
    env = dict(os.environ)
    env["TEST_SESSION"] = TEST_SESSION
    env["TEST_USER"] = TEST_USER
    env["EXPECTED_MSG_COUNT"] = str(len(TURNS))

    # 管理端与游客端共享同一 MySQL；沙箱中管理端 .env 的 DB 密码为空，
    # 这里把游客端可用的 DB 连接信息注入子进程环境，使其能连上同一库做读取校验。
    from config import get_settings as _main_settings
    _ms = _main_settings()
    env["DB_HOST"] = _ms.DB_HOST
    env["DB_PORT"] = str(_ms.DB_PORT)
    env["DB_USER"] = _ms.DB_USER
    env["DB_PASSWORD"] = _ms.DB_PASSWORD
    env["DB_NAME"] = _ms.DB_NAME

    admin_test = os.path.join(MAIN_ROOT, "..", "SoftwareCupAI_admin", "test", "test_dialogue_read.py")
    admin_test = os.path.abspath(admin_test)
    # 用主项目 venv 的 python 运行（依赖 fastapi/httpx/sqlalchemy 均具备）
    py = os.path.join(MAIN_ROOT, ".venv", "Scripts", "python.exe")
    if not os.path.exists(py):
        py = sys.executable

    print(f"[main] 调用管理端读取测试：{admin_test}")
    proc = subprocess.run(
        [py, admin_test],
        env=env,
        cwd=os.path.dirname(admin_test),  # 关键：让管理端加载自己的 .env，而非游客端
        capture_output=True,
        text=True,
    )
    print(proc.stdout)
    if proc.returncode != 0:
        print("=== admin 读取测试失败 stderr ===")
        print(proc.stderr)
        raise AssertionError(f"管理端读取测试失败，returncode={proc.returncode}")


def main():
    test_write()
    test_admin_read()
    # 清理测试数据（不污染生产库）
    db = SessionLocal()
    try:
        _cleanup(db)
        print("[main] 测试数据已清理")
    finally:
        db.close()
    print("\nALL_OK ✅")


if __name__ == "__main__":
    main()
