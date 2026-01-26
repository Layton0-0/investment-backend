# 분석 API

## 1. 종목 분석

### 1.1 요청
```
POST /api/v1/analysis
```

**Request Body**:
```json
{
  "symbol": "005930",
  "periodDays": 30
}
```

**필드 설명**:
- `symbol` (String, required): 종목 코드 또는 종목명
- `periodDays` (Integer, optional): 분석 기간 (일), 기본값: 30

**예시**:
```bash
curl -X POST "http://localhost:8080/api/v1/analysis" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "005930",
    "periodDays": 30
  }'
```

### 1.2 응답

**성공 (200 OK)**:
```json
{
  "symbol": "005930",
  "name": "삼성전자",
  "recommendation": "BUY",
  "confidence": 0.85,
  "currentPrice": 75000.00,
  "indicators": {
    "rsi": 45.5,
    "macd": {
      "macd": 123.45,
      "signal": 100.23,
      "hist": 23.22
    },
    "ema20": 74000.00,
    "ema60": 72000.00,
    "ema120": 70000.00,
    "bollingerBands": {
      "upper": 78000.00,
      "middle": 75000.00,
      "lower": 72000.00
    },
    "atr": 2000.00,
    "vwap": 74500.00
  },
  "analysis": {
    "technical": "RSI 중립 구간, MACD 상승 전환, 이평선 정배열",
    "trend": "상승 추세",
    "support": 72000.00,
    "resistance": 78000.00
  }
}
```

## 2. DTO 정의

### 2.1 AnalysisRequestDto
```java
{
  "symbol": String,        // 종목 코드 또는 종목명 (required)
  "periodDays": Integer    // 분석 기간 (일), 기본값: 30
}
```

### 2.2 AnalysisResponseDto
```java
{
  "symbol": String,                    // 종목 코드
  "name": String,                      // 종목명
  "recommendation": Enum,              // 추천: BUY, SELL, HOLD
  "confidence": BigDecimal,             // 신뢰도 (0.0 ~ 1.0)
  "currentPrice": BigDecimal,          // 현재가
  "indicators": {                       // 기술적 지표
    "rsi": BigDecimal,                 // RSI
    "macd": {                          // MACD
      "macd": BigDecimal,
      "signal": BigDecimal,
      "hist": BigDecimal
    },
    "ema20": BigDecimal,               // 20일 EMA
    "ema60": BigDecimal,               // 60일 EMA
    "ema120": BigDecimal,              // 120일 EMA
    "bollingerBands": {                // 볼린저 밴드
      "upper": BigDecimal,
      "middle": BigDecimal,
      "lower": BigDecimal
    },
    "atr": BigDecimal,                 // ATR
    "vwap": BigDecimal                  // VWAP
  },
  "analysis": {                         // 분석 결과
    "technical": String,                // 기술적 분석
    "trend": String,                    // 추세
    "support": BigDecimal,              // 지지선
    "resistance": BigDecimal            // 저항선
  }
}
```

## 3. 추천 값

- `BUY`: 매수 추천
- `SELL`: 매도 추천
- `HOLD`: 보유 추천

## 4. 기술적 지표

### 4.1 RSI (Relative Strength Index)
- 범위: 0 ~ 100
- 과매수: 70 이상
- 과매도: 30 이하

### 4.2 MACD (Moving Average Convergence Divergence)
- MACD: MACD 라인
- Signal: 시그널 라인
- Hist: 히스토그램 (MACD - Signal)

### 4.3 EMA (Exponential Moving Average)
- EMA20: 20일 지수이동평균
- EMA60: 60일 지수이동평균
- EMA120: 120일 지수이동평균

### 4.4 Bollinger Bands
- Upper: 상단 밴드
- Middle: 중간 밴드 (이동평균)
- Lower: 하단 밴드

### 4.5 ATR (Average True Range)
- 변동성 지표
- 값이 클수록 변동성이 큼

### 4.6 VWAP (Volume Weighted Average Price)
- 거래량 가중 평균 가격

## 5. 비즈니스 규칙

1. 신뢰도가 0.7 이상일 때만 매수/매도 추천
2. 신뢰도가 0.5 미만이면 보유 추천
3. 시장 데이터 API 실패 시 에러 반환
