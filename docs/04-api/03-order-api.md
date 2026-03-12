# 주문 API

## 1. 주문 실행

### 1.1 요청
```
POST /api/v1/orders
```

**Request Body**:
```json
{
  "accountNo": "12345678",
  "symbol": "005930",
  "orderType": "BUY",
  "quantity": 10,
  "price": 75000.00
}
```

**필드 설명**:
- `accountNo` (String, required): 계좌번호
- `symbol` (String, required): 종목 코드
- `orderType` (Enum, required): 주문 유형 (BUY, SELL)
- `quantity` (Integer, required): 주문 수량 (1 이상)
- `price` (BigDecimal, required): 주문 가격 (0 초과)

**예시**:
```bash
curl -X POST "http://localhost:8080/api/v1/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "accountNo": "12345678",
    "symbol": "005930",
    "orderType": "BUY",
    "quantity": 10,
    "price": 75000.00
  }'
```

### 1.2 응답

**성공 (200 OK)**:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "accountNo": "12345678",
  "symbol": "005930",
  "orderType": "BUY",
  "quantity": 10,
  "price": 75000.00,
  "status": "PENDING",
  "executedQuantity": 0,
  "executedPrice": null,
  "orderTime": "2026-01-26T10:00:00",
  "executedTime": null,
  "message": null
}
```

**에러 (400 Bad Request)**:
```json
{
  "code": "EXCEEDS_MAX_INVESTMENT",
  "message": "최대 투자금액(1000000)을 초과합니다: 750000",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-01-26T10:00:00"
}
```

## 2. 주문 조회

### 2.1 요청
```
GET /api/v1/orders/{orderId}?accountNo={accountNo}
```

**Path Parameters**:
- `orderId` (String, required): 주문 ID

**Query Parameters**:
- `accountNo` (String, required): 계좌번호

**예시**:
```bash
curl -X GET "http://localhost:8080/api/v1/orders/550e8400-e29b-41d4-a716-446655440000?accountNo=12345678"
```

### 2.2 응답

**성공 (200 OK)**:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "accountNo": "12345678",
  "symbol": "005930",
  "orderType": "BUY",
  "quantity": 10,
  "price": 75000.00,
  "status": "EXECUTED",
  "executedQuantity": 10,
  "executedPrice": 75000.00,
  "orderTime": "2026-01-26T10:00:00",
  "executedTime": "2026-01-26T10:00:05",
  "message": "체결 완료"
}
```

**에러 (404 Not Found)**:
```json
{
  "code": "ORDER_NOT_FOUND",
  "message": "주문을 찾을 수 없습니다: 550e8400-e29b-41d4-a716-446655440000",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-01-26T10:00:00"
}
```

## 3. 주문 목록 조회

### 3.1 요청
```
GET /api/v1/orders?accountNo={accountNo}
```

**Query Parameters**:
- `accountNo` (String, required): 계좌번호
- `status` (String, optional): 주문 상태 (PENDING, EXECUTED, CANCELLED, FAILED)
- `page` (Integer, optional): 페이지 번호 (기본값: 0)
- `size` (Integer, optional): 페이지 크기 (기본값: 20)

**예시**:
```bash
curl -X GET "http://localhost:8080/api/v1/orders?accountNo=12345678&status=EXECUTED&page=0&size=20"
```

### 3.2 응답

**성공 (200 OK)**:
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "accountNo": "12345678",
    "symbol": "005930",
    "orderType": "BUY",
    "quantity": 10,
    "price": 75000.00,
    "status": "EXECUTED",
    "executedQuantity": 10,
    "executedPrice": 75000.00,
    "orderTime": "2026-01-26T10:00:00",
    "executedTime": "2026-01-26T10:00:05",
    "message": "체결 완료"
  }
]
```

## 4. 주문 취소

### 4.1 요청
```
DELETE /api/v1/orders/{orderId}?accountNo={accountNo}
```

**Path Parameters**:
- `orderId` (String, required): 주문 ID

**Query Parameters**:
- `accountNo` (String, required): 계좌번호

**예시**:
```bash
curl -X DELETE "http://localhost:8080/api/v1/orders/550e8400-e29b-41d4-a716-446655440000?accountNo=12345678"
```

### 4.2 응답

**성공 (204 No Content)**: 응답 본문 없음

**에러 (400 Bad Request)**:
```json
{
  "code": "ORDER_FAILED",
  "message": "이미 체결된 주문은 취소할 수 없습니다.",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-01-26T10:00:00"
}
```

## 5. DTO 정의

### 5.1 OrderRequestDto
```java
{
  "accountNo": String,      // 계좌번호 (required)
  "symbol": String,          // 종목 코드 (required)
  "orderType": Enum,         // 주문 유형: BUY, SELL (required)
  "quantity": Integer,        // 주문 수량 (required, min: 1)
  "price": BigDecimal         // 주문 가격 (required, min: 0.01)
}
```

### 5.2 OrderResponseDto
```java
{
  "id": String,              // 주문 ID
  "accountNo": String,        // 계좌번호
  "symbol": String,           // 종목 코드
  "orderType": Enum,          // 주문 유형: BUY, SELL
  "quantity": Integer,        // 주문 수량
  "price": BigDecimal,        // 주문 가격
  "status": String,           // 주문 상태: PENDING, EXECUTED, CANCELLED, FAILED
  "executedQuantity": Integer, // 체결 수량
  "executedPrice": BigDecimal, // 체결 가격
  "orderTime": LocalDateTime,  // 주문 시간
  "executedTime": LocalDateTime, // 체결 시간
  "message": String            // 메시지
}
```

## 6. 주문 상태

- `PENDING`: 대기 중 (미체결)
- `EXECUTED`: 체결 완료
- `CANCELLED`: 취소됨
- `FAILED`: 실패

## 7. 에러 코드

- `ORDER_FAILED`: 주문 실행 실패
- `ORDER_NOT_FOUND`: 주문을 찾을 수 없음
- `INVALID_ORDER_TYPE`: 잘못된 주문 유형
- `INVALID_ORDER_AMOUNT`: 잘못된 주문 금액
- `EXCEEDS_MAX_INVESTMENT`: 최대 투자금액 초과
- `INSUFFICIENT_BALANCE`: 잔고 부족

## 8. 비즈니스 규칙

1. 주문 금액은 최대 투자금액을 초과할 수 없음
2. 주문 실행 단계에서는 최소 투자금액을 검증하지 않음(소액·만원 단위 종목 등 허용)
3. 체결된 주문은 취소할 수 없음
4. 계좌 잔고가 부족하면 주문 실패
