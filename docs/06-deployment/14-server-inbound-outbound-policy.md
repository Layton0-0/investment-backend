# 서버별 인바운드/아웃바운드 정책 (통합)

**목적**: Oracle 1·2·3(OCI Security List) 및 AWS(보안 그룹)의 **인바운드·아웃바운드 규칙**을 한 문서로 통합한다.  
실제 IP·CIDR은 저장소에 기입하지 않으며, OCI 콘솔·AWS 콘솔에서만 설정한다.

**참조**: [05-multi-vps-oracle-aws-cicd.md](05-multi-vps-oracle-aws-cicd.md), [06-oci-vcn-subnet-design.md](06-oci-vcn-subnet-design.md).

---

## 1. Oracle 1 (데이터, Osaka) — OCI Security List

| 방향 | 소스/대상 | 프로토콜·포트 | 용도 |
|------|-----------|----------------|------|
| **Ingress** | Oracle 2(Korea) Public IP/32 | TCP 5432 | TimescaleDB (앱→DB) |
| **Ingress** | Oracle 3(Mumbai) Public IP/32 | TCP 5432 | 동일 |
| **Ingress** | **AWS(API 계층) Public IP/32** | TCP 5432 | 동일 |
| **Ingress** | Oracle 2, Oracle 3, **AWS** Public IP/32 | TCP 6379 | Redis |
| **Ingress** | GitHub Actions runner IP 또는 관리자 IP | TCP 22 | SSH (배포·관리) |
| **Egress** | 0.0.0.0/0 | All | 패키지·이미지·기타 아웃바운드 |

- **설정 위치**: OCI 콘솔 → Networking → Virtual Cloud Networks → Oracle 1(Osaka) VCN → Security Lists → 해당 Security List → Add Ingress/Egress Rules.
- **필수**: Oracle 2·Oracle 3·**AWS**의 Public IP를 **5432, 6379** Ingress에 각각 추가. 증설 시 새 앱 노드 IP도 동일하게 추가.

---

## 2. Oracle 2 (앱/엣지, Korea) — OCI Security List

| 방향 | 소스/대상 | 프로토콜·포트 | 용도 |
|------|-----------|----------------|------|
| **Ingress** | 0.0.0.0/0 (또는 제한 가능) | TCP 80, 443 | HTTP/HTTPS (app.neekly-report.cloud 등) |
| **Ingress** | GitHub Actions runner IP 또는 관리자 IP | TCP 22 | SSH (CD 배포) |
| **Egress** | 0.0.0.0/0 | All | Oracle 1(Public IP), ghcr.io, AWS(api 도메인) 등 |

---

## 3. Oracle 3 (앱/매크로, India West Mumbai) — OCI Security List

| 방향 | 소스/대상 | 프로토콜·포트 | 용도 |
|------|-----------|----------------|------|
| **Ingress** | 0.0.0.0/0 (또는 제한 가능) | TCP 80, 443 | HTTP/HTTPS |
| **Ingress** | GitHub Actions runner IP 또는 관리자 IP | TCP 22 | SSH (CD 배포) |
| **Egress** | 0.0.0.0/0 | All | Oracle 1(Osaka) Public IP, ghcr.io 등 |

---

## 4. AWS (API 계층, 서울) — 보안 그룹

| 방향 | 소스/대상 | 프로토콜·포트 | 용도 |
|------|-----------|----------------|------|
| **Inbound** | 0.0.0.0/0 (또는 제한 가능) | TCP 22 | SSH (배포·관리) |
| **Inbound** | 0.0.0.0/0 (또는 제한 가능) | TCP 80 | HTTP (Certbot·리다이렉트) |
| **Inbound** | 0.0.0.0/0 (또는 제한 가능) | TCP 443 | HTTPS (api.neekly-report.cloud) |
| **Outbound** | 0.0.0.0/0 | All | Oracle 1(Public IP) 5432/6379, ghcr.io, 기타 |

- **설정 위치**: AWS 콘솔 → EC2 → Security Groups → 해당 인스턴스에 연결된 보안 그룹 → Edit inbound/outbound rules.
- **참고**: AWS는 아웃바운드 기본 허용인 경우가 많음. Oracle 1(Osaka) 쪽에서 **AWS Public IP**를 5432·6379 Ingress에 허용해야 Backend가 DB/Redis에 접속 가능하다.

---

## 5. 요약·체크리스트

| 노드 | Ingress/Inbound 필수 | Egress/Outbound |
|------|----------------------|------------------|
| Oracle 1 | 5432·6379 ← Oracle 2, Oracle 3, **AWS** IP; 22 ← 관리자/runner | All → 0.0.0.0/0 |
| Oracle 2 | 80·443 ← 0.0.0.0/0; 22 ← 관리자/runner | All → 0.0.0.0/0 |
| Oracle 3 | 80·443 ← 0.0.0.0/0; 22 ← 관리자/runner | All → 0.0.0.0/0 |
| AWS | 22·80·443 ← 0.0.0.0/0 | All → 0.0.0.0/0 |

---

## 6. 문서 변경 이력

| 버전 | 일자 | 변경 내용 |
|------|------|-----------|
| 1.0 | 2026-02-20 | 초안: Oracle 1/2/3·AWS 인바운드/아웃바운드 통합. 기존 06·10 문서 중복 제거 후 본 문서 참조로 통일. |
