# 시장 데이터 API 가이드

## 개요

iTick API를 사용하여 시장 데이터를 조회합니다.
확장성을 위해 인터페이스 기반 설계로 구현되어 있어, 향후 다른 제공자를 쉽게 추가할 수 있습니다.

## 아키텍처

### 인터페이스 기반 설계

```
MarketDataClient (인터페이스)
└── ITickMarketDataClient (iTick 구현체)
```

### 주요 변경사항

1. **MarketDataClient 인터페이스**
   - `getIndicator()`: 단일 지표 조회
   - `getBulkIndicators()`: 여러 지표 일괄 조회
   - `getProviderName()`: 제공자 이름 반환

2. **IndicatorResponse (공통 DTO)**
   - 다양한 제공자의 응답을 통일된 형태로 변환
   - RSI, MACD, EMA, VWAP, Bollinger Bands, ATR 등 지원

3. **MarketDataProperties (설정)**
   - `provider`: 사용할 제공자 (itick)
   - `apiKey`: API 키
   - `baseUrl`: Base URL
   - `timeout`: 타임아웃
   - `useMockData`: 모의 데이터 사용 여부
   - `market`: 시장 선택 (us, hk, cn 등)

## 설정 방법

### application.yml

```yaml
investment:
  market-data:
    provider: itick
    api-key: ${MARKET_DATA_API_KEY:}
    base-url: ${MARKET_DATA_BASE_URL:https://open.itick.org}
    timeout: ${MARKET_DATA_TIMEOUT:30000}
    use-mock-data: ${MARKET_DATA_USE_MOCK_DATA:false}
    market: us  # us, hk, cn 등
```

### 환경 변수

```bash
# 제공자 선택
export MARKET_DATA_PROVIDER=itick

# API 키
export MARKET_DATA_API_KEY=your_api_key

# Base URL
export MARKET_DATA_BASE_URL=https://open.itick.org

# 타임아웃
export MARKET_DATA_TIMEOUT=30000

# 모의 데이터 사용
export MARKET_DATA_USE_MOCK_DATA=false

# iTick 시장 선택
export MARKET_DATA_MARKET=us
```

## iTick API 특징

- **Base URL**: `https://open.itick.org`
- **인증**: Bearer Token (API Key)
- **엔드포인트**: `/api/v1/stocks/{market}/kline`
- **지원 시장**: us, hk, cn 등
- **특징**: Kline 데이터 제공, 클라이언트 측에서 지표 계산 필요

## 새로운 제공자 추가 방법

1. **MarketDataClient 인터페이스 구현**

```java
@Component
@ConditionalOnProperty(name = "investment.market-data.provider", havingValue = "newprovider")
public class NewProviderMarketDataClient implements MarketDataClient {
    
    @Override
    public Mono<IndicatorResponse> getIndicator(String indicator, String symbol, String interval) {
        // 구현
    }
    
    @Override
    public Mono<Map<String, IndicatorResponse>> getBulkIndicators(
            String symbol, String interval, String... indicators) {
        // 구현
    }
    
    @Override
    public String getProviderName() {
        return "NewProvider";
    }
}
```

2. **설정에 provider 추가**

```yaml
investment:
  market-data:
    provider: newprovider
```

## 사용 예시

```java
@Autowired
private MarketDataClient marketDataClient;

public Mono<StockAnalysisDto> analyzeStock(String symbol, String interval) {
    return marketDataClient.getBulkIndicators(symbol, interval, "rsi", "macd")
            .map(indicators -> {
                // 처리
            });
}
```

## 주의사항

1. **iTick API 구현**
   - 현재 iTick 클라이언트는 기본 구조만 제공됩니다.
   - 실제 API 문서를 참고하여 Kline 데이터 파싱 및 지표 계산 로직을 구현해야 합니다.
   - https://docs.itick.org/ 참고

## 테스트

```bash
# iTick 사용
export MARKET_DATA_PROVIDER=itick
export MARKET_DATA_API_KEY=your_itick_api_key

# 모의 데이터 사용
export MARKET_DATA_USE_MOCK_DATA=true
```

## 향후 개선 사항

1. **iTick API 완전 구현**
   - Kline 데이터 파싱
   - 기술적 지표 계산 (RSI, MACD, EMA 등)
   - Real-Time Quote 지원

2. **캐싱 및 성능 최적화**
   - 응답 캐싱
   - 배치 요청 최적화
