# 전략 API

## 1. 전략 목록 조회

### 1.1 요청
```
GET /api/v1/strategies/{accountNo}
```

**Path Parameters**:
- `accountNo` (String, required): 계좌번호

**예시**:
```bash
curl -X GET "http://localhost:8080/api/v1/strategies/12345678"
```

### 1.2 응답

**성공 (200 OK)**:
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
    "lastExecutedAt": "2026-01-26T09:00:00",
    "totalExecutions": 100,
    "successCount": 65,
    "failureCount": 35,
    "totalProfitLoss": 500000.00
  }
]
```

## 2. 전략 상세 조회

### 2.1 요청
```
GET /api/v1/strategies/{accountNo}/{strategyType}
```

**Path Parameters**:
- `accountNo` (String, required): 계좌번호
- `strategyType` (Enum, required): 전략 타입 (SHORT_TERM, MEDIUM_TERM, LONG_TERM)

**예시**:
```bash
curl -X GET "http://localhost:8080/api/v1/strategies/12345678/SHORT_TERM"
```

### 2.2 응답

**성공 (200 OK)**: 전략 목록 조회와 동일한 형식

## 3. 전략 생성/업데이트

### 3.1 요청
```
POST /api/v1/strategies
```

**Request Body**:
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

**필드 설명**:
- `accountNo` (String, required): 계좌번호
- `strategyType` (Enum, required): 전략 타입
- `maxInvestmentAmount` (BigDecimal, optional): 최대 투자금액
- `minInvestmentAmount` (BigDecimal, optional): 최소 투자금액
- `riskLevel` (BigDecimal, optional): 리스크 레벨 (0.0 ~ 1.0)
- `confidenceThreshold` (BigDecimal, optional): 신뢰도 임계값 (0.0 ~ 1.0)

**예시**:
```bash
curl -X POST "http://localhost:8080/api/v1/strategies" \
  -H "Content-Type: application/json" \
  -d '{
    "accountNo": "12345678",
    "strategyType": "SHORT_TERM",
    "maxInvestmentAmount": 1000000.00,
    "minInvestmentAmount": 10000.00,
    "riskLevel": 0.7,
    "confidenceThreshold": 0.75
  }'
```

### 3.2 응답

**성공 (200 OK)**: 생성/업데이트된 전략 정보

## 4. 전략 상태 변경

### 4.1 요청
```
PUT /api/v1/strategies/{accountNo}/{strategyType}/status
```

**Path Parameters**:
- `accountNo` (String, required): 계좌번호
- `strategyType` (Enum, required): 전략 타입

**Request Body**:
```json
{
  "status": "PAUSED"
}
```

**예시**:
```bash
curl -X PUT "http://localhost:8080/api/v1/strategies/12345678/SHORT_TERM/status" \
  -H "Content-Type: application/json" \
  -d '{"status": "PAUSED"}'
```

### 4.2 응답

**성공 (200 OK)**: 업데이트된 전략 정보

## 5. 전략 활성화

### 5.1 요청
```
POST /api/v1/strategies/{accountNo}/{strategyType}/activate
```

**예시**:
```bash
curl -X POST "http://localhost:8080/api/v1/strategies/12345678/SHORT_TERM/activate"
```

### 5.2 응답

**성공 (200 OK)**: 활성화된 전략 정보

## 6. 전략 중지

### 6.1 요청
```
POST /api/v1/strategies/{accountNo}/{strategyType}/stop
```

**예시**:
```bash
curl -X POST "http://localhost:8080/api/v1/strategies/12345678/SHORT_TERM/stop"
```

### 6.2 응답

**성공 (200 OK)**: 중지된 전략 정보

## 7. 전략 타입

- `SHORT_TERM`: 단기 전략
- `MEDIUM_TERM`: 중기 전략
- `LONG_TERM`: 장기 전략

## 8. 전략 상태

- `ACTIVE`: 활성 (실행 중)
- `STOPPED`: 중지됨
- `PAUSED`: 일시 정지

## 9. DTO 정의

### 9.1 StrategyDto
```java
{
  "id": String,                    // 전략 ID
  "accountNo": String,             // 계좌번호
  "strategyType": Enum,            // 전략 타입
  "status": Enum,                  // 전략 상태
  "maxInvestmentAmount": BigDecimal, // 최대 투자금액
  "minInvestmentAmount": BigDecimal, // 최소 투자금액
  "riskLevel": BigDecimal,         // 리스크 레벨 (0.0 ~ 1.0)
  "confidenceThreshold": BigDecimal, // 신뢰도 임계값 (0.0 ~ 1.0)
  "lastExecutedAt": LocalDateTime,  // 마지막 실행 시간
  "totalExecutions": Long,          // 총 실행 횟수
  "successCount": Long,             // 성공 횟수
  "failureCount": Long,             // 실패 횟수
  "totalProfitLoss": BigDecimal     // 총 손익
}
```

### 9.2 StrategyStatusUpdateDto
```java
{
  "status": Enum  // 전략 상태: ACTIVE, STOPPED, PAUSED
}
```

## 10. 비즈니스 규칙

1. 계좌별 전략 타입은 유일해야 함
2. ACTIVE 상태인 전략만 실행됨
3. 신뢰도가 임계값 이상일 때만 주문 실행
4. 최대 투자금액 > 최소 투자금액
