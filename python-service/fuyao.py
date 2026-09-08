#!/usr/bin/env python3
"""同花顺金融数据 API 的行情、日K与市场成交额命令行入口。"""
import argparse
import json
import os

from fuyao_client import FuyaoClient, DEFAULT_BASE_URL
from market_indicators import calc_macd


def fetchQuotes(client, args):
    return client.quotes(args.symbols.split(","))


def fetchKlines(client, args):
    data = client.klines(args.symbol, args.period, args.count)
    return {**data, "macd": calc_macd(data["close"])}


def fetchTurnover(client, args):
    return client.marketTurnover(args.symbols.split(","))


def fetchTickers(client, args):
    return client.tickers()


def fetchAnomalyAnalysis(client, args):
    return client.anomalyAnalysisList()


FETCHERS = {"tickers": fetchTickers, "quotes": fetchQuotes, "klines": fetchKlines, "market-turnover": fetchTurnover,
            "anomaly-analysis": fetchAnomalyAnalysis}


def main():
    parser = argparse.ArgumentParser(description="同花顺金融数据 API")
    parser.add_argument("--operation", choices=FETCHERS, required=True)
    parser.add_argument("--api-url", default=DEFAULT_BASE_URL)
    parser.add_argument("--symbol")
    parser.add_argument("--symbols")
    parser.add_argument("--period", default="1d")
    parser.add_argument("--count", type=int, default=60)
    args = parser.parse_args()
    try:
        client = FuyaoClient(os.environ.get("FUYAO_API_KEY"), args.api_url)
        result = FETCHERS[args.operation](client, args)
        print(json.dumps({"status": "success", "data": result}, ensure_ascii=False, allow_nan=False), flush=True)
    except Exception as error:
        print(json.dumps({"status": "error", "message": str(error)}, ensure_ascii=False), flush=True)


if __name__ == "__main__":
    main()
