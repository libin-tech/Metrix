#!/usr/bin/env python3
"""获取AKShare大盘指数实时行情，输出JSON到stdout。

由Spring Boot通过子进程调用，60秒超时。

Usage:
    python akshare_index_spot.py --symbols sh000001,sz399001
"""
import argparse
import json
import os
import sys
from concurrent.futures import ThreadPoolExecutor, TimeoutError as FuturesTimeoutError
from functools import wraps

_script_dir = os.path.dirname(os.path.abspath(__file__))
sys.path = [p for p in sys.path if p != _script_dir]

TIMEOUT_SECONDS = 60


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
    parser = argparse.ArgumentParser(description="AKShare 大盘指数实时行情")
    parser.add_argument("--symbols", required=True, help="指数代码，多个用逗号分隔，如 sh000001,sz399001")
    args = parser.parse_args()

    os.environ["TQDM_DISABLE"] = "1"
    try:
        import akshare as ak
    except ImportError:
        print(json.dumps({"status": "error", "message": "AKShare 未安装"}, ensure_ascii=False), flush=True)
        return

    symbols = [s.strip() for s in args.symbols.split(",") if s.strip()]

    try:
        spot_df = ak.stock_zh_index_spot_sina()
        if spot_df.empty:
            print(json.dumps({"status": "error", "message": "未获取到实时行情数据"}, ensure_ascii=False), flush=True)
            return

        result = {}
        for sym in symbols:
            match = spot_df[spot_df["代码"] == sym]
            if match.empty:
                result[sym] = {"error": "未找到该指数"}
                continue
            sr = match.iloc[0]
            prev_close = float(sr.get("昨收", 0))
            current = float(sr.get("最新价", 0))
            change_pct = round((current - prev_close) / prev_close * 100, 2) if prev_close else 0.0
            change = round(current - prev_close, 2) if prev_close else 0.0
            result[sym] = {
                "name": str(sr.get("名称", "")),
                "current": current,
                "change": change,
                "changePct": change_pct,
                "open": float(sr.get("今开", 0)),
                "high": float(sr.get("最高", 0)),
                "low": float(sr.get("最低", 0)),
                "volume": float(sr.get("成交量", 0)),
                "amount": float(sr.get("成交额", 0)),
                "prevClose": prev_close,
            }

        print(json.dumps({"status": "success", "data": result}, ensure_ascii=False), flush=True)

    except Exception as e:
        print(json.dumps({"status": "error", "message": str(e)}, ensure_ascii=False), flush=True)


if __name__ == "__main__":
    try:
        main()
    except ScriptTimeoutError as e:
        print(json.dumps({"status": "error", "message": str(e)}, ensure_ascii=False), flush=True)
        os._exit(1)
    except Exception as e:
        print(json.dumps({"status": "error", "message": str(e)}, ensure_ascii=False), flush=True)
        os._exit(1)
