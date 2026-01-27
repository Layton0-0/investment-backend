# 로컬 Windows 환경 빠른 시작 가이드

## 🚀 빠른 설치 명령어

### 1. 필수 항목 확인

```powershell
# 환경 확인 스크립트 실행
.\scripts\check-local-env.ps1
```

### 2. 설치가 필요한 항목

#### MariaDB 11.8.5+
1. 다운로드: https://mariadb.org/download/
2. 설치 후 데이터베이스 생성:
   ```sql
   CREATE DATABASE investment CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'investment'@'localhost' IDENTIFIED BY 'password';
   GRANT ALL PRIVILEGES ON investment.* TO 'investment'@'localhost';
   FLUSH PRIVILEGES;
   ```

#### Redis
**옵션 1: WSL2 + Redis (권장)**
```bash
# WSL2 Ubuntu 터미널에서
sudo apt update
sudo apt install -y redis-server
sudo service redis-server start
```

**옵션 2: Memurai (Windows 네이티브)**
- 다운로드: https://www.memurai.com/get-memurai
- 설치 후 자동 시작

**옵션 3: Docker**
```powershell
docker run -d --name redis -p 6379:6379 redis:7-alpine
```

#### Python 3.11+
1. 다운로드: https://www.python.org/downloads/
2. 설치 시 **"Add Python to PATH"** 체크 필수!
3. 가상환경 설정:
   ```powershell
   .\scripts\setup-python-env.ps1
   ```

### 3. 서비스 시작

```powershell
# 1. MariaDB, Redis 시작 확인
.\scripts\start-services.ps1

# 2. Spring Boot 실행 (새 터미널)
.\gradlew.bat bootRun

# 3. AI 서비스 실행 (새 터미널)
cd ai-service\prediction-service
.\venv\Scripts\Activate.ps1
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

### 4. 서비스 확인

- Spring Boot: http://localhost:8080/actuator/health
- AI 서비스: http://localhost:8000/
- Swagger UI: http://localhost:8080/swagger-ui.html

## 📋 체크리스트

- [ ] Java 17+ 설치 확인
- [ ] MariaDB 설치 및 데이터베이스 생성
- [ ] Redis 설치 및 실행
- [ ] Python 3.11+ 설치
- [ ] Python 가상환경 설정 (`.\scripts\setup-python-env.ps1`)
- [ ] Spring Boot 실행 확인
- [ ] AI 서비스 실행 확인

## 🔧 트러블슈팅

### PowerShell 실행 정책 오류
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### 포트 충돌
```powershell
# 포트 사용 확인
netstat -ano | findstr :8080
netstat -ano | findstr :8000
```

### MariaDB 연결 실패
```powershell
# 서비스 시작
Get-Service -Name "MariaDB*" | Start-Service
```

## 📚 상세 가이드

전체 설치 가이드: [docs/06-deployment/05-local-windows-setup.md](docs/06-deployment/05-local-windows-setup.md)
