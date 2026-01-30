# 보안 설정 참조 가이드

## 개요

이 문서는 보안 관련 설정의 상세 참조 가이드입니다.

## 환경 변수 참조

### 보안 설정

#### INVESTMENT_ENCRYPTION_KEY
- **설명**: AES-256 암호화 키 (Base64 인코딩)
- **필수**: 예
- **형식**: Base64 인코딩된 32바이트 문자열
- **생성 방법**: `openssl rand -base64 32`
- **용도**: API 키, 토큰 등 민감한 데이터 암호화

#### INVESTMENT_JWT_SECRET
- **설명**: JWT 토큰 서명에 사용되는 시크릿 키
- **필수**: 예
- **형식**: 최소 256비트 (32자 이상) 문자열
- **생성 방법**: `openssl rand -base64 32`
- **용도**: JWT 토큰 생성 및 검증

#### INVESTMENT_JWT_EXPIRATION
- **설명**: JWT 토큰 만료 시간 (밀리초)
- **필수**: 아니오
- **기본값**: `3600000` (1시간)
- **권장값**: 
  - 개발: `3600000` (1시간)
  - 프로덕션: `1800000` (30분) 또는 `3600000` (1시간)

### 쿠키 보안 설정

#### COOKIE_SECURE
- **설명**: 쿠키 Secure 플래그
- **필수**: 아니오
- **기본값**: `true`
- **값**: `true` (HTTPS만), `false` (HTTP 허용)
- **권장**: 프로덕션에서는 항상 `true`

#### COOKIE_SAME_SITE
- **설명**: 쿠키 SameSite 속성
- **필수**: 아니오
- **기본값**: `Strict`
- **값**: `Strict`, `Lax`, `None`
- **권장**: `Strict` (최고 보안)

### CORS 설정

#### CORS_ALLOWED_ORIGINS
- **설명**: 허용할 Origin 목록
- **필수**: 아니오
- **기본값**: `*` (모든 origin 허용)
- **형식**: 콤마로 구분된 URL 목록
- **예시**: `https://example.com,https://www.example.com`
- **주의**: `*`와 `allowCredentials=true`는 함께 사용 불가

### Rate Limiting 설정

#### RATE_LIMIT_AUTH_ENABLED
- **설명**: 인증 API Rate Limiting 활성화 여부
- **필수**: 아니오
- **기본값**: `true`
- **값**: `true`, `false`

#### RATE_LIMIT_AUTH_REQUESTS_PER_MINUTE
- **설명**: 인증 API 분당 허용 요청 수
- **필수**: 아니오
- **기본값**: `5`
- **권장값**: `5` (무차별 대입 공격 방지)

#### RATE_LIMIT_API_ENABLED
- **설명**: 일반 API Rate Limiting 활성화 여부
- **필수**: 아니오
- **기본값**: `true`
- **값**: `true`, `false`

#### RATE_LIMIT_API_REQUESTS_PER_MINUTE
- **설명**: 일반 API 분당 허용 요청 수
- **필수**: 아니오
- **기본값**: `100`
- **권장값**: `100` (일반적인 사용 패턴 고려)

### 계정 잠금 설정

#### ACCOUNT_LOCK_ENABLED
- **설명**: 계정 잠금 기능 활성화 여부
- **필수**: 아니오
- **기본값**: `true`
- **값**: `true`, `false`

#### ACCOUNT_LOCK_MAX_ATTEMPTS
- **설명**: 계정 잠금 전 최대 인증 실패 횟수
- **필수**: 아니오
- **기본값**: `5`
- **권장값**: `5` (보안과 사용성 균형)

#### ACCOUNT_LOCK_DURATION_MINUTES
- **설명**: 계정 잠금 지속 시간 (분)
- **필수**: 아니오
- **기본값**: `15`
- **권장값**: `15` (무차별 대입 공격 방지)

### 보안 헤더 설정

#### SECURITY_HSTS_MAX_AGE
- **설명**: HSTS (Strict-Transport-Security) 최대 유지 시간 (초)
- **필수**: 아니오
- **기본값**: `31536000` (1년)
- **권장값**: `31536000` (1년)

### 디버그 설정

#### DEBUG_MODE
- **설명**: 디버그 모드 활성화 여부
- **필수**: 아니오
- **기본값**: `false`
- **값**: `true` (상세 에러 노출), `false` (일반 에러만)
- **주의**: 프로덕션에서는 항상 `false`

## application.yml 설정

### investment.security

```yaml
investment:
  security:
    encryption-key: ${INVESTMENT_ENCRYPTION_KEY:}
    jwt-secret: ${INVESTMENT_JWT_SECRET:default-secret-key-change-in-production-minimum-256-bits}
    jwt-expiration: ${INVESTMENT_JWT_EXPIRATION:3600000}
```

## Redis 키 구조

### Rate Limiting 키

- **인증 API**: `rate_limit:auth:{IP주소}`
- **일반 API**: `rate_limit:api:{사용자ID}`
- **TTL**: 60초

### 계정 잠금 키

- **잠금 상태**: `account_lock:{사용자명}`
- **실패 횟수**: `auth_failure:{사용자명}`
- **TTL**: 
  - 잠금: `ACCOUNT_LOCK_DURATION_MINUTES` 분
  - 실패 횟수: 1시간

## 보안 헤더 목록

| 헤더 | 값 | 설명 |
|------|-----|------|
| X-Content-Type-Options | nosniff | MIME 타입 스니핑 방지 |
| X-Frame-Options | DENY | 클릭재킹 방지 |
| X-XSS-Protection | 1; mode=block | XSS 공격 방지 (구형 브라우저) |
| Strict-Transport-Security | max-age=31536000; includeSubDomains | HTTPS 강제 |
| Content-Security-Policy | default-src 'self'; ... | XSS 및 데이터 주입 방지 |
| Referrer-Policy | strict-origin-when-cross-origin | 리퍼러 정보 제한 |
| Permissions-Policy | geolocation=(), microphone=(), camera=() | 브라우저 기능 제한 |

## 비밀번호 정책

### 요구사항

1. **길이**: 최소 8자, 최대 100자
2. **문자 종류**: 대문자, 소문자, 숫자, 특수문자 중 3종류 이상
3. **금지 패턴**:
   - 같은 문자 4번 이상 반복
   - 연속된 숫자 (예: 123, 456)
   - 연속된 영문 (예: abc, xyz)
   - 일반적인 약한 비밀번호 (예: password, admin, 123456)

### 검증 예시

✅ **허용되는 비밀번호**:
- `MyP@ssw0rd`
- `Secure123!`
- `P@ssw0rd2024`

❌ **거부되는 비밀번호**:
- `password` (약한 비밀번호)
- `12345678` (연속된 숫자)
- `AAAAbbbb` (반복 패턴)
- `abc123` (3종류 미만)

## 보안 이벤트 로그 형식

### 공통 필드

```json
{
  "timestamp": "ISO 8601 형식",
  "eventType": "이벤트 타입",
  "userId": "마스킹된 사용자 ID",
  "username": "마스킹된 사용자명",
  "details": {
    // 이벤트별 추가 정보
  }
}
```

### 이벤트별 details 필드

#### AUTHENTICATION_SUCCESS
```json
{
  "ipAddress": "클라이언트 IP 주소"
}
```

#### AUTHENTICATION_FAILURE
```json
{
  "reason": "실패 이유",
  "ipAddress": "클라이언트 IP 주소"
}
```

#### ACCOUNT_LOCKED
```json
{
  "reason": "잠금 이유"
}
```

#### RATE_LIMIT_EXCEEDED
```json
{
  "identifier": "식별자 (IP 또는 사용자 ID)",
  "endpoint": "요청 엔드포인트",
  "ipAddress": "클라이언트 IP 주소"
}
```

## 파일 위치 참조

### 보안 관련 클래스

| 클래스 | 경로 | 설명 |
|--------|------|------|
| `LogMaskingUtil` | `com.investment.common.security` | PII 마스킹 유틸리티 |
| `SecurityAuditService` | `com.investment.common.security` | 보안 이벤트 로깅 |
| `RateLimitFilter` | `com.investment.common.security` | Rate Limiting 필터 |
| `XssFilter` | `com.investment.common.security` | XSS 방지 필터 |
| `AccountLockService` | `com.investment.auth.service` | 계정 잠금 서비스 |
| `PasswordValidator` | `com.investment.common.validation` | 비밀번호 검증기 |
| `SecurityHeadersConfig` | `com.investment.config` | 보안 헤더 설정 |
| `HttpRequestUtil` | `com.investment.common.security` | HTTP 요청 유틸리티 |

### 설정 파일

| 파일 | 경로 | 설명 |
|------|------|------|
| `application.yml` | `src/main/resources` | 기본 설정 |
| `application-local.yml` | `src/main/resources` | 로컬 환경 설정 |
| `.env.example` | 프로젝트 루트 | 환경 변수 예제 |

## 보안 체크리스트

### 배포 전

- [ ] 모든 필수 환경 변수 설정
- [ ] 암호화 키 생성 및 설정
- [ ] JWT 시크릿 키 생성 및 설정
- [ ] CORS 설정 확인 (프로덕션)
- [ ] 쿠키 Secure 플래그 확인
- [ ] Rate Limiting 설정 확인
- [ ] 계정 잠금 설정 확인
- [ ] DEBUG_MODE=false 확인

### 정기 점검

- [ ] 보안 이벤트 로그 검토 (주간)
- [ ] 시크릿 로테이션 계획 (월간)
- [ ] 보안 취약점 스캔 (월간)
- [ ] 의존성 보안 업데이트 (월간)
- [ ] 보안 정책 검토 (분기별)

## 로깅 시 민감정보 마스킹 (개발 규칙)

로그에 **app key, secret, 계좌번호, userId** 등 암호화 저장 항목이 노출되지 않도록 **표준 모듈**을 사용합니다.

### 적용 규칙

1. **INFO/WARN/ERROR**: 반드시 **마스킹된 값만** 출력한다. 평문 출력 금지.
2. **DEBUG**: 필요 시 마스킹된 값 + DEBUG 레벨에서만 실제 값 추가 출력 가능 (로컬 디버깅용).
3. **공통 모듈**: `com.investment.common.security.LogMaskingUtil`만 사용한다. 각 클래스별 private 마스킹 메서드 사용 금지.

### LogMaskingUtil 사용 예

| 대상 | 메서드 | 예시 결과 |
|------|--------|-----------|
| App Key / API Key | `LogMaskingUtil.maskApiKey(value)` | 앞 4자리+**** |
| App Secret | `LogMaskingUtil.maskSecret(value)` | 앞 4자리+**** |
| 계좌번호 | `LogMaskingUtil.maskAccountNo(value)` | ****5678-12 |
| 사용자 ID (UUID 등) | `LogMaskingUtil.maskUserId(value)` | 앞 8자리+**** |
| 사용자명 | `LogMaskingUtil.maskUsername(value)` | 앞 2자리+** |

### DEBUG에서 실제 값 추가 로그

- **INFO 메시지 + DEBUG에서만 실제 값**: `LogMaskingUtil.logWithDebugActual(log, "메시지: key={}", new Object[]{masked}, "key(actual)={}", actual);`
- **DEBUG 메시지 + DEBUG에서만 실제 값**: `LogMaskingUtil.logWithDebugActualAtDebug(log, "메시지: key={}", new Object[]{masked}, "key(actual)={}", actual);`

### 적용 범위

- 로그에 `accountNo`, `userId`, `appKey`, `appSecret`, `token`(민감 토큰) 등이 포함되는 모든 로그 구문.
- API 요청 바디 로그 시 `CANO`(계좌번호) 등 민감 키는 마스킹 후 직렬화.
- 상세: [개발 진행 현황](../09-planning/02-development-status.md) 완료 항목 "로그 마스킹 모듈화" 참조.

## 문서 변경 이력

| 버전 | 일자 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-28 | System | 문서 정리 및 구조화 |
| 1.1 | 2026-01-29 | System | 로깅 시 민감정보 마스킹(LogMaskingUtil) 개발 규칙 섹션 추가 |
