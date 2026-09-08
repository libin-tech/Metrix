#!/usr/bin/env python3
"""新浪沪深 A 股涨跌统计 + 东方财富完整涨跌停池，超时由后端管理。"""
import io
import json
from contextlib import redirect_stdout, redirect_stderr
from datetime import datetime, time
from zoneinfo import ZoneInfo

import pandas as pd

MARKET_ZONE = ZoneInfo("Asia/Shanghai")
STOCK_CODE_PATTERN = r"(?:0|3|6)\d{5}"


def normalizeCodes(series):
    """统一新浪带交易所前缀和东财纯数字股票代码。"""
    return series.astype(str).str.lower().str.replace(r"^(sh|sz|bj)", "", regex=True)


def countBreadth(frame):
    """统计有有效成交报价的沪深 A 股，零成交记录不作为平盘。"""
    if frame is None or frame.empty:
        raise ValueError("新浪全市场行情为空")
    quotes = frame.copy()
    quotes["代码"] = normalizeCodes(quotes["代码"])
    quotes = quotes[quotes["代码"].str.fullmatch(STOCK_CODE_PATTERN)].drop_duplicates("代码")
    numeric = quotes[["最新价", "昨收", "成交量", "涨跌额"]].apply(pd.to_numeric, errors="coerce")
    valid = numeric.notna().all(axis=1) & ~numeric.isin([float("inf"), float("-inf")]).any(axis=1)
    trading = numeric[valid & (numeric["最新价"] > 0) & (numeric["昨收"] > 0) & (numeric["成交量"] > 0)]
    if trading.empty:
        raise ValueError("新浪未返回有效交易报价")
    up = int((trading["涨跌额"] > 0).sum())
    down = int((trading["涨跌额"] < 0).sum())
    flat = int((trading["涨跌额"] == 0).sum())
    total = len(trading)
    return {"up": up, "down": down, "flat": flat, "sampleCount": total,
            "excludedCount": len(quotes) - total,
            "upRatio": round(up / total * 100, 2),
            "downRatio": round(down / total * 100, 2)}


def countPool(frame):
    """对完整股池计数，不使用首页截取的前 20 条列表。"""
    if frame is None:
        raise ValueError("东方财富股池响应缺失")
    if frame.empty:
        return 0
    codes = normalizeCodes(frame["代码"])
    return int(codes[codes.str.fullmatch(STOCK_CODE_PATTERN)].nunique())


def latestTradeDate(calendar, now):
    """非交易日和开盘前取上一已开始的交易日。"""
    dates = pd.to_datetime(calendar["trade_date"], errors="raise").dt.date
    eligible = dates[(dates < now.date()) | ((dates == now.date()) & (now.time() >= time(9, 30)))]
    if eligible.empty:
        raise ValueError("交易日历无有效日期")
    return max(eligible).strftime("%Y%m%d")


def fetchActivity(ak, now):
    """三路数据全部成功才返回快照，失败交由后端保留上次成功结果。"""
    tradeDate = latestTradeDate(ak.tool_trade_date_hist_sina(), now)
    result = countBreadth(ak.stock_zh_a_spot())
    result.update({"limitUp": countPool(ak.stock_zt_pool_em(date=tradeDate)),
                   "limitDown": countPool(ak.stock_zt_pool_dtgc_em(date=tradeDate)),
                   "statDate": tradeDate, "fetchedAt": now.isoformat(), "stale": False,
                   "source": "sina+eastmoney", "scope": "SH_SZ_A"})
    return result


def main():
    try:
        # 库的进度输出不能污染 stdout JSON；后端会合并进程输出流。
        with redirect_stdout(io.StringIO()), redirect_stderr(io.StringIO()):
            import akshare as ak
            data = fetchActivity(ak, datetime.now(MARKET_ZONE))
        result = {"status": "success", "data": data}
    except Exception as error:
        result = {"status": "error", "message": f"市场涨跌统计获取失败: {error}"}
    print(json.dumps(result, ensure_ascii=False, allow_nan=False), flush=True)


if __name__ == "__main__":
    main()
