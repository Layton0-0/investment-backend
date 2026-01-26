# API 엔드포인트 상세

## 1. 계좌 API

### 1.1 계좌 잔고 조회

**엔드포인트**: `GET /api/v1/accounts/{accountNo}/balance`

**설명**: 계좌의 현재 잔고 정보를 조회합니다.

**경로 파라미터**:
- `accountNo` (String, required): 계좌번호

**응답**:
```json
{
  "accountNo": "12345678",
  "deposit": 1000000.00,
  "availableAmount": 950000.00,
  "totalAssetValue": 5000000.00,
  "totalProfitLoss": 500000.00,
  "currency": "KRW"
}
```

**에러 코드**:
- `ACCOUNT_NOT_FOUND`: 계좌를 찾을 수 없음
- `ACCOUNT_ACCESS_DENIED`: 계좌 접근 권한 없음

---

### 1.2 보유 종목 조회

**엔드포인트**: `GET /api/v1/accounts/{accountNo}/positions`

**설명**: 계좌에 보유 중인 종목 목록을 조회합니다.

**경로 파라미터**:
- `accountNo` (String, required): 계좌번호

**응답**:
```json
[
  {
    "symbol": "005930",
    "name": "삼성전자",
    "quantity": 100,
    "averagePrice": 70000.00,
    "currentPrice": 75000.00,
    "assetValue": 7500000.00,
    "profitLoss": 500000.00,
    "profitLossRate": 7.14,
    "currency": "KRW"
  }
]
```

**에러 코드**:
- `ACCOUNT_NOT_FOUND`: 계좌를 찾을 수 없음

---

## 2. 주문 API

### 2.1 주문 실행

**엔드포인트**: `POST /api/v1/orders`

**설명**: 주식 매수/매도 주문을 실행합니다.

**요청 본문**:
```json
{
  "accountNo": "12345678",
  "symbol": "005930",
  "orderType": "BUY",
  "quantity": 10,
  "price": 75000.00
}
```

**응답**:
```json
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "accountNo": "12345678",
  "symbol": "005930",
  "orderType": "BUY",
  "quantity": 10,
  "price": 75000.00,
  "status": "PENDING",
  "orderTime": "2026-01-27T10:00:00",
  "message": "주문이 접수되었습니다."
}
```

**에러 코드**:
- `INVALID_INPUT`: 잘못된 입력값
- `EXCEEDS_MAX_INVESTMENT`: 최대 투자금액 초과
- `INVALID_ORDER_AMOUNT`: 최소 투자금액 미만
- `INSUFFICIENT_BALANCE`: 잔고 부족
- `ORDER_FAILED`: 주문 실행 실패

---

### 2.2 주문 조회

**엔드포인트**: `GET /api/v1/orders/{orderId}`

**설명**: 특정 주문의 상세 정보를 조회합니다.

**경로 파라미터**:
- `orderId` (String, required): 주문 ID

**쿼리 파라미터**:
- `accountNo` (String, required): 계좌번호

**응답**:
```json
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "accountNo": "12345678",
  "symbol": "005930",
  "orderType": "BUY",
  "quantity": 10,
  "price": 75000.00,
  "status": "EXECUTED",
  "executedQuantity": 10,
  "executedPrice": 75000.00,
  "orderTime": "2026-01-27T10:00:00",
  "executedTime": "2026-01-27T10:00:05",
  "message": "주문이 체결되었습니다."
}
```

**에러 코드**:
- `ORDER_NOT_FOUND`: 주문을 찾을 수 없음

---

### 2.3 주문 목록 조회

**엔드포인트**: `GET /api/v1/orders`

**설명**: 계좌의 주문 목록을 조회합니다.

**쿼리 파라미터**:
- `accountNo` (String, required): 계좌번호
- `status` (String, optional): 주문 상태 필터
- `startDate` (String, optional): 시작일 (ISO 8601)
- `endDate` (String, optional): 종료일 (ISO 8601)
- `page` (Integer, optional): 페이지 번호 (기본값: 0)
- `size` (Integer, optional): 페이지 크기 (기본값: 20)

**응답**:
```json
{
  "content": [
    {
      "orderId": "550e8400-e29b-41d4-a716-446655440000",
      "symbol": "005930",
      "orderType": "BUY",
      "quantity": 10,
      "price": 75000.00,
      "status": "EXECUTED",
      "orderTime": "2026-01-27T10:00:00"
    }
  ],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

---

### 2.4 주문 취소

**엔드포인트**: `DELETE /api/v1/orders/{orderId}`

**설명**: 미체결 주문을 취소합니다.

**경로 파라미터**:
- `orderId` (String, required): 주문 ID

**쿼리 파라미터**:
- `accountNo` (String, required): 계좌번호

**응답**: `204 No Content`

**에러 코드**:
- `ORDER_NOT_FOUND`: 주문을 찾을 수 없음
- `ORDER_FAILED`: 주문 취소 실패 (이미 체결됨)

---

## 3. 분석 API

### 3.1 종목 분석

**엔드포인트**: `POST /api/v1/analysis`

**설명**: 종목에 대한 기술적 분석을 수행하고 투자 추천을 제공합니다.

**요청 본문**:
```json
{
  "symbol": "005930",
  "periodDays": 30
}
```

**응답**:
```json
{
  "symbol": "005930",
  "name": "삼성전자",
  "currentPrice": 75000.00,
  "recommendation": "BUY",
  "confidence": 0.85,
  "indicators": {
    "rsi": 45.5,
    "macd": {
      "macd": 500.0,
      "signal": 450.0,
      "histogram": 50.0
    },
    "ema20": 73000.00,
    "ema60": 70000.00,
    "bollingerBands": {
      "upper": 78000.00,
      "middle": 75000.00,
      "lower": 72000.00
    },
    "atr": 2000.00,
    "vwap": 74500.00
  },
  "analysisDate": "2026-01-27T10:00:00"
}
```

**에러 코드**:
- `INVALID_INPUT`: 잘못된 입력값
- `INTERNAL_ERROR`: 분석 실패

---

## 4. 전략 API

### 4.1 전략 목록 조회

**엔드포인트**: `GET /api/v1/strategies/{accountNo}`

**설명**: 계좌의 투자 전략 목록을 조회합니다.

**경로 파라미터**:
- `accountNo` (String, required): 계좌번호

**응답**:
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "accountNo": "12345678",
    "strategyType": "SHORT_TERM",
    "status": "ACTIVE",
    "maxInvestmentAmount": 1000000.00,
    "minInvestmentAmount": 10000.00,
    "riskLevel": 0.7,
    "confidenceThreshold": 0.75,
    "lastExecutedAt": "2026-01-27T09:00:00",
    "totalExecutions": 100,
    "successCount": 70,
    "failureCount": 30,
    "totalProfitLoss": 500000.00
  }
]
```

---

### 4.2 전략 상세 조회

**엔드포인트**: `GET /api/v1/strategies/{accountNo}/{strategyType}`

**설명**: 특정 전략의 상세 정보를 조회합니다.

**경로 파라미터**:
- `accountNo` (String, required): 계좌번호
- `strategyType` (Enum, required): 전략 타입 (SHORT_TERM, MEDIUM_TERM, LONG_TERM)

**응답**: 전략 상세 정보 (4.1과 동일)

---

### 4.3 전략 생성/업데이트

**엔드포인트**: `POST /api/v1/strategies`

**설명**: 새로운 투자 전략을 생성하거나 기존 전략을 업데이트합니다.

**요청 본문**:
```json
{
  "accountNo": "12345678",
  "strategyType": "SHORT_TERM",
  "maxInvestmentAmount": 1000000.00,
  "minInvestmentAmount": 10000.00,
  "riskLevel": 0.7,
  "confidenceThreshold": 0.75
}
```

**응답**: 생성/업데이트된 전략 정보

**에러 코드**:
- `INVALID_INPUT`: 잘못된 입력값
- `INVALID_SETTING_VALUE`: 설정값이 유효 범위를 벗어남

---

### 4.4 전략 상태 변경

**엔드포인트**: `PUT /api/v1/strategies/{accountNo}/{strategyType}/status`

**설명**: 전략의 상태를 변경합니다.

**경로 파라미터**:
- `accountNo` (String, required): 계좌번호
- `strategyType` (Enum, required): 전략 타입

**요청 본문**:
```json
{
  "status": "PAUSED"
}
```

**응답**: 업데이트된 전략 정보

---

### 4.5 전략 활성화

**엔드포인트**: `POST /api/v1/strategies/{accountNo}/{strategyType}/activate`

**설명**: 중지된 전략을 활성화합니다.

**경로 파라미터**:
- `accountNo` (String, required): 계좌번호
- `strategyType` (Enum, required): 전략 타입

**응답**: 활성화된 전략 정보

---

### 4.6 전략 중지

**엔드포인트**: `POST /api/v1/strategies/{accountNo}/{strategyType}/stop`

**설명**: 실행 중인 전략을 중지합니다.

**경로 파라미터**:
- `accountNo` (String, required): 계좌번호
- `strategyType` (Enum, required): 전략 타입

**응답**: 중지된 전략 정보

---

## 5. 설정 API

### 5.1 거래 설정 조회

**엔드포인트**: `GET /api/v1/settings/{accountNo}`

**설명**: 계좌의 거래 설정을 조회합니다.

**경로 파라미터**:
- `accountNo` (String, required): 계좌번호

**응답**:
```json
{
  "accountNo": "12345678",
  "maxInvestmentAmount": 1000000.00,
  "minInvestmentAmount": 10000.00,
  "defaultCurrency": "KRW",
  "autoTradingEnabled": true,
  "riskLevel": 0.7
}
```

**에러 코드**:
- `SETTING_NOT_FOUND`: 설정을 찾을 수 없음

---

### 5.2 거래 설정 저장/업데이트

**엔드포인트**: `PUT /api/v1/settings/{accountNo}`

**설명**: 거래 설정을 저장하거나 업데이트합니다.

**경로 파라미터**:
- `accountNo` (String, required): 계좌번호

**요청 본문**:
```json
{
  "maxInvestmentAmount": 1000000.00,
  "minInvestmentAmount": 10000.00,
  "defaultCurrency": "KRW",
  "autoTradingEnabled": true,
  "riskLevel": 0.7
}
```

**응답**: 저장/업데이트된 설정 정보

**에러 코드**:
- `INVALID_INPUT`: 잘못된 입력값
- `INVALID_SETTING_VALUE`: 설정값이 유효 범위를 벗어남

---

## 6. 트레이딩 포트폴리오 API

### 6.1 트레이딩 포트폴리오 조회

**엔드포인트**: `GET /api/v1/trading-portfolios/{date}`

**설명**: 특정 날짜의 트레이딩 포트폴리오를 조회합니다.

**경로 파라미터**:
- `date` (String, required): 거래일 (ISO 8601, 예: 2026-01-27)

**응답**:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "tradingDate": "2026-01-27",
  "marketSummary": "시장 요약...",
  "topSector1": "반도체",
  "topSector2": "에너지",
  "topSector3": "방산",
  "riskManagementStrategy": "리스크 관리 전략...",
  "positionSize": 10000.00,
  "items": [
    {
      "symbol": "NVDA",
      "name": "NVIDIA Corporation",
      "entryPriceMin": 480.00,
      "entryPriceMax": 490.00,
      "stopLossPrice": 470.00,
      "targetPrice1": 510.00,
      "targetPrice2": 530.00,
      "expectedReturnRate": 5.2,
      "riskRewardRatio": 2.5,
      "technicalBasis": "기술적 근거...",
      "supplyDemandBasis": "수급 근거...",
      "catalystFactor": "촉매 요인...",
      "buyTime": "10:00:00",
      "sellTime": "12:00:00",
      "investmentAmount": 10000.00,
      "expectedProfit": 520.00,
      "ranking": 1
    }
  ]
}
```

**에러 코드**:
- `NOT_FOUND`: 포트폴리오를 찾을 수 없음

---

## 7. 배치 관리 API

### 7.1 배치 작업 목록 조회

**엔드포인트**: `GET /api/v1/batch/jobs`

**설명**: 스케줄러로 실행되는 배치 작업 목록을 조회합니다.

**응답**:
```json
[
  {
    "jobName": "StrategyScheduler",
    "description": "전략 실행 스케줄러",
    "cronExpression": "0 0 * * * ?",
    "lastExecutionTime": "2026-01-27T09:00:00",
    "nextExecutionTime": "2026-01-27T10:00:00",
    "status": "RUNNING"
  },
  {
    "jobName": "TradingPortfolioScheduler",
    "description": "트레이딩 포트폴리오 생성 스케줄러",
    "cronExpression": "0 0 9 * * ?",
    "lastExecutionTime": "2026-01-27T09:00:00",
    "nextExecutionTime": "2026-01-28T09:00:00",
    "status": "SCHEDULED"
  }
]
```
