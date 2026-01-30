# 개발 진행 현황

**목적**: 개발 완료·진행중·진행예정 항목을 한 문서에서 관리하고, [로드맵](../roadmap.md)·[화면·메뉴 기획서](./01-screen-menu-spec.md)·[PRD](../PRD.md)와 연동하여 참조·변동 시 갱신한다.

**갱신 원칙**: 개발 완료·스프린트 시작·범위 변경 시 이 문서를 수정하고, 다른 문서(roadmap, README)에서 이 문서를 참조한다.

---

## 1. 완료 (Completed)

### 도메인·DB·API
- [x] **Strategy 시장(MARKET)**  
  Strategy 엔티티 `market` 필드, Repository 시장 조건 조회, Service/API `market` 파라미터(선택), DTO 반영. DB V3(ACCOUNT_NO,MARKET,STRATEGY_TYPE UK) 반영.
- [x] **뉴스·공시 도메인 및 API**  
  NewsItem 엔티티, NewsItemRepository, NewsItemService, GET `/api/v1/news` (필터·페이징). API 개요 문서 반영.
- [x] **캐시 키 정리**  
  CacheConfig에 CACHE_CURRENT_PRICE 정의, RealtimeMarketDataService에서 상수 사용.

### 화면·메뉴
- [x] **메뉴 설정 및 공통 레이아웃**  
  MenuConfig(appMenuItems), MenuModelAdvice, layout-header·layout-menu fragment, common.css 앱 헤더·네비 스타일.
- [x] **대시보드 보강**  
  공통 헤더·메뉴 적용, 빠른 액션(국내/미국 전략, 뉴스·이벤트, 포트폴리오, 주문·체결, 설정).
- [x] **자동투자 현황**  
  `/auto-invest`, AutoInvestController, auto-invest.html (4단계 파이프라인·시그널 스텁).
- [x] **국내/미국 전략 분리**  
  `/strategies/kr`, `/strategies/us`, WebStrategyController 시장별 조회·폼 market 전달.
- [x] **뉴스·이벤트**  
  `/news`, NewsWebController, news.html, GET /api/v1/news 연동.
- [x] **주문·체결**  
  `/orders`, OrdersWebController, orders.html, 주문 목록 연동.
- [x] **설정**  
  메뉴 "설정" → `/mypage` (기존 마이페이지).

### 인프라·운영
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
- [x] **대시보드 거래 설정 optional 처리**  
  TradingSettingService.getSettingOptional(accountNo) 추가(없으면 empty). DashboardController에서 계좌 데이터 조회 시 getSettingOptional 사용, 있으면 setting 모델 추가·없으면 미추가(거래 설정 카드 미표시). 거래 설정 미저장 상태에서도 대시보드(잔고·보유·주문) 정상 표시.

---

## 2. 진행중 (In progress)

- *(현재 진행중인 스프린트/태스크가 있으면 여기에 항목을 추가한다.)*

---

## 3. 진행예정 (Planned)

산출 기획([자동투자 전략 명세](../02-architecture/12-auto-investment-strategy.md), [뉴스·공시 수집·연동 설계](../02-architecture/13-news-collection-design.md), [화면·메뉴 기획서](./01-screen-menu-spec.md), [로드맵](../roadmap.md))을 토대로 구체화한 항목입니다.

### 단기 (로드맵 Phase 1~4 대응)

- [x] **테스트 코드 보강 (1차)**  
  단위·슬라이스 테스트 추가: PasswordValidator, GlobalExceptionHandler, NewsItemService, NewsController, StrategyManagementService, StrategyApiController, AuthController, AuthService. 기존 OrderController/OrderService/TradingSettingService/KoreaInvestmentMarketDataClient/StockCodeConverter 테스트 수정. getBulkIndicators 모의 데이터 분기 추가. WebMvcTest에 SecurityConfig 의존 MockBean 추가. 전체 테스트 실행: `$env:GRADLE_UNIQUE_BUILD_DIR='1'; .\gradlew test` 또는 `.\scripts\run-tests.ps1`. (AuthController getMyPage 슬라이스 테스트 1건은 addFilters=false 시 principal 미전달로 @Disabled.)
- [ ] **테스트 커버리지**  
  라인 80%·브랜치 70% 목표. 전략(market 파라미터)·뉴스 API 등 추가 커버리지.
- [ ] **성능 최적화**  
  쿼리·캐싱·비동기 적용. 시장 데이터·종목 분석·계좌 조회 응답 시간 목표(평균 500ms, 95%ile 1초) 달성.
- [ ] **데이터 수집 연동 (구축 로드맵 1단계)**  
  Yahoo Finance API(미국), KRX/OpenDart API(한국) 연동. 확정 원천만 파이프라인 입력으로 사용.
- [ ] **팩터 계산 엔진 (구축 로드맵 2단계)**  
  2단계 시그널 수식(듀얼 모멘텀·퀄리티-성장·수급 강도·이격도·변동성 돌파 등)을 Python(Pandas/Numpy) 또는 Java로 구현, **매일 장 시작 전** 종목별 점수 산출 스케줄러.
- [ ] **LSTM 예측 모델(초기)**  
  데이터 수집·전처리, LSTM 모델·학습 파이프라인, 서빙 API. AI는 분석 정보 제공용, 최종 매매 결정은 규칙 엔진 유지.

### 중기 (로드맵 Phase 5~6)

- [ ] **4단계 파이프라인 구현**  
  **1) 유니버스 필터링**: 공통 Liquidity Cut-off, 한국 Sector Relative Strength, 미국 Post-Earnings Drift. **2) 시그널 생성**: 한국 수급 강도(Smart Money Intensity)·이격도(Disparity)·변동성 돌파(k 동적); 미국 듀얼 모멘텀·퀄리티-성장(PEG & Rule of 40)·VAA 변형. **3) 자금 관리**: 켈리(Half-Kelly)·변동성 역가중·ATR 포지션 사이징(1회 1% 리스크). **4) 매매 실행·청산**: ATR Trailing Stop(2.0~2.5), Time-Cut(N일/목표 수익률 미도달 시 전량 매도).
- [ ] **시장·기간별 전략 로직**  
  단기(20%): 유동성·RSI(14)>60 & MACD>Signal·수급 필터, Trailing Stop -3%. 중기(40%): 듀얼 모멘텀 상위 10%, EPS YoY>20%, PEG<1.5, 20·60일선 정배열, 월 1회 리밸런싱, 개별 -10% 손절. 장기(40%): ROE>20%, OPM>25%, Rule of 40, MDD -15%~-20% 분할 매수, 펀더멘털 훼손 시에만 매도.
- [ ] **뉴스·공시 파이프라인 (확정 원천만)**  
  **한국**: DART(Open API) 실시간/단기 폴링 공시, 키워드(무상증자·영업익 30% 증가 등) 포착 시 매수 시그널; 연합뉴스 수집·NLP·속보/긴급 가중치; 네이버 금융(많이 본 뉴스·실시간 검색 종목) 수집·이용약관 준수. **미국**: SEC EDGAR API(8-K·10-K·10-Q), 8-K 발생 시 **최우선 순위** 로직; Reuters 헤드라인·감정 분석; Yahoo Finance(OHLCV·Earnings Calendar·Analyst Up/Down). 수집·저장(TB_NEWS_ITEMS)·감정/중요도/이벤트 유형 분석, 전략 시그널 점수 반영, Fallback(원천 장애 시 파이프라인 중단 없음).
- [ ] **고급 분석·포트폴리오**  
  섹터 분석, 상관관계·리스크 메트릭(VaR/CVaR, Sharpe/Sortino), 리밸런싱 자동화, 리스크 기반 포지션 사이징.
- [ ] **백테스팅 (필수)**  
  과거 10년 데이터로 알고리즘 시뮬레이션. **2020년 코로나 폭락장**, **2022년 금리 인상기** 방어율 검증.
- [ ] **대시보드·UX**  
  자동투자 현황 화면: 파이프라인 단계별 실데이터(유니버스 수·시그널 건수·체결 건수), 시그널/체결 목록 테이블. 대시보드: 계좌 요약(국내·미국 구분), 자동투자 상태 카드. 실시간 차트, 성과 분석, 반응형·모바일.

### 장기 (로드맵 Phase 7~8)

- [ ] **KIS Open API 실전 구축**  
  **WebSocket 우선**: 실시간 호가/체결가(Tick) 구독, 변동성 돌파 시그널 0.1초 단위 감시; 체결 Push 수신 시 익절/손절 대기 로직 즉시 활성화. **REST 퀀트 스코어링**: 국내 순위 분석 API(거래대금·등락률 상위)→주도주 유니버스 매일 아침 갱신; 투자자별 매매동향 API→10분 단위 수급 점수; 미국 해외주식 기간별 시세(환율 포함)·데이터 정합성. **리스크**: Throttling(실전 초당 20회, 주문 2~10회)→주문 요청 큐(메시지 큐 RabbitMQ 등) 순차 처리; Access Token **장 시작 30분 전** Crontab 자동 갱신. **시드·주문**: 국내 지정가·최유리 지정가, 예수금 30~50% 변동성 비중; 미국 실시간 시세(유료)·시장가, 통합증거금; 모의투자 2주 테스트 후 실전.
- [ ] **다중 계좌·실시간 스트리밍**  
  다중 계좌 관리, WebSocket 시세·알림, 통합 포트폴리오 뷰.
- [ ] **화면 확장 (기획서 향후 메뉴)**  
  백테스트(`/backtest`) 결과·파라미터 화면; 리스크 리포트(`/risk`) MDD·VaR·노출 등.
- [ ] **모바일 앱**  
  iOS/Android, 푸시 알림 (선택).

---

## 4. 참조 관계

| 문서 | 역할 |
|------|------|
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
