# 보안 리팩토링 가이드

## 개요

Investment Banking Securities Firm Level 보안 규칙에 맞춰 전체 프로젝트의 보안을 강화하는 리팩토링을 수행했습니다. 이 문서는 구현된 보안 기능, 설정 방법, 운영 가이드를 제공합니다.

**작성일**: 2026-01-28  
**버전**: 1.0.0

## 목차

1. [보안 리팩토링 개요](#보안-리팩토링-개요)
2. [구현된 보안 기능](#구현된-보안-기능)
3. [환경 변수 설정](#환경-변수-설정)
4. [보안 모범 사례](#보안-모범-사례)
5. [운영 가이드](#운영-가이드)
6. [트러블슈팅](#트러블슈팅)

---

## 보안 리팩토링 개요

### 목표

- Zero Trust 원칙 적용
- 최소 권한 원칙 적용
- 보안을 기본값으로 설정
- OWASP Top 10 보안 취약점 방어
- 규제 준수 (Investment Banking Securities Firm Level)

### 주요 변경 사항

1. **인증 및 인가 강화**
   - JWT TTL 단축 (24시간 → 1시간)
   - 재인증 메커니즘 추가
   - 계정 잠금 메커니즘 구현

2. **시크릿 관리**
   - 하드코딩된 시크릿 제거
   - 환경 변수 기반 시크릿 관리

3. **로깅 보안**
   - PII 마스킹 유틸리티 구현
   - 보안 이벤트 감사 로깅

4. **API 보안**
   - Rate Limiting 구현
   - XSS 방지 필터
   - 보안 헤더 추가

5. **입력 검증 강화**
   - 비밀번호 정책 강화
   - 입력 검증 강화

---

## 구현된 보안 기능

### 1. 인증 및 인가

#### 1.1 JWT 토큰 TTL 단축

**변경 사항**:
- 기본 TTL: 24시간 → 1시간
- 환경 변수로 제어 가능: `INVESTMENT_JWT_EXPIRATION`

**파일**: `src/main/java/com/investment/common/security/JwtTokenProvider.java`

**설정**:
```yaml
investment:
  security:
    jwt-expiration: ${INVESTMENT_JWT_EXPIRATION:3600000}  # 1시간 (밀리초)
```

#### 1.2 재인증 메커니즘

**기능**:
- 비밀번호 변경 시 현재 비밀번호 재확인 필수
- API 키 변경 시 현재 비밀번호 재확인 필수

**파일**: `src/main/java/com/investment/auth/service/AuthService.java`

**사용 예시**:
```json
PUT /api/v1/auth/mypage
{
  "currentPassword": "현재비밀번호",
  "password": "새비밀번호",
  "appKey": "새API키"
}
```

#### 1.3 계정 잠금 메커니즘

**기능**:
- 5회 연속 인증 실패 시 계정 잠금 (기본 15분)
- Redis 기반 분산 잠금 관리
- 인증 성공 시 자동 잠금 해제

**파일**: `src/main/java/com/investment/auth/service/AccountLockService.java`

**설정**:
```bash
ACCOUNT_LOCK_ENABLED=true
ACCOUNT_LOCK_MAX_ATTEMPTS=5
ACCOUNT_LOCK_DURATION_MINUTES=15
```

### 2. 시크릿 관리

#### 2.1 하드코딩된 시크릿 제거

**변경 사항**:
- `application-local.yml`에서 하드코딩된 API 키 제거
- 모든 시크릿을 환경 변수로 관리

**파일**: 
- `src/main/resources/application-local.yml`
- `.env.example` (새로 생성)

#### 2.2 환경 변수 기반 시크릿 관리

**필수 환경 변수**:
```bash
# 암호화 키 (AES-256, Base64 인코딩)
INVESTMENT_ENCRYPTION_KEY=

# JWT 시크릿 키 (최소 256비트)
INVESTMENT_JWT_SECRET=
```

**생성 방법**:
```bash
# 암호화 키 생성 (Java 코드 실행)
java -cp . com.investment.common.security.EncryptionUtil
```

### 3. 로깅 보안

#### 3.1 PII 마스킹 유틸리티

**기능**:
- 사용자 ID, 사용자명, 이메일 등 PII 마스킹
- 토큰, 비밀번호, API 키 마스킹
- 로그 메시지 자동 마스킹

**파일**: `src/main/java/com/investment/common/security/LogMaskingUtil.java`

**사용 예시**:
```java
log.info("로그인 성공: userId={}, username={}", 
    LogMaskingUtil.maskUserId(userId), 
    LogMaskingUtil.maskUsername(username));
```

**마스킹 규칙**:
- 사용자 ID: 앞 2자리만 표시 (예: `ab********`)
- 사용자명: 앞 2자리만 표시
- 토큰/API 키: 앞 4자리만 표시
- 비밀번호: 완전 마스킹 (`****`)

#### 3.2 보안 이벤트 감사 로깅

**기능**:
- 인증 성공/실패 로깅
- 권한 위반 로깅
- 계정 잠금/해제 로깅
- 비정상 행동 감지 로깅
- 구조화된 JSON 로그 형식

**파일**: `src/main/java/com/investment/common/security/SecurityAuditService.java`

**로그 형식**:
```json
{
  "timestamp": "2026-01-28T10:30:00",
  "eventType": "AUTHENTICATION_SUCCESS",
  "userId": "ab********",
  "username": "us********",
  "details": {
    "ipAddress": "192.168.1.1"
  }
}
```

**이벤트 타입**:
- `AUTHENTICATION_SUCCESS`: 인증 성공
- `AUTHENTICATION_FAILURE`: 인증 실패
- `AUTHORIZATION_FAILURE`: 권한 위반
- `ACCOUNT_LOCKED`: 계정 잠금
- `ACCOUNT_UNLOCKED`: 계정 잠금 해제
- `PASSWORD_CHANGED`: 비밀번호 변경
- `API_KEY_CHANGED`: API 키 변경
- `SUSPICIOUS_ACTIVITY`: 비정상 행동
- `RATE_LIMIT_EXCEEDED`: Rate Limit 초과
- `TOKEN_EXPIRED`: 토큰 만료
- `TOKEN_INVALID`: 토큰 무효

### 4. API 보안

#### 4.1 Rate Limiting

**기능**:
- 인증 API: IP 기반 rate limiting (분당 5회)
- 일반 API: 사용자 기반 rate limiting (분당 100회)
- Redis 기반 분산 rate limiting

**파일**: `src/main/java/com/investment/common/security/RateLimitFilter.java`

**설정**:
```bash
RATE_LIMIT_AUTH_ENABLED=true
RATE_LIMIT_AUTH_REQUESTS_PER_MINUTE=5
RATE_LIMIT_API_ENABLED=true
RATE_LIMIT_API_REQUESTS_PER_MINUTE=100
```

**응답** (Rate Limit 초과 시):
```json
{
  "code": "RATE_LIMIT_EXCEEDED",
  "message": "요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.",
  "retryAfter": 60
}
```

#### 4.2 XSS 방지 필터

**기능**:
- 요청 파라미터에서 XSS 패턴 검사
- XSS 패턴 발견 시 요청 거부

**파일**: `src/main/java/com/investment/common/security/XssFilter.java`

**검사 패턴**:
- `<script>`, `</script>`
- `javascript:`
- `onerror=`, `onload=`, `onclick=`
- `eval(`, `expression(`
- `<iframe>`, `<object>`, `<embed>`

#### 4.3 보안 헤더

**기능**:
- X-Content-Type-Options: nosniff
- X-Frame-Options: DENY
- X-XSS-Protection: 1; mode=block
- Strict-Transport-Security (HTTPS 환경)
- Content-Security-Policy
- Referrer-Policy
- Permissions-Policy

**파일**: `src/main/java/com/investment/config/SecurityHeadersConfig.java`

**설정**:
```bash
SECURITY_HSTS_MAX_AGE=31536000  # 1년 (초)
```

### 5. 쿠키 보안

#### 5.1 보안 강화된 쿠키 설정

**기능**:
- HttpOnly: JavaScript 접근 방지
- Secure: HTTPS 환경에서만 전송 (환경 변수로 제어)
- SameSite: CSRF 공격 방지 (환경 변수로 제어)

**파일**: `src/main/java/com/investment/auth/controller/AuthController.java`

**설정**:
```bash
COOKIE_SECURE=true          # HTTPS 환경에서는 true
COOKIE_SAME_SITE=Strict     # Strict, Lax, None
```

### 6. CORS 설정 강화

**기능**:
- 프로덕션: 특정 도메인만 허용
- 개발/로컬: 제한적 허용
- `allowCredentials`와 `allowedOrigins` 호환성 확인

**파일**: `src/main/java/com/investment/config/SecurityConfig.java`

**설정**:
```bash
# 프로덕션: 특정 도메인만 허용
CORS_ALLOWED_ORIGINS=https://example.com,https://www.example.com

# 개발: 모든 origin 허용 (권장하지 않음)
CORS_ALLOWED_ORIGINS=*
```

### 7. 비밀번호 정책 강화

**기능**:
- 최소 8자, 최대 100자
- 대문자, 소문자, 숫자, 특수문자 중 3종류 이상 포함
- 일반적인 약한 비밀번호 패턴 금지

**파일**: 
- `src/main/java/com/investment/common/validation/PasswordValidator.java`
- `src/main/java/com/investment/common/validation/ValidPassword.java`

**사용 예시**:
```java
@ValidPassword
private String password;
```

**검증 규칙**:
- 같은 문자 4번 이상 반복 금지
- 연속된 숫자/영문 금지
- 일반적인 약한 비밀번호 금지 (`password`, `admin`, `123456` 등)

### 8. 에러 처리 보안

**기능**:
- 프로덕션 환경에서 스택 트레이스 노출 방지
- 환경 변수로 디버그 모드 제어
- 상세 에러는 내부 로그에만 기록

**파일**: `src/main/java/com/investment/common/exception/GlobalExceptionHandler.java`

**설정**:
```bash
DEBUG_MODE=false  # 프로덕션에서는 false
```

**응답 예시** (프로덕션):
```json
{
  "code": "INTERNAL_ERROR",
  "message": "시스템 오류가 발생했습니다",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-01-28T10:30:00"
}
```

### 9. RBAC 기반 권한 체크

**기능**:
- 역할 기반 접근 제어 (Role-Based Access Control)
- 메서드 레벨 보안 활성화
- `@PreAuthorize` 어노테이션 지원

**파일**: 
- `src/main/java/com/investment/common/security/Role.java`
- `src/main/java/com/investment/config/SecurityConfig.java`

**사용 예시**:
```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/admin/users")
public ResponseEntity<List<User>> getUsers() {
    // ...
}
```

---

## 환경 변수 설정

### 필수 환경 변수

```bash
# 데이터베이스 설정
SPRING_DATASOURCE_URL=jdbc:mariadb://localhost:3306/investment?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Seoul
SPRING_DATASOURCE_USERNAME=investment
SPRING_DATASOURCE_PASSWORD=password

# 보안 설정
INVESTMENT_ENCRYPTION_KEY=          # AES-256 키 (Base64 인코딩, 필수)
INVESTMENT_JWT_SECRET=              # JWT 시크릿 키 (최소 256비트, 필수)
INVESTMENT_JWT_EXPIRATION=3600000  # JWT 만료 시간 (밀리초, 기본: 1시간)

# 쿠키 보안 설정
COOKIE_SECURE=true                  # HTTPS 환경에서는 true
COOKIE_SAME_SITE=Strict            # Strict, Lax, None

# CORS 설정
CORS_ALLOWED_ORIGINS=https://example.com,https://www.example.com

# Rate Limiting 설정
RATE_LIMIT_AUTH_ENABLED=true
RATE_LIMIT_AUTH_REQUESTS_PER_MINUTE=5
RATE_LIMIT_API_ENABLED=true
RATE_LIMIT_API_REQUESTS_PER_MINUTE=100

# 계정 잠금 설정
ACCOUNT_LOCK_ENABLED=true
ACCOUNT_LOCK_MAX_ATTEMPTS=5
ACCOUNT_LOCK_DURATION_MINUTES=15

# 프로덕션 환경 설정
SPRING_PROFILES_ACTIVE=prod
DEBUG_MODE=false
```

### 선택적 환경 변수

```bash
# Redis 설정
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# 보안 헤더 설정
SECURITY_HSTS_MAX_AGE=31536000  # 1년 (초)

# 한국투자증권 API 설정 (시스템 레벨)
KOREA_INVESTMENT_APP_KEY=
KOREA_INVESTMENT_APP_SECRET=
KOREA_INVESTMENT_SERVER_TYPE=1
```

### 환경 변수 생성 가이드

#### 1. 암호화 키 생성

```bash
# Java 코드로 생성
java -cp . com.investment.common.security.EncryptionUtil

# 또는 직접 생성 (Base64 인코딩된 32바이트 키)
openssl rand -base64 32
```

#### 2. JWT 시크릿 키 생성

```bash
# 최소 256비트 (32자 이상) 랜덤 문자열 생성
openssl rand -base64 32
```

#### 3. .env 파일 생성

```bash
# .env.example을 복사하여 .env 파일 생성
cp .env.example .env

# .env 파일 편집하여 실제 값 입력
# 주의: .env 파일은 절대 Git에 커밋하지 마세요!
```

---

## 보안 모범 사례

### 1. 시크릿 관리

✅ **권장 사항**:
- 모든 시크릿을 환경 변수로 관리
- 프로덕션 시크릿은 Kubernetes Secret 또는 AWS Secrets Manager 사용
- 시크릿은 정기적으로 로테이션
- 코드에 하드코딩 금지

❌ **금지 사항**:
- 코드에 시크릿 하드코딩
- Git에 시크릿 커밋
- 로그에 시크릿 출력

### 2. 비밀번호 정책

✅ **권장 사항**:
- 강력한 비밀번호 정책 적용
- 정기적인 비밀번호 변경 권장
- 비밀번호 재사용 방지

❌ **금지 사항**:
- 약한 비밀번호 허용
- 비밀번호를 평문으로 저장
- 비밀번호를 로그에 출력

### 3. 로깅

✅ **권장 사항**:
- PII 마스킹 적용
- 보안 이벤트 로깅
- 구조화된 로그 형식 사용

❌ **금지 사항**:
- 로그에 PII 노출
- 로그에 시크릿 출력
- 로그에 스택 트레이스 노출 (프로덕션)

### 4. 인증 및 인가

✅ **권장 사항**:
- 짧은 JWT TTL 사용
- 재인증 메커니즘 적용
- 계정 잠금 메커니즘 적용
- 최소 권한 원칙 적용

❌ **금지 사항**:
- 긴 JWT TTL 사용
- 재인증 없이 민감한 작업 수행
- 무제한 인증 시도 허용

### 5. API 보안

✅ **권장 사항**:
- Rate Limiting 적용
- 입력 검증 강화
- XSS 방지 필터 적용
- 보안 헤더 추가

❌ **금지 사항**:
- Rate Limiting 없이 API 노출
- 입력 검증 없이 데이터 처리
- XSS 취약점 방치

---

## 운영 가이드

### 1. 배포 전 체크리스트

- [ ] 모든 환경 변수 설정 완료
- [ ] 암호화 키 생성 및 설정
- [ ] JWT 시크릿 키 생성 및 설정
- [ ] CORS 설정 확인 (프로덕션)
- [ ] 쿠키 Secure 플래그 확인 (HTTPS 환경)
- [ ] Rate Limiting 설정 확인
- [ ] 계정 잠금 설정 확인
- [ ] 보안 헤더 설정 확인
- [ ] DEBUG_MODE=false 확인 (프로덕션)

### 2. 모니터링

#### 2.1 보안 이벤트 모니터링

```bash
# 인증 실패 로그 확인
grep "AUTHENTICATION_FAILURE" /LOG/investment-choi.log

# 계정 잠금 로그 확인
grep "ACCOUNT_LOCKED" /LOG/investment-choi.log

# Rate Limit 초과 로그 확인
grep "RATE_LIMIT_EXCEEDED" /LOG/investment-choi.log
```

#### 2.2 보안 메트릭

- 인증 실패 횟수
- 계정 잠금 횟수
- Rate Limit 초과 횟수
- 비정상 행동 감지 횟수

### 3. 정기 점검 사항

#### 3.1 주간 점검

- [ ] 보안 이벤트 로그 검토
- [ ] 계정 잠금 통계 확인
- [ ] Rate Limit 통계 확인

#### 3.2 월간 점검

- [ ] 시크릿 로테이션 계획 수립
- [ ] 보안 취약점 스캔
- [ ] 의존성 보안 업데이트 확인

#### 3.3 분기별 점검

- [ ] 보안 정책 검토
- [ ] 접근 권한 검토
- [ ] 보안 감사 수행

### 4. 시크릿 로테이션

#### 4.1 암호화 키 로테이션

```bash
# 1. 새 암호화 키 생성
NEW_KEY=$(openssl rand -base64 32)

# 2. 환경 변수 업데이트
export INVESTMENT_ENCRYPTION_KEY=$NEW_KEY

# 3. 애플리케이션 재시작
# 주의: 기존 암호화된 데이터는 새 키로 재암호화 필요
```

#### 4.2 JWT 시크릿 로테이션

```bash
# 1. 새 JWT 시크릿 생성
NEW_SECRET=$(openssl rand -base64 32)

# 2. 환경 변수 업데이트
export INVESTMENT_JWT_SECRET=$NEW_SECRET

# 3. 애플리케이션 재시작
# 주의: 기존 JWT 토큰은 무효화됨 (사용자 재로그인 필요)
```

### 5. 비상 대응

#### 5.1 보안 사고 발생 시

1. **즉시 조치**:
   - 영향받은 계정 잠금
   - 관련 API 키 무효화
   - 보안 이벤트 로그 확인

2. **조사**:
   - 보안 이벤트 로그 분석
   - 접근 로그 분석
   - 영향 범위 파악

3. **복구**:
   - 취약점 패치
   - 시크릿 로테이션
   - 사용자 알림

#### 5.2 계정 잠금 해제

```bash
# Redis에서 직접 잠금 해제 (긴급 시)
redis-cli DEL "account_lock:username"
redis-cli DEL "auth_failure:username"
```

---

## 트러블슈팅

### 1. 인증 실패

**증상**: 로그인 실패

**원인 및 해결**:
- 계정 잠금 확인: Redis에서 `account_lock:username` 키 확인
- 비밀번호 정책 확인: 비밀번호가 정책에 맞는지 확인
- JWT 토큰 만료: 토큰이 만료되었는지 확인

### 2. Rate Limit 초과

**증상**: `RATE_LIMIT_EXCEEDED` 에러

**원인 및 해결**:
- 요청 빈도 확인: 분당 요청 수 확인
- Rate Limit 설정 확인: 환경 변수 확인
- Redis 연결 확인: Redis 서버 상태 확인

### 3. 암호화 키 오류

**증상**: `복호화 실패` 에러

**원인 및 해결**:
- 암호화 키 확인: 환경 변수 `INVESTMENT_ENCRYPTION_KEY` 확인
- 키 형식 확인: Base64 인코딩된 32바이트 키인지 확인
- 키 변경 이력 확인: 최근 키 변경 여부 확인

### 4. CORS 오류

**증상**: 브라우저에서 CORS 에러

**원인 및 해결**:
- CORS 설정 확인: `CORS_ALLOWED_ORIGINS` 환경 변수 확인
- Origin 확인: 요청 Origin이 허용 목록에 있는지 확인
- `allowCredentials` 확인: `*`와 함께 사용 불가

### 5. 쿠키 설정 오류

**증상**: 쿠키가 설정되지 않음

**원인 및 해결**:
- Secure 플래그 확인: HTTPS 환경에서 `COOKIE_SECURE=true` 확인
- SameSite 설정 확인: `COOKIE_SAME_SITE` 환경 변수 확인
- 브라우저 개발자 도구에서 쿠키 확인

### 6. 보안 헤더 미적용

**증상**: 보안 헤더가 응답에 없음

**원인 및 해결**:
- 필터 순서 확인: `SecurityHeadersConfig`가 필터 체인에 등록되었는지 확인
- HTTPS 확인: HSTS 헤더는 HTTPS 환경에서만 적용
- 로그 확인: 필터 오류 로그 확인

---

## 참고 자료

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security 공식 문서](https://spring.io/projects/spring-security)
- [JWT Best Practices](https://datatracker.ietf.org/doc/html/rfc8725)
- [CORS 정책 가이드](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS)

---

## 문서 변경 이력

| 버전 | 일자 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-28 | System | 문서 정리 및 구조화 |
