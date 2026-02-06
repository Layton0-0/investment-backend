# Figma 와이어프레임 문서 패키지 — 인덱스

## 목적

로보어드바이저형 자동투자 시스템의 **전체 화면 기획**과 **Figma AI로 제작 가능한 와이어프레임**을 위한 단일 문서 세트입니다. 화면 목록·정보 구조·역할/권한·유저 플로우·컴포넌트·상태/가드레일·화면별 스펙·Figma 구조·AI 프롬프트를 포함합니다.

## 읽는 순서

1. **본 문서(00-index)** — 목적·읽는 순서·용어 링크
2. **[01-information-architecture.md](01-information-architecture.md)** — 사이트맵·내비 규칙·전역 탭
3. **[02-roles-and-permissions.md](02-roles-and-permissions.md)** — 메뉴/행동 단위 권한 매트릭스
4. **[03-user-flows.md](03-user-flows.md)** — 핵심 유저 플로우
5. **[04-component-library.md](04-component-library.md)** — 공통 컴포넌트·상태
6. **[05-states-and-guardrails.md](05-states-and-guardrails.md)** — 안전장치 UI 표현
7. **[06-screen-specs/](06-screen-specs/)** — 화면별 상세 스펙
8. **[07-figma-structure.md](07-figma-structure.md)** — Figma 파일/프레임 네이밍·AutoLayout
9. **[08-figma-ai-prompts.md](08-figma-ai-prompts.md)** — Figma AI 입력 프롬프트 모음
10. **[10-design-ai-full-prompt.md](10-design-ai-full-prompt.md)** — 디자인 AI에 붙여넣기용 통합 프롬프트(모든 화면·UI/UX 워크플로우·연말 세금·킬스위치 포함)

## 용어·단일 소스(SSoT) 링크

| 용어 | 정의·상세 |
|------|------------|
| 4단계 파이프라인 | 유니버스 → 시그널 → 자금관리 → 매매실행. [12-auto-investment-strategy.md](../../02-architecture/12-auto-investment-strategy.md) §5 |
| serverType | URL 쿼리: `1`=모의계좌, `0`=실계좌. [01-screen-menu-spec.md](../01-screen-menu-spec.md) §2 |
| 자동투자 ON | 통합 복합 로직(공통 전처리 → 로보 → 파이프라인) 1회 실행. [12-auto-investment-strategy.md](../../02-architecture/12-auto-investment-strategy.md) §6.1.1 |
| auto-execute / allow-real-execution | 서버 설정. [12-auto-investment-strategy.md](../../02-architecture/12-auto-investment-strategy.md) §6.2 체크리스트 |
| 전략·팩터·파라미터 | [00-strategy-registry.md](../../02-architecture/00-strategy-registry.md) |
| API 연동 | [01-api-overview.md](../../04-api/01-api-overview.md) |

## 문서 변경 이력

| 버전 | 일자 | 변경 내용 |
|------|------|----------|
| 1.0 | 2026-02-04 | 초기 문서 패키지 작성 |
| 1.1 | 2026-02-06 | 10-design-ai-full-prompt 읽는 순서 추가 |
