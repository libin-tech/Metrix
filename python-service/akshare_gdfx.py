#!/usr/bin/env python3
"""获取AKShare十大流通股东数据，输出JSON到stdout。

由Spring Boot通过子进程调用，120秒超时。

Usage:
    python akshare_gdfx.py --symbol 601138 --date 20240930
"""
import argparse
import json
import os
import sys
from concurrent.futures import ThreadPoolExecutor, TimeoutError as FuturesTimeoutError
from datetime import datetime, timedelta
from functools import wraps

_script_dir = os.path.dirname(os.path.abspath(__file__))
sys.path = [p for p in sys.path if p != _script_dir]

TIMEOUT_SECONDS = 120


def _latest_quarter_end():
    """计算最近一个已完成的季度末日期，格式 yyyyMMdd"""
    today = datetime.today()
    year = today.year
    month = today.month
    if month < 4:
        return f"{year - 1}1231"
    elif month < 7:
        return f"{year}0331"
    elif month < 10:
        return f"{year}0630"
    else:
        return f"{year}0930"


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
    parser = argparse.ArgumentParser(description="AKShare 十大流通股东")
    parser.add_argument("--symbol", required=True, help="股票代码")
    parser.add_argument("--date", default="", help="报告期，如 20240930")
    args = parser.parse_args()

    os.environ["TQDM_DISABLE"] = "1"
    try:
        import akshare as ak
    except ImportError:
        print(json.dumps({"status": "error", "message": "AKShare 未安装"}, ensure_ascii=False), flush=True)
        return

    market_prefix = "sh" if args.symbol.startswith(("6", "9")) else "sz"
    symbol_full = f"{market_prefix}{args.symbol}"

    query_date = args.date if args.date else _latest_quarter_end()

    try:
        df = ak.stock_gdfx_free_top_10_em(symbol=symbol_full, date=query_date)
    except Exception as e:
        log_msg = f"带date参数({query_date})获取失败: {e}，尝试不带date参数"
        print(log_msg, file=sys.stderr, flush=True)
        try:
            df = ak.stock_gdfx_free_top_10_em(symbol=symbol_full)
        except Exception as e2:
            print(json.dumps({"status": "error", "message": f"获取十大流通股东失败: {e2}"}, ensure_ascii=False), flush=True)
            return

    if df is None or df.empty:
        print(json.dumps({"status": "error", "message": "十大流通股东数据为空"}, ensure_ascii=False), flush=True)
        return

    records = []
    for _, row in df.iterrows():
        records.append({
            "rank": int(row.get("名次", 0)),
            "holder_name": str(row.get("股东名称", "")),
            "holder_type": str(row.get("股东性质", "")),
            "share_type": str(row.get("股份类型", "")),
            "hold_num": int(row.get("持股数", 0)),
            "free_holdnum_ratio": float(row.get("占总流通股本持股比例", 0)),
            "change_num": str(row.get("增减", "0")),
            "change_ratio": float(row.get("变动比率", 0)),
        })

    print(json.dumps({"status": "success", "data": records}, ensure_ascii=False), flush=True)


if __name__ == "__main__":
    try:
        main()
    except ScriptTimeoutError as e:
        print(json.dumps({"status": "error", "message": str(e)}, ensure_ascii=False), flush=True)
        os._exit(1)
    except Exception as e:
        print(json.dumps({"status": "error", "message": str(e)}, ensure_ascii=False), flush=True)
        os._exit(1)
