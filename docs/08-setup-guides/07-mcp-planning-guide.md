# MCP 기획 문서 관리 가이드

## 개요

Workspace MCP, GitHub MCP, Notion MCP를 활용하여 프로젝트 기획 문서를 효율적으로 관리하고 Cursor가 프로젝트 전체 맥락을 이해할 수 있도록 하는 가이드입니다.

## 설치된 MCP 서버

### 1. Filesystem / Workspace MCP ✅

**상태**: 설치 완료

**설정 위치**: `C:\Users\HNW\.cursor\mcp.json`

**설정 내용**:
```json
{
  "filesystem": {
    "type": "stdio",
    "command": "npx",
    "args": [
      "-y",
      "@modelcontextprotocol/server-filesystem",
      "D:/works/pjt/investment-choi"
    ]
  }
}
```

**기능**:
- 프로젝트 전체 파일 시스템 접근
- 문서 읽기/쓰기
- 디렉토리 탐색
- 파일 검색

**활용 방법**:
- Cursor가 프로젝트 전체 구조를 이해
- 기존 기획 문서, 코드, TODO, 이슈를 맥락으로 활용
- 문서 자동 업데이트 및 구조화

### 2. GitHub MCP ⚙️

**상태**: 설치 완료 (토큰 설정 필요)

**설정 위치**: `C:\Users\HNW\.cursor\mcp.json`

**설정 내용**:
```json
{
  "github": {
    "type": "stdio",
    "command": "npx",
    "args": [
      "-y",
      "@modelcontextprotocol/server-github"
    ],
    "env": {
      "GITHUB_PERSONAL_ACCESS_TOKEN": "${GITHUB_TOKEN}"
    }
  }
}
```

**기능**:
- GitHub 리포지토리 관리
- 코드 검색 및 분석
- 이슈 및 PR 관리
- 오픈소스 구조 분석

**토큰 설정 방법**:

1. **GitHub Personal Access Token 생성**:
   - GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
   - "Generate new token (classic)" 클릭
   - 필요한 권한 선택:
     - `repo` (전체 리포지토리 접근)
     - `read:org` (조직 읽기, 필요시)
   - 토큰 생성 후 복사

2. **환경 변수 설정** (Windows PowerShell):
   ```powershell
   # 사용자 환경 변수로 설정 (영구적)
   [System.Environment]::SetEnvironmentVariable("GITHUB_TOKEN", "your_token_here", "User")
   
   # 또는 현재 세션에만 설정 (임시)
   $env:GITHUB_TOKEN = "your_token_here"
   ```
   
   **참고**: MCP 서버는 `GITHUB_PERSONAL_ACCESS_TOKEN` 환경 변수를 사용하지만, 설정 파일에서는 `${GITHUB_TOKEN}`을 참조합니다. 환경 변수 이름은 `GITHUB_TOKEN`으로 설정하면 됩니다.

3. **Cursor 재시작**:
   - 환경 변수를 설정한 후 Cursor를 재시작해야 합니다.

**활용 방법**:
- 오픈소스 구조 분석
- 경쟁 서비스 코드 분석
- "이 서비스는 왜 이렇게 설계됐는지" 설명 요청
- 기존 GitHub 이슈 및 PR 맥락 활용

### 3. Notion MCP ⚙️

**상태**: 설치 완료 (토큰 설정 필요)

**설정 위치**: `C:\Users\HNW\.cursor\mcp.json`

**설정 내용**:
```json
{
  "notion": {
    "type": "stdio",
    "command": "npx",
    "args": [
      "-y",
      "@notionhq/notion-mcp-server"
    ],
    "env": {
      "NOTION_TOKEN": "${NOTION_TOKEN}"
    }
  }
}
```

**참고**: Notion은 원격 MCP 서버(`https://mcp.notion.com/mcp`)도 제공하며, OAuth 기반 설정으로 더 쉽게 사용할 수 있습니다. 하지만 로컬 패키지(`@notionhq/notion-mcp-server`)도 계속 사용 가능합니다.

**기능**:
- Notion 페이지 읽기/쓰기
- 데이터베이스 쿼리
- 기획 문서 자동 업데이트
- 회의록 및 결정 사항 추적

**토큰 설정 방법**:

1. **Notion Integration 생성**:
   - https://www.notion.so/profile/integrations 접속
   - "New integration" 클릭
   - Integration 이름 입력 (예: "Cursor MCP")
   - Capabilities 설정:
     - Read content ✅
     - Update content ✅ (필요시)
     - Insert content ✅ (필요시)
   - "Submit" 클릭
   - 생성된 Integration의 "Internal Integration Token" 복사

2. **페이지/데이터베이스 연결**:
   - 연결할 Notion 페이지 또는 데이터베이스 열기
   - 우측 상단 "..." 메뉴 → "Connections" → 생성한 Integration 선택
   - 또는 Integration 설정 페이지의 "Access" 탭에서 연결

3. **환경 변수 설정** (Windows PowerShell):
   ```powershell
   # 사용자 환경 변수로 설정 (영구적)
   [System.Environment]::SetEnvironmentVariable("NOTION_TOKEN", "your_token_here", "User")
   
   # 또는 현재 세션에만 설정 (임시)
   $env:NOTION_TOKEN = "your_token_here"
   ```

4. **Cursor 재시작**:
   - 환경 변수를 설정한 후 Cursor를 재시작해야 합니다.

**활용 방법**:
- 기획 문서 자동 업데이트
- 회의록 → 기획 반영
- 결정 이력 추적
- "이 기능을 기존 로드맵에 반영하고 충돌 나는 부분 찾아줘" 같은 명령

## Cursor 활용 시나리오

### 시나리오 1: PRD 구조화

**명령**:
```
이 프로젝트의 목표를 기준으로 PRD를 다시 구조화해줘
```

**동작**:
1. Workspace MCP가 `docs/PRD.md` 및 관련 문서 읽기
2. 프로젝트 전체 구조 분석
3. PRD.md 자동 구조화 및 업데이트

### 시나리오 2: 로드맵 충돌 검사

**명령**:
```
이 기능을 기존 로드맵에 반영하고 충돌 나는 부분 찾아줘
```

**동작**:
1. Workspace MCP가 `docs/roadmap.md` 읽기
2. 새로운 기능 요구사항 분석
3. 기존 로드맵과 충돌 검사
4. 업데이트된 로드맵 제안

### 시나리오 3: GitHub 오픈소스 분석

**명령**:
```
이 오픈소스 프로젝트는 왜 이렇게 설계됐는지 분석해줘
```

**동작**:
1. GitHub MCP가 리포지토리 구조 분석
2. 코드 패턴 및 아키텍처 분석
3. 설계 결정 사항 추출
4. 분석 결과를 `docs/decisions.md`에 추가

### 시나리오 4: Notion 기획 문서 동기화

**명령**:
```
Notion의 기획 문서를 프로젝트 PRD.md에 반영해줘
```

**동작**:
1. Notion MCP가 Notion 페이지 읽기
2. Workspace MCP가 `docs/PRD.md` 읽기
3. 내용 비교 및 동기화
4. PRD.md 업데이트

## 기획 문서 구조

프로젝트의 기획 문서는 다음 구조로 관리됩니다:

```
docs/
├── PRD.md              # 제품 요구사항 문서 (통합)
├── roadmap.md          # 프로젝트 로드맵
├── decisions.md        # 아키텍처 결정 사항
├── 01-requirements/    # 상세 요구사항 문서
│   ├── 01-overview.md
│   ├── 02-functional-requirements.md
│   └── 03-non-functional-requirements.md
└── ...
```

**문서 역할**:
- **PRD.md**: 고수준 제품 요구사항 요약 (Cursor가 프로젝트 목표 이해)
- **roadmap.md**: 단계별 개발 계획 및 마일스톤
- **decisions.md**: 기술 및 설계 결정 사항 기록 (ADR)
- **01-requirements/**: 상세 요구사항 문서 (참조용)

## MCP 설정 확인

### 설정 파일 위치
- Windows: `C:\Users\<사용자명>\.cursor\mcp.json`
- 또는: `%APPDATA%\Cursor\User\globalStorage\mcp.json`

### 설정 확인 방법

1. **Cursor 설정 열기**:
   - `Ctrl + ,` (설정)
   - 또는 `File` → `Preferences` → `Settings`

2. **MCP 서버 확인**:
   - `Features` → `MCP` 섹션에서 설치된 서버 확인

3. **환경 변수 확인** (PowerShell):
   ```powershell
   # GitHub 토큰 확인
   $env:GITHUB_TOKEN
   
   # Notion 토큰 확인
   $env:NOTION_TOKEN
   ```

## 문제 해결

### MCP 서버가 작동하지 않는 경우

1. **Cursor 재시작**: 환경 변수 변경 후 반드시 재시작
2. **환경 변수 확인**: PowerShell에서 `$env:GITHUB_TOKEN` 등으로 확인
3. **설정 파일 확인**: `mcp.json` 파일의 JSON 형식 확인
4. **로그 확인**: Cursor의 개발자 도구에서 MCP 관련 오류 확인

### GitHub MCP 오류

- **토큰 권한 확인**: `repo` 권한이 있는지 확인
- **토큰 만료 확인**: GitHub에서 토큰 상태 확인
- **환경 변수 재설정**: 토큰을 다시 설정하고 Cursor 재시작

### Notion MCP 오류

- **Integration 연결 확인**: Notion 페이지가 Integration에 연결되어 있는지 확인
- **권한 확인**: Integration의 Capabilities 설정 확인
- **토큰 형식 확인**: `secret_`으로 시작하는지 확인

## 참고 자료

- [한국투자증권 MCP 통합 가이드](./06-mcp-integration-guide.md)
- [PRD.md](../PRD.md)
- [roadmap.md](../roadmap.md)
- [decisions.md](../decisions.md)
- [GitHub Personal Access Tokens](https://github.com/settings/tokens)
- [Notion Integrations](https://www.notion.so/profile/integrations)
- [MCP 공식 문서](https://modelcontextprotocol.io/)

## 문서 변경 이력

| 버전 | 일자 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-29 | System | 초기 문서 작성 - Workspace, GitHub, Notion MCP 설정 가이드 |
