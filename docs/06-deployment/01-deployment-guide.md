# 배포 가이드

## 1. 배포 환경

### 1.1 지원 환경
- **로컬 개발**: Windows, macOS, Linux
- **프로덕션**: Linux (Kubernetes 권장)

### 1.2 요구사항
- **Java**: Java 11 (SE)
- **데이터베이스**: MariaDB 11.8.5 이상
- **메모리**: 최소 512MB, 권장 1GB 이상
- **디스크**: 최소 1GB 여유 공간

## 2. 빌드

### 2.1 로컬 빌드
```bash
# Gradle Wrapper 사용
./gradlew build

# 또는
gradlew.bat build  # Windows
```

### 2.2 JAR 파일 생성
```bash
./gradlew bootJar
```

생성된 JAR 파일: `build/libs/investment-choi-1.0.0.jar`

## 3. 로컬 실행

### 3.1 개발 모드
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 3.2 JAR 실행
```bash
java -jar build/libs/investment-choi-1.0.0.jar --spring.profiles.active=local
```

## 4. Docker 배포

### 4.1 Dockerfile (예시)
```dockerfile
FROM openjdk:11-jre-slim

WORKDIR /app

COPY build/libs/investment-choi-1.0.0.jar app.jar

# 로그 디렉토리 생성
RUN mkdir -p /LOG

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 4.2 Docker 이미지 빌드
```bash
docker build -t investment-choi:1.0.0 .
```

### 4.3 Docker 컨테이너 실행
```bash
docker run -d \
  --name investment-choi \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mariadb://host.docker.internal:3306/investment \
  -e SPRING_DATASOURCE_USERNAME=investment \
  -e SPRING_DATASOURCE_PASSWORD=password \
  -v /LOG:/LOG \
  investment-choi:1.0.0
```

## 5. Kubernetes 배포

### 5.1 Deployment 예시
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: investment-choi
spec:
  replicas: 2
  selector:
    matchLabels:
      app: investment-choi
  template:
    metadata:
      labels:
        app: investment-choi
    spec:
      containers:
      - name: investment-choi
        image: investment-choi:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: url
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: password
        volumeMounts:
        - name: log-volume
          mountPath: /LOG
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
      volumes:
      - name: log-volume
        emptyDir: {}
```

### 5.2 Service 예시
```yaml
apiVersion: v1
kind: Service
metadata:
  name: investment-choi
spec:
  selector:
    app: investment-choi
  ports:
  - port: 80
    targetPort: 8080
  type: LoadBalancer
```

## 6. 환경 변수 설정

### 6.1 필수 환경 변수
```bash
# 데이터베이스
SPRING_DATASOURCE_URL=jdbc:mariadb://localhost:3306/investment
SPRING_DATASOURCE_USERNAME=investment
SPRING_DATASOURCE_PASSWORD=password

# 시장 데이터 API
MARKET_DATA_PROVIDER=korea-investment
KOREA_INVESTMENT_APP_KEY=your_app_key
KOREA_INVESTMENT_APP_SECRET=your_app_secret
KOREA_INVESTMENT_SERVER_TYPE=1  # 1: 모의투자, 0: 실거래
MARKET_DATA_USE_MOCK_DATA=false  # 개발/테스트 시 true
```

### 6.2 선택적 환경 변수
```bash

# 거래 설정
MAX_INVESTMENT_AMOUNT=1000000
MIN_INVESTMENT_AMOUNT=10000
```

## 7. 데이터베이스 설정

### 7.1 데이터베이스 생성
```sql
CREATE DATABASE investment CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 7.2 사용자 생성
```sql
CREATE USER 'investment'@'%' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON investment.* TO 'investment'@'%';
FLUSH PRIVILEGES;
```

### 7.3 스키마 생성
```bash
mysql -u investment -p investment < src/main/resources/db/schema.sql
```
schema.sql로 1회 생성한 DB에 앱을 기동하면 Flyway가 **baseline 20**을 적용합니다. 이후 스키마 변경은 `db/migration/V21__*.sql` 형식으로 추가 시 자동 적용됩니다.

## 8. 로그 설정

### 8.1 로그 경로
- **로컬**: `logs/investment-choi.log`
- **Kubernetes**: `/LOG/investment-choi.log`

### 8.2 로그 로테이션
- 최대 파일 크기: 100MB
- 보관 기간: 30일

## 9. 헬스 체크

### 9.1 Health Endpoint
```
GET /actuator/health
```

**응답**:
```json
{
  "status": "UP"
}
```

### 9.2 Liveness Probe
- 경로: `/actuator/health`
- 초기 지연: 60초
- 주기: 10초

### 9.3 Readiness Probe
- 경로: `/actuator/health`
- 초기 지연: 30초
- 주기: 5초

## 10. 모니터링

### 10.1 Metrics Endpoint
```
GET /actuator/metrics
```

### 10.2 주요 메트릭
- `http.server.requests`: HTTP 요청 수
- `jvm.memory.used`: JVM 메모리 사용량
- `jvm.gc.pause`: GC 일시 정지 시간

## 11. 롤백 전략

### 11.1 배포 전 백업
- 데이터베이스 백업 필수
- 이전 버전 JAR 파일 보관

### 11.2 롤백 절차
1. 이전 버전 JAR로 교체
2. 데이터베이스 롤백 (필요 시)
3. 헬스 체크 확인

## 12. 보안 고려사항

### 12.1 시크릿 관리
- 환경 변수는 Kubernetes Secret 사용
- 코드에 하드코딩 금지

### 12.2 네트워크 보안
- 프로덕션 환경에서는 HTTPS 사용
- 방화벽 규칙 설정

### 12.3 데이터베이스 보안
- 최소 권한 원칙 적용
- SSL/TLS 연결 사용 (권장)

## 문서 변경 이력

| 버전 | 일자 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-28 | System | 문서 정리 및 구조화 |
