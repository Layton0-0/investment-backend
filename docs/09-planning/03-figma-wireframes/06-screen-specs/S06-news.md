# S06 — 뉴스·이벤트

## 목적

확정 원천(공시·뉴스·센티멘트) 요약·필터·연동 상태.

## 정보 구조

- 원천별 탭/필터: Fact(DART/SEC), Speed(연합/Reuters), Buzz(네이버/Yahoo). 시장·기간·종목 필터.
- 목록: 제목, 원천, 시장, 종목 연관, 수집 시각, 감정/중요도(표시 시).

## 주요 상호작용

- 필터 변경 → GET /api/v1/news 쿼리 갱신.
- 행 클릭 → 상세(모달 또는 별도 뷰) 선택 구현.

## 상태/에러

- Loading, Empty, Error 표준.

## 권한

- User, Ops: 읽기.

## 연동 API

- GET /api/v1/news (market, source, itemType, symbol, title, from, to, page, size).
