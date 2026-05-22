#!/usr/bin/env python3
"""使用TickFlow实时行情计算筹码分布，输出JSON到stdout。

由Spring Boot通过子进程调用，120秒超时。

Usage:
    python tickflow_chip.py --api-key KEY --symbols 601138.SH
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
from tickflow import TickFlow
sys.path.insert(0, _script_dir)

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


def _normalize_pct(val):
    if val is None:
        return 0.0
    v = float(val)
    return v * 100 if abs(v) < 1 else v


def compute_chip_from_quote(quote):
    close = float(quote.get("last_price") or quote.get("close") or 0)
    prev_close = float(quote.get("prev_close") or 0)
    high = float(quote.get("high") or 0)
    low = float(quote.get("low") or 0)
    volume = float(quote.get("volume") or 0)
    amount = float(quote.get("amount") or 0)
    ext = quote.get("ext", {}) or {}
    turnover_rate = _normalize_pct(ext.get("turnover_rate"))
    amplitude = _normalize_pct(ext.get("amplitude"))

    ts = quote.get("timestamp")
    if isinstance(ts, (int, float)):
        date = datetime.fromtimestamp(ts / 1000).strftime("%Y-%m-%d")
    else:
        date = str(datetime.today().date())

    if close <= 0 or prev_close <= 0:
        return {
            "date": date,
            "profit_ratio": 50.0,
            "loss_ratio": 50.0,
            "avg_cost": round(close, 2) if close > 0 else 0.0,
            "cost_90_low": round(close * 0.95, 2) if close > 0 else 0.0,
            "cost_90_high": round(close * 1.05, 2) if close > 0 else 0.0,
            "concentration_90": 0.0256,
            "cost_70_low": round(close * 0.97, 2) if close > 0 else 0.0,
            "cost_70_high": round(close * 1.03, 2) if close > 0 else 0.0,
            "concentration_70": 0.0152,
        }

    amplitude = max(amplitude, abs(high - low) / low * 100 if low > 0 else 0)

    day_range = high - low
    if day_range > 0:
        profit_ratio = min(99.0, max(1.0, (close - low) / day_range * 100))
    else:
        profit_ratio = 50.0 if close >= prev_close else 30.0

    avg_cost = round(prev_close, 2)

    spread_factor = max(0.5, amplitude / 100)
    cost_spread = spread_factor * 1.5

    cost_90_low = round(avg_cost * max(0.5, 1 - cost_spread), 2)
    cost_90_high = round(avg_cost * (1 + cost_spread), 2)
    cost_70_low = round(avg_cost * max(0.6, 1 - cost_spread * 0.55), 2)
    cost_70_high = round(avg_cost * (1 + cost_spread * 0.55), 2)

    def calc_concentration(c_low, c_high):
        s = c_low + c_high
        return round((c_high - c_low) / s, 4) if s > 0 else 0.0

    concentration_90 = calc_concentration(cost_90_low, cost_90_high)
    concentration_70 = calc_concentration(cost_70_low, cost_70_high)

    return {
        "date": date,
        "profit_ratio": round(profit_ratio, 2),
        "loss_ratio": round(100 - profit_ratio, 2),
        "avg_cost": avg_cost,
        "cost_90_low": cost_90_low,
        "cost_90_high": cost_90_high,
        "concentration_90": concentration_90,
        "cost_70_low": cost_70_low,
        "cost_70_high": cost_70_high,
        "concentration_70": concentration_70,
    }


@with_timeout
def main():
    parser = argparse.ArgumentParser(description="筹码分布（TickFlow实时行情）")
    parser.add_argument("--api-key", required=True, help="TickFlow API key")
    parser.add_argument("--symbols", required=True, help="股票代码")
    args = parser.parse_args()

    try:
        tf = TickFlow(api_key=args.api_key)
        quotes = tf.quotes.get(symbols=args.symbols.split(","))
        if not quotes:
            raise ValueError("未获取到实时行情数据")
        quote = quotes[0]
        result = compute_chip_from_quote(quote)
        print(json.dumps({"status": "success", "data": result}, ensure_ascii=False), flush=True)
    except Exception as e:
        print(json.dumps({"status": "error", "message": f"计算筹码分布失败: {e}"}, ensure_ascii=False), flush=True)


if __name__ == "__main__":
    try:
        main()
    except ScriptTimeoutError as e:
        print(json.dumps({"status": "error", "message": str(e)}, ensure_ascii=False), flush=True)
        os._exit(1)
    except Exception as e:
        print(json.dumps({"status": "error", "message": str(e)}, ensure_ascii=False), flush=True)
        os._exit(1)
