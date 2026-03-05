# 키움증권 Open API 가이드

## 개요

키움증권 Open API를 사용하여 국내 주식 시장 데이터를 조회할 수 있습니다.

## 아키텍처

```
MarketDataClient (인터페이스)
└── KiwoomMarketDataClient (키움증권 구현체)
```

## 설정 방법

### application.yml

```yaml
investment:
  market-data:
    provider: kiwoom
    kiwoom:
      account-no: ${KIWOOM_ACCOUNT_NO:}
      password: ${KIWOOM_PASSWORD:}
      server-type: ${KIWOOM_SERVER_TYPE:1}  # 1: 모의투자, 0: 실거래
      host: ${KIWOOM_HOST:localhost}
      port: ${KIWOOM_PORT:5000}
      auto-login: ${KIWOOM_AUTO_LOGIN:false}
```

### 환경 변수

```bash
# 제공자 선택
export MARKET_DATA_PROVIDER=kiwoom

# 키움증권 계좌 정보
export KIWOOM_ACCOUNT_NO=your_account_no
export KIWOOM_PASSWORD=your_password

# 서버 타입 (1: 모의투자, 0: 실거래)
export KIWOOM_SERVER_TYPE=1

# 모의 데이터 사용 (개발/테스트용)
export MARKET_DATA_USE_MOCK_DATA=true
```

## 키움증권 API 특징

- **종목 코드**: 6자리 숫자 코드 사용
  - 코스피: 000000~099999
  - 코스닥: 100000~999999
- **지원 지표**: RSI, MACD, EMA, Bollinger Bands, ATR, VWAP
- **Rate Limit**: 초당 5회 API 호출 제한
- **플랫폼**: Windows COM/ActiveX 기반

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

## 구현 상태

### 현재 구현된 기능
- ✅ MarketDataClient 인터페이스 구현
- ✅ 종목 코드 변환 유틸리티 (KiwoomCodeConverter)
- ✅ 설정 Properties 구조
- ✅ 모의 데이터 지원 (개발/테스트용)

### 추후 구현 필요
- ⚠️ **COM 인터페이스 호출**: 키움증권 API는 Windows COM/ActiveX 기반이므로, Java에서 직접 사용하려면 다음 중 하나의 방법이 필요합니다:
  
  1. **JNA를 사용한 COM 인터페이스 호출**
     - JNA 라이브러리를 사용하여 COM 인터페이스 직접 호출
     - 복잡하지만 가장 직접적인 방법
     
  2. **JNI를 통한 C++ 래퍼 호출**
     - C++로 COM 인터페이스를 래핑하고 JNI로 호출
     - 성능이 좋지만 빌드/배포가 복잡
     
  3. **REST API 래퍼 서버 사용 (권장)**
     - 별도의 Windows 서버에서 COM 인터페이스를 호출하고 REST API로 제공
     - Java 애플리케이션은 REST API를 통해 데이터 조회
     - 가장 실용적이고 유지보수가 쉬운 방법

## 실제 COM 호출 구현 예시

### 방법 1: REST API 래퍼 서버 (권장)

별도의 Windows 서버에서 키움증권 API를 래핑한 REST API 서버를 구축:

```java
// KiwoomMarketDataClient에서 REST API 호출
return webClient.get()
    .uri("http://kiwoom-wrapper:8080/api/v1/indicator")
    .queryParam("symbol", kiwoomCode)
    .queryParam("indicator", indicator)
    .retrieve()
    .bodyToMono(IndicatorResponse.class);
```

### 방법 2: JNA를 사용한 COM 호출

```java
// JNA 라이브러리 사용 예시 (실제 구현 필요)
import com.sun.jna.platform.win32.COM.COMInvoke;

// COM 인터페이스 호출
// 실제 구현은 키움증권 API 문서 참조 필요
```

## 주의사항

1. **Windows 전용**: 키움증권 Open API는 Windows에서만 동작합니다.
2. **COM 인터페이스**: 실제 구현 시 COM 인터페이스 호출이 필요합니다.
3. **Rate Limit**: 초당 5회 호출 제한을 준수해야 합니다.
4. **모의투자 권장**: 개발/테스트 시 모의투자 서버 사용을 권장합니다.

## 테스트

```bash
# 모의 데이터로 테스트
export MARKET_DATA_PROVIDER=kiwoom
export MARKET_DATA_USE_MOCK_DATA=true

# 실제 키움증권 API 사용 (COM 인터페이스 구현 후)
export MARKET_DATA_PROVIDER=kiwoom
export MARKET_DATA_USE_MOCK_DATA=false
export KIWOOM_ACCOUNT_NO=your_account_no
export KIWOOM_PASSWORD=your_password
export KIWOOM_SERVER_TYPE=1  # 모의투자
```

## 참고 자료

- 키움증권 Open API 문서: https://www.kiwoom.com/h/customer/download/VOpenApiInfoView
- 키움증권 Open API+ 개발가이드
- COM 인터페이스 호출 방법 (JNA/JNI)
