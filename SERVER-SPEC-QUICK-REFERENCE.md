# 서버 스펙 빠른 참조 가이드

## 🚀 빠른 시작 - 최소 구성

### 통합 서버 1대 (개발/테스트용)
```
CPU: 4 Core
RAM: 16GB
Disk: 200GB SSD
OS: Ubuntu 22.04 LTS
Java: OpenJDK 17
Python: 3.11+
MariaDB: 11.8.5+
Redis: 7.0+
GPU: 없음 (CPU 추론)
```

**설치 항목**:
- Spring Boot 애플리케이션
- MariaDB
- Redis
- Python FastAPI 서비스 (CPU 모드)

**예상 비용**: $50-100/월 (클라우드 기준)

---

## 📊 권장 구성 - 소규모 운영

### 서버 1: Spring Boot 애플리케이션
```
CPU: 4 Core
RAM: 8GB
Disk: 100GB SSD
OS: Ubuntu 22.04 LTS
Java: OpenJDK 17
```

### 서버 2: MariaDB
```
CPU: 4 Core
RAM: 8GB
Disk: 200GB SSD
OS: Ubuntu 22.04 LTS
MariaDB: 11.8.5+
```

### 서버 3: Redis
```
CPU: 2 Core
RAM: 4GB
Disk: 20GB
OS: Ubuntu 22.04 LTS
Redis: 7.0+
```

### 서버 4: AI 서비스 (GPU 포함)
```
CPU: 8 Core
RAM: 16GB
GPU: NVIDIA RTX 3060 (12GB VRAM)
Disk: 200GB SSD
OS: Ubuntu 22.04 LTS
Python: 3.11+
CUDA: 11.8+
cuDNN: 8.6+
```

**설치 항목**:
- Prediction Service (LSTM/Transformer)
- Strategy Optimizer (강화학습)
- NLP Service (감정 분석)

**예상 비용**: $260-360/월

---

## 🎯 프로덕션 구성 - 대규모 운영

### Spring Boot 서버 (2대, 로드 밸런싱)
```
각각: CPU 8 Core, RAM 16GB, Disk 200GB SSD
```

### MariaDB 서버 (Master + Slave)
```
Master: CPU 8 Core, RAM 16GB, Disk 500GB SSD (RAID 10)
Slave: CPU 4 Core, RAM 8GB, Disk 500GB SSD
```

### Redis 서버
```
CPU: 4 Core, RAM: 8GB, Disk: 50GB
```

### AI 서비스 서버 (3대)
```
Prediction Service:
- CPU: 16 Core, RAM: 32GB
- GPU: RTX 4090 (24GB VRAM)
- Disk: 200GB SSD

Strategy Optimizer:
- CPU: 16 Core, RAM: 32GB
- GPU: RTX 3080 (10GB VRAM)
- Disk: 200GB SSD

NLP Service:
- CPU: 16 Core, RAM: 32GB
- GPU: RTX 4090 (24GB VRAM)
- Disk: 200GB SSD
```

**예상 비용**: $1,320-1,830/월

---

## 🔧 필수 설치 항목

### 1. Spring Boot 서버
```bash
# Java 17 설치
sudo apt update
sudo apt install openjdk-17-jdk

# 애플리케이션 배포
./gradlew build
java -jar build/libs/investment-choi-2.0.0.jar
```

### 2. MariaDB 서버
```bash
# MariaDB 설치
sudo apt install mariadb-server mariadb-client

# 데이터베이스 생성
mysql -u root -p
CREATE DATABASE investment;
CREATE USER 'investment'@'%' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON investment.* TO 'investment'@'%';
FLUSH PRIVILEGES;
```

### 3. Redis 서버
```bash
# Redis 설치
sudo apt install redis-server

# 설정
sudo nano /etc/redis/redis.conf
# maxmemory 4gb
# maxmemory-policy allkeys-lru
# requirepass YOUR_PASSWORD

# 재시작
sudo systemctl restart redis
```

### 4. AI 서비스 서버 (GPU 포함)

#### CUDA 설치
```bash
# NVIDIA 드라이버 설치
sudo apt install nvidia-driver-535

# CUDA 11.8 설치
wget https://developer.download.nvidia.com/compute/cuda/11.8.0/local_installers/cuda_11.8.0_520.61.05_linux.run
sudo sh cuda_11.8.0_520.61.05_linux.run

# 환경 변수 설정
echo 'export PATH=/usr/local/cuda/bin:$PATH' >> ~/.bashrc
echo 'export LD_LIBRARY_PATH=/usr/local/cuda/lib64:$LD_LIBRARY_PATH' >> ~/.bashrc
source ~/.bashrc
```

#### Python 환경 설정
```bash
# Python 3.11 설치
sudo apt install python3.11 python3.11-venv python3-pip

# 가상환경 생성
python3.11 -m venv venv
source venv/bin/activate

# PyTorch 설치 (CUDA 11.8)
pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu118

# FastAPI 및 기타 라이브러리
pip install fastapi uvicorn[standard]
pip install numpy pandas scikit-learn
pip install transformers sentencepiece

# GPU 확인
python -c "import torch; print(f'CUDA available: {torch.cuda.is_available()}'); print(f'GPU: {torch.cuda.get_device_name(0) if torch.cuda.is_available() else \"None\"}')"
```

---

## 💾 메모리 요구사항 요약

### 모델별 메모리
```
LSTM 모델:
- 모델 크기: 50MB
- 추론 메모리: 500MB
- 배치 처리: 2GB

Transformer 모델:
- 모델 크기: 200MB
- 추론 메모리: 2GB
- 배치 처리: 8GB

강화학습 (PPO):
- 에이전트: 2GB
- 백테스팅: 5GB
- 총: 10GB

NLP (BERT-base):
- 모델 크기: 440MB
- 추론 메모리: 2GB
- 배치 처리: 4GB
```

### GPU VRAM 요구사항
```
RTX 3060 (12GB): LSTM, 경량 Transformer, BERT-base
RTX 3080 (10GB): 중형 모델, 강화학습
RTX 4090 (24GB): 대형 Transformer, 앙상블 모델
A100 (40GB): 프로덕션 대규모 모델
```

---

## 🌐 클라우드 서비스 추천

### AWS
```
- EC2: t3.medium (Spring Boot), t3.large (DB)
- RDS: db.t3.medium (MariaDB)
- ElastiCache: cache.t3.micro (Redis)
- EC2 GPU: g4dn.xlarge (AI 서비스)
- 예상 비용: $200-500/월
```

### GCP
```
- Compute Engine: n1-standard-4 (Spring Boot)
- Cloud SQL: db-n1-standard-4 (MariaDB)
- Memorystore: basic-tier (Redis)
- Compute Engine GPU: n1-standard-8 + T4 GPU
- 예상 비용: $250-600/월
```

### Azure
```
- Virtual Machine: Standard_B4ms (Spring Boot)
- Azure Database for MariaDB: Gen5, 4 vCore
- Azure Cache for Redis: Basic C1
- Virtual Machine GPU: Standard_NC6s_v3
- 예상 비용: $300-700/월
```

---

## 📋 설치 체크리스트

### Spring Boot 서버
- [ ] Java 17 설치 확인
- [ ] 애플리케이션 빌드 및 배포
- [ ] systemd 서비스 등록
- [ ] 로그 디렉토리 생성 (`/LOG`)

### MariaDB 서버
- [ ] MariaDB 11.8.5+ 설치
- [ ] 데이터베이스 및 사용자 생성
- [ ] my.cnf 설정 (innodb_buffer_pool_size 등)
- [ ] 백업 스크립트 설정

### Redis 서버
- [ ] Redis 7.0+ 설치
- [ ] redis.conf 설정 (maxmemory, password)
- [ ] systemd 서비스 등록
- [ ] 연결 테스트

### AI 서비스 서버
- [ ] Python 3.11+ 설치
- [ ] CUDA 및 cuDNN 설치 (GPU 사용 시)
- [ ] NVIDIA 드라이버 설치 확인
- [ ] 가상환경 생성 및 의존성 설치
- [ ] 모델 파일 다운로드
- [ ] GPU 테스트 (`nvidia-smi`, `torch.cuda.is_available()`)
- [ ] systemd 서비스 등록

---

## 🔒 보안 설정

### 방화벽 (UFW)
```bash
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw allow 3306/tcp  # MariaDB (내부 네트워크만)
sudo ufw allow 6379/tcp  # Redis (내부 네트워크만)
sudo ufw enable
```

### SSL/TLS 인증서
```bash
# Let's Encrypt (무료)
sudo apt install certbot
sudo certbot --nginx -d yourdomain.com
```

---

## 📊 성능 모니터링

### 필수 모니터링 도구
```bash
# 시스템 리소스
htop
nvidia-smi  # GPU 모니터링

# 애플리케이션 메트릭
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/health
```

---

## 💡 비용 절감 팁

1. **개발 환경**: 최소 스펙 사용
2. **GPU**: 초기에는 CPU만 사용, 필요 시 GPU 추가
3. **클라우드**: 스팟 인스턴스 활용 (AI 서비스)
4. **캐싱**: Redis로 DB 부하 감소
5. **자동 스케일링**: 트래픽에 따라 서버 자동 조정

---

## 📚 상세 문서

- [서버 스펙 상세 가이드](./docs/06-deployment/03-server-specification.md)
- [배포 가이드](./docs/06-deployment/01-deployment-guide.md)
- [운영 가이드](./docs/06-deployment/02-operations-guide.md)
