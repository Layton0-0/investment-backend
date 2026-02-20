# 배포·인프라 문서 인덱스 (분야별 통합)

**목적**: 배포·인프라 문서를 **분야별로 정리**하고, **중복 제거 상태**와 **최신 기준 읽는 순서**를 한 곳에서 안내한다.  
**갱신 원칙**: 새 배포 문서 추가·역할 변경 시 이 인덱스를 수정한다.

**최종 갱신**: 2026-02-20

---

## 1. 현재 채택 설계 (최신 기준)

- **운영 배포**: **Oracle 3대(Oracle 1 데이터 / Oracle 2 엣지 / Oracle 3 매크로) + AWS 1대(API 계층)**.  
  상세 토폴로지·메모리·CI/CD·보안은 **[05-multi-vps-oracle-aws-cicd.md](05-multi-vps-oracle-aws-cicd.md)** 가 **단일 정리 문서**이다.
- **방화벽·Security List**: 서버별 인바운드/아웃바운드 규칙은 **한 문서로 통합**되어 있음 → **[14-server-inbound-outbound-policy.md](14-server-inbound-outbound-policy.md)**.  
  (과거 06-oci §3, 10-cicd §3에 있던 중복 내용은 제거 후 14 참조로 통일됨.)

---

## 2. 분야별 문서 목록·읽는 순서

### 2.1 배포 설계·토폴로지 (메인)

| 순서 | 문서 | 역할 | 비고 |
|------|------|------|------|
| 1 | [05-multi-vps-oracle-aws-cicd.md](05-multi-vps-oracle-aws-cicd.md) | **메인 배포 문서** — 노드 역할·스펙·메모리 튜닝·CI/CD·보안·체크리스트 | 최신 설계 기준 |
| 2 | [06-oci-vcn-subnet-design.md](06-oci-vcn-subnet-design.md) | OCI 3리전(Osaka/Korea/Mumbai) VCN·서브넷 설계 | Security List 상세는 14 참조 |
| 3 | [09-oci-node-structure.md](09-oci-node-structure.md) | Oracle 1/2 노드 홈 디렉터리·OCI·매크로 유지 정책 | 정리 대상·수행 내용 |

### 2.2 방화벽·보안 (통합됨, 중복 없음)

| 문서 | 역할 |
|------|------|
| **[14-server-inbound-outbound-policy.md](14-server-inbound-outbound-policy.md)** | **통합 문서** — Oracle 1·2·3(OCI Security List), AWS 보안 그룹 인바운드/아웃바운드 규칙 전부 |

- 06-oci §3, 10-cicd §3의 방화벽 내용은 **14로 이전·통합**되었으며, 해당 문서들은 14를 참조한다.

### 2.3 CI/CD·DevOps

| 순서 | 문서 | 역할 |
|------|------|------|
| 1 | [07-cicd-implementation-checklist.md](07-cicd-implementation-checklist.md) | CI/CD 구현 체크리스트·사전 점검·노드별 SSH 점검 절차 |
| 2 | [08-devops-required-tokens-and-keys.md](08-devops-required-tokens-and-keys.md) | GitHub Actions Secrets/Variables·로컬 SSH MCP·이미지 태그 목록 |
| 3 | [10-cicd-firewall-checklist.md](10-cicd-firewall-checklist.md) | CI 상태 표·CD 사전 조건 요약·방화벽은 **14 참조** |

### 2.4 DNS·도메인·E2E

| 순서 | 문서 | 역할 |
|------|------|------|
| 1 | [11-dns-and-domain-setup.md](11-dns-and-domain-setup.md) | 가비아 도메인 + Cloudflare DNS·nginx·api/app 서브도메인 설정 |
| 2 | [12-domain-e2e-readiness.md](12-domain-e2e-readiness.md) | 도메인 E2E 완료 체크리스트·서버별 현황·다음 할일(필수/선택) |
| 3 | [13-manual-operator-tasks.md](13-manual-operator-tasks.md) | **운영자 수동 작업**만 목록 — Security List·DNS·GitHub Secrets·스왑·Certbot 등 |

### 2.5 복구·트러블슈팅

| 문서 | 역할 |
|------|------|
| [13-oracle1-recovery-runbook.md](13-oracle1-recovery-runbook.md) | Oracle 1(Osaka) 데이터 계층 복구 절차 |
| [10-oci-public-subnet-route-table-troubleshooting.md](10-oci-public-subnet-route-table-troubleshooting.md) | OCI Public Subnet·라우팅 테이블·SSH 접속 트러블슈팅 |

### 2.6 기타·참고

| 문서 | 역할 | 비고 |
|------|------|------|
| [01-deployment-guide.md](01-deployment-guide.md) | 일반 배포 가이드(빌드·로컬·Docker·Kubernetes 개요) | **실제 운영 배포는 05 + 본 폴더 문서** 참조 |
| [02-operations-guide.md](02-operations-guide.md) | 운영 가이드 | |
| [03-server-specification.md](03-server-specification.md) | 서버 스펙 | 05 §1과 연동 |
| [04-minimal-cost-setup.md](04-minimal-cost-setup.md) | **최소 비용 대안** (통합 서버 1대·MariaDB 등) | **현재 채택 설계와 다름** — 참고용 대안 |
| [12-local-desktop-topology-options.md](12-local-desktop-topology-options.md) | 로컬 데스크탑 토폴로지 옵션 | 05에서 링크 |

---

## 3. 중복 제거 요약

| 이전 분산 내용 | 통합/정리 결과 |
|----------------|----------------|
| OCI·AWS 인바운드/아웃바운드 규칙이 06 §3, 10 §3 등에 분산 | **14-server-inbound-outbound-policy.md** 단일 문서로 통합. 06·10·13은 "14 참조"로 정리 |
| 배포 토폴로지·노드 역할이 플랜 문서와 다수 문서에 혼재 | **05-multi-vps-oracle-aws-cicd.md** 가 단일 정리 문서. 플랜(.cursor/plans)은 설계 산출 후 05·14·12-domain-e2e 등에 반영됨 |

---

## 4. 참조

- **인프라 저장소**: [investment-infra](../../../investment-infra/README.md)
- **단일 VPS 배포**: [08-setup-guides/06-single-vps-batch-deployment.md](../08-setup-guides/06-single-vps-batch-deployment.md)
- **개발 진행 현황**: [09-planning/02-development-status.md](../09-planning/02-development-status.md) — 완료·진행중·진행예정

---

## 5. 문서 변경 이력

| 버전 | 일자 | 변경 내용 |
|------|------|-----------|
| 1.0 | 2026-02-20 | 초안: 분야별 문서 목록·읽는 순서, 현재 채택 설계, 중복 제거 요약. |

