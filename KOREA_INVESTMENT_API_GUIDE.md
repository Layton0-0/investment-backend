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

// 종목명으로도 사용 가능 (KiwoomCodeConverter가 자동 변환)
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
- ✅ 종목 코드 변환 (KiwoomCodeConverter 활용)
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

## 주의사항

1. **API 키 발급**: 한국투자증권 홈페이지에서 App Key와 App Secret을 발급받아야 합니다.
2. **Rate Limit**: API 호출 제한이 있으므로 적절한 지연 시간을 두고 호출해야 합니다.
3. **모의투자 권장**: 개발/테스트 시 모의투자 서버 사용을 권장합니다.
4. **토큰 관리**: Access Token은 자동으로 캐시되며, 만료 시 자동 갱신됩니다.

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
