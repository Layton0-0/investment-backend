# 공통 컴포넌트·라이브러리

## 1. 레이아웃

| 컴포넌트 | 설명 | Figma 네이밍 제안 |
|----------|------|-------------------|
| LayoutHeader | 로고·서비스명·사용자·마이페이지·로그아웃 | `Layout/Header` |
| LayoutMenu | 사이드 또는 상단 메뉴, 메뉴 ID별 활성 | `Layout/Menu` |
| LayoutAccountTabs | 모의계좌 \| 실계좌 전역 탭 | `Layout/AccountTabs` |
| Container | 본문 최대 너비 1200px, 중앙 정렬 | `Layout/Container` |

## 2. 카드

| 컴포넌트 | 설명 | 변형 |
|----------|------|------|
| Card | 기본 카드(제목+본문) | — |
| CardPipeline | 파이프라인 단계용(라벨+숫자/요약) | 1~4단계 라벨 |
| CardSummary | 계좌 요약(잔고·보유 종목 수 등) | — |
| StatCard | 단일 지표(라벨+값, 수익률 등 색상) | positive/negative |

## 3. 테이블

| 컴포넌트 | 설명 |
|----------|------|
| DataTable | thead + tbody, 정렬 가능 컬럼(선택) |
| TableResponsive | 가로 스크롤 래퍼 |

컬럼 예: 시장(KR/US), 종목, 수량, 가격, 평가금액, 손익, 상태 등. 손익·수익률은 positive/negative 클래스.

## 4. 폼·필터

| 컴포넌트 | 설명 |
|----------|------|
| SegmentControl | "모의계좌 \| 실계좌" 등 2~3개 옵션 |
| Toggle | 자동 매매 ON/OFF, 로보 ON/OFF. 좌측 텍스트·스위치·우측 텍스트 |
| FilterBar | 기간·상태·시장 등 드롭다운/날짜 피커 |
| Input | 텍스트·숫자. 라벨·에러 메시지·힌트 |
| ButtonPrimary / ButtonSecondary | CTA·보조 버튼 |

## 5. 배지·상태 표시

| 컴포넌트 | 설명 |
|----------|------|
| Badge | ACTIVE, STOPPED, PENDING, EXECUTED 등 |
| StatusDot | 녹색/황색/빨강 (정상/경고/오류) |

## 6. 상태(Loading / Empty / Error)

| 상태 | 설명 | UI |
|------|------|-----|
| Loading | 데이터 조회 중 | 스피너 또는 스켈레톤 |
| Empty | 데이터 없음 | 일러/문구 + CTA(예: "설정에서 계좌 등록") |
| Error | API/서버 오류 | 에러 박스(메시지 + 재시도 버튼) |

## 7. 모달·토스트

| 컴포넌트 | 설명 |
|----------|------|
| Modal | 확인(주문 취소 등)·폼(선택). 오버레이+닫기 |
| Toast | 일시적 성공/실패 메시지, 자동 소거 |

## 8. 빠른 액션(대시보드)

| 컴포넌트 | 설명 |
|----------|------|
| QuickActionCard | 아이콘+제목, 링크(serverType 쿼리 포함). 국내 전략·미국 전략·뉴스·포트폴리오·주문·설정 등 |

## 9. CSS 클래스 참조(현재 구현)

- `common.css`: `--color-primary`, `--color-text`, `--space-*`, `--radius-*`, `.card`, `.data-table`, `.balance-row`, `.stat-card`, `.toggle-label`, `.empty-state`, `.segment-control`, `.error-box`.
