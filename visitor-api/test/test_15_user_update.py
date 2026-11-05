"""
修改个人信息 & 修改密码 端到端测试

验证：
- PUT /user/profile/：修改昵称/头像/邮箱/手机号，唯一性检查
- PUT /user/password/：旧密码验证 + 新密码一致性 + 修改成功后可新密码登录
- 清理：恢复测试用户原始数据
"""
import os
import time
import sys

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if PROJECT_ROOT not in sys.path:
    sys.path.insert(0, PROJECT_ROOT)

from database.database import ensure_tables_exist, SessionLocal
from models.user import User

TEST_USERNAME = f"updtest_{int(time.time())}"
TEST_PASSWORD = "CHANGE_ME"
TEST_EMAIL = f"{TEST_USERNAME}@test.com"
TEST_PHONE = "13800138000"

_ORIG = {}


def _cleanup():
    db = SessionLocal()
    try:
        u = db.query(User).filter(User.username == TEST_USERNAME).first()
        if u:
            db.delete(u)
            db.commit()
    finally:
        db.close()


def main():
    ensure_tables_exist()

    from fastapi.testclient import TestClient
    import main
    client = TestClient(main.app)

    # ── 准备：注册一个测试用户 ──
    r = client.post("/user/register/", json={
        "username": TEST_USERNAME,
        "password": TEST_PASSWORD,
        "confirm_password": TEST_PASSWORD,
        "email": TEST_EMAIL,
        "phone_number": TEST_PHONE,
    })
    assert r.status_code == 200 and r.json()["code"] == 200, f"注册失败: {r.text}"

    # 登录取 user_id
    r2 = client.post("/user/login/", json={
        "username": TEST_USERNAME, "password": TEST_PASSWORD,
    })
    assert r2.status_code == 200 and r2.json()["code"] == 200, f"登录失败: {r2.text}"
    uid = r2.json()["data"]["userId"]

    # 1) 修改昵称和头像
    r3 = client.put("/user/profile/", json={
        "user_id": uid, "nickname": "测试昵称", "avatar": "https://cdn.example.com/avatar.png",
    })
    assert r3.status_code == 200 and r3.json()["code"] == 200, r3.text
    d = r3.json()["data"]
    assert d["nickname"] == "测试昵称"
    assert d["avatar"] == "https://cdn.example.com/avatar.png"
    print("[main] 修改昵称+头像 OK")

    # 2) 修改邮箱（唯一性：改成自己不变 → OK）
    r4 = client.put("/user/profile/", json={
        "user_id": uid, "email": TEST_EMAIL,
    })
    assert r4.status_code == 200 and r4.json()["code"] == 200, r4.text
    print("[main] 修改邮箱（不变）OK")

    # 3) 修改手机号
    new_phone = "13800138000"
    r5 = client.put("/user/profile/", json={
        "user_id": uid, "phone_number": new_phone,
    })
    assert r5.status_code == 200 and r5.json()["code"] == 200, r5.text
    assert r5.json()["data"]["phone_number"] == new_phone
    # 改回原手机号（避免后续测试受唯一性影响）
    client.put("/user/profile/", json={"user_id": uid, "phone_number": TEST_PHONE})
    print("[main] 修改手机号 OK")

    # 4) 空字段不修改
    r6 = client.put("/user/profile/", json={"user_id": uid})
    assert r6.status_code == 200 and r6.json()["code"] == 400  # 未提供任何字段
    print("[main] 空字段拒绝 OK")

    # 5) 修改密码：旧密码错误
    r7 = client.put("/user/password/", json={
        "user_id": uid, "old_password": "CHANGE_ME", "new_password": "CHANGE_ME", "confirm_password": "CHANGE_ME",
    })
    assert r7.status_code == 200 and r7.json()["code"] == 400
    print("[main] 旧密码错误拒绝 OK")

    # 6) 修改密码：两次不一致
    r8 = client.put("/user/password/", json={
        "user_id": uid, "old_password": TEST_PASSWORD, "new_password": "CHANGE_ME", "confirm_password": "CHANGE_ME",
    })
    assert r8.status_code == 200 and r8.json()["code"] == 400
    print("[main] 两次密码不一致拒绝 OK")

    # 7) 修改密码成功，用新密码登录
    r9 = client.put("/user/password/", json={
        "user_id": uid, "old_password": TEST_PASSWORD, "new_password": "CHANGE_ME", "confirm_password": "CHANGE_ME",
    })
    assert r9.status_code == 200 and r9.json()["code"] == 200, r9.text
    # 新密码登录
    r10 = client.post("/user/login/", json={"username": TEST_USERNAME, "password": "CHANGE_ME"})
    assert r10.status_code == 200 and r10.json()["code"] == 200, r10.text
    # 旧密码登录应失败
    r11 = client.post("/user/login/", json={"username": TEST_USERNAME, "password": TEST_PASSWORD})
    assert r11.json()["code"] == 400
    print("[main] 修改密码+新密码登录 OK")

    _cleanup()
    print("USER_UPDATE_OK ✅")


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        _cleanup()
        print(f"USER_UPDATE_FAIL ❌ {e}")
        raise
