# S10 — 백테스트

## 목적

4단계 파이프라인 백테스트·로보 어드바이저 백테스트 실행·결과 확인. 실행 전 백테스트(로보 리밸런싱 직전 검증) 노출.

## 정보 구조

- 모드 선택: "4단계 파이프라인 백테스트" | "로보어드바이저 백테스트".
- **4단계**: 기간·시장·전략·초기자본 입력 → [실행]. 결과: 메트릭(MDD·CAGR·Sharpe·Sortino·Calmar·승률·손익비), 수익 곡선, 거래 목록.
- **로보**: 간단(기간·초기자본) + 고급(모멘텀 기간·MA·Top N·리밸런싱 주기·수수료/슬리피지). 결과: 한 줄 해석, 메트릭(CAGR·MDD·Sharpe·Calmar·Turnover·벤치마크), 수익 곡선 vs 벤치마크, 리밸런싱 이력.
- (선택) GET /api/v1/backtest/robo/last-pre-execution?accountNo=xxx → 실행 전 백테스트 결과 요약.

## 주요 상호작용

- [실행] → POST /api/v1/backtest 또는 POST /api/v1/backtest/robo. 응답으로 결과 렌더링.

## 상태/에러

- Loading(실행 중), Empty(실행 전), Error(API 실패·warningMessage 표시).

## 권한

- User, Admin: 읽기·실행.

## 연동 API

- POST /api/v1/backtest, POST /api/v1/backtest/robo, GET /api/v1/backtest/robo/last-pre-execution.
