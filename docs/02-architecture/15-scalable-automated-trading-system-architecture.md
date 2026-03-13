# 확장 가능한 자동매매 시스템 아키텍처

**목적**: 프로덕션 수준의 자동매매 봇을 위한 시스템 아키텍처, 데이터 파이프라인, 주문 실행, 레이턴시, 리스크 관리 및 **컴포넌트 다이어그램·데이터 플로우·장애 처리·모니터링**을 단일 문서로 정의한다.

**관련 문서**: [01-system-architecture.md](./01-system-architecture.md), [00-strategy-registry.md](./00-strategy-registry.md), [12-auto-investment-strategy.md](./12-auto-investment-strategy.md).

---

## 1. 설계 원칙

| 원칙 | 설명 |
|------|------|
| **Production-ready** | 배포·롤백·장애 대응·감사 추적이 가능한 설계 |
| **Modular** | Data / Alpha / Risk / Execution이 명확히 분리되어 교체·확장 가능 |
| **Bot-friendly** | 스케줄·이벤트 기반 자동 실행, 인증·계정 컨텍스트 분리 |
| **Reliability first** | 실패 시 안전 쪽으로 동작(fail-safe), 재시도·회로차단·킬스위치 |
| **Observability** | 메트릭·헬스·로그·알림으로 “왜 거래했는지” 추적 가능 |

---

## 2. 컴포넌트 다이어그램

### 2.1 논리 컴포넌트 (Quant Engine)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         CLIENT / OPERATOR LAYER                                  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │ Web (React) │  │ REST API    │  │ Batch API  │  │ Admin (Kill Switch,     │  │
│  │ Dashboard  │  │ (Orders,    │  │ (Trigger   │  │  Governance, Reconcile) │  │
│  │             │  │  Accounts)  │  │  Jobs)     │  │                         │  │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └───────────┬─────────────┘  │
└─────────┼────────────────┼────────────────┼─────────────────────┼─────────────────┘
          │                │                │                     │
          ▼                ▼                ▼                     ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         APPLICATION LAYER (Spring Boot)                         │
│                                                                                   │
│  ┌───────────────────────────────────────────────────────────────────────────┐  │
│  │ DATA PIPELINE                                                               │  │
│  │  DataPipelineService │ Batch(일봉/팩터/유니버스) │ DataCollector(Python)     │  │
│  │  → TB_DAILY_STOCK, TB_SIGNAL_SCORE, TB_UNIVERSE, TB_ORDER_FLOW, ...        │  │
│  └───────────────────────────────────────────────────────────────────────────┘  │
│                                      │                                           │
│                                      ▼                                           │
│  ┌───────────────────────────────────────────────────────────────────────────┐  │
│  │ ALPHA (Signal & Portfolio)                                                 │  │
│  │  AlphaEngine / StrategyService │ PositionSizingService │ TaxAwareOptimizer  │  │
│  │  Rebalancer │ StrategyWeightResolver │ MacroEconomicStrategyEngine         │  │
│  │  → 권장 포지션 목록 (symbol, side, quantity, notional)                      │  │
│  └───────────────────────────────────────────────────────────────────────────┘  │
│                                      │                                           │
│                                      ▼                                           │
│  ┌───────────────────────────────────────────────────────────────────────────┐  │
│  │ RISK GUARD                                                                 │  │
│  │  RiskGateService │ DailyLossLimitService │ MarketCrashGateService           │  │
│  │  GovernanceHaltService │ PreTradeComplianceEngine (Kill, MDD, 10% cap)     │  │
│  │  → Go/No-Go, size multiplier, order reject                                 │  │
│  └───────────────────────────────────────────────────────────────────────────┘  │
│                                      │                                           │
│                                      ▼                                           │
│  ┌───────────────────────────────────────────────────────────────────────────┐  │
│  │ EXECUTION                                                                  │  │
│  │  PipelineExecutor │ OrderService │ PipelineOrderExecutor (TWAP/VWAP)        │  │
│  │  KoreaInvestmentOrderClient │ OrderRequestQueue (optional)                  │  │
│  │  → 주문 실행, 체결 확인, 포지션 등록 (TB_ORDERS, TB_STRATEGY_POSITION)       │  │
│  └───────────────────────────────────────────────────────────────────────────┘  │
│                                      │                                           │
│  ┌──────────────────────────────────┼───────────────────────────────────────┐  │
│  │ OBSERVABILITY                     │                                         │  │
│  │  Actuator (health, metrics, prometheus) │ AuditLogService │ Discord Alerts │  │
│  │  EmergencyAlertService (미체결 등) │ RiskEventAlertService                  │  │
│  └──────────────────────────────────┴───────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────┘
          │                │                │
          ▼                ▼                ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         INFRASTRUCTURE LAYER                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │ TimescaleDB  │  │ Redis        │  │ 한국투자증권  │  │ Data Collector       │  │
│  │ (PostgreSQL) │  │ (Cache,     │  │ Open API     │  │ / Prediction Service │  │
│  │              │  │  Session)   │  │ (REST)       │  │ (Python, optional)   │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 실행 경로별 컴포넌트 매핑

| 실행 경로 | 진입점 | 주요 컴포넌트 |
|-----------|--------|----------------|
| **단기/중기/장기 파이프라인** | PipelineExecutionScheduler | RiskGate → StrategyWeight → PositionSizing → PipelineExecutor → OrderService → KIS API |
| **청산** | PipelineExitScheduler | ExitRuleService, RealtimeMarketData, OrderService |
| **로보 리밸런싱** | RoboRebalanceScheduler | RoboAllocationEngine, Rebalancer, OrderService |
| **시초가/변동성 돌파** | IntradayBreakoutScheduler | RiskGate, PositionSizing, PipelineExecutor |
| **수동 주문** | OrderController | ComplianceEngine → OrderService → KIS API |

---

## 3. 데이터 플로우 (Data Flow)

### 3.1 시장 데이터 → 시그널 → 주문 (정방향)

```
[외부 시세/공시]
       │
       ▼
┌──────────────────┐     ┌──────────────────┐     ┌──────────────────┐
│ Data Collection  │────▶│ TB_DAILY_STOCK   │────▶│ Factor / Signal  │
│ (Batch, Python)  │     │ TB_ORDER_FLOW    │     │ Calculation      │
│                  │     │ TB_SIGNAL_SCORE   │     │ (Universe, Score)│
└──────────────────┘     └──────────────────┘     └────────┬─────────┘
                                                          │
       ┌──────────────────────────────────────────────────┘
       ▼
┌──────────────────┐     ┌──────────────────┐     ┌──────────────────┐
│ Position Sizing  │────▶│ Risk Gates       │────▶│ PipelineExecutor │
│ (Kelly, ATR,     │     │ (VIX, MDD,       │     │ (권장 → 주문)    │
│  Recommendation) │     │  DailyLoss, Halt)│     │                  │
└──────────────────┘     └──────────────────┘     └────────┬─────────┘
                                                           │
       ┌───────────────────────────────────────────────────┘
       ▼
┌──────────────────┐     ┌──────────────────┐
│ PreTrade         │────▶│ OrderService     │────▶ Korea Investment API
│ Compliance       │     │ (KIS Client)     │
└──────────────────┘     └────────┬─────────┘
                                  │
                                  ▼
                         TB_ORDERS, TB_STRATEGY_POSITION (체결 후)
```

### 3.2 데이터 플로우 요약 표

| 단계 | 입력 | 출력 | 지연 요인 |
|------|------|------|-----------|
| 수집 | API/파일 | TB_DAILY_STOCK 등 | 배치 주기, API 한도 |
| 팩터/시그널 | 일봉·유니버스 | TB_SIGNAL_SCORE, 권장 리스트 | DB 쿼리, PIT 일자 |
| 리스크 게이트 | 지표, 잔고, 피크 | allow/deny, sizeMultiplier | Redis/DB, 매크로 URL |
| 컴플라이언스 | 주문 요청, 계좌 | approved/rejected | DB(피크, halt) |
| 주문 실행 | OrderRequest | 주문번호, 체결 | KIS API 레이턴시, 재시도 |

---

## 4. 레이턴시 고려사항 (Latency Considerations)

| 구간 | 목표 | 조치 |
|------|------|------|
| **시그널 산출** | 기준일 종료 후 N분 이내 | 배치 크론(예: 08:00), 인덱스(bas_dt, market), 팩터 캐시 |
| **리스크 게이트** | &lt; 수백 ms | Redis 캐시(매크로 지표), DB 피크/잔고 인덱스 |
| **Pre-Trade 검사** | &lt; 100 ms | 단일 계좌 조회, Kill Switch Redis/DB |
| **주문 API** | 타임아웃 12s, 재시도 2회 | TimeLimiter, Circuit Breaker, block(ORDER_API_BLOCK_TIMEOUT) |
| **체결 확인** | 장중 주기(예: 5분) | FillConfirmationScheduler, TB_ORDERS 상태 갱신 |
| **E2E (시그널→체결)** | 장 개장 구간 내 완료 | 트레이딩 윈도우(KR 09:00~10:00, 14:30~15:30 등)에 스케줄 정렬 |

- **실시간 청산**: PipelineExitScheduler 주기(예: 5분), RealtimeMarketDataService 현재가/고가 → trailing stop 판단.
- **고빈도 목표가 아닌 경우**: 현재 설계는 일봉/스케줄 기반 자동매매에 적합하며, 초저지연 HFT는 별도 아키텍처가 필요하다.

---

## 5. 장애 처리 (Failure Handling)

### 5.1 계층별 장애 처리

| 계층 | 장애 유형 | 처리 방식 |
|------|-----------|-----------|
| **데이터 수집** | API 장애, 타임아웃 | 재시도, 알림, 이전 일자 데이터로 파이프라인 계속 가능 시 진행 |
| **팩터/유니버스** | 데이터 부재(0건) | 유니버스 0건 시 해당 시장·전략 스킵, 로그·메트릭 기록 |
| **리스크 게이트** | 매크로 URL 실패 | fail-open(데이터 부재 시 허용) 또는 보수적 거부 정책 명시 |
| **컴플라이언스** | Kill Switch ON | 모든 주문 거부, 403/ORDER_REJECTED |
| **주문 API** | 타임아웃/5xx | 재시도(최대 2회), Circuit Breaker 열리면 fallback 예외 → 사용자 재시도 유도 |
| **체결 지연** | 미체결 N분 | Discord 긴급 알림(UnfilledOrderCheckScheduler), 수동 확인 유도 |

### 5.2 Fail-Safe 원칙

- **주문 직전**: PreTradeComplianceEngine 실패 시 → 주문 거부(승인 없음).
- **일일 손실 한도 / MDD 게이트** 초과 시 → 신규 매수만 중단, 매도는 허용.
- **Governance Halt** (전략 열화): 해당 (market, strategyType) 파이프라인 스킵.
- **Kill Switch**: TB_TRADING_HALT.halt_all_orders=true → 모든 주문 거부.

### 5.3 재시도·회로차단

- **OrderService**: Resilience4j Circuit Breaker `orderService`, fallback 시 DomainException 발생, 30초 후 재시도 안내.
- **주문 API**: ORDER_API_MAX_ATTEMPTS=2, ORDER_API_BLOCK_TIMEOUT=12s.
- **외부 시세/매크로**: 타임아웃·재시도는 각 Client 설정에 따름.

### 5.4 장애 시퀀스 (개념)

```
[주문 요청]
    │
    ▼
[PreTrade Compliance] ── reject ──▶ [응답: ORDER_REJECTED]
    │ pass
    ▼
[OrderService.execute]
    │
    ├─ [KIS API timeout/5xx] ── retry(2) ── fail ──▶ [Circuit Breaker open] ──▶ Fallback Exception
    │
    └─ [200 + rt_cd=0] ──▶ [Order 저장] ──▶ [체결 확인 스케줄러] ──▶ [포지션 등록]
```

---

## 6. 모니터링 (Monitoring)

### 6.1 메트릭·헬스

| 항목 | 수단 | 용도 |
|------|------|------|
| **JVM/App** | Spring Actuator `health`, `metrics`, `prometheus` | 장애 탐지, 리소스 사용량 |
| **파이프라인 실행** | 로그 + (선택) 커스텀 메트릭 | 실행 시점, 계좌별, market/strategyType별 성공/스킵 |
| **주문** | TB_ORDERS 상태, 주문 수/거절 수 | 일별 주문 성공률, 컴플라이언스 거절 비율 |
| **리스크** | RiskEventAlertService (Discord) | 일일 손실 한도 임박, VaR 초과 |
| **미체결** | UnfilledOrderCheckScheduler + Discord | N분 미체결 시 알림 |
| **Governance** | TB_GOVERNANCE_CHECK_RESULT, TB_GOVERNANCE_HALT | 전략 열화, 자동 halt |

### 6.2 로깅·감사

- **민감 정보**: LogMaskingUtil로 계좌번호·userId·키 마스킹.
- **주문/거절**: AuditLogService 등으로 “누가, 언제, 어떤 주문을 실행/거절했는지” 추적.
- **전략 결정**: 파이프라인 실행 시 market, strategyType, 계좌, 권장 건수, autoExecute 여부 로그.

### 6.3 알림 채널 (Discord)

| 이벤트 | 채널(설정) | 담당 컴포넌트 |
|--------|------------|----------------|
| 매매 체결 | trade-webhook | DiscordEmergencyAlertService (TRADE) |
| 리스크(손실 한도, VaR 등) | risk-webhook | RiskEventAlertService |
| 시스템(미체결, 장애) | system-webhook | EmergencyAlertService, UnfilledOrderCheckScheduler |
| 거버넌스(전략 열화) | (설정) | StrategyGovernanceCheck 등 |

### 6.4 운영자 체크리스트

- **자동매매 전**: GET `/api/v1/ops/auto-trading-readiness` — 일봉/시그널 건수, 계좌 설정.
- **정합성**: ReconciliationService (TB_STRATEGY_POSITION vs 증권사 잔고), GET `/api/v1/ops/reconcile`.
- **Kill Switch**: GET/PUT `/api/v1/system/kill-switch` (ADMIN).
- **Governance**: GET `/api/v1/ops/governance/results`, `/halts`, halt 해제 API.

---

## 7. 리스크 관리 프레임워크 요약

| 구분 | 내용 |
|------|------|
| **Pre-Trade** | Kill Switch, 단일 종목 10% 상한, MDD 15% 게이트, 거래 설정(최대 투자금액) |
| **실행 전(파이프라인)** | RiskGate(VIX 등), 일일 손실 한도, 시장 급락 게이트, Governance Halt |
| **포지션/전략** | Half-Kelly, ATR 손절, Trailing Stop, Time-Cut, 리스크 기반 캡, 상관관계 패널티 |
| **사후** | Reconciliation, VaR/CVaR/Stress Test, Factor Decay 알림, 거버넌스 자동 검사 |

상세 수식·파라미터는 [00-strategy-registry.md](./00-strategy-registry.md) 참조.

---

## 8. 문서 변경 이력

| 버전 | 일자 | 변경 내용 |
|------|------|-----------|
| 1.0 | 2026-03-13 | 초안: 컴포넌트 다이어그램, 데이터 플로우, 장애 처리, 모니터링, 레이턴시, 리스크 요약 |
