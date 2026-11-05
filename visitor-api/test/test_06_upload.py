"""
测试模块 06 — 文件上传模块
覆盖业务逻辑：
  - 单张图片上传：成功 / 不支持的格式 / 缺少文件
  - 批量图片上传：正常 / 超过9张限制 / 混合类型（部分成功）
  - 视频上传：成功 / 不支持的格式
  - 响应结构校验：code / url / filename / size
  - URL 格式校验：路径以 /uploads/ 开头且可访问

所有测试文件在内存中构造，无需实际文件。
"""
import io
import pytest
from conftest import api, BASE_URL
import requests


# ────────────────────────────────────────────────────────────
#  测试文件工厂
# ────────────────────────────────────────────────────────────

def _make_image(fmt: str = "jpeg", size_bytes: int = 1024) -> tuple:
    """
    在内存中构造最小有效图片文件。
    返回 (filename, file_obj, content_type)
    """
    # 最小 JPEG 字节序列（有效的 1x1 像素 JPEG）
    MINIMAL_JPEG = (
        b"\xff\xd8\xff\xe0\x00\x10JFIF\x00\x01\x01\x00\x00\x01\x00\x01\x00\x00"
        b"\xff\xdb\x00C\x00\x08\x06\x06\x07\x06\x05\x08\x07\x07\x07\t\t"
        b"\x08\n\x0c\x14\r\x0c\x0b\x0b\x0c\x19\x12\x13\x0f\x14\x1d\x1a"
        b"\x1f\x1e\x1d\x1a\x1c\x1c $.' \",#\x1c\x1c(7),01444\x1f'9=82<.342\x1eB"
        b"\xce\xff\xd9"
    )
    # 最小 PNG 字节序列（有效的 1x1 像素 PNG）
    MINIMAL_PNG = (
        b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00"
        b"\x01\x08\x02\x00\x00\x00\x90wS\xde\x00\x00\x00\x0cIDATx\x9cc\xf8"
        b"\x0f\x00\x00\x01\x01\x00\x05\x18\xd8N\x00\x00\x00\x00IEND\xaeB`\x82"
    )

    fmt_map = {
        "jpeg": ("test.jpg",  MINIMAL_JPEG, "image/jpeg"),
        "jpg":  ("test.jpg",  MINIMAL_JPEG, "image/jpeg"),
        "png":  ("test.png",  MINIMAL_PNG,  "image/png"),
        "webp": ("test.webp", MINIMAL_JPEG, "image/webp"),  # 用 JPEG 内容模拟
        "gif":  ("test.gif",  b"GIF89a\x01\x00\x01\x00\x00\xff\x00,\x00\x00\x00\x00\x01\x00\x01\x00\x00\x02\x00;",
                 "image/gif"),
        "txt":  ("test.txt",  b"hello world", "text/plain"),
        "pdf":  ("test.pdf",  b"%PDF-1.4", "application/pdf"),
        "mp4":  ("test.mp4",  b"\x00\x00\x00\x18ftypmp42" + b"\x00" * 50,
                 "video/mp4"),
    }
    name, data, ctype = fmt_map.get(fmt, fmt_map["jpeg"])
    # 如果需要更大的文件，填充到指定大小
    if len(data) < size_bytes:
        data = data + b"\x00" * (size_bytes - len(data))
    return name, io.BytesIO(data), ctype


def _upload_image(fmt: str = "jpeg") -> requests.Response:
    name, fp, ctype = _make_image(fmt)
    return requests.post(
        BASE_URL + "/upload/image",
        files={"file": (name, fp, ctype)},
        timeout=15,
    )


def _upload_video() -> requests.Response:
    name, fp, ctype = _make_image("mp4")
    return requests.post(
        BASE_URL + "/upload/video",
        files={"file": (name, fp, ctype)},
        timeout=15,
    )


# ────────────────────────────────────────────────────────────
#  单张图片上传测试
# ────────────────────────────────────────────────────────────

class TestUploadSingleImage:

    def test_upload_jpeg_success(self):
        """上传 JPEG 图片：应返回 code=200"""
        body = _upload_image("jpeg").json()
        assert body["code"] == 200

    def test_upload_png_success(self):
        """上传 PNG 图片：应返回 code=200"""
        body = _upload_image("png").json()
        assert body["code"] == 200

    def test_upload_gif_success(self):
        """上传 GIF 图片：应返回 code=200"""
        body = _upload_image("gif").json()
        assert body["code"] == 200

    def test_upload_image_response_structure(self):
        """上传成功响应必须包含 code / msg / url / filename / size"""
        body = _upload_image("jpeg").json()
        for field in ("code", "msg", "url", "filename", "size"):
            assert field in body, f"响应缺少字段: {field}"

    def test_upload_image_url_starts_with_uploads(self):
        """上传返回的 url 应以 /uploads/images/ 开头"""
        body = _upload_image("jpeg").json()
        assert body["url"].startswith("/uploads/images/"), \
            f"url 格式错误: {body['url']}"

    def test_upload_image_size_is_positive(self):
        """上传返回的 size 应为正整数"""
        body = _upload_image("jpeg").json()
        assert isinstance(body["size"], int)
        assert body["size"] > 0

    def test_upload_image_file_accessible(self):
        """上传成功后，返回的 url 应可访问（HTTP 200）"""
        body = _upload_image("jpeg").json()
        assert body["code"] == 200
        resp = requests.get(BASE_URL + body["url"], timeout=10)
        assert resp.status_code == 200, \
            f"上传文件无法访问: {BASE_URL + body['url']}"

    def test_upload_unsupported_image_type_returns_400(self):
        """上传不支持的文件类型（txt）：应返回 code=400"""
        body = _upload_image("txt").json()
        assert body["code"] == 400
        assert "格式" in body["msg"] or "支持" in body["msg"]

    def test_upload_pdf_returns_400(self):
        """上传 PDF：应返回 code=400"""
        body = _upload_image("pdf").json()
        assert body["code"] == 400

    def test_upload_image_no_file_returns_422(self):
        """未附带文件：应返回 HTTP 422"""
        resp = requests.post(BASE_URL + "/upload/image", timeout=10)
        assert resp.status_code == 422

    def test_upload_image_filename_not_empty(self):
        """上传成功时 filename 字段不应为空"""
        body = _upload_image("jpeg").json()
        assert body["filename"]
        assert len(body["filename"]) > 0


# ────────────────────────────────────────────────────────────
#  批量图片上传测试
# ────────────────────────────────────────────────────────────

class TestUploadMultipleImages:

    def _build_files(self, count: int, fmt: str = "jpeg") -> list:
        files = []
        for i in range(count):
            name, fp, ctype = _make_image(fmt)
            files.append(("files", (f"img{i}.{fmt}", fp, ctype)))
        return files

    def test_upload_3_images_success(self):
        """批量上传 3 张图：应全部成功"""
        files = self._build_files(3)
        resp = requests.post(BASE_URL + "/upload/images", files=files, timeout=20)
        body = resp.json()
        assert body["code"] == 200
        assert len(body["files"]) == 3

    def test_upload_9_images_max_limit(self):
        """批量上传 9 张图（上限）：应全部成功"""
        files = self._build_files(9)
        resp = requests.post(BASE_URL + "/upload/images", files=files, timeout=30)
        body = resp.json()
        assert body["code"] == 200
        assert len(body["files"]) == 9

    def test_upload_10_images_exceeds_limit(self):
        """批量上传 10 张图（超限）：应返回 code=400"""
        files = self._build_files(10)
        resp = requests.post(BASE_URL + "/upload/images", files=files, timeout=30)
        body = resp.json()
        assert body["code"] == 400
        assert "9" in body["msg"]

    def test_batch_upload_response_structure(self):
        """批量上传响应必须含 code / msg / files / failed"""
        files = self._build_files(2)
        body = requests.post(BASE_URL + "/upload/images", files=files, timeout=15).json()
        for field in ("code", "msg", "files", "failed"):
            assert field in body, f"响应缺少字段: {field}"

    def test_batch_upload_each_file_has_sort_order(self):
        """批量上传结果每项应包含 sort_order 字段"""
        files = self._build_files(2)
        body = requests.post(BASE_URL + "/upload/images", files=files, timeout=15).json()
        for f in body["files"]:
            assert "sort_order" in f

    def test_batch_upload_sort_order_sequential(self):
        """批量上传结果 sort_order 应从 0 开始递增"""
        files = self._build_files(3)
        body = requests.post(BASE_URL + "/upload/images", files=files, timeout=15).json()
        orders = [f["sort_order"] for f in body["files"]]
        assert orders == list(range(len(orders)))

    def test_batch_upload_partial_invalid_type(self):
        """混合上传：有效图片 + 无效类型文件；有效的应成功，无效的应在 failed 列表"""
        name_j, fp_j, ct_j = _make_image("jpeg")
        name_t, fp_t, ct_t = _make_image("txt")
        files = [
            ("files", (name_j, fp_j, ct_j)),
            ("files", (name_t, fp_t, ct_t)),
        ]
        body = requests.post(BASE_URL + "/upload/images", files=files, timeout=15).json()
        assert body["code"] == 200
        assert len(body["files"]) == 1    # 1 张成功
        assert len(body["failed"]) == 1   # 1 张失败


# ────────────────────────────────────────────────────────────
#  视频上传测试
# ────────────────────────────────────────────────────────────

class TestUploadVideo:

    def test_upload_mp4_success(self):
        """上传 MP4 视频：应返回 code=200"""
        body = _upload_video().json()
        assert body["code"] == 200

    def test_upload_video_response_structure(self):
        """视频上传响应必须含 code / msg / url / filename / size"""
        body = _upload_video().json()
        for field in ("code", "msg", "url", "filename", "size"):
            assert field in body, f"响应缺少字段: {field}"

    def test_upload_video_url_starts_with_uploads_videos(self):
        """视频上传返回 url 应以 /uploads/videos/ 开头"""
        body = _upload_video().json()
        assert body["url"].startswith("/uploads/videos/"), \
            f"url 格式错误: {body['url']}"

    def test_upload_video_size_is_positive(self):
        """视频上传返回 size 应为正整数"""
        body = _upload_video().json()
        assert body["size"] > 0

    def test_upload_video_file_accessible(self):
        """视频上传成功后，返回 url 应可访问"""
        body = _upload_video().json()
        assert body["code"] == 200
        resp = requests.get(BASE_URL + body["url"], timeout=10)
        assert resp.status_code == 200

    def test_upload_image_as_video_returns_400(self):
        """以图片文件上传到视频接口：应返回 code=400"""
        name, fp, ctype = _make_image("jpeg")
        body = requests.post(
            BASE_URL + "/upload/video",
            files={"file": (name, fp, ctype)},
            timeout=15,
        ).json()
        assert body["code"] == 400
        assert "格式" in body["msg"] or "支持" in body["msg"]

    def test_upload_video_no_file_returns_422(self):
        """未附带文件上传视频：应返回 HTTP 422"""
        resp = requests.post(BASE_URL + "/upload/video", timeout=10)
        assert resp.status_code == 422
