# S05 — 미국 전략

## 목적

미국(NYSE/NASDAQ) 단기/중기/장기 전략 조회·상태 변경·성과 확인.

## 정보 구조

- 계좌: 실계좌(serverType=0) 자동 사용. 미등록 시 빈 상태 + 설정 링크.
- 전략 목록: S04와 동일 구조, market=US.

## 주요 상호작용

- S04와 동일, market=US.

## 상태/에러

- S04와 동일.

## 권한

- User, Ops: 읽기/쓰기(본인 계좌).

## 연동 API

- GET/POST/PUT /api/v1/strategies, market=US.
