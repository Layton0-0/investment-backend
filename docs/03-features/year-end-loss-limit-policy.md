# 연말 손실 한도 정책 (Year-End Loss Limit Policy)

> **문서 상태**: 1.0 (2026-02-20)  
> **분류**: 기능 명세  
> **관련 코드**: `RiskReportService`, `RiskProperties`, `RiskLimitsDto`

---

## 1. 개요

연말 손실 한도 정책은 포트폴리오의 연간 최대 손실을 관리하기 위한 리스크 통제 메커니즘입니다.

### 1.1 목적
- 연간 누적 손실이 설정된 한도를 초과하지 않도록 보호
- 세금 최적화를 위한 손실 실현 타이밍 지원
- VaR/CVaR 기반 리스크 예측 및 조기 경고

---

## 2. VaR (Value at Risk) 방법론

### 2.1 파라메트릭 VaR (현재 구현)

```
VaR 95% = 1.65 × σ (일일 변동성)
CVaR 95% = 2.06 × σ (일일 변동성)
```

- **설정**: `investment.risk.var-daily-vol-pct`
- **장점**: 단순, 빠른 계산
- **단점**: 정규분포 가정, 극단 이벤트 과소평가 가능

### 2.2 역사적 VaR (Historical VaR) - 확장 구현

```
VaR 95% = 과거 N일 수익률 분포의 5번째 백분위수
CVaR 95% = 5번째 백분위수 이하 수익률의 평균
```

- **설정**: `investment.risk.var-method` = `HISTORICAL`
- **룩백 기간**: `investment.risk.var-lookback-days` (기본 252 거래일)
- **장점**: 실제 분포 반영, 팻테일 포착
- **단점**: 과거 데이터 의존, 데이터 부족 시 부정확

### 2.3 방법론 선택 기준

| 상황 | 권장 방법 |
|------|----------|
| 시스템 초기/데이터 부족 | 파라메트릭 |
| 1년 이상 일봉 데이터 보유 | 역사적 |
| 극단 이벤트 대비 중요 | 역사적 |
| 빠른 계산 필요 | 파라메트릭 |

---

## 3. 연말 손실 한도 정책

### 3.1 연간 손실 한도 (Year-End Loss Limit)

```
연간 손실 한도 = 연초 포트폴리오 가치 × 연간손실한도비율
```

| 설정 항목 | 속성명 | 기본값 | 설명 |
|----------|--------|--------|------|
| 연간 손실 한도 비율 | `year-end-loss-limit-pct` | 20% | 연초 대비 최대 허용 손실 |
| 알림 임계값 | `year-end-alert-threshold-pct` | 80% | 한도의 80% 도달 시 알림 |

### 3.2 단계별 대응

| 단계 | 조건 | 조치 |
|------|------|------|
| 1단계 (경고) | 연간 손실 ≥ 한도의 50% | Discord 알림 |
| 2단계 (주의) | 연간 손실 ≥ 한도의 80% | 신규 매수 비중 50% 축소 |
| 3단계 (차단) | 연간 손실 ≥ 한도 100% | 신규 매수 완전 중단 |

### 3.3 세금 최적화 연동 (Tax-Loss Harvesting)

연말 손실 한도 정책은 세금 리포트와 연동하여:
- 손실 실현 시점 권고
- 기본공제(250만원) 내 손익 조정 제안
- Wash Sale Rule 위반 방지 (30일 재매수 금지)

---

## 4. 설정 속성 (RiskProperties)

### 4.1 현재 구현

```yaml
investment:
  risk:
    regime-gate-enabled: false          # 레짐 게이트 사용
    vix-threshold: 30                   # VIX 임계값
    reduce-size-on-high-vol-pct: 50     # 고변동성 시 축소 비율
    daily-loss-limit-pct: 5             # 일일 손실 한도 %
    var-daily-vol-pct: 1.0              # VaR 일일 변동성 가정 %
    alert-mdd-threshold-pct: 0.8        # MDD 알림 임계값
    alert-var-exceed-enabled: true      # VaR 초과 알림
```

### 4.2 확장 속성 (이번 구현)

```yaml
investment:
  risk:
    # 기존 속성...
    
    # VaR 방법론 (PARAMETRIC | HISTORICAL)
    var-method: PARAMETRIC
    
    # 역사적 VaR 룩백 기간 (거래일)
    var-lookback-days: 252
    
    # 연간 손실 한도 비율 (%)
    year-end-loss-limit-pct: 20
    
    # 연간 손실 한도 알림 임계값 (0~1)
    year-end-alert-threshold-pct: 0.8
```

---

## 5. API 확장

### 5.1 GET /api/v1/risk/limits

응답에 추가:
```json
{
  "regimeGateEnabled": false,
  "vixThreshold": 30,
  "reduceSizeOnHighVolPct": 50,
  "dailyLossLimitPct": 5,
  "varMethod": "PARAMETRIC",
  "varLookbackDays": 252,
  "yearEndLossLimitPct": 20,
  "yearEndAlertThresholdPct": 0.8
}
```

### 5.2 GET /api/v1/risk/summary

응답에 추가:
```json
{
  "...": "기존 필드",
  "yearEndLossStatus": {
    "yearStartValue": 100000000,
    "currentValue": 85000000,
    "lossAmount": 15000000,
    "lossPct": 15.0,
    "limitPct": 20.0,
    "thresholdReached": false,
    "stage": "WARNING"
  }
}
```

---

## 6. 제한 및 면책

### 6.1 제한 사항
- 역사적 VaR은 최소 60거래일 데이터 필요
- 실시간 가격 아닌 종가 기준 계산
- 레버리지 상품(ELW, 선물옵션) 미지원

### 6.2 면책 조항
> 본 시스템의 VaR/CVaR 및 손실 한도 계산은 참고용이며, 투자 손실에 대한 책임을 지지 않습니다. 실제 투자 결정은 전문 자문을 받으시기 바랍니다.

---

## 7. 후속 개발 계획

| Phase | 기능 | 우선순위 |
|-------|------|----------|
| 1 | 역사적 VaR 옵션 추가 | 이번 구현 |
| 2 | 연간 손실 한도 알림 | 후속 |
| 3 | Tax-Loss Harvesting 연동 | 후속 |
| 4 | Monte Carlo VaR | 선택적 |

---

## 변경 이력

| 버전 | 일자 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-02-20 | System | 초기 문서 작성 |
