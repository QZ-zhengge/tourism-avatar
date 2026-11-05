"""
测试模块 04 — 点赞模块
覆盖业务逻辑：
  - 帖子点赞 Toggle：首次点赞 / 取消点赞 / 计数正确
  - 帖子点赞用户列表
  - 查询用户是否点赞（check 接口）
  - 评论点赞（简单增量）
  - 对不存在帖子点赞：code=404
"""
import pytest
from conftest import api, rand_str


def _create_post(user_id: int) -> int:
    """快速发帖，返回 post_id"""
    body = api("post", "/circle/posts/", json={
        "user_id":    user_id,
        "content":    f"点赞测试帖子 [{rand_str()}]",
        "visibility": 0,
    }).json()
    assert body["code"] == 200
    return body["data"]["post_id"]


def _create_comment(post_id: int, user_id: int) -> int:
    """快速发评论，返回 comment_id"""
    body = api("post", "/circle/comments/", json={
        "post_id": post_id,
        "user_id": user_id,
        "content": f"评论点赞测试 [{rand_str()}]",
    }).json()
    assert body["code"] == 200
    return body["data"]["id"]


# ────────────────────────────────────────────────────────────
#  帖子点赞 Toggle 测试
# ────────────────────────────────────────────────────────────

class TestTogglePostLike:

    @pytest.fixture
    def fresh_post(self, test_user):
        return _create_post(test_user["user_id"])

    def test_first_like_returns_is_liked_true(self, test_user, fresh_post):
        """首次点赞：is_liked 应为 True"""
        body = api("post", "/circle/likes/post", json={
            "post_id": fresh_post,
            "user_id": test_user["user_id"],
        }).json()
        assert body["code"] == 200
        assert body["is_liked"] is True

    def test_first_like_increments_count(self, test_user, fresh_post):
        """首次点赞：like_count 应从 0 变为 1"""
        before = api("get", f"/circle/posts/{fresh_post}").json()["data"]["like_count"]
        api("post", "/circle/likes/post", json={
            "post_id": fresh_post,
            "user_id": test_user["user_id"],
        })
        after = api("get", f"/circle/posts/{fresh_post}").json()["data"]["like_count"]
        assert after == before + 1

    def test_second_like_cancels_and_is_liked_false(self, test_user, fresh_post):
        """第二次点赞（已点赞状态）：应取消，is_liked 变为 False"""
        api("post", "/circle/likes/post", json={
            "post_id": fresh_post, "user_id": test_user["user_id"]})
        body = api("post", "/circle/likes/post", json={
            "post_id": fresh_post, "user_id": test_user["user_id"]}).json()
        assert body["code"] == 200
        assert body["is_liked"] is False

    def test_cancel_like_decrements_count(self, test_user, fresh_post):
        """取消点赞：like_count 应减 1"""
        api("post", "/circle/likes/post", json={
            "post_id": fresh_post, "user_id": test_user["user_id"]})
        count_after_like = api("get", f"/circle/posts/{fresh_post}").json()["data"]["like_count"]
        api("post", "/circle/likes/post", json={
            "post_id": fresh_post, "user_id": test_user["user_id"]})
        count_after_unlike = api("get", f"/circle/posts/{fresh_post}").json()["data"]["like_count"]
        assert count_after_unlike == count_after_like - 1

    def test_like_count_never_below_zero(self, test_user, fresh_post):
        """多次取消点赞：like_count 不应小于 0"""
        # 连续取消两次
        api("post", "/circle/likes/post", json={
            "post_id": fresh_post, "user_id": test_user["user_id"]})
        api("post", "/circle/likes/post", json={
            "post_id": fresh_post, "user_id": test_user["user_id"]})
        api("post", "/circle/likes/post", json={
            "post_id": fresh_post, "user_id": test_user["user_id"]})
        detail = api("get", f"/circle/posts/{fresh_post}").json()
        assert detail["data"]["like_count"] >= 0

    def test_like_nonexistent_post_returns_404(self, test_user):
        """对不存在的帖子点赞：应返回 code=404"""
        body = api("post", "/circle/likes/post", json={
            "post_id": 9999999,
            "user_id": test_user["user_id"],
        }).json()
        assert body["code"] == 404

    def test_like_response_structure(self, test_user, fresh_post):
        """点赞响应必须包含 code / msg / is_liked / like_count"""
        body = api("post", "/circle/likes/post", json={
            "post_id": fresh_post, "user_id": test_user["user_id"]}).json()
        for field in ("code", "msg", "is_liked", "like_count"):
            assert field in body, f"点赞响应缺少字段: {field}"

    def test_multiple_users_like_same_post(self, test_user, fresh_post):
        """多个不同用户对同一帖子点赞：like_count 应正确累加"""
        # 使用两个不同的 user_id
        uid1 = test_user["user_id"]
        uid2 = test_user["user_id"] + 1

        before = api("get", f"/circle/posts/{fresh_post}").json()["data"]["like_count"]
        api("post", "/circle/likes/post", json={"post_id": fresh_post, "user_id": uid1})
        api("post", "/circle/likes/post", json={"post_id": fresh_post, "user_id": uid2})
        after = api("get", f"/circle/posts/{fresh_post}").json()["data"]["like_count"]
        assert after == before + 2


# ────────────────────────────────────────────────────────────
#  帖子点赞列表测试
# ────────────────────────────────────────────────────────────

class TestGetPostLikes:

    @pytest.fixture
    def liked_post(self, test_user):
        """创建帖子并点一次赞"""
        pid = _create_post(test_user["user_id"])
        api("post", "/circle/likes/post", json={
            "post_id": pid, "user_id": test_user["user_id"]})
        return pid

    def test_get_likes_list_returns_200(self, liked_post):
        """获取点赞列表：应返回 code=200"""
        body = api("get", f"/circle/likes/post/{liked_post}").json()
        assert body["code"] == 200

    def test_get_likes_list_structure(self, liked_post):
        """点赞列表响应应包含 data / total"""
        body = api("get", f"/circle/likes/post/{liked_post}").json()
        assert "data" in body
        assert "total" in body

    def test_get_likes_list_total_matches(self, liked_post, test_user):
        """点赞列表 total 应与帖子的 like_count 一致"""
        body = api("get", f"/circle/likes/post/{liked_post}").json()
        post_detail = api("get", f"/circle/posts/{liked_post}").json()
        assert body["total"] == post_detail["data"]["like_count"]

    def test_get_likes_for_nonexistent_post(self):
        """获取不存在帖子的点赞列表：应返回 code=404"""
        body = api("get", "/circle/likes/post/9999999").json()
        assert body["code"] == 404

    def test_get_likes_list_item_has_user_id(self, liked_post):
        """点赞列表每项应包含 user_id 和 like_time"""
        body = api("get", f"/circle/likes/post/{liked_post}").json()
        if body["data"]:
            item = body["data"][0]
            assert "user_id" in item
            assert "like_time" in item


# ────────────────────────────────────────────────────────────
#  查询点赞状态（check 接口）
# ────────────────────────────────────────────────────────────

class TestCheckLikeStatus:

    @pytest.fixture
    def fresh_post(self, test_user):
        return _create_post(test_user["user_id"])

    def test_check_not_liked_initially(self, test_user, fresh_post):
        """发帖后默认未点赞：is_liked 应为 False"""
        body = api("get", "/circle/likes/check",
                   params={"post_id": fresh_post,
                           "user_id": test_user["user_id"]}).json()
        assert body["code"] == 200
        assert body["is_liked"] is False

    def test_check_liked_after_like(self, test_user, fresh_post):
        """点赞后查询：is_liked 应为 True"""
        api("post", "/circle/likes/post", json={
            "post_id": fresh_post, "user_id": test_user["user_id"]})
        body = api("get", "/circle/likes/check",
                   params={"post_id": fresh_post,
                           "user_id": test_user["user_id"]}).json()
        assert body["is_liked"] is True

    def test_check_not_liked_after_cancel(self, test_user, fresh_post):
        """点赞后再取消，查询：is_liked 应变回 False"""
        api("post", "/circle/likes/post", json={
            "post_id": fresh_post, "user_id": test_user["user_id"]})
        api("post", "/circle/likes/post", json={
            "post_id": fresh_post, "user_id": test_user["user_id"]})
        body = api("get", "/circle/likes/check",
                   params={"post_id": fresh_post,
                           "user_id": test_user["user_id"]}).json()
        assert body["is_liked"] is False

    def test_check_missing_params_returns_422(self):
        """缺少必填参数：应返回 HTTP 422"""
        resp = api("get", "/circle/likes/check", params={"post_id": 1})
        assert resp.status_code == 422

    def test_is_liked_in_post_detail(self, test_user, fresh_post):
        """点赞后帖子详情中的 is_liked 字段也应为 True"""
        api("post", "/circle/likes/post", json={
            "post_id": fresh_post, "user_id": test_user["user_id"]})
        detail = api("get", f"/circle/posts/{fresh_post}",
                     params={"current_user_id": test_user["user_id"]}).json()
        assert detail["data"]["is_liked"] is True


# ────────────────────────────────────────────────────────────
#  评论点赞测试
# ────────────────────────────────────────────────────────────

class TestCommentLike:

    @pytest.fixture
    def comment_id(self, test_user, test_post):
        return _create_comment(test_post, test_user["user_id"])

    def test_like_comment_returns_200(self, test_user, comment_id):
        """评论点赞：应返回 code=200"""
        body = api("post", "/circle/likes/comment",
                   params={"comment_id": comment_id,
                           "user_id": test_user["user_id"]}).json()
        assert body["code"] == 200

    def test_like_comment_increments_count(self, test_user, comment_id):
        """评论点赞后 like_count 应增加"""
        body = api("post", "/circle/likes/comment",
                   params={"comment_id": comment_id,
                           "user_id": test_user["user_id"]}).json()
        assert "like_count" in body
        assert body["like_count"] >= 1

    def test_like_nonexistent_comment(self, test_user):
        """对不存在评论点赞：应返回 code=404"""
        body = api("post", "/circle/likes/comment",
                   params={"comment_id": 9999999,
                           "user_id": test_user["user_id"]}).json()
        assert body["code"] == 404

    def test_like_comment_missing_params_returns_422(self):
        """缺少必填参数：应返回 HTTP 422"""
        resp = api("post", "/circle/likes/comment",
                   params={"comment_id": 1})
        assert resp.status_code == 422
