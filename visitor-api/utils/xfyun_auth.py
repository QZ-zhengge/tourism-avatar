"""
讯飞开放平台 HMAC-SHA256 鉴权工具
官方文档：https://www.xfyun.cn/doc/tts/virtual_human/API.html

鉴权流程：
1. 构造 signature_origin = "host: {host}\ndate: {date}\n{request_line}"
2. HMAC-SHA256(api_secret, signature_origin) → Base64 → signature
3. authorization_origin = 'api_key="{api_key}", algorithm="hmac-sha256", headers="host date request-line", signature="{signature}"'
4. Base64(authorization_origin) → authorization
5. 请求URL附加参数: ?host=...&date=...&authorization=...
"""
import hmac
import hashlib
import base64
from datetime import datetime, timezone
from email.utils import formatdate
from urllib.parse import urlencode, quote
from typing import Optional


def _get_rfc1123_date() -> str:
    """获取 RFC1123 格式的 UTC 时间字符串，讯飞鉴权要求的时间格式"""
    return formatdate(usegmt=True)


def build_auth_url(
    host: str,
    path: str,
    api_key: str,
    api_secret: str,
    method: str = "POST",
    date: Optional[str] = None,
) -> dict:
    """
    构造讯飞鉴权参数，返回包含 host / date / authorization 的字典，
    调用方可将其附加到请求 URL 的 Query String 中。

    :param host: 服务主机名，如 vms.cn-huadong-1.xf-yun.com
    :param path: 接口路径，如 /v1/private/vms2d_start
    :param api_key: 讯飞控制台 APIKey（32位）
    :param api_secret: 讯飞控制台 APISecret（32位）
    :param method: HTTP 方法（默认 POST）
    :param date: 可选，指定日期字符串（测试用），None 则使用当前时间
    :return: {"host": ..., "date": ..., "authorization": ...}
    """
    if date is None:
        date = _get_rfc1123_date()

    # step1: 构造 signature_origin
    request_line = f"{method} {path} HTTP/1.1"
    signature_origin = f"host: {host}\ndate: {date}\n{request_line}"

    # step2: HMAC-SHA256 签名 + Base64
    signature_sha = hmac.new(
        api_secret.encode("utf-8"),
        signature_origin.encode("utf-8"),
        digestmod=hashlib.sha256,
    ).digest()
    signature = base64.b64encode(signature_sha).decode("utf-8")

    # step3: 构造 authorization_origin
    authorization_origin = (
        f'api_key="{api_key}", algorithm="hmac-sha256", '
        f'headers="host date request-line", signature="{signature}"'
    )

    # step4: Base64 编码
    authorization = base64.b64encode(
        authorization_origin.encode("utf-8")
    ).decode("utf-8")

    return {
        "host": host,
        "date": date,
        "authorization": authorization,
    }


def build_auth_query_url(
    base_url: str,
    host: str,
    path: str,
    api_key: str,
    api_secret: str,
    method: str = "POST",
) -> str:
    """
    构造带鉴权参数的完整请求 URL

    :param base_url: 基础 URL，如 https://vms.cn-huadong-1.xf-yun.com
    :param host: 主机名
    :param path: 接口路径
    :param api_key: APIKey
    :param api_secret: APISecret
    :param method: HTTP 方法
    :return: 完整鉴权 URL
    """
    auth_params = build_auth_url(host, path, api_key, api_secret, method)
    # 按官方文档，date 需要 URL 编码
    query_string = urlencode({
        "host": auth_params["host"],
        "date": auth_params["date"],
        "authorization": auth_params["authorization"],
    })
    return f"{base_url}{path}?{query_string}"


def build_wss_auth_url(
    wss_base: str,
    host: str,
    path: str,
    api_key: str,
    api_secret: str,
) -> str:
    """
    构造 WSS 协议的带鉴权参数的完整 URL（音频驱动接口使用）

    :param wss_base: WSS 基础地址，如 wss://vms.cn-huadong-1.xf-yun.com
    :param host: 主机名
    :param path: 接口路径
    :param api_key: APIKey
    :param api_secret: APISecret
    :return: 完整鉴权 WSS URL
    """
    auth_params = build_auth_url(host, path, api_key, api_secret, "POST")
    query_string = urlencode({
        "host": auth_params["host"],
        "date": auth_params["date"],
        "authorization": auth_params["authorization"],
    })
    return f"{wss_base}{path}?{query_string}"
