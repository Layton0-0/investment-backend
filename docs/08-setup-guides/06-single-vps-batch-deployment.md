# 단일 VPS·배치·배포 절차

**멀티 VPS(Oracle Cloud 2대 + AWS Free Tier 1대) 구성**은 [06-deployment/05-multi-vps-oracle-aws-cicd.md](../06-deployment/05-multi-vps-oracle-aws-cicd.md) 참조.

## 개요

이 문서는 **단일 VPS(Fat VPS)** 환경에서 Investment Backend를 배포하고, Spring Batch 스케줄 작업을 안정적으로 실행·모니터링·복구하기 위한 절차를 정리한다. Kubernetes 미사용, Cron 대신 **Spring Batch + BatchJobScheduler** 기반 스케줄을 전제로 한다.

**참조**: [로드맵 Phase 4](../roadmap.md), [기획·개발 기준](../01-requirements/00-planning-basis.md), [로컬 설정 가이드](01-local-setup-complete.md).

---

## 1. 배포 전제

- **서버**: 단일 VPS (Linux 권장). 메모리·CPU는 동시 실행 Job 수·DB/Redis 부하에 따라 4GB+ 권장.
- **실행 방식**: `java -jar investment-backend.jar` 또는 `./gradlew bootRun` (개발/검증용).
- **설정**: `application.yml` + 환경 변수. 비밀·API 키·DB URL 등은 환경 변수 또는 외부 시크릿 저장소 사용.
- **DB**: PostgreSQL(TimescaleDB) 또는 프로젝트에서 지원하는 DB. Flyway 마이그레이션은 기동 시 자동 적용.
- **Redis**: 선택. 미사용 시 `application-no-redis.yml` 등으로 캐시 타입 simple 사용 가능.

---

## 2. 스케줄(배치) 구조

- **관리 주체**: `BatchJobRegistry`에 Job 정의(이름, cron, 트리거 경로)가 등록되어 있으며, `BatchJobScheduler`가 기동 시 각 Job의 cron에 따라 `JobLauncher.run`을 호출한다.
- **실행 이력**: Spring Batch `JobRepository`(BATCH_* 테이블)에 자동 저장된다. 스케줄 현황 UI(`GET /batch/api/jobs`)는 이 이력과 레지스트리 정의를 조합해 “총 실행/성공/실패·마지막 실행 시각” 등을 노출한다.
- **수동 실행**: 인증 후 `POST /api/v1/trigger/{path}` 로 해당 Job을 한 번 실행할 수 있다. 예: `POST /api/v1/trigger/risk-event-alert`, `POST /api/v1/trigger/auto-buy`. 상세 경로는 [02-api-endpoints.md §트리거 API](../04-api/02-api-endpoints.md) 참조.

### 주요 Job·Cron 요약 (BatchJobRegistry 기준)

| Job ID | 설명 | Cron (Asia/Seoul) | 트리거 경로 |
|--------|------|-------------------|-------------|
| trading-portfolio-generator | 트레이딩 포트폴리오 생성 | 매일 09:00 | /api/v1/trigger/krx-daily 등 |
| krx-daily-collector | KRX 일별 시세 수집 | 매일 16:00 | /api/v1/trigger/krx-daily |
| us-daily-collector | US 일별 시세 수집 | 매일 17:00 | /api/v1/trigger/us-daily |
| factor-calculation | 팩터 계산 | 매일 08:00 | /api/v1/trigger/factor-calculation |
| auto-buy | 자동매수(통합) | 매일 09:10 | /api/v1/trigger/auto-buy |
| pipeline-exit | 파이프라인 청산 평가 | 장중 5분마다(평일) | /api/v1/trigger/pipeline-exit |
| fill-confirmation | 체결 확인 후 포지션 등록 | 매분 | /api/v1/trigger/fill-confirmation |
| unfilled-order-check | 미체결 확인·알림 | 매분 | /api/v1/trigger/unfilled-check |
| risk-event-alert | 리스크 이벤트 알림 | 장중 10분마다(평일) | /api/v1/trigger/risk-event-alert |
| daily-pnl | 일일 PnL 기록 | 평일 16:05 | /api/v1/trigger/daily-pnl |
| pipeline-execution, robo-rebalance | 파이프라인/로보 단독 | cron 없음(수동 전용) | 각각 trigger 경로로 수동 실행 |

정확한 cron·이름·설명은 코드 내 `BatchJobRegistry.getDefinitions()` 및 `application.yml`을 기준으로 한다.

---

## 3. 배포 절차 (요약)

1. **빌드**: `./gradlew bootJar` (또는 CI에서 artifact 생성).
2. **설정**: `application.yml`과 환경 변수로 DB URL, Redis, 한국투자증권 API·토큰, Discord Webhook, 파이프라인 auto-execute 등 설정.
3. **기동**: `java -Dspring.profiles.active=prod -jar build/libs/investment-backend-*.jar` (또는 systemd 서비스 등으로 감싸기).
4. **헬스 확인**: `GET /actuator/health` 로 서비스·DB·Redis 상태 확인.
5. **스케줄 확인**: `GET /batch/api/jobs` (인증 필요)로 Job 목록·마지막 실행 시각 확인.

---

## 4. 모니터링

- **헬스**: `GET /actuator/health`. DB·Redis·예측 서비스 등 상태 요약.
- **스케줄 현황**: `GET /batch/api/jobs`. Job별 실행 횟수·성공/실패·마지막 실행 시각. Admin 또는 인증 사용자만 접근 가능 시 인증 헤더/쿠키 필요.
- **로그**: 애플리케이션 로그 경로를 표준 출력 또는 파일로 두고, 로그 수집 도구(예: 로그 에이전트)로 수집·중앙화 권장. Kubernetes 배포 시에는 `/LOG` 등 규칙에 맞춘다.
- **알림**: Discord 긴급 알림(미체결·리스크 이벤트)이 설정되어 있으면, 해당 이벤트 발생 시 Webhook으로 수신 가능. 알림 이력은 `GET /api/v1/ops/alerts`(Admin)로 조회.

---

## 5. 복구 절차

- **서비스 중단 시**: VPS/프로세스 재기동. `java -jar` 또는 systemd로 재실행. DB·Redis 선행 기동 확인.
- **특정 Job만 재실행**: 해당 Job의 `POST /api/v1/trigger/{path}` 호출. 예: 파이프라인만 다시 실행하려면 `POST /api/v1/trigger/pipeline-execution`.
- **실패 원인 확인**: BATCH_* 테이블(STEP_EXECUTION 등)과 애플리케이션 로그에서 실패 단계·예외 메시지 확인. 필요 시 설정(API 키·한도·네트워크) 점검.
- **DB 마이그레이션**: Flyway는 기동 시 자동 적용. 롤백이 필요하면 Flyway 이력 및 수동 스크립트 검토.

---

## 6. 체크리스트 (배포 전)

- [ ] DB·Redis 접속 정보 및 방화벽 허용
- [ ] 한국투자증권 API 키·시크릿·계좌 설정(모의/실전 구분)
- [ ] 필요 시 `PIPELINE_AUTO_EXECUTE`, `PIPELINE_ALLOW_REAL_EXECUTION` 등 파이프라인 관련 환경 변수 설정
- [ ] Discord Webhook URL(미체결·리스크 이벤트 알림 사용 시)
- [ ] 타임존: 스케줄은 Asia/Seoul 기준으로 등록되어 있음. 서버 타임존 또는 JVM 옵션 확인
- [ ] 로그·헬스 엔드포인트 접근 경로(방화벽·리버스 프록시) 확인

---

## 문서 변경 이력

| 버전 | 일자 | 변경 내용 |
|------|------|----------|
| 1.0 | 2026-02-11 | 초안: 단일 VPS 배포·배치 스케줄·모니터링·복구 절차, 리스크 이벤트 알림 Job 반영 |
