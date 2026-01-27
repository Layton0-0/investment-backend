# local-maria 설정 가이드

## 완료된 작업

1. ✅ MariaDB MCP 서버 폴더 생성: `D:\works\tools\mariadb-mcp`
2. ✅ MariaDB MCP 서버 설정 파일 생성: `.env` (local_maria 사용자로 연결)
3. ✅ Cursor MCP 설정 추가: `mcp.json`에 `local-maria` 서버 추가
4. ✅ 프로젝트 설정 파일 생성: `application-local.yml`
5. ✅ 사용자 생성 SQL 스크립트 생성

## 남은 작업

### 1. MariaDB MCP 서버 설치

다음 중 하나의 방법으로 MariaDB MCP 서버를 설치하세요:

#### 방법 1: Git Clone (권장)
```powershell
cd D:\works\tools
git clone https://github.com/mariadb/mcp.git mariadb-mcp
```

#### 방법 2: ZIP 다운로드
1. https://github.com/mariadb/mcp 에서 ZIP 파일 다운로드
2. `D:\works\tools\mariadb-mcp` 폴더에 압축 해제

### 2. Python 및 uv 설치

#### Python 3.11 설치
- https://www.python.org/downloads/ 에서 Python 3.11 다운로드 및 설치
- 설치 시 "Add Python to PATH" 옵션 선택

#### uv 설치
```powershell
pip install uv
```

### 3. MariaDB MCP 서버 의존성 설치

```powershell
cd D:\works\tools\mariadb-mcp
uv lock
uv sync
```

### 4. MariaDB 사용자 생성

`application-local.yml`에 정의된 사용자 정보로 MariaDB에 사용자를 생성하세요:

#### 방법 1: SQL 스크립트 실행 (권장)
```powershell
# MariaDB bin 폴더가 PATH에 있는 경우
mysql -u root -prhksflwk#mariadb < d:\works\pjt\investment-choi\src\main\resources\db\create-local-maria-user.sql

# 또는 MariaDB 클라이언트로 직접 실행
mysql -u root -p
# 비밀번호 입력: rhksflwk#mariadb
# 그 다음 SQL 스크립트 내용을 복사하여 실행
```

#### 방법 2: 수동 SQL 실행
MariaDB 클라이언트에 접속하여 다음 SQL을 실행:

```sql
CREATE USER IF NOT EXISTS 'local_maria'@'localhost' IDENTIFIED BY 'local_maria_pass';
GRANT ALL PRIVILEGES ON investment.* TO 'local_maria'@'localhost';
GRANT SELECT ON *.* TO 'local_maria'@'localhost';
FLUSH PRIVILEGES;
```

### 5. Cursor 재시작

모든 설정이 완료되면 Cursor를 재시작하세요.

## 사용 방법

### MariaDB MCP 서버 사용

Cursor에서 `local-maria` MCP 서버를 통해 MariaDB에 접속할 수 있습니다:
- 데이터베이스 목록 조회
- 테이블 스키마 조회
- SQL 쿼리 실행 (SELECT만 가능, 읽기 전용 모드)

### Spring Boot 애플리케이션 실행

`local` 프로파일로 애플리케이션을 실행:

```powershell
./gradlew bootRun --args='--spring.profiles.active=local'
```

또는 환경 변수 설정:

```powershell
$env:SPRING_DATASOURCE_USERNAME="local_maria"
$env:SPRING_DATASOURCE_PASSWORD="local_maria_pass"
./gradlew bootRun --args='--spring.profiles.active=local'
```

## 생성된 사용자 정보

`application-local.yml`에 정의된 사용자 정보:

- **사용자명**: `local_maria`
- **비밀번호**: `local_maria_pass`
- **호스트**: `localhost`
- **권한**:
  - `investment` 데이터베이스: 모든 권한
  - 모든 데이터베이스: SELECT 권한 (MCP 서버용)

## 설정 파일 위치

- MariaDB MCP 서버 설정: `D:\works\tools\mariadb-mcp\.env`
- Cursor MCP 설정: `C:\Users\HNW\.cursor\mcp.json`
- Spring Boot 설정: `src\main\resources\application-local.yml`

## 설정 파일 구조

### application-local.yml

`application.yml`을 기준으로 생성된 로컬 개발 환경 설정 파일입니다.

주요 설정:
- **데이터소스**: `local_maria` / `local_maria_pass`
- **JPA**: `ddl-auto: update` (로컬 개발용)
- **SQL 로깅**: `show-sql: true` (로컬 개발용)
- **로그 파일**: `logs/investment-choi.log` (로컬 경로)

### .env (MariaDB MCP 서버)

MCP 서버가 MariaDB에 접속할 때 사용하는 설정:
- `DB_USER=local_maria`
- `DB_PASSWORD=local_maria_pass`
- `MCP_READ_ONLY=true` (읽기 전용 모드)
