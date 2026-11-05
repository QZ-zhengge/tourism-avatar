"""
pytest 公共配置 & Fixtures
全部测试均通过 HTTP 请求调用运行中的后端，
与原始业务代码完全解耦（无任何 import）。

依赖：
    pip install pytest requests pytest-order
运行：
    cd test/
    pytest -v --tb=short
"""
import random
import string
import time
import pytest
import requests

# ────────────────────────────────────────────────────────────
#  全局配置
# ────────────────────────────────────────────────────────────
BASE_URL = "http://127.0.0.1:8000"
TIMEOUT  = 15   # 单请求超时秒数


def api(method: str, path: str, **kwargs) -> requests.Response:
    """统一请求入口，自动加 BASE_URL + timeout"""
    url = BASE_URL + path
    kwargs.setdefault("timeout", TIMEOUT)
    return getattr(requests, method.lower())(url, **kwargs)


def rand_str(n: int = 8) -> str:
    """生成随机字符串，用于构造唯一测试数据"""
    return "".join(random.choices(string.ascii_lowercase + string.digits, k=n))


# ────────────────────────────────────────────────────────────
#  Session-scoped Fixtures（整个测试 session 只创建一次）
# ────────────────────────────────────────────────────────────

@pytest.fixture(scope="session")
def base_url():
    return BASE_URL


@pytest.fixture(scope="session", autouse=True)
def check_server():
    """在所有测试开始前确认后端服务已启动"""
    try:
        resp = requests.get(BASE_URL + "/health", timeout=5)
        assert resp.status_code == 200, f"服务响应异常: {resp.status_code}"
    except requests.exceptions.ConnectionError:
        pytest.exit(
            f"⛔ 无法连接到后端服务 {BASE_URL}\n"
            "请先启动后端：\n"
            "  z:/SoftwareCupAI/.venv/Scripts/python.exe main.py",
            returncode=1,
        )


@pytest.fixture(scope="session")
def test_user():
    """
    创建一个测试用户并返回其信息。
    整个测试 session 共享同一用户，避免重复注册。
    """
    suffix   = rand_str(8)
    username = f"testuser_{suffix}"
    password = "CHANGE_ME"
    email    = f"test_{suffix}@example.com"
    phone    = f"1{random.randint(3000000000, 9999999999)}"

    resp = api("post", "/user/register/", json={
        "username":         username,
        "password":         password,
        "confirm_password": password,
        "email":            email,
        "phone_number":     phone,
    })
    assert resp.status_code == 200
    body = resp.json()
    assert body["code"] == 200, f"注册失败: {body}"

    # 登录获取 user_id
    login_resp = api("post", "/user/login/", json={
        "username": username,
        "password": password,
    })
    login_body = login_resp.json()
    assert login_body["code"] == 200, f"登录失败: {login_body}"

    return {
        "username": username,
        "password": password,
        "email":    email,
        "phone":    phone,
        "user_id":  login_body["data"]["userId"],
    }


@pytest.fixture(scope="session")
def test_post(test_user):
    """
    创建一条测试帖子并返回其 post_id。
    整个测试 session 共享。
    """
    resp = api("post", "/circle/posts/", json={
        "user_id":       test_user["user_id"],
        "content":       f"Session 级测试帖子 [{rand_str()}]",
        "location_name": "测试地点",
        "latitude":      "31.2304",
        "longitude":     "121.4737",
        "visibility":    0,
    })
    assert resp.status_code == 200
    body = resp.json()
    assert body["code"] == 200, f"创建帖子失败: {body}"
    return body["data"]["post_id"]
