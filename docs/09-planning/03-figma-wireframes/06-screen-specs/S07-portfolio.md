# S07 — 포트폴리오

## 목적

보유 종목·일별 트레이딩 포트폴리오(추천 종목) 조회.

## 정보 구조

- 보유 종목 테이블: 종목, 시장, 수량, 평균가, 현재가, 평가금액, 평가손익 등.
- 트레이딩 포트폴리오: 거래일 선택, 추천 종목 목록(진입가, 손절가, 목표가).

## 주요 상호작용

- 거래일 선택 → GET /api/v1/trading-portfolios/{date}.
- 보유 종목은 계좌 API 연동(serverType에 따른 메인 계좌).

## 상태/에러

- Empty: 보유 0건·해당일 포트폴리오 없음.
- Error: API 실패.

## 권한

- User, Ops: 읽기.

## 연동 API

- GET /api/v1/accounts/{accountNo}/positions, GET /api/v1/trading-portfolios/{date}.
