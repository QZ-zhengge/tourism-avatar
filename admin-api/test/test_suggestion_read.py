"""
管理端：建议信箱读取测试（独立进程运行，避免与游客端同名模块冲突）
运行：python test_suggestion_read.py
依赖环境变量：TEST_SUGGESTION_ID / TEST_USER_ID（由主项目测试脚本传入）

说明：只挂载 suggestion 路由（本功能改动点），不导入 main（其 auth 路由依赖 jwt，
      与本功能无关），用最小 FastAPI 应用 + TestClient 校验真实接口契约。
"""
import os
import sys

# 仅把管理端根目录放入 sys.path，避免与游客端同名模块(config/database/models)冲突
ADMIN_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, ADMIN_ROOT)

from fastapi import FastAPI
from fastapi.testclient import TestClient
from routers.suggestion import router  # 仅挂载建议信箱路由（本功能改动点）

TEST_SUGGESTION_ID = os.environ.get("TEST_SUGGESTION_ID", "")
TEST_USER_ID = int(os.environ.get("TEST_USER_ID", "0"))


def main():
    app = FastAPI()
    app.include_router(router)
    client = TestClient(app)

    # 建议列表（管理端读取，连通游客端写入的同一张 user_suggestions 表）
    r = client.get("/api/v1/admin/suggestions")
    assert r.status_code == 200, f"list status={r.status_code} body={r.text}"
    body = r.json()
    items = body.get("items") or (body.get("data", {}) or {}).get("suggestions") or []
    item = next((i for i in items if i["id"] == TEST_SUGGESTION_ID), None)
    assert item is not None, f"测试建议未出现在管理端列表：{items}"
    assert item["user_id"] == TEST_USER_ID, f"user_id 不匹配：{item}"
    assert item.get("content"), f"content 为空：{item}"
    print(f"[admin] 建议列表 OK：命中 id={item['id']} user_id={item['user_id']} status={item['status']}")

    # 标记已读（连通验证：管理端可操作游客端提交的数据）
    r2 = client.put(f"/api/v1/admin/suggestions/{TEST_SUGGESTION_ID}/read")
    assert r2.status_code == 200, f"read status={r2.status_code} body={r2.text}"
    print("[admin] 标记已读 OK")

    # 再次读取，确认状态变为 read
    r3 = client.get("/api/v1/admin/suggestions")
    body3 = r3.json()
    items3 = body3.get("items") or (body3.get("data", {}) or {}).get("suggestions") or []
    item3 = next((i for i in items3 if i["id"] == TEST_SUGGESTION_ID), None)
    assert item3 is not None and item3["status"] == "read", f"状态未更新：{item3}"
    print("[admin] 状态更新为 read OK")

    print("ADMIN_READ_OK")


if __name__ == "__main__":
    main()
