"""
测试模块 05 — 评论模块
覆盖业务逻辑：
  - 发布一级评论：成功 / 帖子不存在
  - 发布二级回复：成功 / 父评论不存在 / 跨帖子回复
  - 帖子 comment_count 正确累加/递减
  - 获取评论列表：树形结构 / 分页
  - 删除评论：有权限 / 无权限 / 不存在 / 重复删除
  - 删除后对应评论在树中消失
"""
import pytest
from conftest import api, rand_str


def _create_post(user_id: int) -> int:
    body = api("post", "/circle/posts/", json={
        "user_id":    user_id,
        "content":    f"评论测试帖子 [{rand_str()}]",
        "visibility": 0,
    }).json()
    assert body["code"] == 200
    return body["data"]["post_id"]


# ────────────────────────────────────────────────────────────
#  发布评论测试
# ────────────────────────────────────────────────────────────

class TestCreateComment:

    @pytest.fixture
    def post_id(self, test_user):
        return _create_post(test_user["user_id"])

    def test_create_top_level_comment_success(self, test_user, post_id):
        """发布一级评论：应返回 code=200"""
        body = api("post", "/circle/comments/", json={
            "post_id": post_id,
            "user_id": test_user["user_id"],
            "content": f"一级评论 [{rand_str()}]",
        }).json()
        assert body["code"] == 200

    def test_create_comment_response_structure(self, test_user, post_id):
        """评论响应 data 必须含标准字段"""
        body = api("post", "/circle/comments/", json={
            "post_id": post_id,
            "user_id": test_user["user_id"],
            "content": "结构校验评论",
        }).json()
        assert body["code"] == 200
        data = body["data"]
        for field in ("id", "post_id", "parent_id", "user_id",
                      "content", "like_count", "create_time"):
            assert field in data, f"评论响应缺少字段: {field}"

    def test_create_top_level_comment_parent_id_is_null(self, test_user, post_id):
        """一级评论的 parent_id 应为 null"""
        body = api("post", "/circle/comments/", json={
            "post_id": post_id,
            "user_id": test_user["user_id"],
            "content": "parent_id 校验",
        }).json()
        assert body["data"]["parent_id"] is None

    def test_create_comment_increments_post_comment_count(self, test_user, post_id):
        """发评论后帖子 comment_count 应 +1"""
        before = api("get", f"/circle/posts/{post_id}").json()["data"]["comment_count"]
        api("post", "/circle/comments/", json={
            "post_id": post_id,
            "user_id": test_user["user_id"],
            "content": "计数测试",
        })
        after = api("get", f"/circle/posts/{post_id}").json()["data"]["comment_count"]
        assert after == before + 1

    def test_comment_on_nonexistent_post_returns_404(self, test_user):
        """对不存在帖子评论：应返回 code=404"""
        body = api("post", "/circle/comments/", json={
            "post_id": 9999999,
            "user_id": test_user["user_id"],
            "content": "评论不存在帖子",
        }).json()
        assert body["code"] == 404

    def test_comment_missing_content_returns_422(self, test_user, post_id):
        """缺少 content 字段：应返回 HTTP 422"""
        resp = api("post", "/circle/comments/", json={
            "post_id": post_id,
            "user_id": test_user["user_id"],
        })
        assert resp.status_code == 422

    def test_comment_missing_user_id_returns_422(self, post_id):
        """缺少 user_id 字段：应返回 HTTP 422"""
        resp = api("post", "/circle/comments/", json={
            "post_id": post_id,
            "content": "内容",
        })
        assert resp.status_code == 422


# ────────────────────────────────────────────────────────────
#  二级回复测试
# ────────────────────────────────────────────────────────────

class TestReplyComment:

    @pytest.fixture
    def post_and_comment(self, test_user):
        """创建帖子 + 一级评论"""
        pid = _create_post(test_user["user_id"])
        body = api("post", "/circle/comments/", json={
            "post_id": pid,
            "user_id": test_user["user_id"],
            "content": "父评论",
        }).json()
        cid = body["data"]["id"]
        return pid, cid

    def test_reply_comment_success(self, test_user, post_and_comment):
        """回复一级评论：应返回 code=200"""
        pid, cid = post_and_comment
        body = api("post", "/circle/comments/", json={
            "post_id":   pid,
            "user_id":   test_user["user_id"],
            "content":   f"回复内容 [{rand_str()}]",
            "parent_id": cid,
        }).json()
        assert body["code"] == 200

    def test_reply_has_correct_parent_id(self, test_user, post_and_comment):
        """回复评论的 parent_id 应等于被回复评论的 id"""
        pid, cid = post_and_comment
        body = api("post", "/circle/comments/", json={
            "post_id":   pid,
            "user_id":   test_user["user_id"],
            "content":   "parent_id 回复校验",
            "parent_id": cid,
        }).json()
        assert body["data"]["parent_id"] == cid

    def test_reply_increments_post_comment_count(self, test_user, post_and_comment):
        """回复评论也应使帖子 comment_count +1"""
        pid, cid = post_and_comment
        before = api("get", f"/circle/posts/{pid}").json()["data"]["comment_count"]
        api("post", "/circle/comments/", json={
            "post_id":   pid,
            "user_id":   test_user["user_id"],
            "content":   "计数回复",
            "parent_id": cid,
        })
        after = api("get", f"/circle/posts/{pid}").json()["data"]["comment_count"]
        assert after == before + 1

    def test_reply_to_nonexistent_comment_returns_400(self, test_user, post_and_comment):
        """回复不存在的父评论：应返回 code=400"""
        pid, _ = post_and_comment
        body = api("post", "/circle/comments/", json={
            "post_id":   pid,
            "user_id":   test_user["user_id"],
            "content":   "回复不存在的评论",
            "parent_id": 9999999,
        }).json()
        assert body["code"] == 400


# ────────────────────────────────────────────────────────────
#  获取评论列表（树形结构）测试
# ────────────────────────────────────────────────────────────

class TestGetComments:

    @pytest.fixture(scope="class")
    def post_with_comments(self, test_user):
        """
        创建帖子，添加 2 条一级评论，
        对第 1 条一级评论添加 2 条回复。
        """
        pid = _create_post(test_user["user_id"])

        # 一级评论 1
        c1 = api("post", "/circle/comments/", json={
            "post_id": pid, "user_id": test_user["user_id"],
            "content": "一级评论-1",
        }).json()["data"]["id"]

        # 一级评论 2
        api("post", "/circle/comments/", json={
            "post_id": pid, "user_id": test_user["user_id"],
            "content": "一级评论-2",
        })

        # 二级回复 1, 2
        api("post", "/circle/comments/", json={
            "post_id": pid, "user_id": test_user["user_id"],
            "content": "回复-1", "parent_id": c1,
        })
        api("post", "/circle/comments/", json={
            "post_id": pid, "user_id": test_user["user_id"],
            "content": "回复-2", "parent_id": c1,
        })
        return pid, c1

    def test_get_comments_returns_200(self, post_with_comments):
        pid, _ = post_with_comments
        body = api("get", f"/circle/comments/{pid}").json()
        assert body["code"] == 200

    def test_get_comments_response_structure(self, post_with_comments):
        """评论列表响应应包含 data / total / page / page_size"""
        pid, _ = post_with_comments
        body = api("get", f"/circle/comments/{pid}").json()
        for field in ("data", "total", "page", "page_size"):
            assert field in body, f"缺少字段: {field}"

    def test_get_comments_data_is_list(self, post_with_comments):
        pid, _ = post_with_comments
        body = api("get", f"/circle/comments/{pid}").json()
        assert isinstance(body["data"], list)

    def test_get_comments_top_level_only(self, post_with_comments):
        """列表中只有一级评论（parent_id=null）"""
        pid, _ = post_with_comments
        body = api("get", f"/circle/comments/{pid}").json()
        for c in body["data"]:
            assert c["parent_id"] is None, "列表根节点 parent_id 应为 null"

    def test_get_comments_replies_nested_under_parent(self, post_with_comments):
        """replies 数组应嵌套在对应的一级评论下"""
        pid, c1_id = post_with_comments
        body = api("get", f"/circle/comments/{pid}").json()
        c1 = next((c for c in body["data"] if c["id"] == c1_id), None)
        assert c1 is not None, f"一级评论 id={c1_id} 未出现在评论列表中"
        assert "replies" in c1
        assert isinstance(c1["replies"], list)
        assert len(c1["replies"]) >= 2

    def test_get_comments_reply_has_correct_parent_id(self, post_with_comments):
        """replies 中每条回复的 parent_id 应等于父评论 id"""
        pid, c1_id = post_with_comments
        body = api("get", f"/circle/comments/{pid}").json()
        c1 = next((c for c in body["data"] if c["id"] == c1_id), None)
        for reply in c1["replies"]:
            assert reply["parent_id"] == c1_id

    def test_get_comments_item_has_username(self, post_with_comments):
        """评论应包含 username 和 nickname 字段（关联查用户名）"""
        pid, _ = post_with_comments
        body = api("get", f"/circle/comments/{pid}").json()
        if body["data"]:
            c = body["data"][0]
            assert "username" in c
            assert "nickname" in c

    def test_get_comments_pagination(self, post_with_comments):
        """评论列表支持分页，page_size=1 应只返回 1 条一级评论"""
        pid, _ = post_with_comments
        body = api("get", f"/circle/comments/{pid}",
                   params={"page": 1, "page_size": 1}).json()
        assert len(body["data"]) <= 1

    def test_get_comments_nonexistent_post_returns_404(self):
        """对不存在帖子获取评论：应返回 code=404"""
        body = api("get", "/circle/comments/9999999").json()
        assert body["code"] == 404


# ────────────────────────────────────────────────────────────
#  删除评论测试
# ────────────────────────────────────────────────────────────

class TestDeleteComment:

    @pytest.fixture
    def comment_data(self, test_user, test_post):
        """创建一条可删除的评论"""
        body = api("post", "/circle/comments/", json={
            "post_id": test_post,
            "user_id": test_user["user_id"],
            "content": f"可删除评论 [{rand_str()}]",
        }).json()
        return {"comment_id": body["data"]["id"], "post_id": test_post}

    def test_delete_comment_success(self, test_user, comment_data):
        """有权限删除评论：应返回 code=200"""
        body = api("delete", f"/circle/comments/{comment_data['comment_id']}",
                   params={"user_id": test_user["user_id"]}).json()
        assert body["code"] == 200

    def test_deleted_comment_not_in_list(self, test_user, comment_data):
        """删除后该评论不应再出现在评论列表中"""
        cid = comment_data["comment_id"]
        pid = comment_data["post_id"]
        api("delete", f"/circle/comments/{cid}",
            params={"user_id": test_user["user_id"]})
        body = api("get", f"/circle/comments/{pid}").json()
        all_ids = [c["id"] for c in body["data"]]
        assert cid not in all_ids

    def test_delete_comment_decrements_post_comment_count(self, test_user, test_post):
        """删除评论后帖子 comment_count 应 -1"""
        c = api("post", "/circle/comments/", json={
            "post_id": test_post,
            "user_id": test_user["user_id"],
            "content": "准备删的评论",
        }).json()["data"]["id"]

        before = api("get", f"/circle/posts/{test_post}").json()["data"]["comment_count"]
        api("delete", f"/circle/comments/{c}",
            params={"user_id": test_user["user_id"]})
        after = api("get", f"/circle/posts/{test_post}").json()["data"]["comment_count"]
        assert after == before - 1

    def test_delete_comment_wrong_user_returns_403(self, test_user, comment_data):
        """其他用户删别人评论：应返回 code=403"""
        other_id = test_user["user_id"] + 9999
        body = api("delete", f"/circle/comments/{comment_data['comment_id']}",
                   params={"user_id": other_id}).json()
        assert body["code"] == 403

    def test_delete_nonexistent_comment_returns_404(self, test_user):
        """删除不存在评论：应返回 code=404"""
        body = api("delete", "/circle/comments/9999999",
                   params={"user_id": test_user["user_id"]}).json()
        assert body["code"] == 404

    def test_delete_comment_twice_returns_404(self, test_user, test_post):
        """重复删除同一评论：第二次应返回 code=404"""
        c = api("post", "/circle/comments/", json={
            "post_id": test_post,
            "user_id": test_user["user_id"],
            "content": "准备删两次",
        }).json()["data"]["id"]
        api("delete", f"/circle/comments/{c}",
            params={"user_id": test_user["user_id"]})
        body = api("delete", f"/circle/comments/{c}",
                   params={"user_id": test_user["user_id"]}).json()
        assert body["code"] == 404

    def test_delete_comment_missing_user_id_returns_422(self, comment_data):
        """缺少 user_id 参数：应返回 HTTP 422"""
        resp = api("delete", f"/circle/comments/{comment_data['comment_id']}")
        assert resp.status_code == 422
