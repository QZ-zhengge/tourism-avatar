"""
测试模块 02 — 用户模块
覆盖业务逻辑：
  - 用户注册：成功 / 用户名重复 / 邮箱重复 / 手机号重复 / 密码不一致
  - 用户登录：成功 / 用户名不存在 / 密码错误
  - 响应结构完整性校验
"""
import random
import pytest
from conftest import api, rand_str


# ────────────────────────────────────────────────────────────
#  辅助函数
# ────────────────────────────────────────────────────────────

def _new_user_payload(suffix=None):
    """生成唯一用户注册数据"""
    s = suffix or rand_str(8)
    phone = f"1{random.randint(3000000000, 9999999999)}"
    return {
        "username":         f"usr_{s}",
        "password":         "CHANGE_ME",
        "confirm_password": "CHANGE_ME",
        "email":            f"usr_{s}@example.com",
        "phone_number":     phone,
    }


# ────────────────────────────────────────────────────────────
#  注册接口测试
# ────────────────────────────────────────────────────────────

class TestUserRegister:

    def test_register_success(self):
        """正常注册：应返回 code=200"""
        resp = api("post", "/user/register/", json=_new_user_payload())
        assert resp.status_code == 200
        body = resp.json()
        assert body["code"] == 200
        assert "注册成功" in body["msg"]

    def test_register_response_has_code_and_msg(self):
        """注册响应体必须包含 code 和 msg 字段"""
        body = api("post", "/user/register/", json=_new_user_payload()).json()
        assert "code" in body
        assert "msg" in body

    def test_register_duplicate_username(self):
        """重复用户名注册：应返回 code=400，提示用户名已存在"""
        payload = _new_user_payload()
        # 先注册一次
        r1 = api("post", "/user/register/", json=payload)
        assert r1.json()["code"] == 200
        # 改邮箱和手机号再注册，保持相同用户名
        payload2 = _new_user_payload()
        payload2["username"] = payload["username"]
        resp = api("post", "/user/register/", json=payload2)
        assert resp.status_code == 200
        body = resp.json()
        assert body["code"] == 400
        assert "用户名已存在" in body["msg"]

    def test_register_duplicate_email(self):
        """重复邮箱注册：应返回 code=400，提示邮箱已被注册"""
        payload = _new_user_payload()
        r1 = api("post", "/user/register/", json=payload)
        assert r1.json()["code"] == 200

        payload2 = _new_user_payload()
        payload2["email"] = payload["email"]   # 相同邮箱
        resp = api("post", "/user/register/", json=payload2)
        body = resp.json()
        assert body["code"] == 400
        assert "邮箱" in body["msg"]

    def test_register_duplicate_phone(self):
        """重复手机号注册：应返回 code=400，提示手机号已被注册"""
        payload = _new_user_payload()
        r1 = api("post", "/user/register/", json=payload)
        assert r1.json()["code"] == 200

        payload2 = _new_user_payload()
        payload2["phone_number"] = payload["phone_number"]  # 相同手机号
        resp = api("post", "/user/register/", json=payload2)
        body = resp.json()
        assert body["code"] == 400
        assert "手机号" in body["msg"]

    def test_register_password_mismatch(self):
        """密码不一致：应返回 code=400"""
        payload = _new_user_payload()
        payload["confirm_password"] = "WrongPass999"
        resp = api("post", "/user/register/", json=payload)
        body = resp.json()
        assert body["code"] == 400
        assert "密码" in body["msg"]

    def test_register_missing_username_returns_422(self):
        """缺少必填字段 username：应返回 HTTP 422"""
        payload = _new_user_payload()
        del payload["username"]
        resp = api("post", "/user/register/", json=payload)
        assert resp.status_code == 422

    def test_register_missing_password_returns_422(self):
        """缺少必填字段 password：应返回 HTTP 422"""
        payload = _new_user_payload()
        del payload["password"]
        resp = api("post", "/user/register/", json=payload)
        assert resp.status_code == 422

    def test_register_invalid_email_format(self):
        """无效邮箱格式：Pydantic 应拦截，返回 422"""
        payload = _new_user_payload()
        payload["email"] = "not-an-email"
        resp = api("post", "/user/register/", json=payload)
        assert resp.status_code == 422


# ────────────────────────────────────────────────────────────
#  登录接口测试
# ────────────────────────────────────────────────────────────

class TestUserLogin:

    @pytest.fixture(scope="class")
    def registered_user(self):
        """为本 class 注册一个测试账号"""
        payload = _new_user_payload()
        r = api("post", "/user/register/", json=payload)
        assert r.json()["code"] == 200
        return payload

    def test_login_success(self, registered_user):
        """正常登录：应返回 code=200 及用户信息"""
        resp = api("post", "/user/login/", json={
            "username": registered_user["username"],
            "password": registered_user["password"],
        })
        assert resp.status_code == 200
        body = resp.json()
        assert body["code"] == 200
        assert "登录成功" in body["msg"]

    def test_login_response_data_structure(self, registered_user):
        """登录成功响应 data 应包含 userId / username / nickname / email / phoneNumber"""
        body = api("post", "/user/login/", json={
            "username": registered_user["username"],
            "password": registered_user["password"],
        }).json()
        assert body["code"] == 200
        data = body["data"]
        for key in ("userId", "username", "nickname", "email", "phoneNumber"):
            assert key in data, f"响应 data 缺少字段: {key}"

    def test_login_user_id_is_positive_int(self, registered_user):
        """登录成功 userId 应是正整数"""
        body = api("post", "/user/login/", json={
            "username": registered_user["username"],
            "password": registered_user["password"],
        }).json()
        assert isinstance(body["data"]["userId"], int)
        assert body["data"]["userId"] > 0

    def test_login_wrong_password(self, registered_user):
        """密码错误：应返回 code=400"""
        resp = api("post", "/user/login/", json={
            "username": registered_user["username"],
            "password": "CHANGE_ME",
        })
        body = resp.json()
        assert body["code"] == 400
        assert "密码" in body["msg"] or "用户名" in body["msg"]

    def test_login_nonexistent_user(self):
        """不存在的用户名：应返回 code=400"""
        resp = api("post", "/user/login/", json={
            "username": "nonexistent_xyz_99999",
            "password": "CHANGE_ME",
        })
        body = resp.json()
        assert body["code"] == 400

    def test_login_missing_username_returns_422(self):
        """缺少 username 字段：应返回 HTTP 422"""
        resp = api("post", "/user/login/", json={"password": "CHANGE_ME"})
        assert resp.status_code == 422

    def test_login_empty_password_returns_error(self):
        """空密码字段：Pydantic min_length 未设时后端返回 400，设了则返回 422；均为拒绝访问"""
        resp = api("post", "/user/login/", json={"username": "someone", "password": ""})
        # 接受 422（Pydantic 校验）或 400/200+code=400（业务层校验）
        assert resp.status_code in (422, 400, 200), \
            f"空密码应被拒绝，实际 HTTP {resp.status_code}"
        if resp.status_code == 200:
            body = resp.json()
            assert body["code"] == 400, "空密码应在业务层返回 code=400"

    def test_login_username_matches_registered(self, registered_user):
        """登录返回的 username 应与注册时一致"""
        body = api("post", "/user/login/", json={
            "username": registered_user["username"],
            "password": registered_user["password"],
        }).json()
        assert body["data"]["username"] == registered_user["username"]

    def test_login_email_matches_registered(self, registered_user):
        """登录返回的 email 应与注册时一致"""
        body = api("post", "/user/login/", json={
            "username": registered_user["username"],
            "password": registered_user["password"],
        }).json()
        assert body["data"]["email"] == registered_user["email"]
