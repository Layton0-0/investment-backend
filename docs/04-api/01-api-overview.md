# API 개요

## 1. API 기본 정보

### 1.1 Base URL
- **개발 환경**: `http://localhost:8080`
- **프로덕션 환경**: `https://api.investment-choi.com`

### 1.2 API 버전
- **현재 버전**: `v1`
- **경로**: `/api/v1/`

### 1.3 인증
- 현재는 계좌번호 기반 접근 제어
- 향후 JWT 토큰 기반 인증 추가 예정

### 1.4 응답 형식
- **Content-Type**: `application/json`
- **인코딩**: UTF-8

## 2. 공통 응답 형식

### 2.1 성공 응답
```json
{
  "data": { ... }
}
```

### 2.2 에러 응답
```json
{
  "code": "ERROR_CODE",
  "message": "오류 메시지",
  "details": ["상세 오류 목록"],
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-01-26T10:00:00"
}
```

### 2.3 HTTP 상태 코드
- `200 OK`: 성공
- `201 Created`: 생성 성공
- `204 No Content`: 성공 (응답 본문 없음)
- `400 Bad Request`: 잘못된 요청
- `404 Not Found`: 리소스를 찾을 수 없음
- `500 Internal Server Error`: 서버 오류

## 3. API 엔드포인트 목록

### 3.0 인증 API
- `POST /api/v1/auth/signup` - 회원가입
- `POST /api/v1/auth/login` - 로그인
- `POST /api/v1/auth/verify-account` - 계좌인증 (회원가입 전: API Key·서버타입·계좌번호가 모의/실전 도메인에서 유효한지 확인)
- `GET /api/v1/auth/mypage` - 마이페이지 조회
- `PUT /api/v1/auth/mypage` - 마이페이지 수정
- `POST /api/v1/auth/logout` - 로그아웃

### 3.1 계좌 API
- `GET /api/v1/accounts/{accountNo}/balance` - 계좌 잔고 조회
- `GET /api/v1/accounts/{accountNo}/positions` - 보유 종목 조회

### 3.2 주문 API
- `POST /api/v1/orders` - 주문 실행
- `GET /api/v1/orders/{orderId}` - 주문 조회
- `GET /api/v1/orders` - 주문 목록 조회
- `DELETE /api/v1/orders/{orderId}` - 주문 취소

### 3.3 전략 API
- `GET /api/v1/strategies/{accountNo}` - 전략 목록 조회 (쿼리: `market` 선택, KR/US)
- `GET /api/v1/strategies/{accountNo}/{strategyType}` - 전략 상세 조회 (쿼리: `market` 선택)
- `POST /api/v1/strategies` - 전략 생성/업데이트 (body에 `market` 포함 가능, 기본 KR)
- `PUT /api/v1/strategies/{accountNo}/{strategyType}/status` - 전략 상태 변경 (쿼리: `market` 선택)
- `POST /api/v1/strategies/{accountNo}/{strategyType}/activate` - 전략 활성화 (쿼리: `market` 선택)
- `POST /api/v1/strategies/{accountNo}/{strategyType}/stop` - 전략 중지 (쿼리: `market` 선택)

### 3.4 분석 API
- `POST /api/v1/analysis` - 종목 분석

### 3.5 설정 API
- `GET /api/v1/settings/{accountNo}` - 거래 설정 조회
- `PUT /api/v1/settings/{accountNo}` - 거래 설정 저장/업데이트

### 3.6 트레이딩 포트폴리오 API
- `GET /api/v1/trading-portfolios/{date}` - 트레이딩 포트폴리오 조회

### 3.7 배치 관리 API
- `GET /api/v1/batch/jobs` - 배치 작업 목록 조회

### 3.8 뉴스·공시 API
- `GET /api/v1/news` - 뉴스·공시 목록 조회 (쿼리: `market`, `source`, `symbol`, `from`, `to`, `page`, `size`)
- `POST /api/v1/news/collect` - 뉴스·공시 수집 실행 (DART·SEC EDGAR 즉시 수집, 인증 필요)

### 3.9 시그널/팩터 API
- `GET /api/v1/signals` - 시그널/팩터 점수 목록 조회 (쿼리: `basDt`, `market`, `symbol`, `factorType`, `page`, `size`)

## 4. API 버전 관리

### 4.1 버전 전략
- URL 경로에 버전 포함 (`/api/v1/`)
- Breaking Change 발생 시 새 버전 생성 (`/api/v2/`)

### 4.2 하위 호환성
- 기존 API는 최소 1년간 유지
- Deprecated API는 6개월 전 공지 후 제거

## 5. Rate Limiting

### 5.1 제한 정책
- **기본**: 초당 50 요청
- **주문 API**: 초당 10 요청
- **분석 API**: 초당 5 요청

### 5.2 Rate Limit 응답
```json
{
  "code": "RATE_LIMIT_EXCEEDED",
  "message": "요청 한도를 초과했습니다.",
  "retryAfter": 60
}
```

## 6. 페이징

### 6.1 페이징 파라미터
- `page`: 페이지 번호 (0부터 시작, 기본값: 0)
- `size`: 페이지 크기 (기본값: 20, 최대: 100)

### 6.2 페이징 응답
```json
{
  "content": [ ... ],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

## 7. 필터링 및 정렬

### 7.1 필터링
- 쿼리 파라미터로 필터링 지원
- 예: `GET /api/v1/orders?accountNo=123&status=EXECUTED`

### 7.2 정렬
- `sort`: 정렬 필드 및 방향
- 예: `GET /api/v1/orders?sort=orderTime,desc`

## 8. 검증

### 8.1 입력 검증
- Bean Validation (`@Valid`) 사용
- 필수 필드 누락 시 `400 Bad Request` 반환

### 8.2 비즈니스 규칙 검증
- 도메인 규칙 위반 시 `DomainException` 발생
- `400 Bad Request` 또는 `422 Unprocessable Entity` 반환

## 9. 문서화

### 9.1 OpenAPI/Swagger
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### 9.2 API 문서 구조
- 각 API별 상세 문서는 별도 파일로 관리
- [API 엔드포인트 상세](./02-api-endpoints.md) (계좌 API 포함)
- [주문 API](./03-order-api.md)
- [전략 API](./04-strategy-api.md)
- [분석 API](./05-analysis-api.md)
- [설정 API](./06-setting-api.md)
- [트레이딩 포트폴리오 API](./07-trading-portfolio-api.md)
- [데이터 수집 API 및 설정](./10-data-collection-api.md) (DART/KRX/Yahoo, 내부 수집 API)

## 문서 변경 이력

| 버전 | 일자 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-28 | System | 문서 정리 및 구조화 - 계좌 API 문서 통합 반영 |
| 1.1 | 2026-01-30 | System | 데이터 수집 API 문서 링크 추가 (10-data-collection-api.md) |
