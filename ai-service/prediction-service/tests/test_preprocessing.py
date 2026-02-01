"""
전처리 단위 테스트
"""
import sys
from pathlib import Path

# app 패키지 경로
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import unittest
import numpy as np
from app.preprocessing.transform import (
    normalize_close_minmax,
    build_sequences,
    preprocess_series_for_serving,
)


class TestNormalizeCloseMinmax(unittest.TestCase):
    def test_normalize_returns_bounded(self):
        close = np.array([100.0, 150.0, 200.0])
        out, mn, mx = normalize_close_minmax(close)
        self.assertGreaterEqual(out.min(), 0.0)
        self.assertLessEqual(out.max(), 1.0)
        self.assertEqual(mn, 100.0)
        self.assertEqual(mx, 200.0)

    def test_normalize_with_fixed_minmax(self):
        close = np.array([110.0, 120.0])
        out, _, _ = normalize_close_minmax(close, min_val=100.0, max_val=200.0)
        self.assertAlmostEqual(float(out[0]), 0.1)
        self.assertAlmostEqual(float(out[1]), 0.2)


class TestBuildSequences(unittest.TestCase):
    def test_sequences_shape(self):
        n = 50
        lookback = 30
        close = np.cumsum(np.random.randn(n).cumsum()) + 100.0
        open_ = close - 1.0
        high = close + 2.0
        low = close - 2.0
        volume = np.ones(n) * 1e6
        seqs, tgts, min_c, max_c = build_sequences(
            close, open_=open_, high=high, low=low, volume=volume,
            lookback=lookback, target_next_close=True, use_ohlcv=True, normalize=True,
        )
        self.assertEqual(seqs.shape[0], n - lookback)
        self.assertEqual(seqs.shape[1], lookback)
        self.assertEqual(seqs.shape[2], 5)
        self.assertEqual(tgts.shape[0], n - lookback)
        self.assertIsNotNone(min_c)
        self.assertIsNotNone(max_c)

    def test_sequences_insufficient_data_returns_empty(self):
        close = np.array([1.0, 2.0])
        seqs, tgts, _, _ = build_sequences(close, lookback=30)
        self.assertEqual(len(seqs), 0)
        self.assertEqual(len(tgts), 0)


class TestPreprocessSeriesForServing(unittest.TestCase):
    def test_serving_output_shape(self):
        lookback = 30
        close = list(np.random.rand(lookback) * 100 + 100)
        x = preprocess_series_for_serving(close=close, lookback=lookback, use_ohlcv=False)
        self.assertEqual(x.shape[0], 1)
        self.assertEqual(x.shape[1], lookback)
        self.assertEqual(x.shape[2], 1)

    def test_serving_ohlcv_shape(self):
        lookback = 30
        close = [float(i) for i in range(100, 100 + lookback)]
        open_ = [c - 1 for c in close]
        high = [c + 2 for c in close]
        low = [c - 1 for c in close]
        volume = [1e6] * lookback
        x = preprocess_series_for_serving(
            close=close, open_=open_, high=high, low=low, volume=volume,
            lookback=lookback, use_ohlcv=True,
        )
        self.assertEqual(x.shape[0], 1)
        self.assertEqual(x.shape[1], lookback)
        self.assertEqual(x.shape[2], 5)

    def test_serving_short_series_returns_empty(self):
        close = [1.0, 2.0, 3.0]
        x = preprocess_series_for_serving(close=close, lookback=30)
        self.assertEqual(x.size, 0)


if __name__ == "__main__":
    unittest.main()
