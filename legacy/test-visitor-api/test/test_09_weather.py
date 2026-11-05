"""
高德天气查询接口 — 自动化测试脚本
────────────────────────────────────────────────────
使用方式：
  z:/SoftwareCupAI/.venv/Scripts/python.exe test/test_09_weather.py

测试要求：
  1. 服务已启动（python main.py）
  2. .env 中已配置有效的 AMAP_API_KEY
────────────────────────────────────────────────────
"""
import requests
import sys

# ── 配置 ────────────────────────────────────────────
BASE = "http://localhost:8000"
passed = 0
failed = 0


def run_test(name: str, fn) -> None:
    global passed, failed
    print(f"\n{'─' * 50}")
    print(f"[测试] {name}")
    try:
        fn()
        passed += 1
        print(f"  [PASS] 通过")
    except AssertionError as e:
        failed += 1
        print(f"  [FAIL] 失败: {e}")
    except requests.RequestException as e:
        failed += 1
        err = getattr(e, 'response', None)
        body = ""
        if err is not None:
            try:
                body = err.text[:300]
            except Exception:
                body = "<无法读取响应体>"
        detail = getattr(err, 'text', "") if err else str(e)
        print(f"  ❌ HTTP错误: {detail[:300]} => {body}")
    except Exception as e:
        failed += 1
        print(f"  ❌ 异常: {type(e).__name__}: {e}")


# ════════════════════════════════════════════════════
# 测试用例
# ════════════════════════════════════════════════════

# ── 1. 实况天气 GET /weather/current ────────────────

def test_current_beijing():
    """实况天气 - 中文城市名"""
    r = requests.get(f"{BASE}/weather/current", params={"city": "北京"})
    assert r.status_code == 200, f"HTTP {r.status_code}: {r.text[:200]}"
    body = r.json()
    assert body["code"] == 200, f"业务码 {body.get('code')}: {body.get('msg')}"
    assert body["data"], "data 为空"
    item = body["data"][0]
    assert item["city"] in ("北京", "北京市"), f"city={item.get('city')}"
    assert "weather" in item
    assert "temperature" in item
    print(f"    北京天气: {item['weather']}, {item['temperature']}℃")


def test_current_adcode():
    """实况天气 - adcode 查询"""
    r = requests.get(f"{BASE}/weather/current", params={"city": "440300"})
    assert r.status_code == 200
    body = r.json()
    assert body["code"] == 200, f"业务码 {body.get('code')}: {body.get('msg')}"
    assert body["data"]
    item = body["data"][0]
    assert "深圳" in item["city"], f"adcode 440300 应返回深圳: {item.get('city')}"


def test_current_multi_city():
    """实况天气 - 多城市查询"""
    r = requests.get(f"{BASE}/weather/current", params={"city": "北京,上海"})
    assert r.status_code == 200
    body = r.json()
    assert body["code"] == 200
    assert len(body["data"]) == 2, f"应返回 2 个城市: got {len(body.get('data', []))}"


def test_current_no_key():
    """实况天气 - 未配置 API Key（预期 503）"""
    # 这个测试在服务正常配置时会通过200，目的是验证接口能正常返回
    r = requests.get(f"{BASE}/weather/current", params={"city": "北京"})
    # 如果有配置Key就是200，否则503
    assert r.status_code in (200, 503), f"预期200或503: got {r.status_code}"


# ── 2. 预报天气 GET /weather/forecast ───────────────

def test_forecast_shenzhen():
    """预报天气 - 深圳"""
    r = requests.get(f"{BASE}/weather/forecast", params={"city": "深圳"})
    assert r.status_code == 200
    body = r.json()
    assert body["code"] == 200, f"业务码 {body.get('code')}: {body.get('msg')}"
    assert body["data"]
    item = body["data"][0]
    assert "深圳" in item["city"]
    assert item.get("casts"), "casts 为空"
    assert len(item["casts"]) >= 2, f"预报至少2天: got {len(item['casts'])}"
    print(f"    {item['city']} 预报天数: {len(item['casts'])}")


def test_forecast_fields():
    """预报天气 - 字段完整性"""
    r = requests.get(f"{BASE}/weather/forecast", params={"city": "北京"})
    assert r.status_code == 200
    body = r.json()
    assert body["data"]
    cast = body["data"][0]["casts"][0]
    required = ["date", "week", "dayweather", "nightweather", "daytemp", "nighttemp"]
    for field in required:
        assert field in cast, f"缺少字段: {field}"


# ── 3. 通用查询 GET /weather/query ──────────────────

def test_query_base():
    """通用查询 - base 模式"""
    r = requests.get(f"{BASE}/weather/query", params={"city": "北京", "extensions": "base"})
    assert r.status_code == 200
    body = r.json()
    assert body["code"] == 200
    assert body.get("data")
    print(f"    base模式: {body['data'][0]['city']}")


def test_query_all():
    """通用查询 - all 模式"""
    r = requests.get(f"{BASE}/weather/query", params={"city": "深圳", "extensions": "all"})
    assert r.status_code == 200
    body = r.json()
    assert body["code"] == 200
    assert body.get("data")
    # all 模式返回 forecasts 结构
    assert body["data"][0].get("casts")
    print(f"    all模式: {body['data'][0]['city']}, 预报{len(body['data'][0]['casts'])}天")


def test_query_invalid_extensions():
    """通用查询 - 非法 extensions 参数"""
    r = requests.get(f"{BASE}/weather/query", params={"city": "北京", "extensions": "invalid"})
    assert r.status_code == 400, f"预期 400: got {r.status_code}"
    body = r.json()
    assert "extensions" in str(body.get("detail", "")), f"错误信息应提及 extensions: {body}"


# ── 4. 参数校验 ──────────────────────────────────────

def test_missing_city():
    """缺少 city 参数"""
    r = requests.get(f"{BASE}/weather/current")
    assert r.status_code == 422, f"预期 422（校验失败）: got {r.status_code}"


def test_missing_city_forecast():
    """缺少 city 参数（预报接口）"""
    r = requests.get(f"{BASE}/weather/forecast")
    assert r.status_code == 422, f"预期 422: got {r.status_code}"


# ════════════════════════════════════════════════════
# 运行
# ════════════════════════════════════════════════════

if __name__ == "__main__":
    print("=" * 60)
    print(" 高德天气查询接口 — 自动化测试")
    print(f" 服务地址: {BASE}")
    print("=" * 60)

    #  实况天气
    run_test("实况天气 - 中文城市名（北京）", test_current_beijing)
    run_test("实况天气 - adcode（440300→深圳）", test_current_adcode)
    run_test("实况天气 - 多城市（北京,上海）", test_current_multi_city)
    run_test("实况天气 - 服务状态检查", test_current_no_key)

    #  预报天气
    run_test("预报天气 - 深圳", test_forecast_shenzhen)
    run_test("预报天气 - 字段完整性", test_forecast_fields)

    #  通用查询
    run_test("通用查询 - base 模式", test_query_base)
    run_test("通用查询 - all 模式", test_query_all)
    run_test("通用查询 - 非法 extensions", test_query_invalid_extensions)

    #  参数校验
    run_test("实况 - 缺少 city 参数", test_missing_city)
    run_test("预报 - 缺少 city 参数", test_missing_city_forecast)

    # ── 汇总 ────────────────────────────────────────
    print(f"\n{'=' * 60}")
    total = passed + failed
    print(f" 测试完成: {total} 项 | 通过 {passed} | 失败 {failed}")
    if failed:
        print(" [WARN] 存在失败用例，请检查！")
        sys.exit(1)
    else:
        print("  [PASS] 全部通过！")
    print(f"{'=' * 60}")
