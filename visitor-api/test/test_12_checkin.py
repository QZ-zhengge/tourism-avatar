"""
地图打卡功能 端到端测试

覆盖：
1. 标准省份编号列表（GET /checkin/provinces）→ 34 条
2. 添加打卡（POST /checkin/）→ 成功，newly_added=True
3. 重复打卡同一省份 → 幂等，newly_added=False
4. 无效省份编号 → code=400
5. 获取用户已打卡省份编号列表（GET /checkin/user/{id}）→ 正确
6. 数据清理
"""
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from database.database import ensure_tables_exist, SessionLocal
from models.user import User
from models.checkin import UserCheckin
import main

client = main.app.test_client() if hasattr(main.app, "test_client") else None
from fastapi.testclient import TestClient  # noqa: E402
client = TestClient(main.app)


def setup_user():
    ensure_tables_exist()
    ts = int(time.time() * 1000)
    db = SessionLocal()
    u = User(
        username=f"ck_{ts}",
        email=f"ck_{ts}@test.com",
        phone_number=f"139{ts % 100000000:08d}",
        password="CHANGE_ME",
        nickname="ck",
    )
    db.add(u)
    db.commit()
    db.refresh(u)
    uid = u.id
    db.close()
    return uid


def teardown_user(uid):
    db = SessionLocal()
    db.query(UserCheckin).filter(UserCheckin.user_id == uid).delete()
    db.query(User).filter(User.id == uid).delete()
    db.commit()
    db.close()


def main_test():
    uid = setup_user()
    try:
        # 1. 省份列表
        r = client.get("/checkin/provinces")
        assert r.status_code == 200, r.text
        prov = r.json()["data"]
        assert len(prov) == 34, f"省份数应为34，实际 {len(prov)}"
        codes = {p["province_code"] for p in prov}
        assert "110000" in codes and "310000" in codes
        print(f"[省份列表] OK：共 {len(prov)} 个省/直辖市/自治区/特区")

        # 2. 添加打卡（北京）
        r = client.post("/checkin/", json={"user_id": uid, "province_code": "110000"})
        assert r.status_code == 200, r.text
        assert r.json()["code"] == 200
        assert r.json()["data"]["newly_added"] is True, "首次打卡应 newly_added=True"
        assert r.json()["data"]["province_name"] == "北京市"
        print("[添加打卡] OK：北京 newly_added=True")

        # 3. 重复打卡（幂等）
        r = client.post("/checkin/", json={"user_id": uid, "province_code": "110000"})
        assert r.status_code == 200
        assert r.json()["data"]["newly_added"] is False, "重复打卡应 newly_added=False"
        print("[重复打卡] OK：幂等 newly_added=False")

        # 4. 无效省份编号
        r = client.post("/checkin/", json={"user_id": uid, "province_code": "999999"})
        assert r.status_code == 200 and r.json()["code"] == 400, "无效编号应返回 400"
        print("[无效编号] OK：code=400")

        # 5. 再点亮上海
        r = client.post("/checkin/", json={"user_id": uid, "province_code": "310000"})
        assert r.json()["data"]["newly_added"] is True

        # 6. 获取用户已打卡列表
        r = client.get(f"/checkin/user/{uid}")
        assert r.status_code == 200, r.text
        got = r.json()["data"]
        assert set(got) == {"110000", "310000"}, f"已打卡列表应为北京+上海，实际 {got}"
        print(f"[用户打卡列表] OK：{got}")

        print("CHECKIN_OK ✅")
    finally:
        teardown_user(uid)


if __name__ == "__main__":
    main_test()
