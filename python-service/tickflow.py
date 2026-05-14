#!/usr/bin/env python3
"""Fetch market data from TickFlow Python SDK, output JSON to stdout.

Called by Spring Boot via subprocess. JSON output format matches what
the original Java HTTP-parsing code expects. Times out after 60 seconds.

Usage:
    python tickflow.py --api-key KEY quotes --symbols 000001.SZ
    python tickflow.py --api-key KEY depth --symbol 000001.SZ
    python tickflow.py --api-key KEY klines --symbol 000001.SZ --period 1d --count 60
"""
import argparse
import json
import os
import signal
import sys
from concurrent.futures import ThreadPoolExecutor, TimeoutError as FuturesTimeoutError
from functools import wraps

# Remove script dir from sys.path so "tickflow" resolves to the installed package,
# not this script file itself.
_script_dir = os.path.dirname(os.path.abspath(__file__))
sys.path = [p for p in sys.path if p != _script_dir]
from tickflow import TickFlow
sys.path.insert(0, _script_dir)

TIMEOUT_SECONDS = 120


class ScriptTimeoutError(Exception):
    """自定义超时异常（兼容 Python < 3.11，其未内置 TimeoutError）"""
    pass


def timeout_handler(signum, frame):
    raise ScriptTimeoutError(f"请求超时（{TIMEOUT_SECONDS}秒）")


def with_timeout(func):
    """装饰器：为函数设置超时时间（Unix 用 signal，Windows 用线程）"""
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
def cmd_quotes(args):
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
    print(json.dumps({"status": "success", "data": result}, ensure_ascii=False))


@with_timeout
def cmd_depth(args):
    tf = TickFlow(api_key=args.api_key)
    depth = tf.depth.get(args.symbol)
    result = {
        "ask_prices": depth.get("ask_prices", []),
        "ask_volumes": depth.get("ask_volumes", []),
        "bid_prices": depth.get("bid_prices", []),
        "bid_volumes": depth.get("bid_volumes", []),
    }
    print(json.dumps({"status": "success", "data": result}, ensure_ascii=False))


@with_timeout
def cmd_klines(args):
    tf = TickFlow(api_key=args.api_key)
    klines = tf.klines.get(args.symbol, period=args.period, count=args.count)
    result = {
        "timestamp": klines.get("timestamp", []),
        "open": klines.get("open", []),
        "high": klines.get("high", []),
        "low": klines.get("low", []),
        "close": klines.get("close", []),
        "volume": klines.get("volume", []),
        "amount": klines.get("amount", []),
    }
    print(json.dumps({"status": "success", "data": result}, ensure_ascii=False))


def main():
    parser = argparse.ArgumentParser(description="TickFlow Market Data CLI")
    parser.add_argument(
        "--api-key",
        default=os.environ.get("TICKFLOW_API_KEY", ""),
        help="TickFlow API key (default: TICKFLOW_API_KEY env var)",
    )
    subparsers = parser.add_subparsers(dest="command", required=True)

    p = subparsers.add_parser("quotes", help="实时行情")
    p.add_argument("--symbols", required=True, help="股票代码，多个用逗号分隔")
    p.set_defaults(func=cmd_quotes)

    p = subparsers.add_parser("depth", help="市场深度")
    p.add_argument("--symbol", required=True, help="股票代码")
    p.set_defaults(func=cmd_depth)

    p = subparsers.add_parser("klines", help="K线数据")
    p.add_argument("--symbol", required=True, help="股票代码")
    p.add_argument("--period", default="1d", help="K线周期，如 1d, 1w")
    p.add_argument("--count", type=int, default=60, help="K线条数")
    p.set_defaults(func=cmd_klines)

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
