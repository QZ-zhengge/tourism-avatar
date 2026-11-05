"""
高德地图 Web服务 API — 天气查询路由
官方文档：https://lbs.amap.com/api/webservice/guide/api/weatherinfo

【接口列表】
  GET  /weather/current   — 实况天气（extensions=base）
  GET  /weather/forecast — 预报天气（extensions=all）
  GET  /weather/query    — 通用查询（可指定 extensions）
"""
import logging

import httpx
from fastapi import APIRouter, Depends, Query, HTTPException
from pydantic import TypeAdapter

from config import get_settings, Settings
from schemas.weather import (
    WeatherQuery,
    WeatherLiveResponse,
    WeatherForecastResponse,
    WeatherLiveItem,
    WeatherForecastItem,
    WeatherCastItem,
    WeatherCurrentResponse,
    WeatherForecastResponseWrapper,
)

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/weather", tags=["天气查询（高德地图API）"])


# ──────────────────────────────────────────────────────
# 工具函数
# ──────────────────────────────────────────────────────

_AMAP_ERROR_MAP = {
    "10000": "INVALID_USER_KEY       【key非法或过期，请检查 .env 中的 AMAP_API_KEY】",
    "10001": "SERVICE_RESPONSE_DATA_ERROR 【无查询结果，检查城市名/编码是否正确】",
    "10003": "DAILY_QUERY_OVER_LIMIT  【key 日调用量超限】",
    "10004": "ACCESS_TOO_FREQUENT    【访问过频，降低请求频率】",
    "10009": "BINDING_FORBIDDEN      【绑定域名无效，到控制台修改白名单】",
    "10012": "PRIVILEGE_AUTHORIZED   【权限不足，该 key 未开通 Web服务 API】",
    "10013": "USER_KEY_PLAT_NOMATCH  【key 与平台不匹配】",
    "10014": "SECURITY_USER_DAILY_QUOTA_EXCEED 【签名校验失败】",
    "10015": "TERMINAL_PARAM_INVALID  【绑定 IP 无效】",
    "10016": "SECURITY_USER_XGQE_EXCEED 【缺少签名参数】",
    "10017": "REFLECTION_XGQE_ERROR   【区块链服务错误】",
    "20000": "ENGINE_RESPONSE_DATA_ERROR  【引擎返回数据异常】",
    "20001": "KEY_PLAT_NOMATCH_20001   【请求 Key 与绑定平台不符】",
    "20002": "API_CONTRACT_UNSUPPORTED 【没有权限使用此 API】",
    "20003": "IP_QUERY_OVER_LIMIT      【IP 日访问量超限】",
    "20800": "OUT_QUOTA_LIMIT         【账号未开通配额】",
    "20801": "KENTLIMIT              【账号 api 并发超出限制】",
    "20802": "OVER_KEY_QUOTA         【APP 后端 QPS 超限】",
    "50000": "ENGINE_RETURN_TIMEOUT    【后台调度超时】",
    "50001": "ENGINE_RESPONSE_DATA_ERROR 【前台返回结果超时】",
    "50002": "ENGINE_FAILED_NO_RESULT  【请求超过限额】",
    "50003": "ENGINE_UN_KNOWN_ERROR   【请求参数值非法】",
    "50004": "ENGINE_REQUEST_TIMEOUT   【引擎返回数据异常】",
    "60000": "ENGINE_RESPONSE_PARAM_ERROR 【账号未开通此 API 配额】",
    "60001": "ENGINE_RESPONSE_DATA_ERROR  【余额耗尽，请充值】",
    "60002": "ENGINE_FAILED_NO_RESULT    【租用账户已欠费】",
    "60003": "UNKNOWN_ERROR               【未知错误】",
}


def _check_amap_config(settings: Settings) -> None:
    """检查高德 API Key 是否已配置"""
    if not settings.AMAP_API_KEY:
        raise HTTPException(
            status_code=503,
            detail=(
                "高德地图API未配置，请在 .env 中设置 AMAP_API_KEY。"
                "获取地址：https://console.amap.com/dev/key/app"
            ),
        )


async def _call_amap(params: dict, settings: Settings) -> dict:
    """
    调用高德天气 API，返回解析后的 JSON。
    所有 HTTP / 业务错误均转换为 HTTPException。
    """
    logger.info(f"[amap] GET restapi.amap.com/v3/weather/weatherInfo city={params.get('city')}")

    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.get(
                "https://restapi.amap.com/v3/weather/weatherInfo",
                params={k: v for k, v in params.items() if v is not None},
            )
        resp.raise_for_status()
        data = resp.json()
    except httpx.RequestError as e:
        raise HTTPException(status_code=502, detail=f"高德API请求失败：{e}")
    except httpx.HTTPStatusError as e:
        raise HTTPException(status_code=502, detail=f"高德API返回错误状态码：{e.response.status_code}")
    except Exception as e:
        # resp 存在但 JSON 解析失败，或其他意外错误
        raw = ""
        if "resp" in locals():
            try:
                raw = resp.text[:200]
            except Exception:
                pass
        raise HTTPException(status_code=502, detail=f"高德API返回数据异常：{raw!r}  ({e})")

    status    = data.get("status", "0")
    info      = data.get("info", "UNKNOWN_ERROR")
    infocode = data.get("infocode", "")

    if status != "1":
        hint = _AMAP_ERROR_MAP.get(infocode, f"高德错误码 {infocode}")
        logger.warning(f"[amap] 业务错误 infocode={infocode} info={info}")
        raise HTTPException(
            status_code=502,
            detail=f"高德API错误：{info}（{infocode}）— {hint}",
        )

    return data


# ──────────────────────────────────────────────────────
# 1. 实况天气（extensions=base）
# ──────────────────────────────────────────────────────

@router.get(
    "/current",
    response_model=WeatherCurrentResponse,
    summary="查询实况天气",
    description="""
根据城市名称或 adcode 查询当前实况天气。

**说明：**
- `city` 支持中文城市名（如 `北京`）、adcode（如 `110000`）、拼音（如 `beijing`）
- 多个城市用半角逗号分隔，最多 10 个
- 数据每小时更新多次

**官方参数对应：**
- `key`        = AMAP_API_KEY（自动填充）
- `city`       = 路径参数 city
- `extensions` = `base`（固定）
- `output`     = `JSON`（固定）
""",
)
async def weather_current(
    city: str = Query(..., description="城市名（如'北京'）或 adcode（如'110000'），多个用','分隔"),
    settings: Settings = Depends(get_settings),
):
    _check_amap_config(settings)

    params = {
        "key":        settings.AMAP_API_KEY,
        "city":       city,
        "extensions": "base",
        "output":     "JSON",
    }

    data = await _call_amap(params, settings)

    # 解析 lives 列表
    lives_raw = data.get("lives", [])
    lives = TypeAdapter(list[WeatherLiveItem]).validate_python(lives_raw)

    return WeatherCurrentResponse(
        code=200,
        msg="查询成功",
        amap_status=data.get("status"),
        amap_info=data.get("info"),
        data=lives,
    )


# ──────────────────────────────────────────────────────
# 2. 预报天气（extensions=all）
# ──────────────────────────────────────────────────────

@router.get(
    "/forecast",
    response_model=WeatherForecastResponseWrapper,
    summary="查询预报天气（未来3~4天）",
    description="""
根据城市名称或 adcode 查询未来 3~4 天预报天气。

**返回字段说明：**
- `casts` 数组中每个元素代表一天，含白天/夜间天气、温度、风向、风力
- 预报每天更新 3 次（8:00 / 11:00 / 18:00）

**官方参数对应：**
- `extensions` = `all`（固定）
""",
)
async def weather_forecast(
    city: str = Query(..., description="城市名（如'深圳'）或 adcode（如'440300'），多个用','分隔"),
    settings: Settings = Depends(get_settings),
):
    _check_amap_config(settings)

    params = {
        "key":        settings.AMAP_API_KEY,
        "city":       city,
        "extensions": "all",
        "output":     "JSON",
    }

    data = await _call_amap(params, settings)

    forecasts_raw = data.get("forecasts", [])
    forecasts = TypeAdapter(list[WeatherForecastItem]).validate_python(forecasts_raw)

    return WeatherForecastResponseWrapper(
        code=200,
        msg="查询成功",
        amap_status=data.get("status"),
        amap_info=data.get("info"),
        data=forecasts,
    )


# ──────────────────────────────────────────────────────
# 3. 通用查询（可指定 extensions）
# ──────────────────────────────────────────────────────

@router.get(
    "/query",
    summary="通用天气查询（可指定 extensions）",
    description="""
通用天气查询接口，通过 `extensions` 参数指定返回实况或预报：
- `extensions=base` → 实况天气（同 /weather/current）
- `extensions=all`  → 预报天气（同 /weather/forecast）

**推荐直接使用 /weather/current 或 /weather/forecast，更语义化。**
""",
)
async def weather_query(
    city: str = Query(..., description="城市名或 adcode"),
    extensions: str = Query("base", description="base=实况，all=预报"),
    settings: Settings = Depends(get_settings),
):
    _check_amap_config(settings)

    if extensions not in ("base", "all"):
        raise HTTPException(status_code=400, detail="extensions 只能为 base 或 all")

    params = {
        "key":        settings.AMAP_API_KEY,
        "city":       city,
        "extensions": extensions,
        "output":     "JSON",
    }

    data = await _call_amap(params, settings)

    amap_status  = data.get("status")
    amap_info    = data.get("info")

    if extensions == "base":
        lives = TypeAdapter(list[WeatherLiveItem]).validate_python(data.get("lives", []))
        return WeatherCurrentResponse(
            code=200, msg="查询成功",
            amap_status=amap_status, amap_info=amap_info,
            data=lives,
        )
    else:
        forecasts = TypeAdapter(list[WeatherForecastItem]).validate_python(data.get("forecasts", []))
        return WeatherForecastResponseWrapper(
            code=200, msg="查询成功",
            amap_status=amap_status, amap_info=amap_info,
            data=forecasts,
        )
