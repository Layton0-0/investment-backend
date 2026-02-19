# OCI 퍼블릭 서브넷 SSH 연결 실패 — Route Table 문제 정리

## 1. 현상

- **증상**: 인스턴스에 Public IP가 할당되어 있는데, SSH 접속 시 `Connection timed out` (port 22).
- **환경**: OCI Mumbai 리전, Public Subnet, Ubuntu 인스턴스, Security List에는 SSH(22) Ingress 규칙 존재 (`0.0.0.0/0` → TCP 22).

---

## 2. 원인

**퍼블릭 서브넷이 인터넷으로 나가는 경로(Route)가 없었음.**

- Public IP가 있어도, **서브넷에 연결된 Route Table**에 `0.0.0.0/0 → Internet Gateway` 규칙이 없으면:
  - 인스턴스 → 인터넷 트래픽(예: 패키지 설치)은 안 나가고,
  - 인터넷 → 인스턴스(예: SSH)도 **응답 경로가 없어** 타임아웃으로 보임.
- 즉, **Route Table 설정**이 퍼블릭 서브넷에 맞지 않았던 것이 근본 원인.

---

## 3. OCI에서 Route Table이 동작하는 방식

### 3.1 Route Table은 “서브넷”에 연결된다

- **Internet Gateway에** Route Table을 “연결”하는 것이 아님.
- **각 서브넷**이 하나의 **Route Table**을 사용한다.
- Route Table을 바꾸려면 **서브넷 편집**에서만 가능하다.

| 잘못된 이해 | 올바른 이해 |
|-------------|-------------|
| IGW에서 “Associate Different Route Table”로 퍼블릭 경로 설정 | 서브넷(Subnet) → Edit → Route table 드롭다운에서 원하는 Route Table 선택 |

### 3.2 “Private IP만 타깃” 제한

- OCI에는 **Internet Gateway를 타깃으로 허용하지 않는** Route Table이 있다.
- 이런 Route Table에는 다음만 가능하다:
  - 규칙 **Target = Private IP** (NAT Gateway, DRG, 커스텀 라우터 등), 또는
  - **규칙 없음(빈 Route Table)**.
- 여기에 `0.0.0.0/0 → Internet Gateway` 규칙을 넣으려고 하면 아래 오류가 난다.

```
Rules in the route table must use private IP as a target. Or the route table can be empty (no rules).
```

- **Default Route Table for &lt;VCN&gt;** 이 여러 서브넷(퍼블릭/프라이빗)에서 공유되거나, “Private 전용”으로 쓰이도록 되어 있으면 위 제한이 걸려 있을 수 있다.

---

## 4. 해결 방법 (요약)

1. **새 Route Table 생성** (VCN → Route Tables → Create Route Table).
2. 그 Route Table에 **규칙 1개 추가**:
   - **Target Type**: Internet Gateway  
   - **Destination CIDR Block**: `0.0.0.0/0`  
   - **Target Internet Gateway**: 해당 VCN의 Internet Gateway 선택  
3. **서브넷 편집**에서 퍼블릭 서브넷(예: `public-investment-mumbai`)의 **Route table**을 방금 만든 Route Table로 **변경**.
4. **Internet Gateway 화면에서** Route Table을 “연결”하거나 바꾸려 하지 않는다.

---

## 5. 공부용 체크리스트 — 퍼블릭 서브넷에서 SSH가 안 될 때

| 순서 | 확인 항목 | 설명 |
|------|-----------|------|
| 1 | 인스턴스 상태 | Running, Public IP 할당 여부 |
| 2 | **Route Table** | 해당 서브넷에 연결된 Route Table에 **0.0.0.0/0 → IGW** 규칙 있는지 |
| 3 | Route Table 연결 위치 | 서브넷(Subnet) → Edit → Route table 에서만 변경 가능 (IGW 쪽 아님) |
| 4 | Security List Ingress | SSH(22) 허용, Source `0.0.0.0/0` 또는 본인 IP |
| 5 | NSG | VNIC에 NSG가 붙어 있으면, 해당 NSG Ingress에도 TCP 22 허용 |
| 6 | 인스턴스 방화벽 | Ubuntu면 `ufw allow 22`, `ufw reload` |

---

## 6. 참고 — Skip Source/Destination Check

- **Route Rule의 Target이 Private IP**일 때(예: NAT용 VM, 라우터 VM)만 해당 VNIC에서 **Skip Source/Destination Check** 를 켜야 함.
- **0.0.0.0/0 → Internet Gateway** 규칙만 쓸 때는 이 옵션과 무관함.

---

## 7. 관련 문서

- [06-oci-vcn-subnet-design.md](06-oci-vcn-subnet-design.md) — VCN·서브넷·Security List 설계
- [05-multi-vps-oracle-aws-cicd.md](05-multi-vps-oracle-aws-cicd.md) — 멀티 VPS·CI/CD (토폴로지·노드 역할)
- [10-cicd-firewall-checklist.md](10-cicd-firewall-checklist.md) — 방화벽/Security List 작업 정리
