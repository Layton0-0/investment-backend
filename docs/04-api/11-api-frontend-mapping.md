# API–프론트엔드 매핑

**목적**: 백엔드 REST API와 프론트엔드 화면·API 모듈의 대응 관계를 단일 소스로 정리한다.

**갱신**: API 추가·경로 변경·화면 연동 변경 시 이 문서를 수정한다.

---

## 1. 백엔드 API 목록 (컨트롤러 기준)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| **인증 (AuthController)** | | |
| POST | `/api/v1/auth/signup` | 회원가입 |
| POST | `/api/v1/auth/login` | 로그인 |
| POST | `/api/v1/auth/verify-account` | 계좌인증 |
| GET | `/api/v1/auth/mypage` | 마이페이지 조회 |
| PUT | `/api/v1/auth/mypage` | 마이페이지 수정 |
| POST | `/api/v1/auth/logout` | 로그아웃 |
| **계좌 (AccountController)** | | |
| GET | `/api/v1/accounts/{accountNo}/balance` | 계좌 잔고 조회 |
| GET | `/api/v1/accounts/{accountNo}/positions` | 보유 종목 조회 |
| GET | `/api/v1/accounts/{accountNo}/buyable-amount` | 매수가능조회 |
| GET | `/api/v1/accounts/{accountNo}/sellable-quantity` | 매도가능수량조회 |
| GET | `/api/v1/accounts/{accountNo}/order-history` | 주문체결조회 |
| GET | `/api/v1/accounts/{accountNo}/assets` | 투자계좌자산현황조회 |
| GET | `/api/v1/accounts/{accountNo}/profit-loss` | 기간별손익조회 |
| **주문 (OrderController)** | | |
| POST | `/api/v1/orders` | 주문 실행 |
| GET | `/api/v1/orders` | 주문 목록 조회 |
| GET | `/api/v1/orders/{orderId}` | 주문 조회 |
| DELETE | `/api/v1/orders/{orderId}` | 주문 취소 |
| **전략 (StrategyApiController)** | | |
| GET | `/api/v1/strategies/{accountNo}` | 전략 목록 조회 |
| GET | `/api/v1/strategies/{accountNo}/{strategyType}` | 전략 상세 조회 |
| POST | `/api/v1/strategies` | 전략 생성/업데이트 |
| PUT | `/api/v1/strategies/{accountNo}/{strategyType}/status` | 전략 상태 변경 |
| POST | `/api/v1/strategies/{accountNo}/{strategyType}/activate` | 전략 활성화 |
| POST | `/api/v1/strategies/{accountNo}/{strategyType}/stop` | 전략 중지 |
| **설정 (SettingController)** | | |
| GET | `/api/v1/settings/accounts` | 계좌 설정 조회 (모의·실 블록) |
| PUT | `/api/v1/settings/accounts` | 계좌 설정 저장 |
| GET | `/api/v1/settings/{accountNo}` | 거래 설정 조회 |
| PUT | `/api/v1/settings/{accountNo}` | 거래 설정 저장/업데이트 |
| **사용자 계좌 (UserAccountController)** | | |
| GET | `/api/v1/user/accounts` | 계좌 목록 |
| GET | `/api/v1/user/accounts/main` | 메인 계좌 조회 |
| GET | `/api/v1/user/accounts/{accountId}` | 계좌 상세 |
| PUT | `/api/v1/user/accounts/{accountId}/main` | 메인 계좌 설정 |
| **대시보드 (DashboardController)** | | |
| GET | `/api/v1/dashboard/performance-summary` | 성과 요약 (총 평가액·MDD·Sharpe·VaR 등) |
| **파이프라인 (PipelineController)** | | |
| GET | `/api/v1/pipeline/summary` | 파이프라인 요약 조회 |
| **트리거 (TriggerController)** | | |
| POST | `/api/v1/trigger/dart-collect` | DART 공시 수집 즉시 실행 |
| POST | `/api/v1/trigger/sec-collect` | SEC EDGAR 공시 수집 즉시 실행 |
| POST | `/api/v1/trigger/krx-daily` | KRX 일별 시세 수집 |
| POST | `/api/v1/trigger/us-daily` | US 일별 시세 수집 |
| POST | `/api/v1/trigger/factor-calculation` | 팩터 계산 즉시 실행 |
| POST | `/api/v1/trigger/auto-buy` | 자동매수(통합) 실행 |
| POST | `/api/v1/trigger/pipeline-execution` | 파이프라인 실행 |
| POST | `/api/v1/trigger/pipeline-exit` | 파이프라인 청산 |
| POST | `/api/v1/trigger/fill-confirmation` | 체결 확인 |
| POST | `/api/v1/trigger/unfilled-check` | 미체결 확인 |
| POST | `/api/v1/trigger/robo-rebalance` | 로보 리밸런싱 |
| POST | `/api/v1/trigger/daily-pnl` | 일일 PnL 기록 |
| POST | `/api/v1/trigger/intraday-breakout` | 장중 변동성 돌파 |
| POST | `/api/v1/trigger/medium-term-rebalance` | 중기 리밸런싱 |
| **뉴스 (NewsController)** | | |
| GET | `/api/v1/news` | 뉴스·공시 목록 조회 |
| POST | `/api/v1/news/collect` | 뉴스·공시 수집 실행 |
| **시그널 (SignalController)** | | |
| GET | `/api/v1/signals` | 시그널/팩터 점수 목록 조회 |
| **트레이딩 포트폴리오 (TradingPortfolioController)** | | |
| GET | `/api/v1/trading-portfolios/today` | 오늘 포트폴리오 조회 |
| GET | `/api/v1/trading-portfolios/date/{date}` | 특정일 포트폴리오 조회 |
| GET | `/api/v1/trading-portfolios/latest` | 최신 포트폴리오 목록 |
| POST | `/api/v1/trading-portfolios/generate` | 포트폴리오 수동 생성 |
| **백테스트 (BacktestController)** | | |
| POST | `/api/v1/backtest` | 백테스트 실행 |
| POST | `/api/v1/backtest/robo` | 로보 어드바이저 백테스트 |
| GET | `/api/v1/backtest/robo/last-pre-execution` | 실행 전 백테스트 최근 결과 |
| POST | `/api/v1/backtest/robo/collect-us-daily` | US 일봉 수집 (로보용) |
| **분석 (AnalysisController)** | | |
| POST | `/api/v1/analysis` | 종목 분석 |
| **시장 데이터 (MarketDataController)** | | |
| GET | `/api/v1/market-data/current-price/{symbol}` | 단일 종목 현재가 |
| POST | `/api/v1/market-data/current-prices` | 다중 종목 현재가 일괄 조회 |
| **배치 관리 (BatchManagementController)** | | |
| GET | `/batch` | 스케줄 현황 페이지 (SSR, Thymeleaf) |
| GET | `/batch/api/jobs` | 배치 작업 목록 JSON (SPA 연동용) |
| **Ops 데이터 파이프라인 (OpsDataPipelineController)** | | |
| GET | `/api/v1/ops/data-pipeline/status` | 데이터 파이프라인 원천별 수집 상태 (ADMIN 전용) |
| **Ops 알림센터 (OpsAlertsController)** | | |
| GET | `/api/v1/ops/alerts` | 알림 이력 조회 (ADMIN 전용, 페이징·레벨 필터) |
| **Ops 감사 로그 (OpsAuditController)** | | |
| GET | `/api/v1/ops/audit` | 감사 로그 조회 (ADMIN 전용, 페이징·eventType·기간 필터) |
| **Ops 모델/예측 (OpsModelController)** | | |
| GET | `/api/v1/ops/model/status` | 모델/예측 상태 조회 (ADMIN 전용) |
| **Ops 시스템 헬스 (OpsHealthController)** | | |
| GET | `/api/v1/ops/health` | 시스템 헬스 요약 (ADMIN 전용) |
| **Ops 전략 거버넌스 (OpsGovernanceController)** | | |
| GET | `/api/v1/ops/governance/results` | 전략 거버넌스 검사 결과 이력 (ADMIN 전용, limit) |
| GET | `/api/v1/ops/governance/halts` | 전략 거버넌스 활성 halt 목록 (ADMIN 전용) |
| PUT | `/api/v1/ops/governance/halts/{market}/{strategyType}/clear` | halt 해제 (ADMIN 전용, body 선택 clearedBy) |
| **연말 세금·리포트 (TaxReportController)** | | |
| GET | `/api/v1/report/tax/summary` | 연말 세금 요약 (year 쿼리, 실데이터 집계) |
| GET | `/api/v1/report/tax/summary/export` | 연말 세금 요약 내보내기 (year, format=csv\|pdf) |

**참고**: API 개요 문서(01-api-overview.md)의 "3.7 배치 관리 API"에는 `GET /api/v1/batch/jobs`로 기술되어 있으나, **현재 백엔드 구현은 `GET /batch/api/jobs`** 이다. SPA 프론트는 동일 base URL로 `/batch/api/jobs`를 호출한다. 향후 `/api/v1/batch/jobs`로 통일할지 별도 결정.

---

## 2. 프론트엔드 매핑 테이블

| 백엔드 API | 프론트 모듈·함수 | 사용 위치 | 비고 |
|------------|------------------|-----------|------|
| POST /api/v1/auth/signup | authApi.signup | RegisterPage | |
| POST /api/v1/auth/login | authApi.login | LoginPage, AuthContext | 응답에 role 포함, 로그인 후 역할은 서버 반환값 사용 |
| POST /api/v1/auth/verify-account | authApi.verifyAccount | RegisterPage | |
| GET /api/v1/auth/mypage | authApi.getMyPage | MyPage | |
| PUT /api/v1/auth/mypage | authApi.updateMyPage | MyPage | |
| POST /api/v1/auth/logout | authApi.logout | AuthContext | |
| GET /api/v1/accounts/{accountNo}/assets | accountApi.getAccountAssets | useDashboardData, Dashboard | |
| GET /api/v1/accounts/{accountNo}/positions | accountApi.getPositions | useDashboardData, Dashboard | |
| GET balance, buyable-amount, sellable-quantity, order-history, profit-loss | accountApi.getBalance, getBuyableAmount, getSellableQuantity, getOrderHistory, getProfitLoss | (필요 시 상세 화면) | 연동 완료 |
| GET /api/v1/orders | ordersApi.getOrders | useDashboardData, Market(Orders), Dashboard. 응답 각 항목에 signalType·exitRuleType(거래 사유) 포함, 주문 목록·대시보드 주문 테이블에 시그널 유형·청산 규칙 컬럼 표시 | |
| DELETE /api/v1/orders/{orderId} | ordersApi.cancelOrder | Market(Orders) | |
| POST /api/v1/orders | ordersApi.placeOrder | Market(Orders) | |
| GET /api/v1/orders/{orderId} | ordersApi.getOrder | 주문 상세·재시도 시 (404 시 null). 응답에 signalType·exitRuleType(거래 사유) 포함 | 연동 완료 |
| GET /api/v1/strategies/{accountNo} | strategyApi.getStrategies | Investment(Strategy) | |
| GET /api/v1/strategies/{accountNo}/{strategyType} | strategyApi.getStrategy | Investment(Strategy) 상세 모달 | 연동 완료 |
| POST /api/v1/strategies | strategyApi.createOrUpdateStrategy | Investment(Strategy) 전략 추가/편집 모달 | 연동 완료 |
| PUT /api/v1/strategies/{accountNo}/{strategyType}/status | strategyApi.updateStrategyStatus | Investment(Strategy) 상세 모달 상태 변경 | 연동 완료 |
| POST activate | strategyApi.activateStrategy | Investment(Strategy) | |
| POST stop | strategyApi.stopStrategy | Investment(Strategy) | |
| GET /api/v1/settings/accounts | settingsApi.getSettingsAccounts | useSettingsAccounts, Settings | |
| PUT /api/v1/settings/accounts | settingsApi.updateSettingsAccounts | Settings | |
| GET /api/v1/settings/{accountNo} | settingsApi.getSettingByAccountNo | useDashboardData, Dashboard | |
| PUT /api/v1/settings/{accountNo} | settingsApi.updateSetting | Settings(자동투자 설정 탭) | |
| GET /api/v1/user/accounts/main | userAccountsApi.getMainAccount | useDashboardData, Market, Investment | |
| GET /api/v1/user/accounts | userAccountsApi.getAccounts | System(Settings) 등록된 계좌 목록 | 연동 완료 |
| GET /api/v1/user/accounts/{accountId} | userAccountsApi.getAccount | (필요 시 상세 조회) | 연동 완료 |
| PUT /api/v1/user/accounts/{accountId}/main | userAccountsApi.setMainAccount | System(Settings) 메인으로 설정 버튼 | 연동 완료 |
| GET /api/v1/dashboard/performance-summary | dashboardApi.getPerformanceSummary | useDashboardData, Dashboard 성과 요약 카드 | 연동 완료 |
| GET /api/v1/pipeline/summary | pipelineApi.getPipelineSummary | useDashboardData, Investment(AutoInvest). 응답 openPositionList에 signalType·exitRuleType 포함, 자동투자 현황 보유 포지션 테이블에 시그널 유형·청산 규칙 컬럼 표시 | |
| POST /api/v1/trigger/* | triggerApi.trigger(path) | Admin(Batch) | path: dart-collect, sec-collect, factor-calculation, auto-buy, pipeline-execution 등. 버튼 라벨 "지금 실행" |
| GET /api/v1/news | newsApi.getNews | Market(News) | |
| POST /api/v1/news/collect | newsApi.collectNews | Market(News) | |
| GET /api/v1/signals | signalsApi.getSignals | Investment(AutoInvest) | |
| GET /api/v1/trading-portfolios/today | tradingPortfolioApi.getTodayPortfolio | Market(Portfolio) | |
| GET date/{date}, latest, POST generate | tradingPortfolioApi.getPortfolioByDate, getLatestPortfolios, generatePortfolio | Market(Portfolio) | |
| POST /api/v1/backtest | backtestApi.runBacktest | System(Backtest) | |
| POST robo, GET last-pre-execution, POST collect-us-daily | backtestApi.runRoboBacktest, getLastPreExecution, collectUsDaily | System(Backtest) | |
| POST /api/v1/analysis | analysisApi.analyze | Market(Portfolio) 종목 분석 모달 | 연동 완료 |
| GET /api/v1/analysis/sector | analysisApi.getSectorAnalysis | Market(Portfolio) 섹터 분석 카드 | 연동 완료 |
| GET /api/v1/analysis/correlation | analysisApi.getCorrelationAnalysis | Market(Portfolio) 상관관계 카드 | 연동 완료 |
| GET /api/v1/risk/portfolio-metrics | riskApi.getPortfolioRiskMetrics | Market(Portfolio) 리스크 메트릭 카드 | 연동 완료 |
| GET /api/v1/trading-portfolios/rebalance-suggestions | tradingPortfolioApi.getRebalanceSuggestions | Market(Portfolio) 리밸런싱 제안 카드 | 연동 완료 |
| GET /api/v1/market-data/current-price/{symbol} | marketDataApi.getCurrentPrice | Market(Portfolio) 종목 분석 모달 현재가 | 연동 완료 |
| POST /api/v1/market-data/current-prices | marketDataApi.getCurrentPrices | 다중 종목 현재가 위젯(선택) | 연동 완료 |
| GET /batch/api/jobs | batchApi.getBatchJobs | Admin(Batch) | **SPA 연동** (문서상 /api/v1/batch/jobs 아님) |
| GET /api/v1/ops/data-pipeline/status | opsApi.getDataPipelineStatus | Admin(Ops 데이터 파이프라인 /ops/data) | 연동 완료 |
| GET /api/v1/ops/alerts | opsApi.getAlerts | Admin(Ops 알림센터 /ops/alerts) | 연동 완료 |
| GET /api/v1/ops/audit | opsApi.getAuditLogs | Admin(Ops 감사 로그 /ops/audit) | 연동 완료 |
| GET /api/v1/ops/model/status | opsApi.getModelStatus | Admin(Ops 모델/예측 /ops/model) | 연동 완료 |
| GET /api/v1/ops/health | opsApi.getHealth | Admin(Ops 시스템 헬스 /ops/health) | 연동 완료 |
| GET /api/v1/ops/governance/results | opsApi.getGovernanceResults | Admin(Ops 전략 거버넌스 /ops/governance) | 백엔드 준비 완료, 프론트 화면 선택 |
| GET /api/v1/ops/governance/halts | opsApi.getGovernanceHalts | Admin(Ops 전략 거버넌스 /ops/governance) | 동일 |
| PUT /api/v1/ops/governance/halts/{market}/{strategyType}/clear | opsApi.clearGovernanceHalt | Admin(Ops 전략 거버넌스 /ops/governance) | 동일 |
| GET /api/v1/report/tax/summary | reportApi.getTaxSummary | TaxReportPage | 연말 세금·리포트 화면 |
| GET /api/v1/report/tax/summary/export | reportApi.downloadTaxSummaryExport (window.open) | TaxReportPage | CSV/PDF 다운로드 |

---

## 3. 화면(라우트)별 사용 API 요약

| 라우트 | 컴포넌트/페이지 | 사용 API |
|--------|-----------------|----------|
| `/` | Dashboard | userAccountsApi.getMainAccount, accountApi.getAccountAssets, accountApi.getPositions, ordersApi.getOrders, pipelineApi.getPipelineSummary, settingsApi.getSettingByAccountNo, dashboardApi.getPerformanceSummary |
| `/login` | LoginPage | authApi.login |
| `/signup` | RegisterPage | authApi.signup, authApi.verifyAccount |
| `/mypage` | MyPage | authApi.getMyPage, authApi.updateMyPage |
| `/auto-invest` | AutoInvest | userAccountsApi.getMainAccount, pipelineApi.getPipelineSummary, signalsApi.getSignals, strategyApi.getStrategies (등) |
| `/strategies/kr`, `/strategies/us` | Strategy | userAccountsApi.getMainAccount, strategyApi.getStrategies, strategyApi.activateStrategy, strategyApi.stopStrategy |
| `/news` | News | newsApi.getNews, newsApi.collectNews |
| `/portfolio` | Portfolio | tradingPortfolioApi.getTodayPortfolio, getPortfolioByDate, getLatestPortfolios, generatePortfolio, getRebalanceSuggestions, analysisApi.analyze, getSectorAnalysis, marketDataApi.getCurrentPrice, riskApi.getPortfolioRiskMetrics (종목 분석·섹터·리스크·리밸런싱 제안 카드) |
| `/orders` | Orders | userAccountsApi.getMainAccount, ordersApi.getOrders, ordersApi.placeOrder, ordersApi.cancelOrder |
| `/batch` | Batch | triggerApi.trigger, batchApi.getBatchJobs |
| `/backtest` | Backtest | backtestApi.runBacktest, runRoboBacktest, getLastPreExecution, collectUsDaily |
| `/settings` | Settings | useSettingsAccountsAll(getSettingsAccounts, updateSettingsAccounts), settingsApi.getSettingByAccountNo, updateSetting, userAccountsApi.getAccounts, setMainAccount (등록된 계좌·메인 설정) |
| `/report/tax` | TaxReportPage | reportApi.getTaxSummary |
| `/risk`, `/ops/*` | OpsPage (OpsDashboard) | riskApi.getRiskSummary, getRiskLimits, getRiskHistory (/risk); Batch에서만 트리거; /ops/data에서 opsApi.getDataPipelineStatus; /ops/governance(선택)에서 opsApi.getGovernanceResults, getGovernanceHalts, clearGovernanceHalt 연동 |

---

## 4. 메뉴(라우트)별 백엔드 API 필요·연동 현황

퍼블/AppShell 메뉴 전부 필요한 기능으로 정리. 각 메뉴에 대해 필요한 백엔드 API·이미 연동된 API·미연동/미구현을 표로 정리한다.

| 메뉴(라우트) | 필요한 백엔드 API | 이미 연동 | 미연동/미구현 |
|-------------|-------------------|----------|---------------|
| 대시보드 `/dashboard` | 메인 계좌(모의·실), 자산·포지션·주문·파이프라인 요약·거래 설정·성과 요약 | getMainAccount, getAccountAssets, getPositions, getOrders, getPipelineSummary, getSettingByAccountNo, getPerformanceSummary | - |
| 자동투자 현황 `/auto-invest` | 메인 계좌, 파이프라인 요약, 시그널, 전략 목록 | 동일 | - |
| 국내/미국 전략 `/strategies/kr`, `/strategies/us` | 메인 계좌, 전략 목록·상세·활성화·중지·생성/편집·status | getStrategies, getStrategy, createOrUpdateStrategy, updateStrategyStatus, activateStrategy, stopStrategy | - |
| 뉴스·이벤트 `/news` | 뉴스 목록, 수집 실행 | getNews, collectNews | - |
| 포트폴리오 `/portfolio` | 오늘·날짜별·최신·수동 생성·섹터·리스크·리밸런싱·상관관계 | getTodayPortfolio, getPortfolioByDate, getLatestPortfolios, generatePortfolio, getSectorAnalysis, getPortfolioRiskMetrics, getRebalanceSuggestions, getCorrelationAnalysis | - |
| 주문·체결 `/orders` | 메인 계좌, 주문 목록·취소·실행 | getOrders, cancelOrder, placeOrder | - |
| 스케줄 현황 `/batch` | 배치 작업 목록, 트리거 | getBatchJobs, triggerApi | - |
| 백테스트 `/backtest` | 백테스트 실행, 로보 3종 | runBacktest, runRoboBacktest, getLastPreExecution, collectUsDaily | - |
| 설정 `/settings` | 계좌 설정 조회·저장, 거래 설정 조회·저장, 등록된 계좌 목록·메인 설정 | getSettingsAccounts, updateSettingsAccounts, getSettingByAccountNo, updateSetting, getAccounts, setMainAccount | - |
| 마이페이지 `/mypage` | 마이페이지 조회·수정 | getMyPage, updateMyPage | - |
| **Admin 전용** | | | |
| 데이터 파이프라인 `/ops/data` | 파이프라인 원천별 수집 상태·최근 기준일·오류 요약 | getDataPipelineStatus (opsApi) | - |
| 알림센터 `/ops/alerts` | 알림 목록·설정 | getAlerts (opsApi) | - |
| 감사 로그 `/ops/audit` | 설정 변경·수동 트리거·실계좌 가드 차단 이벤트 | getAuditLogs (opsApi) | - |
| 리스크 리포트 `/risk` | 리스크 지표·한도·이력 | getRiskSummary, getRiskLimits, getRiskHistory | - |
| 연말 세금·리포트 `/report/tax` | 연도별 세금 요약(실현손익·배당·면책)·CSV/PDF 내보내기 | getTaxSummary, downloadTaxSummaryExport (reportApi) | 연동 완료 |
| 모델/예측 `/ops/model` | 예측 모델 상태·결과 | getModelStatus (opsApi) | 연동 완료 |
| 시스템 헬스 `/ops/health` | 서비스·DB·캐시 헬스 | getHealth (opsApi) | 연동 완료 |
| 전략 거버넌스 `/ops/governance` | 검사 결과·활성 halt·halt 해제 | getGovernanceResults, getGovernanceHalts, clearGovernanceHalt (opsApi) | - |

**순차 개발**: §4.2 우선순위(P0~P3)와 위 표의 미연동 항목을 메뉴 단위로 묶어, 02-development-status.md "진행예정"에 순차 개발 계획으로 반영한다.

---

## 5. 불일치·누락 정리

### 5.1 배치 API 경로

- **문서(01-api-overview.md)**: `GET /api/v1/batch/jobs`
- **실제 구현**: `GET /batch/api/jobs` (BatchManagementController)
- **프론트**: 동일 base URL 기준으로 `/batch/api/jobs` 호출. 상세는 [01-api-overview.md](./01-api-overview.md) §3.7 및 이 문서 §1 참조.

### 5.2 미구현·미연동 (우선순위별 보완)

| 우선순위 | API | 보완 내용 | 상태 |
|----------|-----|-----------|------|
| **P0** | PUT `/api/v1/settings/{accountNo}` | Settings 화면에서 거래 설정 저장 시 호출 (자동투자 ON/OFF·비율·최대투자금 등) | 연동 완료 |
| **P0** | 로보 백테스트 3종 | Backtest 페이지: runRoboBacktest, getLastPreExecution, collectUsDaily | 연동 완료 |
| **P1** | PUT `/api/v1/auth/mypage` | MyPage 프로필(비밀번호) 수정 폼 제출 시 호출 | 연동 완료 |
| **P1** | POST `/api/v1/orders` | Orders 화면 "수동 주문" 폼 제출 시 호출 | 연동 완료 |
| **P2** | POST `/api/v1/news/collect` | 뉴스 화면 "수집 실행" 버튼 | 연동 완료 |
| **P2** | 트레이딩 포트폴리오 date/latest/generate | 포트폴리오 화면 날짜별 조회·최신 목록·수동 생성 버튼 | 연동 완료 |
| **P3** | 분석·시장 데이터 API | 전용 화면 또는 위젯 추가 시 연동 | 연동 완료 (종목 분석 모달·현재가) |

위 P0~P3 항목은 모두 연동 완료되었으며, §2 프론트엔드 매핑 테이블에 해당 행이 반영되어 있다. **Phase 1 순차 검토(기획 정합)**: §4·§5.2 기준 검토 완료, 현재 미연동·미구현 항목 없음.

---

## 문서 변경 이력

| 버전 | 일자 | 변경 내용 |
|------|------|----------|
| 1.0 | 2026-02-05 | 최초 작성: API 목록, 프론트 매핑 테이블, 라우트별 요약, 불일치·누락 정리 |
| 1.1 | 2026-02-05 | §4 메뉴(라우트)별 백엔드 API 필요·연동 현황 추가, Admin 전용 메뉴별 정리, §4→§5 번호 조정 |
| 1.2 | 2026-02-10 | Phase 1 연동: 전략 상세·생성·PUT status (strategyApi), 분석·시장데이터 (analysisApi·marketDataApi) 종목 분석 모달 반영. §2·§4·§5.2 갱신. |
| 1.3 | 2026-02-11 | Phase 1 검증: §2 매핑 테이블에 PUT mypage, POST orders, PUT settings/{accountNo}, POST news/collect, 트레이딩 포트폴리오 date/latest/generate, 로보 백테스트 3종 연동 정보 반영. §3·§4 메뉴별/라우트별 연동 현황 정리. |
| 1.4 | 2026-02-12 | Phase 1 Ops 전략 거버넌스 프론트 연동: §4 전략 거버넌스 행 미연동 → 연동 완료(getGovernanceResults, getGovernanceHalts, clearGovernanceHalt). |
| 1.5 | 2026-02-12 | Phase 1 순차 검토(기획 정합): §4·§5.2 기준 검토 완료, 미연동·미구현 항목 없음 문구 추가. |