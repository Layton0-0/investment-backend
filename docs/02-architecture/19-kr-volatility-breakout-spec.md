# KR 단기 전략 스펙: 변동성 돌파 (Volatility Breakout)

**역할**: 한국(KRX) 시장 단기 자동매매용 변동성 돌파 전략의 단일 페이지 구현 명세.  
**참조**: [00-strategy-registry.md](00-strategy-registry.md) §2.3·§3.2·§3.3, [18-kr-short-term-strategies-top10.md](18-kr-short-term-strategies-top10.md) §1.

---

## 1. 전략명 및 개요

| 항목 | 내용 |
|------|------|
| **전략 ID** | `kr-volatility-breakout` |
| **이름** | 변동성 돌파 (Volatility Breakout) |
| **시장** | KR (한국) |
| **기간** | 단기 (SHORT_TERM) |
| **가설** | 전일 변동폭(Range)의 k배만큼 당일 시가 위를 돌파할 때 모멘텀 지속 가능성이 높다. 진입을 Range×k로 제한해 가짜 돌파를 완화한다. |

---

## 2. 진입 조건 (Entry)

### 2.1 수식

- **목표가 (진입 트리거)**  
  \[
  \text{Target} = \text{Open}_t + (\text{Range}_{t-1} \times k)
  \]
- **Range**  
  \[
  \text{Range}_{t-1} = \text{High}_{t-1} - \text{Low}_{t-1}
  \]
  (전일 고가 − 전일 저가, 수정주가 기준)

- **진입**: 당일 시가(Open_t) 이후 **당일 고가 또는 체결가**가 Target 이상이 되면 매수(또는 시뮬레이션상 해당 일자 진입).

### 2.2 파라미터

| 파라미터 | 설명 | 기본값 | 범위/비고 |
|----------|------|--------|-----------|
| `k` | 돌파 비율 | 0.5 | [0.3, 0.7]; 고정 또는 동적 |
| `k-dynamic` | 한국장 변동성 기반 k 조정 사용 여부 | true | true 시 최근 5일/20일 변동폭 비율로 k 조정 |
| `k-min` | k 하한 | 0.3 | 동적 k 시 클리핑 |
| `k-max` | k 상한 | 0.7 | 동적 k 시 클리핑 |

- **유니버스**: KR 단기 공통 규칙 적용 — 유동성(5일 평균 거래대금 ≥ 임계값), 필요 시 시초가 유동성(예: 300억) 필터. ([00-strategy-registry.md](00-strategy-registry.md) §3.1)

### 2.3 PIT (Point-in-Time)

- **T일 진입 판단** 시 사용 가능 데이터: T일 시가·T일 현재(또는 T일 고가), T−1일 High/Low/Open/Close.
- **T일 종가/Volume** 는 T일 진입·청산 판단에 사용하지 않는다 (look-ahead 방지).

---

## 3. 청산 규칙 (Exit)

| 규칙 | 조건 | 적용 순서 |
|------|------|-----------|
| **트레일링 스탑** | 현재가 ≤ trailingHigh × (1 − trailingPct/100) | 1 |
| **고정 손절 (KR)** | 현재가 ≤ entryPrice × (1 − stopLossPct/100) | 2 |
| **RSI 익절** | RSI(14) ≥ rsiExitThreshold | 3 |
| **Time-Cut** | 매수 후 holdDays 이내 목표 수익률 미도달 시 전량 매도 | 4 |
| **당일 청산 옵션** | 보유 기간 = 1일 (당일 매수 → 당일 종가 매도) | 설계 선택 |

| 파라미터 | 설명 | 기본값 |
|----------|------|--------|
| `trailingPct` | 트레일링 스탑 (%) | 3 |
| `stopLossPct` | 고정 손절 (%) | 5 |
| `rsiExitThreshold` | RSI 익절 | 70 |
| `holdDays` | Time-Cut 보유 일수 | 설정값 (예: 5) |
| `targetReturnPct` | Time-Cut 목표 수익률 (%) | 설정값 (예: 5) |

- 청산은 **동시에 여러 조건이 만족되면** 위 순서대로 먼저 만족한 규칙 적용. 실거래 시 매일(또는 장중 주기) 평가.

---

## 4. 백테스트 체크리스트

구현·검증 시 아래를 필수로 준수한다.

### 4.1 데이터

- [ ] **PIT**: 매 거래일(bas_dt) 기준, 해당 일자 **종료 시점까지 가용한** 데이터만 사용. 당일 종가/거래량으로 당일 진입 판단 금지.
- [ ] **수정주가**: 일봉(Open/High/Low/Close)·Range·진입/청산 가격 모두 **수정주가**.
- [ ] **Survivorship**: 유니버스는 백테스트 일자별 상장 종목만 사용; 상장폐지·델리스트 반영 여부 문서화.

### 4.2 비용

- [ ] **수수료**: 한국 주식 왕복(매수+매도) 수수료 반영.
- [ ] **세금**: 매도 시 증권거래세 반영.
- [ ] **슬리피지**: 매수/매도 각각 또는 왕복 슬리피지 비율 적용 (예: 0.1% 단측 또는 FrictionCostProperties 기준).

### 4.3 필수 메트릭

백테스트 결과에 다음 5종을 **반드시** 포함한다.

| 메트릭 | 설명 |
|--------|------|
| **CAGR** | 연평균 복합 수익률 (%) |
| **Sharpe** | 일수익률 기준 Sharpe (무위험 0 가정, 연율화 √252) |
| **MDD** | 최대 낙폭 (%) — peak 대비 최대 하락률 |
| **Win rate** | 승리 거래 수 / 전체 거래 수 |
| **Profit factor** | 총 이익 / \|총 손실\| |

### 4.4 기타

- [ ] **시드·기간·초기자본·수수료/슬리피지 설정·전략 버전** 기록 (재현 가능성).
- [ ] **스트레스 구간** 검증: 예) 2020-02~04, 2022-01~06 등 극단 구간에서 MDD·승률·Profit factor 확인.
- [ ] **거버넌스**: 정기 백테스트 재실행; MDD/Sharpe 열화 시 중단·검토 정책 적용.

---

## 5. 설정·코드 참조

- **설정**: `investment.factor.volatility-breakout-k`, `volatility-breakout-k-dynamic`, `volatility-breakout-k-min`, `volatility-breakout-k-max`  
- **청산**: `investment.pipeline.short-term-trailing-pct`, `short-term-kr-stop-loss-pct`, `rsi-exit-threshold`, Time-Cut(엔티티: timeCutDays, targetReturnPct)  
- **서비스**: `FactorCalculationService.addVolatilityBreakout`, `getVolatilityBreakoutK`, `calculateDynamicK`; `ExitRuleService` (trailing, 고정 손절, RSI, Time-Cut)

---

*문서 버전: 1.0 | 일자: 2026-03-13 | Ed Thorp 스타일: edge, clarity, backtest rigor.*
