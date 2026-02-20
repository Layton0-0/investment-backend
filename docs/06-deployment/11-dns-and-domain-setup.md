# DNS 및 도메인 설정 (가비아 도메인 + Cloudflare DNS · nginx)

## 1. 개요

- **도메인 등록처**: 가비아 (기존 구매 도메인 사용).
- **도메인**: **neekly-report.cloud** (루트). 서브도메인으로 API와 프론트엔드를 분리한다.
- **DNS 제공자**: Cloudflare (가비아에서 네임서버만 Cloudflare로 변경).

### 1.1 도메인 등록처와 무료 여부

- **등록처가 따로 있어야 하나?**  
  아니요. **Cloudflare에서 도메인을 구매·이전하면** Cloudflare가 등록처 + DNS를 함께 처리하므로 별도 등록처(가비아 등)가 필요 없습니다.  
  Cloudflare 대시보드 → **등록** → 도메인 검색 후 구매 또는 이전하면 됩니다.
- **다른 곳에서 이미 등록한 경우**: 가비아·Namecheap 등에서 산 도메인은 그대로 두고, **네임서버만** Cloudflare로 바꾸면 DNS만 Cloudflare를 쓸 수 있습니다.
- **무료로 쓸 수 있나?**  
  **neekly-report.cloud**처럼 본인이 정한 이름의 **루트 도메인**은 대부분 연간 등록비가 듭니다(.cloud는 보통 연 약 $10~20대).  
  완전 무료인 건 거의 없고, 일부 서비스가 주는 **무료 서브도메인**(예: xxx.github.io, xxx.vercel.app)만 무료로 쓸 수 있습니다.

### 1.2 가비아 vs Cloudflare 단계별 비교

도메인·DNS를 **가비아**로 할지 **Cloudflare**로 할지 선택할 때 참고용이다. (도메인은 가비아에서 등록하고 DNS만 Cloudflare로 바꾸는 조합도 가능하다.)

| 단계 | 비교 항목 | 가비아 | Cloudflare (Free 기준) |
|------|-----------|--------|-------------------------|
| **1. 도메인 등록** | 역할 | **등록처**. .kr/.co.kr/.com 등 구매·갱신. | 등록은 **안 함**. Cloudflare Registrar로 구매·이전 시에만 등록처 역할. |
| | 비용 | 연간 등록비(도메인별 상이). | Registrar 사용 시 원가 판매(마진 없음). DNS만 쓸 경우 등록 비용 없음. |
| **2. DNS 관리** | 레코드 | A, AAAA, CNAME, MX, TXT 등 기본 제공. | 동일 + CAA, SRV 등. 레코드별 **프록시 on/off** 선택 가능. |
| | TTL | 제한적이거나 고정값인 경우 많음. | 레코드별 TTL 설정 가능. Auto 권장. |
| | 전파 속도 | 보통 수 분~수십 분. | 전 세계 Anycast, 전파 빠름. |
| **3. SSL/TLS** | 인증서 발급 | 가비아에서 발급하는 상품 별도(유료 옵션 등). | **프록시 사용 시** Cloudflare가 Edge에서 TLS 종단(클라이언트↔Cloudflare 암호화). 우리 서버까지는 Full/Full Strict로 암호화 가능. |
| | 우리 서버만 SSL | 우리가 Certbot 등으로 발급 후 nginx에 설정. 가비아는 DNS만. | DNS만(프록시 끔) 쓰면 동일. 프록시 켜면 Edge SSL + 우리 서버 SSL 선택 가능. |
| **4. 보안** | 원본 IP 노출 | A 레코드에 서버 IP 직접 등록 → **공격자가 원본 IP를 바로 알 수 있음**. | **프록시 켜면** 외부에는 Cloudflare IP만 보임 → 원본 IP 숨김. |
| | DDoS 완화 | 없음(별도 방화벽·서비스 계약 필요). | **L3/L4·일부 L7** 자동 완화(Free 포함). |
| | WAF | 없음. | **기본 규칙** 제공(Free). SQLi, XSS 등 차단 옵션. |
| | DNSSEC | 제공 여부 상이(제품에 따라). | **설정 가능**(Free). |
| **5. 운영·관리** | 대시보드 | 가비아 회원 → 도메인 관리 → DNS 설정. | Cloudflare 대시보드 → 사이트 → DNS → 레코드. |
| | API/자동화 | 제한적. | **API·Terraform** 등으로 레코드·설정 자동화 가능. |
| **6. 비용 요약** | DNS만 사용 시 | 도메인 연간료만. DNS 추가 비용 보통 없음. | **무료**. 도메인은 다른 등록처에서 등록 후 네임서버만 Cloudflare로 변경하면 됨. |
| | 등록+DNS 한곳에서 | 가비아에서 등록하면 DNS도 가비아에서 관리. | Cloudflare Registrar로 등록 시 등록+DNS 모두 Cloudflare. |

**정리**

- **도메인은 가비아에서 이미 등록했거나 .kr/.co.kr을 쓸 경우**: 등록은 가비아 유지, **DNS만 Cloudflare로 이전**(네임서버 변경)하면 무료로 DDoS 완화·원본 IP 숨김·WAF 등 보안 이점을 쓸 수 있다.
- **보안을 우선**하면: **Cloudflare Free + 프록시(주황 구름)** 조합이 유리하다. 단, 우리 nginx에서 SSL을 직접 쓰고 싶으면 **프록시 끔(회색 구름)** 으로 두고 A 레코드만 Cloudflare에서 관리해도 전파·DNSSEC 등 이점은 있다.
- **가비아만 쓸 경우**: DNS는 기본 A/CNAME 설정만 가능하고, DDoS·WAF·원본 IP 숨김은 없으므로 서버 방화벽·nginx 보안 설정에 더 의존하게 된다.

- **구성**:
  - **api.neekly-report.cloud** → **AWS (API 계층)** Public IP(또는 Elastic IP). AWS 인스턴스(2GB RAM, 30GB EBS)에서 Backend·prediction·data-collector·nginx(api) 운영 시 이 레코드를 AWS IP로 설정. ([05-multi-vps-oracle-aws-cicd.md](05-multi-vps-oracle-aws-cicd.md) 참조.) AWS 미사용 시에는 Oracle 2(Korea) Public IP로 설정 가능.
  - **app.neekly-report.cloud** → **Oracle 2 (Korea)** 또는 Oracle 3 (Mumbai) Public IP. 엣지(Frontend + nginx). /api 요청은 AWS(또는 API 서버)로 프록시. (§2.4 레코드 2 참조.)
- **보안**: 실제 Public IP는 저장소에 커밋하지 않는다. Cloudflare 대시보드·GitHub Variables·노드 `.env`에서만 설정. 코드/설정에는 `EXAMPLE_DOMAIN` 등 플레이스홀더만 사용한다.

---

## 2. 가비아 도메인(neekly-report.cloud) + Cloudflare DNS 설정 (단계별 상세)

**기존에 가비아에서 구매한 도메인 neekly-report.cloud**를 사용하고, **DNS만 Cloudflare**에서 관리하는 경우의 설정 값과 절차를 단계별로 정리한다.  
**실제 Public IP는 저장소에 기입하지 않고**, 가비아·Cloudflare 화면과 본인 메모에만 입력한다.

---

### 2.1 단계 1 — 가비아에서 기존 도메인 확인

| 순서 | 화면/메뉴 | 설정 항목 | 입력할 값 / 확인 사항 |
|------|-----------|-----------|------------------------|
| 1 | 가비아 회원 로그인 | — | [가비아](https://www.gabia.com) 접속 후 로그인 |
| 2 | **마이페이지** → **도메인 관리** | — | **neekly-report.cloud** 도메인이 목록에 있는지 확인 |
| 3 | 해당 도메인 **관리** | — | **네임서버 설정** (또는 **DNS 위임**·**네임서버 변경**) 메뉴 위치 확인. 다음 단계(Cloudflare 사이트 추가 후)에서 여기서 네임서버를 Cloudflare 것으로 바꾼다. |

- 이미 구매한 도메인이므로 **새로 구매할 필요 없음**. 갱신일만 확인해 두면 된다.

---

### 2.2 단계 2 — Cloudflare에 사이트 추가 및 네임서버 확인

| 순서 | 화면/메뉴 | 설정 항목 | 입력할 값 |
|------|-----------|-----------|-----------|
| 1 | [Cloudflare 대시보드](https://dash.cloudflare.com) | 로그인 | Cloudflare 계정으로 로그인 |
| 2 | **웹사이트** 영역 | **사이트 추가** 버튼 | 클릭 |
| 3 | **도메인 입력** | 도메인 | `neekly-report.cloud` (루트만, 서브도메인 없이) |
| 4 | **플랜 선택** | 플랜 | **Free** 선택 후 **계속** |
| 5 | **스캔 결과** (기존 레코드 유무) | — | 기존 레코드가 있으면 그대로 **계속** (나중에 수정 가능). 없으면 빈 상태로 **계속** |
| 6 | **네임서버 안내** 화면 | 표시된 네임서버 2개 | 반드시 **복사해 둔다**. 예시 형식: `xxx.ns.cloudflare.com`, `yyy.ns.cloudflare.com` (실제는 Cloudflare가 부여한 값) |

- 이 단계에서는 **가비아 쪽을 아직 수정하지 않는다**. 다음 단계에서 가비아에 위 네임서버 2개를 등록한다.

---

### 2.3 단계 3 — 가비아에서 네임서버를 Cloudflare 것으로 변경

| 순서 | 화면/메뉴 | 설정 항목 | 입력할 값 |
|------|-----------|-----------|-----------|
| 1 | 가비아 **마이페이지** | 도메인 관리 | **neekly-report.cloud** 선택 |
| 2 | 해당 도메인 **관리** | **네임서버 설정** (또는 **DNS 위임**·**네임서버 변경** 등) 메뉴 진입 | — |
| 3 | 네임서버 입력 방식 | **사용자 정의 네임서버** 또는 **네임서버 직접 입력** 선택 | 가비아에서 “기본 네임서버” 대신 직접 입력하는 옵션 선택 |
| 4 | **1번 네임서버** | 호스트/주소 | Cloudflare에서 안내한 **첫 번째** 네임서버 전체 (예: `xxx.ns.cloudflare.com`) |
| 5 | **2번 네임서버** | 호스트/주소 | Cloudflare에서 안내한 **두 번째** 네임서버 전체 (예: `yyy.ns.cloudflare.com`) |
| 6 | 저장/적용 | — | **저장** 또는 **적용** 후 안내에 따라 확인. 전파에 수 분~최대 24시간 걸릴 수 있음. |

- 3개 이상 입력란 있으면: Cloudflare가 2개만 안내하면 2개만 입력하고 나머지는 비우거나, 가비아 안내에 따름.
- **이후 DNS 레코드(A, CNAME 등)는 모두 Cloudflare 대시보드에서만** 추가·수정한다. 가비아 DNS 설정 메뉴는 더 이상 쓰지 않는다.

---

### 2.4 단계 4 — Cloudflare에서 DNS 레코드 추가 (필드별 상세)

Cloudflare 대시보드 → **웹사이트** → 해당 도메인 선택 → **DNS** → **레코드** → **레코드 추가**.

#### 레코드 1: api 서브도메인 (A)

| 필드명 | 입력할 값 | 비고 |
|--------|-----------|------|
| **유형** | `A` | 드롭다운에서 A 선택 |
| **이름** | `api` | api.neekly-report.cloud 이 됨. 서브도메인만 입력 |
| **IPv4 주소** | **AWS(API 계층) Public IP** 또는 Elastic IP. (AWS 미사용 시 Oracle Korea 등 API 서버 Public IP) | 실제 IP는 Cloudflare 화면에만 입력. 문서에는 기입하지 않음 |
| **프록시 상태** | **DNS만**(회색 구름) | 우리 nginx에서 SSL 종단 시 권장. 주황 구름(프록시)은 원본 IP 숨김·DDoS 완화 시 사용 |
| **TTL** | `Auto` | 그대로 두거나, 원하면 300~3600 등으로 변경 |

- **저장** 클릭.

#### 레코드 2: app 서브도메인 (A)

| 필드명 | 입력할 값 | 비고 |
|--------|-----------|------|
| **유형** | `A` | A 선택 |
| **이름** | `app` | app.neekly-report.cloud 이 됨 |
| **IPv4 주소** | **(현재)** AWS 인스턴스 없음 → 레코드 추가 보류 가능. **추후** AWS EC2 1대 생성 시 해당 인스턴스 Public IP. 또는 **임시**로 Oracle Korea/Mumbai 중 frontend를 띄운 노드의 Public IP | 설계상 app은 AWS(엣지, Nginx+Frontend) 예정. 리전·인스턴스 미생성 시 이 레코드만 나중에 추가하면 됨. |
| **프록시 상태** | **DNS만**(회색 구름) | nginx SSL 직접 사용 시 권장 |
| **TTL** | `Auto` | — |

- **저장** 클릭. (AWS를 아직 만들지 않았다면 **레코드 2는 AWS 생성 후** 그때의 Public IP로 추가해도 됨.)

#### 레코드 3: www (CNAME, 선택)

| 필드명 | 입력할 값 | 비고 |
|--------|-----------|------|
| **유형** | `CNAME` | CNAME 선택 |
| **이름** | `www` | www.neekly-report.cloud → app.neekly-report.cloud 으로 연결 |
| **대상** | `app` 또는 `app.neekly-report.cloud` | Cloudflare에 따라 `app`만 넣어도 되는 경우 있음. 안 되면 FQDN `app.neekly-report.cloud` 입력 |
| **프록시 상태** | **DNS만** 또는 **프록시** | 선택 |
| **TTL** | `Auto` | — |

- **저장** 클릭.

#### 요약 표 (한눈에 보기)

| 유형 | 이름 | 대상/콘텐츠 | 프록시 | TTL |
|------|------|-------------|--------|-----|
| A | api | [Oracle Korea Public IP] | DNS만(회색) | Auto |
| A | app | [AWS Public IP] — **현재 AWS 인스턴스 없음. 생성 후 추가.** 또는 임시: Oracle Korea/Mumbai 중 frontend 노드 IP | DNS만(회색) | Auto |
| CNAME | www | app (또는 app.neekly-report.cloud) | DNS만 또는 프록시 | Auto |

---

### 2.5 단계 5 — 전파 확인

```bash
# api 서브도메인 → API 서버 Public IP로 응답하는지
dig api.neekly-report.cloud +short

# app 서브도메인 → 프론트 서버 Public IP로 응답하는지
dig app.neekly-report.cloud +short

# Windows 등에서
nslookup api.neekly-report.cloud
nslookup app.neekly-report.cloud
```

- A 레코드가 **설정한 Public IP**를 반환하면 전파 완료.  
- 안 나오면 수 분~수 시간 기다린 뒤 다시 시도. 가비아 네임서버 변경 직후에는 24시간까지 걸릴 수 있음.

---

### 2.6 (참고) Cloudflare SSL/TLS 모드 (프록시 사용 시)

**프록시(주황 구름)**를 켠 경우에만 해당. **DNS만** 쓰면 우리 nginx 인증서만 사용하면 된다.

- Cloudflare **SSL/TLS** → **개요**:
  - **유연**: 클라이언트↔Cloudflare 암호화, Cloudflare↔원본은 HTTP. (테스트용)
  - **전체**: 클라이언트↔Cloudflare↔원본 모두 암호화. 원본에 자체 인증서 또는 Cloudflare Origin CA 필요.
  - **전체(엄격)**: 원본에 유효한 인증서 필수. 우리 nginx + Certbot 조합에 적합.

---

## 3. Cloudflare DNS 레코드 설정 (요약)

- 접속: [Cloudflare 대시보드](https://dash.cloudflare.com) → **웹사이트** → **neekly-report.cloud** → **DNS** → **레코드**.
- 상세 입력 값은 위 **§2.4 단계 4** 참조.

---

## 4. Oracle 2 / Oracle 3 — nginx 추가 구성

### 4.1 compose

- **파일**: investment-infra **docker-compose.oracle2-korea-app.yml**.
- **nginx 서비스**: 80/443 포트, `./nginx/nginx.conf`, `./nginx/conf.d`, `./secrets/certs` → `/etc/nginx/certs` 읽기 전용 마운트. `depends_on: backend`.

### 4.2 conf.d (API 가상호스트)

- **파일**: investment-infra **nginx/conf.d/api.conf**.
- 저장소에는 `server_name api.EXAMPLE_DOMAIN;` 및 SSL 경로 `API_DOMAIN` 플레이스홀더만 있다.
- **배포 시 치환**: 노드에서 아래 중 하나 적용.
  - **envsubst**: `export DOMAIN=neekly-report.cloud` 후 `envsubst '${DOMAIN}' < conf.d/api.conf.tpl > conf.d/api.conf` (템플릿을 둔 경우), 또는
  - **수동**: `api.EXAMPLE_DOMAIN` → `api.neekly-report.cloud`, `API_DOMAIN` → `api.neekly-report.cloud` 로 치환.

### 4.3 Certbot 발급 및 nginx volume 마운트

1. **DNS 전파 후** 해당 노드(Oracle Korea 등)에서:
   ```bash
   sudo apt install certbot
   sudo certbot certonly --standalone -d api.neekly-report.cloud
   ```
   - nginx가 이미 80을 쓰고 있으면, nginx를 잠시 중단한 뒤 `certbot certonly --standalone -d api.neekly-report.cloud` 실행 후 nginx 재기동.
   - 또는 `--webroot -w /var/www/certbot -d api.neekly-report.cloud` 사용 시 api.conf의 `/.well-known/acme-challenge/` 경로와 webroot를 맞춘다.

2. **발급 경로**: `/etc/letsencrypt/live/api.neekly-report.cloud/fullchain.pem`, `privkey.pem`.

3. **compose에서 사용**:
   - **방법 A**: 호스트 `/etc/letsencrypt`를 그대로 마운트하도록 compose에서  
     `- /etc/letsencrypt:/etc/nginx/certs:ro` 로 변경 후, api.conf의 cert 경로를 `/etc/nginx/certs/live/api.neekly-report.cloud/...` 로 치환.
   - **방법 B**: `secrets/certs` 사용 시, 노드에서  
     `mkdir -p ~/investment-infra/secrets/certs && sudo cp -rL /etc/letsencrypt/live/api.neekly-report.cloud ~/investment-infra/secrets/certs/live/`  
     로 복사한 뒤, api.conf에서 cert 경로를 `/etc/nginx/certs/live/api.neekly-report.cloud/...` 로 치환. (기본 compose는 `./secrets/certs` → `/etc/nginx/certs` 마운트.)

4. **갱신**: §4.4 인증서 자동 갱신(cron) 참조. 방법 B를 쓰면 갱신 후 위 복사 명령을 다시 실행해야 하므로, **갱신 스크립트 + cron**으로 자동화하는 것을 권장한다.

### 4.4 인증서 자동 갱신 (cron)

- **목적**: Let's Encrypt 인증서는 약 90일마다 만료되므로, 주기적으로 `certbot renew` 실행 후 nginx가 쓰는 인증서를 갱신된 파일로 덮어쓰고 nginx를 재기동해야 한다.
- **스크립트**: investment-infra **scripts/renew-certs-aws.sh**
  - nginx 중지 → `certbot renew` (80 포트 사용) → `/etc/letsencrypt/live/api.neekly-report.cloud`를 `secrets/certs/live/`로 복사 → nginx 기동.
- **AWS 노드에서 cron 등록** (한 번만 설정):
  - 저장소가 예: `/home/ec2-user/investment-infra`에 있다면:
    ```bash
    sudo bash -c 'echo "0 0,12 * * * root /home/ec2-user/investment-infra/scripts/renew-certs-aws.sh >> /var/log/certbot-renew.log 2>&1" > /etc/cron.d/certbot-renew-aws'
    ```
  - 또는 `sudo crontab -e`로 root crontab에 다음 한 줄 추가 (경로를 실제 investment-infra 경로로 변경):
    ```text
    0 0,12 * * * /home/ec2-user/investment-infra/scripts/renew-certs-aws.sh >> /var/log/certbot-renew.log 2>&1
    ```
  - 실행 주기: **0 0,12** = 매일 00:00, 12:00 (Certbot 권장: 하루 2회). 필요 시 `0 */6 * * *` 등으로 조정 가능.
- **실행 권한**: 스크립트는 **root** 또는 **sudo**로 실행되어야 함 (certbot, docker, cp 권한). `/etc/cron.d/`에 넣을 때 사용자 컬럼을 `root`로 두면 root가 실행한다.
- **cron 미설치 시**: Amazon Linux 등에서 `/etc/cron.d/`가 없으면 `sudo dnf install -y cronie` (또는 `sudo yum install -y cronie`) 후 `sudo systemctl enable --now crond` 실행한 뒤 위 cron 등록.
- **Oracle 2 (app 도메인)** 에서도 동일 방식으로 갱신 스크립트를 두고 cron 등록 가능. 이 경우 해당 노드의 compose·도메인에 맞게 스크립트 경로·도메인 변수를 조정한다.

---

## 5. AWS — app 가상호스트 (AWS 인스턴스 생성 후 적용)

**현재 설계**: app(프론트엔드)는 **AWS 1대(엣지 계층, Nginx+Frontend)** 에 두는 것으로 되어 있으나, **AWS 인스턴스는 아직 생성하지 않은 상태**이다. 리전도 문서에 명시되어 있지 않다.  
AWS EC2를 생성한 뒤 `docker-compose.aws-seoul-api.yml`로 Nginx+Frontend를 배포하고, 그때 **app.neekly-report.cloud** A 레코드를 해당 인스턴스 Public IP로 설정하면 된다.

### 5.1 conf.d (앱 가상호스트)

- **파일**: investment-infra **nginx/conf.d/app.conf**.
- `server_name app.EXAMPLE_DOMAIN;`, SSL 경로 `APP_DOMAIN` 플레이스홀더.
- **배포 시**: `app.EXAMPLE_DOMAIN` → `app.neekly-report.cloud`, `APP_DOMAIN` → `app.neekly-report.cloud` 치환.

### 5.2 Certbot 및 volume

- AWS 노드에서 `certbot certonly --standalone -d app.neekly-report.cloud` (또는 webroot 방식) 실행.
- docker-compose.aws-seoul-api.yml은 `./secrets/certs` → `/etc/nginx/certs` 마운트. 호스트에 `/etc/letsencrypt`를 쓰면 해당 경로를 `secrets/certs`에 복사하거나, compose에서 `/etc/letsencrypt:/etc/nginx/certs:ro` 로 바꾸고 conf에서 경로를 `/etc/nginx/certs/live/app.neekly-report.cloud/...` 로 지정.

---

## 6. 배포 시 플레이스홀더 치환

- **저장소**에는 `EXAMPLE_DOMAIN`, `API_DOMAIN`, `APP_DOMAIN` 만 두고, **실제 Public IP는 저장소에 기입하지 않는다.** (도메인 neekly-report.cloud는 이 문서에서 사용 도메인으로만 기재.)
- **치환 시점**: 노드에 SSH 접속한 뒤 배포 전에 수동 치환하거나, CD/스크립트에서 환경 변수(`DOMAIN=neekly-report.cloud`)로 `envsubst` 실행.
- **GitHub Variables**: `DEPLOY_HOST_ORACLE_KOREA` 등에 호스트명(예: api.neekly-report.cloud)을 넣을 수 있다. **호스트명 사용 시 해당 이름이 DNS로 해당 노드 Public IP로 풀이되어야** CD SSH 연결이 성공한다. ([08-devops-required-tokens-and-keys.md](08-devops-required-tokens-and-keys.md) 참조.)

---

## 7. 보안 유의사항

- **Public 저장소**: 실제 Public IP는 코드·설정 파일·이슈/PR에 넣지 않는다. Cloudflare 대시보드·GitHub Secrets/Variables·노드 `.env`·로컬 SSH config에서만 사용.
- **문서**: 이 문서는 사용 도메인 **neekly-report.cloud** 기준으로 작성되었으며, IP는 예시 자리만 두고 실제 값은 기입하지 않는다.
- 전체 규칙: [03-public-repository-security-rules.md](../07-security/03-public-repository-security-rules.md).
