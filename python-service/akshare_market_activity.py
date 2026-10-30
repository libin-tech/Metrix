#!/usr/bin/env python3
"""新浪沪深北 A 股涨跌统计 + 东方财富完整涨跌停池，超时由后端管理。"""
import io
import json
from contextlib import redirect_stdout, redirect_stderr
from datetime import datetime, time
from zoneinfo import ZoneInfo

import pandas as pd

MARKET_ZONE = ZoneInfo("Asia/Shanghai")
STOCK_CODE_PATTERN = r"(?:0|3|6)\d{5}"
BREADTH_CODE_PATTERN = r"(?:(?:0|3|4|6|8)\d{5}|920\d{3})"


def normalizeCodes(series):
    """统一新浪带交易所前缀和东财纯数字股票代码。"""
    return series.astype(str).str.lower().str.replace(r"^(sh|sz|bj)", "", regex=True)


def countBreadth(frame):
    """统计有有效成交报价的沪深北 A 股，零成交记录不作为平盘。"""
    if frame is None or frame.empty:
        raise ValueError("新浪全市场行情为空")
    quotes = frame.copy()
    quotes["代码"] = normalizeCodes(quotes["代码"])
    quotes = quotes[quotes["代码"].str.fullmatch(BREADTH_CODE_PATTERN)].drop_duplicates("代码")
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


def sentimentDetails(ak, tradeDate, limitUpFrame):
    """可选情绪指标失败独立降级，不让非关键数据阻断同一行情快照。"""
    streak = None
    if limitUpFrame.empty:
        streak = 0
    elif "连板数" in limitUpFrame.columns:
        values = pd.to_numeric(limitUpFrame["连板数"], errors="coerce")
        valid = values.notna().all() and (values >= 1).all() and ~values.isin([float("inf"), float("-inf")]).any()
        streak = int(values.max()) if valid else None
    brokenRate = None
    failures = []
    try:
        broken = ak.stock_zt_pool_zbgc_em(date=tradeDate)
        brokenCount = countPool(broken)
        upCodes = set(normalizeCodes(limitUpFrame["代码"])) if not limitUpFrame.empty else set()
        brokenCodes = set(normalizeCodes(broken["代码"])) if not broken.empty else set()
        validCodes = pd.Series(list(upCodes | brokenCodes), dtype=str)
        touched = int(validCodes.str.fullmatch(STOCK_CODE_PATTERN).sum())
        brokenRate = round(brokenCount / touched * 100, 2) if touched else None
    except Exception as error:
        failures.append(f"brokenPool: {type(error).__name__}")
    return {"streakHeight": streak, "brokenRate": brokenRate, "optionalFailures": failures}


def quoteSnapshot(frame):
    """同一全市场快照用于持仓估值；缺失成交额不以零替代。"""
    quotes = frame.copy()
    quotes["代码"] = normalizeCodes(quotes["代码"])
    quotes = quotes[quotes["代码"].str.fullmatch(BREADTH_CODE_PATTERN)].drop_duplicates("代码")
    numeric = quotes[["最新价", "昨收", "成交量"]].apply(pd.to_numeric, errors="coerce")
    valid = numeric.notna().all(axis=1) & ~numeric.isin([float("inf"), float("-inf")]).any(axis=1)
    valid &= (numeric > 0).all(axis=1)
    result = {}
    for index in quotes.index[valid]:
        code = quotes.at[index, "代码"]
        exchange = {"0": "SZ", "3": "SZ", "6": "SH"}.get(code[0], "BJ")
        result[f"{code}.{exchange}"] = {"price": float(numeric.at[index, "最新价"]),
                                         "prevClose": float(numeric.at[index, "昨收"])}
    amounts = pd.to_numeric(quotes.get("成交额", pd.Series(index=quotes.index, dtype=float)), errors="coerce")
    amountValid = amounts.notna() & (amounts >= 0) & ~amounts.isin([float("inf"), float("-inf")])
    total = float(amounts.sum()) if len(quotes) > 0 and amountValid.all() else None
    return result, total


def latestTradeDate(calendar, now):
    """非交易日和开盘前取上一已开始的交易日。"""
    dates = pd.to_datetime(calendar["trade_date"], errors="raise").dt.date
    eligible = dates[(dates < now.date()) | ((dates == now.date()) & (now.time() >= time(9, 30)))]
    if eligible.empty:
        raise ValueError("交易日历无有效日期")
    return max(eligible).strftime("%Y%m%d")


def fetchActivity(ak, now):
    """三路数据全部成功才返回快照，失败交由后端保留上次成功结果。"""
    calendar = ak.tool_trade_date_hist_sina()
    tradeDate = latestTradeDate(calendar, now)
    priorDates = sorted(pd.to_datetime(calendar["trade_date"]).dt.strftime("%Y%m%d"))
    previousDate = max((date for date in priorDates if date < tradeDate), default=None)
    frame = ak.stock_zh_a_spot()
    result = countBreadth(frame)
    quotes, turnover = quoteSnapshot(frame)
    limitUpFrame = ak.stock_zt_pool_em(date=tradeDate)
    result.update({"limitUp": countPool(limitUpFrame),
                   "limitDown": countPool(ak.stock_zt_pool_dtgc_em(date=tradeDate)),
                   "statDate": tradeDate, "fetchedAt": now.isoformat(), "stale": False,
                   "previousTradeDate": previousDate,
                   "sessionPhase": "INTRADAY" if tradeDate == now.strftime("%Y%m%d") and now.time() < time(15, 5) else "CLOSED",
                   "totalTurnover": turnover, "quotes": quotes,
                   "source": "sina+eastmoney", "scope": "SH_SZ_BJ_A", "limitScope": "SH_SZ_A"})
    result.update(sentimentDetails(ak, tradeDate, limitUpFrame))
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
