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
                ["sh900001", 12, 10, 100, 2], ["sh600002", 10, 10, 0, 0],
                ["sh600003", None, 10, 100, 0]]
        data = activity.countBreadth(pd.DataFrame(rows, columns=["代码", "最新价", "昨收", "成交量", "涨跌额"]))
        self.assertEqual((data["up"], data["down"], data["flat"]), (1, 1, 1))
        self.assertEqual(data["sampleCount"], 3)
        self.assertEqual(data["excludedCount"], 2)
        self.assertEqual(data["downRatio"], 33.33)

    def testBreadthIncludesBeijingQuotesAndDeduplicatesCodes(self):
        rows = [["sh600001", 12, 10, 100, 2], ["sz000001", 8, 10, 100, -2],
                ["bj920001", 12, 10, 100, 2], ["920001", 12, 10, 100, 2],
                ["bj430047", 8, 10, 100, -2], ["bj830799", 10, 10, 100, 0],
                ["bj920002", 10, 10, 0, 0], ["sh900001", 12, 10, 100, 2],
                ["sz200001", 12, 10, 100, 2]]
        data = activity.countBreadth(pd.DataFrame(rows, columns=["代码", "最新价", "昨收", "成交量", "涨跌额"]))
        self.assertEqual((data["up"], data["down"], data["flat"]), (2, 2, 1))
        self.assertEqual(data["sampleCount"], 5)
        self.assertEqual(data["excludedCount"], 1)
        self.assertEqual(data["upRatio"], 40.0)

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

    def testSnapshotReportsSeparateBreadthAndLimitPoolScopes(self):
        ak = Mock()
        ak.tool_trade_date_hist_sina.return_value = pd.DataFrame({"trade_date": ["2026-09-11"]})
        ak.stock_zh_a_spot.return_value = pd.DataFrame({"代码": ["sh600001", "bj920001"],
            "最新价": [10, 8], "昨收": [9, 9], "成交量": [100, 100], "涨跌额": [1, -1]})
        ak.stock_zt_pool_em.return_value = pd.DataFrame({"代码": ["600001"]})
        ak.stock_zt_pool_dtgc_em.return_value = pd.DataFrame()
        data = activity.fetchActivity(ak, datetime(2026, 9, 11, 16))
        self.assertEqual((data["up"], data["down"]), (1, 1))
        self.assertEqual(data["scope"], "SH_SZ_BJ_A")
        self.assertEqual(data["limitScope"], "SH_SZ_A")
        self.assertEqual((data["limitUp"], data["limitDown"]), (1, 0))

    def testQuotesAndTurnoverUseTheSameSnapshotWithoutDuplicates(self):
        frame = pd.DataFrame({"代码": ["sh600001", "600001", "bj920001", "sh900001"],
            "最新价": [12, 12, 9, 1], "昨收": [10, 10, 10, 1], "成交量": [100, 100, 100, 1],
            "成交额": [1200, 1200, 900, 1]})
        quotes, turnover = activity.quoteSnapshot(frame)
        self.assertEqual(turnover, 2100)
        self.assertEqual(set(quotes), {"600001.SH", "920001.BJ"})
        self.assertEqual(quotes["600001.SH"], {"price": 12, "prevClose": 10})
        frame.loc[2, "成交额"] = float("nan")
        self.assertIsNone(activity.quoteSnapshot(frame)[1])

    def testMissingTurnoverIsNotZero(self):
        frame = pd.DataFrame({"代码": ["sh600001"], "最新价": [12], "昨收": [10], "成交量": [100]})
        self.assertIsNone(activity.quoteSnapshot(frame)[1])

    def testOptionalSentimentUsesFullPoolsAndDoesNotBlockQuotes(self):
        ak = Mock()
        up = pd.DataFrame({"代码": ["600001", "600002"], "连板数": [1, 3]})
        ak.stock_zt_pool_zbgc_em.return_value = pd.DataFrame({"代码": ["600003", "600003", "920001"]})
        details = activity.sentimentDetails(ak, "20260904", up)
        self.assertEqual(details["streakHeight"], 3)
        self.assertEqual(details["brokenRate"], 33.33)
        ak.stock_zt_pool_zbgc_em.side_effect = RuntimeError("unavailable")
        details = activity.sentimentDetails(ak, "20260904", up)
        self.assertIsNone(details["brokenRate"])
        self.assertEqual(details["streakHeight"], 3)
        self.assertEqual(details["optionalFailures"], ["brokenPool: RuntimeError"])

    def testSnapshotUsesOneCalendarAndDistinguishesIntraday(self):
        ak = Mock()
        ak.tool_trade_date_hist_sina.return_value = pd.DataFrame({"trade_date": ["2026-09-10", "2026-09-11"]})
        ak.stock_zh_a_spot.return_value = pd.DataFrame({"代码": ["sh600001"], "最新价": [10],
            "昨收": [9], "成交量": [100], "涨跌额": [1], "成交额": [1000]})
        ak.stock_zt_pool_em.return_value = pd.DataFrame()
        ak.stock_zt_pool_dtgc_em.return_value = pd.DataFrame()
        ak.stock_zt_pool_zbgc_em.return_value = pd.DataFrame()
        data = activity.fetchActivity(ak, datetime(2026, 9, 11, 14))
        self.assertEqual(data["sessionPhase"], "INTRADAY")
        self.assertEqual(data["previousTradeDate"], "20260910")
        self.assertEqual(data["totalTurnover"], 1000)
        ak.tool_trade_date_hist_sina.assert_called_once()

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
