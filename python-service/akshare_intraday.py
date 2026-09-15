#!/usr/bin/env python3
"""当日不复权分钟行情；成交量由手统一换算为股，历史日期不回填。"""
import argparse
import contextlib
import json
import math
import re
import sys
from datetime import datetime, time, timedelta, timezone

MARKET_ZONE = timezone(timedelta(hours=8))
SHARES_PER_LOT = 100


def finiteNumber(value):
    try:
        result = float(value)
        return result if math.isfinite(result) and result >= 0 else None
    except (ValueError, TypeError):
        return None


def normalizePoints(records, now):
    points = {}
    for row in records:
        instant = datetime.fromisoformat(str(row["时间"])).replace(tzinfo=MARKET_ZONE)
        inSession = time(9, 30) <= instant.time() <= time(11, 30) or time(13) <= instant.time() <= time(15)
        if instant.date() != now.date() or instant > now or not inSession:
            continue
        price = finiteNumber(row.get("收盘"))
        if price is None or price == 0:
            continue
        volume = finiteNumber(row.get("成交量"))
        timestamp = int(instant.timestamp() * 1000)
        points[timestamp] = {"timestamp": timestamp, "time": instant.strftime("%H:%M"), "price": price,
                             "volume": None if volume is None else volume * SHARES_PER_LOT}
    return [points[key] for key in sorted(points)]


def fetchIntraday(symbol, now=None):
    if not re.fullmatch(r"[0-9]{6}\.(SH|SZ|BJ)", symbol):
        raise ValueError("请输入完整 A 股代码")
    now = now or datetime.now(MARKET_ZONE)
    date = now.date().isoformat()
    with contextlib.redirect_stdout(sys.stderr):
        import akshare as ak
        frame = ak.stock_zh_a_hist_min_em(symbol=symbol.split(".")[0], period="1", adjust="",
                                        start_date=f"{date} 09:30:00", end_date=f"{date} 15:00:00")
    points = normalizePoints(frame.to_dict("records"), now)
    return {"thscode": symbol, "date": date, "points": points, "source": "AKShare / 东方财富",
            "dataTime": points[-1]["time"] if points else None}


def main():
    parser = argparse.ArgumentParser(description="当日分时行情")
    parser.add_argument("--symbol", required=True)
    args = parser.parse_args()
    try:
        result = {"status": "success", "data": fetchIntraday(args.symbol)}
    except Exception as error:
        result = {"status": "error", "message": str(error)}
    print(json.dumps(result, ensure_ascii=False, allow_nan=False), flush=True)


if __name__ == "__main__":
    main()
