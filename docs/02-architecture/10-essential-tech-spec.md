# 필수 기술 스펙 및 개발 계획

## 1. 프로젝트 구조 개요

### 1.1 모노리프 vs 마이크로서비스

**현재 구조: 모노리프 (단일 프로젝트)**

```
investment-choi/
├── src/main/java/          # Spring Boot 애플리케이션 (Java)
├── src/main/resources/     # 설정 파일
├── ai-service/             # Python FastAPI 서비스 (별도 디렉토리, 같은 프로젝트)
│   ├── prediction-service/  # 예측 서비스
│   └── requirements.txt
└── docs/                    # 문서
```

**답변: 프로젝트 하나 안에 개발 가능합니다!**

- ✅ **Spring Boot 애플리케이션**: 모노리프로 모든 백엔드 기능 개발
- ✅ **Python AI 서비스**: 같은 프로젝트 내 별도 디렉토리로 관리
- ✅ **배포**: 하나의 서버에 모든 서비스 배포 가능 (최소 비용 구성)

### 1.2 아키텍처 다이어그램

```
┌─────────────────────────────────────────────────────────┐
│              단일 서버 (월 $50-100)                       │
│                                                           │
│  ┌─────────────────────────────────────────────────┐   │
│  │  Spring Boot Application (포트 8080)              │   │
│  │  - REST API                                      │   │
│  │  - Thymeleaf (웹 UI)                             │   │
│  │  - 비즈니스 로직                                  │   │
│  └─────────────────────────────────────────────────┘   │
│                          │                               │
│        ┌─────────────────┼─────────────────┐           │
│        ▼                 ▼                 ▼           │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐         │
│  │ MariaDB  │    │  Redis   │    │ FastAPI  │         │
│  │ (로컬)   │    │ (로컬)   │    │ (포트8000)│         │
│  └──────────┘    └──────────┘    └──────────┘         │
│                                                           │
└─────────────────────────────────────────────────────────┘
```

## 2. 필수 기술 스펙

### 2.1 Backend (Spring Boot)

| 항목 | 버전/기술 | 상태 | 비고 |
|------|----------|------|------|
| Java | 17 (SE) | ✅ 완료 | Java 11 → 17 업그레이드 완료 |
| Spring Boot | 3.2.2 | ✅ 완료 | Spring Boot 2.7 → 3.2 업그레이드 완료 |
| Spring Data JPA | 3.2.x | ✅ 완료 | MariaDB 연동 |
| Spring Cache | 3.2.x | ✅ 설정 완료 | Redis 연동 필요 |
| Spring WebFlux | 3.2.x | ✅ 의존성 추가 | 비동기 처리 |
| Resilience4j | 2.1.0 | ⏳ 구현 필요 | Circuit Breaker |
| SpringDoc OpenAPI | 2.3.0 | ⏳ 설정 필요 | Swagger UI |

### 2.2 Database & Cache

| 항목 | 버전/기술 | 상태 | 비고 |
|------|----------|------|------|
| MariaDB | 11.8.5+ | ✅ 설정 완료 | 로컬/프로덕션 |
| Redis | 최신 | ⏳ 설정 필요 | 캐싱 및 실시간 데이터 |
| HikariCP | 5.x | ✅ 설정 완료 | 커넥션 풀 |

### 2.3 AI/ML 서비스 (Python)

| 항목 | 버전/기술 | 상태 | 비고 |
|------|----------|------|------|
| Python | 3.11+ | ✅ 완료 | FastAPI 서비스 (실제 경로: `investment-prediction-service/`) |
| FastAPI | 최신 | ✅ 완료 | 비동기 API 서버, /api/v1/predict, /api/v1/health |
| PyTorch | 최신 (CPU) | ✅ 완료 | LSTM 모델 (조건부 추론) |
| NumPy/Pandas | 최신 | ✅ 완료 | 데이터 처리·전처리 |

### 2.4 Frontend

| 항목 | 버전/기술 | 상태 | 비고 |
|------|----------|------|------|
| Thymeleaf | 3.1.x | ✅ 설정 완료 | 서버 사이드 렌더링 |
| JavaScript | ES5 | ✅ 사용 중 | 클라이언트 스크립트 |

### 2.5 인프라 & 도구

| 항목 | 버전/기술 | 상태 | 비고 |
|------|----------|------|------|
| Gradle | 8.x | ✅ 설정 완료 | 빌드 도구 |
| Docker | 최신 | ⏳ 설정 필요 | 컨테이너화 (선택) |
| Prometheus | 최신 | ⏳ 설정 필요 | 메트릭 수집 (선택) |

## 3. 필수 기능 구현 우선순위

### Phase 1: 기반 인프라 (1-2주)

#### 1.1 Redis 캐싱 레이어 구현
- [ ] Redis 설정 및 연결 테스트
- [ ] `@Cacheable` 어노테이션 적용
  - 시장 데이터 캐싱 (5분 TTL)
  - 종목 분석 결과 캐싱 (10분 TTL)
  - 계좌 정보 캐싱 (1분 TTL)
- [ ] 캐시 무효화 전략 구현

#### 1.2 OpenAPI/Swagger 문서화
- [ ] SpringDoc 설정
- [ ] API 엔드포인트 문서화
- [ ] DTO 스키마 정의
- [ ] 예제 요청/응답 추가

#### 1.3 Circuit Breaker 패턴 적용
- [ ] Resilience4j 설정
- [ ] 외부 API 호출에 Circuit Breaker 적용
  - 시장 데이터 API (한국투자증권)
  - AI 예측 서비스
- [ ] Fallback 전략 구현

### Phase 2: AI 서비스 통합 (2-3주)

#### 2.1 AI 서비스 클라이언트 완성
- [ ] `AiPredictionClient` 인터페이스 구현
- [ ] `FastApiPredictionClient` 완성
- [ ] 재시도 로직 구현
- [ ] 타임아웃 처리

#### 2.2 Python FastAPI 서비스 기본 구조
- [ ] `ai-service/prediction-service/` 디렉토리 생성
- [ ] FastAPI 기본 구조 구현
- [ ] Health check 엔드포인트
- [ ] 간단한 예측 API (Mock 데이터)
- [ ] Dockerfile 작성 (선택)

#### 2.3 LSTM 예측 모델 (초기 버전)
- [ ] 데이터 수집 및 전처리
- [ ] 간단한 LSTM 모델 개발
- [ ] 모델 학습 파이프라인
- [ ] 모델 서빙 API 구현

### Phase 3: 핵심 기능 강화 (2-3주)

#### 3.1 시장 데이터 처리 개선
- [ ] Redis 캐싱 적용
- [ ] Circuit Breaker 적용
- [ ] 에러 핸들링 강화

#### 3.2 종목 분석 서비스 개선
- [ ] AI 예측 결과 통합
- [ ] 기술적 지표 계산 최적화
- [ ] 분석 결과 캐싱

#### 3.3 전략 실행 엔진 개선
- [ ] 실시간 데이터 기반 의사결정
- [ ] 리스크 관리 강화
- [ ] 로깅 및 모니터링

### Phase 4: 테스트 및 안정화 (1-2주)

#### 4.1 테스트 커버리지 향상
- [ ] 단위 테스트 작성 (목표: 80% 라인 커버리지)
- [ ] 통합 테스트 작성
- [ ] API 테스트 작성

#### 4.2 성능 최적화
- [ ] 쿼리 최적화
- [ ] 캐싱 전략 최적화
- [ ] 비동기 처리 적용

## 4. 프로젝트 디렉토리 구조

```
investment-choi/
├── src/
│   ├── main/
│   │   ├── java/com/investment/
│   │   │   ├── api/controller/        # REST API
│   │   │   ├── web/controller/         # Thymeleaf 컨트롤러
│   │   │   ├── account/                # 계좌 관리
│   │   │   ├── order/                  # 주문 관리
│   │   │   ├── analysis/               # 종목 분석
│   │   │   ├── strategy/               # 전략 관리
│   │   │   ├── ai/                     # AI 서비스 클라이언트
│   │   │   ├── marketdata/             # 시장 데이터
│   │   │   ├── domain/                 # 도메인 엔티티
│   │   │   └── common/                 # 공통 (예외 처리 등)
│   │   └── resources/
│   │       ├── application.yml
│   │       └── templates/              # Thymeleaf 템플릿
│   └── test/
├── ai-service/                         # Python AI 서비스 (새로 생성)
│   ├── prediction-service/
│   │   ├── app/
│   │   │   ├── main.py                 # FastAPI 앱
│   │   │   ├── models/                 # ML 모델
│   │   │   ├── services/               # 비즈니스 로직
│   │   │   └── api/                    # API 엔드포인트
│   │   ├── requirements.txt
│   │   ├── Dockerfile
│   │   └── README.md
│   └── README.md
├── docs/                                # 문서
├── build.gradle
└── README.md
```

**실제 구조 (참고)**: Python 예측 서비스는 모노리프 내부가 아니라 **프로젝트 루트와 형제 디렉터리** `investment-prediction-service/` 로 두고 있음.

```
investment-prediction-service/
├── app/
│   ├── main.py                 # FastAPI 앱 진입점
│   ├── api/routes.py           # /api/v1/predict, /api/v1/health
│   ├── models/                 # LSTM 등 ML 모델
│   ├── services/               # predictor 등 비즈니스 로직
│   ├── preprocessing/          # 전처리
│   └── data/                   # 데이터 로더
├── requirements.txt
├── Dockerfile
└── README.md
```

## 5. 개발 환경 설정

### 5.1 로컬 개발 환경

#### 필수 설치 항목
1. **Java 17** (JDK)
2. **Gradle 8.x**
3. **MariaDB 11.8.5+** (또는 Docker)
4. **Redis** (또는 Docker)
5. **Python 3.11+** (AI 서비스용)

#### Docker Compose (선택, 로컬 개발용)

```yaml
version: '3.8'
services:
  mariadb:
    image: mariadb:11.8.5
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: investment
      MYSQL_USER: investment
      MYSQL_PASSWORD: password
  
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
```

### 5.2 프로파일 설정

- **local**: 로컬 개발 환경
- **dev**: 개발 서버
- **prod**: 프로덕션 서버

## 6. 배포 전략

### 6.1 단일 서버 배포 (최소 비용)

하나의 서버에 모든 서비스를 배포:

1. **MariaDB**: 로컬 설치
2. **Redis**: 로컬 설치
3. **Spring Boot**: systemd 서비스로 실행
4. **Python FastAPI**: systemd 서비스로 실행 (포트 8000)

### 6.2 Docker 배포 (선택)

각 서비스를 Docker 컨테이너로 실행:

```bash
# Spring Boot
docker run -d -p 8080:8080 investment-choi:latest

# FastAPI
docker run -d -p 8000:8000 ai-prediction-service:latest
```

## 7. 다음 단계

### 즉시 시작 가능한 작업

1. ✅ **Redis 캐싱 레이어 구현** (1일)
2. ✅ **OpenAPI/Swagger 설정** (반일)
3. ✅ **Circuit Breaker 패턴 적용** (1일)
4. ✅ **AI 서비스 클라이언트 완성** (1일)
5. ✅ **Python FastAPI 기본 구조 생성** (1일)

### 개발 시작 체크리스트

- [ ] 로컬 MariaDB 설정 및 연결 테스트
- [ ] 로컬 Redis 설정 및 연결 테스트
- [ ] Spring Boot 애플리케이션 실행 확인
- [ ] 기본 API 엔드포인트 테스트
- [ ] Python 환경 설정 (가상환경)

## 8. 참고 문서

- [시스템 아키텍처](./01-system-architecture.md)
- [구현 계획](./04-implementation-plan.md)
- [최소 비용 구성](../06-deployment/04-minimal-cost-setup.md)
- [서버 스펙](../06-deployment/03-server-specification.md)

## 문서 변경 이력

| 버전 | 일자 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-28 | System | 문서 정리 및 구조화 |
