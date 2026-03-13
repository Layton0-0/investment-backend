# 백테스트 구현 검증 보고서 (Quant-Backtest Agent)

**검증 일자**: 2026-03-13  
**범위**: 비용 반영, 필수 메트릭, 스트레스 구간 문서화

---

## 1. 비용 (Costs)

### 1.1 요구사항

- 매수·매도 시 **수수료(commission)**·**슬리피지(slippage)** 차감
- KR 매도 시 **세금(tax)** 적용
- 결과 DTO에 **거래당 마찰비용** 및 **전체 합계(aggregate)** 노출

### 1.2 검증 결과

| 항목 | 상태 | 비고 |
|------|------|------|
| 매수 시 commission + slippage 차감 | ✅ 충족 | `BacktestService.computeBuyFrictionCost()` |
| 매도 시 commission + tax(KR) / secFee(US) + slippage | ✅ 충족 | `BacktestService.computeSellFrictionCost()` |
| US 매도 TAF | ✅ 충족 | `BacktestService.computeTafCost()` |
| PnL에 마찰비용 반영 | ✅ 충족 | `pnl = exitValue - cost - totalFriction` |
| 거래당 totalFrictionCost | ✅ 충족 | `BacktestTradeDto.totalFrictionCost` |
| **전체 합계 totalFrictionCost** | ✅ 보완 완료 | `BacktestRunResult.totalFrictionCost` 추가·집계 |

### 1.3 적용한 수정

- **파일**: `BacktestRunResult.java`  
  - 필드 추가: `totalFrictionCost` (전체 거래 마찰비용 합계, 거래 없으면 0).
- **파일**: `BacktestService.java`  
  - 집계: `trades`의 `totalFrictionCost` 합산 후 result builder에 설정.
- **테스트**: `BacktestServiceTest.run_noTrades_totalFrictionCostIsZero()`  
  - 거래 0건일 때 `result.getTotalFrictionCost()`가 0임을 단언.

---

## 2. 필수 메트릭 (Required Metrics)

### 2.1 요구사항

BacktestRunResult(또는 동등 DTO)에 최소 포함: **cagr**, **sharpeRatio**, **mddPct**, **winRate**, **profitFactor**, **trade count**.

### 2.2 검증 결과

| 메트릭 | BacktestRunResult 필드 | 상태 |
|--------|------------------------|------|
| CAGR | `cagr` | ✅ |
| Sharpe ratio | `sharpeRatio` | ✅ |
| Max drawdown % | `mddPct` | ✅ |
| Win rate | `winRate` | ✅ |
| Profit factor | `profitFactor` | ✅ |
| Trade count | `tradeCount` | ✅ |

추가로 sortinoRatio, calmarRatio, avgWin, avgLoss, winningTrades, losingTrades 등도 노출됨.

### 2.3 테스트

- `BacktestServiceTest.run_noRecommendations_finalEquityEqualsInitial()` 확장  
  - `cagr`, `mddPct` not null 및 거래 없을 때 `winRate`/`profitFactor` null 단언으로 필수 메트릭 존재 검증.

---

## 3. 스트레스 구간 (Stress Periods)

### 3.1 요구사항

- 2020-02~04(코로나), 2022-01~06(금리 인상기) 실행 방법 문서화
- API body 또는 run-backtest.ps1 파라미터 명시
- 결과 기입 위치(§3.1, §3.2 표) 명시

### 3.2 검증 결과

- **문서**: `backtest-stress-results.md`에 **§2.1 스트레스 구간 실행 방법** 추가.
- **내용**:
  - **API**: `POST /api/v1/backtest` Body 예시( startDate, endDate, market, strategyType, initialCapital ) 표로 정리.
  - **run-backtest.ps1**: 프로젝트 루트에서 `-StartDate`, `-EndDate` 등 예시 명시.
  - **run-stress-backtest.ps1**: 백필·팩터·백테스트 4건 실행 후 JSON 저장 방법 및 `-OutJsonPath` 안내.
  - **결과 기입 위치**: 코로나 → **§3.1** 표, 금리 인상기 → **§3.2** 표에 `mddPct`, `cagr`, `tradeCount` 등 붙여넣기 안내.

---

## 4. 변경 파일 및 테스트 요약

| 구분 | 파일 | 변경 내용 |
|------|------|-----------|
| DTO | `BacktestRunResult.java` | `totalFrictionCost` 필드 추가 |
| 서비스 | `BacktestService.java` | 집계 계산 및 builder에 `totalFrictionCost` 설정 |
| 테스트 | `BacktestServiceTest.java` | `run_noTrades_totalFrictionCostIsZero()` 추가, `run_noRecommendations_...`에 필수 메트릭 단언 추가 |
| 문서 | `backtest-stress-results.md` | §2.1 스트레스 구간 실행 방법 추가 |
| API 문서 | `02-api-endpoints.md` | BacktestRunResult 응답에 `totalFrictionCost` 설명 추가 |

**추가/확장된 테스트**

- `BacktestServiceTest.run_noTrades_totalFrictionCostIsZero` — 거래 없을 때 집계 마찰비용 0
- `BacktestServiceTest.run_noRecommendations_finalEquityEqualsInitial` — cagr, mddPct, tradeCount 등 필수 메트릭 존재 검증

---

## 5. 결론

- **(1) 비용**: 매수/매도 시 commission·slippage·tax(KR)·TAF(US) 반영 완료. 거래당·**전체 합계** 마찰비용 모두 DTO에 노출되도록 보완함.
- **(2) 필수 메트릭**: cagr, sharpeRatio, mddPct, winRate, profitFactor, tradeCount 모두 BacktestRunResult에 포함되어 있으며, 단위 테스트로 존재 여부를 검증함.
- **(3) 스트레스 구간**: 2020-02~04, 2022-01~06 실행 방법(API body, run-backtest.ps1, run-stress-backtest.ps1)과 결과 기입 위치(§3.1, §3.2)를 backtest-stress-results.md에 명시함.
