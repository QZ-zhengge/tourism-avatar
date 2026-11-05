"""
游客端（主项目）：个人中心 - 建议信箱 / 我的点赞 / 我的评论 端到端测试
====================================================================
测试内容：
  1. 建表与迁移（user_suggestions 增量加列 user_id/contact，幂等）
  2. 建议信箱：游客端提交建议 + 查询「我的建议」列表
  3. 我的点赞：点赞帖子 + 查询「我点赞过的帖子」列表
  4. 我的评论：发评论 + 查询「我发过的评论」列表
  5. 连通管理端：独立进程运行管理端建议信箱读取测试，断言能读到游客端写入的数据
  6. 清理测试数据（不污染生产库）

运行：
  Z:\\SoftwareCupAI\\.venv\\Scripts\\python.exe test\\test_11_personal_center.py
"""
import os
import sys
import subprocess
import time

MAIN_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, MAIN_ROOT)

# ── 0. 基础导入（触发建表 + 迁移） ─────────────────────────────────
from database.database import SessionLocal, ensure_tables_exist  # noqa: E402
ensure_tables_exist()

from fastapi.testclient import TestClient  # noqa: E402
import main  # noqa: E402
from models.post import Post, PostLike, PostComment  # noqa: E402
from models.suggestion import UserSuggestion  # noqa: E402
from models.user import User  # noqa: E402

client = TestClient(main.app)

TEST_USERNAME = f"pctest_{int(time.time())}"
TEST_PHONE = f"139{int(time.time()) % 100_000_000:08d}"  # 11 位唯一手机号
TEST_PASSWORD = "CHANGE_ME"


def _cleanup(db, user_id, post_id, suggestion_id):
    # 建议（无外键，按 id 删）
    if suggestion_id:
        db.query(UserSuggestion).filter(UserSuggestion.id == suggestion_id).delete()
    # 帖子（级联删点赞/评论）
    if post_id:
        db.query(Post).filter(Post.id == post_id).delete()
    # 用户
    if user_id:
        db.query(User).filter(User.id == user_id).delete()
    db.commit()


def test_suggestion():
    # 注册用户拿 user_id
    r = client.post("/user/register/", json={
        "username": TEST_USERNAME, "password": TEST_PASSWORD, "confirm_password": TEST_PASSWORD,
        "email": f"{TEST_USERNAME}@test.com", "phone_number": TEST_PHONE,
    })
    assert r.status_code == 200, f"注册失败：{r.text}"

    # 注册响应不返回 userId，登录后从 data.userId 取
    r = client.post("/user/login/", json={
        "username": TEST_USERNAME, "password": TEST_PASSWORD,
    })
    assert r.status_code == 200, f"登录失败：{r.text}"
    user_id = r.json()["data"]["userId"]

    # 游客端提交建议
    r = client.post("/suggestions/", json={
        "user_id": user_id,
        "content": "希望增加离线地图功能",
        "contact": "138xxxx8888",
    })
    assert r.status_code == 200, f"提交建议失败：{r.text}"
    sg = r.json()["data"]
    assert sg["user_id"] == user_id
    suggestion_id = sg["id"]
    print(f"[main] 建议提交 OK：id={suggestion_id} user_id={user_id}")

    # 我的建议列表
    r = client.get(f"/suggestions/user/{user_id}")
    assert r.status_code == 200, f"我的建议列表失败：{r.text}"
    body = r.json()
    assert body["total"] >= 1, f"我的建议列表为空：{body}"
    ids = [i["id"] for i in body["data"]]
    assert suggestion_id in ids, f"提交的建议未出现在我的列表：{ids}"
    item = next(i for i in body["data"] if i["id"] == suggestion_id)
    assert item["content"] == "希望增加离线地图功能"
    assert item["contact"] == "138xxxx8888"
    assert item["status"] == "unread"
    print(f"[main] 我的建议列表 OK：total={body['total']} 命中 id={suggestion_id}")

    return user_id, suggestion_id


def test_my_likes_and_comments(user_id):
    # 发帖子
    r = client.post("/circle/posts/", json={
        "user_id": user_id,
        "content": "测试帖：今天去爬山了",
        "location_name": "灵山",
        "visibility": 0,
    })
    assert r.status_code == 200, f"发帖失败：{r.text}"
    post_id = r.json()["data"]["post_id"]
    print(f"[main] 发帖 OK：post_id={post_id}")

    # 点赞
    r = client.post("/circle/likes/post", json={"user_id": user_id, "post_id": post_id})
    assert r.status_code == 200, f"点赞失败：{r.text}"
    assert r.json()["is_liked"] is True, f"点赞状态异常：{r.text}"

    # 我的点赞列表
    r = client.get(f"/circle/likes/user/{user_id}")
    assert r.status_code == 200, f"我的点赞失败：{r.text}"
    body = r.json()
    post_ids = [d["post_id"] for d in body["data"]]
    assert post_id in post_ids, f"点赞的帖子未出现在我的点赞：{post_ids}"
    liked = next(d for d in body["data"] if d["post_id"] == post_id)
    assert liked["post"]["id"] == post_id
    assert "images" in liked["post"]
    print(f"[main] 我的点赞 OK：total={body['total']} 命中 post_id={post_id}")

    # 发评论
    r = client.post("/circle/comments/", json={
        "user_id": user_id, "post_id": post_id, "content": "风景真不错！",
    })
    assert r.status_code == 200, f"发评论失败：{r.text}"
    comment_id = r.json()["data"]["id"]

    # 我的评论列表
    r = client.get(f"/circle/comments/user/{user_id}")
    assert r.status_code == 200, f"我的评论失败：{r.text}"
    body = r.json()
    cids = [d["comment_id"] for d in body["data"]]
    assert comment_id in cids, f"发的评论未出现在我的评论：{cids}"
    cm = next(d for d in body["data"] if d["comment_id"] == comment_id)
    assert cm["content"] == "风景真不错！"
    assert cm["post"]["id"] == post_id
    assert cm["parent_id"] is None
    print(f"[main] 我的评论 OK：total={body['total']} 命中 comment_id={comment_id}")

    return post_id


def test_admin_read(suggestion_id, user_id):
    """独立进程运行管理端建议信箱读取测试，断言能读到游客端写入的数据"""
    env = dict(os.environ)
    env["TEST_SUGGESTION_ID"] = suggestion_id
    env["TEST_USER_ID"] = str(user_id)

    from config import get_settings as _main_settings
    _ms = _main_settings()
    env["DB_HOST"] = _ms.DB_HOST
    env["DB_PORT"] = str(_ms.DB_PORT)
    env["DB_USER"] = _ms.DB_USER
    env["DB_PASSWORD"] = _ms.DB_PASSWORD
    env["DB_NAME"] = _ms.DB_NAME

    admin_test = os.path.join(MAIN_ROOT, "..", "SoftwareCupAI_admin", "test", "test_suggestion_read.py")
    admin_test = os.path.abspath(admin_test)
    py = os.path.join(MAIN_ROOT, ".venv", "Scripts", "python.exe")
    if not os.path.exists(py):
        py = sys.executable

    print(f"[main] 调用管理端建议信箱读取测试：{admin_test}")
    proc = subprocess.run(
        [py, admin_test],
        env=env,
        cwd=os.path.dirname(admin_test),
        capture_output=True,
        text=True,
    )
    print(proc.stdout)
    if proc.returncode != 0:
        print("=== admin 读取测试失败 stderr ===")
        print(proc.stderr)
        raise AssertionError(f"管理端读取测试失败，returncode={proc.returncode}")


def main_run():
    user_id = None
    post_id = None
    suggestion_id = None
    try:
        user_id, suggestion_id = test_suggestion()
        post_id = test_my_likes_and_comments(user_id)
        test_admin_read(suggestion_id, user_id)
    finally:
        db = SessionLocal()
        try:
            _cleanup(db, user_id, post_id, suggestion_id)
            print("[main] 测试数据已清理")
        finally:
            db.close()
    print("\nALL_OK ✅")


if __name__ == "__main__":
    main_run()
