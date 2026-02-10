# 역할·권한 매트릭스

## 1. 역할 정의

| 역할 ID | 표시명 | 설명 |
|---------|--------|------|
| User | 일반 사용자 | 투자·설정·조회 중심. 대시보드·전략·포트폴리오·주문·백테스트·설정 접근. |
| Admin | 관리자 | User 권한 + 스케줄 트리거·데이터 파이프라인·알림·리스크·모델 상태·감사 로그·시스템 헬스 접근. |

## 2. 메뉴별 접근 권한

| 메뉴 ID | 경로 | User | Admin |
|---------|------|------|-----|
| dashboard | `/` | 읽기/이동 | 읽기/이동 |
| auto-invest | `/auto-invest` | 읽기/이동 | 읽기/이동 |
| strategy-kr | `/strategies/kr` | 읽기/쓰기(상태 변경) | 읽기/쓰기 |
| strategy-us | `/strategies/us` | 읽기/쓰기 | 읽기/쓰기 |
| news-events | `/news` | 읽기/필터 | 읽기/필터 |
| portfolio | `/portfolio` | 읽기 | 읽기 |
| orders | `/orders` | 읽기/취소(미체결) | 읽기/취소 |
| schedule-status | `/batch` | 읽기/선택적 "지금 실행" | 읽기/지금 실행 |
| backtest | `/backtest` | 읽기/실행 | 읽기/실행 |
| settings | `/settings` | 읽기/쓰기(본인 계좌) | 읽기/쓰기 |
| data-pipeline | `/ops/data` | — | 읽기 |
| alerts | `/ops/alerts` | — | 읽기/ACK·해제 |
| risk-report | `/risk` | — | 읽기 |
| model-status | `/ops/model` | — | 읽기 |
| audit-log | `/ops/audit` | — | 읽기 |
| system-health | `/ops/health` | — | 읽기 |

## 3. 행동 단위 권한

| 행동 | User | Admin | 비고 |
|------|------|-----|------|
| 계좌·API 저장 | 본인 계좌만 | 본인 계좌만 | API 키 변경 시 비밀번호 확인 |
| 자동투자 ON/OFF | 가능 | 가능 | 설정 화면 토글 |
| 로보 어드바이저 ON/OFF | 가능 | 가능 | 설정 화면 토글 |
| 주문 취소(미체결) | 가능 | 가능 | 본인 주문만 |
| 스케줄 "지금 실행" | 정책에 따라 허용/제한 | 허용 | Job별 트리거 API 호출 |
| allow-real-execution 상태 보기 | — | 읽기 전용 표시 | UI에서 변경 불가, 서버 설정만 |
| 알림 ACK/해제 | — | 허용 | 알림센터 |
| 리스크 게이트·일일 손실 한도 설정 | — | 읽기 또는 관리(정책에 따름) | application.yml 반영 시 운영자만 |

## 4. UI 반영 규칙

- **메뉴 노출**: Admin 전용 메뉴(§2 테이블에서 User가 "—"인 항목)는 역할이 Admin일 때만 메뉴에 표시.
- **버튼 비활성/숨김**: User가 "—"인 행동은 해당 화면에서 버튼 비활성 또는 숨김.
- **실계좌 자동 실행 가드**: `allow-real-execution` 값은 Admin 화면(예: 설정·스케줄)에서 **읽기 전용**으로 표시. 변경은 서버/환경변수만.
