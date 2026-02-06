# 한국투자증권 API 가이드

## 개요

한국투자증권 Open API를 사용하여 국내 주식 시장 데이터를 조회할 수 있습니다.

## 개발 시 필수: 한국투자증권 MCP 사용

이 프로젝트에서는 **한국투자증권 API**를 추가·수정할 때 **한국투자증권 MCP**를 반드시 사용한다.

- **새 API 연동**: MCP로 해당 엔드포인트의 HTTP 메서드(GET/POST), 파라미터 전달 방식(query parameter vs request body)을 확인한 뒤 구현한다.
- **기존 API 수정**: MCP로 스펙을 확인한 뒤 요청 방식·파라미터가 스펙과 일치하는지 검증한다.
- **금지**: MCP 확인 없이 공식 문서 추정만으로 요청 형식(POST+body vs GET+query)을 가정하여 구현하지 않는다.

규칙 상세: [.cursor/rules/MCP.mdc](../../.cursor/rules/MCP.mdc). 결정 사항: [ADR 14 한국투자증권 API 요청 방식 및 MCP 사용](../decisions.md#14-한국투자증권-api-요청-방식-및-mcp-사용).

## 아키텍처

```
MarketDataClient (인터페이스)
└── KoreaInvestmentMarketDataClient (한국투자증권 구현체)
```

## 설정 방법

### application.yml

```yaml
investment:
  market-data:
    provider: korea-investment
    use-mock-data: ${MARKET_DATA_USE_MOCK_DATA:false}
    korea-investment:
      app-key: ${KOREA_INVESTMENT_APP_KEY:}
      app-secret: ${KOREA_INVESTMENT_APP_SECRET:}
      server-type: ${KOREA_INVESTMENT_SERVER_TYPE:1}  # 1: 모의투자, 0: 실거래
      use-token-cache: ${KOREA_INVESTMENT_USE_TOKEN_CACHE:true}
```

### 환경 변수

```bash
# 제공자 선택
export MARKET_DATA_PROVIDER=korea-investment

# 한국투자증권 API 키
export KOREA_INVESTMENT_APP_KEY=your_app_key
export KOREA_INVESTMENT_APP_SECRET=your_app_secret

# 서버 타입 (1: 모의투자, 0: 실거래)
export KOREA_INVESTMENT_SERVER_TYPE=1

# 모의 데이터 사용 (개발/테스트용)
export MARKET_DATA_USE_MOCK_DATA=true
```

## 한국투자증권 API 특징

- **인증 방식**: OAuth 2.0 (App Key, App Secret)
- **API 타입**: REST API
- **종목 코드**: 6자리 숫자 코드 사용
  - 코스피: 000000~099999
  - 코스닥: 100000~999999
- **지원 지표**: RSI, MACD, EMA, Bollinger Bands, ATR, VWAP
- **Base URL**:
  - 실거래: `https://openapi.koreainvestment.com:9443`
  - 모의투자: `https://openapivts.koreainvestment.com:29443`

## 종목 코드 사용 예시

```java
// 6자리 종목코드 사용
String symbol = "005930"; // 삼성전자

// 종목명으로도 사용 가능 (StockCodeConverter가 자동 변환)
String symbol = "삼성전자"; // 자동으로 "005930"으로 변환
```

## 주요 종목 코드

### 코스피 대형주
- `005930`: 삼성전자
- `000660`: SK하이닉스
- `035420`: NAVER
- `051910`: LG화학
- `006400`: 삼성SDI
- `035720`: 카카오
- `207940`: 삼성바이오로직스
- `005380`: 현대차

### 코스닥 대형주
- `035900`: JYP엔터테인먼트
- `251270`: 넷마블

## API 인증 프로세스

1. **Access Token 발급**
   - OAuth 2.0 `client_credentials` 방식
   - 엔드포인트: `/oauth2/tokenP`
   - **요청 형식**: JSON (POST)
   - **Content-Type**: `application/json`
   - **요청 바디**:
     ```json
     {
       "grant_type": "client_credentials",
       "appkey": "YOUR_APP_KEY",
       "appsecret": "YOUR_APP_SECRET"
     }
     ```
   - 토큰 유효기간: 24시간
   - 자동 갱신 지원

2. **API 호출**
   - **필수 헤더**:
     - `authorization: Bearer {access_token}` - 발급받은 Access Token
     - `appkey: {app_key}` - App Key (Required='Y')
     - `appsecret: {app_secret}` - App Secret (Required='Y')
     - `tr_id: {tr_id}` - 거래 ID (API별로 다름)
     - `Content-Type: application/json`
   - **중요**: 한국투자증권 API 문서에서 Required='Y'로 표시된 파라미터는 모든 API 호출 시 반드시 포함해야 합니다. `appkey`와 `appsecret`은 필수 헤더입니다.

## 공통 필수 파라미터

한국투자증권 Open API를 호출할 때 모든 API에서 공통으로 사용되는 필수 파라미터가 있습니다.

### 공통 필수 헤더

모든 API 호출 시 다음 헤더는 **반드시** 포함되어야 합니다:

| 헤더명 | 타입 | 필수 여부 | 설명 |
|--------|------|-----------|------|
| `authorization` | String | Required | OAuth 2.0 Access Token (형식: `Bearer {access_token}`) |
| `appkey` | String | Required='Y' | 발급받은 App Key |
| `appsecret` | String | Required='Y' | 발급받은 App Secret |
| `tr_id` | String | Required | 거래 ID (API별로 고유한 값, 실거래/모의투자에 따라 다름) |
| `Content-Type` | String | Required | `application/json` |

**참고**: 
- `appkey`와 `appsecret`은 한국투자증권 API 문서에서 Required='Y'로 명시된 필수 파라미터입니다.
- `tr_id`는 각 API마다 고유한 값이며, 실거래와 모의투자 서버에서 다른 값을 사용합니다.
- **조회 API**(계좌·시세 조회)는 **GET** 메서드이며 파라미터는 **URI query parameter**로 전달합니다. (주문 실행·토큰 발급 등은 POST + JSON body.)

### 조회 API 요청 방식 (GET + query parameter)

한국투자증권 국내주식 **조회** API(주식잔고조회, 매수가능조회, 매도가능수량조회, 주문체결조회, 투자계좌자산현황조회, 기간별손익조회, 현재가시세, 차트 조회 등)는 다음을 따릅니다:

- **HTTP 메서드**: **GET**
- **파라미터 전달**: **URI query parameter** (JSON body 아님)

예: `GET /uapi/domestic-stock/v1/trading/inquire-balance?CANO=12345678&ACNT_PRDT_CD=01&INQR_DVSN=02&...`

### 공통 파라미터 (조회 API는 query parameter로 전달)

#### 계좌 관련 API 공통 파라미터

계좌 관련 조회 API(주식잔고조회, 매수가능조회, 매도가능수량조회, 주문체결조회 등)는 다음 공통 파라미터를 **query parameter**로 포함합니다:

| 파라미터명 | 타입 | 필수 여부 | 기본값 | 설명 |
|-----------|------|-----------|-------|------|
| `CANO` | String | Required | - | 계좌번호 (8자리 또는 10자리) |
| `ACNT_PRDT_CD` | String | Required | `"01"` | 계좌상품코드 (`"01"`: 주식) |

**사용 예시** (Map을 URI query parameter로 사용):
```java
// 공통 파라미터 Map 생성 후 GET 요청의 query parameter로 전달
Map<String, String> queryParams = KoreaInvestmentRequestBuilder.createAccountRequestBody(
    accountNo, Map.of("INQR_DVSN", "02", "PDNO", "005930"));
URI uri = buildUriWithQueryParams(baseUrl, PATH_INQUIRE_BALANCE, queryParams);
webClient.get().uri(uri).headers(...).retrieve()...
```

#### 시세 관련 API 공통 파라미터

시세 관련 조회 API(차트 조회, 현재가 조회 등)는 계좌번호가 필요 없으며, API별 고유 파라미터를 **query parameter**로 전달합니다.

**차트 조회 API 예시**:
```java
Map<String, String> requestBody = new HashMap<>();
requestBody.put("FID_COND_MRKT_DIV_CODE", "J"); // 시장구분코드 (J: 주식, ETF, ETN)
requestBody.put("FID_INPUT_ISCD", "005930"); // 종목코드
requestBody.put("FID_INPUT_DATE_1", "20250101"); // 시작일자
requestBody.put("FID_INPUT_DATE_2", "20250131"); // 종료일자
requestBody.put("FID_PERIOD_DIV_CODE", "D"); // 기간분할코드 (D: 일봉, W: 주봉, M: 월봉)
```

### 공통 파라미터 사용 가이드

본 시스템에서는 공통 파라미터 생성을 위한 유틸리티 클래스 `KoreaInvestmentRequestBuilder`를 제공합니다:

```java
// 공통 헤더 생성
HttpHeaders headers = KoreaInvestmentRequestBuilder.createCommonHeaders(
    accessToken, appKey, appSecret, trId);

// 계좌 관련 조회 API: 파라미터 Map 생성 후 GET 요청의 query parameter로 사용
Map<String, String> queryParams = KoreaInvestmentRequestBuilder.createAccountRequestBody(
    accountNo, 
    Map.of(
        "INQR_DVSN", "02",  // API별 고유 파라미터
        "AFHR_FLPR_YN", "N"
    )
);
URI uri = buildUriWithQueryParams(baseUrl, path, queryParams);
webClient.get().uri(uri).headers(h -> h.addAll(headers)).retrieve()...
```

**장점**:
- 코드 중복 제거: 공통 로직을 한 곳에서 관리
- 유지보수성 향상: 공통 파라미터 변경 시 한 곳만 수정
- 일관성 보장: 모든 API 호출이 동일한 방식으로 처리
- 확장성: 새로운 API 추가 시 공통 유틸리티 재사용 가능

## 차트 데이터 조회

한국투자증권 API는 차트 데이터를 제공하며, 클라이언트에서 기술적 지표를 계산합니다.

- **TR ID**: `FHKST03010100` (주식현재가 일봉차트 조회)
- **엔드포인트**: `/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice`
- **지원 기간**: 일봉, 주봉, 월봉
- **필수 파라미터**: `FID_COND_MRKT_DIV_CODE`, `FID_INPUT_ISCD`, `FID_INPUT_DATE_1`, `FID_INPUT_DATE_2`, `FID_PERIOD_DIV_CODE`, `FID_ORG_ADJ_PRC` (0: 수정주가, 1: 원주가)

### 응답 구조

한국투자증권 API 응답은 다음과 같은 구조를 가집니다:

```json
{
  "rt_cd": "0",        // 응답 코드 ("0": 성공, 그 외: 실패)
  "msg_cd": "MCA00000", // 메시지 코드
  "msg1": "정상처리",   // 메시지
  "output": {},        // 출력 데이터 (API별로 다름)
  "output2": []         // 출력 데이터 배열 (차트 데이터 등)
}
```

**에러 처리**: `rt_cd`가 "0"이 아니면 에러로 처리하며, `msg1`에 에러 메시지가 포함됩니다.

## 구현된 기능

### 현재 구현된 기능
- ✅ OAuth 2.0 인증 (Access Token 발급 및 자동 갱신)
- ✅ 차트 데이터 조회
- ✅ 기술적 지표 계산 (RSI, MACD, EMA, Bollinger Bands, ATR, VWAP)
- ✅ 종목 코드 변환 (StockCodeConverter 활용)
- ✅ 모의 데이터 지원 (개발/테스트용)
- ✅ 실거래/모의투자 서버 선택
- ✅ 계좌 관련 API (주식잔고조회, 매수가능조회, 매도가능수량조회, 주문체결조회, 투자계좌자산현황조회, 기간별손익조회)

### 기술적 지표 계산

차트 데이터로부터 다음 지표를 계산합니다:

- **RSI**: 14일 기준 상대강도지수
- **MACD**: 12일/26일 지수이동평균 기반
- **EMA**: 20일, 60일, 120일 지수이동평균
- **Bollinger Bands**: 20일 이동평균, 2 표준편차
- **ATR**: 14일 평균진폭
- **VWAP**: 거래량 가중 평균 가격

## API 유량 제한 (Rate Limit)

한국투자증권 API는 계좌(앱키) 단위로 유량 제한이 적용됩니다. (2025.08.07 기준)

### REST API 유량 제한

- **실전투자**: 1초당 20건 (1계좌당)
- **모의투자**: 1초당 2건 (1계좌당)
- **접근토큰발급** (`/oauth2/tokenP`): 1초당 1건 (2023.10.27 시행)

### WebSocket 유량 제한

- **1세션당**: 실시간체결가 + 호가 + 예상체결 + 체결통보 등 실시간 데이터 합산 41건까지 등록 가능
- **제한 범위**: 국내주식/해외주식/국내파생/해외파생 모든 상품 실시간 합산 41건
- **체결통보**: HTS ID 단위로 등록하여, ID에 연결된 모든 계좌의 체결 통보 수신
- **계좌(앱키) 단위**: 1개의 계좌(앱키)당 1세션
- **다중 계좌**: 1개의 PC에서 여러 개의 계좌(앱키)로 세션 연결 가능

### WebSocket 사용 정책 (중요)

한국투자증권은 실시간 웹소켓 시스템에 악영향을 주는 무한 접속 시도, 무한 종목 등록 시도 등으로 인해 **자동 차단** 정책을 시행하고 있습니다.

#### 1. 웹소켓 이용 순서 (필수 준수)

웹소켓 이용 시 다음 순서를 **반드시** 지켜야 합니다:

```
1. 연결 시도
2. 접속 확인
3. 구독 정보 등록
4. 정보 수신
5. PINGPONG 응답 처리 (정보가 없을 경우)
6. 구독 해제 (정보가 필요 없을 경우)
7. 접속 해제
```

⚠️ **주의**: 연결/종료만 반복하거나 위 순서를 지키지 않으면 **자동 차단**될 수 있습니다.

#### 2. 연결/종료 간격

- **최소 간격**: 연결/종료 간격은 **최소 1초** 이상 유지해야 합니다.
- **무한 루프 방지**: 연결과 종료를 빠르게 반복하면 자동 차단됩니다.

#### 3. 구독 등록 간격

- **건당 등록**: 종목을 한 번에 등록 처리하는 경우를 제외하고, 건당 등록은 **0.2초 이내**로 권장됩니다.
- **일괄 등록 권장**: 가능한 경우 여러 종목을 한 번에 등록하는 것을 권장합니다.

#### 4. 자동 차단 정책

다음과 같은 행위는 **자동 차단** 대상입니다:

- ❌ 무한 접속 시도
- ❌ 무한 종목 등록 시도
- ❌ 연결/종료만 반복 (최소 1초 간격 미준수)
- ❌ 웹소켓 이용 순서 미준수
- ❌ 구독 등록 간격 미준수 (0.2초 이내 권장)

#### 5. 개발 권장사항

웹소켓 프로그래밍을 적용하기 전에:

1. ✅ **웹소켓 호출 처리 방식 숙지**: 한국투자증권 API 문서를 충분히 읽고 이해
2. ✅ **충분한 사전 테스트**: 모의투자 환경에서 충분히 테스트 후 실전투자 사용
3. ✅ **에러 처리**: 연결 실패, 재연결 로직 등 예외 상황 처리
4. ✅ **Rate Limiting**: 구독 등록 시 적절한 지연 시간 적용
5. ✅ **로그 모니터링**: 연결 상태, 구독 상태 등을 로그로 모니터링

#### 6. 구현 시 고려사항

향후 웹소켓 구현 시 다음을 고려해야 합니다:

```java
// 예시: 웹소켓 연결 관리 클래스
public class KoreaInvestmentWebSocketClient {
    
    // 연결/종료 간격 제어 (최소 1초)
    private static final long MIN_CONNECTION_INTERVAL_MS = 1000;
    private long lastConnectionTime = 0;
    
    // 구독 등록 간격 제어 (0.2초 이내 권장)
    private static final long SUBSCRIPTION_INTERVAL_MS = 200;
    private long lastSubscriptionTime = 0;
    
    public void connect() {
        long now = System.currentTimeMillis();
        if (now - lastConnectionTime < MIN_CONNECTION_INTERVAL_MS) {
            throw new IllegalStateException("연결 간격이 너무 짧습니다. 최소 1초 간격이 필요합니다.");
        }
        lastConnectionTime = now;
        // 연결 로직...
    }
    
    public void subscribe(String symbol) {
        long now = System.currentTimeMillis();
        if (now - lastSubscriptionTime < SUBSCRIPTION_INTERVAL_MS) {
            // 지연 처리 또는 일괄 등록
            scheduleSubscription(symbol);
            return;
        }
        lastSubscriptionTime = now;
        // 구독 로직...
    }
}
```

### 유량 초과 정책

- 초과 유량에 대한 과금 정책은 현재 없습니다.
- 추가 유량이 필요한 경우: 다른 계좌 API 신청 등록하여 발급받은 앱정보(appkey, appsecret)로 이용

### Rate Limiter 적용

본 시스템에서는 Resilience4j RateLimiter를 사용하여 API 유량 제한을 자동으로 준수합니다:

- **실전투자**: 초당 20건 제한
- **모의투자**: 초당 2건 제한
- **토큰 발급**: 초당 1건 제한

Rate Limiter는 자동으로 호출을 제한하며, 초과 시 적절한 대기 시간 후 재시도합니다.

## 보안 요구사항 (TLS)

한국투자증권 API는 보안 강화를 위해 TLS 버전 요구사항을 적용합니다.

### TLS 버전 요구사항

- **2025.12.12(금) 이후**: TLS 1.0, 1.1 지원 중단
- **필수**: TLS 1.2 이상 사용 필수
- **권장**: TLS 1.3 사용 권장

### 시스템 요구사항

- **Java 버전**: Java 11 이상 (기본적으로 TLS 1.2 지원)
- **현재 프로젝트**: Java 17 사용 (TLS 1.2, 1.3 지원)
- **WebClient**: Spring WebFlux의 WebClient는 시스템의 TLS 설정을 따름

### 확인 방법

Java 17을 사용하는 경우 기본적으로 TLS 1.2 이상을 지원하므로 별도 설정이 필요 없습니다.

만약 TLS 버전을 명시적으로 확인하고 싶다면, 다음 시스템 속성을 설정할 수 있습니다:

```bash
# TLS 1.2 이상만 허용
-Djdk.tls.client.protocols=TLSv1.2,TLSv1.3
```

## Hashkey 생성 (향후 확장)

일부 API(주문 등)는 요청 바디의 무결성을 검증하기 위해 Hashkey가 필요할 수 있습니다.

- **유틸리티 클래스**: `KoreaInvestmentHashkeyUtil`
- **생성 방법**: 요청 바디를 JSON 문자열로 변환한 후, appsecret을 키로 사용하여 HMAC SHA256으로 생성
- **사용 예시**:
  ```java
  @Autowired
  private KoreaInvestmentHashkeyUtil hashkeyUtil;
  
  String hashkey = hashkeyUtil.generateHashkey(requestBody, appSecret);
  headers.set("hashkey", hashkey);
  ```

**참고**: 현재 사용하는 차트 조회 API에는 Hashkey가 필요하지 않지만, 향후 주문 API 등에서 사용할 수 있도록 유틸리티가 제공됩니다.

## 계좌 관련 API

### 구현된 계좌 API

한국투자증권 Open API를 사용하여 계좌 관련 정보를 조회할 수 있습니다.

#### 1. 주식잔고조회
- **TR ID**: `TTTC8434R` (실거래) / `VTTC8434R` (모의투자)
- **엔드포인트**: `/uapi/domestic-stock/v1/trading/inquire-balance`
- **요청 방식**: **GET** + query parameter
- **기능**: 계좌 잔고 정보 및 보유 종목 목록 조회
- **사용 클래스**: `KoreaInvestmentAccountClient.inquireBalance()`
- **시장(KR/US) 구분**: 보유 종목 응답에 거래소 구분 필드(`excg_dvsn_cd` 등)가 있으면 KRX→KR, NASD/NYSE/AMEX→US로 매핑하여 `AccountPositionDto.market`에 설정.

#### 1-2. 해외주식 현재잔고(체결기준) 조회
- **TR ID**: `CTRP6504R` (실거래) / `VTRP6504R` (모의투자)
- **엔드포인트**: `/uapi/overseas-stock/v1/trading/inquire-present-balance`
- **요청 방식**: **GET** + query parameter
- **기능**: 미국(840) 외화(02) 기준 체결 잔고·보유 종목 목록 조회
- **사용 클래스**: `KoreaInvestmentAccountClient.inquireOverseasBalance(userId, accountNo)` — 국내 잔고와 별도 호출 후 `AccountService.getBalanceAndPositions`에서 국내·해외 보유를 병합하여 반환
- **필수 파라미터**: `CANO`, `ACNT_PRDT_CD`, `WCRC_FRCR_DVSN_CD`(02: 외화), `NATN_CD`(840: 미국), `TR_MKET_CD`(00: 전체), `INQR_DVSN_CD`(00: 전체)
- **응답**: `output1` 배열에 보유 종목(필드명은 KIS 해외 API 스펙·ovrs_* 등). 파싱 후 `AccountPositionDto`에 `market=US`, `currency=USD` 설정

#### 2. 매수가능조회
- **TR ID**: `TTTC8908R` (실거래) / `VTTC8908R` (모의투자)
- **엔드포인트**: `/uapi/domestic-stock/v1/trading/inquire-psbl-order`
- **요청 방식**: **GET** + query parameter
- **기능**: 종목별 매수 가능 금액 및 수량 조회
- **사용 클래스**: `KoreaInvestmentAccountClient.inquireBuyableAmount()`
- **필수 파라미터**: `CANO`, `ACNT_PRDT_CD`, `PDNO`, `ORD_UNPR`, `ORD_DVSN`, `CMA_EVLU_AMT_ICLD_YN`, `OVRS_ICLD_YN`

#### 3. 매도가능수량조회
- **TR ID**: `TTTC8901R` (실거래) / `VTTC8901R` (모의투자)
- **엔드포인트**: `/uapi/domestic-stock/v1/trading/inquire-psbl-order2`
- **요청 방식**: **GET** + query parameter
- **기능**: 종목별 매도 가능 수량 조회
- **사용 클래스**: `KoreaInvestmentAccountClient.inquireSellableQuantity()`

#### 4. 주식일별주문체결조회
- **TR ID**: `TTTC0081R` (실거래, 3개월 이내) / `VTTC0081R` (모의투자, 3개월 이내)
- **TR ID (3개월 이전)**: `CTSC9215R` (실거래) / `VTSC9215R` (모의투자)
- **엔드포인트**: `/uapi/domestic-stock/v1/trading/inquire-daily-ccld`
- **요청 방식**: **GET** + query parameter
- **기능**: 일별 주문 체결 내역 조회
- **사용 클래스**: `KoreaInvestmentAccountClient.inquireOrderHistory()`
- **필수 파라미터**: `CANO`, `ACNT_PRDT_CD`, `INQR_STRT_DT`, `INQR_END_DT`, `SLL_BUY_DVSN_CD`, `CCLD_DVSN`, `INQR_DVSN`, `INQR_DVSN_3`, `EXCG_ID_DVSN_CD` (선택)

#### 5. 투자계좌자산현황조회
- **TR ID**: `CTRP6548R` (실거래 전용)
- **엔드포인트**: `/uapi/domestic-stock/v1/trading/inquire-account-balance`
- **요청 방식**: **GET** + query parameter
- **기능**: 계좌 자산 현황 종합 조회 (output1·output2 반환, output2 기준 DTO 매핑). **모의계좌 미지원** — 모의계좌일 때는 `inquireAssets()` 내부에서 주식잔고조회(inquire-balance) 결과로 자산 요약을 구성해 반환(폴백).
- **사용 클래스**: `KoreaInvestmentAccountClient.inquireAssets()`
- **필수 파라미터**: `CANO`, `ACNT_PRDT_CD` / 선택: `INQR_DVSN_1`, `BSPR_BF_DT_APLY_YN`

#### 6. 기간별손익일별합산조회
- **TR ID**: `TTTC8708R` (실거래) / `VTTC8708R` (모의투자)
- **엔드포인트**: `/uapi/domestic-stock/v1/trading/inquire-period-profit-loss`
- **요청 방식**: **GET** + query parameter
- **기능**: 기간별 일별 손익 합산 조회
- **사용 클래스**: `KoreaInvestmentAccountClient.inquirePeriodProfitLoss()`

### 계좌 API 사용 예시

```java
@Autowired
private KoreaInvestmentAccountClient accountClient;

// 주식잔고조회
BalanceAndPositionsResult result = accountClient.inquireBalance(userId, accountNo);
AccountBalanceDto balance = result.getBalance();
List<AccountPositionDto> positions = result.getPositions();

// 매수가능조회
BuyableAmountDto buyable = accountClient.inquireBuyableAmount(userId, accountNo, "005930", new BigDecimal("75000"));

// 매도가능수량조회
SellableQuantityDto sellable = accountClient.inquireSellableQuantity(userId, accountNo, "005930");

// 주문체결조회
List<OrderHistoryDto> orderHistory = accountClient.inquireOrderHistory(
    userId, accountNo, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

// 투자계좌자산현황조회
AccountAssetDto assets = accountClient.inquireAssets(userId, accountNo);

// 기간별손익조회
ProfitLossDto profitLoss = accountClient.inquirePeriodProfitLoss(
    userId, accountNo, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
```

### 계좌 API 특징

- **Rate Limiting**: 모든 계좌 API는 자동으로 Rate Limiter가 적용됩니다.
  - 실전투자: 초당 20건
  - 모의투자: 초당 2건
- **에러 처리**: API 실패 시 DB 폴백 전략을 사용합니다.
- **캐싱**: 계좌 잔고 및 보유 종목은 1분 TTL로 캐싱됩니다.
- **인증**: 모든 API 호출은 사용자별 Access Token을 사용합니다.

## 주의사항

1. **토큰 발급 API 요청 형식**: `/oauth2/tokenP` 엔드포인트는 **JSON 형식**으로 POST 요청해야 합니다. Form 데이터로 보내면 오류가 발생합니다.
2. **API 키 발급**: 한국투자증권 홈페이지에서 App Key와 App Secret을 발급받아야 합니다.
3. **헤더 구성**: 한국투자증권 API 문서에서 Required='Y'로 표시된 파라미터(`appkey`, `appsecret`)는 모든 API 호출 시 필수 헤더에 포함해야 합니다.
4. **응답 처리**: `rt_cd`가 "0"이 아니면 에러로 처리하며, `msg1` 필드를 확인해야 합니다.
5. **Rate Limit 준수**: 시스템에서 자동으로 Rate Limiter를 적용하지만, 대량 호출 시 주의가 필요합니다.
6. **모의투자 권장**: 개발/테스트 시 모의투자 서버 사용을 권장합니다.
7. **토큰 관리**: Access Token은 자동으로 캐시되며, 만료 시 자동 갱신됩니다.
8. **계좌 단위 제한**: 유량 제한은 계좌(앱키) 단위로 적용되므로, 여러 계좌를 사용하는 경우 각각 별도 제한이 적용됩니다.
9. **TLS 버전**: 2025.12.12(금) 이후 TLS 1.2 이상 필수 (현재 Java 17 사용으로 자동 준수)
10. **WebSocket 정책 준수**: 웹소켓 사용 시 반드시 이용 순서, 연결/종료 간격, 구독 등록 간격을 준수해야 합니다. 미준수 시 자동 차단될 수 있습니다.
11. **계좌 API 폴백**: 계좌 API 호출 실패 시 자동으로 DB 폴백을 사용하여 기존 데이터를 반환합니다.

## 테스트

```bash
# 모의 데이터로 테스트
export MARKET_DATA_PROVIDER=korea-investment
export MARKET_DATA_USE_MOCK_DATA=true

# 실제 한국투자증권 API 사용
export MARKET_DATA_PROVIDER=korea-investment
export MARKET_DATA_USE_MOCK_DATA=false
export KOREA_INVESTMENT_APP_KEY=your_app_key
export KOREA_INVESTMENT_APP_SECRET=your_app_secret
export KOREA_INVESTMENT_SERVER_TYPE=1  # 모의투자
```

## MCP를 활용한 개발

### 한국투자 코딩도우미 MCP 소개

한국투자증권이 제공하는 **KIS Code Assistant MCP**를 활용하면 자연어로 한국투자증권 API를 검색하고, 예제 코드를 자동으로 생성받을 수 있습니다.

### MCP 사용 방법

1. **MCP 설치**: Cursor에서 [KIS Code Assistant MCP](https://smithery.ai/server/@KISOpenAPI/kis-code-assistant-mcp) 설치
2. **자연어 질문**: "한국투자증권 주식 현재가 조회 API 코드 보여줘" 같은 질문
3. **예제 코드 확인**: MCP가 제공한 Python 예제 코드 확인
4. **Java 프로젝트 통합**: 제공된 정보를 바탕으로 Java 코드 작성

### MCP 활용 예시

#### 예시 1: API 검색
```
질문: "한국투자증권 주식 매수 주문 API 코드 보여줘"
응답: 
- API 엔드포인트: /uapi/domestic-stock/v1/trading/order-cash
- TR ID: TTTC0012U (매수, 실거래) / VTTC0012U (매수, 모의투자), TTTC0011U (매도, 실거래) / VTTC0011U (매도, 모의투자)
- 필수 파라미터: CANO, ACNT_PRDT_CD, PDNO, ORD_DVSN, ORD_QTY, ORD_UNPR, EXCG_ID_DVSN_CD
- Hashkey 필요 여부: 예
```

#### 예시 2: Java 코드 통합
MCP가 제공한 정보를 바탕으로 기존 Java 프로젝트 구조에 맞게 구현:

```java
// MCP가 제공한 정보 활용
public Mono<OrderResponse> placeBuyOrder(String userId, String accountNo, 
                                         String symbol, int quantity, BigDecimal price) {
    // 1. 사용자 API 키 조회 (DB에서 암호화된 키)
    UserApiKey userApiKey = userApiKeyRepository
        .findByUserIdAndBrokerType(userId, BrokerType.KOREA_INVESTMENT)
        .orElseThrow();
    
    // 2. 토큰 조회
    String accessToken = tokenService.getAccessToken(userId);
    
    // 3. 공통 헤더 생성
    HttpHeaders headers = KoreaInvestmentRequestBuilder.createCommonHeaders(
        accessToken, 
        encryptionUtil.decrypt(userApiKey.getAppKeyEncrypted()),
        encryptionUtil.decrypt(userApiKey.getAppSecretEncrypted()),
        "TTTC0012U"  // MCP가 제공한 TR ID (매수 주문)
    );
    
    // 4. 요청 바디 생성 (MCP가 제공한 파라미터 활용)
    Map<String, String> requestBody = KoreaInvestmentRequestBuilder
        .createAccountRequestBody(accountNo, Map.of(
            "PDNO", symbol,
            "ORD_DVSN", "00",  // 지정가
            "ORD_QTY", String.valueOf(quantity),
            "ORD_UNPR", price.toString(),
            "EXCG_ID_DVSN_CD", "KRX"  // 거래소ID구분코드
        ));
    
    // 5. Hashkey 생성 (MCP가 Hashkey 필요하다고 알려줌)
    String hashkey = hashkeyUtil.generateHashkey(requestBody, 
        encryptionUtil.decrypt(userApiKey.getAppSecretEncrypted()));
    headers.set("hashkey", hashkey);
    
    // 6. API 호출
    return webClient.post()
        .uri("/uapi/domestic-stock/v1/trading/order-cash")
        .headers(h -> h.addAll(headers))
        .bodyValue(requestBody)
        .retrieve()
        .bodyToMono(OrderResponse.class);
}
```

### MCP 활용 시 주의사항

1. **TR ID 확인**: 실거래와 모의투자에서 다른 TR ID 사용
   - 매수 주문: 실거래 `TTTC0012U`, 모의투자 `VTTC0012U`
   - 매도 주문: 실거래 `TTTC0011U`, 모의투자 `VTTC0011U`

2. **Hashkey 필요 여부**: 주문 API는 Hashkey가 필요할 수 있음
   - MCP 응답에서 확인
   - `KoreaInvestmentHashkeyUtil` 활용

3. **필수 파라미터**: MCP가 제공한 필수 파라미터(Required='Y') 모두 포함

4. **기존 구조 활용**: MCP가 제공한 정보를 바탕으로 기존 Java 프로젝트 구조에 맞게 통합
   - `KoreaInvestmentRequestBuilder`: 공통 헤더/바디 생성
   - `KoreaInvestmentTokenService`: 토큰 관리
   - Rate Limiter: API 호출 제한

5. **주문 API·계좌별 서버 타입**: 주문 실행 시 계좌번호(accountNo)에 해당하는 서버 타입(모의/실전)으로 API 키·토큰을 사용한다. `KoreaInvestmentOrderClient`는 `UserAccount`에서 accountNo → serverType을 조회한 뒤 `getUserApiKeyForAccount(userId, accountNo)`, `getAccessToken(userId, serverType)`으로 호출한다.

## 국내주식 주문 API (MCP 검증 반영)

- **엔드포인트**: `/uapi/domestic-stock/v1/trading/order-cash`
- **요청 방식**: **POST** + JSON body (key 대문자: CANO, ACNT_PRDT_CD, PDNO, ORD_DVSN, ORD_QTY, ORD_UNPR, EXCG_ID_DVSN_CD 등)
- **TR ID**: 실거래 매수 `TTTC0012U` / 매도 `TTTC0011U`, 모의투자 매수 `VTTC0012U` / 매도 `VTTC0011U`
- **Hashkey**: 필수. `KoreaInvestmentHashkeyUtil.generateHashkey(requestBody, appSecret)` 사용
- **사용 클래스**: `KoreaInvestmentOrderClient.placeBuyOrder`, `placeSellOrder`

## 해외주식 주문 API (미국)

- **엔드포인트**: `/uapi/overseas-stock/v1/trading/order`
- **요청 방식**: **POST** + JSON body
- **TR ID (미국 NASD/NYSE/AMEX)**: 실거래 매수 `TTTT1002U` / 매도 `TTTT1006U`, 모의투자 매수 `VTTT1002U` / 매도 `VTTT1006U`
- **필수 파라미터**: CANO, ACNT_PRDT_CD, OVRS_EXCG_CD(미국: NASD), PDNO(티커 예: AAPL), ORD_QTY, OVRS_ORD_UNPR(지정가 단가, 시장가 시 "0"), CTAC_TLNO(공란 가능), MGCO_APTM_ODNO(공란 가능), SLL_TYPE(매도 시 "00"), ORD_SVR_DVSN_CD("0"), ORD_DVSN("00": 지정가, 모의투자는 00만 가능)
- **Hashkey**: 필수
- **사용 클래스**: `KoreaInvestmentOrderClient.placeOverseasBuyOrder`, `placeOverseasSellOrder`
- **시장 분기**: `OrderRequestDto.market`이 "US"일 때 해외 주문 API 호출, 그 외 국내 주문 API 호출

### 자세한 가이드

MCP 통합에 대한 자세한 내용은 [MCP 통합 가이드](../08-setup-guides/06-mcp-integration-guide.md)를 참고하세요.

## 참고 자료

- 한국투자증권 Open API 포털: https://apiportal.koreainvestment.com/
- API 개발가이드
- REST API 명세서
- [MCP 통합 가이드](../08-setup-guides/06-mcp-integration-guide.md)
