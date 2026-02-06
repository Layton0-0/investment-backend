# 전략·계산 방식 통합 레지스트리 (Strategy Registry)

**목적**: 프로젝트에서 개발·적용하는 모든 전문적인 투자 방식, 전략, 계산 방법을 한 문서에 정리하고, 전략 업데이트 시 갱신·버전 스택(실패/교훈 포함)으로 경험을 쌓으며, **나라별·분야별·기간별**로 구분해 정확도를 높인다.

**상위 문서**: [12-auto-investment-strategy.md](./12-auto-investment-strategy.md)(고수준 명세), [05-quantitative-strategy.md](./05-quantitative-strategy.md)(규칙 엔진 구조). 상세 수식·파라미터는 **본 문서를 단일 소스**로 참조한다.

---

## 1. 메타

| 항목 | 내용 |
|------|------|
| **현재 전략 문서 버전** | v1.9 |
| **최종 갱신일** | 2026-02-02 |
| **코드 참조** | `factor.service.*`, `factor.execution.ExitRuleService`, `application.yml` (investment.factor, investment.fees, investment.pipeline) |

---

## 2. 공통

### 2.1 수익·리스크 목표

- **MDD(최대 낙폭)**: -15% 이내 통제
- **목표 수익률**: 연평균 **CAGR 30% 이상** (초과 수익 모델)
- **생명선**: 속도(Latency)와 정확도(Accuracy)

### 2.2 켈리 공식 (Kelly Criterion) — Half-Kelly

- **수식**: \( f^* = \frac{bp - q}{b} \), \( q = 1 - p \)
- **적용**: 승률 60% 이상 **그리고** 손익비 2:1 이상 구간에서만 비중 투입. 산출 \( f^* \) 의 **50%만** 적용 (Half-Kelly).
- **초기 운용**: `kelly-enabled: false` 시 **고정 자산 비율**만 사용(1종목당 `kelly-fixed-allocation-pct` % 상한). 데이터 축적 전·검증 전에는 켈리 비활성 권장.
- **설정**: `investment.factor.kelly-enabled` (기본 false), `investment.factor.kelly-fixed-allocation-pct` (기본 2), `investment.factor.kelly-p` (기본 0.6), `investment.factor.kelly-b` (기본 2.0)

### 2.3 변동성 돌파 (Volatility Breakout)

- **수식**: \( \text{Target Price} = \text{Open} + (\text{Range} \times k) \), Range = 전일 고가 − 전일 저가
- **설정**: `investment.factor.volatility-breakout-k` (기본 0.5), `volatility-breakout-k-dynamic` (한국장 k 동적 조정), `volatility-breakout-k-min` / `volatility-breakout-k-max` (0.3 / 0.7)

### 2.4 ATR 기반 포지션 사이징

- **수식**: \( \text{Position Size} = \frac{\text{Total Capital} \times \text{positionRiskPct}}{\text{Entry Price} - \text{Stop Loss}} \), Stop Loss = Entry − ATR × multiplier
- **설정**: `investment.factor.position-risk-pct` (기본 0.01 = 1%), ATR 14일, multiplier 2.0 (코드 상수)

### 2.5 변동성 역가중 (Inverse Volatility Weighting)

- 비중 ∝ 1/σ (σ: 해당 종목 역사적 변동성). 변동성 큰 종목은 비중 축소.

### 2.6 AI/LSTM 활용 방침

- **AI 예측(LSTM 등)**: 분석 정보 제공용. 목표가·신뢰도·방향은 참고 지표로만 사용.
- **최종 매매 결정**: 규칙 엔진(4단계 파이프라인·시그널·청산 규칙) 유지. AI 출력만으로 주문 실행하지 않음.

### 2.7 용어 정의

- **시장 레짐(Market Regime)**, **VaR/CVaR**, **Sharpe/Sortino/Calmar**, **R:R**, **MDD**, **CAGR**, **PEG**, **Rule of 40**, **듀얼 모멘텀**, **Smart Money Intensity**, **Disparity**, **VAA** — 전략·백테스트 문서에서 동일 정의 사용.

### 2.8 백테스트 메트릭

- **MDD**: 최대 낙폭(%). 수익 곡선의 peak 대비 최대 하락률.
- **CAGR**: 연평균 복합 수익률(%). (최종자산/초기자산)^(1/연수) - 1.
- **Sharpe**: 일수익률의 평균/표준편차 × √252 (무위험 수익률 0 가정).
- **Sortino**: 일수익률의 평균/하방편차 × √252.
- **Calmar**: CAGR / |MDD|.
- **승률(p)**: 승리 거래 수 / 전체 거래 수. Half-Kelly 입력.
- **손익비(b)**: 평균 이익 / |평균 손실|. Half-Kelly 입력.

### 2.9 리스크 게이트·일일 손실 한도 (전문 투자자 흐름 P0)

- **리스크 게이트**: 파이프라인 실행 전 시장 레짐·VIX 확인. `investment.risk.regime-gate-enabled`, `vix-threshold`, `reduce-size-on-high-vol-pct`. 고변동성 시 신규 매수 비중 축소. `RiskGateService`, `MacroEconomicStrategyEngine` 연동. **VIX·거시 지표**: `MacroIndicatorProvider`(설정 URL GET JSON 예: `{"vix": 18.5}`)·`DefaultMacroIndicatorProvider`, `PipelineExecutionScheduler`에서 `getCurrentIndicators()` → `evaluateWithIndicators`/`evaluate(vix)`. `investment.risk.macro-indicator-url`(선택).
- **일일 손실 한도**: 당일 시초 평가액 대비 손실이 `daily-loss-limit-pct` 초과 시 당일 신규 매수 중단. `DailyLossLimitService`, `PipelineExecutionScheduler` 실행 전 `isNewBuyAllowed` 검사.
- **설정**: `investment.risk.*` (application.yml).

### 2.9.1 Pre-Trade 컴플라이언스 (Phase 2)

주문 직전 검사(`ComplianceEngine.preTradeCheck`). `OrderService.executeOrderInternal` 맨 앞에서 호출.

- **Kill Switch**: `TB_TRADING_HALT.halt_all_orders=true` 시 모든 주문 거부. API: GET/PUT `/api/v1/system/kill-switch`. Ops 역할만 설정 가능.
- **단일 종목 비중 상한**: 주문 후 해당 종목 비중 > 10%가 되면 거부. 계좌 평가총액·포지션 평가금액 기반.
- **MDD 게이트**: 계좌별 피크(`TB_PORTFOLIO_PEAK`) 대비 현재 평가액으로 MDD 계산. MDD > 15% 시 **신규 매수만** 차단(매도 허용).
- **구현**: `PreTradeComplianceEngine`, `TradingHaltService`, `PortfolioPeakService`. 스텁 사용 시 `investment.compliance.use-stub=true`.

### 2.9.2 TaxAwareOptimizer (Phase 2)

- **역할**: 전략에서 나온 원시 비중에 왕복 마찰 비용(수수료·세금·슬리피지) 반영 후 재정규화.
- **KR**: `FrictionCostProperties.korea.stock` (commission×2 + tax + slippage×2).
- **US**: `FrictionCostProperties.usa.stock` (commission×2 + secFee + slippage×2).
- **구현**: `TaxAwareOptimizerImpl`. 비용을 상쇄하지 못하는 비중은 0으로 두고 나머지 정규화.

### 2.9.3 Rebalancer (Phase 2)

- **역할**: 현재 보유(잔고·포지션) vs 목표 비중 차이로 매수/매도 리스트 생성.
- **입력**: 계좌번호, 시장(KR/US), 목표 비중, 평가총액, userId(잔고 조회용).
- **출력**: `RebalanceItem(symbol, side=BUY|SELL, quantity, notional)`.
- **구현**: `RebalancerImpl` — `AccountService.getBalanceAndPositionsWithUserId`로 포지션 조회 후 시장별 필터, 목표와 차이 산출.

### 2.10 Friction cost (마찰 비용)

백테스트·로보 리밸런싱 시 **수수료·세금·슬리피지**를 반영해 실전에 가까운 PnL을 산출한다. 한국투자증권(KIS) 실전 수수료 체계 기준.

- **설정 경로**: `investment.fees` (application.yml). `FrictionCostProperties` 바인딩.
- **한국(KR) 주식**: 위탁수수료(commission) + 증권거래세(매도 시 tax) + 슬리피지. 왕복 비용 ≈ 0.194% + 슬리피지.
- **한국(KR) ETF**: 위탁수수료 + 슬리피지. 거래세 면제(tax=0).
- **미국(US) 주식/ETF**: 위탁수수료 + SEC Fee(매도) + 슬리피지 + TAF(매도 시 주당 USD). 환전 스프레드는 로보 백테스트에서 `usa.currency.exchange-rate-spread`로 반영.
- **백테스트 반영**: `BacktestService`는 매수 시 `cost + feeBuy`, 매도 시 `exitValue - feeSell - taf`, PnL = (exitValue - cost) - totalFriction. `BacktestTradeDto.totalFrictionCost`로 거래별 마찰 비용 노출.
- **로보 백테스트**: 요청에 `commPct`/`slipPct`가 없으면 `FrictionCostProperties` 기반 round-trip 비율(2×commission + secFee + 2×slippage + 2×exchangeRateSpread) 및 TAF(매도 수량×tafPerShareUsd) 적용. 있으면 기존 commPct/slipPct로 하위 호환.

---

## 3. 나라별 (KR / US)

### 3.1 유니버스 필터링 (Universe Selection)

| 구분 | 알고리즘 | 수식/로직 | 설정/구현 |
|------|----------|-----------|-----------|
| **공통** | Liquidity Cut-off | 거래대금 ≥ 최소 거래대금 | `investment.factor.liquidity-min-trd-val` (기본 10억 원), `UniverseFilterService` |
| **한국(KR) 시초가** | Liquidity Cut-off (Opening) | 시초가/변동성 돌파 시 거래대금 ≥ 300억 원 | `investment.factor.liquidity-min-trd-val-opening` (기본 300억), KR 단기 파이프라인에서 적용 (`PositionSizingService`) |
| **한국(KR)** | Sector Relative Strength | 시장 대비 강한 주도 업종 내 종목만 | TB_SECTOR_RETURN·TB_SYMBOL_SECTOR 데이터 수집 후 적용. `sector-rs-top-n` (상위 N개 업종). 데이터 없으면 유동성만. |
| **미국(US)** | Post-Earnings Drift | 어닝 서프라이즈 강도 상위 20% | TB_EARNINGS_SURPRISE 데이터 수집 후 적용. `earnings-surprise-lookback-days`, `earnings-surprise-top-pct`. 데이터 없으면 유동성만. |

### 3.2 시그널 생성 (Signal Generation)

| 구분 | 알고리즘 | 수식/로직 | 설정/구현 |
|------|----------|-----------|-----------|
| **한국(KR)** | 이격도 (Disparity) | Disparity = (현재가 / 이동평균) × 100. 공격형: 105 돌파 후 102 눌림 매수; 역발상: 85 이하 분할 매수 | `investment.factor.disparity-ma-days` (20), `FactorCalculationService.addDisparity` |
| **한국(KR)** | 변동성 돌파 (k 동적) | Target = Open + (Range × k). 한국장 변동성에 따라 k 동적 조정 | `FactorCalculationService.addVolatilityBreakout`, `calculateDynamicK` |
| **한국(KR)** | 수급 강도·역발상 **분기(Branch)** | **Case A(모멘텀)**: 수급 강함(Smart Money 임계 초과) → RSI&gt;60 &amp; MACD 필터. **Case B(역발상)**: RSI(14)&lt;40(CONTRARIAN_RSI 점수 양수), P/B 데이터 있으면 0.8 이하. 최종 유니버스 = A ∪ B. | TB_ORDER_FLOW·TB_SIGNAL_SCORE. `PositionSizingService.filterSymbolsKrShortTerm`. 데이터 없으면 0. |
| **미국(US)** | 듀얼 모멘텀 | 기간별 수익률 가중합, 종목 모멘텀 > 시장 모멘텀 | TB_DAILY_STOCK(US) 기반. `dual-momentum-period-days`, `dual-momentum-weights`. 시장=유니버스 평균. |
| **미국(US)** | 퀄리티-성장 (PEG & Rule of 40) | PEG & Rule of 40 합산 점수 상위 10% | TB_FUNDAMENTALS 기반. PEG 점수 + Rule of 40(매출증가율+영업이익률). 데이터 없으면 0. |
| **미국(US)** | VAA 변형 | SPY/EFA/EEM/BND 중 모멘텀 스코어 최고 1개 자산 100% 배분 | 문서 정의, 코드 미연동 |

### 3.2.1 로보 어드바이저 동적 자산배분 (백테스트·실행 전 검증)

| 항목 | 수식/로직 | 설정/구현 |
|------|-----------|-----------|
| **입력** | 자산 유니버스(SPY, IEF, TLT, GLD, DBC, BIL), 벤치마크(예: SPY 60% + TLT 40%), 무위험 수익률 | `investment.backtest.robo.asset-symbols`, `benchmark-weights`, `risk-free-rate-pct` |
| **스코어링** | 모멘텀 = N개월 수익률; MA 필터 = 종가 &lt; 200일 MA 시 해당 비중만큼 현금 | `momentum-months` (12), `ma-window-days` (200), `RoboAllocationEngine.computeTargetWeights` |
| **비중** | 변동성 역가중 w_i ∝ 1/σ_i, 상위 Top N만 투자 | `top-n` (4), `volatility-lookback-days` (60) |
| **실행** | 리밸런싱: 월말/분기말; 거래 비용 = 수수료 + 슬리피지 | `rebalance-frequency` (MONTHLY/QUARTERLY), `comm-pct`, `slip-pct` |
| **메트릭** | CAGR, MDD, Sharpe, Calmar, **Turnover**(연간화) | `RoboBacktestService.run`, `RoboRebalanceScheduler` (실행 전 백테스트 통과 시만 리밸런싱) |

- **실행 전 백테스트**: 로보 리밸런싱 직전 최근 N개월 백테스트 실행. MDD·Sharpe 정책 통과 시에만 주문 실행. `pre-execution-lookback-months`, `pre-execution-max-mdd-pct`, `pre-execution-min-sharpe`.
- **리밸런싱 실행가**: `rebalanceExecutionPrice`: CLOSE(당일 종가), NEXT_OPEN(다음 거래일 시가). 백테스트 시 실전에 가까운 가정용. `RoboBacktestService`, `RoboBacktestRequest.rebalanceExecutionPrice`.

- **미국 듀얼 모멘텀(노트) 모드**: 절대 모멘텀 = SPY 12개월 수익률 vs 무위험(T-bill/BIL). SPY 12M &lt; 무위험 → 주식 비중 0(현금/TLT). 상대 모멘텀 = 섹터 ETF(XLK, XLE 등) 6개월 수익률 상위 2개. `dual-momentum-mode`, `robo-sector-etf-symbols`, `robo-momentum-months-relative`(6), `robo-top-n-sector`(2).

### 3.3 청산 (Execution & Exit)

| 구분 | 규칙 | 수식/로직 | 설정/구현 |
|------|------|-----------|-----------|
| **단기(SHORT_TERM)** | -3% Trailing Stop | 현재가 ≤ trailingHigh × (1 − 3/100) 시 매도 | `investment.pipeline.short-term-trailing-pct` (3), `ExitRuleService.evaluateShortTermTrailingStop` |
| **한국(KR) 단기/스윙** | -5% 고정 손절 | 현재가 ≤ entryPrice × (1 − 5/100) 시 매도 | `investment.pipeline.short-term-kr-stop-loss-pct` (5), `ExitRuleService.evaluateKrFixedStopLoss` |
| **한국(KR) 단기/스윙** | 전저점 이탈 손절 | 현재가 &lt; 진입 후 최저가(전저점) 이탈 시 매도 | `investment.pipeline.prior-low-stop-kr-enabled`, `ExitRuleService.evaluatePriorLowStop` (StrategyPosition priorLow 유지) |
| **한국(KR) 단기/스윙** | RSI≥70 익절 | RSI(14) ≥ 70 시 수익 실현 매도 | `investment.pipeline.rsi-exit-threshold` (70), `ExitRuleService.evaluateRsiExit` |
| **중기(MEDIUM_TERM)** | -10% 손절 | 현재가 ≤ entryPrice × (1 − 10/100) 시 매도 | `investment.pipeline.medium-term-stop-loss-pct` (10), `ExitRuleService.evaluateMediumTermStopLoss` |
| **단기(SHORT_TERM)** | Time-Cut | 매수 후 N일 내 목표 수익률 미도달 시 전량 매도. **단기 전용** (중/장기 듀얼 모멘텀은 추세 훼손만 청산). | `StrategyPosition.timeCutDays`, `targetReturnPct`, `ExitRuleEvaluator` (SHORT_TERM만 평가) |
| **장기(LONG_TERM)** | 펀더멘털 훼손 시에만 매도 | 1차 스텁(매도 시그널 없음) | 추후 펀더멘털 데이터·훼손 정의 후 구현 |
| **공통(참고)** | ATR Trailing Stop | 현재가 ≤ trailingHigh − ATR×multiplier 시 매도 | `investment.pipeline.atr-trailing-stop-multiplier` (2.0), `ExitRuleService.evaluateAtrTrailingStop` |

---

## 4. 기간별 전략 (단기 / 중기 / 장기)

| 구간 | 비중 | 목표 수익률 | 대상 | 매수 필터 | 매도/리스크 |
|------|------|-------------|------|-----------|-------------|
| **단기** | 20% | 주간 5%+ / 월간 20%+ | 미국 3배 레버리지 ETF(TQQQ, SOXL), 한국 테마 대장주 | 유동성(한국 5일 평균 거래대금 >1,000억, 미국 >5억 달러), RSI(14)>60 & MACD>Signal, 수급(기관/외국인 순매수 2일+ & 체결강도 120%+) | Trailing Stop: 고점 대비 -3% 시 기계적 매도 |
| **중기** | 40% | 분기 15%+ | 주도 섹터(반도체, AI, 2차전지, 바이오 등) 1등주 | 듀얼 모멘텀: 3개월 수익률 S&P500/KOSPI 대비 상위 10%, EPS 성장(YoY)>20%, PEG<1.5, 주가가 20·60일선 위 정배열 | 월 1회 리밸런싱, 모멘텀 순위 하락 시 교체; 개별 -10% 도달 시 손절 |
| **장기** | 40% | 연 25%+ (복리 극대화) | 경제적 해자 확실한 글로벌 1위 (미국 빅테크 위주) | 퀄리티: ROE>20%, OPM>25%; Rule of 40(매출증가율+영업이익률>40%); MDD -15%~-20% 시 분할 매수 트리거 | 펀더멘털 훼손 시에만 매도; 상대적 손절 없음(저가 시 추가 매수) |

- **시드 배분 요약**: 슈퍼 그로스(장기) 40%, 추세 추종(중기) 40%, 알고리즘 트레이딩(단기) 20%.
- 기간별 전략이 **나라별(KR/US)**·**파이프라인 단계**와 교차할 때(예: 단기-KR 테마주 필터, 장기-US 빅테크) 해당 조합별 수식·임계값은 위 §3 및 코드에서 적용 범위를 확인한다.

---

## 5. 분야별 — 파이프라인 단계

1. **유니버스 선정** → §3.1 (Liquidity + KR: Sector RS / US: Post-Earnings Drift)
2. **시그널 생성** → §3.2 (KR: 이격도·변동성 돌파·수급 강도 / US: 듀얼 모멘텀·퀄리티-성장)
3. **자금 관리** → §2.2 Half-Kelly, §2.4 ATR 포지션 사이징, §2.5 변동성 역가중
4. **매매 실행·청산** → §3.3 ATR Trailing Stop, Time-Cut

향후 섹터/테마(반도체, AI, 2차전지 등)별 세부 전략은 본 섹션 하위에 확장한다.

**트레이딩 포트폴리오(일별 추천 목록)**: 화면용 일별 추천 종목은 단기 파이프라인과 동일한 시그널·필터·포지션 사이징을 사용한다. ShortTermTradingStrategyService가 1차로 PositionSizingService.getRecommendations(basDt, KR, SHORT_TERM, defaultCapital) 결과로 TB_TRADING_PORTFOLIOS/ITEMS를 채우고, 시그널이 없을 때만 실시간 API(StockScreeningService) fallback 또는 모의 데이터를 사용한다. 스케줄은 팩터 계산(08:00) 이후 09:00 KST.

---

## 6. 계산 수식·파라미터 일람

| 구분 | 수식/로직 요약 | application.yml 키 | 대응 서비스/메서드 |
|------|----------------|--------------------|--------------------|
| 이격도 | Disparity = close×100/MA(20) | disparity-ma-days: 20 | FactorCalculationService.addDisparity |
| 변동성 돌파 | Target = Open + Range×k, k 동적(한국) | volatility-breakout-k, k-dynamic, k-min, k-max | addVolatilityBreakout, calculateDynamicK |
| 유동성 | 거래대금 ≥ 임계값 | liquidity-min-trd-val: 1e9 | UniverseFilterService, FactorCalculationService (유동성 팩터) |
| 유동성(시초가) | KR 단기 시초가/변동성 돌파 시 거래대금 ≥ 300억 | liquidity-min-trd-val-opening: 30e9 | PositionSizingService (KR SHORT_TERM) |
| 포지션 리스크 | 1회 매매당 총자산 1% | position-risk-pct: 0.01 | PositionSizingService |
| 켈리 초기 고정 비율 | kelly-enabled=false 시 1종목당 자산 N% 상한 | kelly-enabled: false, kelly-fixed-allocation-pct: 2 | PositionSizingService.applyHalfKelly |
| Half-Kelly | f* = (bp−q)/b, 50% 적용 | kelly-p: 0.6, kelly-b: 2.0 | PositionSizingService.applyHalfKelly |
| 미국 갭 스킵 | 전일 종가 대비 갭 N% 이상 시 진입 스킵 | us-gap-up-skip-pct: 5 | PositionSizingService.filterByUsGapUpSkip |
| Discord 긴급 알림 | 미체결 N분 경과 시 Discord Webhook 발송 | alert-discord-webhook-url, alert-base-url, unfilled-check-minutes: 1 | EmergencyAlertService, UnfilledOrderCheckScheduler |
| 시드 배분 | 계좌별 단기/중기/장기 비율(합=1), 기본 20/40/40 | TB_TRADING_SETTINGS SHORT/MEDIUM/LONG_TERM_RATIO, maxInvestmentAmount, autoTradingEnabled | PipelineExecutionScheduler(자동투자 ON 계좌만) |
| 단기 -3% Trailing | 현재가 ≤ trailingHigh×0.97 시 매도 | short-term-trailing-pct: 3 | ExitRuleService.evaluateShortTermTrailingStop |
| 한국 단기/스윙 -5% 손절 | 현재가 ≤ entryPrice×0.95 시 매도 | short-term-kr-stop-loss-pct: 5 | ExitRuleService.evaluateKrFixedStopLoss |
| 한국 전저점 이탈 | 현재가 &lt; priorLow 시 매도 | prior-low-stop-kr-enabled: true | ExitRuleService.evaluatePriorLowStop |
| 한국 RSI≥70 익절 | RSI(14) ≥ 70 시 매도 | rsi-exit-threshold: 70 | ExitRuleService.evaluateRsiExit |
| 중기 -10% 손절 | 현재가 ≤ entryPrice×0.9 시 매도 | medium-term-stop-loss-pct: 10 | ExitRuleService.evaluateMediumTermStopLoss |
| ATR Trailing Stop | 현재가 ≤ trailingHigh − ATR×mult | atr-trailing-stop-multiplier: 2.0 | ExitRuleService.evaluateAtrTrailingStop |
| Time-Cut | N일 내 목표 수익률 미도달 시 매도 (단기 전용) | (엔티티: timeCutDays, targetReturnPct) | ExitRuleEvaluator (SHORT_TERM만) |
| 듀얼 모멘텀 | 기간별 수익률 가중합, score=종목−시장(%) | dual-momentum-period-days: 21,63,126, dual-momentum-weights: 0.5,0.3,0.2 | FactorCalculationService.addDualMomentum |
| Half-Kelly 전략별 p·b | 백테스트 winRate·profitFactor 연동 | kelly-p/kelly-b + kelly-p-short-term 등 (전략별) | PositionSizingService.getKellyP/getKellyB |
| Sector RS | 상위 N개 업종 내 종목만 | sector-rs-top-n: 5 | UniverseFilterService.filterBySectorRelativeStrength |
| Post-Earnings Drift | 최근 N일 실적 발표 상위 N% | earnings-surprise-lookback-days: 90, earnings-surprise-top-pct: 0.2 | UniverseFilterService.filterByPostEarningsDrift |
| 수급 강도 | 순매수/시총 비율(%) | smart-money-intensity-threshold-pct: 0.005 | FactorCalculationService.addSmartMoneyIntensity (TB_ORDER_FLOW) |
| 퀄리티-성장 | PEG 점수 + Rule of 40 | TB_FUNDAMENTALS (PER, PEG, 매출증가율, 영업이익률) | FactorCalculationService.addQualityGrowth |
| 로보 동적 자산배분 | 모멘텀 N개월·MA 필터·변동성 역가중·Top N | investment.backtest.robo.* (asset-symbols, momentum-months, ma-window-days, top-n, rebalance-frequency, comm-pct, slip-pct, pre-execution-*) | RoboAllocationEngine, RoboBacktestService, RoboRebalanceScheduler |
| 로보 듀얼 모멘텀(노트) | 절대: SPY 12M vs 무위험; 상대: 섹터 ETF 6M 상위 2개 | dual-momentum-mode, robo-sector-etf-symbols, robo-momentum-months-relative: 6, robo-top-n-sector: 2 | RoboAllocationEngine(모드 분기), RoboBacktestProperties |
| 한국 역발상 RSI | RSI(14)&lt;30/40 시 매수 시그널 가중 | contrarian-rsi-threshold: 40 | FactorCalculationService.addContrarianRsiSignal |

---

## 7. 버전 스택 (경험 축적)

전략·팩터·계산 방식이 변경될 때마다 아래 테이블에 한 행을 추가하고, 백테스트/실전 결과가 있으면 **결과**·**교훈·비고**를 기입해 실패 사례를 참고할 수 있게 한다.

| 버전 | 일자 | 적용 시장(나라) | 적용 분야/기간 | 변경 요약 | 결과 | 교훈·비고 |
|------|------|-----------------|----------------|-----------|------|-----------|
| v1.0 | 2026-01-31 | KR, US | 전 단계·단기/중기/장기 | 초기 통합: 공통·나라별·기간별·파이프라인·수식 일람·버전 스택 도입 | 미검증 | 단일 소스로 상세 수식·파라미터 관리 시작 |
| v1.1 | 2026-01-31 | KR, US | 기간별 청산·시드 배분 | 단기 -3% Trailing Stop, 중기 -10% 손절, 장기 스텁; 시드 20/40/40; TB_STRATEGY_POSITION STRATEGY_TYPE; PipelineExecutionScheduler·MediumTermRebalanceScheduler(스텁) | 미검증 | 기간별 파이프라인 실행·청산 분기 반영 |
| v1.2 | 2026-01-31 | KR, US | 청산·포지션 등록 | ATR Trailing Stop 장중 연동(RealtimeMarketDataService 현재가·당일 고가→trailingHigh 갱신); PipelineExitScheduler(장중 5분마다 청산 평가·매도 실행); 체결 확인 후 포지션 등록(TB_ORDERS 포지션 컨텍스트, FillConfirmationScheduler) | 미검증 | 장중 실시간 시세·체결 후 등록 파이프라인 완결 |
| v1.3 | 2026-02-01 | KR, US | 시드 배분·자동투자 대상 | 계좌별 단기/중기/장기 비율(TB_TRADING_SETTINGS), 자동투자 ON 계좌만 PipelineExecutionScheduler 대상; 설정 전용 화면(/settings)에서 모의·실 계좌 API·거래 설정(비율·자동투자 ON/OFF) 한번에 입력·저장 | 미검증 | 모의·실 계좌별 비율·자본·자동투자 ON/OFF 반영 |
| v1.4 | 2026-02-01 | KR | 트레이딩 포트폴리오 | 일별 추천 목록(트레이딩 포트폴리오)을 파이프라인(TB_SIGNAL_SCORE + TB_DAILY_STOCK) 기반 단기 권장으로 1차 생성; 시그널 없을 때 실시간 API fallback; 토큰/API키 요청 내 캐시로 N+1 제거; 스케줄 09:00 KST; 리스크 문구 전략 레지스트리와 동일 | 미검증 | 포트폴리오·자동투자 단일 시그널 소스 정렬 |
| v1.5 | 2026-02-01 | KR, US | 파이프라인 확장 | 미국 듀얼 모멘텀(TB_DAILY_STOCK 기간별 수익률·시장 대비); Half-Kelly 전략별 p·b(백테스트 연동); Sector RS·Post-Earnings Drift(TB_SECTOR_RETURN·TB_SYMBOL_SECTOR·TB_EARNINGS_SURPRISE); 수급 강도·퀄리티-성장(TB_ORDER_FLOW·TB_FUNDAMENTALS). 데이터 없으면 fallback(유동성만/0) | 미검증 | 데이터 수집 후 유니버스·시그널 품질 향상 |
| v1.6 | 2026-02-02 | US | 로보 어드바이저 | 동적 자산배분 백테스트(모멘텀·MA 필터·변동성 역가중·월/분기 리밸런싱·Turnover). RoboAllocationEngine·RoboBacktestService·POST /api/v1/backtest/robo. 실행 전 백테스트(Go/No-Go)·RoboRebalanceScheduler·RoboRebalanceExecutor. TB_TRADING_SETTINGS ROBO_ADVISOR_ENABLED(V17) | 미검증 | 로보 리밸런싱은 목표 비중 로깅만, ETF 주문 연동 추후 |
| v1.7 | 2026-02-02 | KR, US | 월스트리트 정렬 | 한국: 외국인 5일 연속+저평가(P/B 0.8~0.9)+RSI 과매도 진입; 청산 -5%·전저점 이탈·RSI≥70 익절. 미국: 듀얼 모멘텀(노트) 모드 — 절대 SPY 12M vs T-bill, 상대 섹터 ETF 6M 상위 2개. ExitRuleService KR 전용 규칙, RoboAllocationEngine 모드 분기, strategy-registry·12-auto-investment-strategy 문서 반영 | 미검증 | Hunter(KR)·Surfer(US) 전략 명시 |
| v1.8 | 2026-02-02 | KR, US | 자동매매 직전 점검 | Time-Cut 단기(SHORT_TERM) 전용·중/장기 제외. 한국 Hunter 분기(Case A 모멘텀 ∪ Case B 역발상). 시초가 유동성 opening(300억). 켈리 초기 고정 비율(kelly-enabled·kelly-fixed-allocation-pct). 미국 갭 상승 스킵(us-gap-up-skip-pct). 미체결 N분 경과 시 Discord 긴급 알림(UnfilledOrderCheckScheduler·EmergencyAlertService). | 미검증 | Go/No-Go 체크리스트 반영 |

---

## 문서 변경 이력

| 버전 | 일자 | 변경 내용 |
|------|------|-----------|
| 1.0 | 2026-01-31 | 초기 전략·계산 방식 통합 레지스트리 작성 — 메타·공통·나라별·기간별·분야별·수식 일람·버전 스택 반영 |
| 1.1 | 2026-01-31 | 기간별 청산 규칙(-3% Trailing, -10% 손절, 장기 스텁)·시드 배분 20/40/40·수식 일람·버전 스택 v1.1 추가 |
| 1.2 | 2026-01-31 | ATR Trailing Stop 장중 연동·체결 확인 후 포지션 등록 반영, 버전 스택 v1.2 추가 |
| 1.3 | 2026-02-01 | 계좌별 시드 비율·자동투자 ON 대상 반영, 버전 스택 v1.3 추가 |
| 1.4 | 2026-02-01 | 트레이딩 포트폴리오 파이프라인 기반 통합·버전 스택 v1.4 추가 |
| 1.5 | 2026-02-01 | 4단계 파이프라인 확장(듀얼 모멘텀·Half-Kelly p·b·Sector RS·Post-Earnings Drift·수급 강도·퀄리티-성장) 반영, §3·§6·버전 스택 v1.5 추가 |
| 1.6 | 2026-02-02 | 로보 어드바이저 동적 자산배분 백테스트·실행 전 검증·§3.2.1·§6·버전 스택 v1.6 추가 |
| 1.7 | 2026-02-02 | 월스트리트 정렬: 한국 5일 연속·P/B·RSI 과매도·-5%·전저점·RSI70 청산; 미국 듀얼 모멘텀(절대 12M vs T-bill·상대 6M 섹터 2개)·§3·§6·버전 스택 v1.7 추가 |
| 1.8 | 2026-02-02 | 자동매매 직전 점검: Time-Cut 단기 전용·Hunter 분기(Case A/B)·시초가 유동성 opening·켈리 초기 고정 비율·미국 갭 스킵·Discord 긴급 알림·§2·§3·§6·버전 스택 v1.8 추가 |
