# S03 — 자동투자 현황

## 목적

자동매수(통합 복합 로직: 파이프라인+로보) 현황·4단계 파이프라인·오늘 시그널·보유 포지션. "왜 주문이 안 나갔는가" 가드레일 안내.

## 정보 구조

- **4단계 카드**: 1) 유니버스 종목 수 KR·US, 2) 시그널 건수 KR·US, 3) 자금 배분 요약(단기·중기·장기 예상 배분), 4) 보유 포지션 수.
- **시그널 테이블**: 시장, 종목, 팩터, 점수.
- **보유 포지션 테이블**: 시장, 종목, 수량, 진입가, 진입일.
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

- PipelineSummaryService.getSummary(기준일·계좌), GET /api/v1/signals(선택).
