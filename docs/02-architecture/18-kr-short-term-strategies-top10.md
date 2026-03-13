# 한국주식 단타용 퀀트 전략 TOP 10

**목적**: 한국(KRX) 시장 단타(단기) 전략 후보를 실전 퀀트 수준으로 정리하고, 백테스트·검증 포인트를 명시한다.  
**참조**: [00-strategy-registry.md](00-strategy-registry.md) (전략·수식·청산 규칙 통합).

---

## 1. 변동성 돌파 (Volatility Breakout)

| 항목 | 내용 |
|------|------|
| **개요** | 당일 시가 + (전일 고가−전일 저가)×k 돌파 시 매수, 당일 청산 또는 -3% 트레일링 스탑 |
| **진입** | 목표가 = 시가 + Range×k, Range = 전일 High − Low, k ∈ [0.3, 0.7] (동적 k: 최근 5일/20일 변동폭 비율로 조정) |
| **청산** | 단기: -3% 트레일링 스탑, Time-Cut(보유일수), RSI≥70 익절 |
| **백테스트 포인트** | PIT 준수, 수정주가 일봉, 슬리피지·수수료 반영, MDD·CAGR·Sharpe·승률·손익비 |
| **코드 참조** | 00-strategy-registry §2.3, §3.2.1; `FactorCalculationService.getVolatilityBreakoutK` |

---

## 2. 거래량 급증 + 가격 돌파 (Volume Spike + Breakout)

| 항목 | 내용 |
|------|------|
| **개요** | 전일 대비 거래량 N배 이상 + 당일 가격 돌파 동시 충족 시 매수 |
| **진입** | volume_today / volume_avg_5d ≥ threshold (예: 2.0), and 가격 > 전일 고가 또는 시가+Range×k |
| **청산** | 트레일링 스탑, Time-Cut, 또는 목표 수익률 도달 |
| **백테스트 포인트** | 거래량은 PIT(당일 종료 전에는 당일 거래량 미사용), 유동성 필터(거래대금 하한) |
| **코드 참조** | 00-strategy-registry §3.2, 유동성·변동성 돌파 조합 |

---

## 3. 5일 연속 수급 (Smart Money Intensity 연속)

| 항목 | 내용 |
|------|------|
| **개요** | 순매수(기관+외국인)가 5일 연속인 종목에 모멘텀·돌파 조건 결합 |
| **진입** | TB_ORDER_FLOW 등 수급 메타 5일 연속 순매수 + 시그널(모멘텀/돌파) |
| **청산** | -5% 고정 손절, 전저점 이탈, RSI≥70 익절 (00-strategy-registry 월스트리트 정렬 KR) |
| **백테스트 포인트** | 수급 데이터 가용 시점(PIT), 생존자 편향 주의 |
| **코드 참조** | 00-strategy-registry §3.2.1, UniverseFilterService 수급 필터 |

---

## 4. 역발상 RSI (Contrarian RSI)

| 항목 | 내용 |
|------|------|
| **개요** | RSI < 40(과매도) 구간에서 반등 시그널로 매수 |
| **진입** | RSI(14) < threshold_low (예: 40), 이격도 또는 MACD 전환 부가 |
| **청산** | RSI > 65~70 익절, -3% 손절, Time-Cut |
| **백테스트 포인트** | RSI 계산 PIT, 구간별(상승/하락장) 성과 분리 |
| **코드 참조** | 00-strategy-registry §3.2.1 contrarian-rsi-threshold |

---

## 5. 시초가 유동성 필터 + 변동성 돌파

| 항목 | 내용 |
|------|------|
| **개요** | 시초가(장 시작) 구간 거래대금이 최소 N억 이상인 종목만 변동성 돌파 적용 |
| **진입** | 시초가 유동성 ≥ liquidity_min_trd_val_opening (예: 300억), 이후 돌파 시 매수 |
| **청산** | 단기 트레일링 스탑·Time-Cut |
| **백테스트 포인트** | 시초가 구간 거래대금 근사(당일 데이터 사용 시 look-ahead 주의) |
| **코드 참조** | 00-strategy-registry §자동매매 직전 점검, liquidity-min-trd-val-opening |

---

## 6. Hunter 분기 (수급 강함 vs 역발상)

| 항목 | 내용 |
|------|------|
| **개요** | Case A: 수급 강함 → RSI>60 & MACD 골든크로스 / Case B: 역발상 → RSI<40 |
| **진입** | 수급 강한 종목은 추세 추종(RSI+MACD), 약한 종목은 역발상 RSI |
| **청산** | 단기 공통: -3% 트레일링, Time-Cut, RSI 익절 |
| **백테스트 포인트** | 수급/기술지표 모두 PIT, Case A/B 비율에 따른 리스크 분산 |
| **코드 참조** | 00-strategy-registry §자동매매 직전 점검, PositionSizingService.filterSymbolsKrShortTerm |

---

## 7. P/B 필터 + 모멘텀

| 항목 | 내용 |
|------|------|
| **개요** | P/B가 [min, max] 구간인 종목만 모멘텀(또는 돌파) 시그널 적용 |
| **진입** | pb_value_min ≤ P/B ≤ pb_value_max, 모멘텀 순위 상위 또는 돌파 |
| **청산** | 단기 공통 청산 규칙 |
| **백테스트 포인트** | P/B 데이터 시점(PIT), 밸류 종목 특성(변동성·유동성) |
| **코드 참조** | 00-strategy-registry §3.2.1, factor.pb-value-min/max |

---

## 8. 전저점 이탈 손절 (Prior Low Stop)

| 항목 | 내용 |
|------|------|
| **개요** | 보유 중 최근 N일 저점 이탈 시 손절 |
| **진입** | (다른 전략과 동일) 변동성 돌파 또는 모멘텀 |
| **청산** | 당일 저가 < prior_low 시 매도 (Prior Low Stop) |
| **백테스트 포인트** | prior_low 계산 시점(매수일 기준 N일), 일봉 기준 저가 사용 |
| **코드 참조** | 00-strategy-registry §월스트리트 정렬 KR, prior-low-stop-kr-enabled |

---

## 9. 섹터 상대강도 + 개별 돌파

| 항목 | 내용 |
|------|------|
| **개요** | 해당 섹터(업종)가 상대강도 상위일 때, 개별 종목 돌파/모멘텀만 매수 |
| **진입** | Sector RS 상위 N개 업종 내 종목, 개별 시그널(돌파·거래량) |
| **청산** | 단기 공통 |
| **백테스트 포인트** | 섹터 수익률·유니버스 PIT, TB_SECTOR_RETURN·TB_SYMBOL_SECTOR |
| **코드 참조** | 00-strategy-registry §4단계 파이프라인 확장, UniverseFilterService Sector RS |

---

## 10. 뉴스/공시 시그널 결합 (DART·키워드)

| 항목 | 내용 |
|------|------|
| **개요** | 무상증자·영업이익 등 시그널 키워드 공시 발표 후, 유동성·돌파 조건 충족 시 매수 |
| **진입** | NewsItem signalRelevant (DART_SIGNAL 등) + 유니버스·돌파/모멘텀 |
| **청산** | 단기 공통 |
| **백테스트 포인트** | 공시 시점 vs 거래 시점(다음 봉), 감정/중요도 가중 시 문서화 |
| **코드 참조** | 00-strategy-registry, NewsSignalService, 13-news-collection-design |

---

## 공통 백테스트·검증 체크리스트

- **PIT·Look-ahead**: 해당 일자 종료 시점까지 가용 데이터만 사용.
- **수정주가**: 일봉·팩터·백테스트 모두 수정주가.
- **비용**: 수수료·세금·슬리피지 반영 (FrictionCost).
- **필수 메트릭**: CAGR, Sharpe, MDD, 승률, Profit factor.
- **스트레스 구간**: 2020-02~04, 2022-01~06 등 극단 구간 검증 ([backtest-stress-results.md](backtest-stress-results.md)).
- **전략 거버넌스**: 정기 백테스트 재실행, MDD/Sharpe 열화 시 중단·검토.
