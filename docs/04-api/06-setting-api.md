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
  "riskLevel": 0.7,
  "pipelineAutoExecute": true,
  "pipelineAllowRealExecution": false
}
```

- `pipelineAutoExecute` (Boolean, optional): 파이프라인 자동 실행 허용. null이면 서버 기본값(`investment.pipeline.auto-execute`) 사용.
- `pipelineAllowRealExecution` (Boolean, optional): 실계좌 자동 실행 허용. null이면 서버 기본값(`investment.pipeline.allow-real-execution`) 사용. 실계좌(serverType=0)에서만 의미 있음.

**설정 없음 (404 Not Found)**: 해당 계좌에 거래 설정이 없으면 404. 클라이언트는 기본값 폼 표시 후 PUT으로 저장 가능.

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
  "riskLevel": 0.7,
  "pipelineAutoExecute": true,
  "pipelineAllowRealExecution": false
}
```

**필드 설명**:
- `maxInvestmentAmount` (BigDecimal, required): 최대 투자금액
- `minInvestmentAmount` (BigDecimal, required): 최소 투자금액
- `defaultCurrency` (String, required): 기본 통화
- `autoTradingEnabled` (Boolean, required): 자동 매매 활성화 여부
- `riskLevel` (BigDecimal, optional): 리스크 레벨 (0.0 ~ 1.0)
- `pipelineAutoExecute` (Boolean, optional): 파이프라인 자동 실행 허용. null이면 서버 기본값 사용.
- `pipelineAllowRealExecution` (Boolean, optional): 실계좌 자동 실행 허용. null이면 서버 기본값 사용.

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
  "accountNo": String,                // 계좌번호
  "maxInvestmentAmount": BigDecimal, // 최대 투자금액 (required)
  "minInvestmentAmount": BigDecimal, // 최소 투자금액 (required)
  "defaultCurrency": String,          // 기본 통화 (required)
  "autoTradingEnabled": Boolean,      // 자동 매매 활성화 여부 (required)
  "riskLevel": BigDecimal,           // 리스크 레벨 (0.0 ~ 1.0, optional)
  "pipelineAutoExecute": Boolean,     // 파이프라인 자동 실행 허용 (optional, null=서버 기본값)
  "pipelineAllowRealExecution": Boolean // 실계좌 자동 실행 허용 (optional, null=서버 기본값)
}
```

## 4. 비즈니스 규칙

1. 최대 투자금액 > 최소 투자금액
2. 리스크 레벨 범위: 0.0 ~ 1.0
3. 계좌당 설정은 1개만 존재

## 5. 에러 코드

- `SETTING_NOT_FOUND`: 거래 설정을 찾을 수 없음
- `INVALID_SETTING_VALUE`: 잘못된 설정 값
