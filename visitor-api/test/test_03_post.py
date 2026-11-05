"""
测试模块 03 — 帖子模块（驴友圈）
覆盖业务逻辑：
  - 发帖：成功 / 内容为空 / 字段缺失
  - 帖子广场列表：分页 / 公开帖子可见
  - 用户帖子列表：仅返回指定用户帖子
  - 帖子详情：存在 / 不存在
  - 编辑帖子：有权限 / 无权限 / 不存在
  - 绑定媒体：图片 / 视频
  - 删除帖子：有权限 / 无权限 / 不存在 / 重复删除
  - 帖子可见性：公开(0) / 仅好友(1) / 仅自己(2)
"""
import pytest
from conftest import api, rand_str


# ────────────────────────────────────────────────────────────
#  辅助函数
# ────────────────────────────────────────────────────────────

def _create_post(user_id: int, content: str = None,
                 visibility: int = 0, location: str = None) -> dict:
    """发帖辅助函数，返回响应 body"""
    payload = {
        "user_id":    user_id,
        "content":    content or f"测试帖子内容 [{rand_str()}]",
        "visibility": visibility,
    }
    if location:
        payload["location_name"] = location
        payload["latitude"]  = "31.2304"
        payload["longitude"] = "121.4737"
    return api("post", "/circle/posts/", json=payload).json()


# ────────────────────────────────────────────────────────────
#  发帖测试
# ────────────────────────────────────────────────────────────

class TestCreatePost:

    def test_create_post_success(self, test_user):
        """正常发帖：应返回 code=200 及 post_id"""
        body = _create_post(test_user["user_id"])
        assert body["code"] == 200
        assert "post_id" in body.get("data", {})
        assert isinstance(body["data"]["post_id"], int)

    def test_create_post_response_structure(self, test_user):
        """发帖响应必须含 code / msg / data.post_id"""
        body = _create_post(test_user["user_id"])
        assert "code" in body
        assert "msg"  in body
        assert "data" in body
        assert "post_id" in body["data"]

    def test_create_post_with_location(self, test_user):
        """带位置信息发帖：应成功"""
        body = _create_post(test_user["user_id"], location="黄山风景区")
        assert body["code"] == 200

    def test_create_post_empty_content_returns_400(self, test_user):
        """内容和 location 都为空：应返回 code=400"""
        resp = api("post", "/circle/posts/", json={
            "user_id": test_user["user_id"],
            "content": "",
        })
        body = resp.json()
        assert body["code"] == 400

    def test_create_post_missing_user_id_returns_422(self):
        """缺少 user_id：应返回 HTTP 422"""
        resp = api("post", "/circle/posts/", json={"content": "hello"})
        assert resp.status_code == 422

    def test_create_post_visibility_public(self, test_user):
        """visibility=0 (公开) 发帖应成功"""
        body = _create_post(test_user["user_id"], visibility=0)
        assert body["code"] == 200

    def test_create_post_visibility_friends_only(self, test_user):
        """visibility=1 (仅好友) 发帖应成功"""
        body = _create_post(test_user["user_id"], visibility=1)
        assert body["code"] == 200

    def test_create_post_visibility_private(self, test_user):
        """visibility=2 (仅自己) 发帖应成功"""
        body = _create_post(test_user["user_id"], visibility=2)
        assert body["code"] == 200


# ────────────────────────────────────────────────────────────
#  帖子广场列表测试
# ────────────────────────────────────────────────────────────

class TestPostList:

    def test_get_post_list_returns_200(self):
        """获取帖子广场：应返回 code=200"""
        body = api("get", "/circle/posts/").json()
        assert body["code"] == 200

    def test_get_post_list_has_required_fields(self):
        """广场列表响应应包含 data / total / page / page_size"""
        body = api("get", "/circle/posts/").json()
        for field in ("data", "total", "page", "page_size"):
            assert field in body, f"缺少字段: {field}"

    def test_get_post_list_data_is_list(self):
        """广场列表 data 应为数组"""
        body = api("get", "/circle/posts/").json()
        assert isinstance(body["data"], list)

    def test_get_post_list_pagination_page1(self):
        """第一页 page_size=5 返回条数不超过 5"""
        body = api("get", "/circle/posts/", params={"page": 1, "page_size": 5}).json()
        assert len(body["data"]) <= 5

    def test_get_post_list_pagination_page_field(self):
        """分页：返回的 page 字段应等于请求的页码"""
        body = api("get", "/circle/posts/", params={"page": 2, "page_size": 3}).json()
        assert body["page"] == 2

    def test_get_post_list_only_public_visible(self, test_user):
        """广场列表只应包含 visibility=0 的公开帖子"""
        # 发一个私密帖
        priv = _create_post(test_user["user_id"], visibility=2)
        priv_id = priv["data"]["post_id"]

        # 从广场全量获取（取较大 page_size）
        body = api("get", "/circle/posts/", params={"page_size": 50}).json()
        ids = [p["id"] for p in body["data"]]
        assert priv_id not in ids, "私密帖子不应出现在广场列表中"

    def test_get_post_list_item_structure(self, test_user):
        """广场列表每条帖子应包含标准字段"""
        _create_post(test_user["user_id"])  # 确保至少有一条公开帖子
        body = api("get", "/circle/posts/", params={"page_size": 5}).json()
        if body["data"]:
            post = body["data"][0]
            for field in ("id", "user_id", "content", "like_count",
                          "comment_count", "is_liked", "images", "videos"):
                assert field in post, f"帖子缺少字段: {field}"


# ────────────────────────────────────────────────────────────
#  用户帖子列表测试
# ────────────────────────────────────────────────────────────

class TestUserPostList:

    def test_get_user_posts_success(self, test_user):
        """获取指定用户帖子列表：应返回 code=200"""
        body = api("get", f"/circle/posts/user/{test_user['user_id']}").json()
        assert body["code"] == 200

    def test_get_user_posts_all_belong_to_user(self, test_user):
        """用户帖子列表中每条帖子的 user_id 应等于请求的用户ID"""
        body = api("get", f"/circle/posts/user/{test_user['user_id']}",
                   params={"page_size": 50}).json()
        for post in body["data"]:
            assert post["user_id"] == test_user["user_id"]

    def test_get_user_posts_excludes_private(self, test_user):
        """用户帖子列表不应返回 visibility=2 的私密帖子（仅自己可见）"""
        priv = _create_post(test_user["user_id"], visibility=2)
        priv_id = priv["data"]["post_id"]
        body = api("get", f"/circle/posts/user/{test_user['user_id']}",
                   params={"page_size": 50}).json()
        ids = [p["id"] for p in body["data"]]
        # 按业务规则：visibility != 2 才在用户列表可见
        assert priv_id not in ids, "私密帖子(visibility=2)不应出现在用户帖子列表中"

    def test_get_nonexistent_user_posts(self):
        """查询不存在用户的帖子：应返回空列表，total=0"""
        body = api("get", "/circle/posts/user/999999").json()
        assert body["code"] == 200
        assert body["data"] == []
        assert body["total"] == 0


# ────────────────────────────────────────────────────────────
#  帖子详情测试
# ────────────────────────────────────────────────────────────

class TestPostDetail:

    def test_get_post_detail_success(self, test_user, test_post):
        """获取已发帖子详情：应返回 code=200 及完整数据"""
        body = api("get", f"/circle/posts/{test_post}").json()
        assert body["code"] == 200
        assert body["data"]["id"] == test_post

    def test_get_post_detail_structure(self, test_post):
        """帖子详情数据必须包含所有标准字段"""
        body = api("get", f"/circle/posts/{test_post}").json()
        data = body["data"]
        for field in ("id", "user_id", "content", "like_count",
                      "comment_count", "is_liked", "images", "videos",
                      "create_time", "visibility"):
            assert field in data, f"帖子详情缺少字段: {field}"

    def test_get_nonexistent_post_returns_404(self):
        """获取不存在帖子：应返回 code=404"""
        body = api("get", "/circle/posts/9999999").json()
        assert body["code"] == 404

    def test_get_post_detail_is_liked_false_by_default(self, test_post):
        """未传 current_user_id 时 is_liked 应为 False"""
        body = api("get", f"/circle/posts/{test_post}").json()
        assert body["data"]["is_liked"] is False

    def test_get_post_detail_images_is_list(self, test_post):
        """帖子详情 images 字段应为列表"""
        body = api("get", f"/circle/posts/{test_post}").json()
        assert isinstance(body["data"]["images"], list)

    def test_get_post_detail_videos_is_list(self, test_post):
        """帖子详情 videos 字段应为列表"""
        body = api("get", f"/circle/posts/{test_post}").json()
        assert isinstance(body["data"]["videos"], list)


# ────────────────────────────────────────────────────────────
#  编辑帖子测试
# ────────────────────────────────────────────────────────────

class TestUpdatePost:

    @pytest.fixture
    def owned_post(self, test_user):
        """为本 class 创建一条专属帖子（函数级，每个测试独立）"""
        body = _create_post(test_user["user_id"])
        return body["data"]["post_id"]

    def test_update_post_content_success(self, test_user, owned_post):
        """有权限编辑内容：应返回 code=200"""
        body = api("put", f"/circle/posts/{owned_post}",
                   params={"user_id": test_user["user_id"]},
                   json={"content": "更新后的帖子内容"}).json()
        assert body["code"] == 200

    def test_update_post_content_persisted(self, test_user, owned_post):
        """编辑内容后查询详情，内容应已更新"""
        new_content = f"已更新内容 [{rand_str()}]"
        api("put", f"/circle/posts/{owned_post}",
            params={"user_id": test_user["user_id"]},
            json={"content": new_content})
        detail = api("get", f"/circle/posts/{owned_post}").json()
        assert detail["data"]["content"] == new_content

    def test_update_post_visibility(self, test_user, owned_post):
        """编辑可见性：应成功更新"""
        body = api("put", f"/circle/posts/{owned_post}",
                   params={"user_id": test_user["user_id"]},
                   json={"visibility": 1}).json()
        assert body["code"] == 200

    def test_update_post_wrong_user_returns_403(self, test_user, owned_post):
        """其他用户编辑别人帖子：应返回 code=403"""
        other_id = test_user["user_id"] + 9999
        body = api("put", f"/circle/posts/{owned_post}",
                   params={"user_id": other_id},
                   json={"content": "偷改别人内容"}).json()
        assert body["code"] == 403

    def test_update_nonexistent_post_returns_404(self, test_user):
        """编辑不存在帖子：应返回 code=404"""
        body = api("put", "/circle/posts/9999999",
                   params={"user_id": test_user["user_id"]},
                   json={"content": "内容"}).json()
        assert body["code"] == 404

    def test_update_post_missing_user_id_returns_422(self, owned_post):
        """缺少 user_id 查询参数：应返回 HTTP 422"""
        resp = api("put", f"/circle/posts/{owned_post}",
                   json={"content": "内容"})
        assert resp.status_code == 422


# ────────────────────────────────────────────────────────────
#  媒体绑定测试
# ────────────────────────────────────────────────────────────

class TestPostMedia:

    @pytest.fixture
    def fresh_post(self, test_user):
        body = _create_post(test_user["user_id"])
        return body["data"]["post_id"]

    def test_bind_images_success(self, fresh_post):
        """绑定图片到帖子：应返回 code=200"""
        body = api("post", f"/circle/posts/{fresh_post}/media",
                   json={
                       "images": [
                           {"url": "/uploads/images/test1.jpg", "sort_order": 0},
                           {"url": "/uploads/images/test2.jpg", "sort_order": 1},
                       ]
                   }).json()
        assert body["code"] == 200

    def test_bind_images_appear_in_detail(self, fresh_post):
        """绑定图片后帖子详情中 images 应包含绑定的 URL"""
        url1 = f"/uploads/images/img_{rand_str()}.jpg"
        api("post", f"/circle/posts/{fresh_post}/media",
            json={"images": [{"url": url1, "sort_order": 0}]})
        detail = api("get", f"/circle/posts/{fresh_post}").json()
        urls = [img["url"] for img in detail["data"]["images"]]
        assert url1 in urls

    def test_bind_video_success(self, fresh_post):
        """绑定视频到帖子：应返回 code=200"""
        body = api("post", f"/circle/posts/{fresh_post}/media",
                   json={
                       "videos": [{
                           "url":       "/uploads/videos/test.mp4",
                           "cover_url": "/uploads/images/cover.jpg",
                           "duration":  30,
                           "width":     1280,
                           "height":    720,
                       }]
                   }).json()
        assert body["code"] == 200

    def test_bind_to_nonexistent_post_returns_404(self):
        """向不存在帖子绑定媒体：应返回 code=404"""
        body = api("post", "/circle/posts/9999999/media",
                   json={"images": [{"url": "/uploads/images/x.jpg"}]}).json()
        assert body["code"] == 404


# ────────────────────────────────────────────────────────────
#  删除帖子测试
# ────────────────────────────────────────────────────────────

class TestDeletePost:

    @pytest.fixture
    def deletable_post(self, test_user):
        body = _create_post(test_user["user_id"])
        return body["data"]["post_id"]

    def test_delete_post_success(self, test_user, deletable_post):
        """有权限删除帖子：应返回 code=200"""
        body = api("delete", f"/circle/posts/{deletable_post}",
                   params={"user_id": test_user["user_id"]}).json()
        assert body["code"] == 200

    def test_deleted_post_not_visible_in_detail(self, test_user, deletable_post):
        """删除帖子后查询详情：应返回 code=404（软删除）"""
        api("delete", f"/circle/posts/{deletable_post}",
            params={"user_id": test_user["user_id"]})
        body = api("get", f"/circle/posts/{deletable_post}").json()
        assert body["code"] == 404

    def test_delete_post_wrong_user_returns_403(self, test_user, deletable_post):
        """其他用户删别人帖子：应返回 code=403"""
        other_id = test_user["user_id"] + 9999
        body = api("delete", f"/circle/posts/{deletable_post}",
                   params={"user_id": other_id}).json()
        assert body["code"] == 403

    def test_delete_nonexistent_post_returns_404(self, test_user):
        """删除不存在帖子：应返回 code=404"""
        body = api("delete", "/circle/posts/9999999",
                   params={"user_id": test_user["user_id"]}).json()
        assert body["code"] == 404

    def test_delete_post_twice_returns_404(self, test_user):
        """重复删除同一帖子：第二次应返回 code=404"""
        p = _create_post(test_user["user_id"])
        pid = p["data"]["post_id"]
        api("delete", f"/circle/posts/{pid}", params={"user_id": test_user["user_id"]})
        body = api("delete", f"/circle/posts/{pid}",
                   params={"user_id": test_user["user_id"]}).json()
        assert body["code"] == 404

    def test_delete_post_missing_user_id_returns_422(self, deletable_post):
        """缺少 user_id 参数：应返回 HTTP 422"""
        resp = api("delete", f"/circle/posts/{deletable_post}")
        assert resp.status_code == 422
