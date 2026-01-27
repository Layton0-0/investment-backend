# Windows MariaDB → Docker 마이그레이션 가이드

Windows에 직접 설치된 MariaDB를 Docker로 마이그레이션하는 안전한 단계별 가이드입니다.

## ⚠️ 중요 사항

**절대 바로 삭제하지 마세요!** 데이터 백업과 마이그레이션을 먼저 완료해야 합니다.

## 사전 확인

### 1단계: 현재 MariaDB 상태 확인

**PowerShell에서 실행:**

```powershell
# MariaDB 서비스 상태 확인
Get-Service -Name "*mariadb*" | Format-Table -AutoSize
Get-Service -Name "*mysql*" | Format-Table -AutoSize

# 포트 3306 사용 중인 프로세스 확인
netstat -ano | findstr :3306
```

**예상 출력:**
```
TCP    0.0.0.0:3306           0.0.0.0:0              LISTENING       12345
```

### 2단계: 기존 데이터베이스 및 사용자 정보 확인

**MariaDB 클라이언트로 접속:**

```powershell
# root 사용자로 접속 (설치 시 설정한 비밀번호 사용)
mysql -u root -p
```

**MariaDB에서 실행할 SQL:**

```sql
-- 데이터베이스 목록 확인
SHOW DATABASES;

-- 사용자 목록 확인
SELECT User, Host FROM mysql.user;

-- investment_portfolio 또는 investment 데이터베이스가 있는지 확인
SHOW DATABASES LIKE 'investment%';

-- 현재 사용 중인 데이터베이스의 테이블 확인 (있는 경우)
USE investment_portfolio;  -- 또는 investment
SHOW TABLES;

-- 데이터베이스 크기 확인
SELECT 
    table_schema AS 'Database',
    ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS 'Size (MB)'
FROM information_schema.tables
WHERE table_schema IN ('investment', 'investment_portfolio')
GROUP BY table_schema;

EXIT;
```

## 마이그레이션 절차

### 3단계: 데이터 백업

**PowerShell에서 실행 (관리자 권한 권장):**

```powershell
# 백업 디렉토리 생성
New-Item -ItemType Directory -Force -Path "D:\works\pjt\investment-choi\backup"

# 백업 파일명 생성 (날짜 포함)
$backupFile = "D:\works\pjt\investment-choi\backup\mariadb_backup_$(Get-Date -Format 'yyyyMMdd_HHmmss').sql"

# 전체 데이터베이스 백업 (모든 데이터베이스)
mysqldump -u root -p --all-databases --routines --triggers > $backupFile

# 또는 특정 데이터베이스만 백업 (investment 또는 investment_portfolio)
# mysqldump -u root -p investment_portfolio > $backupFile
# mysqldump -u root -p investment > $backupFile

# 백업 파일 확인
Get-Item $backupFile | Select-Object Name, Length, LastWriteTime
```

**백업 파일 크기 확인:**
- 백업 파일이 0KB이면 백업이 실패한 것입니다.
- 백업 파일이 생성되었는지 반드시 확인하세요.

### 4단계: Windows MariaDB 서비스 중지

**PowerShell에서 실행 (관리자 권한 필요):**

```powershell
# MariaDB 서비스 이름 확인
Get-Service -Name "*mariadb*" | Select-Object Name, Status
Get-Service -Name "*mysql*" | Select-Object Name, Status

# 서비스 중지 (서비스 이름은 실제 이름으로 변경)
Stop-Service -Name "MariaDB" -Force
# 또는
Stop-Service -Name "MySQL" -Force

# 서비스 상태 확인
Get-Service -Name "*mariadb*" | Select-Object Name, Status
Get-Service -Name "*mysql*" | Select-Object Name, Status

# 포트 3306이 해제되었는지 확인
netstat -ano | findstr :3306
# 출력이 없어야 정상
```

**또는 서비스 관리자에서:**
1. `Win + R` → `services.msc` 실행
2. "MariaDB" 또는 "MySQL" 서비스 찾기
3. 우클릭 → "중지"

### 5단계: Docker Compose로 MariaDB 시작

**WSL 터미널에서 실행:**

```bash
# WSL 접속
wsl

# 프로젝트 디렉토리로 이동
cd /mnt/d/works/pjt/investment-choi

# Docker Compose로 MariaDB 시작 (Redis도 함께 시작됨)
docker compose up -d mariadb

# 컨테이너 상태 확인
docker compose ps

# 로그 확인 (정상 시작 확인)
docker compose logs mariadb
```

**예상 출력:**
```
investment-mariadb    mariadb:10.11    ...    Up X minutes    0.0.0.0:3306->3306/tcp
```

### 6단계: Docker MariaDB에 데이터 복원

**WSL 터미널에서 실행:**

```bash
# 백업 파일 경로 확인 (Windows 경로를 WSL 경로로 변환)
# D:\works\pjt\investment-choi\backup → /mnt/d/works/pjt/investment-choi/backup

# 백업 파일 목록 확인
ls -lh /mnt/d/works/pjt/investment-choi/backup/*.sql

# Docker MariaDB에 백업 복원
# 방법 1: 전체 데이터베이스 복원
docker compose exec -T mariadb mysql -u root -proot_password < /mnt/d/works/pjt/investment-choi/backup/mariadb_backup_YYYYMMDD_HHMMSS.sql

# 방법 2: 특정 데이터베이스만 복원 (권장)
# 먼저 데이터베이스 생성
docker compose exec mariadb mysql -u root -proot_password -e "CREATE DATABASE IF NOT EXISTS investment_portfolio CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 백업 파일에서 특정 데이터베이스만 추출하여 복원
# (백업 파일이 전체 백업인 경우)
docker compose exec -T mariadb mysql -u root -proot_password investment_portfolio < /mnt/d/works/pjt/investment-choi/backup/mariadb_backup_YYYYMMDD_HHMMSS.sql
```

**또는 PowerShell에서 직접 복원:**

```powershell
# Docker 컨테이너에 백업 파일 복사
docker cp "D:\works\pjt\investment-choi\backup\mariadb_backup_YYYYMMDD_HHMMSS.sql" investment-mariadb:/tmp/backup.sql

# 컨테이너 내에서 복원
docker exec investment-mariadb mysql -u root -proot_password < D:\works\pjt\investment-choi\backup\mariadb_backup_YYYYMMDD_HHMMSS.sql

# 또는 특정 데이터베이스만
docker exec investment-mariadb mysql -u root -proot_password investment_portfolio -e "source /tmp/backup.sql"
```

### 7단계: 데이터 복원 확인

**WSL 터미널에서 실행:**

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
SELECT COUNT(*) FROM 테이블명;  -- 실제 테이블명으로 변경

-- 사용자 확인
SELECT User, Host FROM mysql.user WHERE User = 'local_maria';

EXIT;
```

### 8단계: 애플리케이션 연결 테스트

**Spring Boot 애플리케이션 실행:**

```powershell
# WSL에서 또는 PowerShell에서
cd D:\works\pjt\investment-choi
./gradlew bootRun --args='--spring.profiles.active=local'
```

**연결 확인:**
- 애플리케이션 로그에서 데이터베이스 연결 성공 메시지 확인
- 에러가 없으면 정상 연결된 것입니다.

### 9단계: Windows MariaDB 제거 (선택사항)

**⚠️ 주의: 이 단계는 데이터 마이그레이션이 완전히 완료되고 모든 것이 정상 작동하는 것을 확인한 후에만 수행하세요!**

#### 9-1. 서비스 제거

**PowerShell에서 실행 (관리자 권한 필요):**

```powershell
# MariaDB 서비스 제거
# 서비스 이름 확인
Get-Service -Name "*mariadb*" | Select-Object Name
Get-Service -Name "*mysql*" | Select-Object Name

# 서비스 제거 (서비스 이름은 실제 이름으로 변경)
sc.exe delete "MariaDB"
# 또는
sc.exe delete "MySQL"
```

#### 9-2. 프로그램 제거

1. **설정 → 앱 → 앱 및 기능** 열기
2. "MariaDB" 또는 "MySQL" 검색
3. 제거 클릭

#### 9-3. 데이터 디렉토리 정리 (선택사항)

**⚠️ 주의: 이 단계는 백업이 완료된 후에만 수행하세요!**

```powershell
# MariaDB 데이터 디렉토리 확인 (일반적인 위치)
# C:\Program Files\MariaDB 11.x\data
# C:\ProgramData\MariaDB
# C:\Users\YourName\AppData\Local\MariaDB

# 데이터 디렉토리 백업 (선택사항)
# 필요시 나중을 위해 백업해두세요

# 데이터 디렉토리 삭제 (선택사항, 신중하게 결정)
# Remove-Item -Path "C:\Program Files\MariaDB 11.x\data" -Recurse -Force
```

## 문제 해결

### 포트 충돌이 계속 발생하는 경우

```powershell
# 포트를 사용하는 프로세스 강제 종료
netstat -ano | findstr :3306
# PID 확인 후
taskkill /PID <PID번호> /F
```

### 데이터 복원 실패

```bash
# Docker MariaDB 로그 확인
docker compose logs mariadb

# 백업 파일 형식 확인
head -n 20 /mnt/d/works/pjt/investment-choi/backup/mariadb_backup_*.sql

# 수동으로 SQL 실행
docker compose exec mariadb mysql -u root -proot_password
```

### 사용자 권한 문제

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

## 마이그레이션 체크리스트

- [ ] 기존 MariaDB 데이터베이스 및 테이블 목록 확인
- [ ] 데이터 백업 완료 (백업 파일 크기 확인)
- [ ] Windows MariaDB 서비스 중지
- [ ] 포트 3306 해제 확인
- [ ] Docker MariaDB 시작 및 정상 작동 확인
- [ ] 데이터 복원 완료
- [ ] 데이터 복원 확인 (테이블 및 데이터 개수 확인)
- [ ] 애플리케이션 연결 테스트 성공
- [ ] 모든 기능 정상 작동 확인
- [ ] (선택) Windows MariaDB 제거

## 롤백 방법

문제가 발생하여 원래 상태로 돌아가야 하는 경우:

```powershell
# Docker MariaDB 중지
docker compose stop mariadb

# Windows MariaDB 서비스 시작
Start-Service -Name "MariaDB"  # 또는 "MySQL"

# 서비스 상태 확인
Get-Service -Name "*mariadb*" | Select-Object Name, Status
```

## 참고 사항

- 백업 파일은 안전한 곳에 보관하세요.
- 마이그레이션 후 최소 1주일은 기존 MariaDB를 제거하지 않고 보관하는 것을 권장합니다.
- 프로덕션 환경에서는 더 신중한 절차와 테스트가 필요합니다.
