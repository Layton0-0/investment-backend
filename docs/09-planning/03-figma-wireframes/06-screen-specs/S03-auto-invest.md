# S03 — 자동투자 현황

## 목적

자동매수(통합 복합 로직: 파이프라인+로보) 현황·4단계 파이프라인·오늘 시그널·보유 포지션. "왜 주문이 안 나갔는가" 가드레일 안내.

## 정보 구조

- **상단**: 제목 "자동투자 현황", 부제 "4단계 파이프라인 (유니버스 → 시그널 → 자금관리 → 매매실행) 상태".
- **4단계 카드**: 1단계 유니버스(KR: n, US: n), 2단계 시그널(KR/US 건수, 녹색 강조), 3단계 자금 배분("최대 비중 적용" + 비율 문자열 단기 n% / 중기 n% / 장기 n%), 4단계 보유 포지션(숫자 + "종목 보유중").
- **시그널 목록 테이블**: 종목, 시장, 시그널(BUY/HOLD/SELL Badge), 강도(강/중/약), 목표가. (이미지 기준 UI 정렬 완료)
- **보유 포지션 테이블**: 종목, 시장, 수량, 평균가, 현재가, 손익(양수 녹색·음수 빨강).
- **가드레일 문구**: 자동 매매 OFF, auto-execute 안내, 일일 손실 한도·리스크 게이트 등 — [05-states-and-guardrails.md](../05-states-and-guardrails.md) 반영.

## 주요 상호작용

- (선택) 시그널·포지션 필터/페이징.
- 가드레일 문구 내 "설정" 링크 → /settings.

## 상태/에러

- Loading: 파이프라인 요약·시그널·포지션 로딩.
- Empty: 시그널 0건·포지션 0건 시 테이블 빈 상태.
- Error: PipelineSummaryService 또는 API 실패.

## 권한

- User, Admin 모두 접근.

## 연동 API

- GET /api/v1/pipeline/summary(기준일·계좌): 요약·signalListKr/Us·allocationRatioSummary·openPositionList(currentPrice·pnlPercent 포함).
