# 개발 진행 현황

**목적**: 개발 완료·진행중·진행예정 항목을 한 문서에서 관리하고, [로드맵](../roadmap.md)·[화면·메뉴 기획서](./01-screen-menu-spec.md)·[PRD](../PRD.md)와 연동하여 참조·변동 시 갱신한다.

**갱신 원칙**: 개발 완료·스프린트 시작·범위 변경 시 이 문서를 수정하고, 다른 문서(roadmap, README)에서 이 문서를 참조한다.

---

## 1. 완료 (Completed)

### 도메인·DB·API
- [x] **2.0 아키텍처 Phase 2 (PreTrade·Kill Switch·Portfolio)**  
  Pre-Trade 컴플라이언스: `PreTradeComplianceEngine`(Kill Switch, 단일 종목 10% 상한, MDD 15% 게이트), `OrderService.executeOrderInternal` 주문 직전 호출. Kill Switch: `TB_TRADING_HALT`·`TradingHaltService`, GET/PUT `/api/v1/system/kill-switch`(Ops만 설정). MDD용 `TB_PORTFOLIO_PEAK`·`PortfolioPeakService`. 포트폴리오: `TaxAwareOptimizerImpl`(FrictionCost 기반 비중 조정), `RebalancerImpl`(잔고·포지션 기반 매매 리스트). Flyway V25. [00-strategy-registry.md 2.9.1~2.9.3](../02-architecture/00-strategy-registry.md), [01-system-architecture.md §9](../02-architecture/01-system-architecture.md) 반영.
- [x] **TimescaleDB 전환 및 2.0 아키텍처 Phase 1**
  MariaDB → TimescaleDB(PostgreSQL) 전환: docker-compose timescaledb 서비스, application*.yml·build.gradle PostgreSQL 설정, Flyway V21~V24 PostgreSQL DDL 변환. 초기 스키마는 `init-db` 프로파일 1회 실행. 기관급 퀀트 엔진 패키지 추가: `core.engine.alpha`(AlphaEngine), `core.engine.portfolio`(TaxAwareOptimizer·Rebalancer 스텁), `core.engine.risk`(ComplianceEngine 스텁), `core.engine.execution`(ExecutionGateway), `core.pipeline`(DataPipelineService). [01-system-architecture.md §9](../02-architecture/01-system-architecture.md), [decisions.md ADR 15·16](../decisions.md) 반영.
- [x] **Friction cost(수수료/세금/슬리피지) 설정 및 백테스트 반영**  
  한국투자증권(KIS) 실전 수수료·세금·슬리피지 테이블을 `application.yml`(`investment.fees`) 및 `FrictionCostProperties`에 반영. 일반 백테스트(`BacktestService`)는 매수/매도 시 마찰 비용 차감·PnL 반영·`BacktestTradeDto.totalFrictionCost` 노출. 로보 백테스트(`RoboBacktestService`)는 `FrictionCostProperties` 기반 round-trip·TAF 적용, 요청 `commPct`/`slipPct` 오버라이드 시 하위 호환. [전략 레지스트리 2.9 Friction cost](../02-architecture/00-strategy-registry.md#29-friction-cost-마찰-비용) 참조.
- [x] **로보 백테스트 데이터 부재·평평한 곡선 안내**  
  `RoboBacktestService.run()` 진입 후 요청 구간·자산/벤치마크 심볼에 대해 US 일봉 1건 이상 존재 여부 검사(`hasAnyUsDailyDataInRange`). 없으면 `RoboBacktestResult.warningMessage`에 "선택 기간에 US 일봉 데이터가 없습니다. 데이터 수집 후 다시 시도하세요." 설정. 결과의 `equityCurve`가 전 구간 동일 값이면 "수익 곡선이 평평합니다. US 일봉 데이터 구간을 확인하세요." 추가. `/backtest` 로보 결과 수신 시 `warningMessage`가 있으면 결과 요약 위 경고 문구 표시. API 응답에 `warningMessage` 필드 문서화(02-api-endpoints.md).
- [x] **데이터 수집·백테스트·할당 엔진 로깅 상세화**  
  어떤 시점에서 어떤 원인으로 데이터를 못 받거나 오류가 나는지 바로 알 수 있도록 로깅 보강. **UsMarketCollectionService**: 수집 시작/완료(기간·심볼), collector-url/스크립트 미설정·파일 없음·HTTP 비정상 응답·본문 없음·JSON 파싱 실패·스크립트 비정상 종료 시 WARN/ERROR에 원인·URL·basDt·bodyPreview 등 포함. **RoboBacktestService**: 백테스트 시작/완료·파라미터(DEBUG), 구간 내 US 일봉 없음·평평한 곡선·리밸런싱 전액 현금 시 WARN/DEBUG, getClose/getOpen null 시 TRACE. **RoboAllocationEngine**: 일반/듀얼 모멘텀 진입(DEBUG), 심볼 일봉 없음·종가 부족·SPY/섹터 일봉 부족(TRACE), 전액 현금 반환 사유(WARN).
- [x] **Strategy 시장(MARKET)**  
  Strategy 엔티티 `market` 필드, Repository 시장 조건 조회, Service/API `market` 파라미터(선택), DTO 반영. DB V3(ACCOUNT_NO,MARKET,STRATEGY_TYPE UK) 반영.
- [x] **뉴스·공시 도메인 및 API**  
  NewsItem 엔티티, NewsItemRepository, NewsItemService, GET `/api/v1/news` (필터·페이징). API 개요 문서 반영.
- [x] **TB_NEWS_ITEMS EVENT_TYPE 확장 (V11)**  
  DART report_nm 등 긴 보고서명 저장 시 50자 초과 오류 방지. DB V11(EVENT_TYPE VARCHAR(500)), NewsItem.MAX_EVENT_TYPE_LENGTH·truncateEventType, DartCollectionService·SecCollectionService·InternalDataCollectionController에서 eventType 500자 truncate 적용. (V11 롤백 스크립트는 Flyway·마이그레이션 정리로 제거됨. 필요 시 git history 참조.)
- [x] **캐시 키 정리**  
  CacheConfig에 CACHE_CURRENT_PRICE 정의, RealtimeMarketDataService에서 상수 사용.

### 화면·메뉴
- [x] **한국투자증권 토큰 발급 1분 1회 제한·사용자 단위 락**  
  KoreaInvestmentTokenService에 사용자당 1분 1회 새 발급 제한(TOKEN_ISSUANCE_COOLDOWN_MS)·lastIssuanceTimeByUserId·issuanceLockByUserId 추가. getAccessToken에서 동시에 모의/실 두 타입 발급이 겹치지 않도록 사용자 단위 synchronized 락으로 직렬화. 403 "접근토큰 발급 1분당 1회" 재발 방지.
- [x] **대시보드 모의·실계좌 동시 로드**  
  useDashboardData에서 서버타입과 무관하게 모의·실 메인 계좌 각각에 대해 자산·포지션·주문·파이프라인 요약·거래 설정을 병렬 로드. virtualPositions/realPositions, virtualRecentOrders/realRecentOrders 등 모의·실 구분 저장. Dashboard에서 모의/실 카드에 각각 해당 데이터 매핑, 보유 종목·최근 주문 테이블 모의/실 각각 표시.
- [x] **관리자 인가: User role·로그인 응답 role·OPS 로그인 버튼 제거**  
  TB_USERS에 ROLE 컬럼 추가(V24), User 엔티티·AuthResponseDto에 role 필드, AuthService 로그인/회원가입 응답에 role 포함. 프론트 LoginPage에서 "OPS 로그인" 버튼 제거, 로그인 API 응답의 role로 인가(역할은 서버에서 반환).
- [x] **React 대시보드 API 매핑 정리(Thymeleaf·기획 문서 기준) 및 상세 에러 안내**  
  Thymeleaf 대시보드·01-screen-menu-spec 기준으로 React Dashboard에 메인 계좌·자산·포지션·주문·파이프라인 요약(GET /api/v1/pipeline/summary)·거래 설정(GET /api/v1/settings/{accountNo}) 연동. 백엔드 GlobalExceptionHandler에서 ACCOUNT_NOT_FOUND 시 404 반환. 프론트 userAccountsApi/accountApi/ordersApi/settingsApi/pipelineApi에서 404·400 시 null/[] 반환(graceful). ApiError에 code·details·traceId 추가, errorMessages.ts로 원인별 한글 메시지(getDisplayErrorMessage) 제공, Dashboard 등에서 적용. [02-api-endpoints.md](../04-api/02-api-endpoints.md) 계좌/자산 404 명시.
- [x] **API–프론트엔드 매핑 문서 추가**  
  [11-api-frontend-mapping.md](../04-api/11-api-frontend-mapping.md) 신규: 백엔드 API 목록·프론트 모듈/함수·사용 위치(라우트/페이지)·미연동 정리. 01-api-overview.md §3.7 배치 경로 보정(GET /batch/api/jobs), §9.2에 11 문서 링크 추가.
- [x] **배치 작업 목록 API 연동**  
  프론트 batchApi.getBatchJobs() (GET /batch/api/jobs) 추가, /batch 화면(Batch 컴포넌트)에서 하드코딩 테이블 제거 후 API 응답으로 스케줄 현황 테이블 렌더링. 로딩·에러·지금 실행(triggerPath 기반) 유지.
- [x] **전체 UI/UX 리팩터링 (디자인 시스템·레이아웃·페이지 스타일 통일)**  
  common.css에 디자인 토큰(CSS 변수)·container 1200px·`.data-table`·카드 변형·대시보드/전략/포트폴리오/백테스트 등 공통 유틸리티 추가. 모든 인증 페이지에 layout-header + layout-menu 적용(portfolio, mypage 포함). dashboard/strategies 인라인 스타일 제거·common 클래스 사용. portfolio·error 페이지 common 기반 통일. 접근성·반응형(미디어 쿼리·포커스) 점검. [07-frontend-simplification.md](../02-architecture/07-frontend-simplification.md), [08-frontend-architecture.md](../02-architecture/08-frontend-architecture.md) 반영.
- [x] **메뉴 설정 및 공통 레이아웃**  
  MenuConfig(appMenuItems), MenuModelAdvice, layout-header·layout-menu fragment, common.css 앱 헤더·네비 스타일.
- [x] **대시보드 보강**  
  공통 헤더·메뉴 적용, 빠른 액션(국내/미국 전략, 뉴스·이벤트, 포트폴리오, 주문·체결, 설정).
- [x] **자동투자 현황**  
  `/auto-invest`, AutoInvestController, auto-invest.html (4단계 파이프라인·시그널 건수/목록 GET /api/v1/signals 연동).
- [x] **국내/미국 전략 분리**  
  `/strategies/kr`, `/strategies/us`, WebStrategyController 시장별 조회·폼 market 전달.
- [x] **국내/미국 전략 계좌 자동 사용 및 로고·네비게이션 정리**  
  국내 전략 = 모의계좌(serverType=1), 미국 전략 = 실계좌(serverType=0) 자동 사용. 계좌번호 입력 폼 제거, 계좌 미등록 시 안내 및 설정 링크. 전략 페이지 내 대시보드/국내/미국 중복 링크 제거. 헤더 로고 클릭 시 대시보드(`/`) 이동. 화면·메뉴 기획서 §3.3·§3.4·§4 반영.
- [x] **뉴스·이벤트**  
  `/news`, NewsWebController, news.html, GET /api/v1/news 연동.
- [x] **주문·체결**  
  `/orders`, OrdersWebController, orders.html, 주문 목록 연동.
- [x] **설정**  
  메뉴 "설정" → `/mypage` (기존 마이페이지).
- [x] **설정 UI/UX 리팩토링 (모의/실계좌 선택·토글·빈 상태)**  
  설정 페이지: 카드 제목 "계좌·API 연결", "자동투자 설정" 통일. 등록된 계좌 수(0/1/2)에 따라 거래 설정 분기 — 0개면 빈 상태 + [계좌 설정으로 가기] CTA, 1개면 해당 타입 폼만, 2개면 세그먼트 "모의계좌 | 실계좌" + 선택한 타입 단일 폼. 자동투자·로보 어드바이저를 common.css 토글 마크업(toggle-label·toggle-text-left·toggle-slider·toggle-text-right)으로 스위치 형태 표시. common.css에 .empty-state, .segment-control, .segment-btn 추가. 화면·메뉴 기획서 §3.8 반영.
- [x] **프론트 화면 기획서 기획요청 반영·디자인 AI 전체 프롬프트 문서**  
  [01-screen-menu-spec.md](01-screen-menu-spec.md)에 기획요청 §8·§9 반영: 포트폴리오 세금·수수료 영향 뷰, 연말 세금·리포트 메뉴(§3.10·`/report/tax`), 대시보드/Ops 킬스위치·시스템 헬스 블록. [10-design-ai-full-prompt.md](03-figma-wireframes/10-design-ai-full-prompt.md) 신규: 디자인 AI에 붙여넣기만 하면 되는 통합 프롬프트(디자인 원칙·모든 화면 진입/구성/액션/결과/예외 워크플로우·연말 리포트·세금뷰·킬스위치·가드레일). [00-index.md](03-figma-wireframes/00-index.md) 읽는 순서에 10 문서 링크 추가.
- [x] **전체 화면 기획 + Figma 와이어프레임 문서 패키지**  
  [03-figma-wireframes](03-figma-wireframes/00-index.md) 폴더에 IA·역할/권한·유저 플로우·컴포넌트·가드레일·화면별 스펙(06-screen-specs)·Figma 구조·Figma AI 프롬프트 작성. 2역할(User/Ops)·Ops 확장 메뉴(데이터 파이프라인·알림센터·리스크·모델·감사·헬스) 반영. [01-screen-menu-spec.md](01-screen-menu-spec.md) §6 향후(Ops) 메뉴·§6.2 역할·권한 개요 추가.
- [x] **React 프론트 시니어급 리팩토링**  
  구조: 루트 App.tsx 제거, components/styles를 src 하위로 이관, `@/` path alias 도입. 타입: any 제거, Dashboard/Settings/UI 등 Props·API DTO 명시, http.ts ImportMetaEnv·ApiErrorBody 적용. 비동기: useDashboardData·useSettingsAccounts 훅 추출, 컴포넌트는 훅만 사용. 컴포넌트: Dashboard를 DashboardSummaryCards·DashboardAccountCard·DashboardPositionsTable·DashboardOrdersTable로 분할, DataTable rowKey·그리드 key 안정화. Error Boundary·Settings 토글 no-op 제거·useEffect 의존성 정리·상수(routes.ts)·보안/Error Boundary 문서(README) 반영. 테스트: useDashboardData·LoginPage(401 메시지) 추가.
- [x] **React 프론트(분리 배포) 초기 전환: Vite+React Router + API 연동 골격**  
  `investment-front`에 Vite 기반 실행 환경(`package.json`, `vite.config.ts`, `tsconfig.json`)을 구성하고, mock UI를 기준으로 `react-router-dom` 라우팅 + 공통 `AppShell`(헤더/사이드바/모의·실 토글) 구조를 구성. 로그인/회원가입/마이페이지는 `/api/v1/auth/*` 연동(토큰은 Authorization Bearer 사용)으로 동작. 주요 화면(Dashboard/AutoInvest/Strategies/News/Portfolio/Orders/Batch/Backtest/Settings)은 REST API 기반으로 mock 데이터를 제거하고 조회/트리거를 연결. 백엔드에는 자동투자 현황용 `/api/v1/pipeline/summary` 엔드포인트 추가.
- [x] **Figma MCP 설정 및 프론트 Figma 와이어프레임 문서 정합**  
  `.cursor/mcp.json.template`에 Figma MCP 서버(figma-developer-mcp) 예시 추가. Figma **Make** 파일은 MCP 미지원(Design 파일만 조회 가능)이므로, [07-figma-structure.md](03-figma-wireframes/07-figma-structure.md) §6에 Make/Design 구분·MCP 사용 안내 추가. 프론트는 03-figma-wireframes 문서 기준으로 정합: `globals.css`에 디자인 토큰(--space-sm/md/lg, --radius-sm/md, --color-primary 등), AppShell 본문 최대 너비 1200px(Layout/Container), 로그인/회원가입 S01-auth 스펙(401·계좌 인증 실패 안내 문구) 반영.
- [x] **프론트 화면 플로우 기획서 정합 (경로·메뉴·serverType)**  
  백엔드 [01-screen-menu-spec.md](01-screen-menu-spec.md)·[01-information-architecture.md](03-figma-wireframes/01-information-architecture.md)·[03-user-flows.md](03-figma-wireframes/03-user-flows.md)에 맞춰 React 프론트 라우트·메뉴 수정. 회원가입 경로 `/signup` 추가·`/register`는 `/signup`으로 리다이렉트, 리스크 리포트 경로 `/risk` 추가·Ops 메뉴 링크 `/risk`로 변경, 모든 메뉴·헤더 링크에 `serverType` 쿼리 포함·탭 변경 시 URL 갱신·진입 시 URL의 serverType 동기화, 로그인 페이지 회원가입 링크 `/signup`으로 변경.
- [x] **프론트엔드 보안 리팩토링 및 문서화**  
  `investment-front/docs/`에 01-security, 02-architecture, 03-development-guide 추가. 인증: localStorage 토큰 제거, HttpOnly 쿠키 기반(apiFetch credentials: include, AuthContext 초기 mypage 체크·401 시 skipUnauthorizedHandler). Vite proxy `/api` → 8083. XSS: chart.tsx dangerouslySetInnerHTML 제거(useEffect로 style 주입). 외부 링크: secureUrl(isSafeHref/getSafeHref)·Market.tsx 적용. 사이드바 쿠키 SameSite=Lax·Secure(HTTPS 시). 로그인/회원가입 에러 메시지 일반화. 입력 검증: inputValidation(validateLogin/validateSignup)·Login/Register 연동. 테스트: http.test, secureUrl.test, inputValidation.test, LoginPage/AppRoutes 모킹 보강.
- [x] **Figma 퍼블 반영 (investment-front/publish)**  
  `publish/` Figma 기반 퍼블을 `src/`에 반영. Logo 컴포넌트(SVG), UI.tsx Tailwind/DataTable +/- 셀 스타일, AppShell(Logo·경로별 SegmentControl·max-width 1440px·퍼블 색상), LoginPage/RegisterPage 퍼블 레이아웃·한글 문구, DashboardSummaryCards 아이콘+라벨 퀵메뉴, DashboardAccountCard/Dashboard 퍼블 색상, Investment/Market/Ops/System 카드·타이포 색상 통일. 테스트(LoginPage/AppRoutes) 기대 문자열 한글 반영.
- [x] **smart-portfolio-pal 퍼블리싱 반영 (investment-front)**  
  smart-portfolio-pal 디자인 참고로 전역 스타일(Deep Navy Fintech HSL·Pretendard·gradient·shadow-card)·button variants(hero/heroOutline/success)·랜딩 페이지(/, Header·HeroSection·FeaturesSection·Footer)·라우트 변경(/ = 랜딩, /dashboard = 메인 홈)·AppShell 좌측 사이드바+메인 재구성·로그인 페이지 스플릿 레이아웃·대시보드 카드 토큰 적용. 문서: investment-front/docs/02-architecture.md 요약, 테스트·IntersectionObserver mock 보강.
- [x] **개편 디자인 기준 프론트엔드 적용 (investment-front)**  
  smart-portfolio-pal과 동일한 레이아웃·메뉴·라우트 구조로 정리. **훅**: `useAccountType`(URL `serverType` 쿼리 ↔ AuthContext 동기화). **레이아웃**: AppShell 제거, AppLayout(AppHeader + AppMenu + AccountTabs + max-w-[1200px] 본문) 적용. **라우트**: 페이지 단위 매핑, `/report/tax`(TaxReportPage) 추가, Ops 개별 경로(`/ops/data`, `/ops/alerts` 등) 유지·path 기반 OpsPage subPage 분기. **페이지**: DashboardPage, AutoInvestPage, StrategyPage(/strategies/:market), NewsPage, PortfolioPage, OrdersPage, BatchPage, BacktestPage, SettingsPage, TaxReportPage(스텁), OpsPage(기존). 메뉴: User 메뉴 + 연말 세금·리포트 + Ops 전용(Ops 역할 시). 테스트: AppRoutes(랜딩/대시보드/연말리포트 인증 시 렌더) 보강. [01-screen-menu-spec.md](01-screen-menu-spec.md), [10-design-ai-full-prompt.md](03-figma-wireframes/10-design-ai-full-prompt.md) 반영.
- [x] **설정 페이지 smart-portfolio-pal 디자인 전면 반영 (investment-front)**  
  설정 화면을 개편 디자인에 맞춰 전면 적용. **탭**: "계좌·API 연결" | "자동투자 설정" (shadcn Tabs). **계좌 탭**: 모의계좌 카드·실계좌 카드 각각 표시(연결됨/미등록 배지, API Key/Secret/계좌번호/Current Password, 카드별 [저장]). **자동투자 탭**: 계좌 0개 시 빈 상태 + [계좌 설정으로 가기] CTA; 1~2개 시 세그먼트(모의계좌|실계좌) + 선택 타입별 폼(자동 매매·로보 토글, 최소/최대 투자금, 단·중·장기 비율, [저장]); 서버 설정 읽기 전용 카드(PIPELINE_AUTO_EXECUTE, PIPELINE_ALLOW_REAL_EXECUTION). **훅**: `useSettingsAccountsAll` 추가(virtual/real 동시 조회·타입별 저장). [10-design-ai-full-prompt.md §5.10](03-figma-wireframes/10-design-ai-full-prompt.md), [01-screen-menu-spec.md §3.8](01-screen-menu-spec.md) 반영.

### 인프라·운영
- [x] **Flyway 도입 및 기존 마이그레이션 SQL 정리**  
  spring-boot-starter-flyway 추가, baseline-on-migrate=true·baseline-version=20. 현재 DB는 이미 생성된 상태로 간주하고 V1~V20 마이그레이션·롤백 파일 제거. 신규 스키마 변경은 db/migration/V21__*.sql 형식으로 추가 시 앱 기동 시 자동 적용. schema.sql은 참고/신규 환경 1회 생성용 유지. [01-database-schema.md §8.2](../05-database/01-database-schema.md), [01-local-setup-complete.md §문제 해결](../08-setup-guides/01-local-setup-complete.md) 참조.
- [x] **Redis 캐싱**  
  CacheConfig(Redis), @Cacheable(AccountService, AnalysisService, RealtimeMarketDataService). application-no-redis.yml(캐시 타입 simple) 및 캐시 무효화 주석.
- [x] **OpenAPI 문서화**  
  SpringDoc 설정, 전략 API(market 파라미터)·뉴스 API @Tag·@Operation·@Parameter 반영.
- [x] **Circuit Breaker**  
  RealtimeMarketDataService(getCurrentPrice), OrderService(executeOrder), FastApiPredictionClient(predictPrice·predictBatch). Resilience4j marketDataService·orderService·aiPredictionService 인스턴스, Fallback 정책 반영.

### AI 연동
- [x] **AiPredictionClient·FastApiPredictionClient**  
  인터페이스·구현체, Circuit Breaker·fallback, isModelReady 2xx 체크. application.yml prediction-service 설정.
- [x] **FastAPI 예측 서비스**  
  prediction-service: /api/v1/predict, /api/v1/health, Mock 응답. Dockerfile·docker-compose prediction-service 서비스 추가.

### 기타
- [x] **스케줄 작업 Spring Batch 전환**  
  모든 스케줄 작업을 Spring Batch Job + Tasklet으로 실행. `BatchJobRegistry`로 Job 정의(이름, cron, 트리거 경로) 단일 관리, `BatchJobScheduler`가 cron별 `JobLauncher.run` 등록. 실행 이력은 JobRepository(BATCH_* 테이블)에 자동 저장되며, 스케줄 현황 UI는 총 실행/성공/실패 횟수·마지막 실행 시각을 DB 조회로 표시. Cron 표현식은 `CronDescriptionUtil`로 한국어 실행 주기 표시. 트리거 API·트레이딩 포트폴리오 생성은 Job 실행으로 통일. 기존 스케줄러 클래스에서 `@Scheduled` 제거(공개 메서드는 Tasklet에서 호출). [02-api-endpoints.md](../04-api/02-api-endpoints.md) 트리거 API 동일 경로·응답 유지.
- [x] **Security**  
  새 경로(/auto-invest, /strategies/kr, /strategies/us, /news, /orders) 기존 anyRequest().authenticated()로 인증 적용.
- [x] **JWT 인증 시 사용자 존재 여부 검증(방어코드)**  
  DB 초기화·삭제 후에도 오래된 JWT로 인증되는 것을 방지. UserExistenceChecker(auth), JwtAuthenticationFilter에서 토큰 유효 시 DB 사용자 존재 확인, 없으면 인증 미설정(401/리다이렉트). 쿠키로 전달된 무효 토큰 시 응답에서 token 쿠키 제거.
- [x] **모의/실전 계좌·계좌인증**  
  회원가입·마이페이지 서버 타입 토글 시각과 전송값 일치(왼쪽=모의='1', 오른쪽=실전='0'). 계좌인증 API POST `/api/v1/auth/verify-account` (접근 토큰 발급 성공 시 인증 완료, 잔고조회 미호출). 회원가입 폼: 서버 타입을 계좌번호 위에 배치, 계좌번호 마스킹(type=password), 계좌인증 버튼·한국투자증권 선택 시 계좌인증 필수. Rate limit(verify-account) 적용.
- [x] **로그 마스킹 모듈화 (app key, secret, 계좌번호 INFO 마스킹)**  
  `LogMaskingUtil`에 `maskAccountNo`, `maskSecret` 추가. app key / secret / 계좌번호 / userId 등 암호화 저장 항목은 로그 출력 시 반드시 `LogMaskingUtil` 사용(INFO/WARN/ERROR에서 마스킹만, DEBUG에서 필요 시 실제 값 추가). KoreaInvestmentTokenClient, AuthService, KoreaInvestmentAccountClient, OrderService, KoreaInvestmentOrderClient, Strategy·Account·Setting·Dashboard 등 전역 적용. 상세 규칙: [보안 설정 참조](../07-security/02-security-configuration-reference.md#로깅-시-민감정보-마스킹-개발-규칙).
- [x] **한국투자증권 API 조회 GET+query 수정**  
  조회 API(주식잔고·매수가능·매도가능·주문체결·자산현황·기간별손익·현재가·차트)가 query parameter로 전달되어야 하는데 JSON body로 호출되던 오류 수정. `KoreaInvestmentAccountClient` 7곳·`KoreaInvestmentMarketDataClient` 2곳을 **GET + URI query parameter**로 변경. `buildUriWithQueryParams` 헬퍼 추가, `KoreaInvestmentRequestBuilder` 주석 보강(조회 API는 Map을 query로 사용). [한국투자증권 API 가이드](../04-api/09-korea-investment-api-guide.md)에 조회 API GET·query 명시, [ADR 14](../decisions.md#14-한국투자증권-api-요청-방식-및-mcp-사용) 및 [MCP 규칙](../.cursor/rules/MCP.mdc): 한국투자증권 API 개발 시 MCP 무조건 사용·작업 중 문서 업데이트 필수.
- [x] **투자계좌자산현황조회 404 수정 및 모의계좌 폴백**  
  한국투자증권 공식 예제(inquire_account_balance) 기준으로 path·TR ID·파라미터·응답 파싱 수정. path: `inquire-assets` → `inquire-account-balance`, TR ID: `CTRP6548R`(실거래 전용). **모의계좌는 해당 API 미지원** → 모의(serverType=1)일 때 `inquireAssets()`에서 주식잔고조회(inquire-balance) 결과로 `AccountAssetDto` 구성해 반환(폴백). [09-korea-investment-api-guide.md](../04-api/09-korea-investment-api-guide.md) §5 갱신.
- [x] **대시보드 거래 설정 optional 처리**  
  TradingSettingService.getSettingOptional(accountNo) 추가(없으면 empty). DashboardController에서 계좌 데이터 조회 시 getSettingOptional 사용, 있으면 setting 모델 추가·없으면 미추가(거래 설정 카드 미표시). 거래 설정 미저장 상태에서도 대시보드(잔고·보유·주문) 정상 표시.
- [x] **테스트 커버리지**  
  JaCoCo 도입(build.gradle), 라인 80%·브랜치 70% 목표 설정(jacocoTestCoverageVerification). 전략(market 파라미터)·뉴스 API·API 컨트롤러(Account, Analysis, MarketData, Setting, UserAccount)·서비스(AccountService, AccountVerificationService, UserExistenceChecker, AnalysisService)·FastApiPredictionClient·Batch 등 단위/슬라이스 테스트 추가. `.\scripts\run-tests-with-coverage.ps1` 또는 `gradlew test jacocoTestReport`로 리포트 생성. 로컬 실행 시 build/agent-build를 사용하는 다른 프로세스가 없을 때 실행할 것(Windows 파일 잠금 시 2회차 재실행 또는 `-NoUniqueDir` 사용).
- [x] **성능 최적화 (1차)**  
  **대시보드**: 잔고·보유·주문·거래설정을 CompletableFuture로 병렬 로딩, SecurityContext 전파(`runWithAuth`)로 인증 유지.
- [x] **대시보드 잔고·보유종목 중복 조회 제거**  
  AccountService에 `getBalanceAndPositions(accountNo)` 추가(주식잔고조회 1회만 호출). DashboardController에서 잔고/보유종목용 Future 2개를 1개로 통합. BalanceAndPositionsDto·@Cacheable(balanceAndPositions_) 적용. API 개요에 단일 리소스용 balance/positions·대시보드용 일괄 조회 설명 반영. **시장 데이터**: 현재가 조회를 동기 캐시 계층(`getCurrentPriceBlocking`)으로 통일 — `@Cacheable`·`@CircuitBreaker` 적용, Mono 반환은 `Mono.fromCallable`로 래핑. 다중 종목 현재가(`getCurrentPrices`)는 종목별 캐시 사용 + CompletableFuture 병렬 조회로 응답 시간 단축. 목표: 시장 데이터·계좌 조회 응답 평균 500ms·95%ile 1초 근접.
- [x] **데이터 수집 연동 (구축 로드맵 1단계)**  
  **공통**: NewsItemRepository.existsBySourceAndUrl, NewsItemService.saveCollectedItem, DataCollectionProperties(DART/KRX/내부 API 키), application.yml investment.data.*. **Open DART**: DartApiClient(공시 목록 list.json), DartCollectionService(공시→NewsItem 저장), DataCollectionScheduler(10분마다 DART 수집). **KRX**: KrxApiClient(유가증권 일별매매정보, AUTH_KEY 헤더), 1단계 연동·DTO만, 저장은 2단계 검토. **Yahoo**: Python scripts/yahoo_collector.py(yfinance 또는 스텁), Spring POST /api/v1/internal/collected-news(X-Internal-Data-Key 헤더), NewsItem(SOURCE=YAHOO_FINANCE, ITEM_TYPE=BUZZ) 저장. Fallback: 원천별 try-catch, 해당 원천만 스킵.
- [x] **팩터 계산 엔진 (구축 로드맵 2단계)**  
  **KRX 일별 저장**: TB_DAILY_STOCK(V4), DailyStock 엔티티·DailyStockRepository, KrxCollectionService(OutBlock_1 파싱·저장), DataCollectionScheduler KRX 일별 수집(매일 16:00 KST). **팩터 계산**: FactorCalculationService(이격도·변동성 돌파·유동성), FactorCalculationScheduler(매일 08:00 KST). **시그널 저장·API**: TB_SIGNAL_SCORE(V5), SignalScore 엔티티·SignalScoreRepository·SignalScoreService, GET `/api/v1/signals` (basDt·market·symbol·factorType·페이징). **자동투자 현황**: 시그널 건수·목록 GET /api/v1/signals 연동, 2단계 카드·시그널 테이블 실데이터 표시. application.yml investment.factor.*, API 개요 반영.
- [x] **4단계 파이프라인 구현 (1차)**  
  **1단계 유니버스**: TB_UNIVERSE(V6), Universe 엔티티·UniverseRepository, UniverseFilterService(유동성 Cut-off), FactorCalculationScheduler에서 유니버스 선행 실행 후 팩터 계산은 유니버스 종목만 대상. **2단계 시그널**: 기존 FactorCalculationService에 유니버스 필터 적용. **3단계 자금 관리**: PositionSizingService(ATR 포지션 사이징·변동성 역가중), PositionRecommendationDto, application.factor.position-risk-pct. **4단계 실행·청산**: TB_STRATEGY_POSITION(V7), StrategyPosition·StrategyPositionRepository, PipelineExecutor(dry-run 기본·auto-execute=false), ExitRuleService(Time-Cut 평가). application.pipeline.auto-execute.
- [x] **4단계 파이프라인 확장**  
  **1) 유니버스**: UniverseFilterService에 한국 Sector Relative Strength·미국 Post-Earnings Drift 필터 스텁 구현(후속 데이터 수집 대비 인터페이스). **2) 시그널**: FactorCalculationService 변동성 돌파 k 동적 적용(한국장 변동성 반영, 최근 5일 평균 변동성 기반 k 조정), 한국 수급 강도(Smart Money Intensity)·미국 듀얼 모멘텀·퀄리티-성장(PEG & Rule of 40) 팩터 스텁 구현. **3) 자금 관리**: PositionSizingService에 Half-Kelly 계산 메서드 추가(기본값 p=0.6, b=2.0, 설정값 지원). **4) 청산**: ExitRuleService에 ATR Trailing Stop 평가 로직 추가(장중 고가·현재가 연동, trailing_high 갱신), PipelineExecutor에 체결 확인 후 포지션 등록 옵션 추가(register-position-on-execution 설정). application.yml investment.factor.volatility-breakout-k-dynamic, investment.factor.kelly-p/b, investment.pipeline.atr-trailing-stop-multiplier, investment.pipeline.register-position-on-execution 설정 추가.
- [x] **시장(Market KR/US) 차원 도입**  
  **스케줄러 확장**: FactorCalculationScheduler를 KR/US 모두 처리하도록 확장(processMarket 메서드 추가, 시장별 예외 처리). **US 데이터 수집 스텁**: UsMarketCollectionService 생성(Yahoo Finance 또는 KIS API 연동 준비, 현재는 스텁). **스케줄러 연동**: DataCollectionScheduler에 US 수집 스케줄 추가(매일 17:00 KST, 미국 장 마감 후). **API 일관성**: Strategy/Signal/News API에서 market 파라미터 일관성 확인 완료. application.yml investment.data.us.schedule-cron 설정 추가.
- [x] **US 일별 시세 수집 (yfinance)**  
  **Python**: scripts/us_daily_collector.py — yfinance로 기준일 US 종목 OHLCV·거래대금(volume×close) 수집, JSON 배열 stdout 출력. **Spring**: UsMarketCollectionService에서 yfinance-script-path 설정 시 스크립트 실행·stdout 파싱·TB_DAILY_STOCK(MARKET=US) 저장. DataCollectionProperties.Us(yfinanceScriptPath, symbols, pythonCommand), application.yml investment.data.us.yfinance-script-path, symbols, python-command. 미설정 시 기존처럼 0 반환(스텁).
- [x] **전략·계산 방식 통합 문서 및 버전 스택**  
  [00-strategy-registry.md](../02-architecture/00-strategy-registry.md) 신설 — 공통·나라별(KR/US)·기간별(단기/중기/장기)·파이프라인 단계·수식·파라미터 일람·버전 스택 반영. 12-auto-investment-strategy는 상세 수식·파라미터를 00-strategy-registry 참조로 정리. development-status.mdc에 전략/팩터 변경 시 통합 문서 갱신·버전 스택 추가 규칙 반영. 전략 통합 문서 반영 (버전 1.0).
- [x] **LSTM 예측 모델(초기)**  
  **데이터·전처리**: app/data(시계열 로드·SeriesDataset), app/preprocessing(정규화·시퀀스 생성), scripts/fetch_training_data.py(yfinance OHLCV CSV). **LSTM·학습**: app/models/lstm_model.py(LSTMPredictor), app/train.py(학습 진입점, state_dict 저장). **서빙**: POST /api/v1/predict에 optional series·currentPrice 추가, MODEL_PATH에서 LSTM lazy 로드, series·모델 있으면 LSTM 추론·없으면 Mock. **Spring 연동**: PredictionRequestDto에 optional series·currentPrice, DailyPricePoint DTO, AnalysisService에서 일별 시세(DailyStockRepository)·현재가(RealtimeMarketDataService) 조회 후 예측 요청에 설정. AI는 분석 정보 제공용, 매매는 규칙 엔진 유지.
- [x] **자동투자 현황 파이프라인 실데이터 연동**  
  **서비스·DTO**: PipelineSummaryService(기준일·계좌별 유니버스 수 KR/US·시그널 건수 KR/US·보유 포지션 수·목록 한 번에 조회), PipelineSummaryDto·OpenPositionItemDto. **컨트롤러·화면**: AutoInvestController에서 pipelineSummaryService.getSummary 호출 후 모델에 반영. **auto-invest.html**: 1단계 카드 유니버스 수(KR·US), 2단계 카드 시그널 건수(KR·US), 4단계 카드 보유 포지션 수 실데이터 표시; 시그널 테이블 시장(KR/US) 컬럼·보유 포지션 테이블 추가. StrategyPositionRepository.countByAccountNoAndExitDtIsNull 추가.
- [x] **자동투자 현황 3단계 자금 배분 요약 표시**  
  PipelineSummaryDto에 allocationSummary 필드 추가. PipelineSummaryService에서 계좌별 TradingSetting(최대 투자금·단기/중기/장기 비율) 기반 예상 배분 금액 계산(단기·중기·장기 만/억 포맷), allocationSummary로 반환. AutoInvestController·auto-invest.html 3단계 카드에 배분 요약 표시(설정 없으면 "준비 중"). [01-screen-menu-spec.md](01-screen-menu-spec.md) §3.2 반영.
- [x] **시장·기간별 전략 로직**  
  **DB**: TB_STRATEGY_POSITION에 STRATEGY_TYPE 추가(V8), StrategyPosition 엔티티·Builder 반영. **청산**: ExitRuleService 기간별 분기 — 단기 -3% Trailing Stop(short-term-trailing-pct), 중기 -10% 손절(medium-term-stop-loss-pct)·Time-Cut, 장기 스텁. **자금관리**: PositionSizingService getRecommendations(basDt, market, strategyType, totalCapital), 단기 RSI>60 & MACD>Signal(TechnicalIndicatorUtil), 중기 시그널 상위 10%, 장기 전체. **실행**: PipelineExecutor run(..., strategyType, allocatedCapital), 포지션 저장 시 strategyType. **스케줄러**: PipelineExecutionScheduler(09:10, 0.2/0.4/0.4 배분·6회 run), MediumTermRebalanceScheduler(매월 1일 스텁). application.yml pipeline.short-term-trailing-pct, medium-term-stop-loss-pct, execution-schedule-cron, scheduler.default-capital, medium-term-rebalance-cron. [00-strategy-registry.md](../02-architecture/00-strategy-registry.md) 기간별 청산·시드 배분·버전 스택 v1.1 반영.
- [x] **ATR Trailing Stop 장중 연동 및 체결 확인 후 포지션 등록**  
  **청산 장중 연동**: ExitRuleService.getSellSignals(accountNo, currentPriceBySymbol, todayHighBySymbol) 오버로드 — todayHighBySymbol이 있으면 장중 당일 고가로 trailingHigh 갱신. PipelineExitScheduler(exit-schedule-cron: 장중 5분마다) — 보유 포지션에 대해 RealtimeMarketDataService.getCurrentPrices로 현재가·당일 고가 조회 후 getSellSignals 호출, auto-execute 시 매도 주문 실행·포지션 close. **체결 확인 후 포지션 등록**: TB_ORDERS에 POSITION_BAS_DT·POSITION_MARKET·POSITION_STRATEGY_TYPE 추가(V9), Order.setPositionContext/clearPositionContext. PipelineExecutor에서 register-position-on-execution=true 시 주문 성공 후 Order에 포지션 컨텍스트 저장. FillConfirmationScheduler(fill-confirmation-cron: 매분) — EXECUTED·positionBasDtNotNull 주문에 대해 PipelineExecutor.registerPositionOnExecution 호출 후 컨텍스트 초기화. OrderService.executeOrderForPipeline(request, userId) 추가(파이프라인/청산 스케줄러용). application.yml pipeline.exit-schedule-cron, fill-confirmation-cron. [00-strategy-registry.md](../02-architecture/00-strategy-registry.md) 버전 스택 v1.2 반영.
- [x] **백테스팅 (필수)**  
  **청산 규칙 공통화**: ExitRuleEvaluator·ExitRuleInput·ExitRuleResult — 포지션 + 당일 시세만으로 청산 여부 판단. ExitRuleService는 evaluator 호출로 동일 동작 유지. **백테스트 엔진**: BacktestService — 과거 일봉(TB_DAILY_STOCK)·시그널(TB_SIGNAL_SCORE)로 4단계 파이프라인 일자별 재생, 가상 포지션·거래 목록·수익 곡선 산출. **메트릭**: MDD·CAGR·Sharpe·Sortino·Calmar·승률(winRate)·손익비(profitFactor) 노출(Half-Kelly p·b 연동용). **API**: POST /api/v1/backtest (BacktestRunRequest → BacktestRunResult). **화면**: /backtest 메뉴·BacktestWebController·backtest.html(조건 폼·결과 요약·수익 곡선·거래 목록). 실전 반영(전략별 p·b 주입)은 별도 태스크.
- [x] **모의계좌 자동투자 실행 가능화**  
  **스케줄러 매수 시 사용자 컨텍스트**: PipelineExecutor에서 actuallyExecute 시 accountNo → TradingSettingRepository로 userId 조회 후 `OrderService.executeOrderForPipeline(request, userId)` 호출(스케줄러는 SecurityContext 없음). **실행 대상 계좌**: PipelineExecutionScheduler가 `TradingSettingRepository.findAllByAutoTradingEnabledTrue()`로 자동투자 ON 계좌만 대상. **실제 주문 활성화 조건**: 실제 매수/매도가 나가려면 `investment.pipeline.auto-execute=true`(또는 환경변수 `PIPELINE_AUTO_EXECUTE=true`) 필요. 기본값은 `false`라 설정하지 않으면 dry-run만 동작(주문 생성 없음). 실계좌 자동투자 전 조건: 모의 2주 테스트 권장, KIS 실전 URL·Throttling·토큰 갱신 등은 [12-auto-investment-strategy](../02-architecture/12-auto-investment-strategy.md) §8 및 [로드맵](../roadmap.md) Phase 7 참조.
- [x] **스케줄 현황 메뉴 및 설정 전용 화면**  
  **스케줄 현황**: MenuConfig에 "스케줄 현황"(/batch) 메뉴 추가. BatchManagementService에 데이터 수집(DART/SEC/KRX/US)·팩터 계산·파이프라인 실행/청산/체결확인·중기 리밸런스 등 전체 스케줄 작업 목록 반영. batch-management.html 공통 레이아웃(layout-header·layout-menu) 적용, 제목 "스케줄 현황". **설정 전용 화면**: "설정" 메뉴 경로를 /settings로 변경. GET /settings → settings.html. **계좌/API 한번에**: GET/PUT /api/v1/settings/accounts — 모의·실 계좌 블록 한번에 조회·수정. AuthService getSettingsAccounts/updateSettingsAccounts, SettingsAccountsResponseDto·SettingsAccountBlockDto·SettingsAccountsUpdateRequestDto. **거래 설정**: TB_TRADING_SETTINGS에 단기/중기/장기 비율 컬럼 추가(V10). TradingSetting·TradingSettingDto·TradingSettingService 비율 필드·검증(합=1). settings.html에서 계좌/API 한번에 저장·거래 설정(비율·자동투자 ON/OFF) 계좌별 저장. **모의계좌 자동투자 설정 반영**: PipelineExecutionScheduler가 findAllByAutoTradingEnabledTrue()로 자동투자 ON 계좌만 대상, 계좌별 maxInvestmentAmount·shortTermRatio/mediumTermRatio/longTermRatio 사용(NULL이면 기본 0.2/0.4/0.4). [00-strategy-registry.md](../02-architecture/00-strategy-registry.md) 버전 스택 v1.3 반영.
- [x] **모의/실계좌 전역 탭·대시보드 구역 분리·URL 정합성·메뉴 순서·page-title 제거**  
  **전역 탭**: layout-menu에 모의계좌 | 실계좌 탭 추가(로그인 사용자만). URL 쿼리 `serverType=1`/`0` 유지, MenuModelAdvice에서 `currentServerType`·`currentPath` 주입. **대시보드**: 모의·실 계좌를 각각 조회해 한 화면에 "모의계좌"·"실계좌" 두 구역으로 표시(DashboardController·dashboard.html). **URL 정합성**: 메뉴 링크·빠른 액션·설정·전략 등 내부 링크에 serverType 포함. Orders/AutoInvest/전략 컨트롤러에 serverType 파라미터 반영·getMainAccount(userId, serverType) 사용. **메뉴 순서**: 설정을 맨 끝으로(MenuConfig·01-screen-menu-spec 백테스트 9, 설정 10). **page-title 제거**: 모든 메뉴 템플릿에서 `<h1 class="page-title">` 제거. 설정·마이페이지·백테스트·포트폴리오에 userInfo 주입(탭·헤더 표시). [01-screen-menu-spec.md](01-screen-menu-spec.md)·[08-frontend-architecture.md](../02-architecture/08-frontend-architecture.md) 반영.
- [x] **대시보드·UX 1차**  
  **계좌 요약(국내·미국 구분)**: AccountPositionDto에 market(KR/US) 필드 추가. KoreaInvestmentAccountClient.parsePositionsOutput에서 응답의 excg_dvsn_cd로 KR/US 매핑(KRX→KR, NASD/NYSE/AMEX→US). DashboardController에서 시장별 보유 종목 수(positionCountKr/Us) 집계, dashboard.html에 "계좌 요약 (국내·미국)" 카드·보유 종목 테이블 시장 컬럼 추가. **자동투자 상태 카드**: PipelineSummaryService.getSummary(오늘, accountNo) 연동, 자동 매매 ON/OFF·유니버스·시그널(KR/US)·보유 포지션 수 표시, "자동투자 현황 자세히 보기" 링크. 설정 링크를 /settings로 통일. [01-screen-menu-spec.md](01-screen-menu-spec.md) §3.1·[09-korea-investment-api-guide.md](../04-api/09-korea-investment-api-guide.md) 잔고·보유 시장 구분 반영.
- [x] **트레이딩 포트폴리오 생성 로직을 파이프라인 기반으로 통합**  
  **1차**: ShortTermTradingStrategyService에서 PositionSizingService.getRecommendations(tradingDate, KR, SHORT_TERM, defaultCapital)로 TB_SIGNAL_SCORE + TB_DAILY_STOCK 기반 단기 권장 종목 사용. PositionRecommendationDto → TradingPortfolioItem 변환(목표가 R:R 2:1/3:1, 진입가 ±1%). **2차**: 시그널 없으면 StockScreeningService fallback 또는 모의 데이터. **N+1 제거**: KoreaInvestmentMarketDataClient에서 토큰/API키 5초 TTL 캐시(tokenInfo: token, serverType, appKey, appSecret)로 동일 요청 내 재사용, getChartData/getCurrentPriceFromApi는 tokenInfo만 사용. **스케줄**: TradingPortfolioScheduler 09:00 KST(팩터 08:00 이후). **리스크 문구**: generateRiskManagementStrategy를 전략 레지스트리(포지션 리스크 1%, ATR, Half-Kelly, 단기 -3% Trailing Stop)와 동일하게 수정. application.yml investment.trading-portfolio.default-capital.
- [x] **해외(미국) 잔고·보유 조회 API 연동**  
  **상수**: KoreaInvestmentAccountApiConstants에 PATH_OVERSAS_INQUIRE_PRESENT_BALANCE, TR_ID_OVERSAS_BALANCE_REAL/VIRTUAL(CTRP6504R/VTRP6504R), getOverseasBalanceTrId. **클라이언트**: KoreaInvestmentAccountClient.inquireOverseasBalance(userId, accountNo) — GET+query(CANO, ACNT_PRDT_CD, WCRC_FRCR_DVSN_CD=02, NATN_CD=840, TR_MKET_CD=00, INQR_DVSN_CD=00), output1 파싱·parseOverseasPositionsOutput·parseOverseasPositionItem(ovrs_* 등 필드 대응), market=US·currency=USD. **서비스**: AccountService.getBalanceAndPositions에서 국내 inquireBalance 후 inquireOverseasBalance 호출해 US 보유 목록 병합. 실패 시 해외만 스킵·국내만 반환. [09-korea-investment-api-guide.md](../04-api/09-korea-investment-api-guide.md) 해외주식 현재잔고 조회 섹션 추가.
- [x] **모의계좌 투자 실제 실행 준비**  
  **토큰 서버 타입별 저장**: TB_KOREA_INVESTMENT_TOKENS에 SERVER_TYPE 추가(V12), UK(USER_ID, SERVER_TYPE). KoreaInvestmentToken 엔티티·KoreaInvestmentTokenRepository.findByUserIdAndServerType. **TokenService**: getAccessToken(userId, serverType) 추가, 발급/저장 시 serverType 반영, getAccessToken(userId)는 serverType="1" 위임(호환). **주문 경로 계좌별 키·토큰**: KoreaInvestmentOrderClient에서 accountNo → resolveServerTypeForAccount, getUserApiKeyForAccount(userId, accountNo), getAccessToken(userId, serverType) 사용. **실전 계좌 실행 가드**: investment.pipeline.allow-real-execution(false 기본). PipelineExecutor·PipelineExitScheduler에서 serverType='0' 계좌는 allow-real-execution=false 시 주문 스킵(로그 경고). application.yml pipeline.allow-real-execution, PIPELINE_ALLOW_REAL_EXECUTION. (V12 롤백 스크립트는 Flyway·마이그레이션 정리로 제거됨. 필요 시 git history 참조.)
- [x] **4단계 파이프라인 확장 (데이터 수집 후 실제 구현)**  
  **1) 미국 듀얼 모멘텀**: FactorCalculationService — TB_DAILY_STOCK(US) 기간별 수익률 가중합(dual-momentum-period-days/weights), 시장 모멘텀=유니버스 평균, score=종목 모멘텀−시장 모멘텀(%). **2) Half-Kelly p·b 백테스트 연동**: PositionSizingService.applyHalfKelly(전략별 p·b), application.yml kelly-p/kelly-b + kelly-p-short-term 등 전략별 키(백테스트 winRate·profitFactor 반영용). **3) 유니버스**: TB_SECTOR_RETURN·TB_SYMBOL_SECTOR(V13), TB_EARNINGS_SURPRISE(V14). UniverseFilterService — Sector RS(상위 N개 업종 내 종목), Post-Earnings Drift(최근 N일 실적 발표 상위 20%), 데이터 없으면 유동성만. sector-rs-top-n, earnings-surprise-lookback-days, earnings-surprise-top-pct. **4) 시그널**: TB_ORDER_FLOW(V15), TB_FUNDAMENTALS(V16). FactorCalculationService — 수급 강도(KR) TB_ORDER_FLOW 기반 순매수/시총 비율(%), 퀄리티-성장(US) TB_FUNDAMENTALS 기반 PEG·Rule of 40 합산 점수, 데이터 없으면 0. smart-money-intensity-threshold-pct. [00-strategy-registry.md](../02-architecture/00-strategy-registry.md) 버전 스택 v1.5 반영.
- [x] **로보 어드바이저 백테스트·자동투자 연동**  
  **백테스트 엔진**: RoboAllocationEngine(공통 스코어링·비중), RoboBacktestService(과거 일봉 재생·CAGR·MDD·Sharpe·Calmar·Turnover·벤치마크·리밸런싱 이력). **API**: POST /api/v1/backtest/robo, GET /api/v1/backtest/robo/last-pre-execution. **UI**: /backtest 모드 선택(4단계 파이프라인 | 로보어드바이저), 로보 간단/고급 폼·해석 문구·메트릭 카드·수익 곡선 vs 벤치마크·리밸런싱 이력. **자동투자 연동**: TB_TRADING_SETTINGS ROBO_ADVISOR_ENABLED(V17), RoboRebalanceScheduler(월/분기 말 실행), 실행 전 백테스트(최근 N개월)·정책(MDD·Sharpe) 통과 시에만 RoboRebalanceExecutor 호출(목표 비중 로깅, ETF 주문 연동 추후). RoboPreExecutionResultStore(실행 전 결과 저장)·설정 화면 로보 어드바이저 ON/OFF. [00-strategy-registry.md](../02-architecture/00-strategy-registry.md) v1.6 반영.
- [x] **월스트리트 정렬(한국·미국)**  
  **한국(KR)**: 청산 -5% 고정 손절·전저점 이탈·RSI≥70 익절(ExitRuleEvaluator·ExitRuleService·StrategyPosition PRIOR_LOW V18); 진입 5일 연속 수급 메타(TB_ORDER_FLOW NET_BUY_AMT_1D V19)·역발상 RSI(CONTRARIAN_RSI)·P/B 필터 스텁(UniverseFilterService). **미국(US)**: 듀얼 모멘텀(노트) 모드 — 절대 SPY 12M vs T-bill·상대 섹터 ETF 6M 상위 2개(RoboAllocationEngine.computeTargetWeightsDualMomentumNote·RoboBacktestService·RoboRebalanceExecutor 모드 분기). application.yml pipeline.short-term-kr-stop-loss-pct·prior-low-stop-kr-enabled·rsi-exit-threshold·factor.contrarian-rsi-threshold·pb-value-min/max·backtest.robo.dual-momentum-mode·sector-etf-symbols 등. [00-strategy-registry.md](../02-architecture/00-strategy-registry.md)·[12-auto-investment-strategy.md](../02-architecture/12-auto-investment-strategy.md) v1.7 반영.
- [x] **자동매매 직전 점검 보완 (Go/No-Go)**  
  **Time-Cut 단기 전용**: ExitRuleEvaluator에서 Time-Cut을 SHORT_TERM에만 적용, MEDIUM_TERM/LONG_TERM 제거. PipelineExecutor·BacktestService 포지션 생성 시 SHORT_TERM만 timeCutDays/targetReturnPct 설정. **Hunter 분기(KR 단기)**: PositionSizingService.filterSymbolsKrShortTerm — Case A(수급 강함 → RSI&gt;60 &amp; MACD) ∪ Case B(역발상 RSI&lt;40). **시초가 유동성**: liquidity-min-trd-val-opening(300억), KR 단기 getRecommendations에서 적용. **켈리 초기 고정 비율**: kelly-enabled(false)·kelly-fixed-allocation-pct(2), applyHalfKelly에서 비활성 시 고정 비율만 적용. **미국 갭 스킵**: us-gap-up-skip-pct(5), US 파이프라인에서 전일 대비 갭 N% 이상 종목 제외. **Discord 긴급 알림**: EmergencyAlertService·DiscordEmergencyAlertService(알림에 userId·계좌 마스킹·모의/실전·증권사·URL 포함), UnfilledOrderCheckScheduler(PENDING N분 경과 시 알림), alert-discord-webhook-url·alert-base-url·unfilled-check-minutes. OrderRepository.findByStatusAndOrderTimeBefore. [00-strategy-registry.md](../02-architecture/00-strategy-registry.md) v1.8·[12-auto-investment-strategy.md](../02-architecture/12-auto-investment-strategy.md) §6.2 체크리스트 반영.
- [x] **자동매매 실제 API 연동(KR/US)**  
  **국내 주문 MCP 검증**: 한국투자증권 MCP search_domestic_stock_api(order_cash)로 국내 주문 API 스펙 확인 — 엔드포인트·TR_ID·필수 파라미터·Hashkey 일치. **해외 주문 API 연동**: MCP search_overseas_stock_api(order) 기반 해외주식 주문(/uapi/overseas-stock/v1/trading/order, TTTT1002U/VTTT1002U 매수, TTTT1006U/VTTT1006U 매도) 구현. KoreaInvestmentOrderClient에 placeOverseasBuyOrder·placeOverseasSellOrder 추가(미국 NASD, 지정가 00). **시장 분기**: OrderRequestDto에 market(KR/US) 추가·getMarketOrKr(), OrderService.executeOrderInternal에서 market=US 시 해외 주문 호출. **파이프라인·청산**: PipelineExecutor OrderRequestDto 생성 시 rec.getMarket() 설정, PipelineExitScheduler 매도 요청에 position.getMarket() 설정·시세 없을 때 해당 건 스킵. [09-korea-investment-api-guide.md](../04-api/09-korea-investment-api-guide.md) 국내/해외 주문 API 섹션 추가.
- [x] **전문 투자자 흐름 P0~P3**  
  **P0 리스크 게이트·일일 손실 한도**: RiskProperties·RiskGateService(레짐·VIX·MacroEconomicStrategyEngine 연동), DailyLossLimitService(시초 평가액 기록·isNewBuyAllowed), PipelineExecutionScheduler 실행 전 검사·비중 배율 적용. AccountService.getBalanceAndPositionsWithUserId(파이프라인용). **P1 실행가·상한**: RoboBacktestService rebalanceExecutionPrice(CLOSE/NEXT_OPEN), getOpen·getExecutionPrice. PositionSizingService position-cap-per-symbol-pct·max-new-positions-per-day. **P2 장중 변동성 돌파**: IntradayBreakoutService(전일 유니버스·시가+Range×k·실시간 시세), IntradayBreakoutScheduler(09:10·09:40), BreakoutCandidateDto. **P3 일일 PnL 리뷰**: DailyPnlService·DailyPnlScheduler(16:05), DailyLossLimitService.getOpeningBalance. application.yml investment.risk.*, investment.intraday.*, investment.daily-pnl.*. [00-strategy-registry.md](../02-architecture/00-strategy-registry.md) §2.9·§3.2.1, [12-auto-investment-strategy.md](../02-architecture/12-auto-investment-strategy.md) §6.2 반영.
- [x] **P0~P3 후속: 테스트·VIX·로보 ETF 주문**  
  **테스트**: RiskGateService·DailyLossLimitService·IntradayBreakoutService·DailyPnlService 단위 테스트, PipelineExecutionScheduler·IntradayBreakoutScheduler·DailyPnlScheduler 테스트 추가. **VIX·거시 지표 연동**: MacroIndicatorProvider 인터페이스·DefaultMacroIndicatorProvider(설정 URL GET JSON 파싱 vix/interestRate 등), PipelineExecutionScheduler에 주입·getCurrentIndicators() → evaluateWithIndicators/evaluate(vix). investment.risk.macro-indicator-url(선택). **로보 ETF 주문 실행**: RoboRebalanceExecutor에서 AccountService.getBalanceAndPositionsWithUserId·US 보유 비중 조회 후 목표 비중과 비교해 매수/매도 OrderRequestDto(market=US) 생성·OrderService.executeOrderForPipeline 호출. execute-orders(false 기본)·min-order-amount-usd(50). [00-strategy-registry.md](../02-architecture/00-strategy-registry.md)·[12-auto-investment-strategy.md](../02-architecture/12-auto-investment-strategy.md) 반영.

### 자동투자 프로세스·활성화 체크리스트

실제 자동 매수/매도 주문이 나가게 하려면: **(1)** 설정 화면(/settings)에서 거래 설정 저장, **자동 매매 ON**, 최대 투자금액·단기/중기/장기 비율 입력. **(2)** 서버/환경에서 `PIPELINE_AUTO_EXECUTE=true` 또는 `investment.pipeline.auto-execute: true` 설정(기본값 false이면 dry-run만 동작). **(3)** 모의계좌 권장(실전 전 2주 테스트). **(4)** 실전 계좌 자동 실행은 `PIPELINE_ALLOW_REAL_EXECUTION=true`(또는 `investment.pipeline.allow-real-execution: true`)로만 허용(기본값 false). 상세 플로우·스케줄은 [12-auto-investment-strategy §6.2](../02-architecture/12-auto-investment-strategy.md#62-자동투자-프로세스-플로우) 참조.

---

## 2. 진행중 (In progress)

- **로보·파이프라인 통합 복합 로직**: 자동매수 = 파이프라인+로보 통합 복합 로직(한 오케스트레이션)으로 문서·설계 목표 반영. 트리거 API·스케줄 현황 "지금 실행" 버튼·백테스트 기본 기간(최근 1개월) 구현 진행.

---

## 3. 진행예정 (Planned)

산출 기획([자동투자 전략 명세](../02-architecture/12-auto-investment-strategy.md), [뉴스·공시 수집·연동 설계](../02-architecture/13-news-collection-design.md), [화면·메뉴 기획서](./01-screen-menu-spec.md), [로드맵](../roadmap.md))을 토대로 구체화한 항목입니다.

### 메뉴별 백엔드 순차 개발 (퍼블 메뉴 전부 필요 기능)

- [ ] **메뉴별 백엔드 개발 순차 진행**  
  [11-api-frontend-mapping.md §4](../04-api/11-api-frontend-mapping.md) 메뉴(라우트)별 백엔드 API 필요·연동 현황 및 §5.2 미구현·미연동 우선순위를 기준으로, 백엔드 개발이 필요한 메뉴를 하나씩 구현(API 추가·수정 → 프론트 연동 → 문서 갱신). Ops 전용 메뉴(데이터 파이프라인, 알림센터, 리스크, 모델/예측, 감사 로그, 시스템 헬스)는 각 메뉴별 필요한 백엔드 기능을 11-api-frontend-mapping에 나열한 대로 순차 진행.

### 단기 (로드맵 Phase 1~4 대응)

- [x] **테스트 코드 보강 (1차)**  
  단위·슬라이스 테스트 추가: PasswordValidator, GlobalExceptionHandler, NewsItemService, NewsController, StrategyManagementService, StrategyApiController, AuthController, AuthService. 기존 OrderController/OrderService/TradingSettingService/KoreaInvestmentMarketDataClient/StockCodeConverter 테스트 수정. getBulkIndicators 모의 데이터 분기 추가. WebMvcTest에 SecurityConfig 의존 MockBean 추가. 전체 테스트 실행: `$env:GRADLE_UNIQUE_BUILD_DIR='1'; .\gradlew test` 또는 `.\scripts\run-tests.ps1`. (AuthController getMyPage 슬라이스 테스트 1건은 addFilters=false 시 principal 미전달로 @Disabled.)
- [x] **성능 최적화 (1차)**  
  대시보드 병렬 로딩(잔고·보유·주문·설정), 현재가 동기 캐시·다중 종목 병렬 조회 적용. 상세는 완료 섹션 참조.
- [ ] **성능 최적화 (2차·선택)**  
  쿼리·캐싱·비동기 추가 적용. 시장 데이터·종목 분석·계좌 조회 응답 시간 목표(평균 500ms, 95%ile 1초) 측정·튜닝.
- [x] **데이터 수집 연동 (구축 로드맵 1단계)**  
  DART/KRX/Yahoo 연동·수집·저장·스케줄 적용 완료. 상세는 완료 섹션 참조.
- [x] **팩터 계산 엔진 (구축 로드맵 2단계)**  
  완료. 상세는 완료 섹션 참조.
- [x] **LSTM 예측 모델(초기)**  
  완료. 상세는 완료 섹션 참조.

### 중기 (로드맵 Phase 5~6)

- [x] **4단계 파이프라인 구현 (1차)**  
  유니버스(유동성만)·시그널 유니버스 필터·PositionSizingService·PipelineExecutor·ExitRuleService(Time-Cut) 완료. 상세는 완료 섹션 참조.
- [x] **4단계 파이프라인 확장 (데이터 수집 후 실제 구현)**  
  완료. 상세는 완료 섹션 참조.
- [x] **시장·기간별 전략 로직**  
  완료. 상세는 완료 섹션 참조.
- [ ] **뉴스·공시 파이프라인 (확정 원천만)**  
  **한국**: DART(Open API) 실시간/단기 폴링 공시, 키워드(무상증자·영업익 30% 증가 등) 포착 시 매수 시그널; 연합뉴스 수집·NLP·속보/긴급 가중치; 네이버 금융(많이 본 뉴스·실시간 검색 종목) 수집·이용약관 준수. **미국**: SEC EDGAR API(8-K·10-K·10-Q), 8-K 발생 시 **최우선 순위** 로직; Reuters 헤드라인·감정 분석; Yahoo Finance(OHLCV·Earnings Calendar·Analyst Up/Down). 수집·저장(TB_NEWS_ITEMS)·감정/중요도/이벤트 유형 분석, 전략 시그널 점수 반영, Fallback(원천 장애 시 파이프라인 중단 없음).
- [ ] **고급 분석·포트폴리오**  
  섹터 분석, 상관관계·리스크 메트릭(VaR/CVaR, Sharpe/Sortino), 리밸런싱 자동화, 리스크 기반 포지션 사이징.
- [ ] **대시보드·UX**  
  자동투자 현황 파이프라인 실데이터·시그널/보유 포지션 테이블은 완료. 대시보드: 계좌 요약(국내·미국 구분), 자동투자 상태 카드. 실시간 차트, 성과 분석, 반응형·모바일.
- [ ] **로보어드바이저 사용자 플로우 명확화**  
  랜딩(/) 한 줄 문구("나 대신 투자해주는 로보어드바이저")·CTA 강화. 대시보드·자동투자 현황·설정에 다음 액션(설정으로 가기 등) 및 안내 문구 반영. [00-robo-advisor-product-summary.md](00-robo-advisor-product-summary.md), [03-user-flows.md](03-figma-wireframes/03-user-flows.md) 신규 사용자 권장 경로 참조.

### 장기 (로드맵 Phase 7~8)

- [ ] **KIS Open API 실전 구축**  
  **WebSocket 우선**: 실시간 호가/체결가(Tick) 구독, 변동성 돌파 시그널 0.1초 단위 감시; 체결 Push 수신 시 익절/손절 대기 로직 즉시 활성화. **REST 퀀트 스코어링**: 국내 순위 분석 API(거래대금·등락률 상위)→주도주 유니버스 매일 아침 갱신; 투자자별 매매동향 API→10분 단위 수급 점수; 미국 해외주식 기간별 시세(환율 포함)·데이터 정합성. **리스크**: Throttling(실전 초당 20회, 주문 2~10회)→주문 요청 큐(메시지 큐 RabbitMQ 등) 순차 처리; Access Token **장 시작 30분 전** Crontab 자동 갱신. **시드·주문**: 국내 지정가·최유리 지정가, 예수금 30~50% 변동성 비중; 미국 실시간 시세(유료)·시장가, 통합증거금; 모의투자 2주 테스트 후 실전.
- [ ] **다중 계좌·실시간 스트리밍**  
  다중 계좌 관리, WebSocket 시세·알림, 통합 포트폴리오 뷰.
- [ ] **화면 확장 (기획서 향후 메뉴)**  
  백테스트(`/backtest`) 결과·파라미터 화면은 완료. 리스크 리포트(`/risk`) MDD·VaR·노출 등.
- [ ] **모바일 앱**  
  iOS/Android, 푸시 알림 (선택).

---

## 4. 참조 관계

| 문서 | 역할 |
|------|------|
| [로보어드바이저 제품 요약](00-robo-advisor-product-summary.md) | 첫 목적 기준 한 줄·핵심 플로우·완료/부족/필요·화면–API 매핑 요약 |
| [로드맵](../roadmap.md) | Phase별 목표·일정·체크리스트 |
| [화면·메뉴 기획서](./01-screen-menu-spec.md) | 메뉴 트리·화면 역할·확장 규칙 |
| [자동투자 전략 명세](../02-architecture/12-auto-investment-strategy.md) | 4단계 파이프라인·시장별 알고리즘·원천·KIS 실전 구축·구축 로드맵 |
| [뉴스·공시 수집·연동 설계](../02-architecture/13-news-collection-design.md) | 확정 원천·수집·저장·감정/중요도·전략 연동 |
| [PRD](../PRD.md) | 제품 목표·기능·비기능 요구사항 |
| [API 개요](../04-api/01-api-overview.md) | API 목록·버전·인증 |
| [구현 계획](../02-architecture/04-implementation-plan.md) | Phase별 구현 상세(참고) |

**변동 시**: 완료 항목 추가·이동, 진행중 항목 추가/제거, 진행예정 순서 조정 시 이 문서를 먼저 수정하고, 필요 시 roadmap.md 체크박스·일정을 맞춘다.

---

## 5. 문서 변경 이력

| 버전 | 일자 | 변경 내용 |
|------|------|----------|
| 1.0 | 2026-01-29 | 초기 작성 — 완료/진행중/진행예정 구분, 참조 관계 정리 |
| 1.1 | 2026-01-29 | 진행예정 항목 구체화 — 산출 기획(12·13·01-screen-menu·roadmap) 반영, 4단계 파이프라인·시장별 알고리즘·뉴스 원천·KIS 실전 구축·백테스트·화면 확장 상세화 |
| 1.2 | 2026-01-29 | 완료: 모의/실전 계좌·계좌인증 — 토글 반전 수정, verify-account API, 회원가입 폼 계좌인증·마스킹, 마이페이지 토글 규칙 통일 |
| 1.3 | 2026-01-29 | 완료: JWT 인증 시 사용자 존재 여부 검증(방어코드) — UserExistenceChecker, JwtAuthenticationFilter DB 검사·쿠키 제거 |
| 1.4 | 2026-01-30 | 완료: 한국투자증권 API 조회 GET+query 수정 — AccountClient 7곳·MarketDataClient 2곳 GET+queryParam 적용, MCP 필수 사용·작업 중 문서 업데이트 규칙 반영 |
| 1.5 | 2026-01-30 | 완료: 대시보드 거래 설정 optional 처리 — TradingSettingService.getSettingOptional, DashboardController optional 조회, 설정 없을 때 오류 제거; 대시보드 사용 전 확인 사항(설정값) 문서 반영 |
| 1.6 | 2026-01-30 | 완료: 테스트 커버리지 — JaCoCo 도입, 80% 라인·70% 브랜치 목표, API 컨트롤러·서비스·전략·뉴스·Batch 등 단위/슬라이스 테스트 추가, run-tests-with-coverage.ps1 |
| 1.7 | 2026-01-30 | 완료: 성능 최적화 (1차) — 대시보드 잔고·보유·주문·설정 병렬 로딩, 현재가 동기 캐시 계층·다중 종목 병렬 조회 |
| 1.8 | 2026-01-30 | 완료: 데이터 수집 연동 (1단계) — DART 공시·KRX 시세·Yahoo 내부 API 연동, NewsItem 저장·스케줄러·Fallback |
| 1.9 | 2026-01-30 | 완료: 팩터 계산 엔진 (2단계) — KRX 일별 TB_DAILY_STOCK 저장·KrxCollectionService·FactorCalculationService(이격도·변동성 돌파·유동성)·TB_SIGNAL_SCORE·GET /api/v1/signals·자동투자 현황 시그널 연동 |
| 1.10 | 2026-01-30 | 완료: 4단계 파이프라인 (1차) — TB_UNIVERSE·UniverseFilterService·유니버스 선행 스케줄·PositionSizingService·TB_STRATEGY_POSITION·PipelineExecutor·ExitRuleService(Time-Cut)·pipeline.auto-execute |
| 1.11 | 2026-01-30 | 완료: 4단계 파이프라인 확장 — ExitRuleService ATR Trailing Stop, FactorCalculationService 변동성 돌파 k 동적 적용·시그널 스텁(수급 강도·듀얼 모멘텀·퀄리티-성장), PositionSizingService Half-Kelly, PipelineExecutor 체결 확인 후 포지션 등록 옵션, UniverseFilterService 유니버스 필터 스텁(Sector RS·Post-Earnings Drift). application.yml 설정 추가. |
| 1.12 | 2026-01-30 | 완료: 시장(Market KR/US) 차원 도입 — FactorCalculationScheduler KR/US 모두 처리(processMarket 메서드), UsMarketCollectionService 스텁 추가, DataCollectionScheduler US 수집 스케줄, API 일관성 확인. application.yml investment.data.us.schedule-cron 추가. |
| 1.13 | 2026-01-31 | 완료: 전략·계산 방식 통합 문서 및 버전 스택 — 00-strategy-registry.md 신설(공통·나라별·기간별·파이프라인·수식 일람·버전 스택), 12-auto-investment-strategy 상세 참조 정리, development-status.mdc 전략/팩터 변경 시 통합 문서 갱신 규칙 추가. |
| 1.14 | 2026-01-31 | 완료: US 일별 시세 수집 (yfinance) — scripts/us_daily_collector.py(yfinance OHLCV·trdVal), UsMarketCollectionService 스크립트 호출·JSON 파싱·TB_DAILY_STOCK(MARKET=US) 저장. investment.data.us.yfinance-script-path, symbols, python-command. |
| 1.15 | 2026-01-31 | 완료: LSTM 예측 모델(초기) — 데이터·전처리(app/data, app/preprocessing, fetch_training_data), LSTM·학습(lstm_model, train), 서빙(optional series·currentPrice, MODEL_PATH lazy 로드), Spring DTO·AnalysisService 연동(일별 시세·현재가 채움). 00-strategy-registry AI/LSTM 활용 방침, API 개요 예측 optional series 반영. |
| 1.16 | 2026-01-31 | 완료: 자동투자 현황 파이프라인 실데이터 연동 — PipelineSummaryService·PipelineSummaryDto·OpenPositionItemDto, AutoInvestController 파이프라인 요약 모델, auto-invest 4단계 카드(유니버스·시그널 KR/US·보유 포지션 수)·시그널/보유 포지션 테이블. StrategyPositionRepository.countByAccountNoAndExitDtIsNull. |
| 1.17 | 2026-01-31 | 완료: 시장·기간별 전략 로직 — TB_STRATEGY_POSITION STRATEGY_TYPE(V8), ExitRuleService 기간별 청산(-3%/-10%/장기 스텁), PositionSizingService strategyType·RSI/MACD·상위 10% 필터, PipelineExecutor strategyType·allocatedCapital, PipelineExecutionScheduler·MediumTermRebalanceScheduler(스텁), 00-strategy-registry v1.1 반영. |
| 1.18 | 2026-01-31 | 완료: ATR Trailing Stop 장중 연동 및 체결 확인 후 포지션 등록 — ExitRuleService todayHighBySymbol 오버로드, PipelineExitScheduler(실시간 현재가·당일 고가→청산 평가·매도 실행), TB_ORDERS 포지션 컨텍스트(V9), FillConfirmationScheduler, OrderService.executeOrderForPipeline, 00-strategy-registry v1.2 반영. |
| 1.19 | 2026-02-01 | 완료: 백테스팅 — ExitRuleEvaluator·ExitRuleInput/Result, BacktestService·BacktestRunRequest/Result/TradeDto, POST /api/v1/backtest, /backtest 메뉴·화면, MDD/CAGR/Sharpe/Sortino/Calmar·승률·손익비 노출. |
| 1.20 | 2026-02-01 | 완료: 모의계좌 자동투자 실행 가능화 — PipelineExecutor 스케줄러 경로 executeOrderForPipeline·userId(TradingSetting 조회), PipelineExecutionScheduler 대상 계좌 TradingSetting 기준, TradingSettingRepository.findDistinctAccountNos. |
| 1.21 | 2026-02-01 | 완료: 대시보드·UX 1차 — 계좌 요약(국내·미국 구분) AccountPositionDto.market·excg_dvsn_cd 매핑·시장별 집계·보유 테이블 시장 컬럼; 자동투자 상태 카드 PipelineSummaryService 연동·설정 링크 /settings. |
| 1.22 | 2026-02-01 | 완료: 해외(미국) 잔고·보유 조회 API — inquire-present-balance(GET+query), inquireOverseasBalance·parseOverseasPositionsOutput, getBalanceAndPositions에서 국내+해외 병합. |
| 1.23 | 2026-02-01 | 자동투자 프로세스·활성화 체크리스트 문단 추가(§1 직후). 모의계좌 자동투자 실행 가능화 항목에 실제 주문 활성화 조건(auto-execute=true 필요, 기본값 false) 보강. 12-auto-investment-strategy §6.2 참조. |
| 1.24 | 2026-02-01 | 완료: 모의계좌 투자 실제 실행 준비 — 토큰 서버 타입별 저장(V12), getAccessToken(userId, serverType), 주문 경로 계좌별 API 키·토큰, allow-real-execution 가드, 문서·체크리스트 보강. |
| 1.25 | 2026-02-01 | 완료: 4단계 파이프라인 확장(데이터 수집 후 실제 구현) — 미국 듀얼 모멘텀(TB_DAILY_STOCK), Half-Kelly p·b 백테스트 연동(전략별 설정), Sector RS·Post-Earnings Drift(TB_SECTOR_RETURN·TB_SYMBOL_SECTOR·TB_EARNINGS_SURPRISE V13/V14), 수급 강도·퀄리티-성장(TB_ORDER_FLOW·TB_FUNDAMENTALS V15/V16). 진행예정 항목 체크. |
| 1.26 | 2026-02-02 | 완료: 로보 어드바이저 백테스트·자동투자 연동 — RoboAllocationEngine·RoboBacktestService·POST/GET backtest/robo API·/backtest 모드·설정 로보 ON/OFF·RoboRebalanceScheduler·실행 전 백테스트·TB_TRADING_SETTINGS ROBO_ADVISOR_ENABLED(V17). |
| 1.27 | 2026-02-02 | 완료: 월스트리트 정렬(한국·미국) — **한국(KR)**: 청산 -5% 고정·전저점 이탈·RSI≥70 익절(ExitRuleEvaluator·ExitRuleService·StrategyPosition PRIOR_LOW V18); 진입 5일 연속 수급 메타( TB_ORDER_FLOW NET_BUY_AMT_1D V19)·역발상 RSI(CONTRARIAN_RSI)·P/B 필터 스텁(UniverseFilterService). **미국(US)**: 듀얼 모멘텀(노트) 모드 — 절대 SPY 12M vs T-bill·상대 섹터 ETF 6M 상위 2개(RoboAllocationEngine.computeTargetWeightsDualMomentumNote·RoboBacktestService·RoboRebalanceExecutor 모드 분기). application.yml pipeline.short-term-kr-stop-loss-pct·prior-low-stop-kr-enabled·rsi-exit-threshold·factor.contrarian-rsi-threshold·pb-value-min/max·backtest.robo.dual-momentum-mode·sector-etf-symbols 등. 00-strategy-registry·12-auto-investment-strategy v1.7 반영. |
| 1.28 | 2026-02-03 | 완료: 자동투자 현황 3단계 자금 배분 요약 표시 — PipelineSummaryDto.allocationSummary, PipelineSummaryService 거래 설정 기반 단기·중기·장기 예상 배분 계산·포맷, auto-invest 3단계 카드 실데이터 표시. KRX Open API 필요 목록 문서 추가(08-setup-guides/04-krx-api-required.md). |
| 1.29 | 2026-02-04 | 완료: 전체 화면 기획 + Figma 와이어프레임 문서 패키지 — 03-figma-wireframes(IA·권한·플로우·컴포넌트·가드레일·화면 스펙·Figma 규칙·AI 프롬프트), 01-screen-menu-spec §6 Ops 확장·역할 권한 개요. |