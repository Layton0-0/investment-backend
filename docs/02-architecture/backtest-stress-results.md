# 백테스트 스트레스 검증

**목적**: 극단 구간에서 MDD·청산 규칙·리스크 게이트 동작을 검증하고 결과를 기록한다. [12-auto-investment-strategy.md §6](./12-auto-investment-strategy.md) 및 [02-development-status.md](../09-planning/02-development-status.md) 진행예정 반영.

---

## 1. 시나리오 정의

| 시나리오 | 구간 | 설명 | 요청 파라미터 (예시) |
|----------|------|------|----------------------|
| **코로나 폭락** | 2020-02-24 ~ 2020-04-30 | 2020년 3월 코로나 폭락장. MDD·손절·Trailing Stop 방어율 확인. | startDate: 2020-02-24, endDate: 2020-04-30, market: KR 또는 US, strategyType: SHORT_TERM, initialCapital: 100_000_000 |
| **금리 인상기** | 2022-01-03 ~ 2022-06-30 | 2022년 상반기 금리 인상·성장주 조정. 청산 규칙·리스크 게이트 동작 확인. | startDate: 2022-01-03, endDate: 2022-06-30, market: KR 또는 US, strategyType: SHORT_TERM 또는 MEDIUM_TERM, initialCapital: 100_000_000 |

- **실행 방법**: `POST /api/v1/backtest` (Body: BacktestRunRequest JSON). 인증 필요.
- **데이터 전제**: 해당 구간의 TB_DAILY_STOCK·TB_SIGNAL_SCORE가 수집·팩터 계산되어 있어야 유의미한 결과가 나온다. 데이터가 없으면 거래 0건·수익 곡선 평평할 수 있음.

---

## 2. 검증 기준

1. **MDD**: 정책 상한(-15%)을 넘지 않는지. (극단 구간에서는 일시 초과 가능성 있으나, 청산 규칙이 동작해 추가 낙폭이 제한되는지 확인.)
2. **청산 규칙**: 단기 -3% Trailing Stop, 중기 -10% 손절·Time-Cut 등이 기대대로 동작하는지(청산 사유·거래 목록 확인).
3. **리스크 게이트**: Kill Switch·일일 손실 한도 등이 설정대로 동작하는지(해당 기능이 백테스트 루프에 연동된 경우).

---

## 2.1 스트레스 구간 실행 방법

아래 두 구간(2020-02~04 코로나, 2022-01~06 금리 인상기)을 실행한 뒤 **§3.1**·**§3.2** 표에 결과를 기입한다.

### API로 단건 실행

`POST /api/v1/backtest` (인증 필요). Body 예시:

| 구간 | startDate | endDate | market | strategyType | initialCapital |
|------|-----------|---------|--------|--------------|----------------|
| **코로나 (2020-02~04)** | 2020-02-24 | 2020-04-30 | KR 또는 US | SHORT_TERM | 100000000 |
| **금리 인상기 (2022-01~06)** | 2022-01-03 | 2022-06-30 | KR 또는 US | SHORT_TERM 또는 MEDIUM_TERM | 100000000 |

예: 코로나 KR  
`{ "startDate": "2020-02-24", "endDate": "2020-04-30", "market": "KR", "strategyType": "SHORT_TERM", "initialCapital": 100000000 }`

### run-backtest.ps1 (프로젝트 루트)

단일 백테스트만 실행할 때:

```powershell
# 코로나 구간
.\scripts\run-backtest.ps1 -StartDate "2020-02-24" -EndDate "2020-04-30" -Market "KR" -StrategyType "SHORT_TERM" -BaseUrl "http://localhost:8080"

# 금리 인상기 구간
.\scripts\run-backtest.ps1 -StartDate "2022-01-03" -EndDate "2022-06-30" -Market "US" -StrategyType "SHORT_TERM" -BaseUrl "http://localhost:8080"
```

(인증이 필요한 API면 스크립트에 토큰 전달이 필요할 수 있음. 전체 파이프라인은 아래 run-stress-backtest.ps1 사용.)

### run-stress-backtest.ps1 (백필 + 팩터 + 백테스트 4건)

백엔드·DB·Redis 기동 후, 백필·팩터 계산·백테스트 4건을 한 번에 실행하고 JSON으로 저장:

```powershell
cd investment-backend
.\scripts\run-stress-backtest.ps1 -BaseUrl "http://localhost:8080" -EnvPath ".\.env" -OutJsonPath ".\docs\02-architecture\stress-backtest-results.json"
```

- **결과 기입 위치**  
  - **코로나 (2020-02-24 ~ 2020-04-30)** → **§3.1** 표 (MDD %, CAGR %, 청산 횟수, 거래 수, 이슈·비고).  
  - **금리 인상기 (2022-01-03 ~ 2022-06-30)** → **§3.2** 표.  
- JSON의 `backtestResults` 배열에서 시나리오별 `mddPct`, `cagr`, `tradeCount`, `trades` 등을 복사해 해당 표에 붙여넣는다.

---

## 3. 실행 결과 (데이터 수집 후 기입)

아래는 해당 구간 데이터로 백테스트를 실행한 뒤 메트릭·비고를 기입하는 영역이다. **실제 수치 기입 방법**: `investment-backend/scripts/run-stress-backtest.ps1` 실행 후 생성되는 `docs/02-architecture/stress-backtest-results.json`의 `backtestResults`를 참고하여 본 표를 채운다. (백엔드·DB·Redis 기동 및 KRX/US 데이터 수집 환경 필요.) 기입 완료 후 [roadmap.md Phase 5.2](../roadmap.md) "백테스트 스트레스 검증" 항목을 [x] 처리한다.

### 3.1 코로나 폭락 (2020-02-24 ~ 2020-04-30)

| 항목 | 값 | 비고 |
|------|-----|------|
| 실행일 | 2026-02-20 | 데이터 부재 상태 점검 |
| market | KR / US | |
| strategyType | SHORT_TERM | |
| MDD % | — | 데이터 부재 |
| CAGR % | — | 데이터 부재 |
| 청산 횟수 | — | 데이터 부재 |
| 거래 수 | 0 | 데이터 부재 |
| 이슈·비고 | **구간 내 일봉/시그널 없음.** TB_DAILY_STOCK 해당 구간 0건 확인 (2026-02-20). KRX/US 백필 API(`POST /api/v1/trigger/krx-daily-backfill`, `us-daily-backfill`)로 데이터 수집 후 재실행 필요. |

### 3.2 금리 인상기 (2022-01-03 ~ 2022-06-30)

| 항목 | 값 | 비고 |
|------|-----|------|
| 실행일 | 2026-02-20 | 데이터 부재 상태 점검 |
| market | KR / US | |
| strategyType | SHORT_TERM / MEDIUM_TERM | |
| MDD % | — | 데이터 부재 |
| CAGR % | — | 데이터 부재 |
| 청산 횟수 | — | 데이터 부재 |
| 거래 수 | 0 | 데이터 부재 |
| 이슈·비고 | **구간 내 일봉/시그널 없음.** TB_DAILY_STOCK 해당 구간 0건 확인 (2026-02-20). KRX/US 백필 API로 데이터 수집 후 재실행 필요. |

---

## 4. 데이터 점검 방법

스트레스 구간 실행 전 해당 시장·기간 데이터 유무를 확인할 수 있다.

- **원천별 최근 기준일**: Admin 전용 `GET /api/v1/ops/data-pipeline/status` 응답의 KR/US `latestBasDt`(또는 동일 정보)로 각 시장 최근 수집 일자를 확인. 2020-02~04, 2022-01~06 구간이 포함되려면 과거 일봉 수집(백필) 또는 수동 수집이 선행되어야 함.
- **DB 직접 확인(운영자)**: `TB_DAILY_STOCK`에서 `MARKET='KR'` 또는 `'US'`, `BAS_DT` BETWEEN 시나리오 구간으로 건수 조회. `TB_SIGNAL_SCORE` 동일 구간·시장 조회. 0건이면 백테스트 실행 시 거래 0건·평평한 수익 곡선이 나오므로, 결과 표에 "데이터 부재"로 기입.
- **실행 순서**: 데이터 확보 후 인증된 사용자가 `POST /api/v1/backtest` (Body: startDate, endDate, market, strategyType, initialCapital) 호출 → 응답 메트릭(MDD·CAGR·청산 횟수·거래 수)을 §3.1·§3.2 표에 기입.

---

## 5. 스트레스 구간 데이터 백필

해당 구간 TB_DAILY_STOCK이 없을 때 **과거 일봉 백필** 후 팩터 계산을 실행하면 스트레스 백테스트가 가능하다.

### 5.1 백필 API (인증 필요)

| 트리거 | 메서드 | 쿼리 파라미터 | 설명 |
|--------|--------|----------------|------|
| KRX 일별 백필 | POST /api/v1/trigger/krx-daily-backfill | from (yyyy-MM-dd), to (yyyy-MM-dd) | 해당 기간 KRX 일별 시세 수집 → TB_DAILY_STOCK (MARKET=KR) |
| US 일별 백필 | POST /api/v1/trigger/us-daily-backfill | from (yyyy-MM-dd), to (yyyy-MM-dd) | 해당 기간 US 일별 시세 수집 → TB_DAILY_STOCK (MARKET=US) |

- **KR**: KRX Open API 인증키(`investment.data.krx.auth-key`) 설정 필요. [04-krx-api-required.md](../08-setup-guides/04-krx-api-required.md) 참조.
- **US**: yfinance 스크립트 경로(`investment.data.us.yfinance-script-path`) 등 US 수집 설정 필요. [02-development-status.md](../09-planning/02-development-status.md) US 일별 수집 항목 참조.

### 5.2 백필 후 팩터 계산

일봉만 저장하면 시그널이 없으므로, 백필 직후 **팩터 계산**을 실행해 TB_SIGNAL_SCORE를 채운다.

- `POST /api/v1/trigger/factor-calculation` (기준일 없이 실행 시 최근 일자 기준. 구간 백필만 했을 경우 해당 구간 일봉이 DB에 있으면 팩터 계산 시 그 일자들을 포함할 수 있음. 구현에 따라 전체 재계산 또는 기간 지정 여부는 코드 확인.)

### 5.3 스크립트 사용 예 (PowerShell)

**권장 스크립트**: `investment-backend/scripts/run-stress-backtest.ps1` — 로그인 → 코로나/금리 인상기 KR·US 백필(4회) → 팩터 계산 → 백테스트 4건 실행 후 결과를 JSON으로 저장.

- **사용법**: Backend·DB·Redis 기동 후 실행.
  ```powershell
  cd investment-backend
  .\scripts\run-stress-backtest.ps1 -BaseUrl "http://localhost:8080" -EnvPath ".\.env" -OutJsonPath ".\docs\02-architecture\stress-backtest-results.json"
  ```
  - `-Username`, `-Password` 생략 시 `.env`의 `SUPER_ADMIN_USERNAME`, `SUPER_ADMIN_PASSWORD` 사용. 또는 `STRESS_TEST_USER`, `STRESS_TEST_PASSWORD` 환경변수.
- **출력**: `stress-backtest-results.json`의 `backtestResults` 배열에 시나리오별 `mddPct`, `cagr`, `tradeCount`, `trades` 등 포함. 이 값을 §3.1·§3.2 표에 기입.
- **순서**: 스크립트 실행 → §4로 데이터 점검(선택) → §3 표 기입 → [roadmap.md Phase 5.2](../roadmap.md) 해당 항목 [x] 처리.

---

## 6. Phase 1~3 워크포워드 검증 (최근 1년)

**목적**: Phase 1~3(Universe·시그널·레짐·포트폴리오·실행) 완료 후, 워크포워드(롤링 OOS) 백테스트로 전략 성능을 검증한다. [P8-1] 전략 엔진 통합 테스트.

### 6.1 목표 지표

| 지표 | 목표 | 비고 |
|------|------|------|
| CAGR | ≥ 20% | 연평균 복합 수익률 |
| MDD | ≥ -15% | 최대 낙폭(음수). -15% 이내로 통제 |
| Sharpe | ≥ 1.0 | fold 중 최소 Sharpe (minSharpeRatio) |
| 팩터별 | 단독 Sharpe > 0 | 각 팩터 단독 성과 양수 유지 (선택 검증) |

### 6.2 실행 방법

- **API**: `POST /api/v1/backtest/walk-forward` (인증 필요).
- **요청 본문 (WalkForwardBacktestRequest)**: `startDate`, `endDate`, `market` (KR/US), `strategyType` (SHORT_TERM/MEDIUM_TERM/LONG_TERM), `initialCapital` (필수). `trainDays` (기본 252), `testDays` (기본 63), `stepDays` (기본 63).
- **최근 1년 예시**: `startDate`: (오늘 - 1년), `endDate`: (오늘), `trainDays`: 252, `testDays`: 63, `stepDays`: 63.

### 6.3 실행 결과 (데이터 수집·실행 후 기입)

| 항목 | KR SHORT_TERM | KR MEDIUM_TERM | US SHORT_TERM | US MEDIUM_TERM | 비고 |
|------|----------------|----------------|---------------|----------------|------|
| 실행일 | — | — | — | — | |
| foldCount | — | — | — | — | |
| avgCagr % | — | — | — | — | 목표 ≥ 20 |
| avgMddPct % | — | — | — | — | 목표 ≥ -15 |
| minSharpeRatio | — | — | — | — | 목표 ≥ 1.0 |
| 이슈·비고 | | | | | TB_DAILY_STOCK·TB_SIGNAL_SCORE 해당 구간 확보 후 실행 |

- **검증 완료 시**: [02-development-status.md](../09-planning/02-development-status.md) §1 완료에 P8-1 항목 추가, [00-strategy-registry.md](./00-strategy-registry.md) 버전 스택에 결과·교훈 반영.

---

## 7. 참조

- [.cursor/rules/backtest-quant-research-standards.mdc](../../../.cursor/rules/backtest-quant-research-standards.mdc) — 퀀트 백테스트 표준(통계 타당성, look-ahead/survivorship 방지, 슬리피지·거래비용, **필수 5종 메트릭**: CAGR·Sharpe·MDD·win rate·profit factor, 재현 가능 연구).
- [00-strategy-registry.md §1.1](./00-strategy-registry.md) — 데이터·백테스트 원칙, §7 버전 스택
- [02-development-status.md](../09-planning/02-development-status.md) — 완료·진행예정
- [02-api-endpoints.md §백테스트](../04-api/02-api-endpoints.md) — POST /api/v1/backtest 스펙
- [02-development-status.md §3 진행예정](../09-planning/02-development-status.md) — 스트레스 구간 데이터 수집 후 결과 기입 태스크
- [02-api-endpoints.md §트리거 API](../04-api/02-api-endpoints.md) — krx-daily-backfill, us-daily-backfill
