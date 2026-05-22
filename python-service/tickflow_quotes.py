#!/usr/bin/env python3
"""获取TickFlow实时行情数据，输出JSON到stdout。

由Spring Boot通过子进程调用，120秒超时。

Usage:
    python tickflow_quotes.py --api-key KEY --symbols 000001.SZ
"""
import argparse
import json
import os
import signal
import sys
from concurrent.futures import ThreadPoolExecutor, TimeoutError as FuturesTimeoutError
from functools import wraps

_script_dir = os.path.dirname(os.path.abspath(__file__))
sys.path = [p for p in sys.path if p != _script_dir]
from tickflow import TickFlow
sys.path.insert(0, _script_dir)

TIMEOUT_SECONDS = 120


class ScriptTimeoutError(Exception):
    """自定义超时异常"""
    pass


def timeout_handler(signum, frame):
    raise ScriptTimeoutError(f"请求超时（{TIMEOUT_SECONDS}秒）")


def with_timeout(func):
    """装饰器：为函数设置超时时间"""
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
            signal.signal(signal.SIGALRM, timeout_handler)
            signal.alarm(TIMEOUT_SECONDS)
            try:
                return func(*args, **kwargs)
            finally:
                signal.alarm(0)
    return wrapper


@with_timeout
def main():
    parser = argparse.ArgumentParser(description="TickFlow 实时行情")
    parser.add_argument("--api-key", required=True, help="TickFlow API key")
    parser.add_argument("--symbols", required=True, help="股票代码，多个用逗号分隔")
    args = parser.parse_args()

    tf = TickFlow(api_key=args.api_key)
    quotes = tf.quotes.get(symbols=args.symbols.split(","))
    result = []
    for q in quotes:
        ext = q.get("ext", {}) or {}
        result.append({
            "close": q.get("close"),
            "open": q.get("open"),
            "high": q.get("high"),
            "low": q.get("low"),
            "volume": q.get("volume"),
            "last_price": q.get("last_price"),
            "prev_close": q.get("prev_close"),
            "amount": q.get("amount"),
            "timestamp": q.get("timestamp"),
            "ext": {
                "change_pct": ext.get("change_pct"),
                "change_amount": ext.get("change_amount"),
                "turnover_rate": ext.get("turnover_rate"),
                "amplitude": ext.get("amplitude"),
                "volume_ratio": ext.get("volume_ratio"),
            },
        })
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
