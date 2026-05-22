#!/usr/bin/env python3
"""获取TickFlow K线数据并计算MACD指标，输出JSON到stdout。

由Spring Boot通过子进程调用，120秒超时。

Usage:
    python tickflow_klines.py --api-key KEY --symbol 000001.SZ --period 1d --count 60
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


def calc_ema(data, period):
    """计算指数移动平均（EMA）

    公式：
        k = 2 / (period + 1)
        EMA[0..period-2] = data[0..period-2]（初始填充）
        EMA[period-1] = SMA(data[:period])（首个有效值为前period期的简单平均）
        EMA[i] = (data[i] - EMA[i-1]) * k + EMA[i-1]  （i >= period）

    Args:
        data: 输入价格序列
        period: EMA周期

    Returns:
        EMA序列（长度与data一致）
    """
    k = 2.0 / (period + 1)
    ema = [0.0] * len(data)
    ema[period - 1] = sum(data[:period]) / period
    for i in range(period, len(data)):
        ema[i] = (data[i] - ema[i - 1]) * k + ema[i - 1]
    for i in range(period - 1):
        ema[i] = data[i]
    return ema


def calc_macd(closes):
    """计算MACD指标并生成信号描述

    MACD由三部分组成：
        - DIF（快线）= EMA12 - EMA26
        - DEA（慢线/信号线）= EMA9(DIF)
        - MACD柱（BAR）= 2 * (DIF - DEA)

    信号规则：
        - DIF零轴位置判断中长期趋势方向
        - DIF与DEA交叉判断金叉/死叉
        - 柱状图颜色和变化判断多空动能

    Args:
        closes: 收盘价序列（至少26个数据点）

    Returns:
        dict: 包含 dif / dea / bar / signal 四个字段
              数据不足时返回零值和提示信息
    """
    if len(closes) < 26:
        return {"dif": 0, "dea": 0, "bar": 0, "signal": "数据不足26周期，无法计算MACD"}

    ema12 = calc_ema(closes, 12)
    ema26 = calc_ema(closes, 26)
    dif = [ema12[i] - ema26[i] for i in range(len(closes))]
    dea = calc_ema(dif, 9)
    bar = [2 * (dif[i] - dea[i]) for i in range(len(closes))]

    cur = len(closes) - 1
    prv = cur - 1

    # 信号生成
    parts = []
    if dif[cur] > 0:
        parts.append("DIF在零轴上方")
    elif dif[cur] < 0:
        parts.append("DIF在零轴下方")
    else:
        parts.append("DIF处于零轴")

    if prv >= 0:
        if dif[prv] < dea[prv] and dif[cur] >= dea[cur]:
            parts.append("DIF上穿DEA形成金叉")
        elif dif[prv] > dea[prv] and dif[cur] <= dea[cur]:
            parts.append("DIF下穿DEA形成死叉")
        elif dif[cur] > dea[cur]:
            parts.append("DIF在DEA上方呈多头排列")
        elif dif[cur] < dea[cur]:
            parts.append("DIF在DEA下方呈空头排列")
        else:
            parts.append("DIF与DEA粘合")
    else:
        parts.append("DIF与DEA粘合")

    if bar[cur] > 0:
        parts.append("柱状图为正值（红柱）")
        if bar[cur] > bar[prv]:
            parts.append("且放大，多头动能增强")
        else:
            parts.append("但缩小，多头动能减弱")
    elif bar[cur] < 0:
        parts.append("柱状图为负值（绿柱）")
        if bar[cur] < bar[prv]:
            parts.append("且放大，空头动能增强")
        else:
            parts.append("但缩小，空头动能减弱")
    else:
        parts.append("柱状图归零，多空平衡")

    return {
        "dif": round(dif[cur], 4),
        "dea": round(dea[cur], 4),
        "bar": round(bar[cur], 4),
        "signal": "，".join(parts),
    }


@with_timeout
def main():
    parser = argparse.ArgumentParser(description="TickFlow K线数据（含MACD）")
    parser.add_argument("--api-key", required=True, help="TickFlow API key")
    parser.add_argument("--symbol", required=True, help="股票代码")
    parser.add_argument("--period", default="1d", help="K线周期，如 1d, 1w")
    parser.add_argument("--count", type=int, default=60, help="K线条数")
    args = parser.parse_args()

    tf = TickFlow(api_key=args.api_key)
    klines = tf.klines.get(args.symbol, period=args.period, count=args.count)
    closes = klines.get("close", [])
    result = {
        "timestamp": klines.get("timestamp", []),
        "open": klines.get("open", []),
        "high": klines.get("high", []),
        "low": klines.get("low", []),
        "close": closes,
        "volume": klines.get("volume", []),
        "amount": klines.get("amount", []),
        "macd": calc_macd(closes),
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
