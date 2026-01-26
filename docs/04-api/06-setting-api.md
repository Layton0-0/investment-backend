# 설정 API

## 1. 거래 설정 조회

### 1.1 요청
```
GET /api/v1/settings/{accountNo}
```

**Path Parameters**:
- `accountNo` (String, required): 계좌번호

**예시**:
```bash
curl -X GET "http://localhost:8080/api/v1/settings/12345678"
```

### 1.2 응답

**성공 (200 OK)**:
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

**에러 (404 Not Found)**:
```json
{
  "code": "SETTING_NOT_FOUND",
  "message": "거래 설정을 찾을 수 없습니다: 12345678",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-01-26T10:00:00"
}
```

## 2. 거래 설정 저장/업데이트

### 2.1 요청
```
PUT /api/v1/settings/{accountNo}
```

**Path Parameters**:
- `accountNo` (String, required): 계좌번호

**Request Body**:
```json
{
  "maxInvestmentAmount": 1000000.00,
  "minInvestmentAmount": 10000.00,
  "defaultCurrency": "KRW",
  "autoTradingEnabled": true,
  "riskLevel": 0.7
}
```

**필드 설명**:
- `maxInvestmentAmount` (BigDecimal, required): 최대 투자금액
- `minInvestmentAmount` (BigDecimal, required): 최소 투자금액
- `defaultCurrency` (String, required): 기본 통화
- `autoTradingEnabled` (Boolean, required): 자동 매매 활성화 여부
- `riskLevel` (BigDecimal, optional): 리스크 레벨 (0.0 ~ 1.0)

**예시**:
```bash
curl -X PUT "http://localhost:8080/api/v1/settings/12345678" \
  -H "Content-Type: application/json" \
  -d '{
    "maxInvestmentAmount": 1000000.00,
    "minInvestmentAmount": 10000.00,
    "defaultCurrency": "KRW",
    "autoTradingEnabled": true,
    "riskLevel": 0.7
  }'
```

### 2.2 응답

**성공 (200 OK)**: 업데이트된 설정 정보

**에러 (400 Bad Request)**:
```json
{
  "code": "INVALID_SETTING_VALUE",
  "message": "최대 투자금액은 최소 투자금액보다 커야 합니다.",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-01-26T10:00:00"
}
```

## 3. DTO 정의

### 3.1 TradingSettingDto
```java
{
  "accountNo": String,              // 계좌번호
  "maxInvestmentAmount": BigDecimal, // 최대 투자금액 (required)
  "minInvestmentAmount": BigDecimal, // 최소 투자금액 (required)
  "defaultCurrency": String,         // 기본 통화 (required)
  "autoTradingEnabled": Boolean,     // 자동 매매 활성화 여부 (required)
  "riskLevel": BigDecimal            // 리스크 레벨 (0.0 ~ 1.0, optional)
}
```

## 4. 비즈니스 규칙

1. 최대 투자금액 > 최소 투자금액
2. 리스크 레벨 범위: 0.0 ~ 1.0
3. 계좌당 설정은 1개만 존재

## 5. 에러 코드

- `SETTING_NOT_FOUND`: 거래 설정을 찾을 수 없음
- `INVALID_SETTING_VALUE`: 잘못된 설정 값
