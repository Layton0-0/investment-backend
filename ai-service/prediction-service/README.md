# Prediction Service

AI 기반 주식 가격 예측 서비스

## 빠른 시작

### 로컬 실행

```bash
# 가상환경 생성
python3.11 -m venv venv
source venv/bin/activate

# 의존성 설치
pip install -r requirements.txt

# 서비스 실행
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

### Docker 실행

```bash
docker build -t ai-prediction-service:latest .
docker run -d -p 8000:8000 ai-prediction-service:latest
```

## API 문서

- Swagger UI: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc

## LSTM 예측 (초기)

- **모델**: `app/models/lstm_model.py` (PyTorch LSTMPredictor)
- **전처리**: `app/data/`, `app/preprocessing/` (시계열 로드·정규화·시퀀스)
- **학습**: `python -m app.train --data-csv path/to/ohlcv.csv --lookback 30 --epochs 10 --output-dir ./models` (프로젝트 루트에서 `cd ai-service/prediction-service` 후 실행)
- **학습 데이터 수집**: `python scripts/fetch_training_data.py --symbols AAPL,MSFT --start 2020-01-01 --end 2025-01-01 --output data/train.csv` (프로젝트 루트 `scripts/`)
- **서빙**: `POST /api/v1/predict`에 optional `series`(OHLCV 배열)·`currentPrice` 전달 시 LSTM 추론 사용. 환경변수 `MODEL_PATH` 또는 `LSTM_MODEL_PATH`에 학습된 `.pt` 경로 설정. 미설정 시 Mock 응답.

## 테스트

```bash
# 가상환경 활성화 후
pip install -r requirements.txt
python -m unittest discover -s tests -v
# 또는: python -m pytest tests/ -v
```
