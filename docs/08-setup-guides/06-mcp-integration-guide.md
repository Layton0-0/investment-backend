# 한국투자증권 MCP 통합 가이드

## 개요

한국투자 코딩도우미 MCP(Model Context Protocol)를 활용하여 한국투자증권 Open API 개발을 효율적으로 진행하는 방법을 안내합니다.

## MCP란?

MCP(Model Context Protocol)는 Claude를 개발한 Anthropic에서 만든 프로토콜로, AI 모델이 외부 도구와 데이터에 안전하고 효율적으로 접근할 수 있게 해주는 표준화된 인터페이스입니다.

한국투자증권이 제공하는 **KIS Code Assistant MCP**를 통해 자연어로 한국투자증권 API를 검색하고, 예제 코드를 자동으로 생성받을 수 있습니다.

## 사전 준비

### 1. 한국투자증권 Open API 신청

1. 한국투자증권 계좌 개설 및 ID 연결
2. 한국투자증권 홈페이지 또는 앱에서 Open API 서비스 신청
3. 앱키(App Key), 앱시크릿(App Secret) 발급
4. 모의투자 및 실전투자 앱키 각각 준비

[서비스 신청 안내 바로가기](https://apiportal.koreainvestment.com/about-howto)

### 2. MCP 서버 설치

Cursor에서 KIS Code Assistant MCP를 설치합니다:

1. [KIS Code Assistant MCP 페이지](https://smithery.ai/server/@KISOpenAPI/kis-code-assistant-mcp) 접속
2. **AUTO / Cursor** 선택
3. **One-Click Install** 클릭
4. Cursor에서 **Install** 클릭
5. Cursor 재시작

설치 확인: `Settings` > `MCP Servers`에서 `KIS Code Assistant MCP` 확인

### 3. Python 환경 설정 (선택사항)

MCP 서버 자체는 Cursor가 관리하지만, 로컬에서 테스트하려면 Python 환경이 필요할 수 있습니다.

```powershell
# Python 3.13 이상 권장
python --version

# uv 설치 (패키지 관리자)
powershell -c "irm https://astral.sh/uv/install.ps1 | iex"
```

## MCP 활용 개발 워크플로우

### 1. 자연어로 API 검색

Cursor의 채팅에서 한국투자 코딩도우미 MCP를 활용하여 API를 검색합니다:

**예시 질문들**:
- "한국투자증권 주식 현재가 조회 API 코드 보여줘"
- "주식 매수 주문 API 호출 방법 알려줘"
- "실시간 시세 조회는 어떻게 하나요?"
- "계좌 잔고 조회 API 파라미터는 뭐야?"

### 2. MCP 응답 활용

MCP는 다음 정보를 제공합니다:
- 관련 API 엔드포인트 및 TR ID
- 필수 파라미터 및 헤더
- 예제 호출 코드 (Python 기반)
- API 스키마 정보

### 3. Java 프로젝트에 통합

MCP가 제공한 Python 예제 코드를 기존 Java Spring Boot 프로젝트 구조에 맞게 변환합니다.

**통합 원칙**:
1. 기존 클라이언트 구조 유지 (`KoreaInvestmentAccountClient`, `KoreaInvestmentMarketDataClient`)
2. 공통 유틸리티 활용 (`KoreaInvestmentRequestBuilder`, `KoreaInvestmentHashkeyUtil`)
3. 사용자별 API 키는 DB에서 조회 (암호화된 키 사용)
4. Rate Limiter 및 Circuit Breaker 적용

## 개발 예시

### 예시 1: 주식 현재가 조회 API 통합

**1단계: MCP에 질문**
```
"한국투자증권 주식 현재가 조회 API 코드 보여줘"
```

**2단계: MCP 응답 확인**
- API 엔드포인트: `/uapi/domestic-stock/v1/quotations/inquire-price`
- TR ID: `FHKST01010100` (실거래) / `FHKST01010100` (모의투자)
- 필수 파라미터: `FID_COND_MRKT_DIV_CODE`, `FID_INPUT_ISCD`

**3단계: Java 코드 작성**

기존 `KoreaInvestmentMarketDataClient`에 메서드 추가:

```java
public Mono<CurrentPriceResponse> getCurrentPrice(String symbol) {
    // MCP가 제공한 정보를 바탕으로 구현
    // 기존 패턴 활용: KoreaInvestmentRequestBuilder, 토큰 관리 등
}
```

### 예시 2: 주문 API 통합

**1단계: MCP에 질문**
```
"한국투자증권 주식 매수 주문 API 코드 보여줘"
```

**2단계: MCP 응답 확인**
- API 엔드포인트: `/uapi/domestic-stock/v1/trading/order-cash`
- TR ID: `TTTC0802U` (실거래) / `VTTC0802U` (모의투자)
- Hashkey 필요 여부 확인

**3단계: Java 코드 작성**

새로운 `KoreaInvestmentOrderClient` 클래스 생성 또는 기존 클라이언트에 추가:

```java
@Component
public class KoreaInvestmentOrderClient {
    // MCP가 제공한 정보를 바탕으로 구현
    // Hashkey 생성: KoreaInvestmentHashkeyUtil 활용
    // Rate Limiter 적용
}
```

## 기존 프로젝트 구조와의 통합

### 1. 계층 구조

```
Controller Layer (REST API)
    ↓
Service Layer (비즈니스 로직)
    ↓
Client Layer (외부 API 호출)
    - KoreaInvestmentMarketDataClient
    - KoreaInvestmentAccountClient
    - KoreaInvestmentOrderClient (신규)
```

### 2. 사용자별 API 키 관리

MCP 설정 파일(`kis_devlp.yaml`)은 개발 환경에서만 사용하며, 프로덕션에서는 DB의 암호화된 API 키를 사용합니다:

```java
// 프로덕션 코드 예시
UserApiKey userApiKey = userApiKeyRepository
    .findByUserIdAndBrokerType(userId, BrokerType.KOREA_INVESTMENT)
    .orElseThrow();

String appKey = encryptionUtil.decrypt(userApiKey.getAppKeyEncrypted());
String appSecret = encryptionUtil.decrypt(userApiKey.getAppSecretEncrypted());
```

### 3. 공통 패턴 활용

#### 공통 헤더 생성
```java
HttpHeaders headers = KoreaInvestmentRequestBuilder.createCommonHeaders(
    accessToken, appKey, appSecret, trId);
```

#### 계좌 관련 API 공통 바디 생성
```java
Map<String, String> requestBody = KoreaInvestmentRequestBuilder
    .createAccountRequestBody(accountNo, additionalParams);
```

#### Hashkey 생성 (주문 API용)
```java
String hashkey = hashkeyUtil.generateHashkey(requestBody, appSecret);
headers.set("hashkey", hashkey);
```

### 4. Rate Limiting

모든 API 호출에 Resilience4j RateLimiter를 적용합니다:

```java
RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(
    serverType.equals("0") ? RATE_LIMITER_API_REAL : RATE_LIMITER_API_VIRTUAL
);

return rateLimiter.executeSupplier(() -> {
    // API 호출 로직
});
```

## 보안 고려사항

### 1. API 키 관리

- **개발 환경**: MCP 설정 파일(`kis_devlp.yaml`) 사용 가능
- **프로덕션 환경**: DB의 암호화된 API 키만 사용
- **절대 공개 저장소에 업로드 금지**: `.gitignore`에 설정 파일 추가 확인

### 2. 토큰 관리

기존 `KoreaInvestmentTokenService`를 활용하여 토큰을 관리합니다:

```java
@Autowired
private KoreaInvestmentTokenService tokenService;

String accessToken = tokenService.getAccessToken(userId);
// 토큰이 만료되면 자동으로 재발급됨
```

## 문제 해결

### MCP 연결 실패

1. Cursor 재시작
2. MCP 서버 URL 확인: [KIS Code Assistant MCP](https://smithery.ai/server/@KISOpenAPI/kis-code-assistant-mcp)
3. 방화벽 설정 확인
4. 인터넷 연결 확인

### API 호출 오류

1. MCP가 제공한 API 스키마와 실제 구현 비교
2. 필수 파라미터 확인 (Required='Y' 체크)
3. TR ID 확인 (실거래/모의투자 구분)
4. Hashkey 필요 여부 확인 (주문 API)

### 코드 통합 오류

1. 기존 클라이언트 구조와 일치하는지 확인
2. 공통 유틸리티 클래스 활용 여부 확인
3. Rate Limiter 적용 여부 확인
4. 예외 처리 로직 확인

## 참고 자료

- [한국투자증권 API 가이드](../04-api/09-korea-investment-api-guide.md)
- [시스템 아키텍처](../02-architecture/01-system-architecture.md)
- [필수 기술 스펙](../02-architecture/10-essential-tech-spec.md)
- 한국투자증권 Open API 포털: https://apiportal.koreainvestment.com/
- KIS Code Assistant MCP: https://smithery.ai/server/@KISOpenAPI/kis-code-assistant-mcp

## 문서 변경 이력

| 버전 | 일자 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-28 | System | 초기 문서 작성 |
