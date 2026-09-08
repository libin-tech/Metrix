import sys
from pathlib import Path
import unittest
from unittest.mock import Mock, patch

sys.path.insert(0, str(Path(__file__).parents[1]))
from fuyao_client import FuyaoClient, normalizeQuote, normalizeTicker
import fuyao_client
from market_indicators import calc_macd


class FuyaoTest(unittest.TestCase):
    def client(self, data):
        client = FuyaoClient("test-key")
        response = Mock()
        response.json.return_value = {"code": 0, "data": data}
        client.session = Mock()
        client.session.get.return_value = response
        return client

    def tickerRow(self, code, name="股票"):
        return {"thscode": code, "ticker": code[:6], "name": name, "asset_type": "a-share"}

    def testTickerPaginationAndDeduplication(self):
        client = self.client({"item": []})
        rows = [self.tickerRow("600000.SH"), self.tickerRow("000001.SZ"), self.tickerRow("920001.BJ")]
        with patch.object(fuyao_client, "TICKER_PAGE_SIZE", 2), patch.object(client, "get", side_effect=[
                {"item": rows[:2]}, {"item": [rows[1], rows[2]]}, {"item": []}]) as get:
            result = client.tickers()
        self.assertEqual(len(result), 3)
        self.assertEqual([call.args[1]["offset"] for call in get.call_args_list], [0, 2, 4])
        self.assertEqual(result[2]["tsCode"], "920001.BJ")

    def testTickerFailuresNeverReturnPartialList(self):
        client = self.client({"item": []})
        with self.assertRaises(ValueError):
            client.tickers()
        with self.assertRaises(ValueError):
            normalizeTicker({"thscode": "000001.SH", "ticker": "000001", "name": "指数", "asset_type": "a-share-index"})
        rows = [self.tickerRow("600000.SH"), self.tickerRow("000001.SZ")]
        with patch.object(fuyao_client, "TICKER_PAGE_SIZE", 2), patch.object(client, "get", side_effect=[{"item": rows}, RuntimeError("upstream")]):
            with self.assertRaises(RuntimeError):
                client.tickers()
        with patch.object(fuyao_client, "TICKER_PAGE_SIZE", 2), patch.object(client, "get", return_value={"item": rows}):
            with self.assertRaises(ValueError):
                client.tickers()

    def testQuoteUnitsMissingMetricsAndTimestamp(self):
        quote = normalizeQuote({"last_price": 10.05, "prev_price": 10, "high_price": 11,
                                "low_price": 9, "volume": 12300, "turnover": 130000,
                                "price_change_ratio_pct": -0.5}, "2026-09-07T10:00:00+08:00")
        self.assertEqual(quote["ext"]["change_pct"], -0.5)
        self.assertEqual(quote["ext"]["amplitude"], 20)
        self.assertEqual(quote["volume"], 12300)
        self.assertEqual(quote["amount"], 130000)
        self.assertIsNone(quote["ext"]["turnover_rate"])
        self.assertIsNone(quote["ext"]["volume_ratio"])
        self.assertGreater(quote["timestamp"], 1_000_000_000_000)
        self.assertIsNone(normalizeQuote({}, None)["timestamp"])

    def testAuthenticationAndBusinessError(self):
        client = FuyaoClient("test-key")
        self.assertEqual(client.session.headers["X-api-key"], "test-key")
        client.session = Mock()
        client.session.get.return_value.json.return_value = {"code": 2003, "data": {"item": []}}
        with self.assertRaisesRegex(RuntimeError, "2003"):
            client.quotes(["600519.SH"])
        with self.assertRaises(ValueError):
            FuyaoClient("")

    def testQuoteMatchesRequestedSymbolOrder(self):
        client = self.client({"timestamp": None, "item": [
            {"thscode": "000001.SZ", "last_price": 12}, {"thscode": "600519.SH", "last_price": 1500}]})
        result = client.quotes(["600519.SH", "000001.SZ"])
        self.assertEqual([quote["last_price"] for quote in result], [1500, 12])
        self.assertEqual(client.session.get.call_args.args[0], "https://fuyao.aicubes.cn/api/a-share/prices/snapshot")
        with self.assertRaises(ValueError):
            client.quotes(["600000.SH"])

    def testHistoricalOrderingAdjustmentAndMacd(self):
        rows = [{"date_ms": 1_700_000_000_000 + i * 86400000, "close_price": 10 + i,
                 "open_price": 10 + i, "high_price": 11 + i, "low_price": 9 + i,
                 "volume": 1000, "turnover": 10000} for i in range(70)]
        client = self.client({"item": list(reversed(rows)) + [rows[0]]})
        data = client.klines("000001.SZ", "1d", 60)
        self.assertEqual(data["close"], list(range(20, 80)))
        self.assertEqual(len(data["timestamp"]), 60)
        self.assertEqual(client.session.get.call_args.kwargs["params"]["adjust"], "forward")
        self.assertGreater(calc_macd(data["close"])["dif"], 0)
        with self.assertRaises(ValueError):
            client.klines("000001.SZ", "1m", 60)

    def testHistoricalWindowExpandsForSuspendedStock(self):
        client = self.client({"item": []})
        with patch.object(client, "get", side_effect=[{"item": [{"date_ms": 2, "close_price": 2}]},
                                                     {"item": [{"date_ms": 1, "close_price": 1}, {"date_ms": 2, "close_price": 2}]}]) as get:
            data = client.klines("000001.SZ", "1d", 2)
        self.assertEqual(data["close"], [1, 2])
        self.assertLess(get.call_args_list[1].args[1]["start"], get.call_args_list[0].args[1]["start"])

    def testTurnoverRequiresBothIndexesAndSourceTime(self):
        client = self.client({"timestamp": "2026-09-07T10:00:00+08:00", "item": [
            {"thscode": "000001.SH", "turnover": 100000000},
            {"thscode": "399001.SZ", "turnover": 200000000}]})
        data = client.marketTurnover(["000001.SH", "399001.SZ"])
        self.assertEqual(data["history"], [{"date": "2026-09-07", "amount": 300000000}])
        client.session.get.return_value.json.return_value["data"]["timestamp"] = None
        with self.assertRaises(ValueError):
            client.marketTurnover(["000001.SH", "399001.SZ"])

    def testAnomalyAnalysisListUsesDocumentedEndpoint(self):
        client = self.client({"timestamp": 1_751_260_800_000, "item": [{"thscode": "600519.SH"}]})

        result = client.anomalyAnalysisList()

        self.assertEqual(result["item"][0]["thscode"], "600519.SH")
        self.assertEqual(client.session.get.call_args.args[0],
                         "https://fuyao.aicubes.cn/api/a-share/special-data/anomaly-analysis-list")


if __name__ == "__main__":
    unittest.main()
