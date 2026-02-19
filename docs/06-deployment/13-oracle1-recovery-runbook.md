# Oracle 1 (Osaka) 복구 Runbook

Oracle 1은 **데이터 계층(SPOF)** 이다. 해당 노드 장애 시 전체 API·엣지가 정상이어도 서비스 불가. 아래는 장애 시 복구 절차 요약이다.

---

## 1. 장애 감지

- 앱 노드(Oracle 2, AWS)에서 Backend 로그에 DB/Redis 연결 실패.
- 사용자 요청 타임아웃 또는 5xx.
- 모니터링: `investment-infra/scripts/monitor-node-resources.sh` 또는 OCI 콘솔에서 인스턴스 상태 확인.

---

## 2. 복구 절차 (요약)

| 순서 | 단계 | 내용 |
|------|------|------|
| 1 | **접속 확인** | SSH로 Oracle 1 접속 시도. 불가 시 OCI 콘솔 → Compute → 해당 인스턴스 상태·콘솔 연결 확인. |
| 2 | **인스턴스 재부팅** | OCI 콘솔에서 인스턴스 **재부팅**(Stop → Start). 필요 시 부트 볼륨·네트워크 확인. |
| 3 | **서비스 기동 순서** | SSH 접속 가능해지면 `~/investment-infra`에서 **TimescaleDB → Redis** 순으로 기동. Compose로 한 번에 올릴 경우 `docker compose -f docker-compose.oracle1-osaka-data.yml up -d` (DB가 먼저 기동되도록 depends_on 또는 수동 순서 유지). |
| 4 | **연결 확인** | 앱 노드(Oracle 2, AWS)에서 Backend·data-collector가 Oracle 1 Public IP로 5432/6379 접속되는지 확인. `curl http://localhost:8080/actuator/health` (앱 노드에서). |

---

## 3. Oracle 1에서 서비스 재기동

```bash
cd ~/investment-infra  # 또는 investment-infra 경로
docker compose -f docker-compose.oracle1-osaka-data.yml down
docker compose -f docker-compose.oracle1-osaka-data.yml up -d
docker compose -f docker-compose.oracle1-osaka-data.yml ps
```

- DB가 완전히 기동된 뒤 Redis가 올라가야 앱이 정상 연결된다. Compose 기본 순서로 충분. 문제 시 `docker compose ... up -d timescaledb` 후 `docker compose ... up -d redis`.

---

## 4. Redis 복구 (AOF)

- Redis는 **AOF**(appendonly yes, appendfsync everysec)로 설정되어 있으면 재기동 시 `/data/appendonly.aof` 로드.
- 데이터 손실 최소화. AOF 손상 시: `docker run --rm -v <redis_volume>:/data redis:7-alpine redis-check-aof --fix /data/appendonly.aof` 후 Redis 컨테이너 재기동.

---

## 5. TimescaleDB 복구 (백업에서)

- 정기 백업(`investment-infra/scripts/backup-oracle1-db.sh` 등)이 있다면, 백업 파일을 해당 노드로 복사한 뒤:
  - DB 컨테이너 기동 후 `psql` 또는 `pg_restore`로 복원. (상세는 백업 스크립트·문서 참조.)
- 백업이 없으면 마지막 커밋까지의 데이터만 유지. WAL 설정에 따라 다름.

---

## 6. 사후 확인

- [ ] Oracle 1: `docker compose -f docker-compose.oracle1-osaka-data.yml ps` — timescaledb, redis 모두 Up.
- [ ] 앱 노드: Backend health `GET /actuator/health` 200.
- [ ] 사용자 요청(로그인·API 호출) 정상 동작 샘플 확인.

---

## 참고

- [05-multi-vps-oracle-aws-cicd.md](05-multi-vps-oracle-aws-cicd.md) §3.1 — Oracle 1 SPOF·스왑·shared_buffers.
- [06-oci-vcn-subnet-design.md](06-oci-vcn-subnet-design.md) §3.1 — Security List(5432, 6379).
