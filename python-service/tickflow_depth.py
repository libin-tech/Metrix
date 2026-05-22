#!/usr/bin/env python3
"""获取TickFlow五档深度数据，输出JSON到stdout。

由Spring Boot通过子进程调用，120秒超时。

Usage:
    python tickflow_depth.py --api-key KEY --symbol 000001.SZ
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
    parser = argparse.ArgumentParser(description="TickFlow 五档深度")
    parser.add_argument("--api-key", required=True, help="TickFlow API key")
    parser.add_argument("--symbol", required=True, help="股票代码")
    args = parser.parse_args()

    tf = TickFlow(api_key=args.api_key)
    depth = tf.depth.get(args.symbol)
    result = {
        "ask_prices": depth.get("ask_prices", []),
        "ask_volumes": depth.get("ask_volumes", []),
        "bid_prices": depth.get("bid_prices", []),
        "bid_volumes": depth.get("bid_volumes", []),
    }
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
