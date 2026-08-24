#!/usr/bin/env python3
"""获取首页龙虎榜和资金流向数据，并以 JSON 输出到 stdout。"""
import io
import json
import math
import os
import sys
import warnings
from contextlib import redirect_stdout
from datetime import date, timedelta

import requests

_script_dir = os.path.dirname(os.path.abspath(__file__))
sys.path = [path for path in sys.path if path != _script_dir]

DRAGON_TIGER_LIMIT = 20
FUND_FLOW_LIMIT = 20
POPULARITY_RANK_LIMIT = 20
INDUSTRY_LIMIT = 20
LIMIT_POOL_LIMIT = 20
DRAGON_TIGER_LOOKBACK_DAYS = 30
EASTMONEY_FUND_FLOW_URL = "https://push2.eastmoney.com/api/qt/clist/get"
EASTMONEY_FUND_FLOW_FIELDS = "f2,f3,f12,f14,f62,f184,f100"
EASTMONEY_FUND_FLOW_FILTER = "m:0+t:6+f:!2,m:0+t:13+f:!2,m:0+t:80+f:!2,m:1+t:2+f:!2,m:1+t:23+f:!2,m:0+t:7+f:!2,m:1+t:3+f:!2"
REQUEST_TIMEOUT_SECONDS = 15


def main():
    os.environ["TQDM_DISABLE"] = "1"
    warnings.filterwarnings("ignore")
    try:
        import akshare as ak
    except ImportError:
        print(json.dumps({"status": "error", "message": "AKShare 未安装"}, ensure_ascii=False), flush=True)
        return

    result = {"dragonTiger": [], "fundFlow": [], "popularityRank": [], "industrySectors": [], "limitPools": {}, "errors": {}, "asOf": date.today().isoformat()}
    try:
        result["dragonTiger"] = get_dragon_tiger(ak)
    except Exception as error:
        result["errors"]["dragonTiger"] = str(error)

    try:
        result["fundFlow"] = get_fund_flow()
    except Exception as error:
        result["errors"]["fundFlow"] = str(error)

    try:
        result["popularityRank"] = get_popularity_rank(ak)
    except Exception as error:
        result["errors"]["popularityRank"] = str(error)

    try:
        result["industrySectors"] = get_industry_sectors(ak)
    except Exception as error:
        result["errors"]["industrySectors"] = str(error)

    try:
        result["limitPools"] = get_limit_pools(ak)
    except Exception as error:
        result["errors"]["limitPools"] = str(error)

    print(json.dumps({"status": "success", "data": result}, ensure_ascii=False), flush=True)


def get_dragon_tiger(ak):
    end_date = date.today()
    start_date = end_date - timedelta(days=DRAGON_TIGER_LOOKBACK_DAYS)
    with redirect_stdout(io.StringIO()):
        frame = ak.stock_lhb_detail_em(
            start_date=start_date.strftime("%Y%m%d"),
            end_date=end_date.strftime("%Y%m%d"),
        )
    if frame is None or frame.empty:
        return []

    frame = frame.sort_values(["上榜日", "龙虎榜净买额"], ascending=[False, False]).head(DRAGON_TIGER_LIMIT)
    return [
        {
            "code": as_text(row.get("代码")),
            "name": as_text(row.get("名称")),
            "listedDate": as_text(row.get("上榜日")),
            "reason": as_text(row.get("上榜原因")),
            "changePct": as_number(row.get("涨跌幅")),
            "netAmount": as_number(row.get("龙虎榜净买额")),
        }
        for _, row in frame.iterrows()
    ]


def get_fund_flow():
    """获取主力净流入前列，避免 AKShare 全量分页导致首页请求超时。"""
    response = requests.get(
        EASTMONEY_FUND_FLOW_URL,
        params={
            "fid": "f62",
            "po": "1",
            "pz": FUND_FLOW_LIMIT,
            "pn": "1",
            "np": "1",
            "fltt": "2",
            "invt": "2",
            "ut": "b2884a393a59ad64002292a3e90d46a5",
            "fs": EASTMONEY_FUND_FLOW_FILTER,
            "fields": EASTMONEY_FUND_FLOW_FIELDS,
        },
        timeout=REQUEST_TIMEOUT_SECONDS,
    )
    response.raise_for_status()
    records = response.json().get("data", {}).get("diff", [])
    return [
        {
            "code": as_text(row.get("f12")),
            "name": as_text(row.get("f14")),
            "sector": as_text(row.get("f100")),
            "price": as_number(row.get("f2")),
            "changePct": as_number(row.get("f3")),
            "netAmount": as_number(row.get("f62")),
            "netRatio": as_number(row.get("f184")),
        }
        for row in records
    ]


def get_popularity_rank(ak):
    with redirect_stdout(io.StringIO()):
        frame = ak.stock_hot_rank_em()
    if frame is None or frame.empty:
        return []

    frame = frame.sort_values("当前排名").head(POPULARITY_RANK_LIMIT)
    return [
        {
            "rank": as_number(row.get("当前排名")),
            "code": as_text(row.get("代码")),
            "name": as_text(row.get("股票名称")),
            "price": as_number(row.get("最新价")),
            "changePct": as_number(row.get("涨跌幅")),
        }
        for _, row in frame.iterrows()
    ]


def get_industry_sectors(ak):
    with redirect_stdout(io.StringIO()):
        frame = ak.stock_fund_flow_industry(symbol="即时")
    if frame is None or frame.empty:
        return []

    frame = frame.sort_values("净额", ascending=False).head(INDUSTRY_LIMIT)
    return [
        {
            "name": as_text(row.get("行业")),
            "changePct": as_number(row.get("行业-涨跌幅")),
            "netAmount": as_number(row.get("净额")) * 100000000,
            "leadingStock": as_text(row.get("领涨股")),
            "leadingStockChangePct": as_number(row.get("领涨股-涨跌幅")),
        }
        for _, row in frame.iterrows()
    ]


def get_limit_pools(ak):
    """获取最近交易日的涨停、跌停、强势和炸板股池。"""
    trade_date = get_latest_trade_date(ak)
    pool_fetchers = {
        "limitUp": (ak.stock_zt_pool_em, "连板数"),
        "limitDown": (ak.stock_zt_pool_dtgc_em, "连续跌停"),
        "strong": (ak.stock_zt_pool_strong_em, "入选理由"),
        "broken": (ak.stock_zt_pool_zbgc_em, "炸板次数"),
    }
    result = {"tradeDate": trade_date, "limitUp": [], "limitDown": [], "strong": [], "broken": [], "errors": {}}
    for pool_name, (fetcher, detail_column) in pool_fetchers.items():
        try:
            with redirect_stdout(io.StringIO()):
                frame = fetcher(date=trade_date)
            result[pool_name] = normalize_limit_pool(frame, detail_column)
        except Exception as error:
            result["errors"][pool_name] = str(error)
    return result


def get_latest_trade_date(ak):
    with redirect_stdout(io.StringIO()):
        trade_calendar = ak.tool_trade_date_hist_sina()
    trade_dates = [item for item in trade_calendar["trade_date"].tolist() if item <= date.today()]
    if not trade_dates:
        raise ValueError("未获取到有效交易日")
    return max(trade_dates).strftime("%Y%m%d")


def normalize_limit_pool(frame, detail_column):
    if frame is None or frame.empty:
        return []
    return [
        {
            "code": as_text(row.get("代码")),
            "name": as_text(row.get("名称")),
            "industry": as_text(row.get("所属行业")),
            "price": as_number(row.get("最新价")),
            "changePct": as_number(row.get("涨跌幅")),
            "detail": as_text(row.get(detail_column)),
        }
        for _, row in frame.head(LIMIT_POOL_LIMIT).iterrows()
    ]


def as_text(value):
    if value is None or is_missing(value):
        return ""
    return str(value)


def as_number(value):
    if value is None or is_missing(value):
        return 0
    try:
        return float(value)
    except (TypeError, ValueError):
        return 0


def is_missing(value):
    return isinstance(value, float) and math.isnan(value)


if __name__ == "__main__":
    main()
