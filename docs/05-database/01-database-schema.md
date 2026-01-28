# 데이터베이스 스키마 설계

## 1. 데이터베이스 개요

### 1.1 데이터베이스 정보
- **DBMS**: MariaDB 11.8.5
- **문자셋**: utf8mb4
- **콜레이션**: utf8mb4_unicode_ci
- **엔진**: InnoDB

### 1.2 명명 규칙
- **테이블명**: `TB_` 접두사 사용 (예: `TB_ORDERS`)
- **컬럼명**: 스네이크 케이스 (예: `ACCOUNT_NO`)
- **인덱스명**: `IDX_` 접두사 사용
- **외래키명**: `FK_` 접두사 사용
- **고유키명**: `UK_` 접두사 사용

## 2. 테이블 목록

1. **TB_USERS**: 사용자 정보
2. **TB_USER_API_KEYS**: 사용자 API 키 (암호화 저장)
3. **TB_USER_ACCOUNTS**: 사용자 계좌 정보 (암호화 저장)
4. **TB_ORDERS**: 주문 정보
5. **TB_TRADING_SETTINGS**: 거래 설정
6. **TB_PORTFOLIOS**: 보유 종목
7. **TB_STRATEGIES**: 투자 전략
8. **TB_TRADING_PORTFOLIOS**: 트레이딩 포트폴리오
9. **TB_TRADING_PORTFOLIO_ITEMS**: 트레이딩 포트폴리오 종목

## 3. 테이블 상세

### 3.1 TB_USERS (사용자)

**목적**: 사용자 계정 정보를 저장합니다.

**컬럼**:
| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| TB_USERS_UID | VARCHAR(36) | PK | 사용자 고유 ID (UUID) |
| USERNAME | VARCHAR(50) | NOT NULL, UNIQUE | 사용자 ID |
| PASSWORD_HASH | VARCHAR(255) | NOT NULL | 비밀번호 해시 (BCrypt) |
| CREATED_AT | DATETIME | NOT NULL | 생성 시간 |
| UPDATED_AT | DATETIME | NULL | 수정 시간 |
| LAST_LOGIN_AT | DATETIME | NULL | 마지막 로그인 시간 |

**인덱스**:
- `IDX_USER_USERNAME`: USERNAME (UNIQUE)

### 3.2 TB_USER_API_KEYS (사용자 API 키)

**목적**: 사용자의 증권사 API 키를 암호화하여 저장합니다.

**컬럼**:
| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| TB_USER_API_KEYS_UID | VARCHAR(36) | PK | API 키 고유 ID (UUID) |
| USER_ID | VARCHAR(36) | NOT NULL, FK | 사용자 ID |
| BROKER_TYPE | VARCHAR(50) | NOT NULL | 증권사 타입 (코드) |
| APP_KEY_ENCRYPTED | TEXT | NOT NULL | App Key (암호화) |
| APP_SECRET_ENCRYPTED | TEXT | NOT NULL | App Secret (암호화) |
| SERVER_TYPE | VARCHAR(1) | NOT NULL | 서버 타입 ("1": 모의투자, "0": 실거래) |
| CREATED_AT | DATETIME | NOT NULL | 생성 시간 |
| UPDATED_AT | DATETIME | NULL | 수정 시간 |

**인덱스**:
- `IDX_USER_API_KEY_USER_ID`: USER_ID
- `IDX_USER_API_KEY_BROKER_TYPE`: BROKER_TYPE

**외래키**:
- `FK_USER_API_KEYS_USER`: USER_ID → TB_USERS.TB_USERS_UID (ON DELETE CASCADE)

**비고**:
- 한국투자증권: API 키가 계좌별로 발급됨 (API 키 : 계좌 = 1:1)
- 다른 증권사: 증권사별 하나의 API 키로 여러 계좌 관리 가능 (API 키 : 계좌 = 1:N)

### 3.3 TB_USER_ACCOUNTS (사용자 계좌)

**목적**: 사용자의 증권사 계좌 정보를 암호화하여 저장합니다.

**컬럼**:
| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| TB_USER_ACCOUNTS_UID | VARCHAR(36) | PK | 계좌 고유 ID (UUID) |
| USER_ID | VARCHAR(36) | NOT NULL, FK | 계좌 소유자 |
| USER_API_KEY_ID | VARCHAR(36) | NOT NULL, FK | 계좌에 사용할 API 키 |
| ACCOUNT_NO_ENCRYPTED | TEXT | NOT NULL | 계좌번호 (암호화) |
| BROKER_TYPE | VARCHAR(50) | NOT NULL | 증권사 코드 |
| ACCOUNT_NAME | VARCHAR(100) | NULL | 계좌 별칭 |
| IS_DEFAULT | TINYINT(1) | NOT NULL, DEFAULT 0 | 메인 계좌 여부 |
| IS_ACTIVE | TINYINT(1) | NOT NULL, DEFAULT 1 | 활성화 여부 |
| CREATED_AT | DATETIME | NOT NULL | 생성 시간 |
| UPDATED_AT | DATETIME | NULL | 수정 시간 |

**인덱스**:
- `IDX_USER_ACCOUNTS_USER_ID`: USER_ID
- `IDX_USER_ACCOUNTS_USER_API_KEY`: USER_API_KEY_ID
- `IDX_USER_ACCOUNTS_BROKER_TYPE`: BROKER_TYPE
- `IDX_USER_ACCOUNTS_IS_DEFAULT`: USER_ID, IS_DEFAULT
- `UK_USER_ACCOUNTS_USER_ACCOUNT`: USER_ID, ACCOUNT_NO_ENCRYPTED, BROKER_TYPE (UNIQUE)

**외래키**:
- `FK_USER_ACCOUNTS_USER`: USER_ID → TB_USERS.TB_USERS_UID (ON DELETE CASCADE)
- `FK_USER_ACCOUNTS_API_KEY`: USER_API_KEY_ID → TB_USER_API_KEYS.TB_USER_API_KEYS_UID (ON DELETE RESTRICT)

**비즈니스 규칙**:
- 사용자당 메인 계좌는 1개만 가능 (IS_DEFAULT = 1)
- 계좌 생성 시 메인 계좌가 없으면 자동으로 메인 계좌로 설정
- 메인 계좌 변경 시 기존 메인 계좌는 자동으로 해제
- USER_API_KEY_ID의 BROKER_TYPE과 BROKER_TYPE 컬럼은 일치해야 함
- 한국투자증권: USER_API_KEY_ID와 계좌는 1:1 관계
- 다른 증권사: USER_API_KEY_ID와 계좌는 1:N 관계

### 3.4 TB_ORDERS (주문)

**목적**: 주식 매수/매도 주문 정보를 저장합니다.

**컬럼**:
| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| TB_ORDERS_UID | VARCHAR(36) | PK | 주문 고유 ID (UUID) |
| ACCOUNT_NO | VARCHAR(20) | NOT NULL | 계좌번호 |
| USER_ID | VARCHAR(36) | NULL | 사용자 ID (선택적, 점진적 마이그레이션용) |
| SYMBOL | VARCHAR(20) | NOT NULL | 종목 코드 |
| ORDER_TYPE | VARCHAR(10) | NOT NULL | 주문 유형 (BUY, SELL) |
| QUANTITY | INT | NOT NULL | 주문 수량 |
| PRICE | DECIMAL(18,2) | NOT NULL | 주문 가격 |
| STATUS | VARCHAR(20) | NOT NULL | 주문 상태 |
| EXECUTED_QUANTITY | INT | NULL | 체결 수량 |
| EXECUTED_PRICE | DECIMAL(18,2) | NULL | 체결 가격 |
| ORDER_TIME | DATETIME | NOT NULL | 주문 시간 |
| EXECUTED_TIME | DATETIME | NULL | 체결 시간 |
| MESSAGE | VARCHAR(500) | NULL | 메시지 |
| CREATED_AT | DATETIME | NOT NULL | 생성 시간 |
| UPDATED_AT | DATETIME | NULL | 수정 시간 |

**인덱스**:
- `IDX_TB_ORDERS_ACCOUNT_NO`: ACCOUNT_NO
- `IDX_TB_ORDERS_USER_ID`: USER_ID
- `IDX_TB_ORDERS_ORDER_TIME`: ORDER_TIME

### 3.5 TB_TRADING_SETTINGS (거래 설정)

**목적**: 계좌별 거래 제약 조건을 저장합니다.

**컬럼**:
| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| TB_TRADING_SETTINGS_UID | VARCHAR(36) | PK | 설정 고유 ID (UUID) |
| ACCOUNT_NO | VARCHAR(20) | NOT NULL | 계좌번호 |
| USER_ID | VARCHAR(36) | NULL | 사용자 ID (사용자별 설정 관리용) |
| MAX_INVESTMENT_AMOUNT | DECIMAL(18,2) | NOT NULL | 최대 투자금액 |
| MIN_INVESTMENT_AMOUNT | DECIMAL(18,2) | NOT NULL | 최소 투자금액 |
| DEFAULT_CURRENCY | VARCHAR(3) | NOT NULL | 기본 통화 |
| AUTO_TRADING_ENABLED | TINYINT(1) | NOT NULL | 자동 매매 활성화 여부 |
| RISK_LEVEL | DECIMAL(3,2) | NULL | 리스크 레벨 (0.0 ~ 1.0) |
| CREATED_AT | DATETIME | NOT NULL | 생성 시간 |
| UPDATED_AT | DATETIME | NULL | 수정 시간 |

**인덱스**:
- `IDX_TB_TRADING_SETTINGS_ACCOUNT_NO`: ACCOUNT_NO
- `IDX_TB_TRADING_SETTINGS_USER_ID`: USER_ID
- `UK_TB_TRADING_SETTINGS_USER_ACCOUNT`: USER_ID, ACCOUNT_NO (UNIQUE)

### 3.6 TB_PORTFOLIOS (보유 종목)

**목적**: 계좌의 보유 종목 정보를 저장합니다.

**컬럼**:
| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| TB_PORTFOLIOS_UID | VARCHAR(36) | PK | 포트폴리오 고유 ID (UUID) |
| ACCOUNT_NO | VARCHAR(20) | NOT NULL | 계좌번호 |
| USER_ID | VARCHAR(36) | NULL | 사용자 ID (선택적, 점진적 마이그레이션용) |
| SYMBOL | VARCHAR(20) | NOT NULL | 종목 코드 |
| NAME | VARCHAR(100) | NULL | 종목명 |
| QUANTITY | INT | NOT NULL | 보유 수량 |
| AVERAGE_PRICE | DECIMAL(18,2) | NOT NULL | 평균 매수가 |
| CURRENT_PRICE | DECIMAL(18,2) | NULL | 현재가 |
| CURRENCY | VARCHAR(3) | NOT NULL | 통화 |
| LAST_UPDATED | DATETIME | NULL | 마지막 업데이트 시간 |
| CREATED_AT | DATETIME | NOT NULL | 생성 시간 |
| UPDATED_AT | DATETIME | NULL | 수정 시간 |

**인덱스**:
- `UK_TB_PORTFOLIOS_ACCOUNT_SYMBOL`: ACCOUNT_NO, SYMBOL (UNIQUE)
- `IDX_TB_PORTFOLIOS_ACCOUNT_NO`: ACCOUNT_NO
- `IDX_TB_PORTFOLIOS_USER_ID`: USER_ID
- `IDX_TB_PORTFOLIOS_SYMBOL`: SYMBOL

### 3.7 TB_STRATEGIES (투자 전략)

**목적**: 투자 전략 정보 및 실행 통계를 저장합니다.

**컬럼**:
| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| TB_STRATEGIES_UID | VARCHAR(36) | PK | 전략 고유 ID (UUID) |
| ACCOUNT_NO | VARCHAR(20) | NOT NULL | 계좌번호 |
| USER_ID | VARCHAR(36) | NULL | 사용자 ID (선택적, 점진적 마이그레이션용) |
| STRATEGY_TYPE | VARCHAR(20) | NOT NULL | 전략 타입 |
| STATUS | VARCHAR(20) | NOT NULL | 전략 상태 |
| MAX_INVESTMENT_AMOUNT | DECIMAL(18,2) | NULL | 최대 투자금액 |
| MIN_INVESTMENT_AMOUNT | DECIMAL(18,2) | NULL | 최소 투자금액 |
| RISK_LEVEL | DECIMAL(3,2) | NULL | 리스크 레벨 |
| CONFIDENCE_THRESHOLD | DECIMAL(3,2) | NULL | 신뢰도 임계값 |
| LAST_EXECUTED_AT | DATETIME | NULL | 마지막 실행 시간 |
| TOTAL_EXECUTIONS | BIGINT | DEFAULT 0 | 총 실행 횟수 |
| SUCCESS_COUNT | BIGINT | DEFAULT 0 | 성공 횟수 |
| FAILURE_COUNT | BIGINT | DEFAULT 0 | 실패 횟수 |
| TOTAL_PROFIT_LOSS | DECIMAL(18,2) | DEFAULT 0 | 총 손익 |
| CREATED_AT | DATETIME | NOT NULL | 생성 시간 |
| UPDATED_AT | DATETIME | NULL | 수정 시간 |

**인덱스**:
- `UK_TB_STRATEGIES_ACCOUNT_TYPE`: ACCOUNT_NO, STRATEGY_TYPE (UNIQUE)
- `UK_TB_STRATEGIES_USER_ACCOUNT_TYPE`: USER_ID, ACCOUNT_NO, STRATEGY_TYPE (UNIQUE)
- `IDX_TB_STRATEGIES_ACCOUNT_NO`: ACCOUNT_NO
- `IDX_TB_STRATEGIES_USER_ID`: USER_ID
- `IDX_TB_STRATEGIES_STATUS`: STATUS

### 3.8 TB_TRADING_PORTFOLIOS (트레이딩 포트폴리오)

**목적**: 일별 트레이딩 계획을 저장합니다.

**컬럼**:
| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| TB_TRADING_PORTFOLIOS_UID | VARCHAR(36) | PK | 포트폴리오 고유 ID (UUID) |
| TRADING_DATE | DATE | NOT NULL, UNIQUE | 거래일 |
| MARKET_SUMMARY | TEXT | NULL | 시장 요약 |
| TOP_SECTOR_1 | VARCHAR(100) | NULL | 유망 섹터 1 |
| TOP_SECTOR_2 | VARCHAR(100) | NULL | 유망 섹터 2 |
| TOP_SECTOR_3 | VARCHAR(100) | NULL | 유망 섹터 3 |
| RISK_MANAGEMENT_STRATEGY | TEXT | NULL | 리스크 관리 전략 |
| POSITION_SIZE | DECIMAL(18,2) | NULL | 포지션 사이즈 |
| CREATED_AT | DATETIME | NOT NULL | 생성 시간 |
| UPDATED_AT | DATETIME | NULL | 수정 시간 |

**인덱스**:
- `IDX_TB_TRADING_PORTFOLIOS_DATE`: TRADING_DATE

### 3.9 TB_TRADING_PORTFOLIO_ITEMS (트레이딩 포트폴리오 종목)

**목적**: 트레이딩 포트폴리오의 개별 종목 정보를 저장합니다.

**컬럼**:
| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| TB_TRADING_PORTFOLIO_ITEMS_UID | VARCHAR(36) | PK | 종목 고유 ID (UUID) |
| TRADING_PORTFOLIO_ID | VARCHAR(36) | NOT NULL, FK | 포트폴리오 ID |
| SYMBOL | VARCHAR(20) | NOT NULL | 종목 코드 |
| NAME | VARCHAR(100) | NULL | 종목명 |
| ENTRY_PRICE_MIN | DECIMAL(18,2) | NULL | 최소 진입가 |
| ENTRY_PRICE_MAX | DECIMAL(18,2) | NULL | 최대 진입가 |
| STOP_LOSS_PRICE | DECIMAL(18,2) | NULL | 손절가 |
| TARGET_PRICE_1 | DECIMAL(18,2) | NULL | 목표가 1 |
| TARGET_PRICE_2 | DECIMAL(18,2) | NULL | 목표가 2 |
| EXPECTED_RETURN_RATE | DECIMAL(5,2) | NULL | 기대 수익률 (%) |
| RISK_REWARD_RATIO | DECIMAL(5,2) | NULL | 리스크/리워드 비율 |
| TECHNICAL_BASIS | TEXT | NULL | 기술적 근거 |
| SUPPLY_DEMAND_BASIS | TEXT | NULL | 수급 근거 |
| CATALYST_FACTOR | TEXT | NULL | 촉매 요인 |
| BUY_TIME | TIME | NOT NULL | 매수 시간 |
| SELL_TIME | TIME | NULL | 매도 시간 |
| INVESTMENT_AMOUNT | DECIMAL(18,2) | NULL | 투자금액 |
| EXPECTED_PROFIT | DECIMAL(18,2) | NULL | 예상 수익 |
| RANKING | INT | NOT NULL | 순위 |
| CREATED_AT | DATETIME | NOT NULL | 생성 시간 |
| UPDATED_AT | DATETIME | NULL | 수정 시간 |

**인덱스**:
- `IDX_TB_TRADING_PORTFOLIO_ITEMS_PORTFOLIO`: TRADING_PORTFOLIO_ID
- `IDX_TB_TRADING_PORTFOLIO_ITEMS_SYMBOL`: SYMBOL
- `IDX_TB_TRADING_PORTFOLIO_ITEMS_RANKING`: RANKING

**외래키**:
- `FK_TB_TRADING_PORTFOLIO_ITEMS_PORTFOLIO`: TRADING_PORTFOLIO_ID → TB_TRADING_PORTFOLIOS.TB_TRADING_PORTFOLIOS_UID (ON DELETE CASCADE)

## 4. 관계도

```
TB_USERS (1:N) TB_USER_API_KEYS
  └── USER_ID (FK, CASCADE DELETE)

TB_USERS (1:N) TB_USER_ACCOUNTS
  └── USER_ID (FK, CASCADE DELETE)

TB_USER_API_KEYS (1:N) TB_USER_ACCOUNTS
  └── USER_API_KEY_ID (FK, RESTRICT DELETE)
  └── 한국투자증권: 1:1 관계
  └── 다른 증권사: 1:N 관계

TB_TRADING_SETTINGS (1:1) ACCOUNT_NO
TB_PORTFOLIOS (1:N) ACCOUNT_NO
TB_STRATEGIES (1:N) ACCOUNT_NO
TB_ORDERS (1:N) ACCOUNT_NO

TB_TRADING_PORTFOLIOS (1:N) TB_TRADING_PORTFOLIO_ITEMS
  └── TRADING_PORTFOLIO_ID (FK, CASCADE DELETE)
```

## 5. 인덱스 전략

### 5.1 주요 인덱스
- **계좌번호**: 대부분의 테이블에서 계좌번호로 조회하므로 인덱스 필수
- **거래일**: 트레이딩 포트폴리오는 날짜로 조회
- **주문 시간**: 주문 목록 조회 시 시간순 정렬
- **전략 상태**: 활성 전략만 조회하는 경우가 많음

### 5.2 복합 인덱스
- `UK_TB_PORTFOLIOS_ACCOUNT_SYMBOL`: 계좌번호 + 종목 코드 (UNIQUE)
- `UK_TB_STRATEGIES_ACCOUNT_TYPE`: 계좌번호 + 전략 타입 (UNIQUE)

## 6. 데이터 타입 선택

### 6.1 금액 필드
- **DECIMAL(18,2)**: 정밀도가 중요한 금액 필드
- 소수점 2자리까지 저장

### 6.2 날짜/시간 필드
- **DATETIME**: 타임스탬프 저장
- **DATE**: 날짜만 저장
- **TIME**: 시간만 저장

### 6.3 문자열 필드
- **VARCHAR(20)**: 종목 코드, 계좌번호 등 짧은 문자열
- **VARCHAR(100)**: 종목명 등 중간 길이 문자열
- **TEXT**: 긴 텍스트 (시장 요약, 분석 근거 등)

## 7. 제약 조건

### 7.1 UNIQUE 제약
- `TB_USERS.USERNAME`: 사용자 ID는 고유
- `TB_USER_ACCOUNTS.USER_ID + ACCOUNT_NO_ENCRYPTED + BROKER_TYPE`: 사용자당 동일 증권사의 동일 계좌번호 중복 방지
- `TB_TRADING_SETTINGS.USER_ID + ACCOUNT_NO`: 사용자당 계좌당 설정은 1개
- `TB_PORTFOLIOS.ACCOUNT_NO + SYMBOL`: 계좌당 종목은 1개
- `TB_STRATEGIES.ACCOUNT_NO + STRATEGY_TYPE`: 계좌당 전략 타입은 1개 (하위 호환성)
- `TB_STRATEGIES.USER_ID + ACCOUNT_NO + STRATEGY_TYPE`: 사용자당 계좌당 전략 타입은 1개
- `TB_TRADING_PORTFOLIOS.TRADING_DATE`: 거래일당 포트폴리오는 1개

### 7.2 외래키 제약
- `TB_USER_API_KEYS.USER_ID`: TB_USERS (CASCADE DELETE)
- `TB_USER_ACCOUNTS.USER_ID`: TB_USERS (CASCADE DELETE)
- `TB_USER_ACCOUNTS.USER_API_KEY_ID`: TB_USER_API_KEYS (RESTRICT DELETE)
- `TB_TRADING_PORTFOLIO_ITEMS.TRADING_PORTFOLIO_ID`: CASCADE DELETE

## 8. 마이그레이션 전략

### 8.1 JPA DDL
- **개발 환경**: `ddl-auto: update` (자동 스키마 생성)
- **프로덕션 환경**: `ddl-auto: validate` (스키마 검증만)

### 8.2 수동 마이그레이션
- `schema.sql`: 초기 스키마 생성 스크립트
- Flyway 또는 Liquibase 사용 고려 (향후)

## 문서 변경 이력

| 버전 | 일자 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-28 | System | 문서 정리 및 구조화 |
| 2.0 | 2026-01-28 | System | 사용자 및 계좌 관리 테이블 추가 (TB_USERS, TB_USER_API_KEYS, TB_USER_ACCOUNTS) |
