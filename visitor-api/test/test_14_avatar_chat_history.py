"""
数字人对话历史列表端点 + 实时讲解 user_id 归一化 测试

验证：
- GET /avatar/chat/history/user/{user_id}：分页、total、last_message（取最新一条消息）
- user_id 归一化：保存/查询两端去空白一致；数字(Long)与字符串 user_id 均可匹配
- 不影响现有实时讲解原样存储/返回逻辑
- 清理：删除测试数据，避免污染
"""
import os
import time
import sys

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if PROJECT_ROOT not in sys.path:
    sys.path.insert(0, PROJECT_ROOT)

from database.database import ensure_tables_exist, SessionLocal
from models.avatar_chat import AvatarChatSession, AvatarChatMessage
from models.realtime_explain import RealtimeExplainHistory


TEST_USER = f"chathist_{int(time.time())}"
RT_USER = f"rtnorm_{int(time.time())}"


def _cleanup():
    db = SessionLocal()
    try:
        db.query(AvatarChatMessage).filter(
            AvatarChatMessage.session_id.like(f"{TEST_USER}_%")
        ).delete(synchronize_session=False)
        db.query(AvatarChatSession).filter(
            AvatarChatSession.user_id == TEST_USER
        ).delete()
        # 实时讲解：字符串与数字两种存法都清掉
        db.query(RealtimeExplainHistory).filter(
            RealtimeExplainHistory.user_id == RT_USER
        ).delete()
        db.query(RealtimeExplainHistory).filter(
            RealtimeExplainHistory.user_id == str(RT_USER)
        ).delete()
        db.query(RealtimeExplainHistory).filter(
            RealtimeExplainHistory.user_id == "12345"
        ).delete()
        db.commit()
    finally:
        db.close()


def main():
    ensure_tables_exist()

    from fastapi.testclient import TestClient
    import main
    client = TestClient(main.app)

    # ── 准备：直接落库 2 个会话 + 若干消息（绕过讯飞，纯测读写）──
    db = SessionLocal()
    try:
        s1 = AvatarChatSession(
            session_id=f"{TEST_USER}_s1", user_id=TEST_USER,
            scenic_id="lingshan", avatar_id="118801001",
            message_count=2,  # s1 有 2 条消息
        )
        s2 = AvatarChatSession(
            session_id=f"{TEST_USER}_s2", user_id=TEST_USER,
            scenic_id="lingshan", avatar_id="118801001",
            message_count=1,  # s2 有 1 条消息
        )
        db.add_all([s1, s2])
        db.flush()
        db.add(AvatarChatMessage(
            session_id=f"{TEST_USER}_s1", seq_no=1,
            role="user", content="你好"))
        db.add(AvatarChatMessage(
            session_id=f"{TEST_USER}_s1", seq_no=2,
            role="assistant", content="您好，我是灵山讲解员"))
        db.add(AvatarChatMessage(
            session_id=f"{TEST_USER}_s2", seq_no=1,
            role="user", content="介绍一下灵山大佛"))
        db.commit()
    finally:
        db.close()

    # 1) 列表：total=2，last_message 正确
    r = client.get(f"/avatar/chat/history/user/{TEST_USER}?page=1&page_size=20")
    assert r.status_code == 200, f"列表失败：{r.text}"
    body = r.json()["data"]
    assert body["total"] == 2, f"total 应为2，实际 {body['total']}"
    items = {(it["session_id"], it["last_message"]) for it in body["items"]}
    assert (f"{TEST_USER}_s1", "您好，我是灵山讲解员") in items, "s1 最后一条应为 assistant"
    assert (f"{TEST_USER}_s2", "介绍一下灵山大佛") in items, "s2 最后一条应为 user"
    # message_count 应原样返回（由 start/drive/stop 的埋点维护）
    mc_map = {it["session_id"]: it["message_count"] for it in body["items"]}
    assert mc_map[f"{TEST_USER}_s1"] == 2, "s1 message_count 应为2"
    assert mc_map[f"{TEST_USER}_s2"] == 1, "s2 message_count 应为1"
    print("[main] 对话历史列表 OK total=2，last_message 正确，message_count 正确")

    # 2) 分页
    r2 = client.get(f"/avatar/chat/history/user/{TEST_USER}?page=1&page_size=1")
    assert r2.status_code == 200, r2.text
    b2 = r2.json()["data"]
    assert b2["total"] == 2 and b2["page_size"] == 1 and len(b2["items"]) == 1
    assert b2["page"] == 1
    print("[main] 分页 OK（total=2, page_size=1, items=1）")

    # 3) user_id 归一化：查询时带首尾空格仍能命中（后端 strip）
    r3 = client.get(f"/avatar/chat/history/user/ {TEST_USER} ")
    assert r3.status_code == 200 and r3.json()["data"]["total"] == 2, \
        "user_id 带空格查询应被归一化命中"
    print("[main] user_id 归一化查询 OK（首尾空格命中）")

    # 4) 实时讲解：数字(Long) user_id 保存后，字符串 user_id 查询可命中
    r4 = client.post("/realtime-explain/history", json={
        "user_id": 12345,  # 前端常传 Long
        "image": "data:image/jpeg;base64,INTUSER",
        "ai_response": '{"spots":[{"name":"测试"}]}',
    })
    assert r4.status_code == 200, f"实时讲解(数字user_id)添加失败：{r4.text}"
    assert r4.json()["data"]["user_id"] == "12345", "数字 user_id 应被归一为字符串"
    rl = client.get("/realtime-explain/history/user/12345")
    assert rl.status_code == 200 and rl.json()["data"]["total"] == 1, \
        "数字 user_id 保存后，字符串查询应命中"
    print("[main] 实时讲解 user_id 数字→字符串 归一化匹配 OK")

    # 5) 实时讲解：字符串 user_id 带空格保存，归一化查询命中
    r5 = client.post("/realtime-explain/history", json={
        "user_id": f" {RT_USER} ",  # 带空格
        "image": "data:image/jpeg;base64,STRUSER",
        "ai_response": '{"spots":[{"name":"测试2"}]}',
    })
    assert r5.status_code == 200
    rl2 = client.get(f"/realtime-explain/history/user/{RT_USER}")
    assert rl2.status_code == 200 and rl2.json()["data"]["total"] == 1, \
        "带空格 user_id 保存后，归一化查询应命中"
    print("[main] 实时讲解 user_id 空格归一化匹配 OK")

    _cleanup()
    print("CHAT_HISTORY_OK ✅")


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        _cleanup()
        print(f"CHAT_HISTORY_FAIL ❌ {e}")
        raise
