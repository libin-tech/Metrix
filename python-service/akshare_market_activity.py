#!/usr/bin/env python3
"""获取AKShare赚钱效应分析数据，输出JSON到stdout。

由Spring Boot通过子进程调用。

Usage:
    python akshare_market_activity.py
"""
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
    os.environ["TQDM_DISABLE"] = "1"
    try:
        import akshare as ak
    except ImportError:
        print(json.dumps({"status": "error", "message": "AKShare 未安装"}, ensure_ascii=False), flush=True)
        return

    try:
        df = ak.stock_market_activity_legu()
        if df.empty:
            print(json.dumps({"status": "error", "message": "未获取到数据"}, ensure_ascii=False), flush=True)
            return

        data = {}
        for _, row in df.iterrows():
            item = row["item"]
            value = row["value"]
            if item == "活跃度":
                data["activity"] = str(value)
            elif item == "统计日期":
                data["statDate"] = str(value)
            elif item == "真实涨停":
                data["realLimitUp"] = int(value)
            elif item == "真实跌停":
                data["realLimitDown"] = int(value)
            elif item == "st st*涨停":
                data["stLimitUp"] = int(value)
            elif item == "st st*跌停":
                data["stLimitDown"] = int(value)
            elif item == "上涨":
                data["up"] = int(value)
            elif item == "下跌":
                data["down"] = int(value)
            elif item == "涨停":
                data["limitUp"] = int(value)
            elif item == "跌停":
                data["limitDown"] = int(value)
            elif item == "平盘":
                data["flat"] = int(value)
            elif item == "停牌":
                data["suspended"] = int(value)

        total = data.get("up", 0) + data.get("down", 0) + data.get("flat", 0)
        upRatio = round(data.get("up", 0) / total * 100, 2) if total > 0 else 0
        data["upRatio"] = upRatio
        data["downRatio"] = round(100 - upRatio, 2)

        print(json.dumps({"status": "success", "data": data}, ensure_ascii=False), flush=True)

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
