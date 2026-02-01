"""
LSTM 가격/수익률 예측 모델 (PyTorch)
"""
from typing import Optional

import torch
import torch.nn as nn


class LSTMPredictor(nn.Module):
    """
    입력: (batch, seq_len, features) — OHLCV 또는 close만
    출력: (batch, 1) — 다음 close 또는 수익률
    """

    def __init__(
        self,
        input_size: int = 5,
        hidden_size: int = 64,
        num_layers: int = 2,
        dropout: float = 0.1,
        predict_return: bool = False,
    ):
        super().__init__()
        self.input_size = input_size
        self.hidden_size = hidden_size
        self.num_layers = num_layers
        self.predict_return = predict_return

        self.lstm = nn.LSTM(
            input_size=input_size,
            hidden_size=hidden_size,
            num_layers=num_layers,
            batch_first=True,
            dropout=dropout if num_layers > 1 else 0.0,
        )
        self.fc = nn.Sequential(
            nn.Linear(hidden_size, hidden_size // 2),
            nn.ReLU(),
            nn.Dropout(dropout),
            nn.Linear(hidden_size // 2, 1),
        )

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        """
        Args:
            x: (batch, seq_len, input_size)
        Returns:
            (batch, 1)
        """
        out, _ = self.lstm(x)
        last_hidden = out[:, -1, :]
        return self.fc(last_hidden)

    @classmethod
    def load_from_path(
        cls,
        path: str,
        device: Optional[torch.device] = None,
        **model_kwargs,
    ) -> "LSTMPredictor":
        """state_dict 로드. model_kwargs는 input_size, hidden_size 등 (저장 시와 동일해야 함)."""
        if device is None:
            device = torch.device("cpu")
        state = torch.load(path, map_location=device)
        model = cls(**model_kwargs)
        model.load_state_dict(state, strict=True)
        model.to(device)
        model.eval()
        return model
