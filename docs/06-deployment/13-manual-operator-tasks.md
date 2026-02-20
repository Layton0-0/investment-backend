# 운영자 수동 작업 목록

**목적**: Agent/CI로 수행할 수 없고 **운영자가 반드시 수동으로 수행해야 하는 작업**을 한 문서에서 관리한다.

**갱신 원칙**: 배포·인프라·보안 관련해서 "사람이 직접 해야 하는" 작업은 모두 여기에 기록한다.  
**앞으로도** 새로운 수동 작업이 생기거나 절차가 바뀔 때마다 이 문서에 항목을 추가·갱신한다.

---

## 1. 현재 수동 작업 목록

### 1.1 Oracle 1 (Osaka) — Security List에 AWS IP 허용 — **완료**

- **상태**: 완료. 각 서버별 인바운드/아웃바운드 정책 전체는 **[14-server-inbound-outbound-policy.md](14-server-inbound-outbound-policy.md)** 참조.
- (기존) OCI 콘솔 → Oracle 1(Osaka) VCN → Security List → Ingress에 AWS Public IP 5432·6379 추가.

---

### 1.2 Cloudflare DNS — api / app A 레코드 — **완료**

- **상태**: 완료. api·app A 레코드 및 CNAME(www 등) 설정 완료. 프록시(주황 구름) 여부는 필요 시 알려주면 재작업.
- (기존) [Cloudflare 대시보드](https://dash.cloudflare.com) → neekly-report.cloud → DNS.

---

### 1.3 GitHub Actions — Variables / Secrets (AWS 배포용) — **완료**

- **상태**: 완료. `DEPLOY_HOST_AWS`, `SSH_PRIVATE_KEY_AWS`, (선택) `DEPLOY_USER_AWS` 설정 완료.
- (기존) Settings → Variables and secrets → Actions.

---

### 1.4 AWS(및 Oracle 2/3) — 스왑 확인 및 미설정 시 설정

- **작업**: 각 노드 SSH 접속 후 `free -m`, `swapon --show`로 스왑 확인. **OCI 3대는 각 10GB**, AWS는 2GB 권장. 미적용 시 [05-multi-vps-oracle-aws-cicd.md §3.0](05-multi-vps-oracle-aws-cicd.md) 절차대로 설정. **스왑 끄기(swapoff) 전에는 반드시** 해당 노드에서 컨테이너 정리(down)·이미지 prune으로 메모리 확보 후 진행.
- **이유**: OOM 방지. 배포 전 정리 규칙(해당 compose down + image prune)은 [05 §6, §8](05-multi-vps-oracle-aws-cicd.md) 참조.
- **Agent 수행**: SSH MCP로 해당 노드 접속 가능하면 Agent가 실행. (현재 AWS MCP 키 경로가 다른 사용자 기준이라 접속 실패 시 **운영자가 SSH로 직접** 위 명령 실행.)

---

### 1.5 (선택) Certbot — 도메인 소유 증명

- **작업**: DNS 전파 후 AWS에서 `api.neekly-report.cloud`, Oracle 2에서 `app.neekly-report.cloud` 각각 `sudo certbot certonly --standalone -d <도메인>` 실행.  
  Agent가 SSH MCP로 접속 가능하면 Agent 수행. (현재 AWS MCP 키 경로 미일치 시 **운영자가 SSH로 직접** 실행.) 80 포트 사용 중이면 nginx 잠시 중단 후 실행.
- **api.neekly-report.cloud**: **완료** (AWS, 80 포트 개방 후 Certbot 성공). 인증서는 `/etc/letsencrypt/live/api.neekly-report.cloud/` 및 `~/investment-infra/secrets/certs/live/api.neekly-report.cloud/`에 복사됨. 만료: 2026-05-21.
- **자동 갱신**: AWS 노드에서 **한 번** cron 등록 필요. [11-dns-and-domain-setup.md §4.4](11-dns-and-domain-setup.md#44-인증서-자동-갱신-cron) 참조. 스크립트: `investment-infra/scripts/renew-certs-aws.sh`. 예: `0 0,12 * * * root /home/ec2-user/investment-infra/scripts/renew-certs-aws.sh >> /var/log/certbot-renew.log 2>&1` 를 `/etc/cron.d/certbot-renew-aws`에 추가.

---

## 2. 완료 이력 (참고)

| 일자       | 항목 | 비고 |
|------------|------|------|
| 2026-02-20 | 1.5 Certbot (api) | AWS에서 api.neekly-report.cloud 발급·secrets/certs 복사 완료. |
| (갱신 시)  | —    | 완료된 수동 작업은 위 §1에서 체크 또는 이 표에 요약. |

---

## 3. 참조

- [14-server-inbound-outbound-policy.md](14-server-inbound-outbound-policy.md) — 서버별 인바운드/아웃바운드 정책 통합
- [08-devops-required-tokens-and-keys.md](08-devops-required-tokens-and-keys.md) — GitHub Secrets/Variables 전체 목록
- [11-dns-and-domain-setup.md](11-dns-and-domain-setup.md) — DNS·Certbot 절차
- [05-multi-vps-oracle-aws-cicd.md](05-multi-vps-oracle-aws-cicd.md) — 노드 역할·방화벽
