"""
정규화 및 시퀀스 슬라이딩
"""
from typing import List, Optional, Tuple, Union

import numpy as np


def normalize_close_minmax(
    close: np.ndarray,
    min_val: Optional[float] = None,
    max_val: Optional[float] = None,
) -> Tuple[np.ndarray, float, float]:
    """
    close 시리즈를 [0, 1] min-max 정규화.
    Returns:
        normalized, min_val, max_val (학습 시 사용한 min/max로 서빙 시 동일 적용)
    """
    min_val = float(np.nanmin(close)) if min_val is None else min_val
    max_val = float(np.nanmax(close)) if max_val is None else max_val
    span = max_val - min_val
    if span <= 0:
        span = 1.0
    normalized = (close.astype(np.float64) - min_val) / span
    return np.clip(normalized, 0.0, 1.0).astype(np.float32), min_val, max_val


def ohlcv_to_features(open_: np.ndarray, high: np.ndarray, low: np.ndarray,
                      close: np.ndarray, volume: np.ndarray,
                      use_ohlcv: bool = True) -> np.ndarray:
    """
    OHLCV 배열을 (T, features)로 합침.
    use_ohlcv=True: (T, 5), use_ohlcv=False: (T, 1) close만
    """
    if use_ohlcv:
        return np.stack([open_, high, low, close, volume], axis=1)
    return close.reshape(-1, 1).astype(np.float32)


def build_sequences(
    close: np.ndarray,
    open_: Optional[np.ndarray] = None,
    high: Optional[np.ndarray] = None,
    low: Optional[np.ndarray] = None,
    volume: Optional[np.ndarray] = None,
    lookback: int = 30,
    target_next_close: bool = True,
    use_ohlcv: bool = True,
    normalize: bool = True,
) -> Tuple[np.ndarray, np.ndarray, Optional[float], Optional[float]]:
    """
    시계열에서 (X, y) 시퀀스 생성. 시간 순서 유지.
    X: (N, lookback, features), y: (N,) 다음 close 또는 수익률
    Returns:
        sequences, targets, min_close, max_close (서빙 시 동일 정규화용)
    """
    n = len(close)
    if n < lookback + 1:
        return np.zeros((0, lookback, 5 if use_ohlcv else 1), dtype=np.float32), \
               np.zeros(0, dtype=np.float32), None, None

    open_ = open_ if open_ is not None else close
    high = high if high is not None else close
    low = low if low is not None else close
    volume = volume if volume is not None else np.zeros_like(close)

    min_c, max_c = None, None
    if normalize:
        close_norm, min_c, max_c = normalize_close_minmax(close)
        open_norm, _, _ = normalize_close_minmax(open_, min_c, max_c)
        high_norm, _, _ = normalize_close_minmax(high, min_c, max_c)
        low_norm, _, _ = normalize_close_minmax(low, min_c, max_c)
        vol_norm = volume.astype(np.float64)
        if np.nanmax(vol_norm) > 0:
            vol_norm = vol_norm / np.nanmax(vol_norm)
        vol_norm = vol_norm.astype(np.float32)
        features = ohlcv_to_features(open_norm, high_norm, low_norm, close_norm, vol_norm, use_ohlcv)
    else:
        features = ohlcv_to_features(open_, high, low, close, volume, use_ohlcv)

    seqs = []
    tgts = []
    for i in range(lookback, n):
        seqs.append(features[i - lookback:i])
        if target_next_close:
            tgts.append(float(close[i]))
        else:
            ret = (close[i] - close[i - 1]) / (close[i - 1] + 1e-9)
            tgts.append(float(ret))

    return np.array(seqs), np.array(tgts), min_c, max_c


def preprocess_series_for_serving(
    close: Union[List[float], np.ndarray],
    open_: Optional[Union[List[float], np.ndarray]] = None,
    high: Optional[Union[List[float], np.ndarray]] = None,
    low: Optional[Union[List[float], np.ndarray]] = None,
    volume: Optional[Union[List[float], np.ndarray]] = None,
    lookback: Optional[int] = None,
    use_ohlcv: bool = True,
    min_close: Optional[float] = None,
    max_close: Optional[float] = None,
) -> np.ndarray:
    """
    서빙 시 한 요청의 시계열을 모델 입력 (1, lookback, features)로 변환.
    lookback보다 길면 마지막 lookback만 사용. 짧으면 빈 배열 반환 시 호출자가 Mock 사용.
    """
    close = np.asarray(close, dtype=np.float64)
    n = len(close)
    lb = lookback or min(30, n)
    if n < lb:
        return np.zeros((0, lb, 5 if use_ohlcv else 1), dtype=np.float32)

    open_ = np.asarray(open_, dtype=np.float64) if open_ is not None else close
    high = np.asarray(high, dtype=np.float64) if high is not None else close
    low = np.asarray(low, dtype=np.float64) if low is not None else close
    volume = np.asarray(volume, dtype=np.float64) if volume is not None else np.zeros_like(close)

    # 마지막 lookback만
    close = close[-lb:]
    open_ = open_[-lb:]
    high = high[-lb:]
    low = low[-lb:]
    volume = volume[-lb:]

    if min_close is not None and max_close is not None:
        span = max_close - min_close
        span = span if span > 0 else 1.0
        close_n = (close - min_close) / span
        open_n = (open_ - min_close) / span
        high_n = (high - min_close) / span
        low_n = (low - min_close) / span
    else:
        min_c, max_c = np.nanmin(close), np.nanmax(close)
        span = max_c - min_c if max_c > min_c else 1.0
        close_n = (close - min_c) / span
        open_n = (open_ - min_c) / span
        high_n = (high - min_c) / span
        low_n = (low - min_c) / span
    if np.nanmax(volume) > 0:
        vol_n = volume / np.nanmax(volume)
    else:
        vol_n = volume.astype(np.float32)
    features = ohlcv_to_features(open_n.astype(np.float32), high_n.astype(np.float32),
                                 low_n.astype(np.float32), close_n.astype(np.float32),
                                 vol_n.astype(np.float32), use_ohlcv)
    return features[np.newaxis, :, :]
