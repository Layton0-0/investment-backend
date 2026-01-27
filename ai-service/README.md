# AI Service - Python FastAPI 서비스

## 개요

Investment Choi 프로젝트의 AI/ML 예측 서비스를 제공하는 Python FastAPI 애플리케이션입니다.

## 디렉토리 구조

```
ai-service/
├── prediction-service/      # 예측 서비스
│   ├── app/
│   │   ├── __init__.py
│   │   ├── main.py          # FastAPI 앱 진입점
│   │   ├── api/             # API 엔드포인트
│   │   │   ├── __init__.py
│   │   │   └── routes.py
│   │   ├── models/          # ML 모델
│   │   │   ├── __init__.py
│   │   │   └── lstm_model.py
│   │   ├── services/        # 비즈니스 로직
│   │   │   ├── __init__.py
│   │   │   └── prediction_service.py
│   │   └── utils/           # 유틸리티
│   │       ├── __init__.py
│   │       └── data_loader.py
│   ├── requirements.txt
│   ├── Dockerfile
│   └── README.md
└── README.md
```

## 설치 및 실행

### 1. 가상환경 생성

```bash
cd ai-service/prediction-service
python3.11 -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
```

### 2. 의존성 설치

```bash
pip install -r requirements.txt
```

### 3. 서비스 실행

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

### 4. API 문서 확인

- Swagger UI: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc

## API 엔드포인트

### Health Check
- `GET /api/v1/health` - 서비스 상태 확인

### 예측 API
- `POST /api/v1/predict` - 단일 종목 예측
- `POST /api/v1/predict/batch` - 배치 예측

## 환경 변수

```bash
# 서비스 설정
PORT=8000
LOG_LEVEL=INFO

# 모델 설정
MODEL_PATH=./models/lstm_model.pth
USE_GPU=false

# 데이터 소스
MARKET_DATA_API_URL=http://localhost:8080/api/v1
```

## Docker 실행

```bash
docker build -t ai-prediction-service:latest .
docker run -d -p 8000:8000 ai-prediction-service:latest
```
