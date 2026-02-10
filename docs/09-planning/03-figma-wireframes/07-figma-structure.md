# Figma 파일·구조 규칙

## 1. 파일·페이지 네이밍

- **파일**: `Investment-Choi-Wireframes` 또는 `자동투자-와이어프레임`.
- **페이지(Page)**:
  - `00-IA-Sitemap` — 사이트맵 다이어그램.
  - `01-Auth` — 로그인·회원가입·계좌 인증.
  - `02-Dashboard` — 대시보드(모의·실 구역).
  - `03-AutoInvest` — 자동투자 현황.
  - `04-Strategy` — 국내/미국 전략.
  - `05-News` — 뉴스·이벤트.
  - `06-Portfolio` — 포트폴리오.
  - `07-Orders` — 주문·체결.
  - `08-Batch` — 스케줄 현황.
  - `09-Backtest` — 백테스트.
  - `10-Settings` — 설정.
  - `11-Ops` — Admin 전용(데이터·알림·리스크·모델·감사·헬스).
  - `12-Components` — 공통 컴포넌트·상태.

## 2. 프레임(Frame) 네이밍

- 형식: `화면ID_상태_설명`. 예: `dashboard_virtual_hasAccount`, `auto-invest_empty`, `orders_loading`.
- 화면ID: 소문자, 하이픈. `dashboard`, `auto-invest`, `strategies-kr`, `settings`, `ops-alerts` 등.
- 상태: `default`, `loading`, `empty`, `error`, `hasAccount`, `noAccount`.

## 3. AutoLayout

- **레이아웃**: 상단 Header → Menu → AccountTabs → Container(본문). 본문 내 Section은 세로 AutoLayout, 간격 16~24.
- **카드 그리드**: 2~4열, 자동 줄바꿈. 모바일 뷰는 1열.
- **테이블**: 헤더 행 + 반복 행. AutoLayout Vertical, 패딩 일관.

## 4. Variant 규칙

- **Toggle**: `off` / `on`.
- **Badge**: `active` / `stopped` / `pending` / `executed` / `failed`.
- **StatCard value**: `positive` / `negative` / `neutral`.
- **Button**: `primary` / `secondary` / `danger`(취소 등).

## 5. 디자인 토큰(색·간격)

- `common.css`와 맞춤: Primary, Text, TextMuted, Background, Border. Space: 8, 16, 24. Radius: 4, 8.
- Figma 변수로 정의 시 동일 명칭 권장: `--color-primary`, `--space-md` 등.

## 6. Figma MCP로 디자인 조회 시

- **Figma Design vs Make**: MCP(get_metadata, get_design_context, get_screenshot)는 **Figma Design** 파일(`figma.com/design/:fileKey/...`)만 지원합니다. **Figma Make** 파일(`figma.com/make/...`)은 MCP에서 조회 불가.
- **프로젝트에서 MCP 사용**: `.cursor/mcp.json.template`에 `figma` 서버 예시가 있으면, Cursor 설정에서 Figma 연동 또는 `figma-developer-mcp` 등으로 MCP를 활성화할 수 있음. Design 파일 공유 URL에서 fileKey·node-id를 추출해 사용.
- **Auto Investment Front**: Design 버전 파일 URL이 있으면 MCP로 구조·스타일 조회 후 프론트 코드에 반영 가능.
