# 최소 비용 구성 가이드

## 개요

초기 투자 금액이 크지 않은 상황을 고려한 최소 비용 구성 가이드입니다.

## 목표

- **월 유지비**: $50-100 이하
- **핵심 기능**: 모두 유지
- **확장성**: 수익 발생 시 단계적 확장 가능

## 최소 구성 (월 $50-100)

### 통합 서버 1대

```
서버 스펙:
- CPU: 4 Core
- RAM: 16GB
- Disk: 200GB SSD
- GPU: 없음
- OS: Ubuntu 22.04 LTS

설치 항목:
- Spring Boot 애플리케이션
- MariaDB (로컬)
- Redis (로컬)
- Python FastAPI (CPU만, 경량 모델)
```

### 클라우드 서비스 추천

#### 1. Vultr (가장 저렴)
```
High Frequency Compute:
- 4 vCPU, 16GB RAM, 200GB SSD
- 비용: $48/월
- 위치: 서울 (가까운 지역 선택)
```

#### 2. DigitalOcean
```
Premium Intel:
- 4 vCPU, 16GB RAM, 200GB SSD
- 비용: $96/월
```

#### 3. AWS (예약 인스턴스)
```
t3.xlarge (1년 예약):
- 4 vCPU, 16GB RAM
- 비용: $60-80/월
```

#### 4. 자체 서버 (가장 저렴, 장기)
```
중고 서버 구매:
- 초기 투자: $500-1000
- 월 전기료: $20-30
- 월 인터넷: $30-50
- 총 월 비용: $50-80 (초기 투자 후)
```

## 설치 가이드

### 1. 서버 초기 설정

```bash
# Ubuntu 22.04 업데이트
sudo apt update && sudo apt upgrade -y

# 필수 패키지 설치
sudo apt install -y curl wget git build-essential
```

### 2. Java 17 설치

```bash
# OpenJDK 17 설치
sudo apt install -y openjdk-17-jdk

# 확인
java -version
```

### 3. MariaDB 설치

```bash
# MariaDB 설치
sudo apt install -y mariadb-server mariadb-client

# 보안 설정
sudo mysql_secure_installation

# 데이터베이스 생성
sudo mysql -u root -p
CREATE DATABASE investment;
CREATE USER 'investment'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON investment.* TO 'investment'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### 4. Redis 설치

```bash
# Redis 설치
sudo apt install -y redis-server

# 설정
sudo nano /etc/redis/redis.conf
# maxmemory 4gb
# maxmemory-policy allkeys-lru

# 재시작
sudo systemctl restart redis
sudo systemctl enable redis
```

### 5. Python 환경 설정

```bash
# Python 3.11 설치
sudo apt install -y python3.11 python3.11-venv python3-pip

# 가상환경 생성
python3.11 -m venv /opt/ai-service
source /opt/ai-service/bin/activate

# 경량 라이브러리만 설치
pip install fastapi uvicorn[standard]
pip install torch --index-url https://download.pytorch.org/whl/cpu
pip install numpy pandas scikit-learn
```

### 6. Spring Boot 애플리케이션 배포

```bash
# 프로젝트 클론
cd /opt
git clone <repository-url> investment-choi
cd investment-choi

# 빌드
./gradlew build

# systemd 서비스 등록
sudo nano /etc/systemd/system/investment-choi.service
```

```ini
[Unit]
Description=Investment Choi Application
After=network.target mariadb.service redis.service

[Service]
Type=simple
User=investment
WorkingDirectory=/opt/investment-choi
ExecStart=/usr/bin/java -jar /opt/investment-choi/build/libs/investment-choi-2.0.0.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
# 서비스 시작
sudo systemctl daemon-reload
sudo systemctl enable investment-choi
sudo systemctl start investment-choi
```

### 7. AI 서비스 배포 (선택적)

```bash
# AI 서비스 디렉토리 생성
mkdir -p /opt/ai-service
cd /opt/ai-service

# FastAPI 앱 생성 (간단한 예시)
cat > main.py << 'EOF'
from fastapi import FastAPI
import torch
import numpy as np

app = FastAPI()

# 경량 모델 로드 (예시)
@app.get("/api/v1/health")
def health():
    return {"status": "ok", "gpu": False}

@app.post("/api/v1/predict")
def predict(request: dict):
    # 경량 LSTM 추론 (CPU만)
    # 실제 구현 필요
    return {
        "symbol": request.get("symbol"),
        "predictedPrice": 100.0,
        "confidence": 0.7
    }
EOF

# systemd 서비스 등록
sudo nano /etc/systemd/system/ai-service.service
```

```ini
[Unit]
Description=AI Prediction Service
After=network.target

[Service]
Type=simple
User=investment
WorkingDirectory=/opt/ai-service
Environment="PATH=/opt/ai-service/bin:/usr/local/bin:/usr/bin:/bin"
ExecStart=/opt/ai-service/bin/uvicorn main:app --host 0.0.0.0 --port 8000
Restart=always

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable ai-service
sudo systemctl start ai-service
```

## 설정 파일

### application.yml (프로덕션)

```yaml
spring:
  datasource:
    url: jdbc:mariadb://localhost:3306/investment
    username: investment
    password: ${DB_PASSWORD}
  
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD}

investment:
  ai:
    prediction-service:
      url: http://localhost:8000
      enabled: true  # CPU만 사용
```

## 모니터링 (무료)

### Prometheus (선택적)

```bash
# Prometheus 설치
wget https://github.com/prometheus/prometheus/releases/download/v2.45.0/prometheus-2.45.0.linux-amd64.tar.gz
tar xvfz prometheus-*.tar.gz
cd prometheus-*

# 설정 파일
cat > prometheus.yml << 'EOF'
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'investment-choi'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
EOF

# systemd 서비스 등록
sudo nano /etc/systemd/system/prometheus.service
```

### Grafana (선택적)

```bash
# Grafana 설치
sudo apt install -y apt-transport-https software-properties-common
sudo add-apt-repository "deb https://packages.grafana.com/oss/deb stable main"
sudo apt update
sudo apt install -y grafana

sudo systemctl enable grafana-server
sudo systemctl start grafana-server
```

## 백업 (저비용)

### 자동 백업 스크립트

```bash
#!/bin/bash
# /opt/backup.sh

DATE=$(date +%Y%m%d)
BACKUP_DIR="/opt/backups"
DB_NAME="investment"
DB_USER="investment"
DB_PASS="your_password"

# 백업 디렉토리 생성
mkdir -p $BACKUP_DIR

# MariaDB 백업
mysqldump -u $DB_USER -p$DB_PASS $DB_NAME | gzip > $BACKUP_DIR/db_$DATE.sql.gz

# Redis 백업
redis-cli SAVE
cp /var/lib/redis/dump.rdb $BACKUP_DIR/redis_$DATE.rdb

# 오래된 백업 삭제 (30일 이상)
find $BACKUP_DIR -name "*.gz" -mtime +30 -delete
find $BACKUP_DIR -name "*.rdb" -mtime +30 -delete

# S3 업로드 (선택적, $1-5/월)
# aws s3 cp $BACKUP_DIR/db_$DATE.sql.gz s3://your-bucket/backups/
```

```bash
# Cron 등록 (매일 새벽 2시)
sudo crontab -e
# 0 2 * * * /opt/backup.sh
```

## 비용 모니터링

### 월간 비용 추적

```bash
# AWS 비용 확인
aws ce get-cost-and-usage \
  --time-period Start=2026-01-01,End=2026-01-31 \
  --granularity MONTHLY \
  --metrics BlendedCost
```

## 확장 계획

### 수익 발생 시 단계적 확장

```
월 $50-100 (초기)
  ↓ 수익 발생
월 $150-250 (서버 분리)
  ↓ 수익 증가
월 $500-1000 (GPU 추가)
  ↓ 대규모 운영
월 $1,000+ (프로덕션 구성)
```

## 트러블슈팅

### 메모리 부족 시

```bash
# 스왑 파일 생성
sudo fallocate -l 4G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile

# 영구 설정
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

### 디스크 공간 부족 시

```bash
# 로그 정리
sudo journalctl --vacuum-time=7d

# Docker 정리 (사용 시)
docker system prune -a
```

## 보안 설정

### 방화벽

```bash
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw enable
```

### SSL 인증서 (Let's Encrypt)

```bash
sudo apt install certbot
sudo certbot --nginx -d yourdomain.com
```

## 참고 문서

- [비용 최적화 아키텍처](../02-architecture/09-cost-optimized-architecture.md)
- [서버 스펙](./03-server-specification.md)
