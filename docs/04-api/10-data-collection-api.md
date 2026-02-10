# 데이터 수집 API 및 설정

## 개요

데이터 수집 1단계: Open DART(한국 공시)·KRX(한국 시세)·Yahoo(미국 이벤트) 연동. 수집 데이터는 TB_NEWS_ITEMS에 저장되며, 원천별 Fallback 적용.

## 설정 (application.yml / 환경변수)

DART/SEC는 Spring에 설정 없음. Python 수집기 환경변수(DART_API_KEY, SEC_API_KEY 등)는 investment-data-collector README 참조.

| 프로퍼티 | 환경변수 | 설명 |
|----------|----------|------|
| investment.data.krx.auth-key | KRX_AUTH_KEY | KRX Open API 인증키 (로그 마스킹 대상) |
| investment.data.krx.base-url | KRX_BASE_URL | 기본: https://openapi.krx.co.kr |
| investment.data.us.collector-url | US_COLLECTOR_URL | Python 수집기 URL. 수동 DART/SEC 수집 및 US 일봉 호출에 사용 |
| investment.data.internal-api-key | DATA_COLLECTION_INTERNAL_KEY | 내부 수집 API 키. 미설정 시 내부 API 비활성화 |

## 내부 API (수집기 → Spring)

### POST /api/v1/internal/collected-news

Yahoo 등 외부 수집기가 수집한 뉴스·이벤트를 일괄 등록한다.

- **인증**: `X-Internal-Data-Key` 헤더에 `investment.data.internal-api-key` 와 동일한 값 전달. 미전달·불일치 시 401/403.
- **요청 본문**: `{ "items": [ { "source", "market", "itemType", "title", "summary", "url", "collectedAt", "symbol", "eventType" } ] }`
- **응답**: `{ "received": N, "saved": M }` (200). 중복(SOURCE+URL) 항목은 저장하지 않고 saved에 미포함.

## 스케줄 및 수동 수집

- **DART 공시 / SEC EDGAR 공시**: **Python investment-data-collector**에서 수행. 스케줄은 `POST /dart-collect`, `POST /sec-collect` 호출 또는 `SCHEDULE_DART_SEC=1`로 기동 시 10분(DART)/15분(SEC) 주기. 수동 수집은 **Spring POST /api/v1/news/collect** 가 **Python 수집기 API**(POST /dart-collect, POST /sec-collect)를 호출하는 구조. `investment.data.us.collector-url` 설정 필요.
- **Yahoo**: Cron 또는 수동으로 `collectors/yahoo_collector.py` 실행 후 Spring 내부 API로 전달.

## 참고

- [뉴스·공시 수집·연동 설계](../02-architecture/13-news-collection-design.md)
- [보안 설정 참조](../07-security/02-security-configuration-reference.md) (investment.data, DART/KRX/내부 API 키)
