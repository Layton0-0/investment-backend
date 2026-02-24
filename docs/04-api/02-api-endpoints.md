# API 엔드포인트 상세

## 1. 계좌 API

### 1.1 계좌 잔고 조회

**엔드포인트**: `GET /api/v1/accounts/{accountNo}/balance`

**설명**: 계좌의 현재 잔고 정보를 조회합니다.

**경로 파라미터**:
- `accountNo` (String, required): 계좌번호

**요청 예시**:
```bash
curl -X GET "http://localhost:8080/api/v1/accounts/12345678/balance"
```

**성공 응답 (200 OK)**:
```json
{
  "accountNo": "12345678",
  "deposit": 1000000.00,
  "availableAmount": 950000.00,
  "totalAssetValue": 1500000.00,
  "totalProfitLoss": 500000.00,
  "currency": "KRW"
}
```

**에러 응답 (404 Not Found)**:
```json
{
  "code": "ACCOUNT_NOT_FOUND",
  "message": "계좌를 찾을 수 없습니다: 12345678",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-01-26T10:00:00"
}
```

**에러 코드**:
- `ACCOUNT_NOT_FOUND`: 계좌를 찾을 수 없음
- `ACCOUNT_ACCESS_DENIED`: 계좌 접근 권한 없음

**DTO 정의**:
```java
{
  "accountNo": String,          // 계좌번호
  "deposit": BigDecimal,        // 예수금
  "availableAmount": BigDecimal, // 주문가능금액
  "totalAssetValue": BigDecimal, // 총 평가금액
  "totalProfitLoss": BigDecimal, // 총 손익
  "currency": String            // 통화
}
```

---

### 1.2 보유 종목 조회

**엔드포인트**: `GET /api/v1/accounts/{accountNo}/positions`

**설명**: 계좌에 보유 중인 종목 목록을 조회합니다.

**경로 파라미터**:
- `accountNo` (String, required): 계좌번호

**요청 예시**:
```bash
curl -X GET "http://localhost:8080/api/v1/accounts/12345678/positions"
```

**성공 응답 (200 OK)**:
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
  },
  {
    "symbol": "000660",
    "name": "SK하이닉스",
    "quantity": 50,
    "averagePrice": 120000.00,
    "currentPrice": 130000.00,
    "assetValue": 6500000.00,
    "profitLoss": 500000.00,
    "profitLossRate": 8.33,
    "currency": "KRW"
  }
]
```

**에러 응답 (404 Not Found)**:
```json
{
  "code": "ACCOUNT_NOT_FOUND",
  "message": "계좌를 찾을 수 없습니다: 12345678",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-01-26T10:00:00"
}
```

**에러 코드**:
- `ACCOUNT_NOT_FOUND`: 계좌를 찾을 수 없음
- `ACCOUNT_ACCESS_DENIED`: 계좌 접근 권한 없음

**DTO 정의**:
```java
{
  "symbol": String,             // 종목 코드
  "name": String,               // 종목명
  "quantity": Integer,          // 보유 수량
  "averagePrice": BigDecimal,   // 평균 매수가
  "currentPrice": BigDecimal,   // 현재가
  "assetValue": BigDecimal,      // 평가금액
  "profitLoss": BigDecimal,      // 평가손익
  "profitLossRate": BigDecimal,  // 수익률 (%)
  "currency": String             // 통화
}
```

### 1.3 매수가능조회

**엔드포인트**: `GET /api/v1/accounts/{accountNo}/buyable-amount`

**설명**: 특정 종목의 매수 가능 금액 및 수량을 조회합니다.

**경로 파라미터**:
- `accountNo` (String, required): 계좌번호

**쿼리 파라미터**:
- `symbol` (String, required): 종목코드
- `price` (BigDecimal, required): 주문가격

**요청 예시**:
```bash
curl -X GET "http://localhost:8080/api/v1/accounts/12345678/buyable-amount?symbol=005930&price=75000"
```

**성공 응답 (200 OK)**:
```json
{
  "accountNo": "12345678",
  "symbol": "005930",
  "price": 75000.00,
  "buyableAmount": 950000.00,
  "buyableQuantity": 12,
  "currency": "KRW"
}
```

---

### 1.4 매도가능수량조회

**엔드포인트**: `GET /api/v1/accounts/{accountNo}/sellable-quantity`

**설명**: 특정 종목의 매도 가능 수량을 조회합니다.

**경로 파라미터**:
- `accountNo` (String, required): 계좌번호

**쿼리 파라미터**:
- `symbol` (String, required): 종목코드

**요청 예시**:
```bash
curl -X GET "http://localhost:8080/api/v1/accounts/12345678/sellable-quantity?symbol=005930"
```

**성공 응답 (200 OK)**:
```json
{
  "accountNo": "12345678",
  "symbol": "005930",
  "sellableQuantity": 100,
  "holdingQuantity": 100,
  "averagePrice": 70000.00,
  "currency": "KRW"
}
```

---

### 1.5 주문체결조회

**엔드포인트**: `GET /api/v1/accounts/{accountNo}/order-history`

**설명**: 특정 기간의 주문 체결 내역을 조회합니다.

**경로 파라미터**:
- `accountNo` (String, required): 계좌번호

**쿼리 파라미터**:
- `startDate` (LocalDate, required): 시작일 (ISO 8601 형식: yyyy-MM-dd)
- `endDate` (LocalDate, required): 종료일 (ISO 8601 형식: yyyy-MM-dd)

**요청 예시**:
```bash
curl -X GET "http://localhost:8080/api/v1/accounts/12345678/order-history?startDate=2026-01-01&endDate=2026-01-31"
```

**성공 응답 (200 OK)**:
```json
[
  {
    "accountNo": "12345678",
    "symbol": "005930",
    "orderNo": "20260101001",
    "orderType": "BUY",
    "orderQuantity": 10,
    "orderPrice": 75000.00,
    "executedQuantity": 10,
    "executedPrice": 75000.00,
    "orderStatus": "EXECUTED",
    "orderTime": "2026-01-01T09:00:00",
    "executedTime": "2026-01-01T09:00:05",
    "currency": "KRW"
  }
]
```

---

### 1.6 투자계좌자산현황조회

**엔드포인트**: `GET /api/v1/accounts/{accountNo}/assets`

**설명**: 계좌의 자산 현황을 종합 조회합니다.

**경로 파라미터**:
- `accountNo` (String, required): 계좌번호

**요청 예시**:
```bash
curl -X GET "http://localhost:8080/api/v1/accounts/12345678/assets"
```

**성공 응답 (200 OK)**:
```json
{
  "accountNo": "12345678",
  "totalAssetValue": 1500000.00,
  "deposit": 1000000.00,
  "stockValue": 500000.00,
  "totalProfitLoss": 50000.00,
  "totalProfitLossRate": 3.33,
  "orderableCash": 950000.00,
  "currency": "KRW"
}
```

**에러 응답 (404 Not Found)**: 계좌가 없거나 해당 계좌에 대한 API 키를 찾을 수 없을 때 `ACCOUNT_NOT_FOUND` 코드로 404를 반환합니다. (전역 예외 핸들러에서 `ACCOUNT_NOT_FOUND` → 404 매핑.)

---

### 1.7 기간별손익조회

**엔드포인트**: `GET /api/v1/accounts/{accountNo}/profit-loss`

**설명**: 특정 기간의 손익 정보를 조회합니다.

**경로 파라미터**:
- `accountNo` (String, required): 계좌번호

**쿼리 파라미터**:
- `startDate` (LocalDate, required): 시작일 (ISO 8601 형식: yyyy-MM-dd)
- `endDate` (LocalDate, required): 종료일 (ISO 8601 형식: yyyy-MM-dd)

**요청 예시**:
```bash
curl -X GET "http://localhost:8080/api/v1/accounts/12345678/profit-loss?startDate=2026-01-01&endDate=2026-01-31"
```

**성공 응답 (200 OK)**:
```json
{
  "accountNo": "12345678",
  "startDate": "2026-01-01",
  "endDate": "2026-01-31",
  "totalProfitLoss": 50000.00,
  "totalProfitLossRate": 3.33,
  "realizedProfitLoss": 30000.00,
  "unrealizedProfitLoss": 20000.00,
  "dailyProfitLossList": [
    {
      "date": "2026-01-01",
      "profitLoss": 1000.00,
      "profitLossRate": 0.07
    }
  ],
  "currency": "KRW"
}
```

---

### 1.8 계좌 API 에러 코드

- `ACCOUNT_NOT_FOUND`: 계좌를 찾을 수 없음
- `ACCOUNT_ACCESS_DENIED`: 계좌 접근 권한 없음
- `INSUFFICIENT_BALANCE`: 잔고 부족

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
  "message": "주문이 체결되었습니다.",
  "signalType": "VOLATILITY_BREAKOUT",
  "exitRuleType": null
}
```
- `signalType` (String, optional): 거래 사유 — 진입 시그널 유형(파이프라인 매수 시). 예: VOLATILITY_BREAKOUT, DUAL_MOMENTUM.
- `exitRuleType` (String, optional): 거래 사유 — 청산 규칙 유형(파이프라인 매도 시). 예: ATR_TRAILING_STOP, TIME_CUT, STOP_LOSS.

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

**응답** (주문 목록은 페이징 없이 배열 반환):
```json
[
  {
    "orderId": "550e8400-e29b-41d4-a716-446655440000",
    "accountNo": "12345678",
    "symbol": "005930",
    "orderType": "BUY",
    "quantity": 10,
    "price": 75000.00,
    "status": "EXECUTED",
    "orderTime": "2026-01-27T10:00:00",
    "signalType": "VOLATILITY_BREAKOUT",
    "exitRuleType": null
  }
]
```
- 각 항목에 `signalType`, `exitRuleType` (optional) 포함. 파이프라인 주문이 아닌 경우 null.

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

### 2.5 미체결 전체 취소

**엔드포인트**: `POST /api/v1/orders/cancel-all-pending`

**설명**: 해당 계좌의 대기 중(PENDING) 주문을 모두 취소합니다. 모의/실계좌 구분은 계좌번호로 이루어지며, 동일 API를 모의·실계좌 모두 사용합니다.

**쿼리 파라미터**:
- `accountNo` (String, required): 계좌번호

**응답** (200 OK):
```json
{
  "accountNo": "50161075-01",
  "cancelledCount": 2
}
```

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

### 3.2 상관관계 분석

**엔드포인트**: `GET /api/v1/analysis/correlation`

**설명**: 계좌 포지션 또는 종목 목록의 일봉 수익률 기반 Pearson 상관계수 행렬을 반환합니다. `accountNo`가 있으면 해당 계좌 보유 종목 기준, 없으면 `symbols`·`market`·`from`·`to` 쿼리로 분석합니다. 최소 2종목·20일 이상 공통 일봉 데이터가 필요하며, 부족 시 빈 행렬을 반환합니다.

**쿼리 파라미터**:
- `accountNo` (optional): 계좌번호. 지정 시 인증 사용자의 해당 계좌 보유 종목 기준.
- `symbols` (optional): 종목 코드 목록 (쉼표 구분). accountNo 미지정 시 필수.
- `market` (optional): 시장 (KR/US). 기본값 US.
- `from` (optional): 기간 시작일 (yyyy-MM-dd). 미지정 시 to 기준 60일 전.
- `to` (optional): 기간 종료일 (yyyy-MM-dd). 미지정 시 오늘.

**성공 응답 (200 OK)**:
```json
{
  "market": "US",
  "symbols": ["AAPL", "MSFT"],
  "fromDate": "2025-12-01",
  "toDate": "2026-02-10",
  "matrix": [[1.0, 0.5], [0.5, 1.0]]
}
```
- `matrix[i][j]`: symbols[i] vs symbols[j] 상관계수 (-1 ~ 1). 대각선은 1. 데이터 부족 시 `symbols`·`matrix`는 빈 배열.

**인증**: accountNo 사용 시 `isAuthenticated()` 필요.

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

### 5.3 Kill Switch (Phase 2)

**엔드포인트**: `GET /api/v1/system/kill-switch`

**설명**: 긴급 시 전체 주문 차단(Kill Switch) 상태를 조회합니다. 인증된 사용자 조회 가능.

**응답 (200 OK)**:
```json
{ "haltAllOrders": false }
```

---

**엔드포인트**: `PUT /api/v1/system/kill-switch`

**설명**: Kill Switch를 설정합니다. **ADMIN** 역할만 설정 가능.

**요청 본문**:
```json
{ "haltAllOrders": true }
```

**응답 (200 OK)**: 설정 반영된 상태
```json
{ "haltAllOrders": true }
```

**참고**: `haltAllOrders=true` 시 모든 주문이 Pre-Trade 컴플라이언스에서 거부(ORDER_REJECTED, 403)됩니다.

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

**엔드포인트**: `GET /api/v1/batch/jobs` (권장. nginx가 `/api`만 백엔드로 전달하므로 이 경로 사용.)  
레거시: `GET /batch/api/jobs` (BatchManagementController. nginx에 `/batch` location 없으면 404.)

**설명**: 스케줄러로 실행되는 배치 작업 목록을 조회합니다. SPA 프론트는 `GET /api/v1/batch/jobs`로 연동. [11-api-frontend-mapping.md](./11-api-frontend-mapping.md) §5.1 참조.

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

## 8. 시장 데이터 API

### 8.1 단일 종목 현재가 조회

**엔드포인트**: `GET /api/v1/market-data/current-price/{symbol}`

**설명**: 한국투자증권 API를 통해 단일 종목의 실시간 현재가 정보를 조회합니다.

**경로 파라미터**:
- `symbol` (String, required): 종목 코드 (6자리 또는 종목명), 예: "005930" 또는 "삼성전자"

**요청 예시**:
```bash
curl -X GET "http://localhost:8080/api/v1/market-data/current-price/005930"
```

**성공 응답 (200 OK)**:
```json
{
  "symbol": "005930",
  "name": "삼성전자",
  "currentPrice": 75000.00,
  "changeRate": 1.5,
  "changeAmount": 750.00,
  "previousClose": 49250.00,
  "openPrice": 49300.00,
  "highPrice": 50200.00,
  "lowPrice": 49200.00,
  "volume": 1000000,
  "tradingValue": 50000000000.00,
  "marketCap": 1000000000000.00,
  "listedShares": 20000000,
  "queriedAt": "2026-01-28T10:30:00"
}
```

**에러 응답 (404 Not Found)**:
```json
{
  "code": "SYMBOL_NOT_FOUND",
  "message": "종목을 찾을 수 없습니다: 005930",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-01-28T10:30:00"
}
```

**에러 코드**:
- `SYMBOL_NOT_FOUND`: 종목을 찾을 수 없음
- `API_ERROR`: 한국투자증권 API 호출 실패

---

### 8.2 여러 종목 현재가 일괄 조회

**엔드포인트**: `POST /api/v1/market-data/current-prices`

**설명**: 한국투자증권 API를 통해 여러 종목의 실시간 현재가 정보를 일괄 조회합니다.

**요청 바디**:
```json
[
  "005930",
  "000660",
  "035420"
]
```

**요청 예시**:
```bash
curl -X POST "http://localhost:8080/api/v1/market-data/current-prices" \
  -H "Content-Type: application/json" \
  -d '["005930", "000660", "035420"]'
```

**성공 응답 (200 OK)**:
```json
[
  {
    "symbol": "005930",
    "name": "삼성전자",
    "currentPrice": 75000.00,
    "changeRate": 1.5,
    "changeAmount": 750.00,
    "previousClose": 49250.00,
    "openPrice": 49300.00,
    "highPrice": 50200.00,
    "lowPrice": 49200.00,
    "volume": 1000000,
    "tradingValue": 50000000000.00,
    "marketCap": 1000000000000.00,
    "listedShares": 20000000,
    "queriedAt": "2026-01-28T10:30:00"
  },
  {
    "symbol": "000660",
    "name": "SK하이닉스",
    "currentPrice": 130000.00,
    "changeRate": 2.0,
    "changeAmount": 2600.00,
    "previousClose": 127400.00,
    "openPrice": 128000.00,
    "highPrice": 131000.00,
    "lowPrice": 127500.00,
    "volume": 500000,
    "tradingValue": 65000000000.00,
    "marketCap": 2000000000000.00,
    "listedShares": 15000000,
    "queriedAt": "2026-01-28T10:30:00"
  }
]
```

**에러 응답 (400 Bad Request)**:
```json
{
  "code": "INVALID_REQUEST",
  "message": "종목 코드 목록이 비어있습니다",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-01-28T10:30:00"
}
```

**에러 코드**:
- `INVALID_REQUEST`: 잘못된 요청 (종목 코드 목록이 비어있음 등)
- `API_ERROR`: 한국투자증권 API 호출 실패

**DTO 정의**:
```java
{
  "symbol": String,              // 종목 코드 (6자리)
  "name": String,               // 종목명
  "currentPrice": BigDecimal,    // 현재가
  "changeRate": BigDecimal,      // 전일 대비 등락률 (%)
  "changeAmount": BigDecimal,    // 전일 대비 등락액
  "previousClose": BigDecimal,   // 전일 종가
  "openPrice": BigDecimal,       // 시가
  "highPrice": BigDecimal,       // 고가
  "lowPrice": BigDecimal,        // 저가
  "volume": Long,                // 거래량
  "tradingValue": BigDecimal,    // 거래대금
  "marketCap": BigDecimal,       // 시가총액
  "listedShares": Long,          // 상장주식수
  "queriedAt": LocalDateTime     // 조회 시각
}
```

### 8.3 일봉 차트 조회

**엔드포인트**: `GET /api/v1/market-data/daily-chart`

**설명**: TB_DAILY_STOCK 기반 종목·시장·기간별 일봉 데이터. from/to 미지정 시 최근 1년, 최대 365일. 데이터 없으면 200 + 빈 배열.

**쿼리 파라미터**:
| 이름 | 필수 | 설명 |
|------|------|------|
| symbol | O | 종목 코드 (예: 005930) |
| market | - | 시장 (KR, US). 기본값 KR |
| from | - | 시작일 (yyyy-MM-dd) |
| to | - | 종료일 (yyyy-MM-dd) |

**성공 응답 (200 OK)**:
```json
[
  { "date": "2025-01-02", "open": 72000, "high": 73500, "low": 71800, "close": 73000, "volume": 12000000 },
  { "date": "2025-01-03", "open": 73100, "high": 74200, "low": 72800, "close": 73800, "volume": 9800000 }
]
```

---

## 9. 리스크 리포트 API

리스크 리포트 화면(`/risk`)에서 킬스위치·일일 손실 한도·리스크 게이트·계좌별 MDD·한도 설정·이력을 조회합니다. **인증 필요** (`isAuthenticated()`). 기존 `TradingHaltService`, `RiskGateService`, `DailyLossLimitService`, `PortfolioPeakService` 등을 조합해 DTO로 반환합니다.

### 9.1 리스크 요약

**엔드포인트**: `GET /api/v1/risk/summary`

**설명**: 킬스위치·리스크 게이트·계좌별 일일 손실 한도·MDD·VaR/CVaR 요약을 반환합니다. 사용자(인증 주체) 기준으로 소유 계좌만 포함됩니다. **VaR 방법론**: `var95Pct`, `cvar95Pct`는 현재 **단순 파라메트릭**(1.65σ·2.06σ, 일일 변동성 가정) 기반 1일 VaR 95%, CVaR 95%(%)이며, 꼬리 위험 보완을 위한 역사적 시뮬레이션 또는 Monte Carlo VaR은 향후 선택 사항으로 둔다.

**성공 응답 (200 OK)**: `totalCurrentValue`, `maxMddPct`, `var95Pct`, `cvar95Pct` 포함. 예:
```json
{
  "killSwitchActive": false,
  "regimeGateEnabled": true,
  "riskGateAllowsNewBuy": true,
  "riskGateSizeMultiplier": 1.0,
  "accounts": [...],
  "totalCurrentValue": 10500000,
  "maxMddPct": 0.05,
  "var95Pct": 1.65,
  "cvar95Pct": 2.06
}
```

**에러 응답 (401 Unauthorized)**: 인증되지 않은 경우 `code: "UNAUTHORIZED"`.

---

### 9.2 리스크 한도 설정

**엔드포인트**: `GET /api/v1/risk/limits`

**설명**: 일일 손실 한도·VIX 임계값·고변동 시 축소 비율 등 한도 설정 요약을 반환합니다. `application.yml` / `RiskProperties` 기반입니다.

**성공 응답 (200 OK)**:
```json
{
  "regimeGateEnabled": true,
  "vixThreshold": 30,
  "reduceSizeOnHighVolPct": 50,
  "dailyLossLimitPct": 5
}
```

---

### 9.3 리스크 이력

**엔드포인트**: `GET /api/v1/risk/history?from={yyyy-MM-dd}&to={yyyy-MM-dd}`

**설명**: 게이트 축소·손실 한도 도달 등 리스크 이력을 반환합니다. 1차는 저장 구조 없음으로 빈 배열을 반환합니다. 쿼리 `from`, `to`는 선택이며, 미입력 시 기본 30일 전~오늘입니다.

**쿼리 파라미터**:
- `from` (optional): 시작일 (yyyy-MM-dd)
- `to` (optional): 종료일 (yyyy-MM-dd)

**성공 응답 (200 OK)**: `RiskHistoryItemDto[]` (예: `[]`). 항목이 있으면 `eventType`, `accountNoMasked`, `description`, `occurredAt`(ISO-8601) 포함.

### 9.4 포트폴리오 리스크 메트릭

**엔드포인트**: `GET /api/v1/risk/portfolio-metrics?accountNo={accountNo}`

**설명**: 단일 계좌의 VaR/CVaR/MDD·Sharpe/Sortino(일수익 시계열 있으면) 반환. 해당 계좌가 사용자 소유가 아니면 404.

**인증**: `isAuthenticated()`. **응답**: `PortfolioRiskMetricsDto` (accountNoMasked, currentValue, mddPct, var95Pct, cvar95Pct, sharpeRatio, sortinoRatio).

---

## 9.5 대시보드 API

대시보드 화면에서 성과 요약(총 평가액·MDD·Sharpe·VaR 등) 카드용 데이터를 조회합니다. **인증 필요** (`isAuthenticated()`).

### 9.5.1 성과 요약

**엔드포인트**: `GET /api/v1/dashboard/performance-summary`

**설명**: 사용자 계좌 합산 기준 총 평가액·최대 MDD·Sharpe·Sortino·1일 VaR 95%·CVaR 95%를 반환합니다. 리스크 요약(RiskReportService.getSummary) 데이터를 대시보드용 DTO로 매핑합니다.

**성공 응답 (200 OK)**:
```json
{
  "totalCurrentValue": 15000000,
  "maxMddPct": 0.12,
  "sharpeRatio": null,
  "sortinoRatio": null,
  "var95Pct": 1.65,
  "cvar95Pct": 2.06
}
```

**DTO**: `DashboardPerformanceSummaryDto` — totalCurrentValue, maxMddPct, sharpeRatio, sortinoRatio, var95Pct, cvar95Pct. 데이터 없으면 null.

**기타 API 참고**: 섹터 분석 `GET /api/v1/analysis/sector`(accountNo 또는 symbols+market), 리밸런싱 제안 `GET /api/v1/trading-portfolios/rebalance-suggestions`(accountNo, market=US) — 11-api-frontend-mapping 및 컨트롤러 스펙 참조.

---

## 10. Ops 데이터 파이프라인 API

Admin 전용 메뉴 `/ops/data`(데이터 파이프라인 상태)에서 원천별 수집 상태·최근 기준일·오류 요약을 조회합니다. **인가**: `hasRole('ADMIN')`.

### 10.1 데이터 파이프라인 상태

**엔드포인트**: `GET /api/v1/ops/data-pipeline/status`

**설명**: DART/SEC/KRX/US 원천별 마지막 배치 실행 시각·최근 기준일(뉴스는 최근 수집일, 시세는 최근 basDt)·상태(OK/WARNING/ERROR)·마지막 실패 시 오류 요약을 반환합니다.

**성공 응답 (200 OK)**:
```json
{
  "sources": [
    {
      "sourceId": "DART",
      "displayName": "DART 공시",
      "lastRunTime": "2026-02-09T10:00:00",
      "lastBaselineDate": "2026-02-09",
      "status": "OK",
      "errorSummary": null
    }
  ],
  "updatedAt": "2026-02-09T12:00:00"
}
```

**DTO**: `DataPipelineStatusDto` — `sources` (원천별 `DataPipelineSourceStatusDto`), `updatedAt`. 각 원천: `sourceId`, `displayName`, `lastRunTime`, `lastBaselineDate`, `status`, `errorSummary`.

---

## 11. Ops 알림센터 API

Admin 전용 메뉴 `/ops/alerts`에서 Discord 긴급 알림 등 알림 이력을 조회합니다. **인가**: `hasRole('ADMIN')`. Discord 발송 시 동일 내용이 TB_ALERT_LOG에 저장됩니다.

### 11.1 알림 목록

**엔드포인트**: `GET /api/v1/ops/alerts`

**설명**: 알림 이력을 페이징·레벨 필터로 조회합니다.

**쿼리 파라미터**:
- `page` (int, optional): 페이지 번호 (0부터). 기본값 0
- `size` (int, optional): 페이지 크기 (1~100). 기본값 20
- `level` (String, optional): 필터 — INFO, WARNING, ERROR

**성공 응답 (200 OK)**:
```json
{
  "items": [
    {
      "id": 1,
      "occurredAt": "2026-02-10T12:00:00+09:00",
      "level": "WARNING",
      "component": "UnfilledOrder",
      "message": "** [긴급] 미체결 주문 알림 **\n..."
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

**DTO**: `AlertListResponseDto` — `items` (AlertItemDto 배열), `page`, `size`, `totalElements`, `totalPages`. `AlertItemDto`: `id`, `occurredAt` (ISO-8601), `level`, `component`, `message`.

---

## 12. Ops 감사 로그 API

Admin 전용 메뉴 `/ops/audit`에서 설정 변경·수동 트리거·실계좌 가드 차단 이벤트 이력을 조회합니다. **인가**: `hasRole('ADMIN')`. 이벤트 기록 시 userId/accountNo는 마스킹 후 TB_AUDIT_LOG에 저장됩니다.

### 12.1 감사 로그 목록

**엔드포인트**: `GET /api/v1/ops/audit`

**설명**: 감사 이력을 페이징·이벤트유형·기간 필터로 조회합니다.

**쿼리 파라미터**:
- `page` (int, optional): 페이지 번호 (0부터). 기본값 0
- `size` (int, optional): 페이지 크기 (1~100). 기본값 20
- `eventType` (String, optional): 필터 — SETTING_CHANGE, MANUAL_TRIGGER, REAL_ACCOUNT_GUARD_BLOCKED
- `from` (LocalDate, optional): 기간 시작 (yyyy-MM-dd)
- `to` (LocalDate, optional): 기간 종료 (yyyy-MM-dd)

**성공 응답 (200 OK)**:
```json
{
  "items": [
    {
      "id": 1,
      "occurredAt": "2026-02-10T12:00:00+09:00",
      "eventType": "SETTING_CHANGE",
      "userIdMasked": "ab***",
      "accountNoMasked": "****-12",
      "summary": "거래 설정 저장",
      "result": "SUCCESS",
      "ipAddress": null
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

**DTO**: `AuditLogListResponseDto` — `items` (AuditLogItemDto 배열), `page`, `size`, `totalElements`, `totalPages`. `AuditLogItemDto`: `id`, `occurredAt` (ISO-8601), `eventType`, `userIdMasked`, `accountNoMasked`, `summary`, `result`, `ipAddress`.

### 12.2 Ops 모델/예측 상태

Admin 전용 메뉴 `/ops/model`에서 예측 서비스(AI) 상태를 조회합니다. **인가**: `hasRole('ADMIN')`.

**엔드포인트**: `GET /api/v1/ops/model/status`

**설명**: AiPredictionClient 기반 헬스 체크 결과·설정 URL 표시(마스킹)·마지막 체크 시각을 반환합니다.

**성공 응답 (200 OK)**:
```json
{
  "modelReady": true,
  "serviceUrl": "configured",
  "lastCheckAt": "2026-02-10T12:00:00Z",
  "version": null,
  "failureRateRecent": null
}
```

**DTO**: `OpsModelStatusDto` — `modelReady` (boolean), `serviceUrl` (configured/not configured), `lastCheckAt` (Instant), `version` (optional), `failureRateRecent` (optional, 0~1).

### 12.3 Ops 시스템 헬스

Admin 전용 메뉴 `/ops/health`에서 DB·Redis·예측 서비스 상태 요약을 조회합니다. **인가**: `hasRole('ADMIN')`.

**엔드포인트**: `GET /api/v1/ops/health`

**설명**: DB(DataSource), Redis(캐시), 예측 서비스(FastAPI) 상태를 통합한 요약. Redis 미설정(no-redis 프로파일) 시 `redis: "UNKNOWN"`.

**성공 응답 (200 OK)**:
```json
{
  "db": "UP",
  "redis": "UP",
  "predictionService": "UP",
  "lastCheckedAt": "2026-02-10T12:00:00Z"
}
```

**DTO**: `OpsHealthDto` — `db`, `redis`, `predictionService` (각 "UP"|"DOWN"|"UNKNOWN"), `lastCheckedAt` (Instant).

### 12.4 Ops 전략 거버넌스

Admin 전용: 전략 거버넌스 검사 결과 이력·(market, strategyType)별 자동 매매 중단(halt) 조회 및 halt 해제. **인가**: `hasRole('ADMIN')`. 알림 이력은 기존 `GET /api/v1/ops/alerts`에서 `component=StrategyGovernance` 필터로 조회.

**엔드포인트**:
- `GET /api/v1/ops/governance/results?limit=20` — 최근 검사 결과(RUN_AT 내림차순). `limit`(1~500, 기본 20). 응답: `GovernanceCheckResultDto[]` (id, runAt, market, strategyType, mddPct, sharpeRatio, degraded, startDate, endDate, createdAt).
- `GET /api/v1/ops/governance/halts` — 현재 활성 halt 목록(CLEARED_AT IS NULL). 응답: `GovernanceHaltDto[]` (market, strategyType, haltedAt, reason).
- `PUT /api/v1/ops/governance/halts/{market}/{strategyType}/clear` — 해당 (market, strategyType) halt 해제. Body(선택): `{ "clearedBy": "userId" }`. 204 No Content.

---

## 13. 연말 세금·리포트 API

기획요청 §9: 한국 개인투자자 연말 세금·리포팅. 연간 실현손익·국내/해외·배당·예상 세금. 실데이터는 사용자 계좌별 기간별손익조회(realizedProfitLoss) 연도 합산. **인증 필요**.

### 13.1 세금 요약

**엔드포인트**: `GET /api/v1/report/tax/summary`

**쿼리 파라미터**: `year` (int, optional): 기준 연도. 미입력 시 현재 연도.

**성공 응답 (200 OK)**: `TaxReportSummaryDto` — `year`, `domesticRealizedGainLoss`, `overseasRealizedGainLoss`, `dividendTotal`, `estimatedTax`, `disclaimer`. 집계 근거는 01-api-overview §3.12 참조.

### 13.2 세금 요약 내보내기 (CSV/PDF)

**엔드포인트**: `GET /api/v1/report/tax/summary/export`

**쿼리 파라미터**: `year` (int, optional): 기준 연도. `format` (string, 기본 "csv"): `csv` 또는 `pdf`.

**성공 응답 (200 OK)**: `Content-Disposition: attachment`, 본문은 CSV(UTF-8) 또는 PDF 바이너리.

---

## 트리거 API (수동 실행)

스케줄 작업을 수동으로 한 번 실행할 때 사용합니다. 스케줄 현황(`/batch`) 화면의 "지금 실행" 버튼 및 자동투자 현황의 "파이프라인 수동 실행 (dry-run)" 등에서 호출합니다. **인증 필요**. 구현은 Spring Batch Job을 `JobLauncher.run`으로 실행하며, 경로·요청 파라미터·응답 형식(`success`, `message` 등)은 기존과 동일합니다.

**공통 응답**: `200 OK` 시 JSON `{ "success": true|false, "message": "..." }` 및 작업별 추가 필드. 실패 시에도 200으로 반환하고 `success: false`, `message`에 사유.

| 엔드포인트 | 설명 | 쿼리/비고 |
|------------|------|-----------|
| `POST /api/v1/trigger/dart-collect` | DART 공시 수집 즉시 실행 | - |
| `POST /api/v1/trigger/sec-collect` | SEC EDGAR 공시 수집 즉시 실행 | - |
| `POST /api/v1/trigger/krx-daily` | KRX 일별 시세 수집 즉시 실행 | `basDt` (optional, yyyy-MM-dd). 미입력 시 오늘 |
| `POST /api/v1/trigger/us-daily` | US 시장 일별 시세 수집 즉시 실행 | `basDt` (optional). 미입력 시 오늘. 응답에 `saved` 포함 |
| `POST /api/v1/trigger/krx-daily-backfill` | KRX 일별 시세 기간 백필 (스트레스 구간 등) | `from`, `to` (required, yyyy-MM-dd) |
| `POST /api/v1/trigger/us-daily-backfill` | US 일별 시세 기간 백필 (스트레스 구간 등) | `from`, `to` (required, yyyy-MM-dd) |
| `POST /api/v1/trigger/factor-calculation` | 유니버스 필터 및 팩터(시그널) 계산 즉시 실행 | - |
| `POST /api/v1/trigger/auto-buy` | 자동매수(통합): 공통 전처리 → 로보(ETF) → 파이프라인(개별종목) 순 실행 | `dryRun` (optional, boolean). true면 실제 주문 없이 실행. 응답에 `dryRun` 포함 |
| `POST /api/v1/trigger/pipeline-execution` | 4단계 파이프라인만 수동 실행 (스케줄은 자동매수 통합 사용) | `dryRun` (optional, boolean). true면 실제 주문 없이 실행. 응답에 `dryRun` 포함 |
| `POST /api/v1/trigger/pipeline-exit` | 보유 포지션 청산 규칙 평가 및 매도 시그널 시 주문 실행 | - |
| `POST /api/v1/trigger/fill-confirmation` | 체결된 주문에 대해 포지션 등록 | - |
| `POST /api/v1/trigger/unfilled-check` | PENDING N분 경과 주문에 대해 Discord 긴급 알림 | - |
| `POST /api/v1/trigger/risk-event-alert` | 일일 손실 한도 임박·VaR 95% 초과 검사 후 Discord 리스크 이벤트 알림 발송 | - |
| `POST /api/v1/trigger/robo-rebalance` | 로보 리밸런싱만 수동 실행 (스케줄은 자동매수 통합 사용) | `dryRun` (optional, boolean). true면 백테스트만 실행·저장, ETF 주문 없음. 응답에 `dryRun` 포함 |
| `POST /api/v1/trigger/daily-pnl` | 장 마감 후 계좌별 당일 수익률 기록 | - |
| `POST /api/v1/trigger/intraday-breakout` | 장중 변동성 돌파(09:00~10:00 구간) 실행 | 설정 시에만 유효 |
| `POST /api/v1/trigger/medium-term-rebalance` | 중기(MEDIUM_TERM) 월 1회 리밸런싱 훅 (스텁) | - |
| `POST /api/v1/trigger/strategy-governance-check` | 전략 거버넌스 검사: 최근 N개월 백테스트 실행 후 MDD/Sharpe 열화 시 Discord 알림 | - |

**요청 예시**:
```bash
curl -X POST "http://localhost:8080/api/v1/trigger/dart-collect" -H "Content-Type: application/json" --cookie "token=..."
curl -X POST "http://localhost:8080/api/v1/trigger/pipeline-execution?dryRun=true" -H "Content-Type: application/json" --cookie "token=..."
```

---

## 백테스트 API

### POST /api/v1/backtest

**엔드포인트**: `POST /api/v1/backtest`

**설명**: 기간·시장·전략타입·초기자본으로 4단계 파이프라인을 과거 일봉·시그널로 재생하여 메트릭·수익 곡선·거래 목록을 반환합니다. 마찰 비용(수수료·세금·슬리피지)은 `application.yml`의 `investment.fees` 설정을 적용합니다. 인증 필요.

**요청 본문 (BacktestRunRequest)**: startDate, endDate, market (KR/US), strategyType (SHORT_TERM/MEDIUM_TERM/LONG_TERM), initialCapital

**성공 응답 (200 OK, BacktestRunResult)**: startDate, endDate, market, strategyType, initialCapital, finalEquity, totalReturnPct, cagr, mddPct, sharpeRatio, sortinoRatio, calmarRatio, winRate, avgWin, avgLoss, profitFactor, tradeCount, winningTrades, losingTrades, equityCurve, trades (각 거래에 totalFrictionCost 포함)

---

### POST /api/v1/backtest/walk-forward

**엔드포인트**: `POST /api/v1/backtest/walk-forward`

**설명**: Walk-Forward(롤링 Out-of-Sample) 백테스트. 구간을 train/test 윈도우로 나누어 각 test 구간만 백테스트 실행 후 fold별 메트릭을 집계합니다. 전략 파라미터는 재추정하지 않고 기존 설정을 사용합니다. 오버피팅 완화·일반화 성능 추정용. 인증 필요.

**요청 본문 (WalkForwardBacktestRequest)**: startDate, endDate, market, strategyType, initialCapital (필수). trainDays (기본 252), testDays (기본 63), stepDays (기본 63, testDays와 같으면 비중첩)

**성공 응답 (200 OK, WalkForwardBacktestResult)**: startDate, endDate, market, strategyType, trainDays, testDays, stepDays, foldCount, folds (List&lt;BacktestRunResult&gt;), avgCagr, avgMddPct, minSharpeRatio, avgSharpeRatio, avgWinRate, avgProfitFactor

---

### POST /api/v1/backtest/robo

**엔드포인트**: `POST /api/v1/backtest/robo`

**설명**: 로보 어드바이저 동적 자산배분(모멘텀·변동성 역가중) 백테스트. 기간·초기자본·선택 파라미터로 실행 후 메트릭·수익 곡선·벤치마크 곡선·리밸런싱 이력을 반환합니다. 요청에 commPct/slipPct가 없으면 `investment.fees`(미국 ETF round-trip·TAF)를 적용하고, 있으면 해당 값으로 오버라이드(하위 호환)합니다. 인증 필요.

**요청 본문 (RoboBacktestRequest)**: startDate, endDate, initialCapital (필수). optional: assetSymbols, momentumMonths, maWindowDays, topN, rebalanceFrequency (MONTHLY/QUARTERLY), driftThresholdPct, commPct, slipPct, riskFreeRatePct, benchmarkWeights, volatilityLookbackDays

**성공 응답 (200 OK, RoboBacktestResult)**: startDate, endDate, initialCapital, finalEquity, totalReturnPct, cagr, mddPct, sharpeRatio, calmarRatio, turnover, benchmarkCagr, benchmarkMddPct, equityCurve, benchmarkCurve, rebalanceHistory, warningMessage (선택, 데이터 부재·평평한 곡선 안내)

---

### GET /api/v1/backtest/robo/last-pre-execution

**엔드포인트**: `GET /api/v1/backtest/robo/last-pre-execution?accountNo=xxx`

**설명**: 로보 리밸런싱 스케줄러가 마지막으로 실행한 실행 전 백테스트 결과(통과/미통과·MDD·Sharpe·실행 시각)를 조회합니다. 인증 필요.

**쿼리**: accountNo (필수)

**성공 응답 (200 OK, LastPreExecutionResultDto)**: accountNo, passed, mddPct, sharpeRatio, runAt. 결과 없으면 204 No Content.

---

### POST /api/v1/backtest/robo/collect-us-daily

**엔드포인트**: `POST /api/v1/backtest/robo/collect-us-daily`

**설명**: 로보 백테스트에 필요한 US 일봉(SPY, TLT, 섹터 ETF 등)을 yfinance 스크립트로 수집합니다. `investment.data.us.yfinance-script-path`가 설정되어 있어야 합니다. startDate/endDate 미입력 시 최근 30일 수집. 인증 필요.

**요청 본문 (CollectUsDailyRequest, 선택)**: startDate, endDate (둘 다 optional. null이면 endDate=오늘, startDate=30일 전)

**성공 응답 (200 OK, CollectUsDailyResponse)**: collectedDays (수집한 일수), savedTotal (총 저장 건수), message (안내, 스크립트 미설정 등)

---

## 문서 변경 이력

| 버전 | 일자 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-28 | System | 문서 정리 및 구조화 - 계좌 API 상세 내용 통합 |
| 1.1 | 2026-01-28 | System | 시장 데이터 API 섹션 추가 (현재가 조회) |
| 1.2 | 2026-02-03 | System | 트리거 API(수동 실행) 섹션 추가 - DART/SEC/KRX/US 수집·팩터 계산·파이프라인 실행/청산·체결 확인·미체결 확인·로보 리밸런싱·일일 PnL·장중 변동성 돌파·중기 리밸런스 |
| 1.3 | 2026-02-06 | System | §9 리스크 리포트 API 추가 - GET /api/v1/risk/summary, /limits, /history (인증 필요, DTO·에러 처리) |
| 1.4 | 2026-02-09 | System | §10 Ops 데이터 파이프라인 API 추가 - GET /api/v1/ops/data-pipeline/status (ADMIN 전용, 원천별 수집 상태·최근 기준일·오류 요약) |
| 1.5 | 2026-02-10 | System | §11 Ops 알림센터 API 추가 - GET /api/v1/ops/alerts (ADMIN 전용, 페이징·레벨 필터, Discord 발송 이력 저장·조회) |
| 1.6 | 2026-02-10 | System | §12 연말 세금·리포트 API (스텁) - GET /api/v1/report/tax/summary (인증 필요, year 선택, 실데이터·PDF/CSV는 후속) |
| 1.7 | 2026-02-10 | System | §12 Ops 감사 로그 API 추가 - GET /api/v1/ops/audit (ADMIN 전용, 페이징·eventType·기간 필터, 설정 변경·수동 트리거·실계좌 가드 차단 이벤트). §13 연말 세금 리넘버링 |
| 1.8 | 2026-02-10 | System | §12.2 Ops 모델/예측 상태 API - GET /api/v1/ops/model/status (ADMIN 전용). §12.3 Ops 시스템 헬스 API - GET /api/v1/ops/health (ADMIN 전용, DB·Redis·예측 서비스) |
| 1.9 | 2026-02-11 | System | §9 risk/summary VaR 방법론 명시 — 현재 파라메트릭(1.65σ·2.06σ), 역사적/Monte Carlo VaR은 선택 사항 (기획 고도화) |
| 1.10 | 2026-02-11 | System | §12.4 Ops 전략 거버넌스 API — GET /api/v1/ops/governance/results, GET /api/v1/ops/governance/halts, PUT …/halts/{market}/{strategyType}/clear (ADMIN 전용, 검사 결과·halt 조회/해제) |