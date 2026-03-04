# 시스템 아키텍처

## 1. 전체 아키텍처 개요

```
┌─────────────────────────────────────────────────────────────┐
│                      클라이언트 레이어                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  웹 브라우저  │  │  REST API    │  │  모바일 앱   │      │
│  │  (React SPA) │  │   클라이언트  │  │   (향후)     │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                   애플리케이션 레이어                         │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Spring Boot Application                  │  │
│  │  ┌──────────────┐  ┌──────────────┐                │  │
│  │  │ API Layer (REST)  │                │  │
│  │  └──────────────┘  └──────────────┘                │  │
│  │  ┌──────────────────────────────────────────────┐  │  │
│  │  │           Service Layer                      │  │  │
│  │  │  Account │ Order │ Strategy │ Analysis      │  │  │
│  │  └──────────────────────────────────────────────┘  │  │
│  │  ┌──────────────────────────────────────────────┐  │  │
│  │  │           Domain Layer                       │  │  │
│  │  │  Entity │ Repository │ Domain Logic         │  │  │
│  │  └──────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
┌──────────────┐  ┌──────────────────────────────┐
│ TimescaleDB  │  │  한국투자증권 Open API        │
│ (PostgreSQL) │  │  (REST API)                   │
│  시계열·일반  │  │  - 시장 데이터 조회             │
│              │  │  - 기술적 지표 계산            │
└──────────────┘  └──────────────────────────────┘
```

## 2. 레이어 아키텍처

### 2.1 계층 구조

```
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│  - REST API Controllers                 │
└─────────────────────────────────────────┘
                    │
┌─────────────────────────────────────────┐
│         Application Layer               │
│  - Service Layer                        │
│  - DTO (Data Transfer Objects)          │
└─────────────────────────────────────────┘
                    │
┌─────────────────────────────────────────┐
│         Domain Layer                    │
│  - Entities                             │
│  - Repositories                         │
│  - Domain Services                      │
└─────────────────────────────────────────┘
                    │
┌─────────────────────────────────────────┐
│         Infrastructure Layer            │
│  - Database (JPA/Hibernate)            │
│  - External API Clients                 │
│  - Configuration                        │
└─────────────────────────────────────────┘
```

### 2.2 패키지 구조

```
com.investment
├── api/controller/          # REST API 컨트롤러
├── account/                 # 계좌 관련
│   ├── dto/
│   └── service/
├── order/                   # 주문 관련
│   ├── dto/
│   └── service/
├── analysis/                # AI 분석 관련
│   ├── dto/
│   └── service/
├── strategy/                # 거래 전략 관련
│   ├── domain/             # 도메인 모델 (StrategyType, StrategyStatus)
│   ├── dto/
│   ├── scheduler/           # 스케줄러
│   └── service/
├── setting/                 # 설정 관련
│   ├── dto/
│   └── service/
├── tradingportfolio/        # 트레이딩 포트폴리오
│   ├── controller/
│   ├── dto/
│   ├── scheduler/
│   └── service/
├── marketdata/              # 시장 데이터
│   ├── client/             # MarketDataClient 인터페이스 및 구현체
│   │   ├── impl/
│   │   └── IndicatorResponse.java
│   ├── config/
│   └── util/
├── taapi/                   # 기술적 분석 API
│   ├── dto/
│   └── service/
├── domain/                  # 도메인 엔티티 및 리포지토리
│   ├── entity/
│   └── repository/
├── common/                  # 공통 (예외 처리 등)
│   └── exception/
├── config/                  # 설정 클래스
└── batch/                   # 배치 작업
    ├── controller/
    ├── dto/
    └── service/
```

## 3. 기준 문서와의 대응

앞으로의 개발 기준인 [minimum-architecture-requirement.md](../01-requirements/minimum-architecture-requirement.md), [기획요청.md](../01-requirements/기획요청.md), [gemini-설계.md](../01-requirements/gemini-설계.md)에서 정의한 **Alpha–Risk–Execution·Portfolio·Compliance** 논리 구조와 현재 구현의 대응 관계는 [00-planning-basis.md](../01-requirements/00-planning-basis.md)에서 단일 소스로 관리한다. 요약만 아래 표로 둔다.

| 논리 블록 (기준 문서) | 구현 패키지/서비스 |
|----------------------|-------------------|
| **Gateway (KIS Adapter)** | `account`·`order`·`marketdata` — 한국투자증권 REST 클라이언트, 토큰·국내/해외 정규화 |
| **Data Engine** | `core.pipeline`, `datacollection`, `batch` — 데이터 수집·팩터·유니버스·시그널 (수정주가·Feature Store 강화는 개발예정) |
| **Brain (Alpha·Portfolio)** | `core.engine.alpha`, `core.engine.portfolio`, `strategy`, `factor` — 시그널·TaxAwareOptimizer·Rebalancer |
| **Risk Guard (Compliance)** | `core.engine.risk`, `risk.service` — PreTradeComplianceEngine, TradingHaltService, PortfolioPeakService |
| **Execution** | `order` — OrderService, executeOrderForPipeline, KR/US 스마트 라우팅 |

§1 다이어그램의 애플리케이션 레이어는 위 논리 블록에 따라 **Data Engine → Brain → Risk Guard → Execution** 순서로 파이프라인이 동작하며, Gateway는 한국투자증권 Open API와의 접점이다.

## 4. 주요 컴포넌트

### 4.1 API 레이어
- **AccountController**: 계좌 조회 API
- **OrderController**: 주문 관리 API
- **AnalysisController**: 종목 분석 API
- **StrategyApiController**: 전략 관리 API
- **SettingController**: 거래 설정 API
- **TradingPortfolioController**: 트레이딩 포트폴리오 API
- **BatchManagementController**: 배치 작업 관리 API

### 4.2 서비스 레이어
- **AccountService**: 계좌 정보 조회 서비스
- **OrderService**: 주문 실행 및 관리 서비스
- **AnalysisService**: 종목 분석 서비스
- **StrategyService**: 전략 실행 서비스
- **StrategyManagementService**: 전략 관리 서비스
- **TradingStrategyService**: 거래 전략 서비스
- **TradingSettingService**: 거래 설정 서비스
- **TradingPortfolioService**: 트레이딩 포트폴리오 서비스
- **ShortTermTradingStrategyService**: 단기 트레이딩 전략 서비스
- **StockAnalysisService**: 주식 분석 서비스
- **StockScreeningService**: 주식 스크리닝 서비스

### 4.3 도메인 레이어
- **Entity**: Order, Strategy, TradingPortfolio, TradingPortfolioItem, TradingSetting, Portfolio
- **Repository**: 각 엔티티별 Repository 인터페이스
- **Domain Model**: StrategyType, StrategyStatus

### 4.4 인프라 레이어
- **MarketDataClient**: 시장 데이터 조회 인터페이스
  - **KoreaInvestmentMarketDataClient**: 한국투자증권 API 구현체
- **Database**: JPA/Hibernate를 통한 TimescaleDB(PostgreSQL) 접근

## 5. 데이터 흐름

### 5.1 주문 실행 흐름

```
Client Request
    │
    ▼
OrderController
    │
    ▼
OrderService
    │
    ├──► TradingSettingRepository (설정 조회)
    ├──► Validation (금액 검증)
    ├──► OrderRepository (주문 저장)
    └──► External API (키움증권 API 호출)
    │
    ▼
Response
```

### 5.2 종목 분석 흐름

```
Client Request
    │
    ▼
AnalysisController
    │
    ▼
AnalysisService
    │
    ├──► MarketDataClient (시장 데이터 조회)
    │    └──► KoreaInvestmentMarketDataClient
    ├──► StockAnalysisService (기술적 분석)
    └──► AnalysisResponseDto 생성
    │
    ▼
Response
```

### 5.3 전략 실행 흐름

```
StrategyScheduler (스케줄러)
    │
    ▼
StrategyService
    │
    ├──► StrategyRepository (활성 전략 조회)
    ├──► AnalysisService (종목 분석)
    ├──► TradingStrategyService (매매 결정)
    ├──► OrderService (주문 실행)
    └──► StrategyRepository (실행 결과 업데이트)
```

### 5.4 트레이딩 포트폴리오 생성 흐름

```
TradingPortfolioScheduler (매일 오전 9시)
    │
    ▼
TradingPortfolioService
    │
    ├──► ShortTermTradingStrategyService
    │    ├──► StockScreeningService (종목 스크리닝)
    │    ├──► StockAnalysisService (종목 분석)
    │    └──► TradingPortfolioItem 생성
    ├──► TradingPortfolio 생성
    └──► TradingPortfolioRepository (저장)
```

## 6. 외부 시스템 연동

### 6.1 한국투자증권 Open API
- **연동 방식**: REST API
- **인증 방식**: OAuth 2.0 (App Key, App Secret)
- **구현 상태**: 완전 구현
  - OAuth 2.0 인증 및 자동 토큰 갱신
  - 차트 데이터 조회
  - 기술적 지표 계산 (RSI, MACD, EMA, Bollinger Bands, ATR, VWAP)
  - 실시간 현재가 조회
  - 주문 API (매수/매도)
- **서버 타입**: 모의투자/실거래 선택 가능
- **MCP 통합**: 한국투자 코딩도우미 MCP를 활용한 개발 환경 지원

### 6.2 데이터베이스
- **TimescaleDB**: PostgreSQL 기반 시계열·관계형 데이터베이스 (Docker: `timescale/timescaledb:latest-pg16`)
- **JPA/Hibernate**: ORM 프레임워크 (PostgreSQLDialect)
- **초기 스키마**: 신규 환경은 `--spring.profiles.active=local,init-db` 1회 실행 후 일반 프로파일로 전환

## 7. 스케줄러

### 7.1 StrategyScheduler
- **기능**: 활성화된 전략을 주기적으로 실행
- **실행 주기**: 설정 가능 (기본 1시간)

### 7.2 TradingPortfolioScheduler
- **기능**: 매일 일별 트레이딩 포트폴리오 생성
- **실행 시간**: 매일 오전 9시 (한국 시간)

## 8. 예외 처리

### 8.1 예외 계층
- **DomainException**: 비즈니스 규칙 위반
- **AppException**: 애플리케이션 레벨 오류
- **GlobalExceptionHandler**: 전역 예외 처리

### 8.2 에러 응답 형식
```json
{
  "code": "ERROR_CODE",
  "message": "오류 메시지",
  "details": ["상세 오류 목록"],
  "traceId": "UUID",
  "timestamp": "2026-01-26T10:00:00"
}
```

## 9. 설정 관리

### 9.1 프로파일
- **local**: 로컬 개발 환경
- **dev**: 개발 환경
- **prod**: 프로덕션 환경

### 9.2 주요 설정
- **데이터베이스**: 연결 정보, 커넥션 풀 설정
- **시장 데이터**: 제공자 선택, API 키, 타임아웃
- **거래 설정**: 최대/최소 투자금액, 기본 통화
- **로깅**: 로그 레벨, 로그 파일 경로

## 10. 2.0 개편안 (기관급 퀀트 엔진)

기존 구조를 유지하면서 **Alpha - Risk - Execution** 분리 원칙을 반영한 패키지·컴포넌트가 추가되었다.

### 10.1 논리 아키텍처 (Quant Engine)

- **DataPipeline**: 시장 데이터 수집·정제 진입점 (`core.pipeline.DataPipelineService`). 수정주가는 한투 API `FID_ORG_ADJ_PRC=0` 사용으로 명시.
- **Alpha**: 전략 시그널 생성 (`core.engine.alpha.AlphaEngine` → 기존 StrategyService 위임).
- **Portfolio**: 포트폴리오 최적화·리밸런싱 (`TaxAwareOptimizerImpl`, `RebalancerImpl`). Phase 2 구현 완료.
- **Risk**: 주문 직전 컴플라이언스 (`PreTradeComplianceEngine`). Kill Switch, 단일 종목 10% 상한, MDD 15% 게이트. `OrderService`에서 주문 직전 호출.
- **Execution**: 주문 집행 게이트웨이 (`core.engine.execution.ExecutionGateway` → OrderService 위임).

### 10.2 추가 패키지

- `core.engine.alpha`: AlphaEngine, AlphaEngineFacade
- `core.engine.portfolio`: TaxAwareOptimizer, TaxAwareOptimizerImpl, Rebalancer, RebalancerImpl, StubPortfolioComponents
- `core.engine.risk`: ComplianceEngine, PreTradeComplianceEngine(디폴트), ComplianceEngineStub(테스트 전용), ComplianceResult
- `core.engine.execution`: ExecutionGateway, OrderServiceExecutionGateway
- `core.pipeline`: DataPipelineService
- `risk.service`: TradingHaltService, PortfolioPeakService
- **Kill Switch API**: GET/PUT `/api/v1/system/kill-switch` (KillSwitchController). ADMIN만 설정 가능.

---

## 문서 변경 이력

| 버전 | 일자 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-28 | System | 문서 정리 및 구조화 |
| 2.0 | 2026-02-06 | System | TimescaleDB 전환, 2.0 개편안(Quant Engine·패키지) 반영 |
| 2.1 | 2026-02-06 | System | §3 기준 문서와의 대응 섹션 추가(논리 블록↔패키지 매핑), 섹션 번호 3~10 재정렬 |
