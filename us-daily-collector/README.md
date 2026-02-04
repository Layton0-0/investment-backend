# US 일봉 수집 서비스 (yfinance)

Docker Compose로 기동하는 US 시장 일별 시세 수집 서비스. Spring 앱이 HTTP로 호출해 TB_DAILY_STOCK에 저장한다.

## 기동

```bash
# 프로젝트 루트에서
docker-compose up -d us-daily-collector
```

- 포트: 8001
- 헬스: `GET http://localhost:8001/health`
- 수집: `POST http://localhost:8001/us-daily` — body: `{ "bas_dt": "YYYY-MM-DD", "symbols": ["SPY", "TLT", ...] }`

## Spring 연동

`.env` 또는 환경 변수에 다음을 설정하면 스크립트 대신 이 서비스를 사용한다.

- **호스트에서 앱 실행 시**: `US_COLLECTOR_URL=http://localhost:8001`
- **앱도 Docker 내부에서 실행 시**: `US_COLLECTOR_URL=http://us-daily-collector:8001`
