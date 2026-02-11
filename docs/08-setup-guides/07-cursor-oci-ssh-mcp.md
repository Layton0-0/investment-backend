# Cursor OCI SSH MCP (로컬에서 원격 명령)

로컬 Cursor 창을 연 상태에서 **OCI 서버(Oracle Osaka / Oracle Korea 등)에 SSH로 명령만 실행**하고 싶을 때는 **SSH MCP 서버**를 사용한다.  
(원격에서 파일 편집·터미널까지 쓰려면 [05-multi-vps-oracle-aws-cicd.md §9](../06-deployment/05-multi-vps-oracle-aws-cicd.md#9-cursor-remote-ssh로-oci-접속)의 Remote-SSH를 사용한다.)

---

## 1. 용도

- 로컬 프로젝트 폴더를 연 채로 Agent가 OCI에서 **배포 스크립트 실행**, **로그 조회**, **docker compose 명령** 등을 수행할 때.
- 도구: `exec`(일반 명령), `sudo-exec`(sudo 필요 시, 설정 시에만 사용).

---

## 2. MCP 서버

- **추천**: [tufantunc/ssh-mcp](https://github.com/tufantunc/ssh-mcp) (TypeScript, `exec` / `sudo-exec`, 타임아웃 지원).
- 실행: `npx -y ssh-mcp -- --host=... --user=... --key=...` 형태. 인자는 아래 예시 참고.

---

## 3. 설정 (Cursor)

프로젝트 템플릿: [.cursor/mcp.json.template](../../.cursor/mcp.json.template)(`ssh-mcp-oracle-osaka-yoon`, `ssh-mcp-oracle-korea-jihee` 항목 포함).  
실제 사용 시 **복사해 `C:\Users\<사용자명>\.cursor\mcp.json`으로 두고**, 아래 플레이스홀더를 본인 환경에 맞게 바꾼다.

- **시크릿**: SSH 비밀키 경로, 호스트 IP, sudo 비밀번호는 **저장소에 커밋하지 않는다.** `mcp.json`은 사용자 홈 `.cursor`에만 두고 Git에 올리지 않는다.
- **호스트별**로 Oracle Osaka / Oracle Korea를 구분해 두 개 등록한다(`ssh-mcp-oracle-osaka-yoon`, `ssh-mcp-oracle-korea-jihee`). 각각 `--host`, `--key`를 다르게 둘 수 있다.

### 3.1 mcp.json 예시 (플레이스홀더)

Oracle Osaka (yoon) / Oracle Korea (jihee) 두 호스트:

```json
"ssh-mcp-oracle-osaka-yoon": {
  "type": "stdio",
  "command": "npx",
  "args": [
    "-y",
    "ssh-mcp",
    "--",
    "--host=YOUR_ORACLE_OSAKA_PUBLIC_IP",
    "--port=22",
    "--user=ubuntu",
    "--key=YOUR_SSH_KEY_PATH_OSAKA",
    "--timeout=60000",
    "--maxChars=none"
  ]
},
"ssh-mcp-oracle-korea-jihee": {
  "type": "stdio",
  "command": "npx",
  "args": [
    "-y",
    "ssh-mcp",
    "--",
    "--host=YOUR_ORACLE_KOREA_PUBLIC_IP",
    "--port=22",
    "--user=ubuntu",
    "--key=YOUR_SSH_KEY_PATH_KOREA",
    "--timeout=60000",
    "--maxChars=none"
  ]
}
```

- `YOUR_ORACLE_OSAKA_PUBLIC_IP` / `YOUR_ORACLE_KOREA_PUBLIC_IP`: 각 OCI VM Public IP.
- `ubuntu`: Ubuntu 이미지 기본 사용자. Oracle Linux면 `opc`.
- `YOUR_SSH_KEY_PATH_OSAKA` / `YOUR_SSH_KEY_PATH_KOREA`: Windows에서 각 호스트용 비밀키 경로(절대 경로). **경로에 공백이 있으면 반드시 따옴표로 감싼다**(예: `"--key=\"D:/OneDrive - HKNC/path/to/key.key\""`). 그렇지 않으면 `'ssh-mcp'는(은) 내부 또는 외부 명령...` 오류로 MCP가 시작되지 않는다.
- `--timeout`: 밀리초. 기본 60000(1분).
- `--maxChars=none`: 명령 길이 제한 해제(필요 시).
- sudo가 필요하면 `"--sudoPassword=..."` 추가(보안상 저장소에 넣지 말 것).

---

## 4. 사용 가능한 도구

| 도구 | 설명 |
|------|------|
| `exec` | 원격 서버에서 셸 명령 실행. 파라미터: `command`(필수), `description`(선택). |
| `sudo-exec` | sudo로 명령 실행. 서버 시작 시 `--sudoPassword` 설정 시에만 사용. `--disableSudo`로 비활성화 가능. |

---

## 5. 문제 해결

### 5.1 'ssh-mcp'는(은) 내부 또는 외부 명령... 오류

MCP가 시작되지 않고 위 오류가 나오면:

1. **Node.js/npm 설치 확인**: PowerShell에서 `node --version`, `npm --version`, `npx --version` 실행. 모두 버전이 나와야 함.
2. **ssh-mcp 패키지 직접 테스트**: `npx -y ssh-mcp --version` 실행. 패키지가 다운로드되고 실행되는지 확인.
3. **npm 캐시 정리**: `npm cache clean --force` 후 재시도.
4. **전역 설치 후 직접 실행** (권장): `npm install -g ssh-mcp` 실행 후 `mcp.json`에서 `"command": "npx"` → `"command": "ssh-mcp"`, `"args"`에서 `"-y", "ssh-mcp", "--"` 제거. 예:

```json
"ssh-mcp-oracle-korea-jihee": {
  "type": "stdio",
  "command": "ssh-mcp",
  "args": [
    "--",
    "--host=146.56.98.230",
    "--port=22",
    "--user=ubuntu",
    "--key=\"D:/OneDrive - HKNC/study/cloud/key/neekly/layton/e2/ssh-key-2025-07-22.key\"",
    "--timeout=60000",
    "--maxChars=none"
  ]
}
```

### 5.2 경로에 공백이 있을 때

`--key=` 뒤 경로에 공백이 있으면 반드시 따옴표로 감싼다: `"--key=\"D:/OneDrive - HKNC/path/to/key.key\""`

---

## 6. 보안

- SSH 비밀키·패스워드·Public IP는 **문서·저장소에 실제 값으로 넣지 않는다.** 위 예시는 전부 플레이스홀더.
- Agent가 원격에서 실행하는 명령은 **배포·로그 조회·점검** 수준으로 제한. DB DML 등 위험 작업은 기존 규칙(수동 DML 금지 등)을 따른다.
