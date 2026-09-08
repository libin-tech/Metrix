"""同花顺金融数据 API 客户端与对内行情格式适配。"""
import math
import re
from datetime import datetime, timedelta, timezone

import requests

DEFAULT_BASE_URL = "https://fuyao.aicubes.cn"
MARKET_TIME_ZONE = timezone(timedelta(hours=8))
TICKER_PAGE_SIZE = 10000
MAX_HISTORY_DAYS = 3650
MIN_HISTORY_DAYS = 180
HISTORY_WINDOW_MULTIPLIER = 3
MILLISECONDS_PER_SECOND = 1000
KLINE_FIELDS = {"timestamp": "date_ms", "open": "open_price", "high": "high_price",
               "low": "low_price", "close": "close_price", "volume": "volume", "amount": "turnover"}


def number(value):
    try:
        result = float(value)
        return result if math.isfinite(result) else None
    except (TypeError, ValueError):
        return None


def timestampMillis(value):
    if value is None:
        return None
    numeric = number(value)
    if numeric is not None:
        return int(numeric * MILLISECONDS_PER_SECOND if numeric < 10_000_000_000 else numeric)
    instant = datetime.fromisoformat(str(value).replace("Z", "+00:00"))
    if instant.tzinfo is None:
        instant = instant.replace(tzinfo=MARKET_TIME_ZONE)
    return int(instant.timestamp() * MILLISECONDS_PER_SECOND)


class FuyaoClient:
    def __init__(self, apiKey, baseUrl=DEFAULT_BASE_URL, timeout=30):
        if not apiKey or not apiKey.strip():
            raise ValueError("请配置同花顺金融数据 API Key")
        self.baseUrl = baseUrl.rstrip("/")
        self.timeout = timeout
        self.session = requests.Session()
        self.session.headers.update({"X-api-key": apiKey, "Accept": "application/json"})

    def get(self, path, params):
        response = self.session.get(self.baseUrl + path, params=params, timeout=self.timeout)
        response.raise_for_status()
        payload = response.json()
        if payload.get("code") != 0:
            raise RuntimeError(f"同花顺金融数据 API 请求失败（code={payload.get('code')}）")
        data = payload.get("data")
        if not isinstance(data, dict) or not isinstance(data.get("item"), list):
            raise ValueError("同花顺金融数据 API 返回格式无效")
        return data

    def tickers(self):
        records = {}
        offset = 0
        while True:
            data = self.get("/api/meta/tickers/list", {
                "asset_type": "a-share", "limit": TICKER_PAGE_SIZE, "offset": offset,
            })
            page = data["item"]
            normalized = [normalizeTicker(row) for row in page]
            freshCodes = {row["tsCode"] for row in normalized} - records.keys()
            if page and not freshCodes:
                raise ValueError("标的列表分页重复，已停止同步")
            records.update({row["tsCode"]: row for row in normalized})
            if len(page) < TICKER_PAGE_SIZE:
                break
            offset += len(page)
        if not records:
            raise ValueError("同花顺金融数据 API 未返回 A 股标的，同步已取消")
        return list(records.values())

    def quotes(self, symbols):
        data = self.get("/api/a-share/prices/snapshot", {"thscodes": ",".join(symbols)})
        bySymbol = {row["thscode"]: row for row in data["item"]}
        if any(symbol not in bySymbol for symbol in symbols):
            raise ValueError("同花顺金融数据 API 未返回请求标的的行情")
        return [normalizeQuote(bySymbol[symbol], data.get("timestamp")) for symbol in symbols]

    def klines(self, symbol, period, count):
        if period != "1d" or count < 1:
            raise ValueError("同花顺金融数据 API 仅支持日K，条数必须为正数")
        end = datetime.now(MARKET_TIME_ZONE)
        windowDays = min(MAX_HISTORY_DAYS, max(MIN_HISTORY_DAYS, count * HISTORY_WINDOW_MULTIPLIER))
        records = []
        while True:
            data = self.get("/api/a-share/prices/historical", {
                "thscode": symbol, "interval": period, "adjust": "forward",
                "start": int((end - timedelta(days=windowDays)).timestamp() * MILLISECONDS_PER_SECOND),
                "end": int(end.timestamp() * MILLISECONDS_PER_SECOND),
            })
            records = sorted({row["date_ms"]: row for row in data["item"]}.values(), key=lambda row: row["date_ms"])
            if len(records) >= count or windowDays == MAX_HISTORY_DAYS:
                break
            windowDays = min(MAX_HISTORY_DAYS, windowDays * 2)
        if not records:
            raise ValueError("同花顺金融数据 API 未返回有效日K")
        records = records[-count:]
        if any(number(row.get("close_price")) is None for row in records):
            raise ValueError("同花顺金融数据 API 日K缺少收盘价")
        return {key: [number(row.get(field)) for row in records] for key, field in KLINE_FIELDS.items()}

    def marketTurnover(self, symbols):
        data = self.get("/api/a-share-index/prices/snapshot", {"thscodes": ",".join(symbols)})
        rows = {row["thscode"]: row for row in data["item"]}
        timestamp = timestampMillis(data.get("timestamp"))
        amounts = [number(rows.get(symbol, {}).get("turnover")) for symbol in symbols]
        if timestamp is None or any(amount is None or amount < 0 for amount in amounts):
            raise ValueError("同花顺金融数据 API 指数成交额或数据时间缺失")
        tradeDate = datetime.fromtimestamp(timestamp / MILLISECONDS_PER_SECOND, MARKET_TIME_ZONE).date().isoformat()
        amount = round(sum(amounts))
        return {"amount": amount, "history": [{"date": tradeDate, "amount": amount}]}

    def anomalyAnalysisList(self):
        return self.get("/api/a-share/special-data/anomaly-analysis-list", {})


def normalizeQuote(row, timestamp):
    previous = number(row.get("prev_price"))
    high = number(row.get("high_price"))
    low = number(row.get("low_price"))
    amplitude = (high - low) / previous * 100 if previous and high is not None and low is not None else None
    return {
        "close": number(row.get("last_price")), "last_price": number(row.get("last_price")),
        "open": number(row.get("open_price")), "high": high, "low": low,
        "volume": number(row.get("volume")), "prev_close": previous,
        "amount": number(row.get("turnover")), "timestamp": timestampMillis(timestamp),
        "ext": {"change_pct": number(row.get("price_change_ratio_pct")),
                "change_amount": number(row.get("price_change")), "amplitude": amplitude,
                "turnover_rate": None, "volume_ratio": None},
    }


def normalizeTicker(row):
    code = str(row.get("thscode") or "").strip()
    symbol = str(row.get("ticker") or "").strip()
    name = str(row.get("name") or "").strip()
    if row.get("asset_type") != "a-share" or not re.fullmatch(r"[0-9]{6}\.(SH|SZ|BJ)", code):
        raise ValueError("上游标的代码或资产类型无效，同步已取消")
    if symbol != code.split(".")[0] or not name:
        raise ValueError("上游标的名称或纯代码缺失，同步已取消")
    return {"tsCode": code, "symbol": symbol, "name": name}
