import importlib.util
from pathlib import Path
from datetime import datetime
import unittest
from unittest.mock import Mock
import pandas as pd

spec = importlib.util.spec_from_file_location("activity", Path(__file__).parents[1] / "akshare_market_activity.py")
activity = importlib.util.module_from_spec(spec)
spec.loader.exec_module(activity)


class MarketActivityTest(unittest.TestCase):
    def testBreadthExcludesInvalidSuspendedAndOtherMarkets(self):
        rows = [["sh600001", 12, 10, 100, 2], ["sz000001", 8, 10, 100, -2],
                ["sz300001", 10, 10, 100, 0], ["sh600001", 12, 10, 100, 2],
                ["bj920001", 12, 10, 100, 2], ["sh600002", 10, 10, 0, 0],
                ["sh600003", None, 10, 100, 0]]
        data = activity.countBreadth(pd.DataFrame(rows, columns=["代码", "最新价", "昨收", "成交量", "涨跌额"]))
        self.assertEqual((data["up"], data["down"], data["flat"]), (1, 1, 1))
        self.assertEqual(data["sampleCount"], 3)
        self.assertEqual(data["excludedCount"], 2)
        self.assertEqual(data["downRatio"], 33.33)

    def testFullPoolNotTruncatedAndDeduplicated(self):
        codes = [f"600{i:03}" for i in range(55)] + ["600000", "920001"]
        self.assertEqual(activity.countPool(pd.DataFrame({"代码": codes})), 55)
        self.assertEqual(activity.countPool(pd.DataFrame()), 0)
        with self.assertRaises(ValueError):
            activity.countPool(None)

    def testCalendarUsesPreviousSessionBeforeOpenAndOnWeekend(self):
        calendar = pd.DataFrame({"trade_date": ["2026-09-04", "2026-09-07"]})
        for instant, expected in [("2026-09-06T12:00", "20260904"),
                                  ("2026-09-07T09:00", "20260904"),
                                  ("2026-09-07T10:00", "20260907")]:
            self.assertEqual(activity.latestTradeDate(calendar, datetime.fromisoformat(instant)), expected)

    def testUpstreamErrorDoesNotProducePartialSuccess(self):
        ak = Mock()
        ak.tool_trade_date_hist_sina.return_value = pd.DataFrame({"trade_date": ["2026-09-04"]})
        ak.stock_zh_a_spot.return_value = pd.DataFrame({"代码": ["sh600001"], "最新价": [10],
            "昨收": [9], "成交量": [100], "涨跌额": [1]})
        ak.stock_zt_pool_em.side_effect = RuntimeError("upstream unavailable")
        with self.assertRaisesRegex(RuntimeError, "upstream unavailable"):
            activity.fetchActivity(ak, datetime(2026, 9, 4, 12))
        ak.stock_zt_pool_em.assert_called_once_with(date="20260904")


if __name__ == "__main__":
    unittest.main()
