# 한국투자증권 API 가이드

## 개요

한국투자증권 Open API를 사용하여 국내 주식 시장 데이터를 조회할 수 있습니다.

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
   - 토큰 유효기간: 24시간
   - 자동 갱신 지원

2. **API 호출**
   - 헤더에 `authorization: Bearer {access_token}` 포함
   - `appkey`, `appsecret`, `tr_id` 헤더 포함

## 차트 데이터 조회

한국투자증권 API는 차트 데이터를 제공하며, 클라이언트에서 기술적 지표를 계산합니다.

- **TR ID**: `FHKST03010100` (주식현재가 일봉차트 조회)
- **엔드포인트**: `/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice`
- **지원 기간**: 일봉, 주봉, 월봉

## 구현된 기능

### 현재 구현된 기능
- ✅ OAuth 2.0 인증 (Access Token 발급 및 자동 갱신)
- ✅ 차트 데이터 조회
- ✅ 기술적 지표 계산 (RSI, MACD, EMA, Bollinger Bands, ATR, VWAP)
- ✅ 종목 코드 변환 (StockCodeConverter 활용)
- ✅ 모의 데이터 지원 (개발/테스트용)
- ✅ 실거래/모의투자 서버 선택

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

## 주의사항

1. **API 키 발급**: 한국투자증권 홈페이지에서 App Key와 App Secret을 발급받아야 합니다.
2. **Rate Limit 준수**: 시스템에서 자동으로 Rate Limiter를 적용하지만, 대량 호출 시 주의가 필요합니다.
3. **모의투자 권장**: 개발/테스트 시 모의투자 서버 사용을 권장합니다.
4. **토큰 관리**: Access Token은 자동으로 캐시되며, 만료 시 자동 갱신됩니다.
5. **계좌 단위 제한**: 유량 제한은 계좌(앱키) 단위로 적용되므로, 여러 계좌를 사용하는 경우 각각 별도 제한이 적용됩니다.
6. **TLS 버전**: 2025.12.12(금) 이후 TLS 1.2 이상 필수 (현재 Java 17 사용으로 자동 준수)
7. **WebSocket 정책 준수**: 웹소켓 사용 시 반드시 이용 순서, 연결/종료 간격, 구독 등록 간격을 준수해야 합니다. 미준수 시 자동 차단될 수 있습니다.

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

## 참고 자료

- 한국투자증권 Open API 포털: https://apiportal.koreainvestment.com/
- API 개발가이드
- REST API 명세서
