# S01 — 인증(로그인·회원가입·계좌 인증)

## 목적

- 로그인: 기존 사용자 인증 후 대시보드로 이동.
- 회원가입: 서버 타입·API 키·계좌번호·계좌 인증 후 가입 완료.
- 계좌 인증: verify-account로 모의/실전 도메인에서 API 키·계좌 유효성 확인.

## 정보 구조

- **로그인**: 이메일/ID·비밀번호, [로그인], 회원가입 링크.
- **회원가입**: 서버 타입(모의/실계좌), API Key, Secret, 계좌번호, [계좌 인증], 비밀번호, 사용자명 등, [가입].

## 주요 상호작용

- 로그인 제출 → POST /api/v1/auth/login → 성공 시 토큰 저장·`/` 리다이렉트(serverType=1).
- 회원가입 시 [계좌 인증] → POST /api/v1/auth/verify-account → 성공 시 "인증 완료" 표시.
- 가입 완료 → POST /api/v1/auth/signup → 로그인 페이지 또는 자동 로그인.

## 상태/에러

- 401: "아이디 또는 비밀번호가 올바르지 않습니다."
- 계좌 인증 실패: "API 키 또는 계좌번호를 확인해주세요."
- Rate limit: "요청이 많습니다. 잠시 후 다시 시도해주세요."

## 권한

- 비인증 접근. 로그인 후 User/Admin 역할에 따라 메뉴 노출.

## 연동 API

- POST /api/v1/auth/login, POST /api/v1/auth/signup, POST /api/v1/auth/verify-account.
