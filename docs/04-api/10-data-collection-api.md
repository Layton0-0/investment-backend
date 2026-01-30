# 데이터 수집 API 및 설정

## 개요

데이터 수집 1단계: Open DART(한국 공시)·KRX(한국 시세)·Yahoo(미국 이벤트) 연동. 수집 데이터는 TB_NEWS_ITEMS에 저장되며, 원천별 Fallback 적용.

## 설정 (application.yml / 환경변수)

| 프로퍼티 | 환경변수 | 설명 |
|----------|----------|------|
| investment.data.dart.api-key | DART_API_KEY | Open DART API 인증키 (로그 마스킹 대상) |
| investment.data.dart.base-url | DART_BASE_URL | 기본: https://opendart.fss.or.kr/api |
| investment.data.dart.collect-days | DART_COLLECT_DAYS | 수집 기간(일). 기본 3 |
| investment.data.dart.schedule-cron | DART_SCHEDULE_CRON | DART 수집 cron. 기본 10분마다 |
| investment.data.krx.auth-key | KRX_AUTH_KEY | KRX Open API 인증키 (로그 마스킹 대상) |
| investment.data.krx.base-url | KRX_BASE_URL | 기본: https://openapi.krx.co.kr |
| investment.data.internal-api-key | DATA_COLLECTION_INTERNAL_KEY | 내부 수집 API 키. 미설정 시 내부 API 비활성화 |

## 내부 API (수집기 → Spring)

### POST /api/v1/internal/collected-news

Yahoo 등 외부 수집기가 수집한 뉴스·이벤트를 일괄 등록한다.

- **인증**: `X-Internal-Data-Key` 헤더에 `investment.data.internal-api-key` 와 동일한 값 전달. 미전달·불일치 시 401/403.
- **요청 본문**: `{ "items": [ { "source", "market", "itemType", "title", "summary", "url", "collectedAt", "symbol", "eventType" } ] }`
- **응답**: `{ "received": N, "saved": M }` (200). 중복(SOURCE+URL) 항목은 저장하지 않고 saved에 미포함.

## 스케줄

- **DART 공시**: DataCollectionScheduler에서 10분마다 실행. DartCollectionService.collectAndSave() → NewsItem(SOURCE=DART, ITEM_TYPE=FACT) 저장.
- **Yahoo**: Cron 또는 수동으로 `scripts/yahoo_collector.py` 실행 후 Spring 내부 API로 전달.

## 참고

- [뉴스·공시 수집·연동 설계](../02-architecture/13-news-collection-design.md)
- [보안 설정 참조](../07-security/02-security-configuration-reference.md) (investment.data, DART/KRX/내부 API 키)
