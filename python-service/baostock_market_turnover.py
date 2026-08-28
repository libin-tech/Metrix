#!/usr/bin/env python3
"""汇总 Baostock 上证综指与深证成指日线成交额，输出市场成交额历史。"""
import argparse
import json
import math
import sys
from contextlib import redirect_stdout
from datetime import datetime, timedelta, timezone
from io import StringIO

import baostock as bs


MARKET_TIME_ZONE = timezone(timedelta(hours=8))
SHANGHAI_SYMBOL = "sh.000001"
SHENZHEN_SYMBOL = "sz.399001"
KLINE_FIELDS = "date,amount"
KLINE_FREQUENCY = "d"
NO_ADJUST_FLAG = "3"
MIN_CALENDAR_DAYS = 160
CALENDAR_DAYS_PER_TRADE_DAY = 3


def get_trade_date_range(count):
    """根据目标交易日数量计算足够覆盖节假日的自然日查询范围。"""
    end_date = datetime.now(MARKET_TIME_ZONE).date()
    calendar_days = max(MIN_CALENDAR_DAYS, count * CALENDAR_DAYS_PER_TRADE_DAY)
    start_date = end_date - timedelta(days=calendar_days)
    return start_date.isoformat(), end_date.isoformat()


def parse_amount(value, symbol, trade_date):
    """校验并转换 Baostock 返回的成交额。"""
    try:
        amount = float(value)
    except (TypeError, ValueError) as error:
        raise RuntimeError(f"{symbol} 在 {trade_date} 的成交额无效") from error
    if not math.isfinite(amount) or amount < 0:
        raise RuntimeError(f"{symbol} 在 {trade_date} 的成交额无效")
    return amount


def query_daily_amounts(symbol, start_date, end_date):
    """读取单个指数每日成交额，并以交易日作为键返回。"""
    result = bs.query_history_k_data_plus(
        symbol,
        KLINE_FIELDS,
        start_date=start_date,
        end_date=end_date,
        frequency=KLINE_FREQUENCY,
        adjustflag=NO_ADJUST_FLAG,
    )
    if result.error_code != "0":
        raise RuntimeError(f"Baostock 查询 {symbol} 失败: {result.error_msg}")

    daily_amounts = {}
    while result.next():
        trade_date, amount = result.get_row_data()
        daily_amounts[trade_date] = parse_amount(amount, symbol, trade_date)
    if not daily_amounts:
        raise RuntimeError(f"Baostock 未返回 {symbol} 的日线成交额")
    return daily_amounts


def build_market_turnover(count):
    """按交易日合并两市指数成交额，生成最近指定数量的历史数据。"""
    start_date, end_date = get_trade_date_range(count)
    with redirect_stdout(StringIO()):
        login_result = bs.login()
        if login_result.error_code != "0":
            raise RuntimeError(f"Baostock 登录失败: {login_result.error_msg}")

        try:
            shanghai_amounts = query_daily_amounts(SHANGHAI_SYMBOL, start_date, end_date)
            shenzhen_amounts = query_daily_amounts(SHENZHEN_SYMBOL, start_date, end_date)
        finally:
            bs.logout()

    shared_dates = sorted(set(shanghai_amounts).intersection(shenzhen_amounts))
    history = [
        {"date": trade_date, "amount": round(shanghai_amounts[trade_date] + shenzhen_amounts[trade_date])}
        for trade_date in shared_dates[-count:]
    ]
    if len(history) < count:
        raise RuntimeError(f"Baostock 仅返回 {len(history)} 个共同交易日，少于请求的 {count} 个")

    current = history[-1]["amount"]
    previous = history[-2]["amount"] if len(history) > 1 else current
    return {"amount": current, "difference": current - previous, "history": history}


def main():
    parser = argparse.ArgumentParser(description="Baostock市场成交额历史")
    parser.add_argument("--count", type=int, default=60, help="需要的交易日数量")
    args = parser.parse_args()
    if args.count < 1:
        raise RuntimeError("交易日数量必须大于零")
    print(json.dumps({"status": "success", "data": build_market_turnover(args.count)}, ensure_ascii=False), flush=True)


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        print(json.dumps({"status": "error", "message": str(error)}, ensure_ascii=False), flush=True)
        sys.exit(1)
