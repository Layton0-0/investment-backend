# 도메인 E2E 완료 여부 및 다음 우선순위

**목적**: 도메인(api/app.neekly-report.cloud) 접근 시 **처음부터 끝까지** 사용 가능한지 점검·갱신하고, 다음 할일(필수/선택)을 정리한다.

**참조**: [13-manual-operator-tasks.md](13-manual-operator-tasks.md)(수동 작업), [05-multi-vps-oracle-aws-cicd.md](05-multi-vps-oracle-aws-cicd.md), [11-dns-and-domain-setup.md](11-dns-and-domain-setup.md).

---

## 1. E2E 완료 체크리스트

| # | 항목 | 필수 | 확인일 | 비고 |
|---|------|------|--------|------|
| 1 | 도메인 DNS 전파 (가비아 NS → Cloudflare, api/app A 레코드) | 예 | — | 수동: [13-manual-operator-tasks.md §1.2](13-manual-operator-tasks.md) |
| 2 | api.neekly-report.cloud → 동작하는 API 서버(Backend) | 예 | — | AWS API 스택 배포·SSL 후 |
| 3 | app.neekly-report.cloud → Frontend + nginx(/api 프록시) | 예 | — | Oracle 2 엣지 전용 배포 후 |
| 4 | SSL (api/app 각각 Certbot 발급, nginx 마운트) | 예 | — | |
| 5 | nginx 설정 치환 (EXAMPLE_DOMAIN → neekly-report.cloud) | 예 | — | |
| 6 | Oracle 1 Security List (API 서버 → 5432/6379 허용) | 예 | — | [14-server-inbound-outbound-policy.md](14-server-inbound-outbound-policy.md). 13 §1.1 완료. |
| 7 | CD 배포 후 Backend 헬스체크 통과 (Oracle 2/3, AWS) | 예 | — | |
| 8 | 브라우저 E2E: app 접속 → 로그인 → API 호출 정상 | 예 | — | |

---

## 2. 서버별 배포 설계 대비 현황

| 노드 | 설계 역할 | 현재 배포 | 갭 |
|------|-----------|-----------|-----|
| Oracle 1 | 데이터 (TimescaleDB, Redis) | 데이터 계층 | — |
| AWS | API (Backend, prediction, data-collector, nginx api) | API 전용 compose·배포 | — |
| Oracle 2 | 엣지 (Frontend, nginx app, /api→AWS) | **엣지 전용** (Frontend, nginx) | CD·스크립트 반영 후 검증 |
| Oracle 3 | 매크로 전용 (앱 스택 없음) | 앱 스택 제거·정리 | CD·스크립트 반영 후 검증 |

---

## 3. 다음 할일 — 필수

- **옵션 B(설계안)**: AWS = API, Oracle 2/3 = 엣지.
- Oracle 1 Security List에 **AWS Public IP** 5432·6379 허용 ([14-server-inbound-outbound-policy.md](14-server-inbound-outbound-policy.md)). 13 §1.1 완료.
- **AWS**: API 전용 compose·deploy, `.env`(Oracle 1 DB/Redis), Certbot api.neekly-report.cloud, DNS api → AWS IP.
- **Oracle 2/3**: 엣지 전용 compose·deploy, nginx app.conf + location /api → AWS, Certbot app.neekly-report.cloud, DNS app → Oracle 2 IP.
- **CD**: `DEPLOY_HOST_AWS`, `SSH_PRIVATE_KEY_AWS` 등록 후 AWS job 실행.
- 배포 후 **브라우저 E2E** 검증.

---

## 4. 다음 할일 — 선택

- 스왑 확인(모든 노드 2GB 권장). JVM 힙 제한(Oracle 2에서 API 스택 유지 시). www CNAME. Cloudflare 프록시. 모니터링.

---

## 5. 문서 변경 이력

| 버전 | 일자 | 변경 내용 |
|------|------|-----------|
| 1.0 | 2026-02-20 | 초기 작성 — E2E 체크리스트, 서버별 현황, 필수/선택 할일. |
