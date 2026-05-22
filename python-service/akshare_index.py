#!/usr/bin/env python3
"""获取AKShare大盘指数数据，输出JSON到stdout。

由Spring Boot通过子进程调用，120秒超时。

Usage:
    python akshare_index.py --symbols sh000001,sz399001 --date 2025-01-01
"""
import argparse
import json
import os
import sys
from concurrent.futures import ThreadPoolExecutor, TimeoutError as FuturesTimeoutError
from datetime import datetime
from functools import wraps

_script_dir = os.path.dirname(os.path.abspath(__file__))
sys.path = [p for p in sys.path if p != _script_dir]

TIMEOUT_SECONDS = 120


class ScriptTimeoutError(Exception):
    pass


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
def main():
    parser = argparse.ArgumentParser(description="AKShare 大盘指数数据")
    parser.add_argument("--symbols", required=True, help="指数代码，多个用逗号分隔，如 sh000001,sz399001")
    parser.add_argument("--date", default=None, help="目标日期，格式 yyyy-MM-dd，用于指定复盘日期")
    args = parser.parse_args()

    os.environ["TQDM_DISABLE"] = "1"
    try:
        import akshare as ak
    except ImportError:
        print(json.dumps({"status": "error", "message": "AKShare 未安装"}, ensure_ascii=False), flush=True)
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
                except Exception:
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
    print(json.dumps({"status": "success", "data": result}, ensure_ascii=False), flush=True)


if __name__ == "__main__":
    try:
        main()
    except ScriptTimeoutError as e:
        print(json.dumps({"status": "error", "message": str(e)}, ensure_ascii=False), flush=True)
        os._exit(1)
    except Exception as e:
        print(json.dumps({"status": "error", "message": str(e)}, ensure_ascii=False), flush=True)
        os._exit(1)
