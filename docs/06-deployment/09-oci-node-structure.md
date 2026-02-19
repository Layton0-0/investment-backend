# OCI 노드별 디렉터리 구조 및 정리 기준

## 개요

Oracle 1(Osaka)·Oracle 2(Korea)·Oracle 3(Mumbai) 각 노드의 홈 디렉터리 구조를 파악한 결과와, **OCI 매크로 관련 항목을 제외한** 기존 구조 정리 기준을 정리한다. **노드 역할(토폴로지)**은 [05-multi-vps-oracle-aws-cicd.md §2](05-multi-vps-oracle-aws-cicd.md) 참조. 실제 IP·호스트명은 문서에 기입하지 않는다.

---

## 1. Oracle 1 (Osaka) — 데이터 계층

### 1.1 파악된 구조 (정리 전)

| 경로 | 용도 | 소유 | 비고 |
|------|------|------|------|
| `~/investment-infra/` | CD 배포용 인프라 (compose, 스크립트, .env) | ubuntu | **유지** — 현재 배포 표준 |
| `~/docker-compose/` | **기존** 수동 배포용 (backend, frontend, jenkins, nginx, certs, env.dev) | ubuntu/docker | **정리 대상** — investment-infra로 이관 |
| `~/jenkins_home/` | Jenkins 데이터 (jobs, credentials, plugins) | opc | **유지** — token-macro 등 매크로 관련 플러그인 포함 가능 |
| `~/.cursor/`, `~/.cursor-server/` | Cursor IDE 원격 개발 | ubuntu | 유지 |
| `~/.gradle/` | Gradle 캐시 (opc 소유) | opc | 유지 |
| `/home/ubuntu/docker/` | Docker 루트 디렉터리 (root) | root | 시스템 — 건드리지 않음 |

### 1.2 OCI·매크로 관련 (삭제 금지)

- `~/jenkins_home/plugins/token-macro*` — Jenkins Token Macro 플러그인
- (Osaka에는 `~/.oci` 없음 — OCI CLI 설정은 Korea 등 다른 노드에 있음)

### 1.3 Docker 상태 (예시)

- **유지**: `investment-timescaledb`, `investment-redis` (investment-infra oracle1 compose)
- **기존**: `jenkins` 컨테이너 — `~/docker-compose` 기반으로 기동 중일 수 있음. 해당 compose 제거 시 컨테이너는 down 처리됨.

### 1.4 정리 수행 내용 (완료)

- `~/docker-compose` 디렉터리 전체 삭제 (root 소유 certs 등은 `sudo rm -rf`로 제거). 기존 backend/frontend/jenkins/nginx compose 프로젝트 제거됨.
- `~/investment-infra`, `~/jenkins_home`, `~/.cursor*`, `~/.gradle` 등 **OCI 매크로 관련 및 배포 표준은 유지**.

---

## 2. Oracle 2 (Korea) — 엣지 (역할 상세는 05 §2 참조)

### 2.1 파악된 구조 (정리 전)

| 경로 | 용도 | 비고 |
|------|------|------|
| `~/investment-infra/` | CD 배포용 인프라, .env 포함 | **유지** |
| `~/.oci/` | OCI CLI 설정·키 (config, *.pem, *.key.pub) | **유지 — OCI 매크로 관련** |
| `~/docker-compose/` | **기존** compose (docker-compose.yml, duckling, osaka/) | **정리 대상** |
| `~/docker-compose/osaka/.oci/` | Osaka용 OCI 설정·키 (config, pem, key.pub) | **유지 — OCI 매크로 관련** |
| `~/docker-compose/osaka/.ssh/` | 스크립트·env·로그 (create_a1_instance_osaka.sh, e2.env, output_osaka.log) | 정리 가능 |
| `~/db_data/` | MariaDB 데이터 디렉터리 (ibdata1, mysql, neeklyreportdb 등) | 기존 DB — **삭제하지 않음** (필요 시 별도 지시) |
| `~/output.log`, `~/output_osaka.log` | 대용량 로그 파일 | 정리 대상 (용량 확보) |

### 2.2 OCI·매크로 관련 (삭제 금지)

- `~/.oci/` — OCI CLI config, API 키, SSH 키
- `~/docker-compose/osaka/.oci/` — Osaka 인스턴스용 OCI 키·설정

### 2.3 정리 수행 내용 (완료)

- `~/output.log`, `~/output_osaka.log` 삭제 (용량 확보).
- `~/docker-compose/docker-compose.yml`, `~/docker-compose/duckling/` 삭제.
- `~/docker-compose/osaka/.ssh/`, `~/docker-compose/osaka/output_osaka.log` 삭제.
- **유지**: `~/docker-compose/osaka/.oci/`, `~/.oci/`, `~/investment-infra/`, `~/db_data/`. (Korea에는 `~/docker-compose/osaka/` 아래 .oci만 남김)

---

## 3. Oracle 3 (Mumbai)

- SSH 연결 타임아웃으로 구조 미파악. 접속 가능 시 Oracle 2와 유사한 패턴으로 `.oci`·`investment-infra`는 유지하고, 기존 docker-compose/로그 등만 정리하는 것을 권장.

---

## 4. 정리 요약

| 노드 | 삭제·정리 | 유지 (OCI 매크로·배포 표준) |
|------|------------|-----------------------------|
| Osaka | `~/docker-compose/` 전체 | `~/investment-infra/`, `~/jenkins_home/`(token-macro 포함), `~/.cursor*`, `~/.gradle` |
| Korea | `~/docker-compose/*` 중 .oci 제외, `~/output*.log` | `~/.oci/`, `~/docker-compose/osaka/.oci/`, `~/investment-infra/`, `~/db_data/` |
| Mumbai | (접속 후 동일 원칙 적용) | `~/.oci/`, `~/investment-infra/` |

---

## 5. 참고 문서

- [05-multi-vps-oracle-aws-cicd.md](05-multi-vps-oracle-aws-cicd.md) — 노드 역할·토폴로지
- [08-devops-required-tokens-and-keys.md](08-devops-required-tokens-and-keys.md) — 배포 시 .env·시크릿

---

## 문서 변경 이력

| 버전 | 일자 | 변경 내용 |
|------|------|-----------|
| 1.0 | 2026-02-12 | 초안: Osaka/Korea 노드 구조 파악·정리 기준·OCI 매크로 유지 정책 문서화 |
