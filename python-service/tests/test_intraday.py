import sys
import unittest
from datetime import datetime
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parents[1]))
from akshare_intraday import MARKET_ZONE, normalizePoints


class IntradayTest(unittest.TestCase):
    def row(self, instant, price=10, volume=12):
        return {"时间": instant, "收盘": price, "成交量": volume}

    def testOnlyTodayCompletedTradingMinutesSortedAndDeduplicated(self):
        now = datetime(2026, 9, 11, 14, 0, tzinfo=MARKET_ZONE)
        records = [self.row("2026-09-10 15:00:00"), self.row("2026-09-11 14:01:00"),
                   self.row("2026-09-11 12:00:00"), self.row("2026-09-11 09:25:00"),
                   self.row("2026-09-11 13:01:00"), self.row("2026-09-11 09:31:00"),
                   self.row("2026-09-11 13:01:00", 11, 25)]
        points = normalizePoints(records, now)
        self.assertEqual([point["time"] for point in points], ["09:31", "13:01"])
        self.assertEqual(points[1]["volume"], 2500)
        self.assertEqual(points[1]["price"], 11)

    def testMissingVolumeIsNotInventedAndInvalidPricesAreSkipped(self):
        now = datetime(2026, 9, 11, 15, 0, tzinfo=MARKET_ZONE)
        records = [self.row("2026-09-11 09:31:00", volume=None),
                   self.row("2026-09-11 09:32:00", price=float('nan')),
                   self.row("2026-09-11 09:33:00", price=0),
                   self.row("2026-09-11 15:00:00", volume=0)]
        points = normalizePoints(records, now)
        self.assertEqual(len(points), 2)
        self.assertIsNone(points[0]["volume"])
        self.assertEqual(points[1]["volume"], 0)
        self.assertEqual(normalizePoints([self.row("2026-09-10 15:00:00")], now), [])


if __name__ == '__main__':
    unittest.main()
