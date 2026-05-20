#!/usr/bin/env python3
"""Fetch chip data from Sina (via AKShare), output JSON to stdout.

Called by Spring Boot via subprocess. Times out after 120 seconds.

Usage:
    python akshare.py chip --symbol 601138
"""
import argparse
import json
import math
import os
import sys
from concurrent.futures import ThreadPoolExecutor, TimeoutError as FuturesTimeoutError
from datetime import datetime, timedelta
from functools import wraps

_script_dir = os.path.dirname(os.path.abspath(__file__))
sys.path = [p for p in sys.path if p != _script_dir]

TIMEOUT_SECONDS = 120
FACTOR = 150
RANGE_DAYS = 120


class ScriptTimeoutError(Exception):
    pass


class CyqCalculator:

    def __init__(self, records):
        self.records = records

    def compute(self, index):
        kdata = self._lookback(index)
        if not kdata:
            raise ValueError("K线数据不足")

        maxprice = max(e["high"] for e in kdata)
        minprice = min(e["low"] for e in kdata)
        accuracy = max(0.01, (maxprice - minprice) / (FACTOR - 1))
        xdata = [0.0] * FACTOR

        for ele in kdata:
            o, c, h, l = ele["open"], ele["close"], ele["high"], ele["low"]
            avg = (o + c + h + l) / 4
            tr = min(1.0, ele["turnover"] / 100)
            H = int((h - minprice) / accuracy)
            L = math.ceil((l - minprice) / accuracy)
            gv = FACTOR - 1 if h == l else 2 / (h - l)
            gi = int((avg - minprice) / accuracy)

            for i in range(FACTOR):
                xdata[i] *= (1 - tr)

            if h == l:
                xdata[gi] += gv * tr / 2
            else:
                for j in range(L, H + 1):
                    cp = minprice + accuracy * j
                    if cp <= avg:
                        xdata[j] += gv * tr if abs(avg - l) < 1e-8 else (cp - l) / (avg - l) * gv * tr
                    else:
                        xdata[j] += gv * tr if abs(h - avg) < 1e-8 else (h - cp) / (h - avg) * gv * tr

        cp = kdata[-1]["close"]
        total = sum(xdata)

        def cost(chip):
            s = 0.0
            for i in range(FACTOR):
                if s + xdata[i] > chip:
                    return minprice + i * accuracy
                s += xdata[i]
            return minprice + (FACTOR - 1) * accuracy

        def bp(price):
            below = sum(xdata[i] for i in range(FACTOR) if price >= minprice + i * accuracy)
            return below / total if total else 0.0

        def pct(p):
            ps = [(1 - p) / 2, (1 + p) / 2]
            pr = [cost(total * ps[0]), cost(total * ps[1])]
            conc = 0.0 if (pr[0] + pr[1]) == 0 else (pr[1] - pr[0]) / (pr[0] + pr[1])
            return [round(pr[0], 2), round(pr[1], 2)], round(conc, 4)

        b = bp(cp)
        ac = cost(total * 0.5)
        pr90, conc90 = pct(0.9)
        pr70, conc70 = pct(0.7)

        return {
            "date": str(kdata[-1]["date"]),
            "profit_ratio": round(b * 100, 2),
            "loss_ratio": round((1 - b) * 100, 2),
            "avg_cost": round(ac, 2),
            "cost_90_low": pr90[0],
            "cost_90_high": pr90[1],
            "concentration_90": conc90,
            "cost_70_low": pr70[0],
            "cost_70_high": pr70[1],
            "concentration_70": conc70,
        }

    def _lookback(self, index):
        start = max(0, index - RANGE_DAYS + 1)
        return self.records[start:index + 1]


def timeout_handler(signum, frame):
    raise ScriptTimeoutError(f"请求超时（{TIMEOUT_SECONDS}秒）")


def with_timeout(func):
    @wraps(func)
    def wrapper(*args, **kwargs):
        if sys.platform == "win32":
            executor = ThreadPoolExecutor(max_workers=1)
            future = executor.submit(func, *args, **kwargs)
            try:
                return future.result(timeout=TIMEOUT_SECONDS)
            except FuturesTimeoutError:
                raise ScriptTimeoutError(f"请求超时（{TIMEOUT_SECONDS}秒）")
            finally:
                executor.shutdown(wait=False)
        else:
            import signal
            signal.signal(signal.SIGALRM, timeout_handler)
            signal.alarm(TIMEOUT_SECONDS)
            try:
                return func(*args, **kwargs)
            finally:
                signal.alarm(0)
    return wrapper


@with_timeout
def cmd_chip(args):
    os.environ["TQDM_DISABLE"] = "1"
    try:
        import akshare as ak
    except ImportError:
        print(json.dumps({"status": "error", "message": "AKShare 未安装"}, ensure_ascii=False))
        return

    end_date = datetime.now().strftime("%Y%m%d")
    start_date = (datetime.now() - timedelta(days=420)).strftime("%Y%m%d")
    market_prefix = "sh" if args.symbol.startswith(("6", "9")) else "sz"
    symbol_full = f"{market_prefix}{args.symbol}"

    df = ak.stock_zh_a_daily(symbol=symbol_full, start_date=start_date, end_date=end_date, adjust="")
    if df.empty:
        print(json.dumps({"status": "error", "message": "未获取到K线数据"}, ensure_ascii=False))
        return

    records = []
    for _, row in df.iterrows():
        records.append({
            "date": row["date"],
            "open": float(row["open"]),
            "high": float(row["high"]),
            "low": float(row["low"]),
            "close": float(row["close"]),
            "volume": float(row["volume"]),
            "turnover": float(row["turnover"]) * 100,
        })

    if len(records) < 30:
        print(json.dumps({"status": "error", "message": f"K线数据不足（{len(records)}条）"}, ensure_ascii=False))
        return

    calculator = CyqCalculator(records)
    result = calculator.compute(len(records) - 1)
    print(json.dumps({"status": "success", "data": result}, ensure_ascii=False))


@with_timeout
def cmd_index(args):
    os.environ["TQDM_DISABLE"] = "1"
    try:
        import akshare as ak
    except ImportError:
        print(json.dumps({"status": "error", "message": "AKShare 未安装"}, ensure_ascii=False))
        return

    target_date = args.date
    symbols = args.symbols.split(",")

    result = {}
    for sym in symbols:
        sym = sym.strip()
        if not sym:
            continue
        try:
            df = ak.stock_zh_index_daily(symbol=sym)
            if df.empty:
                result[sym] = {"error": "未获取到数据"}
                continue
            df = df.tail(30)
            records = []
            for _, row in df.iterrows():
                records.append({
                    "date": str(row["date"]),
                    "open": float(row["open"]),
                    "close": float(row["close"]),
                    "high": float(row["high"]),
                    "low": float(row["low"]),
                    "volume": float(row["volume"]),
                })

            # 如果目标日期是当天，获取实时行情数据并确保当天记录存在
            today_str = datetime.now().strftime("%Y-%m-%d")
            if target_date and target_date == today_str:
                try:
                    spot_df = ak.stock_zh_index_spot_sina()
                    spot_match = spot_df[spot_df["代码"] == sym]
                    if not spot_match.empty:
                        sr = spot_match.iloc[0]
                        today_record = {
                            "date": today_str,
                            "open": float(sr["今开"]),
                            "close": float(sr["最新价"]),
                            "high": float(sr["最高"]),
                            "low": float(sr["最低"]),
                            "volume": float(sr["成交量"]),
                        }
                        found = False
                        for i, r in enumerate(records):
                            if r["date"] == today_str:
                                records[i] = today_record
                                found = True
                                break
                        if not found:
                            records.append(today_record)
                except Exception as e:
                    pass

            if target_date:
                matched = [r for r in records if r["date"] == target_date]
                if matched:
                    latest = matched[-1]
                    idx = records.index(latest)
                    prev = records[idx - 1] if idx > 0 else {}
                else:
                    latest = records[-1]
                    prev = records[-2] if len(records) >= 2 else {}
            else:
                latest = records[-1] if records else {}
                prev = records[-2] if len(records) >= 2 else {}
            change_pct = 0.0
            if prev.get("close") and prev["close"] != 0:
                change_pct = round((latest["close"] - prev["close"]) / prev["close"] * 100, 2)
            result[sym] = {
                "records": records,
                "latest": latest,
                "changePct": change_pct,
                "targetDate": target_date or str(latest.get("date", ""))
            }
        except Exception as e:
            result[sym] = {"error": str(e)}
    print(json.dumps({"status": "success", "data": result}, ensure_ascii=False))


def main():
    parser = argparse.ArgumentParser(description="AKShare 筹码数据 CLI")
    parser.add_argument("--api-key", default=os.environ.get("TICKFLOW_API_KEY", ""),
                        help="(保留参数)")
    subparsers = parser.add_subparsers(dest="command", required=True)
    p = subparsers.add_parser("chip", help="筹码分布")
    p.add_argument("--symbol", required=True, help="股票代码")
    p.set_defaults(func=cmd_chip)
    p_idx = subparsers.add_parser("index", help="大盘指数数据")
    p_idx.add_argument("--symbols", required=True, help="指数代码，多个用逗号分隔，如 sh000001,sz399001")
    p_idx.add_argument("--date", default=None, help="目标日期，格式 yyyy-MM-dd，用于指定复盘日期")
    p_idx.set_defaults(func=cmd_index)

    args = parser.parse_args()
    try:
        args.func(args)
    except ScriptTimeoutError as e:
        print(json.dumps({"status": "error", "message": str(e)}, ensure_ascii=False))
        os._exit(1)
    except Exception as e:
        print(json.dumps({"status": "error", "message": str(e)}, ensure_ascii=False))
        os._exit(1)


if __name__ == "__main__":
    main()
