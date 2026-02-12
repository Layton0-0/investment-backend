# CI/CD 구현 체크리스트

## 개요

멀티 VPS 배포를 위한 CI/CD 점검 결과와 권장 구조를 체크리스트 형태로 정리한다. 실제 워크플로우는 `.github/workflows`에 추가하는 단계에서 본 문서를 참조한다. 배포 토폴로지·시크릿·노드별 동작은 [05-multi-vps-oracle-aws-cicd.md](05-multi-vps-oracle-aws-cicd.md)를 참조한다.

---

## 1. 문서 대비 갭 정리

| 항목 | 문서 내용 | 현재 구현 | 조치 |
|------|-----------|-----------|------|
| CI 트리거 | PR/푸시 시 빌드·테스트·이미지 푸시 | 완료 | 각 서비스 레포 ci.yml |
| CD 트리거 | main 푸시 또는 workflow_dispatch | 완료 | investment-infra/cd.yml |
| 빌드·테스트 | Backend: gradlew test bootJar, Front: npm ci/build/test 등 | 완료 | 워크플로우에서 재현 |
| 이미지 푸시 | ghcr.io, 태그=commit SHA 7자리 | 완료 | 각 서비스 Dockerfile 기준 빌드·푸시 |
| CD 실행 | SSH로 노드 접속 후 deploy-*.sh | 완료 | Actions에서 SSH 배포 job |
| 배포 후 검증 | 배포 직후 Backend 헬스체크 | 완료 | cd.yml Verify Oracle 2 / Oracle 3 step |

---

## 2. 권장 CI/CD 구조

### 2.1 레포 전략

- 멀티 레포 유지 시 **각 서비스 레포에 CI 워크플로우**를 두고, **배포(CD)는 한 곳에서 통합**.
- CD "한 곳": investment-infra 레포 또는 상위 auto-investment-project(또는 별도 deploy 레포).

### 2.2 CI (서비스별)

- **트리거**: 해당 레포의 PR/푸시 (main 또는 배포 브랜치).
- **단계**: Checkout → Build & Test → Docker build → Push to ghcr.io (태그: `sha-${GITHUB_SHA::7}` 또는 `latest`).
- **시크릿**: `GITHUB_TOKEN`(ghcr.io 푸시) 또는 PAT.

### 2.3 CD (통합)

- **트리거**: main(또는 release) 푸시, 또는 `deploy/*` 태그.
- **입력**: 배포할 이미지 태그(기본값 `GITHUB_SHA` 7자리).
- **단계**:
  1. Oracle 1 (필요 시) — SSH로 `deploy-oracle1.sh`.
  2. Oracle 2 — `.env`에 `BACKEND_TAG`, `PREDICTION_TAG`, `DATA_COLLECTOR_TAG` 설정 후 `deploy-oracle2.sh`.
  3. Oracle 3 (Mumbai) — 동일하게 `.env` 설정 후 `deploy-oracle3-mumbai.sh` 또는 `deploy-oracle2.sh` 실행.
  4. AWS 사용 시 — `FRONTEND_TAG` 설정 후 `deploy-aws.sh`.
- **시크릿**: `SSH_PRIVATE_KEY_ORACLE_OSAKA`, `SSH_PRIVATE_KEY_ORACLE_KOREA`, `SSH_PRIVATE_KEY_ORACLE_MUMBAI`, (선택) `SSH_PRIVATE_KEY_AWS`, 호스트 정보(또는 SSH config 상정).

---

## 3. 구현 체크리스트

### 3.1 사전 점검

- [x] 각 서비스 레포에 Dockerfile 존재: backend, frontend, data-collector, prediction-service 모두 있음.
- [x] 이미지 이름: `investment-frontend`로 통일 (docker-compose.aws.yml·oracle2 등).
- [x] CD 워크플로우: investment-infra 레포 cd.yml에서 이미지 태그 입력 후 SSH 배포. 배포 전 `git fetch && git reset --hard origin/main`으로 최신 compose/스크립트 반영.

### 3.2 CI 워크플로우 (서비스별)

- [x] investment-backend: `.github/workflows/ci.yml` — checkout, Gradle test, bootJar, Docker build, push to ghcr.io.
- [x] investment-front: `.github/workflows/ci.yml` — checkout, npm ci, build, test, Docker build, push (image: investment-frontend).
- [x] investment-prediction-service: `.github/workflows/ci.yml` — checkout, pip install, pytest, Docker build, push.
- [x] investment-data-collector: `.github/workflows/ci.yml` — pip install, Docker build, push.

### 3.3 CD 워크플로우

- [x] **위치**: `investment-infra/.github/workflows/cd.yml`. 트리거: `workflow_dispatch`(입력 image_tag), push to main.
- [x] job: Oracle 1 → Oracle 2 → Oracle 3 (Mumbai) → AWS(선택). 각 노드에서 `investment-infra` 경로로 SSH 후 set-env-tags + deploy 스크립트 실행.
- [x] **배포 후 검증**: Oracle 2 / Oracle 3 배포 직후 `curl -sf http://localhost:8080/actuator/health` 로 Backend 헬스체크. 실패 시 워크플로우 실패.
- [ ] **시크릿 등록**(운영자 작업): 저장소 **Settings → Secrets and variables → Actions**에서만 등록. SSH 비밀키 **내용**을 붙여넣기(코드/문서에 절대 넣지 않음). 이름: `SSH_PRIVATE_KEY_ORACLE_OSAKA`, `SSH_PRIVATE_KEY_ORACLE_KOREA`, `SSH_PRIVATE_KEY_ORACLE_MUMBAI`, (선택) `SSH_PRIVATE_KEY_AWS`. **전체 목록·안내**: [08-devops-required-tokens-and-keys.md](08-devops-required-tokens-and-keys.md).
- [ ] **저장소 변수**(운영자 작업): Settings → Variables. `DEPLOY_HOST_ORACLE_OSAKA`, `DEPLOY_HOST_ORACLE_KOREA`, `DEPLOY_HOST_ORACLE_MUMBAI`, (선택) `DEPLOY_HOST_AWS`, (선택) `DEPLOY_USER`(기본 ubuntu). 배포할 노드만 설정하면 해당 단계만 실행됨. **전체 목록**: [08-devops-required-tokens-and-keys.md](08-devops-required-tokens-and-keys.md).

### 3.4 배포 스크립트·문서

- [x] investment-infra: `deploy-oracle1.sh`, `deploy-oracle2.sh`, `deploy-oracle3-mumbai.sh`, `deploy-aws.sh`, `set-env-tags.sh`, `check-node-ready.sh` 존재. scripts/README.md 반영.
- [x] investment-infra: `.env.example` 제공(필수 변수 목록만, 값 없음). 각 노드에서 복사 후 .env 설정.
- [x] 05-multi-vps-oracle-aws-cicd.md: Oracle 3(Mumbai), 시크릿, CD 순서, SSH config(oci-mumbai) 반영.

---

## 4. 참고 문서

- [05-multi-vps-oracle-aws-cicd.md](05-multi-vps-oracle-aws-cicd.md)
- [06-oci-vcn-subnet-design.md](06-oci-vcn-subnet-design.md)
- [08-devops-required-tokens-and-keys.md](08-devops-required-tokens-and-keys.md) — DevOps 구축 시 필요한 Secrets/Variables·로컬 MCP·이미지 태그/REGISTRY 정리.
- [investment-infra/scripts/README.md](../../../investment-infra/scripts/README.md)

---

## 문서 변경 이력

| 버전 | 일자 | 변경 내용 |
|------|------|-----------|
| 1.0 | 2026-02-11 | 초안: CI/CD 갭 정리, 권장 구조, 구현 체크리스트 |
| 1.1 | 2026-02-12 | §1 갭 정리·§3.3 CD 워크플로우 현황 반영(배포 후 검증·시크릿/변수 문서 링크). §4 참고 문서에 08-devops-required-tokens-and-keys.md 추가. |
| 1.2 | 2026-02-12 | §3.1 사전 점검 완료 처리. CD에 배포 전 git fetch/reset 추가·Oracle 1은 deploy-oracle1.sh 사용. §3.4 .env.example·스크립트 목록 반영. |