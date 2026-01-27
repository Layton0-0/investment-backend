# 시스템 아키텍처

## 1. 전체 아키텍처 개요

```
┌─────────────────────────────────────────────────────────────┐
│                      클라이언트 레이어                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  웹 브라우저  │  │  REST API    │  │  모바일 앱   │      │
│  │  (Thymeleaf) │  │   클라이언트  │  │   (향후)     │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                   애플리케이션 레이어                         │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Spring Boot Application                  │  │
│  │  ┌──────────────┐  ┌──────────────┐                │  │
│  │  │ Web Layer    │  │  API Layer   │                │  │
│  │  │ (Thymeleaf)  │  │  (REST)      │                │  │
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
│  MariaDB     │  │  한국투자증권 Open API        │
│  (데이터베이스)│  │  (REST API)                   │
│              │  │  - 시장 데이터 조회             │
│              │  │  - 기술적 지표 계산            │
└──────────────┘  └──────────────────────────────┘
```

## 2. 레이어 아키텍처

### 2.1 계층 구조

```
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│  - Web Controllers (Thymeleaf)         │
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
├── web/controller/          # Thymeleaf 웹 컨트롤러
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

## 3. 주요 컴포넌트

### 3.1 API 레이어
- **AccountController**: 계좌 조회 API
- **OrderController**: 주문 관리 API
- **AnalysisController**: 종목 분석 API
- **StrategyApiController**: 전략 관리 API
- **SettingController**: 거래 설정 API
- **TradingPortfolioController**: 트레이딩 포트폴리오 API
- **BatchManagementController**: 배치 작업 관리 API

### 3.2 서비스 레이어
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

### 3.3 도메인 레이어
- **Entity**: Order, Strategy, TradingPortfolio, TradingPortfolioItem, TradingSetting, Portfolio
- **Repository**: 각 엔티티별 Repository 인터페이스
- **Domain Model**: StrategyType, StrategyStatus

### 3.4 인프라 레이어
- **MarketDataClient**: 시장 데이터 조회 인터페이스
  - **KoreaInvestmentMarketDataClient**: 한국투자증권 API 구현체
- **Database**: JPA/Hibernate를 통한 MariaDB 접근

## 4. 데이터 흐름

### 4.1 주문 실행 흐름

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

### 4.2 종목 분석 흐름

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

### 4.3 전략 실행 흐름

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

### 4.4 트레이딩 포트폴리오 생성 흐름

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

## 5. 외부 시스템 연동

### 5.1 한국투자증권 Open API
- **연동 방식**: REST API
- **인증 방식**: OAuth 2.0 (App Key, App Secret)
- **구현 상태**: 완전 구현
  - OAuth 2.0 인증 및 자동 토큰 갱신
  - 차트 데이터 조회
  - 기술적 지표 계산 (RSI, MACD, EMA, Bollinger Bands, ATR, VWAP)
- **서버 타입**: 모의투자/실거래 선택 가능

### 5.3 데이터베이스
- **MariaDB**: 관계형 데이터베이스
- **JPA/Hibernate**: ORM 프레임워크

## 6. 스케줄러

### 6.1 StrategyScheduler
- **기능**: 활성화된 전략을 주기적으로 실행
- **실행 주기**: 설정 가능 (기본 1시간)

### 6.2 TradingPortfolioScheduler
- **기능**: 매일 일별 트레이딩 포트폴리오 생성
- **실행 시간**: 매일 오전 9시 (한국 시간)

## 7. 예외 처리

### 7.1 예외 계층
- **DomainException**: 비즈니스 규칙 위반
- **AppException**: 애플리케이션 레벨 오류
- **GlobalExceptionHandler**: 전역 예외 처리

### 7.2 에러 응답 형식
```json
{
  "code": "ERROR_CODE",
  "message": "오류 메시지",
  "details": ["상세 오류 목록"],
  "traceId": "UUID",
  "timestamp": "2026-01-26T10:00:00"
}
```

## 8. 설정 관리

### 8.1 프로파일
- **local**: 로컬 개발 환경
- **dev**: 개발 환경
- **prod**: 프로덕션 환경

### 8.2 주요 설정
- **데이터베이스**: 연결 정보, 커넥션 풀 설정
- **시장 데이터**: 제공자 선택, API 키, 타임아웃
- **거래 설정**: 최대/최소 투자금액, 기본 통화
- **로깅**: 로그 레벨, 로그 파일 경로
