"""
实时讲解历史记录 端到端测试

验证：
- 添加记录：image / ai_response 原样存储、原样返回（后端不解析）
- 用户列表：按 user_id 反查，返回全部记录且内容一致
- 清理：删除测试数据，避免污染
"""
import os
import time
import sys

# 将项目根目录加入 sys.path，保证可 import main
PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if PROJECT_ROOT not in sys.path:
    sys.path.insert(0, PROJECT_ROOT)

from database.database import ensure_tables_exist, SessionLocal
from models.realtime_explain import RealtimeExplainHistory


TEST_USER = f"rt_test_{int(time.time())}"
IMG_1 = "data:image/jpeg;base64,/9j/4AAQSkZJRg_TEST_A"
AI_1 = '{"spots":[{"name":"灵山大佛","duration":30}],"summary":"讲解1"}'
IMG_2 = "https://cdn.example.com/explain2.png"
AI_2 = '{"spots":[{"name":"九龙灌浴"}],"summary":"讲解2"}'


def _cleanup():
    db = SessionLocal()
    try:
        db.query(RealtimeExplainHistory).filter(
            RealtimeExplainHistory.user_id == TEST_USER
        ).delete()
        db.commit()
    finally:
        db.close()


def main():
    ensure_tables_exist()

    # 用完整 app 测试，验证路由已注册且与其他模块共存
    from fastapi.testclient import TestClient
    import main
    client = TestClient(main.app)

    # 1) 添加第一条
    r1 = client.post("/realtime-explain/history", json={
        "user_id": TEST_USER,
        "image": IMG_1,
        "ai_response": AI_1,
        "scenic_id": "lingshan",
    })
    assert r1.status_code == 200, f"添加1失败：{r1.text}"
    d1 = r1.json()["data"]
    assert d1["image"] == IMG_1, "image 未原样存储"
    assert d1["ai_response"] == AI_1, "ai_response 未原样存储"
    assert d1["user_id"] == TEST_USER
    id1 = d1["id"]
    print(f"[main] 添加1 OK id={id1} image/ai_response 原样一致")

    # 2) 添加第二条
    r2 = client.post("/realtime-explain/history", json={
        "user_id": TEST_USER,
        "image": IMG_2,
        "ai_response": AI_2,
    })
    assert r2.status_code == 200, f"添加2失败：{r2.text}"
    d2 = r2.json()["data"]
    assert d2["image"] == IMG_2 and d2["ai_response"] == AI_2
    id2 = d2["id"]
    print(f"[main] 添加2 OK id={id2} image/ai_response 原样一致")

    # 3) 用户列表
    rl = client.get(f"/realtime-explain/history/user/{TEST_USER}")
    assert rl.status_code == 200, f"列表失败：{rl.text}"
    body = rl.json()["data"]
    assert body["total"] == 2, f"列表 total 应为2，实际 {body['total']}"
    assert len(body["items"]) == 2
    # 不依赖严格顺序（同秒入库时 created_at 可能相同），校验两条内容均原样返回
    got = {(it["id"], it["image"], it["ai_response"]) for it in body["items"]}
    assert (id1, IMG_1, AI_1) in got, "列表缺少记录1（内容未原样返回）"
    assert (id2, IMG_2, AI_2) in got, "列表缺少记录2（内容未原样返回）"
    print(f"[main] 列表 OK total=2，两条内容均原样返回")

    # 4) 未登录（user_id 为空）也能存
    r3 = client.post("/realtime-explain/history", json={
        "image": "data:image/png;base64,EMPTY_USER_TEST",
        "ai_response": '{"note":"no user"}',
    })
    assert r3.status_code == 200 and r3.json()["data"]["user_id"] is None
    print("[main] 未登录（user_id 空）添加 OK")

    _cleanup()
    print("REALTIME_OK ✅")


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        _cleanup()
        print(f"REALTIME_FAIL ❌ {e}")
        raise
