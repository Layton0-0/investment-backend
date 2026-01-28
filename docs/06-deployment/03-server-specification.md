# 서버 인프라 스펙 가이드

## 개요

고수익 자동투자 시스템을 운영하기 위한 서버 인프라 스펙 및 설정 가이드를 제공합니다.

## 시스템 구성

### 전체 아키텍처
```
┌─────────────────────────────────────────────────────────┐
│  Spring Boot Application (Java 17)                     │
│  - Trading Service                                      │
│  - Analysis Service                                     │
│  - Portfolio Service                                    │
└──────────────────┬──────────────────────────────────────┘
                   │
        ┌──────────┴──────────┐
        ▼                     ▼
┌──────────────┐    ┌──────────────────────┐
│   MariaDB    │    │   Redis Cache        │
│  (Database)  │    │  (Caching Layer)     │
└──────────────┘    └──────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────┐
│  AI/ML Services (Python FastAPI)                        │
│  - Prediction Service (LSTM/Transformer)                │
│  - Strategy Optimizer (Reinforcement Learning)          │
│  - NLP Service (News/Sentiment Analysis)                │
└─────────────────────────────────────────────────────────┘
```

---

## 1. Spring Boot 애플리케이션 서버

### 최소 스펙
```
CPU: 2 Core
RAM: 4GB
Disk: 50GB SSD
OS: Linux (Ubuntu 20.04+ / CentOS 7+)
Java: OpenJDK 17
```

### 권장 스펙
```
CPU: 4 Core
RAM: 8GB
Disk: 100GB SSD
OS: Linux (Ubuntu 22.04 LTS)
Java: OpenJDK 17 또는 Oracle JDK 17
```

### 프로덕션 스펙
```
CPU: 8 Core
RAM: 16GB
Disk: 200GB SSD
OS: Linux (Ubuntu 22.04 LTS)
Java: OpenJDK 17
```

### JVM 설정
```bash
# application.yml 또는 환경 변수
JAVA_OPTS="-Xms2g -Xmx6g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

**설명**:
- `-Xms2g`: 초기 힙 메모리 2GB
- `-Xmx6g`: 최대 힙 메모리 6GB
- `-XX:+UseG1GC`: G1 가비지 컬렉터 사용
- `-XX:MaxGCPauseMillis=200`: GC 일시정지 목표 200ms

---

## 2. MariaDB 데이터베이스 서버

### 최소 스펙
```
CPU: 2 Core
RAM: 4GB
Disk: 100GB SSD
OS: Linux
MariaDB: 11.8.5+
```

### 권장 스펙
```
CPU: 4 Core
RAM: 8GB
Disk: 200GB SSD (RAID 10 권장)
OS: Linux
MariaDB: 11.8.5+
```

### 프로덕션 스펙
```
CPU: 8 Core
RAM: 16GB
Disk: 500GB SSD (RAID 10)
OS: Linux
MariaDB: 11.8.5+
```

### MariaDB 설정 (my.cnf)
```ini
[mysqld]
# 기본 설정
port = 3306
bind-address = 0.0.0.0

# 메모리 설정
innodb_buffer_pool_size = 4G  # RAM의 50-70%
innodb_log_file_size = 512M
innodb_log_buffer_size = 64M
max_connections = 200

# 성능 설정
innodb_flush_log_at_trx_commit = 2
innodb_flush_method = O_DIRECT
query_cache_type = 0  # MariaDB 10.6+에서는 비활성화

# 로그 설정
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow.log
long_query_time = 2
```

### 디스크 공간 계산
```
예상 데이터량:
- 주문 데이터: 1,000건/일 × 365일 = 365,000건
- 각 주문: 약 1KB
- 연간 주문 데이터: 약 365MB
- 5년 보관: 약 2GB

- 포트폴리오 데이터: 1건/일 × 365일 = 365건
- 각 포트폴리오: 약 10KB
- 연간 포트폴리오: 약 4MB
- 5년 보관: 약 20MB

- 시장 데이터 캐시: 일일 약 100MB
- 30일 보관: 약 3GB

총 예상 용량: 약 10GB (5년 기준)
권장 용량: 100GB 이상 (여유 공간 포함)
```

---

## 3. Redis 캐싱 서버

### 최소 스펙
```
CPU: 1 Core
RAM: 2GB
Disk: 10GB (로그용)
OS: Linux
Redis: 7.0+
```

### 권장 스펙
```
CPU: 2 Core
RAM: 4GB
Disk: 20GB
OS: Linux
Redis: 7.0+
```

### 프로덕션 스펙
```
CPU: 4 Core
RAM: 8GB
Disk: 50GB
OS: Linux
Redis: 7.0+
```

### Redis 설정 (redis.conf)
```conf
# 메모리 설정
maxmemory 4gb
maxmemory-policy allkeys-lru

# 지속성 설정 (선택적)
save 900 1
save 300 10
save 60 10000

# 네트워크 설정
bind 0.0.0.0
port 6379
timeout 300

# 보안 설정
requirepass YOUR_REDIS_PASSWORD
```

### 메모리 사용량 계산
```
캐시 항목:
- 분석 결과: 1KB × 1,000종목 = 1MB
- 시장 데이터: 10KB × 100종목 = 1MB
- 포트폴리오: 5KB × 30일 = 150KB

총 예상 용량: 약 5MB
권장 메모리: 2GB 이상 (여유 공간 포함)
```

---

## 4. AI/ML 서비스 서버 (Python FastAPI)

### 4.1 Prediction Service (예측 서비스)

#### 최소 스펙 (CPU만 사용)
```
CPU: 4 Core
RAM: 8GB
Disk: 50GB SSD
OS: Linux (Ubuntu 22.04)
Python: 3.11+
CUDA: 불필요
```

#### 권장 스펙 (GPU 사용)
```
CPU: 8 Core
RAM: 16GB
GPU: NVIDIA RTX 3060 (12GB VRAM) 또는 이상
Disk: 100GB SSD
OS: Linux (Ubuntu 22.04)
Python: 3.11+
CUDA: 11.8+
cuDNN: 8.6+
```

#### 프로덕션 스펙 (고성능)
```
CPU: 16 Core
RAM: 32GB
GPU: NVIDIA RTX 4090 (24GB VRAM) 또는 A100 (40GB)
Disk: 200GB SSD
OS: Linux (Ubuntu 22.04)
Python: 3.11+
CUDA: 12.0+
cuDNN: 8.9+
```

#### 모델별 메모리 요구사항
```
LSTM 모델:
- 모델 크기: 약 50MB
- 추론 시 메모리: 약 500MB
- 배치 처리 (100개): 약 2GB

Transformer 모델:
- 모델 크기: 약 200MB
- 추론 시 메모리: 약 2GB
- 배치 처리 (100개): 약 8GB

앙상블 모델 (3개 모델):
- 총 메모리: 약 10GB
```

#### 설치 가이드
```bash
# Python 3.11 설치
sudo apt update
sudo apt install python3.11 python3.11-venv python3-pip

# 가상환경 생성
python3.11 -m venv venv
source venv/bin/activate

# 의존성 설치
pip install fastapi uvicorn[standard]
pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu118
pip install numpy pandas scikit-learn

# GPU 확인
python -c "import torch; print(torch.cuda.is_available())"
```

---

### 4.2 Strategy Optimizer Service (전략 최적화 서비스)

#### 최소 스펙
```
CPU: 8 Core
RAM: 16GB
Disk: 100GB SSD
OS: Linux
Python: 3.11+
GPU: 선택적 (백테스팅 가속화용)
```

#### 권장 스펙
```
CPU: 16 Core
RAM: 32GB
GPU: NVIDIA RTX 3080 (10GB VRAM) 또는 이상
Disk: 200GB SSD
OS: Linux
Python: 3.11+
CUDA: 11.8+
```

#### 프로덕션 스펙
```
CPU: 32 Core
RAM: 64GB
GPU: NVIDIA A100 (40GB VRAM) 또는 V100 (32GB)
Disk: 500GB SSD
OS: Linux
Python: 3.11+
CUDA: 12.0+
```

#### 메모리 요구사항
```
강화학습 에이전트:
- PPO 에이전트: 약 2GB
- DQN 에이전트: 약 1GB
- 백테스팅 데이터: 약 5GB
- 총 메모리: 약 10GB

백테스팅:
- 5년 데이터: 약 10GB
- 시뮬레이션 메모리: 약 5GB
- 총 메모리: 약 20GB
```

#### 설치 가이드
```bash
# Ray/RLlib 설치
pip install ray[rllib]
pip install stable-baselines3
pip install gymnasium

# 백테스팅 라이브러리
pip install backtrader zipline-reloaded
```

---

### 4.3 NLP Service (자연어 처리 서비스)

#### 최소 스펙 (경량 모델)
```
CPU: 4 Core
RAM: 8GB
Disk: 50GB SSD
OS: Linux
Python: 3.11+
GPU: 불필요 (CPU 추론)
```

#### 권장 스펙 (중형 모델)
```
CPU: 8 Core
RAM: 16GB
GPU: NVIDIA RTX 3060 (12GB VRAM)
Disk: 100GB SSD
OS: Linux
Python: 3.11+
CUDA: 11.8+
```

#### 프로덕션 스펙 (대형 모델)
```
CPU: 16 Core
RAM: 32GB
GPU: NVIDIA RTX 4090 (24GB VRAM) 또는 A100
Disk: 200GB SSD
OS: Linux
Python: 3.11+
CUDA: 12.0+
```

#### 모델별 메모리 요구사항
```
경량 모델 (DistilBERT):
- 모델 크기: 약 250MB
- 추론 시 메모리: 약 1GB
- 배치 처리: 약 2GB

중형 모델 (BERT-base):
- 모델 크기: 약 440MB
- 추론 시 메모리: 약 2GB
- 배치 처리: 약 4GB

대형 모델 (GPT-2, Llama 2 7B):
- 모델 크기: 약 7GB
- 추론 시 메모리: 약 14GB
- 배치 처리: 약 20GB
```

#### 설치 가이드
```bash
# Transformers 라이브러리
pip install transformers torch
pip install sentencepiece tokenizers

# 감정 분석
pip install transformers[torch] torch

# 벡터 DB (선택적)
pip install pinecone-client
# 또는
pip install weaviate-client
```

---

## 5. Vector DB 서버 (선택적)

### Pinecone (클라우드 서비스)
```
스펙: 클라우드 제공 (관리 불필요)
용량: 
- Starter: 100K 벡터
- Standard: 1M 벡터
- Enterprise: 무제한
```

### Weaviate (자체 호스팅)

#### 최소 스펙
```
CPU: 4 Core
RAM: 8GB
Disk: 50GB SSD
OS: Linux
```

#### 권장 스펙
```
CPU: 8 Core
RAM: 16GB
Disk: 200GB SSD
OS: Linux
```

#### 설치 가이드
```bash
# Docker로 실행
docker run -d \
  --name weaviate \
  -p 8080:8080 \
  -e QUERY_DEFAULTS_LIMIT=25 \
  -e AUTHENTICATION_ANONYMOUS_ACCESS_ENABLED=true \
  -e PERSISTENCE_DATA_PATH=/var/lib/weaviate \
  -v weaviate_data:/var/lib/weaviate \
  semitechnologies/weaviate:latest
```

---

## 6. 전체 시스템 구성 예시

### 시나리오 1: 최소 구성 (개발/테스트)

```
서버 1: 통합 서버
- Spring Boot + MariaDB + Redis
- CPU: 4 Core
- RAM: 16GB
- Disk: 200GB SSD
- 비용: 약 $50-100/월

서버 2: AI 서비스 (CPU만)
- Prediction Service + Strategy Optimizer + NLP Service
- CPU: 8 Core
- RAM: 16GB
- Disk: 100GB SSD
- GPU: 없음
- 비용: 약 $50-80/월

총 비용: 약 $100-180/월
```

### 시나리오 2: 권장 구성 (소규모 운영)

```
서버 1: Spring Boot 애플리케이션
- CPU: 4 Core
- RAM: 8GB
- Disk: 100GB SSD
- 비용: 약 $40-60/월

서버 2: MariaDB
- CPU: 4 Core
- RAM: 8GB
- Disk: 200GB SSD
- 비용: 약 $50-70/월

서버 3: Redis
- CPU: 2 Core
- RAM: 4GB
- Disk: 20GB
- 비용: 약 $20-30/월

서버 4: AI 서비스 (GPU 포함)
- Prediction Service + Strategy Optimizer + NLP Service
- CPU: 8 Core
- RAM: 16GB
- GPU: RTX 3060 (12GB)
- Disk: 200GB SSD
- 비용: 약 $150-200/월

총 비용: 약 $260-360/월
```

### 시나리오 3: 프로덕션 구성 (대규모 운영)

```
서버 1-2: Spring Boot (로드 밸런싱)
- 각각: CPU 8 Core, RAM 16GB, Disk 200GB SSD
- 비용: 약 $100-150/월 × 2 = $200-300/월

서버 3: MariaDB (Master)
- CPU: 8 Core
- RAM: 16GB
- Disk: 500GB SSD (RAID 10)
- 비용: 약 $150-200/월

서버 4: MariaDB (Slave, 백업)
- CPU: 4 Core
- RAM: 8GB
- Disk: 500GB SSD
- 비용: 약 $80-120/월

서버 5: Redis (클러스터)
- CPU: 4 Core
- RAM: 8GB
- Disk: 50GB
- 비용: 약 $40-60/월

서버 6: Prediction Service
- CPU: 16 Core
- RAM: 32GB
- GPU: RTX 4090 (24GB)
- Disk: 200GB SSD
- 비용: 약 $300-400/월

서버 7: Strategy Optimizer
- CPU: 16 Core
- RAM: 32GB
- GPU: RTX 3080 (10GB)
- Disk: 200GB SSD
- 비용: 약 $250-350/월

서버 8: NLP Service
- CPU: 16 Core
- RAM: 32GB
- GPU: RTX 4090 (24GB)
- Disk: 200GB SSD
- 비용: 약 $300-400/월

총 비용: 약 $1,320-1,830/월
```

---

## 7. 클라우드 서비스별 추천

### AWS
```
- EC2: t3.medium (Spring Boot), t3.large (DB)
- RDS: db.t3.medium (MariaDB)
- ElastiCache: cache.t3.micro (Redis)
- EC2 GPU: g4dn.xlarge (AI 서비스)
- 예상 비용: $200-500/월
```

### Google Cloud Platform (GCP)
```
- Compute Engine: n1-standard-4 (Spring Boot)
- Cloud SQL: db-n1-standard-4 (MariaDB)
- Memorystore: basic-tier (Redis)
- Compute Engine GPU: n1-standard-8 + T4 GPU (AI 서비스)
- 예상 비용: $250-600/월
```

### Azure
```
- Virtual Machine: Standard_B4ms (Spring Boot)
- Azure Database for MariaDB: Gen5, 4 vCore
- Azure Cache for Redis: Basic C1
- Virtual Machine GPU: Standard_NC6s_v3 (AI 서비스)
- 예상 비용: $300-700/월
```

---

## 8. 네트워크 요구사항

### 대역폭
```
- Spring Boot → MariaDB: 약 10Mbps
- Spring Boot → Redis: 약 5Mbps
- Spring Boot → AI 서비스: 약 50Mbps (모델 추론 시)
- 총 대역폭: 약 100Mbps 이상 권장
```

### 포트 설정
```
Spring Boot: 8080
MariaDB: 3306
Redis: 6379
Prediction Service: 8000
Strategy Optimizer: 8001
NLP Service: 8002
```

---

## 9. 모니터링 및 로깅

### 모니터링 서버 (선택적)
```
CPU: 2 Core
RAM: 4GB
Disk: 50GB
OS: Linux
소프트웨어: Prometheus + Grafana
```

### 로그 서버 (선택적)
```
CPU: 2 Core
RAM: 4GB
Disk: 100GB
OS: Linux
소프트웨어: ELK Stack (Elasticsearch, Logstash, Kibana)
```

---

## 10. 백업 및 재해복구

### 백업 스토리지
```
- MariaDB 백업: 일일 전체 백업 + 증분 백업
- 예상 용량: 50GB (30일 보관)
- 권장: 클라우드 스토리지 (S3, GCS, Azure Blob)
```

### 재해복구
```
- 백업 복구 시간 목표 (RTO): 1시간
- 백업 복구 시점 목표 (RPO): 24시간
- 권장: 다른 리전에 백업 서버 구축
```

---

## 11. 보안 요구사항

### 방화벽 설정
```
- SSH: 22 (제한된 IP만 허용)
- HTTP/HTTPS: 80, 443
- 데이터베이스: 3306 (내부 네트워크만)
- Redis: 6379 (내부 네트워크만)
- AI 서비스: 8000-8002 (내부 네트워크만)
```

### SSL/TLS 인증서
```
- Let's Encrypt (무료)
- 또는 상용 인증서
```

---

## 12. 설치 체크리스트

### Spring Boot 서버
- [ ] Java 17 설치
- [ ] 애플리케이션 빌드 및 배포
- [ ] systemd 서비스 등록
- [ ] 로그 디렉토리 생성 (`/LOG`)

### MariaDB 서버
- [ ] MariaDB 11.8.5+ 설치
- [ ] 데이터베이스 및 사용자 생성
- [ ] my.cnf 설정
- [ ] 백업 스크립트 설정

### Redis 서버
- [ ] Redis 7.0+ 설치
- [ ] redis.conf 설정
- [ ] 비밀번호 설정
- [ ] systemd 서비스 등록

### AI 서비스 서버
- [ ] Python 3.11+ 설치
- [ ] CUDA 및 cuDNN 설치 (GPU 사용 시)
- [ ] 가상환경 생성 및 의존성 설치
- [ ] 모델 파일 다운로드
- [ ] systemd 서비스 등록

---

## 13. 성능 튜닝 가이드

### Spring Boot
```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
```

### MariaDB
```sql
-- 인덱스 최적화
CREATE INDEX idx_order_account_date ON orders(account_no, created_at);
CREATE INDEX idx_strategy_account_type ON strategies(account_no, strategy_type);
```

### Redis
```conf
# redis.conf
maxmemory-policy allkeys-lru
save ""  # 지속성 비활성화 (성능 우선)
```

---

## 14. 비용 최적화 팁

1. **개발 환경**: 최소 스펙 사용
2. **프로덕션**: 필요에 따라 스펙 조정
3. **GPU**: 초기에는 CPU만 사용, 필요 시 GPU 추가
4. **클라우드**: 스팟 인스턴스 활용 (AI 서비스)
5. **캐싱**: Redis로 DB 부하 감소

---

## 참고 문서

- [배포 가이드](./01-deployment-guide.md)
- [운영 가이드](./02-operations-guide.md)
- [성능 영향 분석](../02-architecture/06-performance-impact-analysis.md)

## 문서 변경 이력

| 버전 | 일자 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-28 | System | 문서 정리 및 구조화 |
