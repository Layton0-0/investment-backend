# Windows 로컬 개발 환경 구축 가이드

## 개요

서버 배포 전 로컬 Windows PC에서 전체 시스템을 구축하고 테스트하기 위한 가이드입니다.

## 사전 확인 (이미 설치된 항목 확인)

### 1. Java 17 확인

```powershell
# Java 버전 확인
java -version

# Java 17 이상이 설치되어 있어야 함
# 예: openjdk version "17.0.x"
```

### 2. Gradle 확인 (Wrapper 사용, 전역 설치 불필요)

이 프로젝트는 **Gradle Wrapper**를 사용합니다. winget/Chocolatey 등으로 Gradle을 전역 설치할 필요 없이, 프로젝트 루트에서 아래만 하면 됩니다.

1. **Wrapper JAR 한 번만 준비** (최초 1회)
   ```powershell
   # 프로젝트 루트에서 실행
   .\scripts\setup-gradle-wrapper.ps1
   ```
   스크립트가 `gradle\wrapper\gradle-wrapper.jar`를 다운로드합니다. 실패 시 [GitHub v7.6.3 wrapper](https://github.com/gradle/gradle/raw/v7.6.3/gradle/wrapper/gradle-wrapper.jar)에서 직접 받아 `gradle\wrapper\gradle-wrapper.jar`에 저장하면 됩니다.

2. **빌드/실행**
   ```powershell
   .\gradlew.bat -v          # 버전 확인
   .\gradlew.bat build       # 빌드
   .\gradlew.bat bootRun     # 실행
   ```

3. **테스트**: `.\gradlew test` (build 잠금 시 **표준**: `.\scripts\run-tests.ps1` 또는 `$env:GRADLE_UNIQUE_BUILD_DIR='1'; .\gradlew test`)

### 3. Git 확인

```powershell
# Git 버전 확인
git --version
```

## 필수 설치 항목

### 1. MariaDB 11.8.5+ 설치

#### 방법 1: 공식 설치 프로그램 (권장)

1. **다운로드**
   - https://mariadb.org/download/
   - Windows용 MSI 설치 프로그램 다운로드
   - 버전: 11.8.5 이상

2. **설치**
   ```powershell
   # 설치 프로그램 실행 후 다음 설정:
   # - Root 비밀번호: root (또는 원하는 비밀번호)
   # - 포트: 3306 (기본값)
   # - 서비스로 실행: 예
   ```

3. **데이터베이스 및 사용자 생성**
   ```powershell
   # MariaDB에 접속 (설치 시 설정한 root 비밀번호 사용)
   mysql -u root -p
   ```

   ```sql
   -- 데이터베이스 생성
   CREATE DATABASE investment CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   
   -- 사용자 생성
   CREATE USER 'investment'@'localhost' IDENTIFIED BY 'password';
   
   -- 권한 부여
   GRANT ALL PRIVILEGES ON investment.* TO 'investment'@'localhost';
   FLUSH PRIVILEGES;
   
   -- 확인
   SHOW DATABASES;
   EXIT;
   ```

4. **연결 테스트**
   ```powershell
   # 명령줄에서 연결 테스트
   mysql -u investment -p -h localhost investment
   # 비밀번호: password
   ```

#### 방법 2: Docker 사용 (선택)

```powershell
# Docker Desktop이 설치되어 있다면
docker run -d `
  --name mariadb `
  -p 3306:3306 `
  -e MYSQL_ROOT_PASSWORD=root `
  -e MYSQL_DATABASE=investment `
  -e MYSQL_USER=investment `
  -e MYSQL_PASSWORD=password `
  mariadb:11.8.5
```

### 2. Redis 설치

#### 방법 1: WSL2 + Redis (권장)

1. **WSL2 설치 확인**
   ```powershell
   # WSL2 설치 여부 확인
   wsl --version
   
   # WSL2가 없으면 설치
   wsl --install
   # 재부팅 필요
   ```

2. **WSL2에서 Redis 설치**
   ```bash
   # WSL2 Ubuntu 터미널에서 실행
   sudo apt update
   sudo apt install -y redis-server
   
   # Redis 설정
   sudo nano /etc/redis/redis.conf
   # maxmemory 2gb
   # maxmemory-policy allkeys-lru
   
   # Redis 서비스 시작
   sudo service redis-server start
   sudo service redis-server enable
   
   # 연결 테스트
   redis-cli ping
   # 응답: PONG
   ```

3. **Windows에서 Redis 접속**
   - WSL2의 Redis는 `localhost:6379`로 접속 가능

#### 방법 2: Memurai (Windows 네이티브 Redis)

1. **다운로드**
   - https://www.memurai.com/get-memurai
   - Windows용 Redis 호환 서버

2. **설치**
   - 설치 프로그램 실행
   - 기본 설정으로 설치 (포트 6379)

3. **서비스 시작**
   ```powershell
   # Windows 서비스로 자동 시작됨
   # 수동 시작/중지
   net start Memurai
   net stop Memurai
   ```

#### 방법 3: Docker 사용 (선택)

```powershell
docker run -d `
  --name redis `
  -p 6379:6379 `
  redis:7-alpine
```

### 3. Python 3.11+ 설치

1. **다운로드**
   - https://www.python.org/downloads/
   - Python 3.11 이상 다운로드 (예: 3.11.9, 3.12.x)

2. **설치**
   ```powershell
   # 설치 프로그램 실행 시:
   # ✅ "Add Python to PATH" 체크 필수!
   # ✅ "Install for all users" 선택 (선택사항)
   ```

3. **설치 확인**
   ```powershell
   # Python 버전 확인
   python --version
   # 예: Python 3.11.9
   
   # pip 확인
   pip --version
   ```

4. **가상환경 생성 및 패키지 설치**
   ```powershell
   # 프로젝트 루트로 이동
   cd d:\works\pjt\investment-choi
   
   # AI 서비스 디렉토리로 이동
   cd ai-service\prediction-service
   
   # 가상환경 생성
   python -m venv venv
   
   # 가상환경 활성화
   .\venv\Scripts\Activate.ps1
   # 만약 실행 정책 오류가 나면:
   # Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
   
   # 패키지 설치
   pip install --upgrade pip
   pip install -r requirements.txt
   
   # PyTorch CPU 버전 설치 (requirements.txt에 포함되어 있지만 확인)
   pip install torch --index-url https://download.pytorch.org/whl/cpu
   ```

5. **설치 확인**
   ```powershell
   # FastAPI 설치 확인
   python -c "import fastapi; print(fastapi.__version__)"
   
   # PyTorch 설치 확인
   python -c "import torch; print(torch.__version__)"
   ```

### 4. Docker Desktop (선택, 권장)

MariaDB와 Redis를 Docker로 실행하려는 경우:

1. **다운로드**
   - https://www.docker.com/products/docker-desktop/
   - Docker Desktop for Windows

2. **설치**
   - 설치 프로그램 실행
   - WSL2 백엔드 사용 (권장)

3. **확인**
   ```powershell
   docker --version
   docker-compose --version
   ```

## 설치 스크립트 (PowerShell)

전체 설치를 자동화하는 스크립트:

```powershell
# install-local-env.ps1
# 관리자 권한으로 실행 권장

Write-Host "=== Investment Choi 로컬 환경 구축 ===" -ForegroundColor Green

# 1. Java 확인
Write-Host "`n[1/5] Java 확인 중..." -ForegroundColor Yellow
$javaVersion = java -version 2>&1 | Select-String "version"
if ($javaVersion) {
    Write-Host "✅ Java 설치됨: $javaVersion" -ForegroundColor Green
} else {
    Write-Host "❌ Java가 설치되지 않았습니다." -ForegroundColor Red
    Write-Host "   Java 17 이상을 설치해주세요: https://adoptium.net/" -ForegroundColor Yellow
    exit 1
}

# 2. Gradle 확인
Write-Host "`n[2/5] Gradle 확인 중..." -ForegroundColor Yellow
$gradleVersion = .\gradlew.bat -v 2>&1 | Select-String "Gradle"
if ($gradleVersion) {
    Write-Host "✅ Gradle Wrapper 사용 가능" -ForegroundColor Green
} else {
    Write-Host "⚠️  Gradle Wrapper를 찾을 수 없습니다." -ForegroundColor Yellow
}

# 3. MariaDB 확인
Write-Host "`n[3/5] MariaDB 확인 중..." -ForegroundColor Yellow
try {
    $mariadb = Get-Service -Name "MariaDB*" -ErrorAction SilentlyContinue
    if ($mariadb) {
        Write-Host "✅ MariaDB 서비스 발견: $($mariadb.Name)" -ForegroundColor Green
    } else {
        Write-Host "❌ MariaDB가 설치되지 않았습니다." -ForegroundColor Red
        Write-Host "   설치 가이드를 참고하세요." -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ MariaDB 확인 실패" -ForegroundColor Red
}

# 4. Redis 확인
Write-Host "`n[4/5] Redis 확인 중..." -ForegroundColor Yellow
try {
    $redis = redis-cli ping 2>&1
    if ($redis -eq "PONG") {
        Write-Host "✅ Redis 실행 중" -ForegroundColor Green
    } else {
        Write-Host "❌ Redis가 실행되지 않았습니다." -ForegroundColor Red
        Write-Host "   WSL2 또는 Memurai를 설치하세요." -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ Redis 확인 실패 (redis-cli가 없습니다)" -ForegroundColor Red
}

# 5. Python 확인
Write-Host "`n[5/5] Python 확인 중..." -ForegroundColor Yellow
try {
    $pythonVersion = python --version 2>&1
    if ($pythonVersion -match "Python 3\.(1[1-9]|[2-9][0-9])") {
        Write-Host "✅ $pythonVersion 설치됨" -ForegroundColor Green
        
        # 가상환경 확인
        if (Test-Path "ai-service\prediction-service\venv") {
            Write-Host "✅ Python 가상환경 존재" -ForegroundColor Green
        } else {
            Write-Host "⚠️  Python 가상환경이 없습니다. 생성 중..." -ForegroundColor Yellow
            Set-Location "ai-service\prediction-service"
            python -m venv venv
            .\venv\Scripts\Activate.ps1
            pip install --upgrade pip
            pip install -r requirements.txt
            Set-Location ..\..
            Write-Host "✅ 가상환경 생성 및 패키지 설치 완료" -ForegroundColor Green
        }
    } else {
        Write-Host "❌ Python 3.11 이상이 필요합니다." -ForegroundColor Red
        Write-Host "   현재: $pythonVersion" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ Python이 설치되지 않았습니다." -ForegroundColor Red
    Write-Host "   Python 3.11 이상을 설치해주세요: https://www.python.org/downloads/" -ForegroundColor Yellow
}

Write-Host "`n=== 확인 완료 ===" -ForegroundColor Green
Write-Host "다음 단계:" -ForegroundColor Cyan
Write-Host "1. MariaDB 데이터베이스 및 사용자 생성" -ForegroundColor White
Write-Host "2. Redis 서비스 시작" -ForegroundColor White
Write-Host "3. Spring Boot 애플리케이션 실행: .\gradlew.bat bootRun" -ForegroundColor White
Write-Host "4. AI 서비스 실행: cd ai-service\prediction-service && .\venv\Scripts\Activate.ps1 && uvicorn app.main:app --reload" -ForegroundColor White
```

## 빠른 시작 가이드

### 1. 키/시크릿 설정 (.env — 한 곳에서만 입력)

**모든 API 키·비밀번호·시크릿은 `.env` 한 파일에서만 입력하면 됩니다.**

1. **`.env` 파일 준비**
   - 프로젝트 루트에 `.env`가 있으면 그대로 사용. 없으면 `application-local.yml`에서 참조하는 변수명을 기준으로 생성.
   - 변수명·기본값은 `src/main/resources/application-local.yml` 참고.

2. **`.env` 파일을 열어 필요한 값만 입력**
   - DB: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` (로컬 기본: `investment_portfolio`, `local_maria`, `local_maria_pass`)
   - Redis(없으면 비움): `REDIS_PASSWORD`
   - 보안(필수): `INVESTMENT_ENCRYPTION_KEY`, `INVESTMENT_JWT_SECRET` (생성: `openssl rand -base64 32`)
   - 한국투자증권(선택): `KOREA_INVESTMENT_APP_KEY`, `KOREA_INVESTMENT_APP_SECRET`
   - 데이터 수집(선택): `DART_API_KEY`, `KRX_AUTH_KEY`, `DATA_COLLECTION_INTERNAL_KEY`

3. **실행 방법**
   - **IntelliJ / IDE**: Run Configuration의 **Working directory**를 프로젝트 루트(`investment-choi`)로 두면, 앱 시작 시 **프로젝트 루트의 `.env`를 자동으로 읽어** 환경 변수로 사용합니다. (EnvFile 플러그인 없이 동작)
   - **터미널**: `.env`를 로드한 뒤 실행하려면 `.\scripts\bootRun-with-env.ps1` 사용.

- **주의**: `.env`는 Git에 포함되지 않습니다. 키는 `.env`에만 입력하고, `application.yml`은 수정하지 않아도 됩니다.

### 2. 데이터베이스 스키마 생성

```powershell
# Spring Boot 애플리케이션 실행 시 자동으로 스키마 생성됨
# 또는 수동으로 SQL 스크립트 실행
mysql -u investment -p investment < docs/05-database/schema.sql
```

### 3. Spring Boot 애플리케이션 실행

```powershell
# 프로젝트 루트에서 — .env 로드 후 실행 (권장)
.\scripts\bootRun-with-env.ps1

# 또는 .env 없이 실행 (기본값/시스템 환경변수 사용)
.\gradlew.bat bootRun

# 빌드 후 실행
.\gradlew.bat build
java -jar build\libs\investment-choi-2.0.0.jar
```

### 3-1. Cursor/Agent 전용 서버 (포트 8084)

동일 DB/Redis를 쓰면서 **별도 포트(8084)** 로 서버를 띄워 Cursor/Agent에서 확인할 때 사용합니다. 기존 로컬 서버(8083)와 충돌하지 않습니다.

- **8083**: IntelliJ에서 수동 실행·확인용 (일상 개발용).
- **8084**: Cursor/Agent로 확인할 때만 임시로 사용. **확인을 마친 뒤에는 반드시 8084 서버를 종료**한다.

```powershell
# 방법 1: 스크립트 사용 (프로젝트 루트에서)
.\scripts\bootRun-agent.ps1

# 방법 2: Gradle 직접 실행
.\gradlew.bat bootRun --args="--spring.profiles.active=local,local-agent"
```

- **포트**: 8084  
- **로그 파일**: `logs/investment-choi-agent.log`  
- **프로파일**: `local` 설정을 그대로 쓰고, 포트와 로그만 `local-agent`에서 덮어씀.
- **빌드 잠금 회피**: `bootRun-agent.ps1`은 내부적으로 `GRADLE_UNIQUE_BUILD_DIR=1`을 설정하여, 기존 `build` 디렉터리가 잠겨 있어도 임시 디렉터리에 빌드 후 실행합니다.
- **종료**: 8084로 띄운 터미널에서 `Ctrl+C`로 프로세스를 종료한다.

### 3-2. 빌드·테스트 시 주의사항 (build 잠금 회피)

Windows 등에서 `build` 디렉터리가 다른 프로세스에 의해 잠겨 있으면 `Failed to clean up stale outputs` 등으로 빌드/테스트가 실패할 수 있습니다. 아래 **표준 방식**을 사용합니다.

| 목적 | 표준 명령 | 비고 |
|------|-----------|------|
| **테스트** | `.\scripts\run-tests.ps1` | `GRADLE_UNIQUE_BUILD_DIR=1`로 임시 디렉터리에 빌드 후 테스트 |
| **테스트 (환경변수 직접)** | `$env:GRADLE_UNIQUE_BUILD_DIR='1'; .\gradlew test` | 스크립트 없이 동일 동작 |
| **Agent 전용 서버 실행** | `.\scripts\bootRun-agent.ps1` | 포트 8084, 동일하게 빌드 잠금 회피 |

- **일반 빌드/실행** (`.\gradlew build`, `.\gradlew bootRun`): 잠금 없을 때는 그대로 사용. 잠금 발생 시 위 스크립트 또는 `$env:GRADLE_UNIQUE_BUILD_DIR='1'` 설정 후 실행.

### 4. AI 서비스 실행

```powershell
# 새 PowerShell 창에서
cd ai-service\prediction-service

# 가상환경 활성화
.\venv\Scripts\Activate.ps1

# FastAPI 서버 실행
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# 또는 Python으로 직접 실행
python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

### 5. 서비스 확인

```powershell
# Spring Boot 확인 (로컬 포트 8083)
curl http://localhost:8083/actuator/health

# AI 서비스 확인
curl http://localhost:8000/

# Swagger UI 확인
# 브라우저에서: http://localhost:8083/swagger-ui.html
```

### 5-1. 대시보드 사용 전 확인 사항 (입력해야 하는 설정값)

대시보드(계좌 요약·잔고·보유·주문)를 오류 없이 사용하려면 아래 설정이 선행되어야 합니다.

| 구분 | 내용 |
|------|------|
| **계좌·API** | API 키(앱키·시크릿), 서버 타입(모의/실전), 계좌번호, 계좌인증(접근 토큰 발급). 회원가입·마이페이지에서 입력. |
| **거래 설정** | 최대/최소 투자금액, 기본 통화, 자동매매 여부, 리스크 레벨. **한 번도 저장하지 않으면** DB에 거래 설정 행이 없어, 대시보드에서는 "거래 설정" 카드만 비표시됨(잔고·보유·주문은 정상 표시). 설정 화면(마이페이지 또는 `PUT /api/v1/settings/{accountNo}`)에서 **한 번 이상 저장**하면 해당 계좌에 대한 거래 설정이 생성됨. |

- 거래 설정 미저장 상태에서도 대시보드는 동작하며, 거래 설정 카드만 숨겨짐. "설정에서 거래 설정을 등록해 주세요" 안내는 설정 화면(마이페이지)에서 진행하면 됨.

### 6. 개발 시 표준 명령 요약

| 목적 | 명령 | 포트/비고 |
|------|------|-----------|
| 일반 서버 실행 (IntelliJ 수동 확인용) | `.\gradlew.bat bootRun` | **8083** (local), 일상 개발·수동 확인용 |
| Agent/Cursor 전용 서버 (임시) | `.\scripts\bootRun-agent.ps1` | **8084**, 확인 후 **반드시 종료** (Ctrl+C) |
| 빌드 | `.\gradlew.bat build` | 잠금 시 `$env:GRADLE_UNIQUE_BUILD_DIR='1'` 선 설정 |
| 테스트 | `.\scripts\run-tests.ps1` 또는 `.\gradlew test` | 잠금 시 run-tests.ps1 권장 |
| Gradle 버전 확인 | `.\gradlew.bat -v` | Wrapper 사용 |

- **8083**: IntelliJ에서 수동 실행·확인용. **8084**: Cursor/Agent 확인 시에만 임시 사용하고, 확인 끝나면 8084 종료.
- **로컬 프로파일**: `local`(기본 8083), `local-agent`(8084). 로깅·민감정보 마스킹 규칙은 [보안 설정 참조](../07-security/02-security-configuration-reference.md#로깅-시-민감정보-마스킹-개발-규칙) 참조.

## Docker Compose 사용 (선택)

모든 서비스를 Docker로 실행하려면:

```yaml
# docker-compose.yml (프로젝트 루트에 생성)
version: '3.8'

services:
  mariadb:
    image: mariadb:11.8.5
    container_name: investment-mariadb
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: investment
      MYSQL_USER: investment
      MYSQL_PASSWORD: password
    volumes:
      - mariadb_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: investment-redis
    ports:
      - "6379:6379"
    command: redis-server --maxmemory 2gb --maxmemory-policy allkeys-lru
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  mariadb_data:
```

```powershell
# Docker Compose 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f

# 중지
docker-compose down
```

## 트러블슈팅

### Agent 테스트 실행 전 확인

- **8084 포트**: Cursor/Agent로 서버를 띄웠다면 **테스트 실행 전 8084 서버를 반드시 종료**한다. (해당 터미널에서 Ctrl+C.) 8084가 켜져 있으면 build/ 또는 임시 빌드 잠금으로 테스트가 실패할 수 있다.
- **임시 빌드 삭제 오류**: Windows에서 `Unable to delete directory ... test-results\test\binary` 발생 시, `.\scripts\run-tests.ps1 -NoUniqueDir` 또는 `.\scripts\run-tests-with-coverage.ps1 -NoUniqueDir` 로 **프로젝트 build 폴더**를 사용해 실행한다. (build 폴더는 IntelliJ와 공유되므로, 테스트 후 IntelliJ에서 빌드하면 갱신된다.)

### MariaDB 연결 실패

```powershell
# 서비스 상태 확인
Get-Service -Name "MariaDB*"

# 서비스 시작
Start-Service -Name "MariaDB*"

# 방화벽 확인
netsh advfirewall firewall show rule name="MariaDB"
```

### Redis 연결 실패

```powershell
# WSL2에서 Redis 상태 확인
wsl -e bash -c "sudo service redis-server status"

# WSL2에서 Redis 시작
wsl -e bash -c "sudo service redis-server start"

# Memurai 사용 시
Get-Service -Name "Memurai*"
Start-Service -Name "Memurai*"
```

### Python 가상환경 활성화 오류

```powershell
# PowerShell 실행 정책 변경
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# 또는 cmd 사용
cd ai-service\prediction-service
venv\Scripts\activate.bat
```

### 포트 충돌

```powershell
# 포트 사용 확인
netstat -ano | findstr :3306  # MariaDB
netstat -ano | findstr :6379  # Redis
netstat -ano | findstr :8080  # Spring Boot
netstat -ano | findstr :8000  # FastAPI

# 프로세스 종료
taskkill /PID <PID> /F
```

## 다음 단계

1. ✅ 모든 서비스가 정상 실행되는지 확인
2. ✅ Spring Boot 애플리케이션과 AI 서비스 통신 테스트
3. ✅ 데이터베이스 연결 및 Redis 캐싱 테스트
4. ✅ API 엔드포인트 테스트 (Swagger UI 사용)
5. ✅ 통합 테스트 실행

## 참고 문서

- [시스템 아키텍처](../02-architecture/01-system-architecture.md)
- [필수 기술 스펙](../02-architecture/10-essential-tech-spec.md)
- [최소 비용 구성](../06-deployment/04-minimal-cost-setup.md)

## 문서 변경 이력

| 버전 | 일자 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-28 | System | 문서 정리 및 구조화 - setup-guides 폴더로 이동 |
| 1.1 | 2026-01-30 | System | §5-1 대시보드 사용 전 확인 사항(입력해야 하는 설정값) 추가; §5 서비스 확인 포트 8083 명시 |
