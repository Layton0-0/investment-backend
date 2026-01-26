# 트레이딩 포트폴리오 API

## 1. 트레이딩 포트폴리오 조회

### 1.1 요청
```
GET /api/v1/trading-portfolios/{date}
```

**Path Parameters**:
- `date` (String, required): 거래일 (yyyy-MM-dd 형식)

**예시**:
```bash
curl -X GET "http://localhost:8080/api/v1/trading-portfolios/2026-01-26"
```

### 1.2 응답

**성공 (200 OK)**:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "tradingDate": "2026-01-26",
  "marketSummary": "📌 시장 요약:\n현재 시장은 혼조세를 보이고 있습니다...",
  "topSector1": "반도체 (AI 반도체 수요 증가, 공급 부족 지속)",
  "topSector2": "에너지 (원유 가격 상승, 재생에너지 정책 지원)",
  "topSector3": "방산 (지정학적 리스크, 국방 예산 증가)",
  "riskManagementStrategy": "📌 리스크 관리 전략:\n- 포지션 사이즈: 종목당 최대 $10,000...",
  "positionSize": 10000.00,
  "items": [
    {
      "id": "660e8400-e29b-41d4-a716-446655440001",
      "symbol": "NVDA",
      "name": "NVIDIA Corporation",
      "entryPriceMin": 480.00,
      "entryPriceMax": 490.00,
      "stopLossPrice": 470.00,
      "targetPrice1": 510.00,
      "targetPrice2": 530.00,
      "expectedReturnRate": 5.20,
      "riskRewardRatio": 2.50,
      "technicalBasis": "RSI 중립 구간, MACD 상승 전환, 이평선 정배열",
      "supplyDemandBasis": "거래량 150% 급증, VWAP 위에서 거래",
      "catalystFactor": "실적 발표 예정(다음주), AI 관련 호재",
      "buyTime": "10:00:00",
      "sellTime": "12:00:00",
      "investmentAmount": 10000.00,
      "expectedProfit": 520.00,
      "ranking": 1
    }
  ]
}
```

**에러 (404 Not Found)**:
```json
{
  "code": "NOT_FOUND",
  "message": "트레이딩 포트폴리오를 찾을 수 없습니다: 2026-01-26",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-01-26T10:00:00"
}
```

## 2. DTO 정의

### 2.1 TradingPortfolioDto
```java
{
  "id": String,                      // 포트폴리오 ID
  "tradingDate": LocalDate,          // 거래일
  "marketSummary": String,            // 시장 요약
  "topSector1": String,              // 유망 섹터 1
  "topSector2": String,              // 유망 섹터 2
  "topSector3": String,              // 유망 섹터 3
  "riskManagementStrategy": String,   // 리스크 관리 전략
  "positionSize": BigDecimal,        // 포지션 사이즈
  "items": List<TradingPortfolioItemDto> // 종목 목록
}
```

### 2.2 TradingPortfolioItemDto
```java
{
  "id": String,                      // 종목 ID
  "symbol": String,                  // 종목 코드
  "name": String,                    // 종목명
  "entryPriceMin": BigDecimal,       // 최소 진입가
  "entryPriceMax": BigDecimal,       // 최대 진입가
  "stopLossPrice": BigDecimal,       // 손절가
  "targetPrice1": BigDecimal,        // 목표가 1
  "targetPrice2": BigDecimal,        // 목표가 2
  "expectedReturnRate": BigDecimal,   // 기대 수익률 (%)
  "riskRewardRatio": BigDecimal,     // 리스크/리워드 비율
  "technicalBasis": String,          // 기술적 근거
  "supplyDemandBasis": String,        // 수급 근거
  "catalystFactor": String,           // 촉매 요인
  "buyTime": LocalTime,              // 매수 시간
  "sellTime": LocalTime,             // 매도 시간
  "investmentAmount": BigDecimal,     // 투자금액
  "expectedProfit": BigDecimal,       // 예상 수익
  "ranking": Integer                  // 순위
}
```

## 3. 자동 생성

트레이딩 포트폴리오는 매일 오전 9시 (한국 시간)에 자동으로 생성됩니다.

- **스케줄러**: `TradingPortfolioScheduler`
- **서비스**: `TradingPortfolioService`
- **전략**: `ShortTermTradingStrategyService`

## 4. 비즈니스 규칙

1. 거래일당 포트폴리오는 1개만 존재
2. 종목은 최대 5개까지 포함
3. 종목은 순위(ranking)로 정렬됨

## 5. 에러 코드

- `NOT_FOUND`: 트레이딩 포트폴리오를 찾을 수 없음
