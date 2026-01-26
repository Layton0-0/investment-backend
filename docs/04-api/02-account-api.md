# 계좌 API

## 1. 계좌 잔고 조회

### 1.1 요청
```
GET /api/v1/accounts/{accountNo}/balance
```

**Path Parameters**:
- `accountNo` (String, required): 계좌번호

**예시**:
```bash
curl -X GET "http://localhost:8080/api/v1/accounts/12345678/balance"
```

### 1.2 응답

**성공 (200 OK)**:
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

**에러 (404 Not Found)**:
```json
{
  "code": "ACCOUNT_NOT_FOUND",
  "message": "계좌를 찾을 수 없습니다: 12345678",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-01-26T10:00:00"
}
```

## 2. 보유 종목 조회

### 2.1 요청
```
GET /api/v1/accounts/{accountNo}/positions
```

**Path Parameters**:
- `accountNo` (String, required): 계좌번호

**예시**:
```bash
curl -X GET "http://localhost:8080/api/v1/accounts/12345678/positions"
```

### 2.2 응답

**성공 (200 OK)**:
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

**에러 (404 Not Found)**:
```json
{
  "code": "ACCOUNT_NOT_FOUND",
  "message": "계좌를 찾을 수 없습니다: 12345678",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-01-26T10:00:00"
}
```

## 3. DTO 정의

### 3.1 AccountBalanceDto
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

### 3.2 AccountPositionDto
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

## 4. 에러 코드

- `ACCOUNT_NOT_FOUND`: 계좌를 찾을 수 없음
- `ACCOUNT_ACCESS_DENIED`: 계좌 접근 권한 없음
- `INSUFFICIENT_BALANCE`: 잔고 부족
