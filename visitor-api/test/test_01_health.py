"""
测试模块 01 — 健康检查
覆盖：根路径 / 和 /health 两个端点
"""
from conftest import api


class TestHealth:
    """服务健康检查"""

    def test_root_endpoint_returns_200(self):
        """根路径应返回 HTTP 200"""
        resp = api("get", "/")
        assert resp.status_code == 200

    def test_root_endpoint_status_ok(self):
        """根路径响应体 status 字段应为 ok"""
        body = api("get", "/").json()
        assert body.get("status") == "ok"

    def test_root_endpoint_has_message(self):
        """根路径响应体应包含 message 字段"""
        body = api("get", "/").json()
        assert "message" in body
        assert len(body["message"]) > 0

    def test_health_endpoint_returns_200(self):
        """/health 应返回 HTTP 200"""
        resp = api("get", "/health")
        assert resp.status_code == 200

    def test_health_endpoint_status_healthy(self):
        """/health 响应体 status 字段应为 healthy"""
        body = api("get", "/health").json()
        assert body.get("status") == "healthy"

    def test_health_endpoint_has_service(self):
        """/health 响应体应包含 service 字段"""
        body = api("get", "/health").json()
        assert "service" in body
