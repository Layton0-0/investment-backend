# CI/CD 및 방화벽 점검 체크리스트

## 1. CI 상태 (서비스별)

| 저장소 | 워크플로우 | 트리거 | 최근 상태 | 비고 |
|--------|------------|--------|-----------|------|
| investment-backend | `.github/workflows/ci.yml` | push/PR → main, dev | 성공 | Gradle test, bootJar, Docker push ghcr.io |
| investment-front (frontend) | `.github/workflows/ci.yml` | push/PR → main | 성공 | npm ci, build, vitest(--run), Docker push |
| investment-prediction-service | `.github/workflows/ci.yml` | push/PR → main | 성공 | pip, pytest, Docker push |
| investment-data-collector | `.github/workflows/ci.yml` | push/PR → main | 성공 | pip, Docker push |
| investment-infra | `.github/workflows/cd.yml` | push → main, workflow_dispatch | CD만 존재 | 아래 §2·§3 참조 |

- **CI 실패 시**: 해당 레포에서 `gh run list` / `gh run view <id> --log-failed`로 원인 확인. 프론트는 Vitest만 실행( e2e 제외), 백엔드는 `./gradlew test`.
- **이미지 푸시**: 각 CI가 `ghcr.io/<owner 소문자>/<이미지명>:latest`, `sha-<7자리>` 푸시. CD에서 이 태그 사용.

---

## 2. CD 사전 조건 (investment-infra)

CD가 성공하려면 **GitHub 저장소 설정**과 **각 노드 준비**가 필요하다.

### 2.1 GitHub Actions Secrets / Variables

- **Secrets** (Settings → Secrets and variables → Actions → Secrets):  
  `SSH_PRIVATE_KEY_ORACLE_OSAKA`, `SSH_PRIVATE_KEY_ORACLE_KOREA`, `SSH_PRIVATE_KEY_ORACLE_MUMBAI`, (선택) `SSH_PRIVATE_KEY_AWS`, (private 이미지 사용 시) `GHCR_PULL_TOKEN`
- **Variables**:  
  `DEPLOY_HOST_ORACLE_OSAKA`, `DEPLOY_HOST_ORACLE_KOREA`, `DEPLOY_HOST_ORACLE_MUMBAI`, (선택) `DEPLOY_HOST_AWS`, (선택) `DEPLOY_USER`(기본 ubuntu)
- 상세: [08-devops-required-tokens-and-keys.md](08-devops-required-tokens-and-keys.md)

### 2.2 노드 준비

- 각 노드에 **investment-infra** 클론, Docker·Docker Compose, **.env** 설정 완료.
- **메모리·스왑**: [05-multi-vps-oracle-aws-cicd.md §3.0](05-multi-vps-oracle-aws-cicd.md) 노드별 스왑 현황 확인.
- 점검: SSH 접속 후 `~/investment-infra/scripts/check-node-ready.sh` 실행.
- 순서: [07-cicd-implementation-checklist.md §3.5](07-cicd-implementation-checklist.md) 참조.

---

## 3. 방화벽 / Security List 작업 정리

CD 및 서비스 동작을 위해 **서버별 인바운드/아웃바운드 규칙**을 반영해야 한다.  
**전체 정책(OCI Oracle 1·2·3, AWS 보안 그룹)** 은 **[14-server-inbound-outbound-policy.md](14-server-inbound-outbound-policy.md)** 에 통합되어 있다.

### 3.1 CD 실패 시 점검

- **"dial tcp ...:22: i/o timeout"**: GitHub Actions 러너 → 해당 노드 SSH(22) 불가.  
  - 해당 노드 VM 기동 여부, **Security List Ingress TCP 22**에 **GitHub runner IP** 또는 넓은 대역 허용 여부 확인.  
  - (Runner IP는 가변일 수 있어, 실무에서는 관리자 IP만 22 허용하거나, VPN/배스천을 두는 경우가 많음.)
- **Oracle 2/3에서 Backend health 실패**: 노드 내부에서 `curl -s http://localhost:8080/actuator/health` 확인.  
  - DB/Redis 연결 실패 시 Oracle 1 방화벽에 Oracle 2/3/**AWS** Public IP가 5432·6379에 허용돼 있는지 재확인.

---

## 4. 문서 변경 이력

| 버전 | 일자 | 변경 내용 |
|------|------|-----------|
| 1.0 | 2026-02-13 | 초안: CI 상태 표, CD 사전 조건, 방화벽/Security List 작업 정리 |
