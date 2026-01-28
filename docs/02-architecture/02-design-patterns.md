# 설계 패턴 및 원칙

## 1. 아키텍처 패턴

### 1.1 계층형 아키텍처 (Layered Architecture)
- **Presentation Layer**: Web Controllers, REST API Controllers
- **Application Layer**: Service Layer, DTO
- **Domain Layer**: Entities, Repositories, Domain Logic
- **Infrastructure Layer**: Database, External API Clients

### 1.2 도메인 주도 설계 (DDD)
- **Entity**: 도메인 엔티티 (Order, Strategy, TradingPortfolio 등)
- **Repository**: 데이터 접근 추상화
- **Domain Service**: 도메인 로직 캡슐화

## 2. 디자인 패턴

### 2.1 전략 패턴 (Strategy Pattern)
- **MarketDataClient**: 시장 데이터 제공자 전략
  - `KoreaInvestmentMarketDataClient`: 한국투자증권 API 전략
- **TradingStrategyService**: 거래 전략 패턴
  - `ShortTermTradingStrategyService`: 단기 전략
  - `MediumTermTradingStrategyService`: 중기 전략 (향후)
  - `LongTermTradingStrategyService`: 장기 전략 (향후)

### 2.2 팩토리 패턴 (Factory Pattern)
- **MarketDataConfig**: MarketDataClient 팩토리
  - `@ConditionalOnProperty`를 통한 구현체 선택

### 2.3 빌더 패턴 (Builder Pattern)
- **Entity Builder**: Lombok `@Builder` 사용
  - `Order.builder()`
  - `Strategy.builder()`
  - `TradingPortfolio.builder()`

### 2.4 DTO 패턴 (Data Transfer Object)
- **Request DTO**: API 요청 데이터
  - `OrderRequestDto`
  - `AnalysisRequestDto`
  - `StrategyDto`
- **Response DTO**: API 응답 데이터
  - `OrderResponseDto`
  - `AnalysisResponseDto`
  - `TradingPortfolioDto`

### 2.5 리포지토리 패턴 (Repository Pattern)
- **JPA Repository**: Spring Data JPA 사용
  - `OrderRepository`
  - `StrategyRepository`
  - `TradingPortfolioRepository`

## 3. SOLID 원칙

### 3.1 단일 책임 원칙 (SRP)
- 각 클래스는 하나의 책임만 가짐
  - `OrderService`: 주문 실행만 담당
  - `AnalysisService`: 종목 분석만 담당
  - `StrategyService`: 전략 실행만 담당

### 3.2 개방-폐쇄 원칙 (OCP)
- **MarketDataClient 인터페이스**: 새로운 제공자 추가 시 기존 코드 수정 없이 확장 가능
- **StrategyType Enum**: 새로운 전략 타입 추가 가능

### 3.3 리스코프 치환 원칙 (LSP)
- **MarketDataClient 구현체**: 인터페이스 계약을 준수하여 상호 교체 가능

### 3.4 인터페이스 분리 원칙 (ISP)
- **MarketDataClient**: 필요한 메서드만 포함
  - `getIndicator()`
  - `getBulkIndicators()`
  - `getProviderName()`

### 3.5 의존성 역전 원칙 (DIP)
- **Service Layer**: Repository 인터페이스에 의존
- **MarketDataClient**: 인터페이스에 의존, 구현체는 런타임에 주입

## 4. 설계 원칙

### 4.1 KISS (Keep It Simple, Stupid)
- 복잡한 로직보다 단순하고 명확한 코드 선호
- 과도한 추상화 지양

### 4.2 DRY (Don't Repeat Yourself)
- 중복 코드 제거
- 공통 로직은 유틸리티 클래스나 공통 서비스로 추출

### 4.3 YAGNI (You Aren't Gonna Need It)
- 현재 필요하지 않은 기능은 구현하지 않음
- 미래 확장성보다 현재 요구사항에 집중

### 4.4 관심사의 분리 (SoC)
- **레이어 분리**: Presentation, Application, Domain, Infrastructure
- **책임 분리**: 각 서비스는 명확한 책임을 가짐

## 5. 예외 처리 패턴

### 5.1 계층화된 예외 처리
```
GlobalExceptionHandler (전역)
    │
    ├──► DomainException (비즈니스 규칙 위반)
    │    └──► ErrorCode 상수
    │
    └──► AppException (시스템 레벨 오류)
         └──► ErrorCode 상수
```

### 5.2 에러 응답 표준화
- 모든 API 오류는 `ErrorResponse` 형식으로 반환
- `traceId`를 통한 로그 추적 가능

## 6. 비동기 처리 패턴

### 6.1 Reactive Programming
- **WebFlux**: 시장 데이터 조회 시 비동기 처리
- **Mono/Flux**: Reactor 타입 사용

### 6.2 스케줄러 패턴
- **@Scheduled**: Spring 스케줄러 사용
- **StrategyScheduler**: 전략 실행 스케줄러
- **TradingPortfolioScheduler**: 포트폴리오 생성 스케줄러

## 7. 설정 관리 패턴

### 7.1 프로파일 기반 설정
- `application.yml`: 기본 설정
- `application-local.yml`: 로컬 개발 환경
- `application-dev.yml`: 개발 환경
- `application-prod.yml`: 프로덕션 환경

### 7.2 Properties 클래스
- **@ConfigurationProperties**: 타입 안전한 설정 바인딩
  - `MarketDataProperties`
  - `TradingProperties`

## 8. 테스트 패턴

### 8.1 테스트 슬라이스
- **@WebMvcTest**: 컨트롤러 테스트
- **@DataJpaTest**: 리포지토리 테스트
- **@SpringBootTest**: 통합 테스트

### 8.2 Mock 객체
- **Mockito**: 외부 의존성 모킹
- **@MockBean**: Spring Bean 모킹

## 9. 로깅 패턴

### 9.1 구조화 로깅
- JSON 형식의 구조화된 로그
- `traceId`를 통한 요청 추적

### 9.2 로그 레벨
- **ERROR**: 에러 발생 시
- **WARN**: 경고 상황
- **INFO**: 주요 비즈니스 이벤트
- **DEBUG**: 개발/디버깅 정보

## 10. 트랜잭션 관리

### 10.1 선언적 트랜잭션
- **@Transactional**: 서비스 메서드에 선언
- **트랜잭션 전파**: 기본값 (REQUIRED) 사용

### 10.2 읽기 전용 트랜잭션
- **@Transactional(readOnly = true)**: 조회 메서드에 적용

## 문서 변경 이력

| 버전 | 일자 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-28 | System | 문서 정리 및 구조화 |
