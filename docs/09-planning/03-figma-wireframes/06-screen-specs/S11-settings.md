# S11 — 설정

## 목적

API 키·계좌·거래 설정(비율·한도·자동투자 ON/OFF·로보 ON/OFF). 리스크 파라미터는 표시/설명 중심(읽기 전용 권장).

## 정보 구조

- **계좌·API 연결**: 모의계좌/실계좌 세그먼트. API Key, Secret, 계좌번호 입력. [계좌 인증](선택). 한 번에 저장.
- **자동투자 설정**: 등록 계좌 0개 → 빈 상태 + [계좌 설정으로 가기]. 1개 → 해당 타입 단일 폼. 2개 → "모의계좌 | 실계좌" 세그먼트 + 선택 타입 폼. 항목: 최대 투자금액, 단기/중기/장기 비율, 자동 매매 토글, 로보 어드바이저 토글, [저장].

## 주요 상호작용

- 계좌·API 저장 → PUT /api/v1/settings/accounts.
- 거래 설정 저장 → PUT /api/v1/settings/{accountNo}. 자동투자·로보 토글 연동(로보 ON 시 자동투자도 ON).
- API 키 변경 시 비밀번호 확인(정책에 따름).

## 상태/에러

- 저장 성공: "저장되었습니다." 인라인. 실패: 에러 메시지. 비율 합=1 검증.

## 권한

- User, Ops: 본인 계좌 읽기/쓰기. auto-execute·allow-real-execution은 읽기 전용 표시(Ops 또는 설정 하단 안내).

## 연동 API

- GET/PUT /api/v1/settings/accounts, GET/PUT /api/v1/settings/{accountNo}, GET /api/v1/user/accounts/main?serverType=.
