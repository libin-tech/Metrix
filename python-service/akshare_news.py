#!/usr/bin/env python3
"""获取AKShare股票新闻数据，输出JSON到stdout。

由Spring Boot通过子进程调用，60秒超时。

Usage:
    python akshare_news.py --symbol 300059
"""
import argparse
import io
import json
import os
import sys
from concurrent.futures import ThreadPoolExecutor, TimeoutError as FuturesTimeoutError
from contextlib import redirect_stdout
from datetime import datetime
from functools import wraps

_script_dir = os.path.dirname(os.path.abspath(__file__))
sys.path = [p for p in sys.path if p != _script_dir]

TIMEOUT_SECONDS = 60


class ScriptTimeoutError(Exception):
    pass


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
            signal.signal(signal.SIGALRM, lambda s, f: (_ for _ in ()).throw(ScriptTimeoutError(f"请求超时（{TIMEOUT_SECONDS}秒）")))
            signal.alarm(TIMEOUT_SECONDS)
            try:
                return func(*args, **kwargs)
            finally:
                signal.alarm(0)
    return wrapper


@with_timeout
def main():
    parser = argparse.ArgumentParser(description="AKShare 股票新闻")
    parser.add_argument("--symbol", required=True, help="股票代码，如 300059")
    args = parser.parse_args()

    os.environ["TQDM_DISABLE"] = "1"
    try:
        import akshare as ak
    except ImportError:
        print(json.dumps({"status": "error", "message": "AKShare 未安装"}, ensure_ascii=False), flush=True)
        return

    try:
        with redirect_stdout(io.StringIO()):
            df = ak.stock_news_em(symbol=args.symbol)
    except Exception as e:
        print(json.dumps({"status": "error", "message": f"获取新闻失败: {e}"}, ensure_ascii=False), flush=True)
        return

    if df is None or df.empty:
        print(json.dumps({"status": "success", "data": [], "count": 0}, ensure_ascii=False), flush=True)
        return

    news_list = []
    for _, row in df.iterrows():
        news_item = {}
        for col in df.columns:
            val = row[col]
            if isinstance(val, float) and val != val:
                val = ""
            elif isinstance(val, (datetime,)):
                val = val.strftime("%Y-%m-%d %H:%M:%S")
            elif not isinstance(val, (str, int, float, bool)):
                val = str(val)
            news_item[col] = val
        news_list.append(news_item)

    data = _normalize(news_list)
    print(json.dumps({"status": "success", "data": data, "count": len(data)}, ensure_ascii=False, flush=True)


def _normalize(news_list):
    """将AKShare返回的字段名映射为统一格式"""
    field_map = {
        "新闻标题": "title",
        "新闻内容": "summary",
        "文章来源": "source",
        "发布时间": "publishTime",
        "新闻链接": "url",
    }

    result = []
    for item in news_list:
        normalized = {}
        for old_key, new_key in field_map.items():
            val = item.get(old_key, "")
            if not val:
                val = item.get(new_key, "")
            normalized[new_key] = str(val) if val else ""
        if not normalized.get("title"):
            continue
        result.append(normalized)
    return result


if __name__ == "__main__":
    main()
