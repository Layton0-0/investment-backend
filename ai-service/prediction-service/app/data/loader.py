"""
시계열 로드 및 PyTorch 데이터셋
"""
from dataclasses import dataclass
from pathlib import Path
from typing import List, Optional, Tuple

import numpy as np
import pandas as pd
import torch
from torch.utils.data import Dataset


@dataclass
class OHLCVRow:
    """OHLCV 한 행"""
    date: str
    open: float
    high: float
    low: float
    close: float
    volume: float


def load_ohlcv_csv(
    path: str,
    date_col: str = "date",
    symbol_col: Optional[str] = "symbol",
    symbol_filter: Optional[str] = None,
) -> pd.DataFrame:
    """
    CSV에서 OHLCV 시계열 로드.
    컬럼: date, (symbol), open, high, low, close, volume
    """
    df = pd.read_csv(path)
    required = [date_col, "open", "high", "low", "close", "volume"]
    for col in required:
        if col not in df.columns:
            raise ValueError(f"CSV에 '{col}' 컬럼이 없습니다.")
    df[date_col] = pd.to_datetime(df[date_col])
    df = df.sort_values(date_col).reset_index(drop=True)
    if symbol_col and symbol_col in df.columns and symbol_filter:
        df = df[df[symbol_col] == symbol_filter].reset_index(drop=True)
    return df


def dataframe_to_ohlcv_rows(df: pd.DataFrame, date_col: str = "date") -> List[OHLCVRow]:
    """DataFrame을 OHLCVRow 리스트로 변환"""
    rows = []
    for _, r in df.iterrows():
        rows.append(
            OHLCVRow(
                date=str(r[date_col])[:10],
                open=float(r["open"]),
                high=float(r["high"]),
                low=float(r["low"]),
                close=float(r["close"]),
                volume=float(r["volume"]) if pd.notna(r["volume"]) else 0.0,
            )
        )
    return rows


class SeriesDataset(Dataset):
    """
    시퀀스 슬라이딩 데이터셋.
    X: (seq_len, features), y: 다음 close 또는 수익률 1개
    """

    def __init__(
        self,
        sequences: np.ndarray,
        targets: np.ndarray,
    ):
        """
        Args:
            sequences: (N, seq_len, features) float32
            targets: (N,) float32, 다음 close 또는 수익률
        """
        self.sequences = torch.from_numpy(sequences.astype(np.float32))
        self.targets = torch.from_numpy(targets.astype(np.float32))

    def __len__(self) -> int:
        return len(self.targets)

    def __getitem__(self, idx: int) -> Tuple[torch.Tensor, torch.Tensor]:
        return self.sequences[idx], self.targets[idx]
