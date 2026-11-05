"""
高德地图 Web服务 API — 天气查询（weatherInfo）

官方文档：https://lbs.amap.com/api/webservice/guide/api/weatherinfo
接口地址：GET https://restapi.amap.com/v3/weather/weatherInfo

【请求参数】
  key        必填  Web服务API Key
  city       必填  城市编码（adcode，如北京110000）或城市名称
  extensions 可选  base=实况天气（默认），all=预报天气
  output     可选  返回格式，默认JSON

【返回字段（实况 lives）】
  province      省份名
  city          城市名
  adcode        区域编码
  weather       天气现象（晴、多云、阴、小雨…）
  temperature   实时气温（摄氏度）
  winddirection 风向
  windpower     风力
  humidity      空气湿度（%）
  reporttime    数据发布时间
  temperature_float  气温浮点值
  humidity_float     湿度浮点值

【返回字段（预报 forecasts）】
  city          城市名
  adcode        区域编码
  province      省份名
  reporttime    数据发布时间
  casts[]      未来几天预报列表
    date          日期（yyyy-MM-dd）
    week         星期（1~7）
    dayweather   白天天气
    nightweather 夜间天气
    daytemp      白天温度
    nighttemp    夜间温度
    daywind      白天风向
    nightwind    夜间风向
    daypower     白天风力
    nightpower   夜间风力
"""
from typing import Optional, List, Literal
from pydantic import BaseModel, Field


# ─────────────────────────────────────────────────────────────
# 请求模型
# ─────────────────────────────────────────────────────────────

class WeatherQuery(BaseModel):
    """天气查询请求参数（GET /weather/current、/weather/forecast）"""

    city: str = Field(
        ...,
        description=(
            "城市名称（如'北京'）或 adcode（如'110000'）。"
            "支持中文城市名、citycode、adcode，多个城市用','分隔（最多10个）。"
        ),
        examples=["北京", "110000", "深圳,shenzhen"],
    )
    extensions: Literal["base", "all"] = Field(
        default="base",
        description="base=实况天气（默认），all=含未来3~4天预报",
    )

    class Config:
        json_schema_extra = {
            "example": {
                "city": "北京",
                "extensions": "all",
            }
        }


# ─────────────────────────────────────────────────────────────
# 响应模型 — 实况天气（lives）
# ─────────────────────────────────────────────────────────────

class WeatherLiveItem(BaseModel):
    """实况天气条目（lives 数组元素）"""

    province: str = Field(..., description="省份名")
    city: str = Field(..., description="城市名")
    adcode: str = Field(..., description="区域编码（adcode）")
    weather: str = Field(..., description="天气现象（晴、多云、阴、小雨…）")
    temperature: str = Field(..., description="实时气温（摄氏度，字符串格式）")
    winddirection: str = Field(..., description="风向（东、南、西、北、东北…）")
    windpower: str = Field(..., description="风力等级（≤3、4、5…）")
    humidity: str = Field(..., description="空气湿度（%，字符串格式）")
    reporttime: str = Field(..., description="数据发布时间 yyyy-MM-dd HH:mm:ss")
    temperature_float: Optional[str] = Field(None, description="气温浮点值")
    humidity_float: Optional[str] = Field(None, description="湿度浮点值")


class WeatherLiveResponse(BaseModel):
    """实况天气查询响应（extensions=base）"""

    status: str = Field(..., description="返回状态：1=成功，0=失败")
    info: str = Field(..., description="返回信息，成功时为 'OK'")
    infocode: str = Field(..., description="返回状态码")
    lives: Optional[List[WeatherLiveItem]] = Field(None, description="实况天气列表")


# ─────────────────────────────────────────────────────────────
# 响应模型 — 预报天气（forecasts）
# ─────────────────────────────────────────────────────────────

class WeatherCastItem(BaseModel):
    """单日预报条目（casts 数组元素）"""

    date: str = Field(..., description="日期 yyyy-MM-dd")
    week: str = Field(..., description="星期（1~7）")
    dayweather: str = Field(..., description="白天天气现象")
    nightweather: str = Field(..., description="夜间天气现象")
    daytemp: str = Field(..., description="白天温度（℃）")
    nighttemp: str = Field(..., description="夜间温度（℃）")
    daywind: str = Field(..., description="白天风向")
    nightwind: str = Field(..., description="夜间风向")
    daypower: str = Field(..., description="白天风力")
    nightpower: str = Field(..., description="夜间风力")


class WeatherForecastItem(BaseModel):
    """预报天气条目（forecasts 数组元素）"""

    city: str = Field(..., description="城市名")
    adcode: str = Field(..., description="区域编码")
    province: str = Field(..., description="省份名")
    reporttime: str = Field(..., description="数据发布时间")
    casts: List[WeatherCastItem] = Field(..., description="未来几天预报列表")


class WeatherForecastResponse(BaseModel):
    """预报天气查询响应（extensions=all）"""

    status: str = Field(..., description="返回状态：1=成功，0=失败")
    info: str = Field(..., description="返回信息")
    infocode: str = Field(..., description="返回状态码")
    forecasts: Optional[List[WeatherForecastItem]] = Field(None, description="预报天气列表")


# ─────────────────────────────────────────────────────────────
# 统一业务响应
# ─────────────────────────────────────────────────────────────

class WeatherBaseResponse(BaseModel):
    """业务层统一响应"""

    code: int = Field(..., description="业务状态码：200=成功，其他=失败")
    msg: str = Field(..., description="提示信息")
    amap_status: Optional[str] = Field(None, description="高德API原始 status（1=成功，0=失败）")
    amap_info: Optional[str] = Field(None, description="高德API原始 info 信息")


class WeatherCurrentResponse(WeatherBaseResponse):
    """实况天气业务响应"""

    data: Optional[List[WeatherLiveItem]] = None


class WeatherForecastResponseWrapper(WeatherBaseResponse):
    """预报天气业务响应"""

    data: Optional[List[WeatherForecastItem]] = None
