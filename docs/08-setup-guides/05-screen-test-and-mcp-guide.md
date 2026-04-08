# 화면 테스트 및 MCP 구성 가이드

Cursor가 시니어 QA처럼 화면을 검증할 수 있도록 MCP 구성과 화면·인프라 테스트 방법을 정리한다.

---

## 1. MCP 권장 구성 (ON / OFF)

| MCP | 권장 | 용도 |
|-----|------|------|
| **cursor-ide-browser** | **ON 필수** | 화면 테스트 핵심. Cursor 기본 제공. Agent가 URL 이동·스냅샷·클릭·입력·판단 가능. 비활성화하지 말 것. |
| **한국투자증권 (KIS Code Assistant)** | **ON** | API 개발 시 필수 ([.cursor/rules/korea-investment-api.md](../../.cursor/rules/korea-investment-api.md)). |
| **filesystem** | **ON** | 코드·설정·테스트 결과 파일 접근. |
| **github** | 선택 | 이슈/PR·코드 검색 시 유용. |
| **ssh-mcp-oracle-osaka-yoon / ssh-mcp-oracle-korea-jihee** | 선택 | 로컬 Cursor에서 OCI 서버(Oracle Osaka / Oracle Korea) 원격 명령(배포·로그 등) 시 사용. [07-cursor-oci-ssh-mcp.md](07-cursor-oci-ssh-mcp.md) 참조. |
| notion | OFF 권장 | Notion 기획 문서 미사용 시 OFF. |
| local-maria | OFF 권장 | DB는 TimescaleDB(PostgreSQL) 사용. MariaDB MCP 미사용 시 OFF. |
| figma | OFF 권장 | Figma 디자인 조회 미사용 시 OFF. |

- MCP 설정 파일: `C:\Users\<사용자명>\.cursor\mcp.json`
- 프로젝트 템플릿: [.cursor/mcp.json.template](../../.cursor/mcp.json.template) — filesystem 경로는 워크스페이스 루트(`D:/works/pjt/auto-investment-project`)로 맞춰 사용.

---

## 2. cursor-ide-browser로 화면 직접 검증 (실시간 QA)

1. **프론트 기동**: `cd investment-frontend && npm run dev` → http://localhost:5173
2. **백엔드 기동**(API 연동 필요 시): `.\scripts\bootRun-agent.ps1` → 8084
3. **Agent 작업**: `browser_navigate` → `browser_snapshot`(또는 `browser_take_screenshot`) → 필요 시 `browser_click`, `browser_fill` 등으로 시나리오 진행 후 결과 판단

로그인 후 대시보드 노출, 에러 메시지, 버튼/폼 존재 여부 등 요구사항 단위 시나리오를 Agent가 대신 실행·판단할 수 있다.

---

## 3. Playwright E2E (재현·CI)

- **위치**: `investment-frontend/e2e/`, `investment-frontend/playwright.config.ts`
- **실행**: `cd investment-frontend && npm run e2e` (또는 `npx playwright test`)
- **전제**: 프론트가 http://localhost:5173 에서 동작. config의 webServer가 자동 기동하거나, 미리 `npm run dev` 후 실행 가능.
- **스펙 예**: 랜딩 노출·로그인 링크 이동, 로그인 폼 로드·빈 제출 시 검증 메시지.

CI(Jenkins/GitHub Actions 등)에서 `npm run e2e` 실행으로 회귀 방지.

---

## 4. API·인프라 정합성 (curl / k6)

- **스모크**: [scripts/smoke-api.ps1](../../scripts/smoke-api.ps1) — `.\scripts\smoke-api.ps1` 또는 `.\scripts\smoke-api.ps1 -Port 8084` (Agent 서버). GET /actuator/health 호출 후 exit code로 판단.
- **부하**(선택): k6 스크립트로 동시 접속·지연 측정. Agent가 “API 정합성 확인” 요청 시 해당 스크립트 실행·결과 해석.

---

## 5. 참고

- 플랜: 시니어 QA 화면테스트 MCP 설계
- 백엔드 테스트: [03-test-execution.md](03-test-execution.md)
- 로컬 설정: [01-local-setup-complete.md](01-local-setup-complete.md)
