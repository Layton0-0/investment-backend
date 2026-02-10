# Figma Make 단일 프롬프트(5,000자 이내)

아래를 그대로 Figma Make 채팅에 붙여넣어 **저충실도(회색 톤) 와이어프레임**을 생성해줘.

## 컨텍스트
- 서비스: Investment Choi 자동투자(한국/미국 통합)
- 핵심: 4단계 파이프라인(유니버스→시그널→자금관리→매매실행) + (옵션) 로보(ETF)
- 전역 탭: 인증 후 모든 화면에 `모의계좌(serverType=1) | 실계좌(serverType=0)` 탭을 메뉴 아래 표시
- 역할: `User`(투자/설정/조회), `Admin`(운영: 스케줄/데이터/알림/리스크/모델/감사/헬스). Admin 메뉴는 Admin만 노출.

## 결과물 규칙(필수)
- 스타일: 로우파이 와이어프레임(회색), 외부 이미지/아이콘 최소, placeholder 허용
- 레이아웃(인증 후 공통): Header(로고/서비스명/사용자/마이페이지/로그아웃) → Menu → AccountTabs → Container(본문 1200px)
- AutoLayout: 본문 세로 간격 16~24, 카드 그리드 2~4열, 테이블 thead+rows

## 파일/페이지/프레임
- 파일명: `Investment-Choi-Wireframes`
- 페이지: `00-IA-Sitemap`, `01-Auth`, `02-Dashboard`, `03-AutoInvest`, `04-Strategy`, `05-News`, `06-Portfolio`, `07-Orders`, `08-Batch`, `09-Backtest`, `10-Settings`, `11-Ops`, `12-Components`
- 프레임명 규칙: `screenId_state_desc` (state: default/loading/empty/error/hasAccount/noAccount)

## 메뉴(표시명 그대로)
User 메뉴: 대시보드(`/`), 자동투자 현황(`/auto-invest`), 국내 전략(`/strategies/kr`), 미국 전략(`/strategies/us`), 뉴스·이벤트(`/news`), 포트폴리오(`/portfolio`), 주문·체결(`/orders`), 스케줄 현황(`/batch`), 백테스트(`/backtest`), 설정(`/settings`)
Admin 메뉴(Admin만): 데이터 파이프라인(`/ops/data`), 알림센터(`/ops/alerts`), 리스크 리포트(`/risk`), 모델/예측(`/ops/model`), 감사 로그(`/ops/audit`), 시스템 헬스(`/ops/health`)
권한: User는 Admin 메뉴 숨김. `/batch`의 “지금 실행”은 Admin만 활성.

## 컴포넌트(12-Components에 제작 후 재사용)
Layout/Header, Layout/Menu, Layout/AccountTabs, Layout/Container
Card/Base, Card/Pipeline, Card/Summary, Card/Stat(positive/negative/neutral)
Table/DataTable, Filter/FilterBar, Form/Input, Form/SegmentControl, Form/Toggle
Button/Primary, Button/Secondary, Button/Danger
Badge/Status(active/stopped/pending/executed/failed), Toast(success/error)
State/Loading, State/Empty, State/ErrorBox, Modal/Confirm

## 가드레일 문구(화면에 반드시 표현)
- 계좌 미등록: “모의계좌를 등록해주세요/실계좌를 등록해주세요” + 설정 CTA
- 자동 매매 OFF: “주문이 나가지 않습니다. 설정에서 자동 매매를 켜세요.” + 설정 링크
- 서버 Dry-Run(auto-execute=false): “현재 서버는 Dry-Run 모드입니다. 실제 주문은 서버 설정(PIPELINE_AUTO_EXECUTE)이 필요합니다.”(읽기 전용)
- 실계좌 자동 실행 차단(allow-real-execution=false): “실계좌 자동 실행은 서버 설정(PIPELINE_ALLOW_REAL_EXECUTION)으로만 허용됩니다.”(Admin에서 읽기 전용)
- 리스크 게이트/일일 손실 한도: “신규 매수 축소/중단” + 원인 요약
- Rate limit: “요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.”
실제 주문과 연결 가능 행동(자동 매매 ON, 지금 실행)은 확인 모달(2중 확인) 사용.

## 화면 생성(각 페이지에 최소 default, 중요 화면은 loading/empty/error도)
01-Auth: `auth-login_default`, `auth-signup_default`(서버타입+API key/secret+계좌번호+계좌인증 버튼 포함)
02-Dashboard:
- `dashboard_default_virtualAndReal`: QuickAction 6개(국내/미국전략/뉴스/포트폴리오/주문/설정) + 모의구역 + 실구역(각각 자동투자상태 카드, 계좌요약, Stat 4개, 잔고 카드, 보유 테이블, 최근주문 테이블)
- `dashboard_empty_noAccount`
03-AutoInvest:
- `auto-invest_default_summary`: 4단계 카드(유니버스KR·US/시그널KR·US/자금배분요약/보유포지션수) + 시그널 테이블 + 포지션 테이블 + 가드레일 영역
- `auto-invest_loading`, `auto-invest_empty_noSignals`
04-Strategy: `strategies-kr_default_list`, `strategies-us_default_list`(단/중/장기 카드/상태배지)
05-News: `news_default_list`(필터: 원천/시장/기간/종목 + 목록 테이블)
06-Portfolio: `portfolio_default`(보유 테이블 + 날짜선택 + 추천 테이블)
07-Orders: `orders_default_list`(필터 + 주문테이블 + 미체결 취소 버튼), (옵션)`orders_modal_cancelConfirm`
08-Batch: `batch_default_jobs`(Job 테이블 + 지금 실행 버튼(Admin만) + 확인모달)
09-Backtest: `backtest_default`(모드 선택 4단계/로보 + 입력 폼 + 결과: 메트릭 카드/수익곡선 placeholder/거래테이블), `backtest_loading`
10-Settings: `settings_default`(계좌·API 연결 + 자동투자 설정 폼 + 토글 + 저장 + 가드레일 안내), `settings_empty_noAccount`
11-Ops(Admin만): `ops-data_default`, `ops-alerts_default_list`, `risk_default_report`, `ops-model_default`, `ops-audit_default`, `ops-health_default`

## 샘플 데이터
시장: KR/US, 종목: 005930·000660·AAPL·NVDA·TQQQ, 손익: +1,230,000 / -540,000, 수익률: +2.35% / -1.12%
파이프라인 예시: 유니버스 “KR 120 · US 80”, 시그널 “KR 12 · US 7”, 포지션 “4”

## 금지
하이파이 UI/과도한 컬러·이미지 금지. 민감정보는 샘플 텍스트만. 전역 탭(serverType) 누락 금지.

