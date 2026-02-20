# Oracle Cloud (OCI) VCN·서브넷 설계

## 개요

이 문서는 Oracle Cloud Infrastructure(OCI)에서 **Osaka**, **Korea**, **India West (Mumbai)** 3리전을 사용하는 멀티 VPS 구성의 VCN(Virtual Cloud Network)·서브넷·Security List 설계를 정리한다. 상세 배포·CI/CD는 [05-multi-vps-oracle-aws-cicd.md](05-multi-vps-oracle-aws-cicd.md)를 참조한다.

---

## 1. 리전과 VCN 관계

- **Oracle Osaka**(ap-osaka-1), **Oracle Korea**(ap-chuncheon-1), **India West (Mumbai)**(ap-mumbai-1)는 각각 **서로 다른 리전**이다.
- OCI에서 **VCN은 리전 단위**이므로, 한 VCN으로 여러 리전을 묶는 것은 불가능하다.
- **리전당 1 VCN** 구성: Osaka 1개, Korea 1개, Mumbai 1개.
- **크로스 리전 통신**: 동일 VCN 내 Private IP 사용 불가. 앱 노드(Korea, Mumbai)가 데이터 노드(Osaka)의 DB/Redis에 접속할 때 **Oracle 1(Osaka)의 Public IP**를 사용한다. 추후 Remote VCN Peering 검토 가능.

---

## 2. 리전별 VCN·서브넷 설계

### 2.1 공통 원칙

- VCN CIDR은 리전별로 겹치지 않게 하면 추후 Peering 시 충돌이 없다.
- 퍼블릭 서비스(SSH, HTTP/HTTPS)는 Public Subnet. 현재 1GB E2 1대씩 구성이면 **Public Subnet만**으로 운영하며, Security List로 포트 제한으로 보안을 보완한다.

### 2.2 리전당 1 VCN (Option A)

| 리전 | VCN 이름(예) | VCN CIDR | 서브넷 이름(예) | 서브넷 CIDR | 용도 |
|------|----------------|----------|-------------------|-------------|------|
| Osaka (ap-osaka-1) | vcn-investment-osaka | 10.0.0.0/16 | public-investment-osaka | 10.0.1.0/24 | 데이터 노드(Oracle 1) |
| Korea (ap-chuncheon-1) | vcn-investment-korea | 10.1.0.0/16 | public-investment-korea | 10.1.1.0/24 | 앱 노드(Oracle 2) |
| India West (ap-mumbai-1) | vcn-investment-mumbai (또는 기존 VCN 이름 사용) | 10.2.0.0/16 | 해당 VCN의 Public 서브넷 | — | 앱 노드(Oracle 3) |

- **인터넷 접속**: 각 VCN에 **Internet Gateway** 1개, 해당 서브넷의 기본 라우팅 테이블에 `0.0.0.0/0 → IGW` 추가.
- **E2 인스턴스**: 각 서브넷에 배치, Public IP 부여(또는 예약 Public IP). Private IP는 서브넷 CIDR 내 자동 할당.
- **Mumbai 실제 구성 (2026-02-11 프로비저닝)**: 리전 ap-mumbai-1, VCN **aifer-vcn**, Shape VM.Standard.E2.1.Micro, OS Canonical Ubuntu 24.04, 사용자 **ubuntu**. Public IP는 저장소에 기입하지 않고, 배포 시 GitHub Variables `DEPLOY_HOST_ORACLE_MUMBAI` 및 Oracle 1(Osaka) Security List(5432/6379) 허용에만 사용한다.

### 2.3 Private Subnet (선택, Option B)

- 데이터 노드만 Private Subnet에 두고 앱 노드는 Public Subnet에 두는 구성. Oracle 1을 Private에 두려면 NAT Gateway 필요.
- **현재는 Osaka(데이터)와 Korea/Mumbai(앱)가 리전이 다르므로**, Oracle 1을 Private에 두면 앱 노드가 Osaka Private IP에 직접 접근할 수 없다. 따라서 **Option A(Public 위주)** 가 단순하고, 보안은 Security List로 5432/6379를 앱 노드 IP만 허용하는 방식이 현실적이다.

---

## 3. Security List (방화벽) 설계

**서버별 인바운드/아웃바운드 규칙 전체**는 **[14-server-inbound-outbound-policy.md](14-server-inbound-outbound-policy.md)** 에 통합되어 있다. OCI Oracle 1·2·3 및 AWS 보안 그룹 정책은 해당 문서를 따른다.

- **Oracle 1**: 5432·6379 Ingress ← Oracle 2, Oracle 3, **AWS** Public IP; 22 ← 관리자/runner. Egress All.
- **Oracle 2·3**: 80·443·22 Ingress; Egress All. 크로스 리전 통신은 Oracle 1 Public IP 사용.

---

## 4. E2 서버 증설 시 배치

- **스왑·메모리 튜닝**(노드별 권장 스왑): [05-multi-vps-oracle-aws-cicd.md](05-multi-vps-oracle-aws-cicd.md) §3 참조.
- **동일 스펙**: VM.Standard.E2.1.Micro (1 OCPU, 1GB RAM). Always Free 한도 확인.
- **India West (Mumbai) — 프로비저닝 완료**: ap-mumbai-1, VCN aifer-vcn, VM.Standard.E2.1.Micro, Ubuntu 24.04, 사용자 ubuntu. 역할은 앱 계층(Oracle 3). **필수**: Oracle 1(Osaka) Security List에 Mumbai 인스턴스 **Public IP**를 TCP 5432, 6379 Ingress에 추가. 배포 전 investment-infra 클론 및 Docker·.env 설정은 [05-multi-vps-oracle-aws-cicd.md](05-multi-vps-oracle-aws-cicd.md) §Mumbai 노드 초기 설정 또는 `investment-infra/scripts/setup-oracle3-mumbai.sh` 참조.
- **기타 증설**: 앱 2호기는 Korea 또는 Mumbai Public 서브넷에 추가. 데이터 전용 추가는 Osaka에 추가. 모든 앱 노드 Public IP를 Oracle 1 Security List 5432/6379 허용 대상에 포함한다.

---

## 5. 참고 문서

- [14-server-inbound-outbound-policy.md](14-server-inbound-outbound-policy.md) — 서버별 인바운드/아웃바운드 정책 통합 (Oracle 1·2·3·AWS)
- [05-multi-vps-oracle-aws-cicd.md](05-multi-vps-oracle-aws-cicd.md) — 멀티 VPS 배포·CI/CD·보안·체크리스트
- [investment-infra README](../../../investment-infra/README.md)

---

## 문서 변경 이력

| 버전 | 일자 | 변경 내용 |
|------|------|-----------|
| 1.0 | 2026-02-11 | 초안: 3리전(Osaka, Korea, Mumbai) VCN/서브넷/Security List 설계 |
| 1.1 | 2026-02-11 | Mumbai 실제 프로비저닝 반영: ap-mumbai-1, VCN aifer-vcn, Ubuntu 24.04, 사용자 ubuntu. Public IP는 문서 미기입 원칙 유지. |
