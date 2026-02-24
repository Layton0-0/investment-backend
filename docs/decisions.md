# 아키텍처 결정 사항 (Architecture Decision Records)

이 문서는 Investment Choi 프로젝트의 주요 아키텍처 및 기술 결정 사항을 기록합니다. 각 결정의 배경, 대안, 그리고 선택한 이유를 명확히 문서화하여 향후 의사결정 시 참고할 수 있도록 합니다.

## 결정 사항 목록

1. [모노리프 vs 마이크로서비스](#1-모노리프-vs-마이크로서비스)
2. [Java 버전 선택](#2-java-버전-선택)
3. [Spring Boot 버전 선택](#3-spring-boot-버전-선택)
4. [데이터베이스 선택](#4-데이터베이스-선택)
5. [아키텍처 패턴 선택](#5-아키텍처-패턴-선택)
6. [시장 데이터 제공자 선택](#6-시장-데이터-제공자-선택)
7. [캐싱 전략](#7-캐싱-전략)
8. [예외 처리 전략](#8-예외-처리-전략)
9. [API 설계 결정](#9-api-설계-결정)
10. [보안 설계 결정](#10-보안-설계-결정)
11. [시장 차원(KR/US) 도입](#11-시장-차원krus-도입)
12. [뉴스·공시 파이프라인 채택](#12-뉴스공시-파이프라인-채택)
13. [로깅 시 민감정보 마스킹 표준화](#13-로깅-시-민감정보-마스킹-표준화)
14. [한국투자증권 API 요청 방식 및 MCP 사용](#14-한국투자증권-api-요청-방식-및-mcp-사용)
15. [TimescaleDB 전환](#15-timescaledb-전환)
16. [Alpha-Risk-Execution 분리 및 Portfolio/Risk 레이어 도입](#16-alpha-risk-execution-분리-및-portfoliorisk-레이어-도입)
17. [관리자 계정 생성·로그인 정책](#17-관리자-계정-생성로그인-정책)
18. [KIS Open API 실전 구축](#18-kis-open-api-실전-구축-토큰-장전주문-큐순위투자자websocket)
19. [데이터 정합성 (수정주가)](#19-데이터-정합성-수정주가)
20. [Point-in-Time 및 Look-ahead 방지](#20-point-in-time-및-look-ahead-방지)
21. [전략 거버넌스·중단 원칙](#21-전략-거버넌스중단-원칙)
22. [파이프라인 실행 플래그 계정별 저장](#22-파이프라인-실행-플래그-계정별-저장)
23. [시스템 표준 시간 Asia/Seoul](#23-시스템-표준-시간-asiaseoul)
24. [전략 비중 동적 결정](#24-전략-비중-동적-결정)
25. [국내/미국 전략 시스템 기본화 및 조회 중심](#25-국내미국-전략-시스템-기본화-및-조회-중심)

---

## 1. 모노리프 vs 마이크로서비스

**결정일**: 2026년 1월  
**상태**: 확정  
**결정**: 모노리프 아키텍처 채택

### 배경
프로젝트 초기 단계에서 시스템 아키텍처를 결정해야 했습니다. 마이크로서비스와 모노리프 중 선택이 필요했습니다.

### 고려 사항
- **팀 규모**: 소규모 개발 팀
- **프로젝트 규모**: 중소규모 프로젝트
- **배포 복잡도**: 단순한 배포 프로세스 선호
- **비용**: 최소 비용 구성 목표

### 대안

#### 대안 1: 마이크로서비스
**장점**:
- 서비스별 독립적 배포 및 확장 가능
- 기술 스택 다양화 가능
- 서비스별 장애 격리

**단점**:
- 운영 복잡도 증가
- 네트워크 오버헤드
- 분산 시스템 복잡성
- 높은 인프라 비용

#### 대안 2: 모노리프 (선택)
**장점**:
- 단순한 배포 및 운영
- 낮은 인프라 비용
- 개발 및 디버깅 용이
- 빠른 개발 속도

**단점**:
- 서비스별 독립적 확장 어려움
- 기술 스택 고정

### 결정 이유
1. **비용 효율성**: 단일 서버 배포로 최소 비용 구성 가능
2. **개발 속도**: 초기 개발 단계에서 빠른 개발 및 배포 가능
3. **운영 단순성**: 소규모 팀에서 관리하기 쉬운 구조
4. **확장 가능성**: 향후 필요 시 마이크로서비스로 전환 가능

### 영향
- 단일 Spring Boot 애플리케이션으로 모든 기능 구현
- Python AI 서비스는 별도 디렉토리로 관리하되 같은 프로젝트 내 유지
- 단일 서버 배포 전략 채택

**참고 문서**: [시스템 아키텍처](./02-architecture/01-system-architecture.md), [필수 기술 스펙](./02-architecture/10-essential-tech-spec.md)

---

## 2. Java 버전 선택

**결정일**: 2026년 1월  
**상태**: 확정  
**결정**: Java 17 (SE) 사용

### 배경
Spring Boot 3.x는 Java 17 이상을 요구합니다.

### 고려 사항
- Spring Boot 호환성
- 장기 지원 (LTS) 버전
- 성능 및 기능

### 대안

#### 대안 1: Java 11
- Spring Boot 2.x와 호환
- LTS 버전
- **단점**: Spring Boot 3.x 미지원

#### 대안 2: Java 17 (선택)
- Spring Boot 3.x 호환
- LTS 버전 (2029년까지 지원)
- 향상된 성능 및 기능

#### 대안 3: Java 21
- 최신 LTS 버전
- **단점**: 일부 라이브러리 호환성 문제 가능

### 결정 이유
1. **Spring Boot 3.x 호환성**: 최신 Spring Boot 기능 활용
2. **장기 지원**: 2029년까지 LTS 지원
3. **안정성**: 충분히 검증된 버전

### 영향
- 모든 개발 환경에서 Java 17 사용
- CI/CD 파이프라인에서 Java 17 빌드

**참고 문서**: [필수 기술 스펙](./02-architecture/10-essential-tech-spec.md)

---

## 3. Spring Boot 버전 선택

**결정일**: 2026년 1월  
**상태**: 확정  
**결정**: Spring Boot 3.2.2 사용

### 배경
Spring Boot 2.7에서 3.2로 업그레이드 결정

### 고려 사항
- Java 버전 호환성
- 최신 기능 및 보안 패치
- 라이브러리 호환성

### 결정 이유
1. **최신 기능**: Spring Boot 3.x의 최신 기능 활용
2. **보안**: 최신 보안 패치 적용
3. **성능**: 향상된 성능 및 최적화

### 영향
- Spring Boot 3.2.2 기반 개발
- Jakarta EE 네임스페이스 사용 (javax → jakarta)
- 의존성 업데이트 필요

**참고 문서**: [필수 기술 스펙](./02-architecture/10-essential-tech-spec.md)

---

## 4. 데이터베이스 선택

**결정일**: 2026년 1월  
**상태**: 확정  
**결정**: MariaDB 11.8.5 사용

### 배경
관계형 데이터베이스 선택 필요

### 고려 사항
- 비용
- 성능
- MySQL 호환성
- 오픈소스

### 대안

#### 대안 1: MySQL
- 널리 사용됨
- **단점**: 상용 라이선스 고려 필요

#### 대안 2: MariaDB (선택)
- MySQL 호환
- 완전 오픈소스
- 우수한 성능
- 비용 효율적

#### 대안 3: PostgreSQL
- 고급 기능
- **단점**: MySQL과의 호환성 차이

### 결정 이유
1. **비용 효율성**: 완전 오픈소스
2. **MySQL 호환성**: 기존 MySQL 도구 및 지식 활용 가능
3. **성능**: 우수한 성능 제공

### 영향
- MariaDB 11.8.5 이상 사용
- JPA/Hibernate를 통한 데이터 접근

**참고 문서**: [데이터베이스 스키마](./05-database/01-database-schema.md)

---

## 5. 아키텍처 패턴 선택

**결정일**: 2026년 1월  
**상태**: 확정  
**결정**: 계층형 아키텍처 (Layered Architecture) + DDD 요소

### 배경
시스템 구조 설계 시 아키텍처 패턴 선택 필요

### 고려 사항
- 유지보수성
- 테스트 용이성
- 확장성

### 대안

#### 대안 1: 계층형 아키텍처 (선택)
**구조**:
- Presentation Layer
- Application Layer
- Domain Layer
- Infrastructure Layer

**장점**:
- 명확한 책임 분리
- 이해하기 쉬움
- 테스트 용이

#### 대안 2: 헥사고날 아키텍처
- **단점**: 초기 프로젝트에 과도한 복잡성

### 결정 이유
1. **단순성**: 명확하고 이해하기 쉬운 구조
2. **유지보수성**: 각 레이어의 명확한 책임
3. **테스트 용이성**: 레이어별 독립적 테스트 가능

### 영향
- 4계층 구조로 프로젝트 구성
- DDD의 Entity, Repository 패턴 활용

**참고 문서**: [시스템 아키텍처](./02-architecture/01-system-architecture.md), [설계 패턴](./02-architecture/02-design-patterns.md)

---

## 6. 시장 데이터 제공자 선택

**결정일**: 2026년 1월  
**상태**: 확정  
**결정**: 한국투자증권 Open API 사용

### 배경
국내 주식 시장 데이터 제공자 선택 필요

### 고려 사항
- API 안정성
- 데이터 품질
- 비용
- 문서화 수준

### 대안

#### 대안 1: 한국투자증권 Open API (선택)
**장점**:
- 공식 API
- 무료 (계좌 개설 시)
- 실거래 및 모의투자 지원
- 상세한 문서

**단점**:
- Rate Limiting 제약

#### 대안 2: 키움증권 API
- **단점**: ActiveX 기반, Windows 전용

#### 대안 3: 유료 데이터 제공자
- **단점**: 높은 비용

### 결정 이유
1. **비용 효율성**: 무료 사용 가능
2. **공식 지원**: 공식 API로 안정성 보장
3. **문서화**: 상세한 문서 및 MCP 지원

### 영향
- 한국투자증권 Open API 통합
- Rate Limiting 및 Circuit Breaker 적용 필요
- MCP를 통한 개발 지원

**참고 문서**: [한국투자증권 API 가이드](./04-api/09-korea-investment-api-guide.md)

---

## 7. 캐싱 전략

**결정일**: 2026년 1월  
**상태**: 확정  
**결정**: Redis를 사용한 분산 캐싱

### 배경
시장 데이터 조회 성능 향상 및 API 호출 최소화 필요

### 고려 사항
- 캐시 일관성
- 성능
- 비용

### 대안

#### 대안 1: 인메모리 캐싱 (Caffeine)
- **단점**: 서버 재시작 시 데이터 손실, 분산 환경에서 공유 불가

#### 대안 2: Redis (선택)
**장점**:
- 분산 캐싱 지원
- 영속성 옵션
- 다양한 데이터 구조 지원

**단점**:
- 추가 인프라 필요

### 결정 이유
1. **성능**: 빠른 조회 성능
2. **확장성**: 향후 수평 확장 시 캐시 공유 가능
3. **유연성**: 다양한 캐싱 전략 구현 가능

### 영향
- Redis 캐싱 레이어 구현
- TTL 기반 캐시 무효화 전략
- Spring Cache 통합

**참고 문서**: [필수 기술 스펙](./02-architecture/10-essential-tech-spec.md)

---

## 8. 예외 처리 전략

**결정일**: 2026년 1월  
**상태**: 확정  
**결정**: 계층화된 예외 처리 (DomainException, AppException)

### 배경
일관된 예외 처리 및 에러 응답 형식 필요

### 고려 사항
- 에러 추적성
- 사용자 경험
- 보안

### 대안

#### 대안 1: 단일 예외 타입
- **단점**: 예외 구분 어려움

#### 대안 2: 계층화된 예외 (선택)
**구조**:
- DomainException (비즈니스 규칙 위반)
- AppException (시스템 레벨 오류)
- GlobalExceptionHandler (전역 처리)

**장점**:
- 명확한 예외 구분
- 일관된 에러 응답
- 추적 가능한 traceId

### 결정 이유
1. **명확성**: 예외 타입별 명확한 구분
2. **일관성**: 표준화된 에러 응답 형식
3. **추적성**: traceId를 통한 로그 추적

### 영향
- DomainException 및 AppException 클래스 구현
- GlobalExceptionHandler 구현
- 표준화된 ErrorResponse 형식

**참고 문서**: [설계 패턴](./02-architecture/02-design-patterns.md)

---

## 9. API 설계 결정

**결정일**: 2026년 1월  
**상태**: 확정  
**결정**: RESTful API 설계, 버전 관리 (/api/v1)

### 배경
API 설계 표준 수립 필요

### 고려 사항
- 확장성
- 호환성
- 문서화

### 결정 사항

#### 9.1 RESTful API 설계
- REST 원칙 준수
- 리소스 기반 URL 설계
- HTTP 메서드 적절한 사용

#### 9.2 API 버전 관리
- `/api/v1` 접두사 사용
- 향후 breaking change 시 `/api/v2` 도입

#### 9.3 DTO 패턴
- Entity 직접 노출 금지
- Request/Response DTO 분리

### 결정 이유
1. **표준성**: 널리 사용되는 RESTful 설계
2. **확장성**: 버전 관리로 하위 호환성 유지
3. **보안**: Entity 직접 노출 방지

### 영향
- 모든 API는 `/api/v1` 접두사 사용
- DTO를 통한 데이터 전송
- OpenAPI/Swagger 문서화

**참고 문서**: [API 개요](./04-api/01-api-overview.md)

---

## 10. 보안 설계 결정

**결정일**: 2026년 1월  
**상태**: 확정  
**결정**: JWT 기반 인증, 계좌번호 기반 접근 제어

### 배경
금융 데이터 처리 시 보안 필수

### 고려 사항
- 인증/인가
- 데이터 암호화
- 로그 보안

### 결정 사항

#### 10.1 인증
- JWT 토큰 사용
- 짧은 TTL (1시간)
- 재인증 필수 (민감 작업)

#### 10.2 인가
- 계좌번호 기반 접근 제어
- 서버 사이드 검증

#### 10.3 데이터 보안
- 민감 정보 암호화 저장
- 로그 마스킹 (PII)
- HTTPS 필수 (프로덕션)

#### 10.4 API 키 관리
- 환경 변수 또는 시크릿 관리자 사용
- DB에 암호화하여 저장

### 결정 이유
1. **보안**: 금융 데이터 보호 필수
2. **규정 준수**: 개인정보보호법 준수
3. **사용자 경험**: JWT로 무상태 인증

### 영향
- JWT 기반 인증 구현
- 계좌번호 암호화 저장
- 로그 마스킹 적용

**참고 문서**: [보안 리팩토링 가이드](./07-security/01-security-refactoring-guide.md)

---

## 11. 시장 차원(KR/US) 도입

**결정일**: 2026년 1월  
**상태**: 확정  
**결정**: 전략·유니버스·시그널에 **시장(Market)** 차원 도입 — KR(국내 KOSPI/KOSDAQ), US(미국 NYSE/NASDAQ) 구분. 시장별·분류별로 상이한 수학적 모델 적용.

### 배경
자동투자수익 비전 하에 국내·미국 주식을 하나의 인터페이스로 통합 관리하며, 한국/미국 시장의 **미시구조(Micro-structure)** 가 다르므로 파이프라인 내 각 단계에서 시장별로 다른 알고리즘을 적용해야 승산이 있음.

### 결정 사항
- **Market** 값 객체: KR, US.
- **전략**: 계좌 + 시장 + 전략 타입(단/중/장기) 조합으로 유일 키. (기존 계좌 + 전략 타입에서 확장.)
- **유니버스/시그널**: 한국(Sector Relative Strength, 수급 강도, 이격도, 변동성 돌파 등) / 미국(Post-Earnings Drift, 듀얼 모멘텀, 퀄리티-성장, VAA 등) 별도 적용.
- **DB**: TB_STRATEGIES에 MARKET 컬럼 추가, UK를 ACCOUNT_NO + MARKET + STRATEGY_TYPE으로 변경.

### 영향
- 도메인 모델·DB 스키마에 Market 반영. 전략 API에 market 파라미터 확장.
- [자동투자 전략 명세](./02-architecture/12-auto-investment-strategy.md), [도메인 모델](./03-design/01-domain-model.md), [DB 스키마](./05-database/01-database-schema.md) 참조.

---

## 12. 뉴스·공시 파이프라인 채택

**결정일**: 2026년 1월  
**상태**: 확정  
**결정**: 투자 의사결정에 **공시/데이터(Fact)**·**뉴스/속보(Speed)**·**센티멘트/수급(Buzz)** 수집·연동 파이프라인 채택. **확정 원천만** 파이프라인에 연결하여 속도(Latency)·정확도(Accuracy) 확보.

### 배경
공격형 로보어드바이저는 0.1초 단위 정보 해석·주문 실행이 중요하므로, 신뢰할 수 있고 데이터 처리가 용이하며 트래픽이 몰려 시장 방향성을 결정짓는 원천만 사용하기로 함.

### 결정 사항
- **한국**: Fact = DART(Open API), Speed = 연합뉴스(Yonhap), Buzz = 네이버 금융(Naver). DART는 Real-time Push 권장.
- **미국**: Fact = SEC EDGAR(API), Speed = Reuters(로이터), Buzz = Yahoo Finance. 8-K 발생 시 최우선 순위 로직 실행.
- **저장**: TB_NEWS_ITEMS 테이블로 수집 항목 저장. 감정/중요도/이벤트 유형 분석 후 전략 연동(시그널 점수 반영).
- **Fallback**: 원천 미수집·장애 시 해당 입력만 제외하고 파이프라인 중단 없음.

### 영향
- [뉴스·공시 수집·연동 설계](./02-architecture/13-news-collection-design.md), [자동투자 전략 명세 §7](./02-architecture/12-auto-investment-strategy.md#7-공시데이터뉴스센티멘트-원천-확정-파이프라인-연결-소스), [DB 스키마 TB_NEWS_ITEMS](./05-database/01-database-schema.md) 참조.

---

## 13. 로깅 시 민감정보 마스킹 표준화

**결정일**: 2026년 1월  
**상태**: 확정  
**결정**: app key, secret, 계좌번호, userId 등 암호화 저장 항목은 로그 출력 시 반드시 `LogMaskingUtil`을 사용하며, INFO/WARN/ERROR에서는 마스킹된 값만 출력한다.

### 배경
- 보안 규칙상 "Never log secrets, tokens, passwords"를 준수해야 함.
- 여러 클래스에서 개별 마스킹 로직이 중복·불일치할 위험이 있음.

### 결정 내용
- **공통 모듈**: `com.investment.common.security.LogMaskingUtil`만 사용. `maskApiKey`, `maskSecret`, `maskAccountNo`, `maskUserId` 등 제공.
- **INFO/WARN/ERROR**: 민감 값은 마스킹된 값만 로그에 출력.
- **DEBUG**: 로컬 디버깅 시에만 `logWithDebugActual` / `logWithDebugActualAtDebug`로 실제 값 추가 출력 가능.

### 영향
- [보안 설정 참조 - 로깅 시 민감정보 마스킹](./07-security/02-security-configuration-reference.md#로깅-시-민감정보-마스킹-개발-규칙), [개발 진행 현황 - 로그 마스킹 모듈화](./09-planning/02-development-status.md) 참조.

---

## 14. 한국투자증권 API 요청 방식 및 MCP 사용

**결정일**: 2026년 1월  
**상태**: 확정  
**결정**: 한국투자증권 국내주식 **조회** API는 **GET + query parameter**로 호출하며, 한국투자증권 API를 사용하는 모든 개발·수정 시 **한국투자증권 MCP**를 반드시 사용한다.

### 배경
- 주식잔고조회 등 조회 API가 query parameter로 값을 보내야 하는데, 기존 구현이 JSON body로 전송하여 오류가 발생함.
- 한국투자증권 공식 스펙상 조회 API는 GET 메서드에 URI query parameter로 전달됨.
- API별 HTTP 메서드·파라미터 위치(쿼리 vs body)를 MCP로 확인하지 않으면 동일한 형식 오류가 재발할 수 있음.

### 결정 사항
- **조회 API 요청 방식**: 계좌 조회(주식잔고조회, 매수가능·매도가능·주문체결·자산현황·기간별손익) 및 시세 조회(현재가·차트)는 **GET** + **URI query parameter**로 호출한다. 주문 실행·토큰 발급은 POST+JSON body 유지.
- **개발 시 MCP 필수**: 한국투자증권 API를 추가·수정할 때마다 **한국투자증권 MCP**로 해당 엔드포인트의 HTTP 메서드·파라미터 방식을 확인한 뒤 구현한다. MCP 확인 없이 요청 형식을 가정하여 구현하지 않는다.
- **토큰 사용 정책**: 접근 토큰은 1회 발급 후 DB에 저장하고, 이후 모든 API 호출은 `getAccessToken(userId, serverType)`으로 DB에서만 조회한다. 클라이언트(Account/MarketData 등)는 `issueTokenForUser`를 호출하지 않으며, 401 시에도 재발급이 아니라 getAccessToken 재조회 후 1회 재시도만 한다.
- **문서 동기화**: 작업 중 API·설계·결정이 바뀌면 관련 문서(API 가이드, development-status, decisions 등)를 반드시 갱신한다.

### 영향
- [KoreaInvestmentAccountClient](src/main/java/com/investment/account/client/KoreaInvestmentAccountClient.java): 7곳 GET+queryParam 적용.
- [KoreaInvestmentMarketDataClient](src/main/java/com/investment/marketdata/client/impl/KoreaInvestmentMarketDataClient.java): 현재가·차트 조회 GET+query 적용.
- [한국투자증권 API 가이드](./04-api/09-korea-investment-api-guide.md): 조회 API GET·query parameter 명시, 개발 시 MCP 사용 안내.
- [.cursor/rules/MCP.mdc](../.cursor/rules/MCP.mdc): 한국투자증권 API 개발 시 MCP 무조건 사용 규칙.

### 한국투자증권 API 변경 대응
- **2026-02-11**: 주식잔고조회(v1_국내주식-006) INQR_DVSN 02(종목별) 제한 → 01(대출일별) 사용. 공지에 따라 `KoreaInvestmentAccountClient.inquireBalance`, `verifyAccountByCredentials` 및 API 가이드 예시를 01로 수정. 상세: [09-korea-investment-api-guide.md §주식잔고조회](./04-api/09-korea-investment-api-guide.md).

---

## 15. TimescaleDB 전환

**결정일**: 2026년 2월  
**상태**: 확정  
**결정**: MariaDB에서 TimescaleDB(PostgreSQL 기반)로 전환

### 배경
기관급 퀀트 엔진 개편안에서 시계열 데이터(일봉·팩터·시그널) 적재·조회 효율과 데이터 정합성 강화를 위해 시계열 DB 도입이 권장됨.

### 결정 사항
- **DB**: TimescaleDB (`timescale/timescaledb:latest-pg16`) 사용. Docker Compose에서 MariaDB 서비스를 TimescaleDB로 교체.
- **드라이버·다이얼렉트**: `org.postgresql.Driver`, `PostgreSQLDialect`. Flyway 마이그레이션 V21~V24는 PostgreSQL 문법으로 변환.
- **초기 스키마**: 신규 환경은 `spring.profiles.active=local,init-db` 1회 실행( Hibernate ddl-auto=update ) 후 일반 프로파일로 전환.

### 영향
- docker-compose: investment-infra 레포의 docker-compose.local.yml 사용. TimescaleDB 포트 5432, Redis, 볼륨 timescaledb_data.
- [application*.yml](src/main/resources/): driver, url, dialect → PostgreSQL.
- [build.gradle](build.gradle): mariadb·flyway-mysql 제거, postgresql 의존성 추가.
- [db/migration](src/main/resources/db/migration/): V21~V24 PostgreSQL 호환 DDL.

---

## 16. Alpha-Risk-Execution 분리 및 Portfolio/Risk 레이어 도입

**결정일**: 2026년 2월  
**상태**: 확정  
**결정**: 기관급 퀀트 엔진 구조(Alpha - Risk - Execution) 분리 및 Portfolio Construction·PreTrade Compliance 레이어 도입

### 배경
기존 OrderService 내 검증만으로는 "의사결정 깊이"와 "데이터 정합성"이 부족하다는 개편안에 따라, 전략(Alpha)·리스크(Compliance)·집행(Execution)을 명시적으로 분리하고 포트폴리오 최적화 레이어를 추가한다.

### 결정 사항
- **Alpha**: `core.engine.alpha.AlphaEngine` — 전략 시그널 생성 진입점. Phase 1에서 StrategyService 위임.
- **Portfolio**: `core.engine.portfolio` — TaxAwareOptimizer(세금·비용 반영), Rebalancer(목표 비중 대비 매매 리스트). Phase 1 스텁, Phase 2 구현.
- **Risk**: `core.engine.risk.ComplianceEngine` — 주문 직전 PreTrade 검사(개별 종목 비중 상한, MDD 게이트, Kill Switch). Phase 1 스텁, Phase 2 구현.
- **Execution**: `core.engine.execution.ExecutionGateway` — 주문 집행·한투 국내/해외 라우팅. Phase 1에서 OrderService 위임.
- **Data Pipeline**: `core.pipeline.DataPipelineService` — 데이터 수집·정제 오케스트레이션 진입점. 수정주가 정책 명시.

### 영향
- [01-system-architecture.md](./02-architecture/01-system-architecture.md) §9 개편안 반영.
- 신규 패키지: core.engine.alpha, core.engine.portfolio, core.engine.risk, core.engine.execution, core.pipeline.

---

## 17. 관리자 계정 생성·로그인 정책

**결정일**: 2026년 2월  
**상태**: 확정  
**결정**: 관리자 계정은 공개 회원가입으로 생성하지 않음. 최초는 부트스트랩/시드, 추가는 ADMIN 전용 API 또는 시드.

### 배경
상용 시스템(OWASP, Keycloak, Laravel 등) 관행에 맞춰 관리자 계정을 안전하게 운용해야 함. 누구나 관리자로 가입할 수 있으면 보안 위험.

### 결정 사항
- **관리자 회원가입**: 공개 API로 제공하지 않음. 일반 회원가입(`POST /api/v1/auth/signup`)은 계속 일반 사용자(User)만 생성.
- **최초 관리자**: (1) Flyway/시드로 지정 계정(예: yoon) ROLE=Admin 반영, (2) 부트스트랩: `investment.security.bootstrap-admin.enabled=true` 및 username/password 설정 시 ADMIN 0건일 때만 1회 User(role=Admin) 생성.
- **추가 관리자**: 기존 ADMIN만 호출 가능한 `POST /api/v1/admin/users` (body: username, password, role=Admin). 또는 DB 시드/수동 부여.
- **관리자 로그인**: 기존 `POST /api/v1/auth/login` 그대로 사용. DB의 role로 구분.
- **인가**: JWT 인증 시 DB에서 User 조회 후 `User.getRole()`을 Spring Security 권한(ROLE_USER/ROLE_ADMIN)으로 매핑하여 SecurityContext에 반영. `@PreAuthorize("hasRole('ADMIN')")` 등이 정상 동작.

### 영향
- JwtAuthenticationFilter: User 조회 후 Role.fromDbRole(user.getRole())로 권한 설정.
- Role enum: USER, ADMIN. DB role 문자열 "User", "Admin" 매핑.
- AdminUserController: POST /api/v1/admin/users, @PreAuthorize("hasRole('ADMIN')").
- application.yml: investment.security.bootstrap-admin.*, investment.security.super-admin.* (슈퍼관리자 yoon 비밀번호 동기화용).

**참고**: [개발 진행 현황](./09-planning/02-development-status.md), [역할·권한](09-planning/03-figma-wireframes/02-roles-and-permissions.md).

---

## 18. KIS Open API 실전 구축 (토큰 장전·주문 큐·순위/투자자·WebSocket)

**결정일**: 2026-02-11  
**상태**: 확정  
**결정**: 한국투자증권 API 실전 운용을 위해 토큰 장전 갱신·주문 Throttling/큐·순위·투자자 REST·WebSocket 스켈레톤을 도입한다.

### 배경
실전/모의 환경에서 API 제한 준수·당일 거래 안정성·퀀트 스코어링(유니버스·수급)·실시간 시세/체결 연동이 필요하다.

### 결정 사항
- **토큰 장전 갱신**: 장 시작 30분 전(기본 08:30 KST, cron) 전 사용자·모의/실 serverType별 Access Token 강제 갱신. `TokenRefreshScheduler`, `KoreaInvestmentTokenService.forceRefreshAllTokensForMarketOpen()`.
- **주문 큐·Throttling**: 주문 경로 앞단에 인메모리 `OrderRequestQueue`(BlockingQueue + 단일 소비자 + Resilience4j RateLimiter). 설정: throttle.enabled, orders-per-second, queue-max-size, reject-when-full.
- **순위·투자자 API**: `KoreaInvestmentRankClient`(거래량 순위, 시장별 투자자 매매동향 일별). path/TR_ID는 MCP(volume_rank, inquire_investor_daily_by_market) 확인 후 application.yml rankApi에 설정. **실전 후속(2026-02-11)**: 기본 path·TR_ID를 application.yml에 반영(거래량순위 `/uapi/domestic-stock/v1/quotations/volume-rank`, FHKST01710000/FHPST01710000; 시장별 투자자 `/uapi/domestic-stock/v1/quotations/inquire-investor-daily-by-market`, FHKST03010100/FHPST03010100). 유니버스 연동: `UniverseFilterService`에서 `investment.factor.volume-rank-enabled`, `volume-rank-user-id` 설정 시 KR 유니버스에 거래량 순위 교집합 적용.
- **WebSocket**: `KoreaInvestmentWebSocketClient` 인터페이스(실시간 호가·체결통보 구독/해제/연결). `NoOpKoreaInvestmentWebSocketClient`(enabled=false), `KoreaInvestmentWebSocketClientImpl`(enabled=true). MCP asking_price_total·ccnl_notice 반영. path·quote-tr-id·ccnl-notice-tr-id·approval-key(선택) 설정. **실전 후속(2026-02-11)**: approval_key REST 발급 연동. `KoreaInvestmentTokenClient.getApprovalKey(accessToken, serverType)`(POST /oauth2/Approval), `KoreaInvestmentTokenService.getApprovalKey(userId, serverType)`. WebSocket Impl에서 `approval-key-fetch-enabled=true` 시 연결 시 REST로 발급 후 구독 메시지에 사용. 상세: 09-korea-investment-api-guide.md §WebSocket approval_key 발급.

### 제한 사항
- 실전·모의 동시 사용 시 TR_ID는 환경별로 다르므로(실전 FHPST*, 모의 FHKST*) 환경변수 등으로 분리 설정 권장.
- WebSocket approval_key REST 응답 포맷은 포털 문서 기준이며, 미지원 시 설정에서 직접 입력.

### 영향
- MarketDataProperties: token.pre-market-refresh-*, throttle.*, rankApi.*, websocket.*(approvalKeyFetchEnabled 추가)
- OrderService: OrderRequestQueue 주입·enabled 시 submit 경로 사용
- UniverseFilterService: 선택적 KoreaInvestmentRankClient·volume-rank-enabled·volume-rank-user-id
- 09-korea-investment-api-guide.md 실전 구축·순위/투자자 path·TR_ID·approval_key 발급, decisions.md 본 ADR

**참고**: [09-korea-investment-api-guide.md §실전 구축](./04-api/09-korea-investment-api-guide.md), [02-development-status.md](./09-planning/02-development-status.md).

---

## 19. 데이터 정합성 (수정주가)

**결정일**: 2026-02-11  
**상태**: 확정  
**결정**: 백테스트·팩터 계산은 **수정주가(adjusted price)** 만 사용한다. 원주가는 차트 표시 등에만 사용 가능하다.

### 배경
원주가로 백테스트하면 주식 분할·배당 시 수익률·MDD가 왜곡되어 결과가 무효가 된다. [minimum-architecture-requirement.md](./01-requirements/minimum-architecture-requirement.md)에서 "Raw prices destroy backtests"로 명시되어 있다.

### 결정 사항
- **일봉 저장·팩터 계산·백테스트 입력**: 수정주가만 사용. 한투 API 사용 시 `FID_ORG_ADJ_PRC=0`(수정주가) 사용.
- **원주가**: 차트 표시·UI용으로만 사용 가능. 전략 검증·시그널 계산에는 사용하지 않는다.

### 영향
- [00-strategy-registry.md](./02-architecture/00-strategy-registry.md) §1.1, [02-development-status.md](./09-planning/02-development-status.md) 수정주가 파이프라인 필수 반영, [roadmap.md](./roadmap.md) Phase 5.2.

---

## 20. Point-in-Time 및 Look-ahead 방지

**결정일**: 2026-02-11  
**상태**: 확정  
**결정**: 시그널·백테스트는 **해당 일자(bas_dt) 시점까지 가용한 데이터만** 사용한다. Look-ahead(미래 정보 유입) 금지.

### 배경
백테스트에서 미래 정보가 섞이면 수익률이 과대평가되어 실전과 괴리가 발생한다. 퀀트 시스템 규칙상 "백테스트는 해당 일자 당시 가용 정보만 사용"해야 한다.

### 결정 사항
- **Point-in-Time (PIT)**: 시그널 계산·백테스트 시 bas_dt 종료 시점까지 공개된 데이터만 사용.
- **Look-ahead bias 방지**: 당일 종가로 진입/청산 판단 시, 당일 종가는 해당 일자 백테스트 루프에서 시뮬레이션 종료 후에만 사용.

### 영향
- [00-strategy-registry.md](./02-architecture/00-strategy-registry.md) §1.1, [02-development-status.md](./09-planning/02-development-status.md) PIT 정책 반영.

---

## 21. 전략 거버넌스·중단 원칙

**결정일**: 2026-02-11  
**상태**: 확정  
**결정**: 전략이 더 이상 말이 안 되면 **즉시 거래 중단**한다. 전략 버전 스택에 결과·교훈을 채우고, 성과 열화 시 검토·거래 중단 여부를 결정한다.

### 배경
퀀트 시스템 규칙([Quant-Trading-System.mdc](../.cursor/rules/Quant-Trading-System.mdc)): "If a strategy stops making sense, it stops trading—immediately." 버전 스택의 "결과·교훈"이 대부분 "미검증"인 상태에서는 운영 원칙이 문서화되어 있지 않았다.

### 결정 사항
- **전략 중단 원칙**: 전략이 말이 안 되면 즉시 거래 중단. 정기 백테스트 재실행·MDD/Sharpe 열화 시 검토 후 거래 중단 여부 결정.
- **버전 스택**: 전략·팩터 변경 시 [00-strategy-registry.md](./02-architecture/00-strategy-registry.md) 버전 스택에 결과·교훈을 기입해 실패 사례를 참고할 수 있게 한다.

### 영향
- [00-strategy-registry.md](./02-architecture/00-strategy-registry.md) §1.1, [12-auto-investment-strategy.md](./02-architecture/12-auto-investment-strategy.md) §6.2 체크리스트 항목 13, [02-development-status.md](./09-planning/02-development-status.md) 진행예정.

---

## 22. 파이프라인 실행 플래그 계정별 저장

**결정일**: 2026년 2월  
**상태**: 확정  
**결정**: PIPELINE_AUTO_EXECUTE·PIPELINE_ALLOW_REAL_EXECUTION을 계정(계좌) 단위로 TB_TRADING_SETTINGS에 저장하고, 스케줄러/실행기는 계정별 값을 우선·서버 기본값 fallback으로 사용한다.

### 배경
기존에는 `investment.pipeline.auto-execute`, `investment.pipeline.allow-real-execution` 서버 전역 설정만 있어 사용자별로 "이 계좌는 자동 실행 허용/실계좌 주문 허용"을 선택할 수 없었다. 설정 화면에서 사용자가 계정별로 선택·저장할 수 있도록 요구됨.

### 결정 사항
- TB_TRADING_SETTINGS에 PIPELINE_AUTO_EXECUTE·PIPELINE_ALLOW_REAL_EXECUTION 컬럼 추가(Flyway V33). NULL = 서버 기본값 사용.
- PipelineExecutionScheduler: 자동투자 ON 계좌에 대해 계정별 pipelineAutoExecute가 null이면 서버 autoExecute 사용. effective가 true인 계좌만 실제 실행(dryRun=false).
- PipelineExecutor: 실계좌(serverType=0) 주문 시 계정별 pipelineAllowRealExecution이 null이면 서버 allowRealExecution 사용.
- GET/PUT `/api/v1/settings/{accountNo}` 요청/응답에 두 필드 포함.

### 영향
- [06-setting-api.md](./04-api/06-setting-api.md), [02-development-status.md](./09-planning/02-development-status.md), [02-api-endpoints.md](./04-api/02-api-endpoints.md) 반영. 프론트 설정 화면에서 "파이프라인 실행 설정" 카드로 편집·저장.

---

## 23. 시스템 표준 시간 Asia/Seoul

**결정일**: 2026-02-24  
**상태**: 확정  
**결정**: 시스템 전체(백엔드·DB·컨테이너) 표준 시간을 **Asia/Seoul**로 통일

### 배경
로그·스케줄·DB 타임스탬프·배치 실행 시간 등이 서버/컨테이너 기본 UTC로 처리되면 한국 사용자·운영 관점에서 해석이 어렵고, 배치 크론도 Asia/Seoul 기준으로 이미 등록되어 있어 일관성을 위해 전 계층을 Asia/Seoul로 맞추기로 함.

### 결정 사항
- **백엔드(Spring Boot)**  
  - JVM: `-Duser.timezone=Asia/Seoul` (Gradle `bootRun`·Dockerfile ENTRYPOINT).  
  - JPA/Hibernate: `spring.jpa.properties.hibernate.jdbc.time_zone: Asia/Seoul` (application.yml).
- **DB(TimescaleDB/PostgreSQL)**  
  - 컨테이너: `TZ=Asia/Seoul`, `PGTZ=Asia/Seoul`.  
  - init 스크립트: `docker/timescaledb/init/02_timezone_asia_seoul.sql`에서 `SET timezone = 'Asia/Seoul';`.
- **Docker**  
  - 모든 compose 서비스(backend, frontend, nginx, redis, timescaledb, prediction-service, data-collector)에 `environment.TZ: Asia/Seoul` 설정.  
  - 모든 Dockerfile에 `ENV TZ=Asia/Seoul` 및 백엔드 이미지는 `ENTRYPOINT`에 `-Duser.timezone=Asia/Seoul` 포함.

### 영향
- 로그·배치·API 응답의 날짜/시간이 모두 Asia/Seoul 기준으로 해석·표시됨.  
- 인프라: `investment-infra` docker-compose 전 파일 및 각 서비스 Dockerfile 반영.

---

## 24. 전략 비중 동적 결정

**결정일**: 2026-02-24  
**상태**: 확정  
**결정**: 단기/중기/장기 전략 비중을 시장 레짐에 따라 동적으로 결정하며, 레짐별 목표 비중은 설정(application.yml)으로 관리한다.

### 배경
파이프라인 실행 시 총 자본을 단기/중기/장기로 나누는 비중이 기존에는 계정 설정 또는 고정 기본값(0.2/0.4/0.4)이었음. 퀀트 관점에서 고변동성·스트레스 시 단기 비중 축소·장기 비중 확대, 저변동·추세 구간에서 단기/중기 확대가 바람직함.

### 결정
- **StrategyWeightResolver**: 파이프라인 실행 시점에 `MacroEconomicStrategyEngine.decideStrategy(indicators)`로 레짐 판별 후, `StrategyWeightProperties`의 레짐별 목표 비중을 조회·클리핑·정규화하여 (shortPct, midPct, longPct) 합=1 반환.
- **설정 외부화**: `investment.trading.strategy-weights` 하위에 `enabled`, `min-weight`, `max-weight`, `regime-weights.<레짐명>.short-pct/mid-pct/long-pct`로 튜닝 가능.
- **하위 호환**: 지표 없음·예외·비활성화 시 설정 비중 또는 (0.2, 0.4, 0.4) fallback.

### 결과
- `PipelineExecutionScheduler`에서 Resolver 호출 후 반환 비중으로 자본 배분. 기존 `sizeMultiplier`(리스크 게이트)는 유지.
- 전략 레지스트리(00-strategy-registry.md)에 레짐별 테이블·설정 경로·변경 이력 반영.

---

## 25. 국내/미국 전략 시스템 기본화 및 조회 중심

**결정일**: 2026-02-24  
**상태**: 확정  
**결정**: 국내/미국 전략 화면은 사용자가 전략을 "등록"하는 것이 아니라, 시스템이 적용한 기본 전략(단기/중기/장기 3건)을 **조회·상태 변경**만 하도록 한다. 사용자 전략 등록은 필수가 아니다.

### 배경
자동 투자 프로그램에서 국내·미국 전략을 사용자가 직접 추가하는 UX는 "자동화" 정체성과 맞지 않으며, 사용자 개입을 최소화하고 시스템이 기본 전략을 갖추어 조회·활성/중지만 선택하게 하는 것이 적합하다는 요구가 있었다.

### 결정
- **시스템 기본 전략 보장**: `GET /api/v1/strategies?accountNo=...&market=KR|US` 호출 시 해당 계좌+시장에 전략이 없으면 백엔드에서 단기/중기/장기 3건을 시스템 기본값으로 자동 생성(ensure on read) 후 반환. TradingSetting의 비율·최대 투자금 등으로 기본값 산출.
- **파이프라인과 전략 상태 연동**: 파이프라인 실행 시 (accountNo, market, strategyType)별 Strategy를 조회하고, status가 STOPPED 또는 PAUSED이면 해당 run 스킵. UI에서 "중지"한 전략은 실제로 파이프라인에서 실행되지 않음.
- **프론트**: "전략 추가" 버튼 제거. 빈 상태 문구를 "시스템이 적용한 단기/중기/장기 전략을 조회합니다. 활성/중지는 각 전략 카드에서 변경할 수 있습니다."로 변경. 편집은 기존 전략 카드에서 제한된 필드(예: 최대/최소 금액)만 허용 가능.

### 영향
- 화면·메뉴 기획서(01-screen-menu-spec.md) §3.3·§3.4 목적·API 설명 갱신.
- StrategyManagementService ensure 로직, PipelineExecutionScheduler Strategy status 체크, 프론트 Investment.tsx 전략 블록 수정.

---

## 참고 문서

- [시스템 아키텍처](./02-architecture/01-system-architecture.md)
- [설계 패턴 및 원칙](./02-architecture/02-design-patterns.md)
- [필수 기술 스펙](./02-architecture/10-essential-tech-spec.md)
- [PRD](./PRD.md)
- [로드맵](./roadmap.md)

## 문서 변경 이력

| 버전 | 일자 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-29 | System | 초기 결정 사항 문서 작성 - 아키텍처 문서에서 주요 결정 사항 추출 |
| 1.1 | 2026-01-29 | System | ADR 11 시장 차원(KR/US) 도입, ADR 12 뉴스·공시 파이프라인 채택 추가 |
| 1.2 | 2026-01-29 | System | ADR 13 로깅 시 민감정보 마스킹 표준화(LogMaskingUtil) 추가 |
| 1.3 | 2026-01-30 | System | ADR 14 한국투자증권 API 요청 방식(GET+query) 및 MCP 필수 사용, 작업 중 문서 업데이트 규칙 반영 |
| 1.4 | 2026-02-06 | System | ADR 15 TimescaleDB 전환, ADR 16 Alpha-Risk-Execution·Portfolio/Risk 레이어 도입 |
| 1.5 | 2026-02-06 | System | ADR 17 관리자 계정 생성·로그인 정책 (공개 회원가입 없음, 부트스트랩·ADMIN 전용 API·JWT role 반영) |
| 1.6 | 2026-02-11 | System | ADR 18 KIS Open API 실전 구축 (토큰 장전 갱신·주문 큐·순위/투자자 API·WebSocket 스켈레톤) |
| 1.7 | 2026-02-11 | System | 한국투자증권 API 변경 대응: 주식잔고조회 INQR_DVSN 02 제한 → 01(대출일별) 사용 (ADR 14 하위) |
| 1.8 | 2026-02-11 | System | ADR 19 데이터 정합성(수정주가), ADR 20 Point-in-Time·Look-ahead 방지, ADR 21 전략 거버넌스·중단 원칙 추가 (기획 고도화 퀀트 관점) |
| 1.9 | 2026-02-24 | System | ADR 22 파이프라인 실행 플래그 계정별 저장 (TB_TRADING_SETTINGS, 설정 API·스케줄러/실행기) |
| 1.10 | 2026-02-24 | System | ADR 23 시스템 표준 시간 Asia/Seoul (JVM·JPA·DB·Docker 전 계층) |
| 1.11 | 2026-02-24 | System | ADR 24 전략 비중 동적 결정 (레짐별 목표 비중, StrategyWeightResolver·설정 외부화) |
| 1.12 | 2026-02-24 | System | ADR 25 국내/미국 전략 시스템 기본화·조회 중심 (ensure on read, 파이프라인 STOPPED 스킵, 전략 추가 버튼 제거) |
