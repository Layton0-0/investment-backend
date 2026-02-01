"""
LSTM 모델 단위 테스트 (forward shape)
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import unittest
import torch
from app.models.lstm_model import LSTMPredictor


class TestLSTMPredictor(unittest.TestCase):
    def test_forward_shape_single_batch(self):
        batch, seq_len, features = 1, 30, 5
        model = LSTMPredictor(input_size=features, hidden_size=64, num_layers=2)
        x = torch.randn(batch, seq_len, features)
        out = model(x)
        self.assertEqual(out.shape, (batch, 1))

    def test_forward_shape_batch(self):
        batch, seq_len, features = 8, 30, 5
        model = LSTMPredictor(input_size=features, hidden_size=64, num_layers=2)
        x = torch.randn(batch, seq_len, features)
        out = model(x)
        self.assertEqual(out.shape, (batch, 1))

    def test_forward_close_only(self):
        batch, seq_len, features = 1, 30, 1
        model = LSTMPredictor(input_size=features, hidden_size=32, num_layers=1)
        x = torch.randn(batch, seq_len, features)
        out = model(x)
        self.assertEqual(out.shape, (batch, 1))


if __name__ == "__main__":
    unittest.main()
