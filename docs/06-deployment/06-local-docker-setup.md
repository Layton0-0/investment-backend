# 로컬 Docker 인프라 설정 가이드 (Windows 11 + WSL)

Windows 11에서 WSL을 사용하여 Docker Compose로 필수 인프라를 설정하는 단계별 가이드입니다.

## 사전 요구사항

- Windows 11
- WSL 2 설치 완료
- Docker Desktop for Windows 설치 완료 (WSL 2 백엔드 사용)
- 사용자: `yoon`
- 홈 디렉토리: `/home/yoon`

## 1단계: WSL 접속 및 프로젝트 디렉토리 확인

```bash
# WSL 터미널 열기 (PowerShell 또는 Windows Terminal에서)
wsl

# 현재 사용자 확인
whoami
# 출력: yoon

# 홈 디렉토리 확인
echo $HOME
# 출력: /home/yoon

# 프로젝트 디렉토리로 이동
# Windows 경로를 WSL 경로로 변환: d:\works\pjt\investment-choi → /mnt/d/works/pjt/investment-choi
cd /mnt/d/works/pjt/investment-choi

# 현재 위치 확인
pwd
# 출력: /mnt/d/works/pjt/investment-choi
```

## 2단계: Docker 및 Docker Compose 설치 확인

```bash
# Docker 버전 확인
docker --version
# 예상 출력: Docker version 24.x.x 또는 그 이상

# Docker Compose 버전 확인
docker compose version
# 예상 출력: Docker Compose version v2.x.x 또는 그 이상

# Docker 서비스 상태 확인
docker ps
# 정상이면 컨테이너 목록이 표시되거나 빈 목록이 표시됨
```

**문제 해결:**
- Docker가 실행되지 않으면 Windows에서 Docker Desktop을 시작하세요.
- `docker: command not found` 오류가 발생하면 Docker Desktop이 WSL 2와 통합되지 않은 것입니다. Docker Desktop 설정에서 "Use the WSL 2 based engine" 옵션을 활성화하세요.

## 3단계: 프로젝트 디렉토리 구조 확인

```bash
# docker-compose.yml 파일 존재 확인
ls -la docker-compose.yml
# 출력: -rw-r--r-- 1 yoon yoon ... docker-compose.yml

# 프로젝트 루트 디렉토리 확인
ls -la
# docker-compose.yml 파일이 보여야 합니다
```

## 4단계: Docker Compose로 인프라 시작

```bash
# 백그라운드에서 모든 서비스 시작
docker compose up -d

# 또는 상세 로그와 함께 시작 (디버깅 시 유용)
docker compose up
# Ctrl+C로 중지 (백그라운드 모드가 아닐 때)
```

**예상 출력:**
```
[+] Running 3/3
 ✔ Network investment-choi_investment-network    Created
 ✔ Container investment-redis                    Started
 ✔ Container investment-mariadb                  Started
```

## 5단계: 컨테이너 상태 확인

```bash
# 실행 중인 컨테이너 확인
docker compose ps

# 예상 출력:
# NAME                  IMAGE               COMMAND                  SERVICE   CREATED         STATUS          PORTS
# investment-mariadb    mariadb:10.11       "docker-entrypoint.s…"   mariadb   2 minutes ago   Up 2 minutes    0.0.0.0:3306->3306/tcp
# investment-redis      redis:7-alpine       "docker-entrypoint.s…"   redis     2 minutes ago   Up 2 minutes    0.0.0.0:6379->6379/tcp

# 컨테이너 로그 확인
docker compose logs mariadb
docker compose logs redis

# 모든 서비스의 로그 확인
docker compose logs -f
# Ctrl+C로 종료
```

## 6단계: 서비스 연결 테스트

### MariaDB 연결 테스트

```bash
# MariaDB 컨테이너에 접속
docker compose exec mariadb mysql -u local_maria -plocal_maria_pass investment_portfolio

# MariaDB 쿼리 실행
SHOW DATABASES;
USE investment_portfolio;
SHOW TABLES;
EXIT;
```

**또는 외부에서 연결 테스트 (호스트에서):**

```bash
# MariaDB 클라이언트가 설치되어 있다면
mysql -h 127.0.0.1 -P 3306 -u local_maria -plocal_maria_pass investment_portfolio
```

### Redis 연결 테스트

```bash
# Redis 컨테이너에 접속
docker compose exec redis redis-cli

# Redis 명령어 실행
PING
# 출력: PONG

SET test_key "test_value"
GET test_key
# 출력: "test_value"

EXIT
```

**또는 외부에서 연결 테스트:**

```bash
# Redis 클라이언트가 설치되어 있다면
redis-cli -h 127.0.0.1 -p 6379
PING
# 출력: PONG
```

## 7단계: 애플리케이션 설정 확인

애플리케이션이 다음 설정으로 연결할 수 있는지 확인:

- **MariaDB**: `localhost:3306` (또는 `127.0.0.1:3306`)
  - Database: `investment_portfolio`
  - Username: `local_maria`
  - Password: `local_maria_pass`

- **Redis**: `localhost:6379` (또는 `127.0.0.1:6379`)
  - Password: 없음 (기본 설정)

## 8단계: 서비스 중지 및 재시작

```bash
# 모든 서비스 중지 (컨테이너는 유지, 데이터는 보존)
docker compose stop

# 모든 서비스 시작
docker compose start

# 모든 서비스 중지 및 컨테이너 제거 (데이터 볼륨은 유지)
docker compose down

# 모든 서비스 중지, 컨테이너 제거, 볼륨까지 삭제 (주의!)
docker compose down -v
```

## 9단계: 데이터 백업 (선택사항)

```bash
# MariaDB 데이터 백업
docker compose exec mariadb mysqldump -u local_maria -plocal_maria_pass investment_portfolio > backup_$(date +%Y%m%d_%H%M%S).sql

# Redis 데이터 백업 (RDB 파일 복사)
docker compose cp redis:/data/dump.rdb ./backup_redis_$(date +%Y%m%d_%H%M%S).rdb
```

## 10단계: 문제 해결

### 포트 충돌 문제

**Windows에 MariaDB가 이미 설치되어 있는 경우:**

포트 3306이 이미 사용 중이면 다음 중 하나를 선택하세요:

1. **Windows MariaDB를 Docker로 마이그레이션 (권장)**
   - 자세한 가이드: [07-mariadb-migration-to-docker.md](./07-mariadb-migration-to-docker.md)
   - 데이터 백업 → Windows MariaDB 중지 → Docker 시작 → 데이터 복원

2. **포트 변경 (임시 해결책)**
   ```bash
   # 포트 사용 중인 프로세스 확인 (WSL에서)
   sudo netstat -tulpn | grep :3306
   sudo netstat -tulpn | grep :6379
   
   # Windows에서 확인 (PowerShell)
   netstat -ano | findstr :3306
   netstat -ano | findstr :6379
   ```
   
   포트가 이미 사용 중이면 `docker-compose.yml`에서 포트를 변경하세요:
   
   ```yaml
   ports:
     - "3307:3306"  # MariaDB 외부 포트 변경
     - "6380:6379"  # Redis 외부 포트 변경
   ```
   
   **주의:** 포트를 변경하면 `application-local.yml`의 데이터소스 URL도 변경해야 합니다:
   ```yaml
   url: jdbc:mariadb://localhost:3307/investment_portfolio?...
   ```

### 컨테이너가 시작되지 않는 경우

```bash
# 컨테이너 로그 확인
docker compose logs mariadb
docker compose logs redis

# 컨테이너 재생성
docker compose up -d --force-recreate

# 볼륨 삭제 후 재시작 (주의: 데이터 삭제됨)
docker compose down -v
docker compose up -d
```

### 권한 문제

```bash
# Docker 그룹에 사용자 추가 (필요한 경우)
sudo usermod -aG docker $USER
# WSL 재시작 필요
```

## 11단계: 자동 시작 설정 (선택사항)

WSL 부팅 시 자동으로 Docker Compose를 시작하려면:

```bash
# .bashrc 또는 .zshrc에 추가
echo 'cd /mnt/d/works/pjt/investment-choi && docker compose up -d' >> ~/.bashrc

# 또는 systemd 서비스로 설정 (WSL에서 systemd 지원 시)
```

## 주요 명령어 요약

```bash
# 서비스 시작
docker compose up -d

# 서비스 상태 확인
docker compose ps

# 서비스 로그 확인
docker compose logs -f

# 서비스 중지
docker compose stop

# 서비스 재시작
docker compose restart

# 서비스 중지 및 제거
docker compose down

# 서비스 중지, 제거, 볼륨 삭제
docker compose down -v

# 특정 서비스만 재시작
docker compose restart mariadb
docker compose restart redis
```

## 다음 단계

인프라가 정상적으로 실행되면:

1. Spring Boot 애플리케이션 실행
2. 애플리케이션 로그에서 데이터베이스 연결 확인
3. API 테스트 수행

## 참고 사항

- 데이터는 Docker 볼륨에 저장되므로 컨테이너를 삭제해도 데이터는 유지됩니다.
- 프로덕션 환경에서는 보안 설정(비밀번호, 네트워크 격리 등)을 강화해야 합니다.
- 로컬 개발 환경이므로 기본 보안 설정만 적용되어 있습니다.
