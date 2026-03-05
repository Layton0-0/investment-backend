# 로컬 개발 환경 구축 가이드 (완전판)

## 개요

이 가이드는 완전히 새로운 Windows 컴퓨터에서도 **한두 개 파일만 따라하면** Investment Choi 프로젝트의 로컬 개발 환경을 구축할 수 있도록 설계되었습니다.

**WSL2 + Docker Compose** 조합을 사용하여 모든 인프라를 자동으로 설정합니다.

## 빠른 시작 (완전 자동화)

### 전체 자동화 스크립트 (단일 실행 파일)

```powershell
# 프로젝트 루트에서 실행 (하나의 명령으로 모든 것 자동화!)

# 방법 A: .cmd 사용 (실행 정책 변경 없이 실행)
.\scripts\setup-local-complete.cmd

# 방법 B: .ps1 직접 실행 (실행 정책 허용이 필요한 경우 아래 먼저 실행)
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
.\scripts\setup-local-complete.ps1
```

**실행 정책 오류가 나는 경우** (`이 시스템에서 스크립트를 실행할 수 없으므로...`):  
위 **방법 A**처럼 `.\scripts\setup-local-complete.cmd` 를 사용하면 실행 정책 설정 없이 실행됩니다. 또는 한 번만 **방법 B**의 `Set-ExecutionPolicy` 를 실행한 뒤 `.ps1` 을 사용해도 됩니다.

이 **하나의 스크립트**가 다음을 **완전 자동으로** 수행합니다:
1. **사전 요구사항 확인 및 자동 설치** (WSL2, Docker Desktop, Java, Python, Git)
2. Gradle Wrapper 설정
3. Docker Compose로 인프라 시작 (MariaDB, Redis, AI 서비스)
4. Python 가상환경 생성 및 패키지 설치
5. `.env` 파일 템플릿 생성
6. MCP 설정 파일 템플릿 생성

**주의사항:**
- WSL2 설치 시 **재부팅이 필요**할 수 있습니다. 재부팅 후 스크립트를 다시 실행하세요.
- Docker Desktop 설치 후 **수동으로 Docker Desktop을 시작**해야 합니다.
- 일부 설치 후 **터미널을 재시작**해야 PATH가 업데이트됩니다 (스크립트가 안내).

### 방법 2: 단계별 수동 설정

아래 단계를 순서대로 따라하세요.

## 1단계: 사전 요구사항 확인

### 1-1. Windows 11 확인

```powershell
# Windows 버전 확인
winver
```

Windows 11 (빌드 22000 이상) 권장. Windows 10도 가능하지만 WSL2 설치가 필요합니다.

### 1-2. 사전 요구사항 자동 설치

**`setup-local-complete.ps1` 스크립트가 자동으로 설치합니다:**

스크립트 실행 시 다음을 자동으로 설치합니다:
- **WSL2** (재부팅 필요할 수 있음)
- **Docker Desktop** (winget 사용)
- **Java 17** (winget 사용)
- **Python 3.11** (winget 사용)
- **Git** (winget 사용, 선택사항)

**주의사항:**
- **winget 필요**: Windows 10/11에 기본 포함되어 있지만, 없으면 [Windows App Installer](https://aka.ms/getwinget) 설치 필요
- **관리자 권한**: WSL2 설치 시 관리자 권한 필요 (스크립트가 안내)
- **재부팅**: WSL2 설치 후 재부팅 필요할 수 있음 (스크립트가 안내)
- **터미널 재시작**: 설치 후 PATH 업데이트를 위해 터미널 재시작 필요 (스크립트가 안내)

### 1-3. 수동 설치 (선택사항)

자동 설치가 실패하거나 수동으로 설치하려면:

#### WSL2 설치

```powershell
# 관리자 권한으로 실행
wsl --install

# 설치 후 재부팅 필요
```

#### Docker Desktop 설치

```powershell
# winget 사용 (자동)
winget install --id Docker.DockerDesktop

# 또는 수동 다운로드: https://www.docker.com/products/docker-desktop/
```

#### Java 17+ 설치

```powershell
# winget 사용 (자동)
winget install --id EclipseAdoptium.Temurin.17.JDK

# 또는 수동 다운로드: https://adoptium.net/
```

#### Python 3.11+ 설치

```powershell
# winget 사용 (자동)
winget install --id Python.Python.3.11

# 또는 수동 다운로드: https://www.python.org/downloads/
# 설치 시 "Add Python to PATH" 체크 필수!
```

### 1-4. 사전 요구사항 확인

```powershell
# 자동 확인 스크립트 실행
.\scripts\check-prerequisites.ps1
```

모든 항목이 [OK]로 표시되면 다음 단계로 진행하세요.

## 2단계: 프로젝트 설정

### 2-1. Gradle Wrapper 설정

프로젝트는 Gradle Wrapper를 사용합니다. 전역 Gradle 설치가 필요 없습니다.

```powershell
# Gradle Wrapper JAR 다운로드 (최초 1회)
.\scripts\setup-gradle-wrapper.ps1

# 확인
.\gradlew.bat -v
```

### 2-2. Docker Compose로 인프라 시작

프로젝트 루트의 `docker-compose.yml`을 사용하여 모든 인프라를 시작합니다.

```powershell
# Docker Desktop이 실행 중인지 확인
docker ps

# Docker Compose 시작
docker compose up -d

# 컨테이너 상태 확인
docker compose ps

# 로그 확인
docker compose logs -f
```

**시작되는 서비스:**
- **MariaDB** (포트 3306): 데이터베이스
- **Redis** (포트 6379): 캐시
- **AI Service** (포트 8000): 예측 서비스

**연결 테스트:**

```powershell
# MariaDB 연결 테스트
docker compose exec mariadb mysql -u local_maria -plocal_maria_pass investment_portfolio -e "SELECT 1;"

# Redis 연결 테스트
docker compose exec redis redis-cli ping
# 응답: PONG
```

### 2-3. Python 가상환경 설정

```powershell
# Python 가상환경 생성 및 패키지 설치
.\scripts\setup-python-env.ps1
```

또는 수동으로:

```powershell
cd ai-service\prediction-service
python -m venv venv
.\venv\Scripts\Activate.ps1
pip install --upgrade pip
pip install -r requirements.txt
```

## 3단계: 환경 변수 설정

### 3-1. .env 파일 생성

프로젝트 루트에 `.env` 파일을 생성합니다. 모든 API 키와 비밀번호는 이 파일에만 입력합니다.

```powershell
# .env.template이 있으면 복사
Copy-Item .env.template .env

# .env 파일 편집 (메모장 또는 원하는 에디터)
notepad .env
```

### 3-2. 필수 환경 변수

`.env` 파일에 다음 변수들을 설정하세요:

```env
# 데이터베이스 (Docker Compose 기본값 사용)
SPRING_DATASOURCE_URL=jdbc:mariadb://localhost:3306/investment_portfolio?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Seoul
SPRING_DATASOURCE_USERNAME=local_maria
SPRING_DATASOURCE_PASSWORD=local_maria_pass

# Redis (Docker Compose 기본값 사용)
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# 보안 키 (생성 필요)
INVESTMENT_ENCRYPTION_KEY=<Base64 인코딩된 32바이트 키>
INVESTMENT_JWT_SECRET=<최소 256비트 랜덤 문자열>

# 한국투자증권 API (선택사항)
KOREA_INVESTMENT_APP_KEY=<앱키>
KOREA_INVESTMENT_APP_SECRET=<앱시크릿>

# 데이터 수집 API (선택사항)
DART_API_KEY=<DART API 키>
KRX_AUTH_KEY=<KRX 인증키>   # KRX Open API는 서비스별 이용신청 필요. [KRX API 필요 목록](./04-krx-api-required.md) 참조.
DATA_COLLECTION_INTERNAL_KEY=<내부 API 키>

# US 시장 일별 시세 (yfinance). 로보 백테스트·US 일봉 수집용
# 1) .\scripts\setup-us-daily-collect.ps1 실행 → yfinance 설치 및 테스트
# 2) 앱을 프로젝트 루트에서 실행하면 기본 경로(scripts/us_daily_collector.py) 사용
# 다른 CWD에서 실행 시: US_YFINANCE_SCRIPT_PATH=<us_daily_collector.py 절대경로>, US_PYTHON_COMMAND=python
# US_SYMBOLS=AAPL,MSFT,GOOGL,SPY,TLT,BIL,... (기본값에 로보용 SPY,TLT,BIL 포함)
```

#### 3-2-1. US 일봉 수집 (yfinance) — 로보 백테스트용

로보어드바이저 백테스트에서 US 일봉(SPY, TLT 등)이 필요할 때:

**방법 A — Docker Compose (권장)**  
1. `docker-compose up -d us-daily-collector`로 US 일봉 수집 서비스 기동.  
2. `.env`에 `US_COLLECTOR_URL=http://localhost:8001` 설정. (Spring이 같은 호스트에서 실행될 때)  
3. 앱 실행 후 백테스트 페이지 → 로보어드바이저 탭 → **US 일봉 수집** 버튼으로 기간 수집.

**방법 B — 로컬 Python 스크립트**  
1. **한 번만 설정**: 프로젝트 루트에서 `.\scripts\setup-us-daily-collect.ps1` 실행 → Python에 yfinance 설치 및 스크립트 테스트.  
2. **앱 실행**: 앱을 **프로젝트 루트**에서 실행(예: `.\gradlew bootRun`)하면 기본 경로 `scripts/us_daily_collector.py`가 사용됩니다.  
3. **수집**: 백테스트 페이지 → 로보어드바이저 탭 → **US 일봉 수집** 버튼으로 기간 수집.

다른 작업 디렉터리에서 앱을 실행하는 경우, 스크립트 설정 안내에서 출력되는 `US_YFINANCE_SCRIPT_PATH`(절대 경로)와 `US_PYTHON_COMMAND`를 `.env`에 설정하세요.

**US/KRX 수집이 0건일 때 확인할 env**

- **US 일별 수집**: Docker 사용 시 `.env`에 `US_COLLECTOR_URL=http://data-collector:8001`(Compose 내부) 또는 `http://localhost:8001`(호스트에서 Backend 실행 시). 미설정이면 로그에 `US 시장 일별 수집 스킵: collector-url·yfinance-script-path 미설정` WARN 출력. `US_SYMBOLS`가 비어 있으면 수집 대상 없음으로 스킵.
- **KRX 일별 수집**: `KRX_AUTH_KEY`(또는 `investment.data.krx.auth-key`) 미설정 시 로그에 `KRX AUTH_KEY 미설정: 조회 스킵` WARN 출력. **KRX 실패 원인**: (1) AUTH_KEY 미설정·만료, (2) 한투 폴백 사용 시 해당 API 키·연결 상태 확인. 시그널 0건일 때 전체 점검 순서는 [13-manual-operator-tasks.md §1.11 시그널이 0건으로 보일 때 점검 순서](../06-deployment/13-manual-operator-tasks.md) 참고.
- **헬스로 한눈에 확인**: `GET /actuator/health` 응답의 `dataCollectionHealthIndicator` detail에서 `usCollectorConfigured`, `krxAuthConfigured` 여부 확인 (값 자체는 노출하지 않음).

**보안 키 생성 방법:**

```powershell
# PowerShell에서
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))

# 또는 OpenSSL 사용 (WSL2에서)
openssl rand -base64 32
```

### 3-3. .env 파일 사용

Spring Boot 애플리케이션은 프로젝트 루트의 `.env` 파일을 자동으로 읽습니다 (IntelliJ/Cursor에서).

터미널에서 실행할 때는:

```powershell
# .env 로드 후 실행
.\scripts\bootRun-with-env.ps1
```

### 3-4. 모의계좌 자동투자 켜기 (선택사항)

모의계좌로 4단계 파이프라인(유니버스→시그널→자금관리→매매실행)을 **스케줄러가 자동 실행**하도록 하려면:

1. **회원가입/마이페이지**에서 서버 타입 **모의투자** 선택 후 계좌인증 완료.
2. **거래설정**(마이페이지 또는 대시보드) 저장 — 자동투자 대상은 거래설정이 있는 계좌만 해당.
3. `.env` 또는 환경 변수에 다음 설정 후 앱 재기동:
   - `PIPELINE_AUTO_EXECUTE=true` — 실제 주문 실행(미설정 시 기본값 false, dry-run만).
   - `PIPELINE_SCHEDULER_DEFAULT_CAPITAL=금액(원)` — 스케줄러용 총자산(0이면 파이프라인 실행 스킵).
4. **실행 스케줄**: 기본 09:10 KST 1회 (`application.yml`의 `investment.pipeline.execution-schedule-cron` 참조). 청산은 장중 5분마다(`exit-schedule-cron`).

**자동투자 실제 주문**을 켜려면 `PIPELINE_AUTO_EXECUTE=true` 설정. 프로세스 플로우·스케줄·체크리스트 상세는 [자동투자 전략 명세 §6.2](../02-architecture/12-auto-investment-strategy.md#62-자동투자-프로세스-플로우) 참조.

실계좌 전환 전 **모의 2주 테스트** 권장. 실계좌용 KIS URL·Throttling·토큰 갱신 등은 [자동투자 전략 명세](../02-architecture/12-auto-investment-strategy.md) §8 및 [로드맵](../roadmap.md) Phase 7 참조.

### 3-5. 모의계좌 실제 실행 검증

모의계좌에서 실제 주문이 나가는지 검증하려면:

1. **설정 (/settings)**: 모의계좌만 자동 매매 ON, 거래 설정(최대 투자금액·단기/중기/장기 비율) 저장.
2. **환경 변수**: `PIPELINE_AUTO_EXECUTE=true`, `PIPELINE_ALLOW_REAL_EXECUTION=false`(기본값, 실전 계좌 자동 실행 미허용).
3. **스케줄**: 파이프라인 실행 09:10 KST, 청산 장중 5분마다, 체결 확인 매분. 로그에서 주문 요청·서버 타입(모의) 확인.
4. **모의 Rate Limit**: 한국투자증권 모의투자 1초당 2건 제한. 동시 다수 종목 주문 시 대기 발생할 수 있음.

### 3-6. 로컬 Docker 풀스택 배포 시 모의투자 실제 실행

**investment-infra**의 `docker-compose.local-full.yml`로 로컬 풀스택을 띄우면 **모의투자 실제 실행**이 기본 적용됩니다.

- **Backend** 서비스에 다음 환경 변수가 설정됨:
  - `PIPELINE_AUTO_EXECUTE=true` — dry-run 비활성화, 스케줄러/수동 트리거 시 실제 주문 실행
  - `PIPELINE_ALLOW_REAL_EXECUTION=false` — 실전 계좌(serverType=0) 자동 실행 차단, **모의계좌만** 주문 실행
  - `PIPELINE_SCHEDULER_DEFAULT_CAPITAL=10000000` — 계정별 최대투자금 미설정 시 스케줄러용 기본 자본(1천만 원)

**배포 절차** (프로젝트 루트 또는 investment-infra):

```powershell
# Backend JAR 빌드 후 풀스택 기동
cd investment-infra
.\scripts\local-up.ps1
# 또는: docker compose -f docker-compose.local-full.yml up -d --build
```

접속: **http://localhost** (프론트), **http://localhost:8080** 또는 **http://localhost/api** (백엔드 API).  
모의계좌 인증·거래설정(자동 매매 ON, 최대 투자금 등) 후 09:10 KST 스케줄 또는 `POST /api/v1/trigger/auto-buy` 수동 트리거 시 **실제 모의 주문**이 실행됩니다.

## 4단계: MCP 설치 (선택사항)

MCP(Model Context Protocol)는 Cursor에서 프로젝트를 더 효율적으로 개발할 수 있게 해주는 도구입니다.

### 4-1. MCP 설정 파일 위치

MCP 설정 파일: `C:\Users\<사용자명>\.cursor\mcp.json`

### 4-2. 한국투자증권 MCP (KIS Code Assistant)

1. [KIS Code Assistant MCP 페이지](https://smithery.ai/server/@KISOpenAPI/kis-code-assistant-mcp) 접속
2. **AUTO / Cursor** 선택
3. **One-Click Install** 클릭
4. Cursor에서 **Install** 클릭
5. Cursor 재시작

**사용 방법:**
- Cursor 채팅에서 "한국투자증권 주식 현재가 조회 API 코드 보여줘" 같은 질문 가능
- MCP가 API 스펙과 예제 코드를 자동으로 제공

### 4-3. Workspace MCP (Filesystem)

프로젝트 전체 파일 시스템에 접근할 수 있게 해줍니다.

`mcp.json`에 다음 추가:

```json
{
  "filesystem": {
    "type": "stdio",
    "command": "npx",
    "args": [
      "-y",
      "@modelcontextprotocol/server-filesystem",
      "D:/works/pjt/investment-choi"
    ]
  }
}
```

**주의**: 프로젝트 경로를 실제 경로로 변경하세요.

### 4-4. GitHub MCP

GitHub 리포지토리 관리 및 코드 분석에 사용됩니다.

1. **GitHub Personal Access Token 생성**:
   - GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
   - "Generate new token (classic)" 클릭
   - 권한: `repo` (전체 리포지토리 접근)
   - 토큰 생성 후 복사

2. **환경 변수 설정**:
   ```powershell
   [System.Environment]::SetEnvironmentVariable("GITHUB_TOKEN", "your_token_here", "User")
   ```

3. **mcp.json에 추가**:
   ```json
   {
     "github": {
       "type": "stdio",
       "command": "npx",
       "args": [
         "-y",
         "@modelcontextprotocol/server-github"
       ],
       "env": {
         "GITHUB_PERSONAL_ACCESS_TOKEN": "${GITHUB_TOKEN}"
       }
     }
   }
   ```

4. **Cursor 재시작**

### 4-5. Notion MCP

Notion 페이지 읽기/쓰기 및 기획 문서 관리에 사용됩니다.

1. **Notion Integration 생성**:
   - https://www.notion.so/profile/integrations 접속
   - "New integration" 클릭
   - Integration 이름 입력 (예: "Cursor MCP")
   - Capabilities: Read content, Update content, Insert content
   - "Submit" 클릭
   - 생성된 "Internal Integration Token" 복사

2. **환경 변수 설정**:
   ```powershell
   [System.Environment]::SetEnvironmentVariable("NOTION_TOKEN", "your_token_here", "User")
   ```

3. **mcp.json에 추가**:
   ```json
   {
     "notion": {
       "type": "stdio",
       "command": "npx",
       "args": [
         "-y",
         "@notionhq/notion-mcp-server"
       ],
       "env": {
         "NOTION_TOKEN": "${NOTION_TOKEN}"
       }
     }
   }
   ```

4. **Notion 페이지 연결**:
   - 연결할 Notion 페이지 열기
   - 우측 상단 "..." 메뉴 → "Connections" → 생성한 Integration 선택

5. **Cursor 재시작**

### 4-6. MariaDB MCP (선택사항)

MariaDB에 직접 쿼리할 수 있게 해줍니다. 읽기 전용 모드로 사용합니다.

1. **MariaDB MCP 서버 설치**:
   ```powershell
   cd D:\works\tools
   git clone https://github.com/mariadb/mcp.git mariadb-mcp
   cd mariadb-mcp
   pip install uv
   uv lock
   uv sync
   ```

2. **.env 파일 생성** (`D:\works\tools\mariadb-mcp\.env`):
   ```env
   DB_HOST=localhost
   DB_PORT=3306
   DB_USER=local_maria
   DB_PASSWORD=local_maria_pass
   MCP_READ_ONLY=true
   ```

3. **mcp.json에 추가**:
   ```json
   {
     "local-maria": {
       "type": "stdio",
       "command": "uv",
       "args": [
         "run",
         "mcp-server-mariadb"
       ],
       "env": {
         "DB_HOST": "localhost",
         "DB_PORT": "3306",
         "DB_USER": "local_maria",
         "DB_PASSWORD": "local_maria_pass",
         "MCP_READ_ONLY": "true"
       }
     }
   }
   ```

4. **Cursor 재시작**

## 5단계: 애플리케이션 실행

### 5-1. Spring Boot 실행

**일반 실행 (포트 8083, IntelliJ 수동 확인용):**

```powershell
# .env 로드 후 실행
.\scripts\bootRun-with-env.ps1

# 또는 직접 실행
.\gradlew.bat bootRun
```

**Agent/Cursor 전용 서버 (포트 8084, 임시 확인용):**

```powershell
# 포트 8084로 실행
.\scripts\bootRun-agent.ps1

# 확인 후 반드시 종료 (Ctrl+C)
```

**주의**: 8084 서버는 확인을 마친 뒤 반드시 종료하세요. 테스트 실행 전에도 8084가 켜져 있으면 빌드 잠금으로 실패할 수 있습니다.

### 5-2. AI 서비스 실행

새 PowerShell 터미널에서:

```powershell
cd ai-service\prediction-service
.\venv\Scripts\Activate.ps1
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

### 5-3. 서비스 확인

```powershell
# Spring Boot 확인 (포트 8083)
curl http://localhost:8083/actuator/health

# AI 서비스 확인
curl http://localhost:8000/

# Swagger UI 확인
# 브라우저에서: http://localhost:8083/swagger-ui.html
```

## 6단계: 빠른 시작 체크리스트

다음 체크리스트를 따라 모든 설정이 완료되었는지 확인하세요:

- [ ] Windows 11 설치됨
- [ ] WSL2 설치 및 확인됨
- [ ] Docker Desktop 설치 및 실행 중
- [ ] Java 17+ 설치됨
- [ ] Python 3.11+ 설치됨
- [ ] Git 설치됨 (선택사항)
- [ ] Gradle Wrapper 설정됨
- [ ] Docker Compose로 인프라 시작됨 (MariaDB, Redis, AI 서비스)
- [ ] Python 가상환경 생성 및 패키지 설치됨
- [ ] .env 파일 생성 및 필수 변수 설정됨
- [ ] MCP 설정 완료 (선택사항)
- [ ] Spring Boot 실행 성공 (포트 8083)
- [ ] AI 서비스 실행 성공 (포트 8000)
- [ ] 서비스 연결 테스트 성공

## 주요 명령어 요약

| 목적 | 명령 | 포트/비고 |
|------|------|-----------|
| **사전 요구사항 확인** | `.\scripts\check-prerequisites.ps1` | - |
| **전체 자동화 설정** | `.\scripts\setup-local-complete.ps1` | - |
| **Docker Compose 시작** | `docker compose up -d` | - |
| **Docker Compose 중지** | `docker compose down` | - |
| **Docker 로그 확인** | `docker compose logs -f` | - |
| **일반 서버 실행** | `.\scripts\bootRun-with-env.ps1` | **8083** (local) |
| **Agent 전용 서버** | `.\scripts\bootRun-agent.ps1` | **8084** (임시, 확인 후 종료) |
| **빌드** | `.\gradlew.bat build` | - |
| **테스트** | `.\scripts\run-tests.ps1` | - |

## 다음 단계

1. ✅ 모든 서비스가 정상 실행되는지 확인
2. ✅ Spring Boot 애플리케이션과 AI 서비스 통신 테스트
3. ✅ 데이터베이스 연결 및 Redis 캐싱 테스트
4. ✅ API 엔드포인트 테스트 (Swagger UI 사용)
5. ✅ 통합 테스트 실행

## 문제 해결

자세한 트러블슈팅은 [부록 문서](./02-appendix.md)를 참고하세요.

주요 문제:
- 포트 충돌
- Docker 컨테이너 문제
- MCP 연결 실패
- 빌드 잠금 문제

### Spring Batch 메타데이터 테이블이 없을 때

**근본 원인**: Flyway 도입 시 기존 마이그레이션 SQL을 정리하면서 **BATCH_* 테이블을 생성하던 V20__spring_batch_metadata.sql을 삭제**했습니다. Flyway는 baseline 20으로 “이미 적용됨”만 기록하고, 버전 21 이상 스크립트만 실행하는데, 당시에는 V21이 없었기 때문에 **어떤 마이그레이션도 BATCH_* 테이블을 만들지 않는 상태**가 되었습니다. 여기에 `spring.batch.jdbc.initialize-schema: never` 설정으로 Spring Batch가 스키마를 자동 생성하지 않으므로, 테이블이 없는 DB에서는 배치 실행 시 `BATCH_JOB_INSTANCE` 없음 오류가 발생합니다.

**해결**: `db/migration/V21__spring_batch_metadata.sql`이 BATCH_* 테이블을, `V22__spring_batch_sequences.sql`이 MariaDB용 SEQUENCE(BATCH_JOB_SEQ, BATCH_JOB_EXECUTION_SEQ, BATCH_STEP_EXECUTION_SEQ)를 생성합니다. 앱을 **한 번 재기동**하면 Flyway가 V21·V22를 순서대로 실행합니다. `Unknown SEQUENCE: 'BATCH_JOB_SEQ'` 오류는 Spring Batch 5가 MariaDB 10.3+에서 네이티브 SEQUENCE를 사용하기 때문에 발생하며, V22 적용으로 해결됩니다.

### Flyway "Detected failed migration" / V22 SEQUENCE "out of range value"

**증상**: 기동 시 `FlywayValidateException: Detected failed migration to version 22` 또는 `Sequence 'BATCH_JOB_SEQ' has out of range value for options` 발생.

**원인**: 이전 기동에서 V22가 실패한 상태로 schema history에 남았거나, MariaDB SEQUENCE 옵션(MINVALUE 0, MAXVALUE 9223372036854775807)이 허용 범위를 벗어남.

**해결**:
- **로컬**: `application-local.yml`에 `spring.flyway.repair-on-validate-failure: true`가 설정되어 있으면, 검증 실패 시 자동으로 repair 후 migrate 재시도.
- V22는 MariaDB 규칙에 맞게 `MINVALUE 1`, `START WITH 1`, `MAXVALUE 9223372036854775806`으로 정의되어 있으며, 기존 잘못된 시퀀스는 `DROP SEQUENCE IF EXISTS` 후 재생성.

### DART/SEC 공시 수집과 NoClassDefFoundError

**역할 이전**: DART·SEC EDGAR 공시 수집 **배치는 이제 Python investment-data-collector**에서 수행한다. Spring에서는 해당 배치 Job·Trigger가 제거되어 더 이상 해당 경로로 실행되지 않는다.

**과거 증상**: Spring Batch에서 DART/SEC를 실행할 때 `NoClassDefFoundError: DartListResponseDto` / `SecSubmissionsResponseDto`가 났다면, 해당 배치는 이미 Python으로 이전되었으므로 **동일 오류는 Spring에서 재현되지 않는다**. (Python 수집기는 `POST /dart-collect`, `POST /sec-collect` 또는 `SCHEDULE_DART_SEC=1`로 동작.)

## 참고 문서

- [시스템 아키텍처](../02-architecture/01-system-architecture.md)
- [필수 기술 스펙](../02-architecture/10-essential-tech-spec.md)
- [API 가이드](../04-api/01-api-overview.md)
- [한국투자증권 API 가이드](../04-api/09-korea-investment-api-guide.md)
- [보안 설정 참조](../07-security/02-security-configuration-reference.md)

## 문서 변경 이력

| 버전 | 일자 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-30 | System | 통합 가이드 작성 (WSL2 + Docker Compose 중심) |
