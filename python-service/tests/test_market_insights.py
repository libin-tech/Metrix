import importlib.util
from pathlib import Path
import unittest
from unittest.mock import Mock, patch

import pandas as pd
import requests

spec = importlib.util.spec_from_file_location("insights", Path(__file__).parents[1] / "akshare_market_insights.py")
insights = importlib.util.module_from_spec(spec)
spec.loader.exec_module(insights)


class MarketInsightsTest(unittest.TestCase):
    def testFullRankingsKeepRecordsBeyondTwenty(self):
        frame = pd.DataFrame({
            "代码": [f"600{i:03}" for i in range(37)],
            "当前排名": range(37, 0, -1), "上榜日": ["2026-09-07"] * 37,
            "龙虎榜净买额": range(37),
        })
        ak = Mock()
        ak.stock_lhb_detail_em.return_value = frame
        ak.stock_hot_rank_em.return_value = frame
        dragonTiger = insights.get_dragon_tiger(ak)
        popularity = insights.get_popularity_rank(ak)
        pool = insights.normalize_limit_pool(frame, "连板数")
        self.assertEqual([len(dragonTiger), len(popularity), len(pool)], [37, 37, 37])
        self.assertEqual(dragonTiger[0]["netAmount"], 36)
        self.assertEqual(popularity[0]["rank"], 1)
        self.assertEqual(pool[-1]["code"], "600036")

    def fundResponse(self, total, codes):
        response = Mock()
        response.json.return_value = {"data": {"total": total, "diff": [
            {"f12": str(code), "f62": 100 - code} for code in codes
        ]}}
        return response

    def testFundFlowUsesActualPageSizeAndLoadsLastPage(self):
        with patch.object(insights.requests, "Session") as sessionFactory:
            session = sessionFactory.return_value.__enter__.return_value
            session.get.side_effect = [self.fundResponse(45, range(20)),
                                       self.fundResponse(45, range(20, 40)),
                                       self.fundResponse(45, range(40, 45))]
            records = insights.get_fund_flow()
            self.assertEqual(len(records), 45)
            self.assertEqual(records[-1]["code"], "44")
            self.assertEqual([call.kwargs["params"]["pn"] for call in session.get.call_args_list], [1, 2, 3])

    def testFundFlowFailureDoesNotReturnPartialRanking(self):
        with patch.object(insights.requests, "Session") as sessionFactory:
            session = sessionFactory.return_value.__enter__.return_value
            session.get.side_effect = [self.fundResponse(45, range(20)), requests.Timeout()]
            with self.assertRaises(requests.Timeout):
                insights.get_fund_flow()

    def testFundFlowRejectsRepeatedPages(self):
        with patch.object(insights.requests, "Session") as sessionFactory:
            session = sessionFactory.return_value.__enter__.return_value
            session.get.return_value = self.fundResponse(45, range(20))
            with self.assertRaises(ValueError):
                insights.get_fund_flow()

    def testEmptyFundFlow(self):
        with patch.object(insights.requests, "Session") as sessionFactory:
            session = sessionFactory.return_value.__enter__.return_value
            session.get.return_value = self.fundResponse(0, [])
            self.assertEqual(insights.get_fund_flow(), [])
            session.get.assert_called_once()

    def testFundFlowRejectsTruncatedLastPage(self):
        with patch.object(insights.requests, "Session") as sessionFactory:
            session = sessionFactory.return_value.__enter__.return_value
            session.get.side_effect = [self.fundResponse(25, range(20)),
                                       self.fundResponse(25, range(20, 22))]
            with self.assertRaises(ValueError):
                insights.get_fund_flow()


if __name__ == "__main__":
    unittest.main()
