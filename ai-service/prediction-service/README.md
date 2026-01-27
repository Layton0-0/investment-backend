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

## 다음 단계

1. LSTM 모델 구현 (`app/models/lstm_model.py`)
2. 데이터 로더 구현 (`app/utils/data_loader.py`)
3. 예측 서비스 로직 구현 (`app/services/prediction_service.py`)
4. 모델 학습 파이프라인 구축
