# API 토큰·인증 설정 가이드

## 1. 개요

### 1.1 문서 목적

본 문서는 자동투자 시스템의 실전 운영을 위한 외부 API 인증 정보 관리 가이드입니다.
토큰 발급, 갱신, 보안 관리에 대한 모범 사례를 제공합니다.

### 1.2 대상 API 목록

| API | 용도 | 인증 방식 | 필수 여부 |
|-----|------|-----------|-----------|
| 한국투자증권 KIS API | 국내 주식 시세·주문 | OAuth 2.0 (App Key/Secret) | 필수 |
| DART Open API | 한국 공시 정보 | API Key | 필수 |
| SEC EDGAR | 미국 공시 정보 | User-Agent 헤더 | 필수 |
| Yahoo Finance | 미국 시세 (yfinance) | 없음 (비공식 라이브러리) | 선택 |
| 네이버 금융 | 국내 뉴스 크롤링 | 없음 (웹 스크래핑) | 선택 |

---

## 2. 한국투자증권 KIS API

### 2.1 App Key/Secret 발급 절차

1. **KIS Developers 회원가입**
   - URL: https://apiportal.koreainvestment.com
   - 한국투자증권 계좌 필요

2. **앱 등록**
   - KIS Developers → 내 앱 관리 → 앱 등록
   - 앱 이름, 설명 입력
   - **모의투자용**과 **실거래용**을 각각 별도 등록 권장

3. **App Key/Secret 확인**
   - 앱 등록 완료 후 App Key, App Secret 발급
   - **발급 즉시 안전한 곳에 저장** (Secret은 재확인 불가할 수 있음)

### 2.2 토큰 발급 및 자동 갱신

#### Access Token 발급

```http
POST /oauth2/tokenP HTTP/1.1
Host: openapi.koreainvestment.com:9443
Content-Type: application/json

{
  "grant_type": "client_credentials",
  "appkey": "${APP_KEY}",
  "appsecret": "${APP_SECRET}"
}
```

**응답 예시:**
```json
{
  "access_token": "eyJ0eXAi...",
  "token_type": "Bearer",
  "expires_in": 86400
}
```

#### WebSocket Approval Key 발급

```http
POST /oauth2/Approval HTTP/1.1
Host: openapi.koreainvestment.com:9443
Content-Type: application/json

{
  "grant_type": "client_credentials",
  "appkey": "${APP_KEY}",
  "secretkey": "${APP_SECRET}"
}
```

#### 자동 갱신 전략

토큰 유효기간은 24시간입니다. 다음과 같이 자동 갱신을 설정합니다:

**방법 1: 장 시작 전 Crontab**
```bash
# /etc/cron.d/kis-token-refresh
# 매일 오전 8:30 (장 시작 30분 전) 토큰 갱신
30 8 * * 1-5 appuser /app/scripts/refresh-kis-token.sh
```

**방법 2: Spring Scheduler (구현됨)**
```java
@Scheduled(cron = "0 30 8 * * MON-FRI")
public void refreshTokenBeforeMarketOpen() {
    tokenService.refreshAccessToken();
    tokenService.refreshApprovalKey();
}
```

**방법 3: 만료 1시간 전 자동 갱신**
```java
// KoreaInvestmentTokenService.java 구현 참고
if (tokenExpiresAt.minusHours(1).isBefore(LocalDateTime.now())) {
    refreshAccessToken();
}
```

### 2.3 모의투자 / 실전 서버 구분

| 구분 | serverType | Base URL | 용도 |
|------|------------|----------|------|
| 모의투자 | `1` | `https://openapivts.koreainvestment.com:29443` | 개발·테스트 |
| 실전투자 | `0` | `https://openapi.koreainvestment.com:9443` | 운영 |

**주의사항:**
- 모의투자와 실전투자는 **별도 App Key/Secret** 사용 권장
- 테스트 코드에서는 **반드시 모의투자(serverType=1)** 사용
- 환경 변수로 서버 타입 전환 (`KOREA_INVESTMENT_SERVER_TYPE`)

### 2.4 TR_ID 구분

실거래와 모의투자는 일부 API에서 TR_ID가 다릅니다:

| API | 실거래 TR_ID | 모의투자 TR_ID |
|-----|--------------|----------------|
| 주식 현재가 | FHKST01010100 | FHKST01010100 |
| 매수 주문 | TTTC0802U | VTTC0802U |
| 매도 주문 | TTTC0801U | VTTC0801U |
| 체결 조회 | TTTC8001R | VTTC8001R |

---

## 3. 외부 뉴스 API

### 3.1 DART Open API (금융감독원 공시)

**발급 절차:**
1. https://opendart.fss.or.kr 회원가입
2. 인증키 신청 (무료)
3. 일일 요청 한도: 10,000건

**환경 변수:**
```bash
DART_API_KEY=your_dart_api_key
```

**사용 예시:**
```python
# investment-data-collector/collectors/dart_collector.py 참고
import requests

response = requests.get(
    "https://opendart.fss.or.kr/api/list.json",
    params={
        "crtfc_key": os.environ["DART_API_KEY"],
        "bgn_de": "20260101",
        "end_de": "20260220"
    }
)
```

### 3.2 SEC EDGAR (미국 공시)

**인증:** 별도 API Key 없음. User-Agent 헤더 필수.

**SEC 요구사항:**
- User-Agent에 이메일 포함 (봇 식별용)
- 요청 간격: 최소 0.1초

```python
headers = {
    "User-Agent": "YourAppName admin@yourdomain.com"
}
```

### 3.3 연합뉴스 / Google News RSS

**인증:** 없음 (공개 RSS)

**Rate Limit 준수:**
- 요청 간격: 최소 3분
- User-Agent 명시 권장

### 3.4 네이버 금융 (웹 스크래핑)

**인증:** 없음

**이용약관 준수 필수:**
- robots.txt 확인
- 요청 간격: 최소 2초
- User-Agent 명시
- 과도한 요청 금지

```python
headers = {
    "User-Agent": "Mozilla/5.0 (compatible; InvestBot/1.0)"
}
time.sleep(2)  # 2초 간격
```

### 3.5 Yahoo Finance (yfinance)

**인증:** 없음 (비공식 라이브러리)

**주의사항:**
- 비공식 라이브러리로 안정성 보장 없음
- Rate Limit 자동 적용됨
- 상업적 사용 시 Yahoo 이용약관 확인 필요

```python
import yfinance as yf
ticker = yf.Ticker("AAPL")
```

---

## 4. 보안 원칙

### 4.1 Secrets 관리 규칙

> ⚠️ **절대 금지사항**
> - 소스 코드에 API Key/Secret 직접 기입
> - Git 저장소에 .env 파일 커밋
> - 로그에 토큰/키 전문 출력

### 4.2 .env.example 템플릿

프로젝트 루트에 `.env.example` 파일을 유지합니다:

```bash
# .env.example
# 복사하여 .env로 사용 (절대 커밋 금지)

# ============================================
# 한국투자증권 KIS API
# ============================================
KOREA_INVESTMENT_APP_KEY=your_app_key_here
KOREA_INVESTMENT_APP_SECRET=your_app_secret_here
KOREA_INVESTMENT_SERVER_TYPE=1  # 1: 모의투자, 0: 실전

# 다중 계좌 (선택)
# KOREA_INVESTMENT_APP_KEY_REAL=your_real_app_key
# KOREA_INVESTMENT_APP_SECRET_REAL=your_real_app_secret

# ============================================
# DART Open API (금융감독원 공시)
# ============================================
DART_API_KEY=your_dart_api_key_here

# ============================================
# SEC EDGAR
# ============================================
SEC_USER_AGENT=YourAppName admin@yourdomain.com

# ============================================
# 데이터베이스
# ============================================
DATABASE_URL=jdbc:postgresql://localhost:5432/investment
DATABASE_USERNAME=investment
DATABASE_PASSWORD=your_db_password

# ============================================
# 로깅
# ============================================
LOG_LEVEL=INFO
```

### 4.3 Secret Manager 사용

운영 환경에서는 Secret Manager 사용을 권장합니다:

**AWS Secrets Manager:**
```yaml
# application-prod.yml
spring:
  config:
    import: aws-secretsmanager:investment-api-keys
```

**Kubernetes Secrets:**
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: kis-api-credentials
type: Opaque
stringData:
  app-key: ${KIS_APP_KEY}
  app-secret: ${KIS_APP_SECRET}
```

**환경 변수 주입:**
```yaml
# docker-compose.yml
services:
  backend:
    environment:
      - KOREA_INVESTMENT_APP_KEY=${KOREA_INVESTMENT_APP_KEY}
      - KOREA_INVESTMENT_APP_SECRET=${KOREA_INVESTMENT_APP_SECRET}
```

### 4.4 로그 마스킹 (LogMaskingUtil)

민감 정보는 로그에 마스킹 처리합니다:

```java
// LogMaskingUtil.java (구현 완료)
public static String maskAppKey(String appKey) {
    if (appKey == null || appKey.length() <= 8) {
        return "***";
    }
    return appKey.substring(0, 4) + "****" + appKey.substring(appKey.length() - 4);
}

// 사용 예시
log.info("KIS API 호출 - App Key: {}", LogMaskingUtil.maskAppKey(appKey));
// 출력: KIS API 호출 - App Key: PSbl****1234
```

**마스킹 대상:**
- App Key / App Secret
- Access Token
- 계좌번호
- 개인정보 (이름, 전화번호 등)

---

## 5. 환경별 설정

### 5.1 로컬 개발 환경

```bash
# .env (로컬용)
KOREA_INVESTMENT_SERVER_TYPE=1        # 모의투자
MARKET_DATA_USE_MOCK_DATA=true        # Mock 데이터 사용
LOG_LEVEL=DEBUG
```

### 5.2 개발 서버 (Dev)

```bash
KOREA_INVESTMENT_SERVER_TYPE=1        # 모의투자
MARKET_DATA_USE_MOCK_DATA=false       # 실제 API 호출
LOG_LEVEL=DEBUG
```

### 5.3 운영 서버 (Prod)

```bash
KOREA_INVESTMENT_SERVER_TYPE=0        # 실전투자
MARKET_DATA_USE_MOCK_DATA=false
LOG_LEVEL=INFO
```

### 5.4 환경별 체크리스트

| 항목 | 로컬 | Dev | Prod |
|------|------|-----|------|
| serverType | 1 (모의) | 1 (모의) | 0 (실전) |
| Mock 데이터 | 가능 | 권장하지 않음 | 금지 |
| 토큰 자동갱신 | 수동 | 자동 | 자동 |
| 로그 레벨 | DEBUG | DEBUG | INFO |
| Secret Manager | 불필요 | 권장 | 필수 |

---

## 6. 트러블슈팅

### 6.1 토큰 발급 실패

**증상:** `401 Unauthorized` 또는 토큰 발급 API 오류

**확인 사항:**
1. App Key/Secret이 정확한지 확인
2. 모의투자/실전투자 URL이 일치하는지 확인
3. KIS Developers에서 앱 상태가 '사용중'인지 확인

### 6.2 API 호출 시 인증 오류

**증상:** `{"rt_cd":"1","msg_cd":"OAPCO001","msg1":"유효하지 않은 접근토큰입니다."}`

**해결:**
```java
// 토큰 강제 갱신
tokenService.refreshAccessToken();
```

### 6.3 Rate Limit 초과

**증상:** `429 Too Many Requests`

**해결:**
- 요청 간격 조정 (최소 0.1초)
- Exponential Backoff 적용

---

## 7. 참고 문서

- [한국투자증권 API 가이드](../04-api/09-korea-investment-api-guide.md)
- [다중 계좌·실시간 스트리밍 설계](../02-architecture/14-multi-account-realtime-streaming.md)
- [뉴스·공시 수집 설계](../02-architecture/13-news-collection-design.md)
- [KIS Developers 공식 문서](https://apiportal.koreainvestment.com)
- [DART Open API](https://opendart.fss.or.kr)

---

## 문서 변경 이력

| 버전 | 일자 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-02-20 | System | 초기 문서 작성 — KIS API 토큰 발급, 외부 뉴스 API, 보안 원칙, 환경별 설정 |
