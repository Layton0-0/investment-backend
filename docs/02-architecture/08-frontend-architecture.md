# 프론트엔드 아키텍처 (가벼운 버전)

## 개요

아키텍처 문서(`03-ai-redesign.md`)에서 설계한 클라이언트 레이어를 참고하되,
실제 구현은 가볍고 간단하게 유지합니다.

## 클라이언트 레이어 구조

### 아키텍처 문서 설계
```
┌─────────────────────────────────────────────────────────┐
│                    클라이언트 레이어                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  Next.js     │  │  React App   │  │  Mobile App  │  │
│  │  (SSR/SSG)   │  │  (Dashboard) │  │   (향후)     │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### 실제 구현 (가벼운 버전)

현재는 **React (Vite) SPA** 단일 클라이언트로 구현하며,
REST API와 연동하는 구조입니다.

```
┌─────────────────────────────────────────────────────────┐
│                    클라이언트 레이어                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  React SPA   │  │  REST API    │  │  향후 확장   │  │
│  │  (Vite)      │  │  클라이언트  │  │  - Next.js   │  │
│  │  - 단일 앱   │  │  - fetch API │  │  - Mobile    │  │
│  │  - JWT 인증  │  │  - Bearer 토큰│  │              │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## 구현 전략

### 현재: React SPA
- **React (Vite)**: 단일 페이지 애플리케이션
- **TypeScript**: 타입 안정성
- **REST API**: Bearer JWT 인증, fetch/axios
- **공통 레이아웃**: AppLayout(헤더·메뉴), RequireAuth

### 향후 확장 (필요 시)
- **Next.js**: SSR/SSG 지원
- **Mobile**: 네이티브 앱 또는 PWA

#### React SPA (분리 배포/개발) — 구현 시작
- **프론트 디렉토리**: `investment-front/`
- **개발 서버**: Vite (기본 5173)
- **백엔드 연동**: `VITE_API_BASE_URL` 환경변수로 API base URL 지정 (예: `http://localhost:8083`)
- **인증**: `/api/v1/auth/login` 응답의 `token`을 브라우저 저장소에 보관 후, 모든 API 요청에 `Authorization: Bearer <token>` 헤더로 전송 (백엔드 `JwtAuthenticationFilter`는 헤더 토큰 지원)
- **CORS**: 백엔드는 `CORS_ALLOWED_ORIGINS`(기본 `*`)로 제어하며, Bearer 토큰 방식은 credentials 없이 동작 가능

## 현재 구현 특징

### 1. 가벼운 디자인
- **파일 크기**: 약 8KB (HTML + CSS)
- **로딩 시간**: 0.5-1초
- **외부 의존성**: 없음
- **디자인 토큰**: `common.css`에 `:root` CSS 변수(색상·간격·radius·타이포) 정의. 통일된 container 최대 너비 **1200px** (`--container-max`).
- **레이아웃 필수**: 모든 메뉴 화면은 **layout-header** + **layout-menu** fragment 사용. 본문은 `<main class="container">` 또는 `.settings-container`(900px) 사용.
- **계좌 타입 탭**: layout-menu 내에 **모의계좌 | 실계좌** 탭을 두며, 로그인 사용자만 노출. URL 쿼리 `serverType=1`(모의) / `serverType=0`(실계좌)로 상태 유지. 메뉴·내부 링크에는 `serverType` 쿼리를 포함해 이동 시 계좌 타입이 유지되도록 함.

### 2. 핵심 기능만
- 계좌 조회
- 보유 종목 표시
- 최근 주문 표시
- 거래 설정 확인

### 3. REST API 호환
- React SPA가 REST API 단일 클라이언트
- JWT 인증, CORS 설정으로 백엔드 연동

## 확장 가능 메뉴·레이아웃 원칙

- **메뉴 구조**: 메뉴는 이후 계속 확장될 수 있으므로 **설정/코드 기반** 구조(메뉴 ID, 순서, 권한, 라우트 매핑)를 권장합니다.
- **공통 레이아웃**: 모든 화면은 **헤더(로고·사용자·로그아웃)**·**사이드/상단 메뉴**·**본문 영역**을 공통으로 두고, 본문만 메뉴별로 교체합니다.
- **화면 기획 상세**: 메뉴 트리(1차안), 메뉴별 화면 역할·요소, 확장 규칙은 **[09-planning/01-screen-menu-spec.md](../09-planning/01-screen-menu-spec.md)** 를 참조합니다.

## 페이지 구조

### 1. 대시보드 (`/`)
- 계좌 잔고
- 보유 종목
- 최근 주문
- 거래 설정

### 2. 자동투자 현황 (`/auto-invest`) — 확장
- 4단계 파이프라인 현황
- 시그널·체결 요약

### 3. 국내 전략 (`/strategies/kr`) / 미국 전략 (`/strategies/us`)
- 전략 목록(단/중/장기)
- 전략 상태 관리
- 전략 생성/수정
- 시장(KR/US)별 분리

### 4. 뉴스·이벤트 (`/news`) — 확장
- 공시·뉴스·센티멘트 요약

### 5. 포트폴리오 (`/portfolio`)
- 일별 포트폴리오
- 추천 종목
- 가격 목표

### 6. 주문·체결 (`/orders`)
- 주문 목록·체결 내역

### 7. 배치 관리 (`/batch`)
- 배치 작업 목록
- 배치 실행 상태

## API 엔드포인트

모든 페이지는 REST API도 지원합니다:

```
GET  /api/v1/accounts/{accountNo}/balance
GET  /api/v1/accounts/{accountNo}/positions
GET  /api/v1/orders?accountNo={accountNo}
GET  /api/v1/settings/{accountNo}
GET  /api/v1/strategies/{accountNo}
GET  /api/v1/trading-portfolios/{date}
```

## 향후 확장 계획

### Next.js 마이그레이션 (선택적)
```
1. Next.js 프로젝트 생성
2. API 라우트 설정
3. 페이지 컴포넌트 마이그레이션
4. SSR/SSG 최적화
```

### React 컴포넌트 (선택적)
```
1. 컴포넌트 분리
2. 상태 관리 (Zustand/Redux)
3. 실시간 업데이트 (WebSocket)
4. 차트 라이브러리 통합
```

## 성능 목표

### 현재 (React SPA)
- 로딩 시간: < 1초
- 파일 크기: < 10KB
- 렌더링 시간: < 50ms

### 향후 (Next.js/React)
- 로딩 시간: < 2초 (초기), < 0.5초 (네비게이션)
- 파일 크기: < 200KB (초기), < 50KB (청크)
- 렌더링 시간: < 100ms

## 참고 문서

- [화면·메뉴 기획서](../09-planning/01-screen-menu-spec.md) — 확장 가능 메뉴 트리·메뉴별 화면·확장 규칙
- [아키텍처 재설계](./03-ai-redesign.md)
- [프론트엔드 간소화](./07-frontend-simplification.md)

## 문서 변경 이력

| 버전 | 일자 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-28 | System | 문서 정리 및 구조화 - 과거 설계 문서로 분류 |
