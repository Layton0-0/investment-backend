# 12-auto-investment-strategy.md 구현 점검 보고서

**기준 문서**: [12-auto-investment-strategy.md](./12-auto-investment-strategy.md)  
**점검 일자**: 2026-02-09  
**점검 범위**: 명세 §1~§10 및 §6.1/§6.2 구현 여부·설정·코드 매핑

---

## 요약

| 구분 | 상태 | 비고 |
|------|------|------|
| §1 수익·리스크 목표 | 목표치 정의 | MDD/CAGR는 백테스트·로보 설정에 반영, "0.1초 단위"는 WebSocket/실시간 연동 시 적용 |
| §2 Core Logic (Kelly, 변동성 돌파) | 구현됨 | PositionSizingService(Half-Kelly), FactorCalculationService/IntradayBreakoutService(변동성 돌파) |
| §3 기간별 알고리즘·필터 | 구현됨 | 단기/중기/장기 비중·필터·청산 규칙 코드 반영 |
| §4 시드 배분·리스크 관리 | 구현됨 | 20/40/40, ATR 1% 리스크, 변동성 역가중(RoboAllocationEngine·PositionSizingService) |
| §5 4단계 파이프라인 | 대부분 구현 | 유니버스/시그널 확장(Sector RS, Post-Earnings)은 데이터 수집 후 스텁·설정만 |
| §6 로드맵·자동매수·플로우 | 구현됨 | AutoBuyOrchestrator, 09:10, 청산 5분마다, 체크리스트 대부분 반영 |
| §7 데이터 원천 | 부분 구현 | DART/SEC/KRX/US 구현. 연합뉴스·로이터·네이버·Yahoo 뉴스/센티멘트 파이프라인 연결 미확인 |
| §8 KIS API 실전 구축 | 부분 구현 | REST 주문·계좌 연동 있음. WebSocket 우선·Throttling 큐·토큰 Crontab은 문서 권장 수준 |
| §9 용어·§10 확장성 | 문서 정리 | 코드 네이밍·설정 키와 레지스트리 일치 |

**불일치·보완 필요**  
- `application.yml`의 `pipeline.auto-execute` 기본값이 `true`. 명세 §6.2는 "기본값 false"로 서술 → **문서 또는 설정 정합성 검토 필요**.  
- §7 Speed/Buzz 원천(연합·로이터·네이버·Yahoo)은 **명세 대비 구현 범위 확인 필요**.

---

## 1. §1 수익·리스크 목표

| 항목 | 명세 | 구현 |
|------|------|------|
| MDD -15% 이내 | 목표 | 로보 실행 전 백테스트 `pre-execution-max-mdd-pct`, 백테스트 메트릭으로 검증 |
| CAGR 30%+ | 목표 | 백테스트·로보 메트릭으로 추적, 자동 거부 규칙은 없음 |
| 0.1초 단위 정보·주문 | 생명선 | REST 주문·실시간 시세 연동. WebSocket 실시간 체결/호가는 별도 연동 시 적용 |

**판정**: 목표치 정의는 반영됨. 실시간 레이턴시는 현재 REST 기반이며, §8의 "WebSocket 우선" 확장 시 충족 가능.

---

## 2. §2 Core Logic

### 2.1 켈리 공식 (Half-Kelly)

- **구현**: `PositionSizingService` — `kelly-p`(0.6), `kelly-b`(2.0), `applyHalfKelly()`에서 50% 적용.  
- **초기 운용**: `kelly-enabled: false`, `kelly-fixed-allocation-pct: 2` (§6.2 체크리스트 5번 반영).  
- **전략별**: `kelly-p-short-term` 등 전략별 p·b 설정 가능(백테스트 연동용).

### 2.2 변동성 돌파

- **구현**: `FactorCalculationService.addVolatilityBreakout`, `calculateDynamicK` (한국장 변동성 반영).  
- **설정**: `volatility-breakout-k`, `volatility-breakout-k-dynamic`, `k-min`/`k-max`.  
- **장중**: `IntradayBreakoutService` (시가+Range×k), `IntradayBreakoutScheduler` (09:10, 09:40).

**판정**: 명세대로 구현됨.

---

## 3. §3 기간별 세부 알고리즘 및 필터

- **단기 20%**: RSI>60 & MACD>Signal (`PositionSizingService.filterSymbolsByStrategyType`), -3% Trailing Stop, Time-Cut, KR 시 -5% 고정·전저점 이탈·RSI≥70 익절.  
- **중기 40%**: 시그널 상위 10% 필터, -10% 손절, Time-Cut 미적용.  
- **장기 40%**: 필터 없음(전체 시그널), 청산 스텁(매도 시그널 없음).  
- **시드 배분**: `PipelineExecutionScheduler`에서 0.2/0.4/0.4로 KR/US×SHORT/MEDIUM/LONG 6회 run.

**판정**: 명세 표와 §6.1 구현 상태와 일치.

---

## 4. §4 시드 배분 및 리스크 관리

- **4.1 포트폴리오 비중**: TB_TRADING_SETTINGS 단/중/장기 비율, 기본 20/40/40.  
- **4.2 ATR 포지션 사이징**: `position-risk-pct: 0.01` (1회 매매당 총자산 1%), ATR 기반 손절가·권장 금액.  
- **4.3 변동성 역가중**: `PositionSizingService`(포지션 권장), `RoboAllocationEngine`(로보 비중)에서 적용.

**판정**: 구현됨.

---

## 5. §5 4단계 파이프라인

### 5.1 유니버스 필터링

| 구분 | 명세 | 구현 |
|------|------|------|
| 공통 Liquidity Cut-off | 거래대금 ≥ 임계값 | `UniverseFilterService`, `liquidity-min-trd-val` (기본 10억) |
| 한국 시초가 300억 | 시초가/변동성 돌파 시 | `liquidity-min-trd-val-opening: 30000000000`, KR SHORT_TERM `PositionSizingService` |
| 한국 Sector RS | 주도 업종 내 종목 | `sector-rs-top-n`, TB_SECTOR_RETURN·TB_SYMBOL_SECTOR 수집 후 적용(스텁) |
| 미국 Post-Earnings Drift | 어닝 서프라이즈 상위 20% | `earnings-surprise-*`, TB_EARNINGS_SURPRISE 수집 후 적용(스텁) |

### 5.2 시그널 생성

| 구분 | 명세 | 구현 |
|------|------|------|
| 미국 듀얼 모멘텀(노트) | SPY 12M vs 무위험, 섹터 6M 상위 2개 | `RoboAllocationEngine.computeTargetWeightsDualMomentumNote`, 로보 설정 |
| 한국 Hunter Case A/B | Case A(모멘텀) ∪ Case B(역발상) | `PositionSizingService.filterSymbolsKrShortTerm`, CONTRARIAN_RSI, RSI·MACD |
| 변동성 돌파(한국형) | k 동적, 유동성 300억 | FactorCalculationService, IntradayBreakoutService, opening 유동성 |
| 청산 규칙 (한국) | -5%, 전저점 이탈, RSI≥70 | `ExitRuleEvaluator` (KR_FIXED_STOP_LOSS, KR_PRIOR_LOW_STOP, KR_RSI_EXIT) |

### 5.3·5.4 자금 관리·매매 실행·청산

- 켈리·변동성 역가중: §2·§4와 동일.  
- ATR Trailing Stop: `ExitRuleService.evaluateAtrTrailingStop`, `atr-trailing-stop-multiplier: 2.0`.  
- Time-Cut: 단기(SHORT_TERM) 전용, `ExitRuleEvaluator`·`PipelineExecutor`에서 timeCutDays/targetReturnPct.

**판정**: 4단계 파이프라인 및 기간별·시장별 분기는 명세대로 구현됨. Sector RS·Post-Earnings는 데이터 수집·테이블 확보 후 활성화되는 구조.

---

## 6. §6 로드맵·자동매수·플로우

### 6.1.1 자동매수 = 통합 복합 로직

- `AutoBuyOrchestrator`: 로보 → 파이프라인 순 호출.  
- Batch Job `auto-buy`: 09:10 KST cron (`BatchJobRegistry`).  
- `pipeline-execution`·`robo-rebalance`: cron 제거(수동 전용).  
- `POST /api/v1/trigger/auto-buy` (dryRun optional).

### 6.2 스케줄·플로우

- 데이터 수집: DART 10분, SEC 15분, KRX 16:00, US 17:00 — `DataCollectionScheduler`, Batch Job ID로 반영.  
- 팩터 계산: 08:00 — `FactorCalculationScheduler`, `factor.schedule-cron`.  
- 자동매수(통합): 09:10 — `auto-buy` Job, `execution-schedule-cron: 0 10 9 * * *`.  
- 청산: 장중 5분마다 — `PipelineExitScheduler`, `exit-schedule-cron`.  
- 체결 확인: 매분 — `FillConfirmationScheduler`, `fill-confirmation-cron`.

### §6.2 Go/No-Go 체크리스트

| # | 항목 | 구현 |
|---|------|------|
| 1 | 국내/해외 주문 API 분기 | `OrderService.executeOrderForPipeline` → `request.getMarketOrKr()`로 KR/US 분기, 국내/해외 주문 클라이언트 호출 |
| 2 | 한국 Case A ∪ Case B | `PositionSizingService.filterSymbolsKrShortTerm` |
| 3 | 시초가 유동성 300억, 최유리/IOC | `liquidity-min-trd-val-opening`, 주문 타입(최유리/IOC)은 한투 API 호출부에서 확인 필요 |
| 4 | Time-Cut 단기 전용 | `BacktestService`·`PipelineExecutor`·`ExitRuleEvaluator`에서 SHORT_TERM만 timeCutDays/targetReturnPct 적용 |
| 5 | 켈리 비활성·고정 비율 | `kelly-enabled: false`, `kelly-fixed-allocation-pct: 2` |
| 6 | 미체결 Discord 알림 | `UnfilledOrderCheckScheduler`, `EmergencyAlertService`(userId·계좌 마스킹·모의/실전·증권사·URL 포함) |
| 7 | 설정 화면 자동 매매 ON | 프론트 설정·PUT /api/v1/settings/{accountNo} |
| 8 | 서버 auto-execute | **불일치**: 명세 "기본값 false", `application.yml`은 `PIPELINE_AUTO_EXECUTE:true` → 문서 또는 기본값 정리 필요 |
| 9~12 | 모의 권장·실전 가드·로보 ETF 주문 등 | allow-real-execution, RoboRebalanceExecutor.executeOrderForPipeline, execute-orders 설정 등 반영 |

**판정**: 체크리스트 대부분 구현됨. `auto-execute` 기본값만 문서와 설정 불일치.

---

## 7. §7 공시/데이터·뉴스 원천

- **Fact**: DART/SEC EDGAR — Python investment-data-collector에서 수집 후 Spring 내부 API로 전달(구현됨).  
- **시세**: KRX 일별(`KrxApiClient`·관련 Job), US(`UsMarketCollectionService`) — 구현됨.  
- **Speed/Buzz**: 명세의 연합뉴스(Yonhap), 로이터(Reuters), 네이버 금융, Yahoo Finance는 **동일 파이프라인 연결·원천 확정**이 코드에서 확인되지 않음. 뉴스/공시는 DART·SEC_EDGAR 소스로 TB_NEWS_ITEMS 등에 저장되는 구조.

**판정**: §7.1 표의 Fact·시세 계열은 구현됨. Speed/Buzz 원천은 별도 설계(예: 13-news-collection-design.md)와의 연동 여부·구현 범위 확인 필요. 구현 범위는 12-auto-investment-strategy.md §7.5 및 13-news-collection-design.md §2.4에 반영함.

---

## 8. §8 한국투자증권 API 실전 구축

- **REST 주문·계좌**: `KoreaInvestmentOrderClient`, `OrderService` — 국내/해외 분기 구현됨.  
- **실시간 시세·체결(WebSocket)**: 명세는 "WebSocket 우선" 권장. 현재는 REST 기반 호출로 보임.  
- **Throttling·큐**: 문서 권장 수준, 메시지 큐 도입 여부는 미확인.  
- **토큰 갱신**: TokenService·장 시작 전 갱신 정책은 인증 모듈에 구현되어 있음.

**판정**: REST 기반 실전 연동은 구현됨. WebSocket·Throttling 큐는 단계적 확장 대상.

---

## 9. 권장 조치

1. **문서·설정 정합성**: `12-auto-investment-strategy.md` §6.2 항목 8과 `application.yml`의 `pipeline.auto-execute` 기본값을 일치시키기. (명세대로라면 기본값 false 권장.)  
2. **§7 Speed/Buzz**: 연합·로이터·네이버·Yahoo 등 뉴스/센티멘트 원천이 파이프라인에 연결된 경로가 있으면 문서화; 없으면 명세 §7과 구현 범위를 구분해 명시.  
3. **최유리/IOC**: §6.2 항목 3의 "최유리 지정가 또는 IOC"가 한투 주문 API 호출 시 실제로 사용되는지 코드에서 확인 후, 미반영 시 주문 타입 매핑 보완.

---

이 보고서는 [12-auto-investment-strategy.md](./12-auto-investment-strategy.md) 기준으로 구현을 점검한 결과이며, 상세 수식·설정 키는 [00-strategy-registry.md](./00-strategy-registry.md)와 동일하게 참조한다.
