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

## 3. 실행 결과 (데이터 수집 후 기입)

아래는 해당 구간 데이터로 백테스트를 실행한 뒤 메트릭·비고를 기입하는 영역이다.

### 3.1 코로나 폭락 (2020-02-24 ~ 2020-04-30)

| 항목 | 값 | 비고 |
|------|-----|------|
| 실행일 | (실행 후 기입) | |
| market | KR / US | |
| strategyType | SHORT_TERM | |
| MDD % | | |
| CAGR % | | |
| 청산 횟수 | | |
| 거래 수 | | |
| 이슈·비고 | 해당 구간 TB_DAILY_STOCK·TB_SIGNAL_SCORE 수집 후 POST /api/v1/backtest로 실행하여 위 값을 기입. 데이터 부재 시 거래 0건·수익 곡선 평평·"구간 내 일봉/시그널 없음" 기록. | |

### 3.2 금리 인상기 (2022-01-03 ~ 2022-06-30)

| 항목 | 값 | 비고 |
|------|-----|------|
| 실행일 | (실행 후 기입) | |
| market | KR / US | |
| strategyType | SHORT_TERM / MEDIUM_TERM | |
| MDD % | | |
| CAGR % | | |
| 청산 횟수 | | |
| 거래 수 | | |
| 이슈·비고 | 동일. 해당 구간 데이터 수집 후 백테스트 실행·결과 기입. 데이터 부재 시 위와 동일 처리. | |

---

## 4. 데이터 점검 방법

스트레스 구간 실행 전 해당 시장·기간 데이터 유무를 확인할 수 있다.

- **원천별 최근 기준일**: Admin 전용 `GET /api/v1/ops/data-pipeline/status` 응답의 KR/US `latestBasDt`(또는 동일 정보)로 각 시장 최근 수집 일자를 확인. 2020-02~04, 2022-01~06 구간이 포함되려면 과거 일봉 수집(백필) 또는 수동 수집이 선행되어야 함.
- **DB 직접 확인(운영자)**: `TB_DAILY_STOCK`에서 `MARKET='KR'` 또는 `'US'`, `BAS_DT` BETWEEN 시나리오 구간으로 건수 조회. `TB_SIGNAL_SCORE` 동일 구간·시장 조회. 0건이면 백테스트 실행 시 거래 0건·평평한 수익 곡선이 나오므로, 결과 표에 "데이터 부재"로 기입.
- **실행 순서**: 데이터 확보 후 인증된 사용자가 `POST /api/v1/backtest` (Body: startDate, endDate, market, strategyType, initialCapital) 호출 → 응답 메트릭(MDD·CAGR·청산 횟수·거래 수)을 §3.1·§3.2 표에 기입.

---

## 5. 참조

- [00-strategy-registry.md §1.1](./00-strategy-registry.md) — 데이터·백테스트 원칙
- [02-development-status.md](../09-planning/02-development-status.md) — 완료·진행예정
- [02-api-endpoints.md §백테스트](../04-api/02-api-endpoints.md) — POST /api/v1/backtest 스펙
- [02-development-status.md §3 진행예정](../09-planning/02-development-status.md) — 스트레스 구간 데이터 수집 후 결과 기입 태스크
