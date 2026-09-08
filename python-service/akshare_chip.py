#!/usr/bin/env python3
"""获取 AKShare 最近交易日筹码指标；进程超时由 Spring Boot 统一管理。"""
import argparse
from contextlib import redirect_stdout
import json
import math
import sys
import time
from requests.exceptions import ConnectionError, Timeout

MAX_ATTEMPTS = 3


COST_FIELDS = {
    "avg_cost": "平均成本",
    "cost_90_low": "90成本-低",
    "cost_90_high": "90成本-高",
    "concentration_90": "90集中度",
    "cost_70_low": "70成本-低",
    "cost_70_high": "70成本-高",
    "concentration_70": "70集中度",
}


def finiteNumber(value):
    """拒绝缺失或非有限指标，避免输出假数据。"""
    number = float(value)
    if not math.isfinite(number):
        raise ValueError("筹码指标包含无效数值")
    return number


def normalizeChip(frame):
    """取最近日期，获利比例从 0～1 转成百分数，集中度保持原始比例。"""
    if frame is None or frame.empty:
        raise ValueError("未获取到筹码分布数据")
    row = frame.sort_values("日期").iloc[-1]
    profitRatio = finiteNumber(row["获利比例"])
    if not 0 <= profitRatio <= 1:
        raise ValueError("筹码获利比例超出有效范围")
    result = {key: finiteNumber(row[column]) for key, column in COST_FIELDS.items()}
    result.update({
        "date": str(row["日期"])[:10],
        "profit_ratio": round(profitRatio * 100, 2),
        "loss_ratio": round((1 - profitRatio) * 100, 2),
    })
    return result


def fetchChip(ak, symbol):
    """仅对网络连接/超时重试，最多三次，退避一秒、两秒。"""
    for attempt in range(MAX_ATTEMPTS):
        try:
            return ak.stock_cyq_em(symbol=symbol, adjust="qfq")
        except (ConnectionError, Timeout):
            if attempt == MAX_ATTEMPTS - 1:
                raise
            time.sleep(2 ** attempt)


def main():
    parser = argparse.ArgumentParser(description="AKShare 筹码分布")
    parser.add_argument("--symbol", required=True, help="六位股票代码")
    args = parser.parse_args()
    try:
        # stdout 仅保留 JSON 协议，库的进度与提示走 stderr。
        with redirect_stdout(sys.stderr):
            import akshare as ak
            frame = fetchChip(ak, args.symbol)
            data = normalizeChip(frame)
        result = {"status": "success", "data": data}
    except Exception as error:
        result = {"status": "error", "message": f"AKShare筹码获取失败: {error}"}
    print(json.dumps(result, ensure_ascii=False, allow_nan=False), flush=True)


if __name__ == "__main__":
    main()
