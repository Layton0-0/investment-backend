# DevOps 구축 시 필요한 토큰·키 정리

## 개요

이 문서는 **배포·CI/CD에 직접 사용하는** 토큰·키·변수만 한곳에 정리한다.  
사용자가 아래 항목을 세팅한 뒤 **"세팅했어, 다시 명령해"**라고 하면, Agent가 SSH MCP로 각 노드 점검·배포 검증을 이어갈 수 있다.

**보안**: 실제 비밀키 내용·IP·호스트명은 **코드/문서에 절대 기입하지 않는다.**  
값은 GitHub Settings 또는 로컬 `mcp.json`(저장소에 커밋하지 않음)에서만 설정한다.

---

## 1. GitHub Actions (investment-infra CD)

CD 워크플로우는 **investment-infra** 저장소의 `.github/workflows/cd.yml`에서 실행된다.  
배포할 노드에만 아래 Secrets/Variables를 등록하면 해당 노드 step만 실행된다.

### 1.1 Secrets (Settings → Secrets and variables → Actions → Secrets)

| 이름 | 용도 | 값 |
|------|------|-----|
| `SSH_PRIVATE_KEY_ORACLE_OSAKA` | Oracle Osaka(Oracle 1) SSH 배포 | 해당 노드 배포용 SSH **비밀키 전체 내용**을 붙여넣기 |
| `SSH_PRIVATE_KEY_ORACLE_KOREA` | Oracle Korea(Oracle 2) SSH 배포 | 동일 |
| `SSH_PRIVATE_KEY_ORACLE_MUMBAI` | India West (Mumbai, Oracle 3) SSH 배포 | 동일 |
| `SSH_PRIVATE_KEY_AWS` | AWS SSH 배포 (AWS 사용 시) | 동일 |
| `GHCR_PULL_TOKEN` | GHCR private 이미지 pull (Oracle 2/3, AWS) | GitHub PAT. 권한: **read:packages**. 이미지가 private일 때만 필요. 없으면 해당 step에서 docker login 생략. |

- **등록 방법**: GitHub 저장소 → Settings → Secrets and variables → Actions → New repository secret.  
- **값**: PEM 형식 비밀키 파일 전체 내용을 복사해 붙여넣기. **문서·코드에는 절대 넣지 않는다.**

### 1.2 Variables (Settings → Secrets and variables → Actions → Variables)

| 이름 | 용도 | 값 |
|------|------|-----|
| `DEPLOY_HOST_ORACLE_OSAKA` | Oracle Osaka 호스트 | 해당 VM의 **Public IP** (또는 호스트명). 문서에 실제 IP 기입 금지. |
| `DEPLOY_HOST_ORACLE_KOREA` | Oracle Korea 호스트 | 동일 |
| `DEPLOY_HOST_ORACLE_MUMBAI` | Mumbai 호스트 | 동일 |
| `DEPLOY_HOST_AWS` | AWS 호스트 (선택) | 동일 |
| `DEPLOY_USER` | SSH 로그인 사용자 | 기본값 `ubuntu` (Oracle Linux면 `opc`) |

- 배포하지 않을 노드는 해당 Variable을 **비워 두면** CD에서 해당 step이 스킵된다.

### 1.3 최종 확인용 체크리스트 (CD 실행 전)

CD를 처음 실행하기 전에 아래를 확인한다.

| 구분 | 항목 | 확인 |
|------|------|------|
| **Secrets** | `SSH_PRIVATE_KEY_ORACLE_OSAKA` (Oracle 1 배포 시) | Settings → Secrets and variables → Actions → Secrets |
| | `SSH_PRIVATE_KEY_ORACLE_KOREA` (Oracle 2 배포 시) | 동일 |
| | `SSH_PRIVATE_KEY_ORACLE_MUMBAI` (Oracle 3 배포 시) | 동일 |
| | `SSH_PRIVATE_KEY_AWS` (AWS 배포 시) | 동일 |
| | `GHCR_PULL_TOKEN` (이미지가 private일 때) | 동일 |
| **Variables** | `DEPLOY_HOST_ORACLE_OSAKA` (Public IP 또는 호스트명) | Settings → Variables |
| | `DEPLOY_HOST_ORACLE_KOREA` | 동일 |
| | `DEPLOY_HOST_ORACLE_MUMBAI` | 동일 |
| | `DEPLOY_HOST_AWS` (선택) | 동일 |
| | `DEPLOY_USER` (기본 ubuntu, 미설정 시 ubuntu 사용) | 동일 |
| **노드** | 각 배포 대상 노드에 investment-infra 클론됨 | SSH로 `ls ~/investment-infra` 확인 |
| | 각 노드에 .env 설정됨 (07-cicd-implementation-checklist §3.5 참조) | `./scripts/check-node-ready.sh` |
| **이미지** | GHCR에 backend / prediction-service / data-collector (및 필요 시 frontend) 이미지 존재 | 각 서비스 레포 main 푸시로 CI 1회 실행 후 확인 |

위가 모두 충족된 뒤 **Actions → CD → Run workflow** 로 배포한다.

### 1.4 CD 정상화 상태 (정리)

- **트리거**: `push` to main (경로 무시: `**.md`, `docs/**`) 또는 `workflow_dispatch`.
- **노드별 실행**: Variables에 `DEPLOY_HOST_*`가 설정된 노드만 해당 step 실행. Secrets에 `SSH_PRIVATE_KEY_*`, (private 이미지 사용 시) `GHCR_PULL_TOKEN` 필요.
- **이미지**: REGISTRY는 `ghcr.io/<owner 소문자>`, CD에서 자동 설정. 스크립트 실행 비트는 reset 후 `chmod +x scripts/*.sh`로 보정.
- **확인**: 로컬에서 `gh auth login` 후 `gh run watch --exit-status`로 성공/실패 확인 가능. 규칙: [.cursor/rules/cd-push-and-verify.mdc](../../.cursor/rules/cd-push-and-verify.mdc).

**CD에서 "manifest unknown" 나올 때**: 해당 이미지가 GHCR에 아직 없음. **investment-backend**, **investment-prediction-service**, **investment-data-collector**(, **investment-frontend**) 각 레포에서 **CI를 한 번씩 main에 푸시**해 `latest` 이미지를 GHCR에 올린 뒤 CD를 다시 돌린다.

**Oracle 2/3 deploy 단계**: 첫 풀 시 prediction-service 이미지(~2.9GB) 때문에 오래 걸릴 수 있음. `cd.yml`에서 해당 SSH 단계 `command_timeout`을 15m으로 두었음. **Oracle 3 (Mumbai)**에서 `dial tcp ...:22: i/o timeout`이 나오면 GitHub Actions 러너에서 해당 호스트로 SSH 접속이 안 되는 것이므로, VM 기동 여부·방화벽/보안 그룹(22 포트)·네트워크를 점검한다.

### 1.5 Agent가 CD 푸시 후 직접 확인하려면

**방법 1 — GitHub CLI (권장)**  
로컬에 [GitHub CLI](https://cli.github.com/) 설치 후 `gh auth login` 실행.  
이후 Agent(Cursor)가 `investment-infra` 푸시를 한 뒤 다음으로 실행 결과를 확인할 수 있다.

- `gh run list --workflow=cd.yml --limit 1` (최근 CD run)
- `gh run watch <run-id> --exit-status` (해당 run 완료까지 대기, 성공/실패 반환)

토큰 권한: `workflow`(또는 repo) 있으면 됨.

**방법 2 — GitHub Actions MCP**  
Cursor MCP에 **GitHub Actions Trigger MCP**를 추가하면 Agent가 워크플로우 실행·상태 조회를 할 수 있다.

- 패키지: `@nextdrive/github-action-trigger-mcp` (npx로 실행 가능)
- 설정: 사용자 MCP 설정 파일에 서버 추가, `GITHUB_PERSONAL_ACCESS_TOKEN`(또는 `GITHUB_TOKEN`) 설정. 토큰 권한에 `workflow` 포함.
- 프로젝트 규칙: [.cursor/rules/cd-push-and-verify.mdc](../../.cursor/rules/cd-push-and-verify.mdc) 참고. **gh가 설치돼 있으면** 한 번 `gh auth login` 후 Agent가 푸시·`gh run watch`로 CD 결과까지 확인 가능.

---

## 2. 로컬 Cursor SSH MCP

로컬 Cursor에서 Agent가 각 OCI 노드에 **원격 명령**(배포 스크립트, 로그 조회, docker compose 등)을 실행하려면 SSH MCP를 설정한다.

- **설정 파일**: `C:\Users\<사용자명>\.cursor\mcp.json` (프로젝트가 아닌 **사용자 홈**에 둠. Git에 올리지 않음.)
- **템플릿**: 프로젝트의 [.cursor/mcp.json.template](../../.cursor/mcp.json.template)을 복사한 뒤, 아래 플레이스홀더를 **본인 환경 값**으로 교체한다.

### 2.1 호스트별 MCP 항목

| MCP 서버 이름 | 호스트 | `--host` | `--user` | `--key` |
|---------------|--------|----------|----------|---------|
| ssh-mcp-oracle-osaka-yoon | Oracle Osaka | YOUR_ORACLE_OSAKA_PUBLIC_IP | ubuntu | YOUR_SSH_KEY_PATH_OSAKA |
| ssh-mcp-oracle-korea-jihee | Oracle Korea | YOUR_ORACLE_KOREA_PUBLIC_IP | ubuntu | YOUR_SSH_KEY_PATH_KOREA |
| ssh-mcp-oracle-mumbai-yoon | Oracle Mumbai | YOUR_ORACLE_MUMBAI_PUBLIC_IP | ubuntu | YOUR_SSH_KEY_PATH_MUMBAI |

- **키 경로**: Windows 절대 경로. **따옴표 없이** `--key=D:/path/to/key.key` 형태로 넣고, 경로는 **슬래시(`/`)만** 사용. 공백이 있으면 키를 `%USERPROFILE%\.ssh\oci_osaka.key` 등 공백 없는 경로로 복사 후 그 경로를 사용. (그렇지 않으면 ENOENT 등으로 MCP가 동작하지 않음.)
- 상세 설정·문제 해결: [07-cursor-oci-ssh-mcp.md](../08-setup-guides/07-cursor-oci-ssh-mcp.md) §3.2·§5.3 참조.

---

## 3. 이미지 태그·REGISTRY (통합 배포 시)

### 3.1 통합 배포 시 이미지 태그

- **권장**: `latest` 또는 `sha-<7자리>` (예: `sha-abc1234`).  
  각 서비스 레포 CI가 푸시하는 태그와 맞춰야 한다.  
  - Backend/Frontend/Prediction/Data-collector CI는 `sha-<전체SHA>`, `sha-<7자리>`, `latest` 세 가지 태그로 푸시.
- **CD 입력**: `workflow_dispatch` 시 입력한 `image_tag` 또는 push 시 기본값.  
  investment-infra의 cd.yml에서는 기본값을 `latest` 또는 `sha-${GITHUB_SHA::7}` 형태로 사용할 수 있다.  
  **멀티 레포**이므로 “통합 배포 시점”에 맞출 태그(예: backend main 브랜치 최신 SHA)를 수동으로 입력하거나, infra 쪽에서 고정 정책을 두고 사용한다.

### 3.2 .env의 REGISTRY

- **형식**: `REGISTRY=ghcr.io/<GitHub repository owner>`  
  예: 저장소가 `myorg/investment-backend`이면 `REGISTRY=ghcr.io/myorg`.
- **이유**: 이미지 풀 경로가 `ghcr.io/<owner>/investment-backend:latest` 등이 되어야 하므로, `<owner>` 부분을 REGISTRY에 넣는다.
- 노드별 `.env`에 `REGISTRY`, `BACKEND_TAG`, `PREDICTION_TAG`, `DATA_COLLECTOR_TAG`, `FRONTEND_TAG` 및 DB/Redis 연결 정보를 설정한다.  
  (실제 비밀번호·IP는 저장소에 커밋하지 않는다.) investment-infra 루트에 `.env.example`이 있으니 복사 후 값만 채운다.

### 3.3 DB 비밀번호 (노드 .env)

- **비밀번호는 .env에서만 설정**하며, 값은 저장소에 커밋하지 않는다. (공개 시에도 규칙·예시를 문서에 적지 않는다.)
- **Oracle 1 (TimescaleDB)** 와 **Oracle 2 (Backend)** 는 같은 DB를 사용하므로, 두 노드의 `.env`에 넣는 `POSTGRES_PASSWORD`는 **동일한 값**이어야 한다.

---

## 4. 배포 파이프라인 실행 (방법 A)

Secrets/Variables(§1) 세팅이 끝났으면 아래 순서로 CD를 실행한다.

1. **GitHub** → **investment-infra** 저장소 → **Actions** 탭.
2. 왼쪽에서 **"CD"** 워크플로 선택.
3. 오른쪽 **"Run workflow"** 클릭 → Branch **main**, Input **image_tag**에 `latest` 또는 `sha-<7자리>` 입력 → **Run workflow**.
4. 실행이 끝나면 run 이름은 **"CD - Deploy Oracle 1/2/3 (latest)"** (또는 입력한 태그) 형태로 표시된다.
5. **검증**: 같은 run 페이지에서 **Deploy Oracle 1 (Osaka)** → **Deploy Oracle 2 (Korea)** → **Deploy Oracle 3 (Mumbai)** step 성공 여부 확인. 마지막 **Verify Oracle 2**, **Verify Oracle 3 (Mumbai, Backend health)** step이 녹색이면 Backend 헬스체크까지 통과한 것이다.

---

## 5. 기타 (참고)

- **ghcr.io 푸시**: 각 서비스 레포 CI는 기본 제공 `GITHUB_TOKEN`으로 ghcr.io에 푸시한다.  
  Private 레포/패키지 권한이 필요하면 Organization/Repository 설정에서 패키지 쓰기 권한을 부여하거나, PAT를 Secrets에 등록해 사용한다.
- **Discord Webhook, 한국투자증권 API 등**: 배포·CI/CD와 직접 연동하는 토큰이 아니라 애플리케이션 런타임용이므로, [05-multi-vps-oracle-aws-cicd.md](05-multi-vps-oracle-aws-cicd.md) §8 배포 전·후 체크리스트를 참조한다. 본 문서에는 배포·SSH·이미지 푸시에 쓰는 항목만 정리한다.

---

## 6. 참고 문서

- [05-multi-vps-oracle-aws-cicd.md](05-multi-vps-oracle-aws-cicd.md) — 배포 토폴로지, CD 순서, 시크릿 이름.
- [07-cicd-implementation-checklist.md](07-cicd-implementation-checklist.md) — CI/CD 구현 체크리스트.
- [07-cursor-oci-ssh-mcp.md](../08-setup-guides/07-cursor-oci-ssh-mcp.md) — SSH MCP 설정 상세.

---

## 문서 변경 이력

| 버전 | 일자 | 변경 내용 |
|------|------|-----------|
| 1.0 | 2026-02-12 | 초안: DevOps 구축 시 필요한 토큰·키·Variables·REGISTRY/이미지 태그 정리 |
| 1.1 | 2026-02-12 | §4 배포 파이프라인 실행 (방법 A) 절차·검증 방법 추가. §5·§6 번호 조정. |
| 1.2 | 2026-02-12 | §3.3 DB 비밀번호: .env 전용·동기화 안내만 유지, 구체 규칙 제거(공개 대비). |
| 1.3 | 2026-02-12 | §1.3 최종 확인용 체크리스트(CD 실행 전) 추가. §1.4·§1.5 번호 조정. §3.1 이미지 태그 7자리 반영. |
