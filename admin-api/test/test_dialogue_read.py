"""
管理端：数字人对话历史读取测试（独立进程运行，避免与游客端同名模块冲突）
运行：python test_dialogue_read.py
依赖环境变量：TEST_SESSION / TEST_USER / EXPECTED_MSG_COUNT（由主项目测试脚本传入）

说明：只挂载 dialogue 路由（本功能改动点），不导入 main（其 auth 路由依赖 jwt，
      与本功能无关），用最小 FastAPI 应用 + TestClient 校验真实接口契约。
"""
import os
import sys

# 仅把管理端根目录放入 sys.path，避免与游客端同名模块(config/database/models)冲突
ADMIN_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, ADMIN_ROOT)

from fastapi import FastAPI
from fastapi.testclient import TestClient
from routers.dialogue import router  # 仅挂载对话分析路由（本功能改动点）

TEST_SESSION = os.environ.get("TEST_SESSION", "TEST_AVATARCHAT_0001")
TEST_USER = os.environ.get("TEST_USER", "u_test_001")
EXPECTED_MSG_COUNT = int(os.environ.get("EXPECTED_MSG_COUNT", "4"))


def main():
    app = FastAPI()
    app.include_router(router)
    client = TestClient(app)

    # 1) 会话列表
    r = client.get("/api/v1/admin/dialogue/sessions", params={"scenic_id": "lingshan"})
    assert r.status_code == 200, f"list status={r.status_code} body={r.text}"
    data = r.json().get("data", {})
    items = data.get("items", [])
    item = next((i for i in items if i["session_id"] == TEST_SESSION), None)
    assert item is not None, f"测试会话未出现在管理端列表：{items}"
    assert item["user_id"] == TEST_USER, f"user_id 不匹配：{item}"
    assert item["message_count"] == EXPECTED_MSG_COUNT, f"message_count 不匹配：{item}"
    assert item.get("avatar_name"), f"avatar_name 未关联出来：{item}"
    print(f"[admin] 列表 OK：total={data.get('total')} 命中 message_count={item['message_count']} avatar_name={item['avatar_name']}")

    # 2) 消息详情
    r2 = client.get(f"/api/v1/admin/dialogue/sessions/{TEST_SESSION}/messages")
    assert r2.status_code == 200, f"detail status={r2.status_code} body={r2.text}"
    d2 = r2.json().get("data", {})
    assert d2["message_count"] == EXPECTED_MSG_COUNT, d2
    msgs = d2["messages"]
    assert len(msgs) == EXPECTED_MSG_COUNT, f"消息条数不符：{msgs}"
    assert msgs[0]["role"] == "user", f"首条应为 user：{msgs[0]}"
    assert msgs[1]["role"] == "assistant", f"第二条应为 assistant：{msgs[1]}"
    # 校验字段完整性
    for m in msgs:
        assert "seq_no" in m and "content" in m and "source" in m and "created_at" in m, m
    print(f"[admin] 详情 OK：{len(msgs)} 条消息，首条 user='{msgs[0]['content'][:12]}...'")

    # 3) 404 路径
    r3 = client.get("/api/v1/admin/dialogue/sessions/NON_EXISTENT_SESSION/messages")
    assert r3.status_code == 404, f"期望404：{r3.status_code}"
    print("[admin] 404 路径 OK")

    print("ADMIN_READ_OK")


if __name__ == "__main__":
    main()
