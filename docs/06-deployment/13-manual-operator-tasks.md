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
- **자동 갱신**: AWS 노드에서 **한 번** cron 등록 필요.
  - **상태**: Oracle 2와 동일하게 구성함. cronie 설치, `/etc/cron.d/certbot-renew-aws` 등록(PATH 포함), `docker-compose.aws-api.yml`·`nginx/`·`scripts/renew-certs-aws.sh` 배치 완료. 스크립트에 cron용 PATH 설정 반영.
  - **Docker**: 해당 AWS 노드에 **Docker 설치 완료** (dnf install docker, Docker Compose v2 CLI 플러그인 GitHub 설치, systemctl enable --now docker, ec2-user docker 그룹 추가). 갱신 스크립트는 root cron에서 `docker compose` 호출 가능. API 스택 기동은 `.env` 설정 후 `./scripts/deploy-aws-api.sh` 실행.
  - **ec2-user docker 권한**: 새 세션에서도 `docker ps` 가능하도록 **소켓 666** 적용. 재시작 후 유지: `sudo mkdir -p /etc/systemd/system/docker.socket.d` 후 `SocketMode=0666` 인 override.conf 작성, `systemctl daemon-reload && systemctl restart docker.socket docker`.
  - **API 스택 컨테이너**: `~/investment-infra/.env`에 REGISTRY, *_TAG, SPRING_DATASOURCE_URL(Oracle 1 IP), REDIS_HOST(Oracle 1 IP) 반영됨. **POSTGRES_PASSWORD**만 Oracle 1과 동일한 DB 비밀번호로 AWS 서버에서 설정 후 `./scripts/deploy-aws-api.sh` 실행(또는 CD로 배포). 이미지 pull은 ghcr.io 인증 필요.
  - **미등록 시** 운영자 SSH 접속 후: `echo "0 0,12 * * * root PATH=/usr/bin:/bin:/usr/local/bin /home/ec2-user/investment-infra/scripts/renew-certs-aws.sh >> /var/log/certbot-renew.log 2>&1" | sudo tee /etc/cron.d/certbot-renew-aws && sudo chmod 644 /etc/cron.d/certbot-renew-aws`.
  - 상세: [11-dns-and-domain-setup.md §4.4](11-dns-and-domain-setup.md#44-인증서-자동-갱신-cron).

---

### 1.6 Cloudflare Error 521 (Web server is down) 대응

- **증상**: 브라우저에서 `neekly-report.cloud` 또는 `app.neekly-report.cloud` 접속 시 Cloudflare "Error 521 — Web server is down" 표시. (Cloudflare는 정상, 오리진 서버 미응답.)
- **오리진**: **Oracle 2 (Korea)** — Nginx 엣지 + Frontend. [14-server-inbound-outbound-policy.md §2](14-server-inbound-outbound-policy.md#2-oracle-2-앱엣지-korea--oci-security-list) 참조.
- **운영자 체크리스트** (순서대로 확인):
  1. **Cloudflare DNS**  
     [Cloudflare 대시보드](https://dash.cloudflare.com) → neekly-report.cloud → DNS  
     - `app` (A): **Oracle 2 Korea Public IP**를 가리키는지 확인.  
     - 루트 `@`(neekly-report.cloud) 사용 시: 해당 A 레코드도 **Oracle 2 Korea Public IP**인지 확인.  
     - 잘못된 IP(예: 이전 호스트·AWS만 사용 중인 경우)면 Oracle 2 Public IP로 수정.
  2. **OCI Security List (Oracle 2 Korea)**  
     OCI 콘솔 → Oracle 2(Korea) VCN → Security List  
     - **Ingress**: TCP **80**, **443** 소스 **0.0.0.0/0** 허용 규칙 존재 여부 확인. [14-server-inbound-outbound-policy.md §2](14-server-inbound-outbound-policy.md#2-oracle-2-앱엣지-korea--oci-security-list)  
     - 없으면 추가 후 저장.
  3. **Oracle 2 호스트 (SSH)**  
     - `docker ps` → `investment-nginx-edge`, `investment-frontend` 상태가 Up 인지 확인.  
     - `curl -I http://localhost` → HTTP 200 등 응답 확인.  
     - Down 이면 `~/investment-infra`에서 `./scripts/deploy-oracle2-edge.sh` 또는 `docker compose -f docker-compose.oracle2-edge.yml up -d` 실행.
  4. **Oracle 2 호스트 iptables (방화벽)**  
     OCI Security List만 열려 있어도 **호스트 iptables**에서 80/443이 막혀 있으면 521 발생.  
     - `sudo iptables -L INPUT -n --line-numbers` 로 **tcp dpt:80**, **tcp dpt:443** ACCEPT 규칙이 REJECT 앞에 있는지 확인.  
     - 없으면: `sudo iptables -I INPUT 5 -p tcp --dport 80 -j ACCEPT`, `sudo iptables -I INPUT 5 -p tcp --dport 443 -j ACCEPT`  
     - 재부팅 후 유지: `sudo iptables-save | sudo tee /etc/iptables.rules` 후, 부팅 시 복원 스크립트 또는 `netfilter-persistent` 사용.
  5. **Cloudflare SSL/TLS 모드 (프록시 사용 시)**  
     오리진(Oracle 2)은 현재 **HTTP(80)만** 제공하고, **HTTPS(443) 서버 블록은 비활성** 상태.  
     Cloudflare가 **Full** 또는 **Full (strict)** 이면 오리진 **443**으로 접속 시도 → TLS 실패 → **521**.  
     - [Cloudflare 대시보드](https://dash.cloudflare.com) → neekly-report.cloud → **SSL/TLS** → **Overview**  
     - **Encryption mode** 를 **Flexible** 로 변경. (방문자↔Cloudflare는 HTTPS, Cloudflare↔오리진은 **HTTP 80** 사용.)  
     - **보안**: Flexible이면 **Cloudflare↔오리진 구간이 평문**이라, 해당 구간 도청 가능. 권장은 **§1.7**대로 app 인증서 적용 후 **Full** 전환.
- **참고**: Cloudflare 프록시(주황 구름) 사용 시에도 오리진은 80/443에서 인터넷(또는 Cloudflare IP) 접속을 허용해야 함.

---

### 1.7 (권장) app 오리진 HTTPS — Certbot on Oracle 2 + Cloudflare Full

- **목적**: **Flexible** 대신 **Full** 사용으로 Cloudflare↔오리진 구간까지 TLS 적용. (Certbot은 **api**는 AWS에서 완료, **app**은 Oracle 2에서 별도 발급.)
- **작업 순서** (Oracle 2 Korea SSH):
  0. **Certbot 미설치 시**  
     - 호스트에 설치: `sudo apt-get update && sudo apt-get install -y certbot`  
     - 또는 Docker 한 번 실행(80 사용 가능해야 함, nginx 중지 후):  
       `sudo docker run --rm -p 80:80 -v /etc/letsencrypt:/etc/letsencrypt -v /var/lib/letsencrypt:/var/lib/letsencrypt certbot/certbot certonly --standalone -d app.neekly-report.cloud -d neekly-report.cloud --non-interactive --agree-tos --register-unsafely-without-email`
  1. **Certbot 발급**  
     - 80 사용 중인 nginx를 잠시 중지: `cd ~/investment-infra && docker compose -f docker-compose.oracle2-edge.yml stop nginx`  
     - `sudo certbot certonly --standalone -d app.neekly-report.cloud -d neekly-report.cloud` (루트 도메인도 함께 발급 시 두 개 모두 입력)  
     - 인증서 경로: `/etc/letsencrypt/live/app.neekly-report.cloud/` (fullchain.pem, privkey.pem)
  2. **인증서를 compose 볼륨 경로로 복사**  
     - `mkdir -p ~/investment-infra/secrets/certs/live && sudo cp -rL /etc/letsencrypt/live/app.neekly-report.cloud ~/investment-infra/secrets/certs/live/`  
     - 필요 시 `sudo chown -R $(whoami) ~/investment-infra/secrets/certs/live/app.neekly-report.cloud`
  3. **nginx 443 서버 블록 활성화**  
     - `investment-infra/nginx/conf.d.edge/app.conf` 에서 443용 `server { listen 443 ssl; ... }` 블록 **주석 해제**.  
     - `server_name`에 `neekly-report.cloud` 포함 시 루트 도메인도 443에서 처리 가능.  
     - 배포(또는 수동 수정 후 `git pull` + `docker compose -f docker-compose.oracle2-edge.yml up -d --force-recreate nginx`)
  4. **Cloudflare SSL/TLS**  
     - **SSL/TLS** → **Overview** → **Encryption mode** 를 **Full** (또는 **Full (strict)**) 로 변경.
  5. **갱신**  
     - Let’s Encrypt 만료 전(90일)에 Oracle 2에서 `certbot renew` 실행. (80 사용 중이면 nginx 잠시 중지 후 실행.)  
     - **스크립트**: `investment-infra/scripts/renew-certs-oracle2-edge.sh`. **cron 등록** (한 번만): [11-dns §4.4](11-dns-and-domain-setup.md#44-인증서-자동-갱신-cron). 예: `echo "0 0,12 * * * root /home/ubuntu/investment-infra/scripts/renew-certs-oracle2-edge.sh >> /var/log/certbot-renew-oracle2.log 2>&1" | sudo tee /etc/cron.d/certbot-renew-oracle2`
- **완료 후**: 방문자↔Cloudflare↔오리진 전 구간 HTTPS.

---

### 1.8 로컬 Docker Compose — 로그 영구 보관 및 일일 백업

- **목적**: 로컬 풀스택 로그를 **영구 보관 구조**로 두고, 필요 시 일일 스냅샷 백업도 수행.
- **영구 보관 (docker-compose 적용)**  
  - **방법 A**: 모든 서비스에 `logging: driver: json-file`, `max-size: "100m"`, `max-file: "5"` → 컨테이너당 로그 5개 로테이션.  
  - **방법 B**: backend에 `./logs/backend:/LOG` 볼륨 → Spring Boot `logging.file.name=/LOG/investment-choi.log` 가 호스트 `investment-infra/logs/backend/` 에 저장됨. **down 해도 로그 유지.**  
  - 적용: `docker-compose.local-full.yml` 에 이미 반영. 재기동 시 `logs/backend` 디렉터리는 Docker가 없으면 생성.
- **일일 백업 (선택)**  
  - `investment-infra/scripts/backup-local-compose-logs.ps1` — 각 서비스 stdout/stderr를 `logs-backup/YYYYMMDD/<서비스>.log` 로 저장(30일 초과 분 자동 삭제).  
  - 한 번만 등록: `.\scripts\register-log-backup-task.ps1` → Windows 작업 스케줄러 **Investment-Local-Compose-LogBackup**, 매일 03:00 실행.  
  - 삭제: `Unregister-ScheduledTask -TaskName Investment-Local-Compose-LogBackup`
- **Backend 재시작이 16:00/17:00(KST) 이후인 경우**  
  - 당일 **KRX 일별 수집**(16:00)·**US 일별 수집**(17:00)은 스케줄에 의해 이미 지나 있어 자동 실행되지 않음.  
  - **보완 실행**: Ops → 스케줄 현황에서 **KRX 일별 수집**, **US 시장 일별 수집** 각각 **지금 실행** 버튼 클릭. 또는 API로 `POST /api/v1/trigger/krx-daily`, `POST /api/v1/trigger/us-daily` 호출(인증 필요).  
  - 실행 여부는 `GET /api/v1/batch/jobs` 응답의 `lastExecutionTime` 또는 [plans/qa/scripts/배치_실행_이력_점검.sql](../../../plans/qa/scripts/배치_실행_이력_점검.sql) 로 확인.

---

### 1.9 Discord 긴급 알림 Webhook 설정 (선택)

- **목적**: 미체결 주문·리스크 이벤트·전략 거버넌스 열화 시 Discord 채널로 알림 수신.
- **작업**:
  1. Discord 서버 → 알림 받을 채널 → **연동** → **웹후크** → **새 웹후크** → URL 복사.
  2. Backend가 읽는 환경에 **`PIPELINE_ALERT_DISCORD_WEBHOOK_URL`** 설정 (예: Docker `.env`, 배포 서버 환경 변수). 값은 `https://discord.com/api/webhooks/...` 형태. **저장소에 커밋하지 않음.**
  3. Backend 재시작 후 **연결 테스트**: `POST /api/v1/trigger/discord-test` (인증 필요).  
     - 응답 `success: true` → 채널에 테스트 메시지가 오면 정상.  
     - `success: false`, "Webhook URL이 설정되지 않았습니다" → 환경 변수·재시작 확인.
- **알림이 나가는 조건**: (1) 미체결 N분 경과(기본 1분), (2) 일일 손실 한도 임박·VaR 95% 초과, (3) 전략 거버넌스 검사 후 MDD/Sharpe 열화. URL 미설정 시 해당 이벤트에서 알림 스킵(로그만 DEBUG).

---

### 1.10 전략 거버넌스 검사 결과 이력 (이력 없음 시)

- **증상**: Ops → 전략 거버넌스 화면에서 **검사 결과 이력**이 "이력 없음"으로만 표시됨.
- **원인**: `strategy-governance-check` Job이 아직 한 번도 실행되지 않아 TB_GOVERNANCE_CHECK_RESULT에 데이터가 없음.
- **작업**: (1) **수동 실행**: Ops → 스케줄 현황에서 "전략 거버넌스 검사" **지금 실행** 버튼 클릭, 또는 `POST /api/v1/trigger/strategy-governance-check` 호출. (2) **스케줄 대기**: 매월 1일 02:00 KST에 자동 실행되므로 그 후에는 이력이 쌓임.
- **참고**: API `GET /api/v1/ops/governance/results` 및 프론트 연동은 완료되어 있으며, 이력이 없으면 빈 배열이 반환되는 것이 정상임.

---

### 1.11 자동매매 가동 전 점검

- **목적**: 매일 09:10 KST 자동매수(통합) 실행 전에 운영자가 확인할 수 있는 체크리스트. 반복 검증·배포 후 점검에 사용.
- **점검 항목** (순서대로 확인 권장):
  1. **인프라**: DB(Spring Batch 메타데이터 테이블 존재), Redis 연결, Backend 기동 후 `auto-buy` 스케줄 등록 로그 확인 (`Scheduled batch job: id=auto-buy, cron=0 10 9 * * *`).
  2. **데이터 파이프라인**: 전일 **KRX 일별 수집**(16:00), **US 일별 수집**(17:00), 당일 **팩터 계산**(08:00)이 선행 완료되어 있어야 함. 재기동이 16:00/17:00 이후면 §1.8 대로 수동 트리거 (`POST /api/v1/trigger/krx-daily`, `POST /api/v1/trigger/us-daily`, `POST /api/v1/trigger/factor-calculation`).
  3. **계좌·설정**: 자동매매할 계좌의 TB_TRADING_SETTINGS에서 `AUTO_TRADING_ENABLED=true`, `MAX_INVESTMENT_AMOUNT>0`, 실제 주문 시 `PIPELINE_AUTO_EXECUTE=true`(또는 서버 기본). 실전 계좌면 `PIPELINE_ALLOW_REAL_EXECUTION` 허용. TB_USER_ACCOUNTS에 해당 계좌 등록·USER_ID 매칭 확인.
  4. **게이트**: 활성 **거버넌스 halt** 없음 확인(Ops → 전략 거버넌스, 활성 halt 해제 시 `PUT /api/v1/ops/governance/halts/{market}/{strategyType}/clear`). 해당 계좌·시장·전략이 중지/일시정지 아님 확인.
  5. **준비 상태 API**: `GET /api/v1/ops/auto-trading-readiness`(인증 필요)로 자동투자 ON 계좌 수, 전일 일봉·시그널 row 수, 활성 halt 수를 한 번에 확인. 09:10 전 점검용.
- **시그널이 0건으로 보일 때 점검 순서** (자동투자 현황 화면에서 시그널 수가 0인 경우):
  1. **파이프라인 요약 기준일**: `GET /api/v1/pipeline/summary`는 **basDt 미입력 시 전일(어제)**을 기준일로 사용함. 프론트에서 basDt 없이 호출하면 전일 기준 시그널이 조회됨. 전일 팩터 계산이 완료되었는지 확인.
  2. **준비 상태 확인**: `GET /api/v1/ops/auto-trading-readiness`로 전일(basDt) TB_DAILY_STOCK·TB_SIGNAL_SCORE row 수 확인. `dailyStockRowCount`·`signalScoreRowCount`가 0이면 선행 데이터·팩터 미실행.
  3. **수동 트리거**: 위에서 일봉·시그널이 비어 있으면 순서대로 `POST /api/v1/trigger/krx-daily`, `POST /api/v1/trigger/us-daily`, `POST /api/v1/trigger/factor-calculation` 실행 후 다시 readiness·pipeline/summary 확인. KRX 수집 실패 시 [01-local-setup-complete.md §US/KRX 수집](../08-setup-guides/01-local-setup-complete.md)의 KRX_AUTH_KEY·한투 폴백 env 확인.
- **수동 검증**: `POST /api/v1/trigger/auto-buy?dryRun=true`로 1회 실행 후 Backend 로그에서 대상 계좌·스킵 사유 메시지 확인. 실제 주문 전에는 dryRun=true 권장.
- **참조**: [12-auto-investment-strategy.md §6.2](../02-architecture/12-auto-investment-strategy.md), 자동매매 선행 조건 종합 계획(plans).

---

### 1.12 로컬 PC — 평일 18:00(KST) 일일 GitHub 동기화 배치

- **목적**: 전체 레포(루트 + .gitmodules 서브모듈) 변경분을 평일 오후 6시(한국시간)에 자동 커밋·푸시.
- **스크립트**: 프로젝트 루트 `scripts/daily-git-sync.ps1` (주말은 스킵, 서브모듈별·루트 순으로 add/commit/push).
- **등록**: 프로젝트 루트에서 `.\scripts\register-daily-git-sync-task.ps1` 실행 → Windows 작업 스케줄러에 **Investment-Daily-Git-Sync** 등록 (평일 18:00 로컬 시간). 18:00 KST로 맞추려면 Windows 표준 시간대를 (UTC+09:00) 서울로 설정.
- **삭제**: `Unregister-ScheduledTask -TaskName Investment-Daily-Git-Sync`

---

### 1.13 주문 실패 시 점검 (수동 주문·Circuit Breaker)

- **증상**: 주문·체결 화면에서 수동 주문 시 "일시적으로 주문 API를 사용할 수 없습니다. 회로가 일시 중단되었습니다. 30초 후 다시 시도해 주세요." 등 오류 표시.
- **점검 항목** (순서대로 확인):
  1. **Circuit Breaker**: 주문 API 호출이 연속 실패 시 Resilience4j Circuit Breaker가 OPEN 상태가 됨. **30초 대기 후** 재시도하면 회로가 HALF_OPEN으로 전환되어 주문이 다시 시도됨. 동일 오류가 반복되면 KIS API·네트워크·토큰을 점검.
  2. **가격 자동 채움**: 국내(KR) 종목은 종목 선택 후 수량을 입력하면 현재가가 자동으로 가격 필드에 채워짐. 가격이 비정상(예: 1원)이면 한국투자증권 API에서 거부될 수 있으므로, 종목·수량 입력 후 가격이 채워졌는지 확인하고 필요 시 수동 수정.
  3. **모의/실전 계좌·API 키**: 사용 중인 계좌가 모의/실전 중 어느 쪽인지, 해당 계좌에 맞는 한국투자증권 앱키·시크릿이 TB_USER_API_KEYS 등에 등록되어 있는지 확인. 토큰 만료·401 시 Backend 로그에서 재발급 시도 여부 확인.
- **참조**: [09-korea-investment-api-guide.md](../04-api/09-korea-investment-api-guide.md), application.yml `resilience4j.circuitbreaker.instances.orderService` (waitDurationInOpenState: 30s).

---

### 1.14 API 연동 검증 — 모의계좌 주문·잔고·체결 확인

- **목적**: 한국투자증권 API가 정상 연동되었는지 **모의계좌**로 주문·잔고·매수가능금액 등을 호출해 확인. **실계좌는 사용하지 않음.**
- **전제 조건**:
  1. Backend `.env`(또는 배포 환경)에 한국투자증권 **모의투자** 앱키·시크릿 설정.
  2. TB_USER_ACCOUNTS에 **모의계좌** 계좌번호 등록, 해당 사용자로 로그인 가능.
  3. 킬스위치 비활성: `GET /api/v1/system/kill-switch` → `haltAllOrders: false`.
- **검증 순서** ( [plans/qa/api-qa.http](../../../plans/qa/api-qa.http) §8 참조 ):
  1. **로그인** → JWT 토큰 획득.
  2. **모의계좌 잔고**: `GET /api/v1/accounts/{모의계좌번호}/balance` → 200, 예수금·총자산 등 반환.
  3. **매수가능금액**: `GET /api/v1/accounts/{모의계좌번호}/buyable-amount?symbol=005930&price=50000&quantity=1` → 200.
  4. **주문 실행** (소액 권장): `POST /api/v1/orders` body에 accountNo(모의계좌), symbol=005930, market=KR, side=BUY, quantity=1, price=현재가 근처, orderType=LIMIT → 200 및 orderId 반환.
  5. **주문 목록**: `GET /api/v1/orders?accountNo={모의계좌번호}` → 방금 넣은 주문 포함 확인.
  6. (선택) **미체결 취소**: `DELETE /api/v1/orders/{orderId}?accountNo={모의계좌번호}`.
- **실패 시**: §1.13 주문 실패 시 점검 참조. 모의계좌·모의 API 키·토큰 갱신·Circuit Breaker 대기 확인.

---

### 1.15 Backend 기동 실패 및 스레드 정리 경고 (Unable to start web server / Lettuce·Hikari)

- **증상**: Backend 기동 시 `ApplicationContextException: Unable to start web server` 발생 후, Tomcat 종료 시 `lettuce-timer-*-*`, `HikariPool-* housekeeper`, `HikariPool-* connection adder` 스레드가 정리되지 않았다는 메모리 누수 경고가 로그에 남음.
- **원인**:
  1. **웹 서버 기동 실패**: 로그 **상단**에서 실제 원인 확인. 흔한 경우는 **포트 사용 중** (예: 8080 이미 사용) → `Address already in use` 또는 `Port 8080 was already in use`. 로컬에서 Agent/다른 인스턴스가 이미 8080을 쓰고 있으면 해당 프로세스 종료 후 재기동.
  2. **스레드 경고**: 기동 실패로 컨텍스트가 취소될 때 Redis(Lettuce)·HikariCP가 이미 스레드를 띄운 뒤라, 종료 단계에서 이들이 완전히 정리되기 전에 검사가 이뤄지면 위 경고가 출력됨.
- **완화 조치** (이미 적용됨):
  - **그레이스풀 셧다운**: `server.shutdown=graceful`, `spring.lifecycle.timeout-per-shutdown-phase=35s` 로 컨텍스트 종료 시 Bean 정리 대기 시간 확보.
  - **Lettuce 종료 대기**: `spring.data.redis.lettuce.shutdown-timeout=5s` 로 Redis 연결·타이머 정리 시간 확보.
- **운영자 확인**: "Unable to start web server" 발생 시 로그 **맨 위**부터 확인해 포트·바인딩·DB/Redis 연결 실패 등 **실제 예외 메시지**를 찾고, 해당 원인 해결 후 재기동. 스레드 경고만으로는 정상 기동 후에는 영향 없을 수 있으나, 동일 포트 중복 기동 등은 반드시 제거.

---

## 2. 완료 이력 (참고)

| 일자       | 항목 | 비고 |
|------------|------|------|
| 2026-02-20 | 1.5 Certbot (api) | AWS에서 api.neekly-report.cloud 발급·secrets/certs 복사 완료. |
| 2026-02-20 | 1.6 Error 521 (iptables) | Oracle 2 Korea 호스트 iptables INPUT에 80/443 ACCEPT 추가. 원인: OCI만 열고 호스트 방화벽에서 80/443 미허용. |
| 2026-02-20 | 1.7 app Certbot (Oracle 2) | Oracle 2에서 Docker certbot으로 app.neekly-report.cloud·neekly-report.cloud 발급, secrets/certs/live 복사, app.conf 443 활성화 완료. Cloudflare Full 전환 가능. |
| 2026-02-20 | 1.7 갱신 cron (Oracle 2) | renew-certs-oracle2-edge.sh 추가, Oracle 2에 /etc/cron.d/certbot-renew-oracle2 등록(0 0,12 매일). |
| 2026-02-20 | 1.5 AWS 갱신 cron | Oracle 2와 동일하게: cronie, cron 등록(PATH 포함), docker-compose.aws-api.yml·nginx·scripts 배치. 해당 노드에 Docker 미설치 시 갱신은 Docker 설치 후 동작. |
| 2026-02-20 | 1.5 AWS Docker 설치 | AWS 노드에 Docker·Docker Compose v2 설치, 기동, ec2-user docker 그룹 추가. 갱신 cron에서 docker compose 정상 호출 가능. |
| 2026-02-20 | 1.5 AWS docker 권한·컨테이너 | 소켓 666 + systemd override로 ec2-user docker ps 가능. .env 플레이스홀더·deploy-aws-api.sh 배치. 이미지 pull은 ghcr.io 인증 후 deploy 재실행 필요. |
| (갱신 시)  | —    | 완료된 수동 작업은 위 §1에서 체크 또는 이 표에 요약. |

---

## 3. 참조

- [14-server-inbound-outbound-policy.md](14-server-inbound-outbound-policy.md) — 서버별 인바운드/아웃바운드 정책 통합
- [08-devops-required-tokens-and-keys.md](08-devops-required-tokens-and-keys.md) — GitHub Secrets/Variables 전체 목록
- [11-dns-and-domain-setup.md](11-dns-and-domain-setup.md) — DNS·Certbot 절차
- [05-multi-vps-oracle-aws-cicd.md](05-multi-vps-oracle-aws-cicd.md) — 노드 역할·방화벽
