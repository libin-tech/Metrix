import importlib.util
from pathlib import Path
import unittest
import pandas as pd

spec = importlib.util.spec_from_file_location("indexData", Path(__file__).parents[1] / "akshare_index.py")
indexData = importlib.util.module_from_spec(spec)
spec.loader.exec_module(indexData)


class IndexHistoryTest(unittest.TestCase):
    def testTargetDateAppliedBeforeTail(self):
        dates = pd.bdate_range("2025-01-01", periods=350).strftime("%Y-%m-%d")
        frame = pd.DataFrame({"date": dates, "close": range(350)})
        history = indexData.selectHistory(frame, dates[200], 130)
        self.assertEqual(len(history), 130)
        self.assertEqual(history.iloc[-1]["date"], dates[200])
        self.assertEqual(history.iloc[0]["date"], dates[71])

    def testSortAndDeduplicateWithoutInventingRecords(self):
        frame = pd.DataFrame({"date": ["2026-01-05", "2026-01-02", "2026-01-05"], "close": [10, 9, 11]})
        history = indexData.selectHistory(frame, "2026-01-05", 130)
        self.assertEqual(list(history["close"]), [9, 11])
        self.assertTrue(indexData.selectHistory(frame, "2025-01-01", 130).empty)
