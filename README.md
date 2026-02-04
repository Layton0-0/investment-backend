# Investment Choi - 주식 투자 수익 분석 및 자동 매매 시스템

한국투자증권 Open API를 활용한 주식 투자 수익 분석 및 자동 매매 시스템입니다.

## 기술 스택

- **Backend**: Spring Boot 2.7.18 (Java 11)
- **Frontend**: Thymeleaf
- **Database**: MariaDB
- **Build Tool**: Gradle
- **API**: 한국투자증권 Open API

## 주요 기능

1. **계좌 조회**: 계좌 잔고 및 보유 종목 조회
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
├── marketdata/              # 시장 데이터 API 연동
├── domain/                  # 도메인 엔티티 및 리포지토리
└── common/                  # 공통 (예외 처리 등)
```

## 설정

### application.yml

주요 설정 항목:

```yaml
investment:
  market-data:
    provider: korea-investment
    korea-investment:
      app-key: ${KOREA_INVESTMENT_APP_KEY:}
      app-secret: ${KOREA_INVESTMENT_APP_SECRET:}
      server-type: ${KOREA_INVESTMENT_SERVER_TYPE:1}  # 1: 모의투자, 0: 실거래
  trading:
    max-investment-amount: ${MAX_INVESTMENT_AMOUNT:1000000}
    min-investment-amount: ${MIN_INVESTMENT_AMOUNT:10000}
    default-currency: USD
```

### 환경 변수

- `DB_USERNAME`: 데이터베이스 사용자명
- `DB_PASSWORD`: 데이터베이스 비밀번호
- `KOREA_INVESTMENT_APP_KEY`: 한국투자증권 API App Key
- `KOREA_INVESTMENT_APP_SECRET`: 한국투자증권 API App Secret
- `KOREA_INVESTMENT_SERVER_TYPE`: 서버 타입 (1: 모의투자, 0: 실거래)
- `MARKET_DATA_USE_MOCK_DATA`: 모의 데이터 사용 여부 (개발/테스트용)
- `MAX_INVESTMENT_AMOUNT`: 최대 투자금액
- `MIN_INVESTMENT_AMOUNT`: 최소 투자금액

## 로컬 개발 환경 구축

**완전히 새로운 컴퓨터에서도 한두 개 파일만 따라하면 설정 가능합니다!**

### 빠른 시작 (자동화)

```powershell
# 전체 자동화 스크립트 실행
.\scripts\setup-local-complete.ps1
```

### 단계별 가이드

자세한 설정 가이드는 다음 문서를 참고하세요:

- **[로컬 개발 환경 구축 가이드 (완전판)](docs/08-setup-guides/01-local-setup-complete.md)** - WSL2 + Docker Compose 중심
- **[부록 (마이그레이션, 인코딩, 트러블슈팅)](docs/08-setup-guides/02-appendix.md)** - 고급 설정 및 문제 해결

### 사전 요구사항 확인

```powershell
# 사전 요구사항 자동 확인
.\scripts\check-prerequisites.ps1
```

## 빌드 및 실행

### 빌드

```bash
./gradlew build
```

### 실행

```powershell
# .env 로드 후 실행 (권장)
.\scripts\bootRun-with-env.ps1

# 또는 직접 실행
.\gradlew.bat bootRun
```

또는

```bash
java -jar build/libs/investment-choi-1.0.0.jar
```

### 개발 모드 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### 테스트 및 커버리지

- **표준 테스트 실행 (권장)**  
  ```powershell
  .\gradlew.bat test --no-daemon
  ```  
  상세 옵션·스크립트·특정 테스트만 실행하는 방법은 [테스트 실행 가이드](docs/08-setup-guides/03-test-execution.md) 참고.
- **목표**: 라인 커버리지 80% 이상, 브랜치 커버리지 70% 이상 (JaCoCo `jacocoTestCoverageVerification`).
- **커버리지 포함 실행**  
  - PowerShell: `.\scripts\run-tests-with-coverage.ps1` (임시 빌드 사용 후 삭제)  
  - 또는 `.\gradlew.bat test jacocoTestReport --no-daemon`
- **Windows 참고**: `build`/`agent-build`를 사용 중인 다른 프로세스(IntelliJ, 이전 Gradle 등)가 있으면 테스트 실행 시 파일 잠금으로 실패할 수 있음. IntelliJ를 닫거나 해당 프로세스를 종료한 뒤 실행하거나, 2회차 재실행으로 회피할 수 있음.
- **리포트**: HTML은 `build/reports/jacoco/test/html/index.html` (또는 스크립트 사용 시 `coverage-report/index.html`).

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

## 한국투자증권 Open API 연동

한국투자증권 Open API는 REST API 방식으로 제공됩니다.

1. 한국투자증권 Open API 포털에서 App Key와 App Secret 발급
2. 환경 변수에 API 키 설정
3. 모의투자 서버로 테스트 후 실거래 서버 사용

자세한 내용은 [한국투자증권 API 가이드](./KOREA_INVESTMENT_API_GUIDE.md)를 참고하세요.

## 데이터베이스 스키마

프로젝트는 JPA를 사용하며, 다음 엔티티들이 있습니다:

- `Order`: 주문 정보
- `TradingSetting`: 거래 설정
- `Portfolio`: 보유 종목

데이터베이스 스키마는 JPA가 자동으로 생성하거나, 수동으로 DDL을 실행해야 합니다.

## 테스트

**표준 실행 (Windows):**

```powershell
.\gradlew.bat test --no-daemon
```

**Linux/macOS:**

```bash
./gradlew test --no-daemon
```

자세한 내용은 [테스트 실행 가이드](docs/08-setup-guides/03-test-execution.md) 참고.

**커버리지**: JaCoCo 리포트 생성 — 라인 80%, 브랜치 70% 목표.

```powershell
.\gradlew.bat test jacocoTestReport --no-daemon
# 또는 .\scripts\run-tests-with-coverage.ps1
```

리포트: `build/reports/jacoco/test/html/index.html`. 한계값 검증: `.\gradlew.bat jacocoTestCoverageVerification --no-daemon`.

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

- 한국투자증권 API 키 및 시크릿은 환경 변수로 관리
- 프로덕션 환경에서는 HTTPS 사용 필수
- API 키 및 인증 정보는 절대 코드에 하드코딩하지 않음
- 개발/테스트 시 모의투자 서버 사용 권장

## 라이선스

이 프로젝트는 개인 사용 목적으로 개발되었습니다.
