"""
LSTM 모델 로드 (lazy). MODEL_PATH 환경변수.
"""
import os
from typing import Optional

from app.models.lstm_model import LSTMPredictor


_model: Optional[LSTMPredictor] = None
_model_path_checked: bool = False


def get_model(
    input_size: int = 5,
    hidden_size: int = 64,
    num_layers: int = 2,
) -> Optional[LSTMPredictor]:
    """Lazy load LSTM from MODEL_PATH or LSTM_MODEL_PATH. 실패 시 None."""
    global _model, _model_path_checked
    if _model is not None:
        return _model
    if _model_path_checked:
        return None
    _model_path_checked = True
    path = os.environ.get("MODEL_PATH") or os.environ.get("LSTM_MODEL_PATH")
    if not path or not os.path.isfile(path):
        return None
    try:
        _model = LSTMPredictor.load_from_path(
            path,
            model_kwargs=dict(
                input_size=input_size,
                hidden_size=hidden_size,
                num_layers=num_layers,
                dropout=0.1,
                predict_return=False,
            ),
        )
        return _model
    except Exception:
        return None
