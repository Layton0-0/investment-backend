# 도메인 모델 설계

## 1. 도메인 개요

Investment Choi 시스템의 핵심 도메인은 **투자 거래**입니다. 주요 도메인 개념은 다음과 같습니다:

- **계좌 (Account)**: 투자 계좌
- **주문 (Order)**: 매수/매도 주문
- **전략 (Strategy)**: 투자 전략
- **포트폴리오 (Portfolio)**: 보유 종목
- **트레이딩 포트폴리오 (Trading Portfolio)**: 일별 거래 계획
- **거래 설정 (Trading Setting)**: 거래 제약 조건

## 2. 엔티티 상세

### 2.1 Order (주문)

**목적**: 주식 매수/매도 주문 정보를 저장합니다.

**속성**:
- `id` (UUID): 주문 고유 ID
- `accountNo` (String): 계좌번호
- `symbol` (String): 종목 코드
- `orderType` (Enum): 주문 유형 (BUY, SELL)
- `quantity` (Integer): 주문 수량
- `price` (BigDecimal): 주문 가격
- `status` (String): 주문 상태 (PENDING, EXECUTED, CANCELLED, FAILED)
- `executedQuantity` (Integer): 체결 수량
- `executedPrice` (BigDecimal): 체결 가격
- `orderTime` (LocalDateTime): 주문 시간
- `executedTime` (LocalDateTime): 체결 시간
- `message` (String): 메시지
- `createdAt` (LocalDateTime): 생성 시간
- `updatedAt` (LocalDateTime): 수정 시간

**비즈니스 규칙**:
- 주문 금액은 최대 투자금액을 초과할 수 없음
- 주문 금액은 최소 투자금액 이상이어야 함
- 체결된 주문은 취소할 수 없음

### 2.2 Strategy (전략)

**목적**: 투자 전략 정보 및 실행 통계를 저장합니다.

**속성**:
- `id` (UUID): 전략 고유 ID
- `accountNo` (String): 계좌번호
- `strategyType` (Enum): 전략 타입 (SHORT_TERM, MEDIUM_TERM, LONG_TERM)
- `status` (Enum): 전략 상태 (ACTIVE, STOPPED, PAUSED)
- `maxInvestmentAmount` (BigDecimal): 최대 투자금액
- `minInvestmentAmount` (BigDecimal): 최소 투자금액
- `riskLevel` (BigDecimal): 리스크 레벨 (0.0 ~ 1.0)
- `confidenceThreshold` (BigDecimal): 신뢰도 임계값 (0.0 ~ 1.0)
- `lastExecutedAt` (LocalDateTime): 마지막 실행 시간
- `totalExecutions` (Long): 총 실행 횟수
- `successCount` (Long): 성공 횟수
- `failureCount` (Long): 실패 횟수
- `totalProfitLoss` (BigDecimal): 총 손익
- `createdAt` (LocalDateTime): 생성 시간
- `updatedAt` (LocalDateTime): 수정 시간

**비즈니스 규칙**:
- 계좌별 전략 타입은 유일해야 함 (UNIQUE KEY)
- ACTIVE 상태인 전략만 실행됨
- 신뢰도가 임계값 이상일 때만 주문 실행

**도메인 메서드**:
- `activate()`: 전략 활성화
- `stop()`: 전략 중지
- `pause()`: 전략 일시 정지
- `recordExecution(boolean success, BigDecimal profitLoss)`: 실행 결과 기록
- `isActive()`: 활성 상태 여부 확인

### 2.3 TradingPortfolio (트레이딩 포트폴리오)

**목적**: 일별 트레이딩 계획을 저장합니다.

**속성**:
- `id` (UUID): 포트폴리오 고유 ID
- `tradingDate` (LocalDate): 거래일 (UNIQUE)
- `marketSummary` (String): 시장 요약
- `topSector1` (String): 유망 섹터 1
- `topSector2` (String): 유망 섹터 2
- `topSector3` (String): 유망 섹터 3
- `riskManagementStrategy` (String): 리스크 관리 전략
- `positionSize` (BigDecimal): 포지션 사이즈
- `items` (List<TradingPortfolioItem>): 종목 목록 (OneToMany)
- `createdAt` (LocalDateTime): 생성 시간
- `updatedAt` (LocalDateTime): 수정 시간

**비즈니스 규칙**:
- 거래일은 유일해야 함 (UNIQUE KEY)
- 매일 최대 1개의 포트폴리오만 생성 가능

**도메인 메서드**:
- `addItem(TradingPortfolioItem item)`: 종목 추가
- `updateMarketSummary(String summary)`: 시장 요약 업데이트
- `updateTopSectors(String s1, String s2, String s3)`: 유망 섹터 업데이트

### 2.4 TradingPortfolioItem (트레이딩 포트폴리오 종목)

**목적**: 트레이딩 포트폴리오의 개별 종목 정보를 저장합니다.

**속성**:
- `id` (UUID): 종목 고유 ID
- `tradingPortfolio` (TradingPortfolio): 포트폴리오 (ManyToOne)
- `symbol` (String): 종목 코드
- `name` (String): 종목명
- `entryPriceMin` (BigDecimal): 최소 진입가
- `entryPriceMax` (BigDecimal): 최대 진입가
- `stopLossPrice` (BigDecimal): 손절가
- `targetPrice1` (BigDecimal): 목표가 1
- `targetPrice2` (BigDecimal): 목표가 2
- `expectedReturnRate` (BigDecimal): 기대 수익률 (%)
- `riskRewardRatio` (BigDecimal): 리스크/리워드 비율
- `technicalBasis` (String): 기술적 근거
- `supplyDemandBasis` (String): 수급 근거
- `catalystFactor` (String): 촉매 요인
- `buyTime` (LocalTime): 매수 시간
- `sellTime` (LocalTime): 매도 시간
- `investmentAmount` (BigDecimal): 투자금액
- `expectedProfit` (BigDecimal): 예상 수익
- `ranking` (Integer): 순위
- `createdAt` (LocalDateTime): 생성 시간
- `updatedAt` (LocalDateTime): 수정 시간

**비즈니스 규칙**:
- 진입가 범위: `entryPriceMin <= entryPriceMax`
- 손절가 < 진입가 < 목표가1 < 목표가2
- 리스크/리워드 비율은 1.0 이상 권장

### 2.5 TradingSetting (거래 설정)

**목적**: 계좌별 거래 제약 조건을 저장합니다.

**속성**:
- `id` (UUID): 설정 고유 ID
- `accountNo` (String): 계좌번호 (UNIQUE)
- `maxInvestmentAmount` (BigDecimal): 최대 투자금액
- `minInvestmentAmount` (BigDecimal): 최소 투자금액
- `defaultCurrency` (String): 기본 통화
- `autoTradingEnabled` (Boolean): 자동 매매 활성화 여부
- `riskLevel` (BigDecimal): 리스크 레벨
- `createdAt` (LocalDateTime): 생성 시간
- `updatedAt` (LocalDateTime): 수정 시간

**비즈니스 규칙**:
- 계좌번호는 유일해야 함 (UNIQUE KEY)
- 최대 투자금액 > 최소 투자금액
- 리스크 레벨 범위: 0.0 ~ 1.0

### 2.6 Portfolio (포트폴리오)

**목적**: 계좌의 보유 종목 정보를 저장합니다.

**속성**:
- `id` (UUID): 포트폴리오 고유 ID
- `accountNo` (String): 계좌번호
- `symbol` (String): 종목 코드
- `name` (String): 종목명
- `quantity` (Integer): 보유 수량
- `averagePrice` (BigDecimal): 평균 매수가
- `currentPrice` (BigDecimal): 현재가
- `currency` (String): 통화
- `lastUpdated` (LocalDateTime): 마지막 업데이트 시간
- `createdAt` (LocalDateTime): 생성 시간
- `updatedAt` (LocalDateTime): 수정 시간

**비즈니스 규칙**:
- 계좌번호 + 종목 코드 조합은 유일해야 함 (UNIQUE KEY)

## 3. 도메인 값 객체 (Value Objects)

### 3.1 StrategyType (전략 타입)
- `SHORT_TERM`: 단기 전략
- `MEDIUM_TERM`: 중기 전략
- `LONG_TERM`: 장기 전략

### 3.2 StrategyStatus (전략 상태)
- `ACTIVE`: 활성 (실행 중)
- `STOPPED`: 중지됨
- `PAUSED`: 일시 정지

## 4. 도메인 서비스

### 4.1 OrderService
- 주문 실행 비즈니스 로직
- 주문 검증 (금액, 수량 등)

### 4.2 StrategyService
- 전략 실행 비즈니스 로직
- 매매 결정 로직

### 4.3 TradingPortfolioService
- 트레이딩 포트폴리오 생성 비즈니스 로직
- 종목 스크리닝 및 분석

## 5. 리포지토리 인터페이스

### 5.1 OrderRepository
- `findByAccountNo(String accountNo)`: 계좌별 주문 조회
- `findByIdAndAccountNo(String id, String accountNo)`: 주문 조회

### 5.2 StrategyRepository
- `findByAccountNo(String accountNo)`: 계좌별 전략 조회
- `findByAccountNoAndStrategyType(String accountNo, StrategyType type)`: 전략 조회
- `findByStatus(StrategyStatus status)`: 상태별 전략 조회

### 5.3 TradingPortfolioRepository
- `findByTradingDate(LocalDate date)`: 날짜별 포트폴리오 조회
- `existsByTradingDate(LocalDate date)`: 날짜별 포트폴리오 존재 여부

### 5.4 TradingSettingRepository
- `findByAccountNo(String accountNo)`: 계좌별 설정 조회

## 6. 도메인 이벤트 (향후 확장)

향후 이벤트 기반 아키텍처로 확장 시 고려할 이벤트:

- `OrderExecutedEvent`: 주문 체결 이벤트
- `StrategyExecutedEvent`: 전략 실행 이벤트
- `PortfolioUpdatedEvent`: 포트폴리오 업데이트 이벤트

## 문서 변경 이력

| 버전 | 일자 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-28 | System | 문서 정리 및 구조화 |
