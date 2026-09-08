"""筹码字段与 JSON 协议回归测试，不依赖外部行情网络。"""
import contextlib
import importlib.util
import io
import json
from pathlib import Path
import sys
import types
import unittest
from unittest.mock import patch, Mock
from requests.exceptions import ConnectionError

import pandas as pd

spec = importlib.util.spec_from_file_location("chip", Path(__file__).parents[1] / "akshare_chip.py")
chip = importlib.util.module_from_spec(spec)
spec.loader.exec_module(chip)


class ChipTest(unittest.TestCase):
    def frame(self):
        return pd.DataFrame([
            {"日期": "2026-09-04", "获利比例": 0.005, "平均成本": 10,
             "90成本-低": 8, "90成本-高": 12, "90集中度": 0.2,
             "70成本-低": 9, "70成本-高": 11, "70集中度": 0.1},
            {"日期": "2026-09-03", "获利比例": 0.8, "平均成本": 20,
             "90成本-低": 18, "90成本-高": 22, "90集中度": 0.1,
             "70成本-低": 19, "70成本-高": 21, "70集中度": 0.05},
        ])

    def testLatestDateAndUnits(self):
        data = chip.normalizeChip(self.frame())
        self.assertEqual(data, {
            "date": "2026-09-04", "profit_ratio": 0.5, "loss_ratio": 99.5,
            "avg_cost": 10, "cost_90_low": 8, "cost_90_high": 12,
            "concentration_90": 0.2, "cost_70_low": 9, "cost_70_high": 11,
            "concentration_70": 0.1,
        })

    def testInvalidDataRejected(self):
        for invalid in (None, pd.DataFrame()):
            with self.assertRaises(ValueError):
                chip.normalizeChip(invalid)
        for value in (float("nan"), float("inf"), -0.1, 1.1):
            frame = self.frame()
            frame.loc[0, "获利比例"] = value
            with self.assertRaises(ValueError):
                chip.normalizeChip(frame)

    def testRetriesRemoteDisconnect(self):
        fetch = Mock(side_effect=[ConnectionError("Remote end closed connection without response"), self.frame()])
        with patch("time.sleep"):
            result = chip.fetchChip(types.SimpleNamespace(stock_cyq_em=fetch), "601138")
        self.assertEqual(len(result), 2)
        self.assertEqual(fetch.call_count, 2)

    def testRetriesAreBounded(self):
        fetch = Mock(side_effect=ConnectionError("RemoteDisconnected"))
        with patch("time.sleep"), self.assertRaises(ConnectionError):
            chip.fetchChip(types.SimpleNamespace(stock_cyq_em=fetch), "601138")
        self.assertEqual(fetch.call_count, 3)

    def testDoesNotRetrySchemaErrors(self):
        fetch = Mock(side_effect=ValueError("invalid data"))
        with self.assertRaises(ValueError):
            chip.fetchChip(types.SimpleNamespace(stock_cyq_em=fetch), "601138")
        self.assertEqual(fetch.call_count, 1)

    def testProtocolAndFailure(self):
        def fetch(**kwargs):
            self.assertEqual(kwargs, {"symbol": "601138", "adjust": "qfq"})
            print("上游进度输出")
            return self.frame()

        for fetcher, status in ((fetch, "success"), (lambda **kwargs: pd.DataFrame(), "error")):
            output = io.StringIO()
            with patch.dict(sys.modules, {"akshare": types.SimpleNamespace(stock_cyq_em=fetcher)}), \
                    patch.object(sys, "argv", ["akshare_chip.py", "--symbol", "601138"]), \
                    contextlib.redirect_stdout(output), contextlib.redirect_stderr(io.StringIO()):
                chip.main()
            self.assertEqual(json.loads(output.getvalue())["status"], status)


if __name__ == "__main__":
    unittest.main()
