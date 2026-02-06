# S08 — 주문·체결

## 목적

주문 목록·체결 내역·미체결 취소.

## 정보 구조

- 주문 목록 테이블: 주문일시, 종목, 시장, 매수/매도, 수량, 가격, 상태, 체결가 등.
- 필터: 기간, 상태(PENDING/EXECUTED/CANCELLED 등), 시장.

## 주요 상호작용

- 필터 적용 → GET /api/v1/orders 쿼리 갱신.
- 미체결 행 [취소] → 확인 모달 → DELETE /api/v1/orders/{orderId}.

## 상태/에러

- Loading, Empty, Error. 취소 실패 시 토스트 또는 인라인 메시지.

## 권한

- User, Ops: 읽기, 본인 주문 취소 가능.

## 연동 API

- GET /api/v1/orders?accountNo=..., DELETE /api/v1/orders/{orderId}.
