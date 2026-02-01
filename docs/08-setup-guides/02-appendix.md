# 로컬 개발 환경 구축 부록

이 문서는 [메인 가이드](./01-local-setup-complete.md)의 부록으로, 고급 설정 및 문제 해결 방법을 다룹니다.

## 목차

1. [마이그레이션 가이드](#1-마이그레이션-가이드)
2. [인코딩 문제 해결](#2-인코딩-문제-해결)
3. [트러블슈팅](#3-트러블슈팅)

## 1. 마이그레이션 가이드

### 1-1. Windows MariaDB → Docker 마이그레이션

기존에 Windows에 직접 설치된 MariaDB를 Docker로 마이그레이션하는 방법입니다.

#### 사전 확인

```powershell
# MariaDB 서비스 상태 확인
Get-Service -Name "*mariadb*" | Format-Table -AutoSize

# 포트 3306 사용 중인 프로세스 확인
netstat -ano | findstr :3306
```

#### 마이그레이션 절차

**1단계: 데이터 백업**

```powershell
# 백업 디렉토리 생성
New-Item -ItemType Directory -Force -Path "backup"

# 백업 파일명 생성
$backupFile = "backup\mariadb_backup_$(Get-Date -Format 'yyyyMMdd_HHmmss').sql"

# 전체 데이터베이스 백업
mysqldump -u root -p --all-databases --routines --triggers > $backupFile

# 백업 파일 크기 확인
Get-Item $backupFile | Select-Object Name, Length, LastWriteTime
```

**2단계: Windows MariaDB 서비스 중지**

```powershell
# 서비스 중지 (관리자 권한 필요)
Stop-Service -Name "MariaDB" -Force

# 포트 3306 해제 확인
netstat -ano | findstr :3306
# 출력이 없어야 정상
```

**3단계: Docker Compose로 MariaDB 시작**

```powershell
# WSL 접속
wsl

# 프로젝트 디렉토리로 이동
cd /mnt/d/works/pjt/investment-choi

# Docker Compose로 MariaDB 시작
docker compose up -d mariadb

# 컨테이너 상태 확인
docker compose ps
```

**4단계: 데이터 복원**

```bash
# WSL 터미널에서
# 백업 파일 경로 (Windows 경로를 WSL 경로로 변환)
# D:\works\pjt\investment-choi\backup → /mnt/d/works/pjt/investment-choi/backup

# 데이터베이스 생성
docker compose exec mariadb mysql -u root -proot_password -e "CREATE DATABASE IF NOT EXISTS investment_portfolio CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 백업 복원
docker compose exec -T mariadb mysql -u root -proot_password investment_portfolio < /mnt/d/works/pjt/investment-choi/backup/mariadb_backup_YYYYMMDD_HHMMSS.sql
```

**5단계: 데이터 복원 확인**

```bash
# Docker MariaDB에 접속
docker compose exec mariadb mysql -u local_maria -plocal_maria_pass investment_portfolio

# MariaDB에서 실행할 SQL:
```

```sql
-- 데이터베이스 목록 확인
SHOW DATABASES;

-- 테이블 목록 확인
USE investment_portfolio;
SHOW TABLES;

-- 데이터 확인 (예시)
SELECT COUNT(*) FROM 테이블명;

EXIT;
```

**6단계: 애플리케이션 연결 테스트**

```powershell
# Spring Boot 애플리케이션 실행
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

애플리케이션 로그에서 데이터베이스 연결 성공 메시지 확인.

**7단계: Windows MariaDB 제거 (선택사항)**

⚠️ **주의**: 데이터 마이그레이션이 완전히 완료되고 모든 것이 정상 작동하는 것을 확인한 후에만 수행하세요!

```powershell
# 서비스 제거 (관리자 권한 필요)
sc.exe delete "MariaDB"

# 프로그램 제거
# 설정 → 앱 → 앱 및 기능 → "MariaDB" 검색 → 제거
```

#### 롤백 방법

문제가 발생하여 원래 상태로 돌아가야 하는 경우:

```powershell
# Docker MariaDB 중지
docker compose stop mariadb

# Windows MariaDB 서비스 시작
Start-Service -Name "MariaDB"
```

## 2. 인코딩 문제 해결

### 2-1. 문제 상황

프로젝트의 Java 소스 파일에서 한글 주석이 깨져서 표시되는 문제가 발생할 수 있습니다.

### 2-2. 원인

1. **Gradle 빌드 설정 부족**: `build.gradle`에 Java 컴파일 인코딩 설정이 없음
2. **IDE 설정 부족**: `.editorconfig` 파일이 없어 IDE 인코딩 설정이 일관되지 않음
3. **파일 저장 인코딩**: 일부 파일이 UTF-8이 아닌 다른 인코딩으로 저장됨

### 2-3. 해결 방법

#### build.gradle 인코딩 설정

`build.gradle`에 다음 설정이 있는지 확인:

```gradle
// Java 컴파일 인코딩 설정
tasks.withType(JavaCompile) {
    options.encoding = 'UTF-8'
}

// JavaDoc 인코딩 설정
tasks.withType(Javadoc) {
    options.encoding = 'UTF-8'
}

// 리소스 처리 인코딩 설정
processResources {
    encoding = 'UTF-8'
}
```

#### .editorconfig 파일 확인

프로젝트 루트에 `.editorconfig` 파일이 있는지 확인:

```ini
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
trim_trailing_whitespace = true

[*.java]
charset = utf-8
```

#### gradle.properties 확인

`gradle.properties`에 다음 설정이 있는지 확인:

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
```

#### IDE 설정 확인

**IntelliJ IDEA / Cursor:**
1. File → Settings → Editor → File Encodings
2. Global Encoding: UTF-8
3. Project Encoding: UTF-8
4. Default encoding for properties files: UTF-8
5. Transparent native-to-ascii conversion 체크

**VS Code:**
1. Settings → Files: Encoding → UTF-8
2. 또는 `.vscode/settings.json`에 추가:
   ```json
   {
     "files.encoding": "utf8",
     "files.autoGuessEncoding": true
   }
   ```

### 2-4. 예방 방법

1. **IDE 설정 통일**: 모든 개발자가 동일한 인코딩 설정 사용
2. **.editorconfig 활용**: 프로젝트 루트에 `.editorconfig` 파일로 인코딩 강제
3. **Git 설정**: `.gitattributes` 파일에 인코딩 설정 추가
4. **빌드 스크립트**: `build.gradle`에 인코딩 설정 명시

### 2-5. .gitattributes 추가 (선택사항)

프로젝트 루트에 `.gitattributes` 파일을 추가:

```
*.java text eol=lf charset=utf-8
*.properties text eol=lf charset=utf-8
*.yml text eol=lf charset=utf-8
*.yaml text eol=lf charset=utf-8
*.md text eol=lf charset=utf-8
```

## 3. 트러블슈팅

### 3-1. 포트 충돌

#### 문제: 포트가 이미 사용 중

```powershell
# 포트 사용 확인
netstat -ano | findstr :3306  # MariaDB
netstat -ano | findstr :6379  # Redis
netstat -ano | findstr :8083  # Spring Boot
netstat -ano | findstr :8000  # AI Service
```

#### 해결 방법

**방법 1: 프로세스 종료**

```powershell
# PID 확인
netstat -ano | findstr :3306

# 프로세스 종료
taskkill /PID <PID> /F
```

**방법 2: Docker Compose 포트 변경**

`docker-compose.yml`에서 포트 변경:

```yaml
services:
  mariadb:
    ports:
      - "3307:3306"  # 외부 포트 변경
```

`application-local.yml`도 함께 변경:

```yaml
spring:
  datasource:
    url: jdbc:mariadb://localhost:3307/investment_portfolio?...
```

### 3-2. Docker 컨테이너 문제

#### 문제: 컨테이너가 시작되지 않음

```powershell
# 컨테이너 로그 확인
docker compose logs mariadb
docker compose logs redis

# 컨테이너 재생성
docker compose up -d --force-recreate

# 볼륨 삭제 후 재시작 (주의: 데이터 삭제됨)
docker compose down -v
docker compose up -d
```

#### 문제: 컨테이너가 계속 재시작됨

```powershell
# 컨테이너 상태 확인
docker compose ps

# 로그 확인
docker compose logs -f mariadb

# 컨테이너 내부 접속
docker compose exec mariadb bash
```

### 3-3. MCP 연결 실패

#### 문제: MCP 서버가 작동하지 않음

**해결 방법:**

1. **Cursor 재시작**: 환경 변수 변경 후 반드시 재시작
2. **환경 변수 확인**:
   ```powershell
   $env:GITHUB_TOKEN
   $env:NOTION_TOKEN
   ```
3. **설정 파일 확인**: `C:\Users\<사용자명>\.cursor\mcp.json`의 JSON 형식 확인
4. **로그 확인**: Cursor의 개발자 도구에서 MCP 관련 오류 확인

#### 문제: GitHub MCP 오류

- **토큰 권한 확인**: `repo` 권한이 있는지 확인
- **토큰 만료 확인**: GitHub에서 토큰 상태 확인
- **환경 변수 재설정**: 토큰을 다시 설정하고 Cursor 재시작

#### 문제: Notion MCP 오류

- **Integration 연결 확인**: Notion 페이지가 Integration에 연결되어 있는지 확인
- **권한 확인**: Integration의 Capabilities 설정 확인
- **토큰 형식 확인**: `secret_`으로 시작하는지 확인

### 3-4. 빌드 잠금 문제

#### 문제: `Failed to clean up stale outputs`

Windows에서 `build` 디렉터리가 다른 프로세스에 의해 잠겨 있으면 발생합니다.

**해결 방법:**

**방법 1: 표준 스크립트 사용 (권장)**

```powershell
# 테스트
.\scripts\run-tests.ps1

# Agent 전용 서버 실행
.\scripts\bootRun-agent.ps1
```

**방법 2: 환경 변수 설정**

```powershell
$env:GRADLE_UNIQUE_BUILD_DIR='1'
.\gradlew test
```

**방법 3: IntelliJ 종료**

IntelliJ가 `build` 디렉터리를 잠그고 있을 수 있습니다. IntelliJ를 종료한 후 다시 시도하세요.

### 3-5. Python 가상환경 문제

#### 문제: 가상환경 활성화 오류

```powershell
# PowerShell 실행 정책 변경
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# 또는 cmd 사용
cd ai-service\prediction-service
venv\Scripts\activate.bat
```

#### 문제: 패키지 설치 실패

```powershell
# 가상환경 재생성
cd ai-service\prediction-service
Remove-Item -Recurse -Force venv
python -m venv venv
.\venv\Scripts\Activate.ps1
pip install --upgrade pip
pip install -r requirements.txt
```

### 3-6. 데이터베이스 연결 실패

#### 문제: MariaDB 연결 실패

```powershell
# Docker 컨테이너 상태 확인
docker compose ps mariadb

# 컨테이너 로그 확인
docker compose logs mariadb

# 연결 테스트
docker compose exec mariadb mysql -u local_maria -plocal_maria_pass investment_portfolio -e "SELECT 1;"
```

#### 문제: 사용자 권한 문제

```bash
# Docker MariaDB에 root로 접속
docker compose exec mariadb mysql -u root -proot_password

# 사용자 재생성 및 권한 부여
```

```sql
CREATE USER IF NOT EXISTS 'local_maria'@'%' IDENTIFIED BY 'local_maria_pass';
GRANT ALL PRIVILEGES ON investment_portfolio.* TO 'local_maria'@'%';
FLUSH PRIVILEGES;
```

### 3-7. Redis 연결 실패

#### 문제: Redis 연결 실패

```powershell
# Docker 컨테이너 상태 확인
docker compose ps redis

# 컨테이너 로그 확인
docker compose logs redis

# 연결 테스트
docker compose exec redis redis-cli ping
# 응답: PONG
```

### 3-8. Agent 테스트 실행 전 확인

#### 문제: 테스트 실행 시 빌드 잠금

**해결 방법:**

- **8084 포트**: Cursor/Agent로 서버를 띄웠다면 **테스트 실행 전 8084 서버를 반드시 종료**한다. (해당 터미널에서 Ctrl+C)
- **임시 빌드 삭제 오류**: Windows에서 `Unable to delete directory ... test-results\test\binary` 발생 시, `.\scripts\run-tests.ps1 -NoUniqueDir` 또는 `.\scripts\run-tests-with-coverage.ps1 -NoUniqueDir`로 **프로젝트 build 폴더**를 사용해 실행한다.

## 참고 자료

- [Gradle 인코딩 설정](https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_system_properties)
- [EditorConfig](https://editorconfig.org/)
- [Java 인코딩 문제 해결](https://docs.oracle.com/javase/tutorial/i18n/text/index.html)
- [Docker Compose 문서](https://docs.docker.com/compose/)
- [WSL2 문서](https://learn.microsoft.com/windows/wsl/)

## 문서 변경 이력

| 버전 | 일자 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-30 | System | 부록 문서 작성 (마이그레이션, 인코딩, 트러블슈팅) |
