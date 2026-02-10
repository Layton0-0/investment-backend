# S04 — 국내 전략

## 목적

국내(KOSPI/KOSDAQ) 단기/중기/장기 전략 조회·상태 변경·성과 확인.

## 정보 구조

- 계좌: 모의계좌(serverType=1) 자동 사용. 미등록 시 빈 상태 + 설정 링크.
- 전략 목록: 단기/중기/장기별 상태(ACTIVE/STOPPED/PAUSED), 비중, 최대/최소 금액, 성과 지표.
- 유니버스/시그널 요약(API 제공 시): 주도주 유니버스, 수급 점수 등.

## 주요 상호작용

- 전략 활성화/중지 → PUT /api/v1/strategies/{accountNo}/{strategyType}/status 또는 activate/stop.
- 전략 생성/수정 → POST/PUT /api/v1/strategies, market=KR.

## 상태/에러

- Empty: 모의계좌 미등록.
- Error: API 실패.

## 권한

- User, Admin: 읽기/쓰기(본인 계좌).

## 연동 API

- GET/POST/PUT /api/v1/strategies, market=KR. GET /api/v1/strategies/{accountNo}/{strategyType}?market=KR.
