# Investment Choi - 주식 투자 수익 분석 및 자동 매매 시스템

키움증권 Open API를 활용한 주식 투자 수익 분석 및 자동 매매 시스템입니다.

## 기술 스택

- **Backend**: Spring Boot 2.7.18 (Java 11)
- **Frontend**: Thymeleaf
- **Database**: MariaDB
- **Build Tool**: Gradle
- **API**: 키움증권 Open API

## 주요 기능

1. **계좌 조회**: 키움증권 계좌 잔고 및 보유 종목 조회
2. **주문 관리**: 주식 매수/매도 주문 실행 및 조회
3. **AI 분석**: 종목 분석 및 투자 추천
4. **자동 매매**: AI 분석 결과를 바탕으로 한 자동 매매 결정
5. **설정 관리**: 최대 투자금액, 최소 투자금액 등 거래 설정 관리

## 프로젝트 구조

```
src/main/java/com/investment/
├── api/controller/          # REST API 컨트롤러
├── web/controller/          # Thymeleaf 웹 컨트롤러
├── account/                 # 계좌 관련
├── order/                   # 주문 관련
├── analysis/                # AI 분석 관련
├── strategy/                # 거래 전략 관련
├── setting/                 # 설정 관련
├── kiwoom/                  # 키움증권 API 연동
├── domain/                  # 도메인 엔티티 및 리포지토리
└── common/                  # 공통 (예외 처리 등)
```

## 설정

### application.yml

주요 설정 항목:

```yaml
investment:
  kiwoom:
    api:
      enabled: false  # 키움증권 API 활성화 여부
      host: localhost
      port: 5000
    account:
      account-no: ${KIWOOM_ACCOUNT_NO:}
      password: ${KIWOOM_PASSWORD:}
  trading:
    max-investment-amount: ${MAX_INVESTMENT_AMOUNT:1000000}
    min-investment-amount: ${MIN_INVESTMENT_AMOUNT:10000}
    default-currency: USD
```

### 환경 변수

- `DB_USERNAME`: 데이터베이스 사용자명
- `DB_PASSWORD`: 데이터베이스 비밀번호
- `KIWOOM_API_ENABLED`: 키움증권 API 활성화 여부
- `KIWOOM_ACCOUNT_NO`: 키움증권 계좌번호
- `KIWOOM_PASSWORD`: 키움증권 비밀번호
- `MAX_INVESTMENT_AMOUNT`: 최대 투자금액
- `MIN_INVESTMENT_AMOUNT`: 최소 투자금액

## 빌드 및 실행

### 빌드

```bash
./gradlew build
```

### 실행

```bash
./gradlew bootRun
```

또는

```bash
java -jar build/libs/investment-choi-1.0.0.jar
```

### 개발 모드 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

## API 엔드포인트

### 계좌 조회

- `GET /api/v1/accounts/{accountNo}/balance` - 계좌 잔고 조회
- `GET /api/v1/accounts/{accountNo}/positions` - 보유 종목 조회

### 주문

- `POST /api/v1/orders` - 주문 실행
- `GET /api/v1/orders/{orderId}?accountNo={accountNo}` - 주문 조회
- `GET /api/v1/orders?accountNo={accountNo}` - 주문 목록 조회
- `DELETE /api/v1/orders/{orderId}?accountNo={accountNo}` - 주문 취소

### 설정

- `GET /api/v1/settings/{accountNo}` - 거래 설정 조회
- `PUT /api/v1/settings/{accountNo}` - 거래 설정 저장/업데이트

### AI 분석

- `POST /api/v1/analysis` - 종목 분석

## 웹 인터페이스

- `GET /` - 대시보드 (계좌번호를 쿼리 파라미터로 전달)

## 키움증권 Open API 연동

키움증권 Open API는 OCX 방식으로 제공됩니다. 실제 연동을 위해서는:

1. 키움증권 Open API SDK 설치
2. `KiwoomApiService` 클래스의 TODO 주석 부분 구현
3. 주요 API:
   - `CommConnect`: 로그인
   - `SendOrder`: 주문 전송
   - `SetInputValue` / `CommRqData`: 데이터 조회

## 데이터베이스 스키마

프로젝트는 JPA를 사용하며, 다음 엔티티들이 있습니다:

- `Order`: 주문 정보
- `TradingSetting`: 거래 설정
- `Portfolio`: 보유 종목

데이터베이스 스키마는 JPA가 자동으로 생성하거나, 수동으로 DDL을 실행해야 합니다.

## 테스트

```bash
./gradlew test
```

## 예외 처리

프로젝트는 계층화된 예외 처리를 사용합니다:

- `DomainException`: 비즈니스 규칙 위반
- `AppException`: 시스템 레벨 오류
- `GlobalExceptionHandler`: 전역 예외 처리

모든 API 오류는 다음 형식으로 반환됩니다:

```json
{
  "code": "ERROR_CODE",
  "message": "오류 메시지",
  "details": ["상세 오류 목록"],
  "traceId": "UUID",
  "timestamp": "2026-01-26T10:00:00"
}
```

## 보안 주의사항

- 키움증권 계좌번호 및 비밀번호는 환경 변수로 관리
- 프로덕션 환경에서는 HTTPS 사용 필수
- API 키 및 인증 정보는 절대 코드에 하드코딩하지 않음

## 라이선스

이 프로젝트는 개인 사용 목적으로 개발되었습니다.
