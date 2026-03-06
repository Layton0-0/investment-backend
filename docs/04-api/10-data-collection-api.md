# 데이터 수집 API 및 설정

## 개요

데이터 수집 1단계: Open DART(한국 공시)·KRX(한국 시세)·Yahoo(미국 이벤트) 연동. 수집 데이터는 TB_NEWS_ITEMS에 저장되며, 원천별 Fallback 적용.

## 설정 (application.yml / 환경변수)

DART/SEC는 Spring에 설정 없음. Python 수집기 환경변수(DART_API_KEY, SEC_API_KEY 등)는 investment-data-collector README 참조.

| 프로퍼티 | 환경변수 | 설명 |
|----------|----------|------|
| investment.data.krx.auth-key | KRX_AUTH_KEY | KRX Open API 인증키 (로그 마스킹 대상) |
| investment.data.krx.base-url | KRX_BASE_URL | 기본: https://openapi.krx.co.kr |
| investment.data.krx.korea-investment-fallback-enabled | KRX_KOREA_INVESTMENT_FALLBACK_ENABLED | KRX 실패 시 한투 API 일봉 보조 소스 사용 여부 (기본 false) |
| investment.data.krx.korea-investment-fallback-user-id | KRX_KOREA_INVESTMENT_FALLBACK_USER_ID | 폴백 시 한투 API 호출에 쓸 사용자 ID (해당 사용자 API 키로 토큰 발급) |
| investment.data.krx.fallback-symbols-source | KRX_FALLBACK_SYMBOLS_SOURCE | PREVIOUS_DAY(전일 TB_DAILY_STOCK) 또는 CONFIG |
| investment.data.krx.fallback-symbols | KRX_FALLBACK_SYMBOLS | fallback-symbols-source=CONFIG일 때 종목 코드(쉼표 구분) |
| investment.data.us.collector-url | US_COLLECTOR_URL | Python 수집기 URL. 수동 DART/SEC 수집 및 US 일봉 호출에 사용 |
| investment.data.us.symbols | US_SYMBOLS | US 일봉 수집 대상. 기본: 지수·섹터 ETF(SPY,QQQ,XLK,XLF 등) + 대표 주식(퀀트 유니버스) |
| investment.data.internal-api-key | DATA_COLLECTION_INTERNAL_KEY | 내부 수집 API 키. 미설정 시 내부 API 비활성화 |

**SEC 유니버스**(Python 수집기): `SEC_CIKS` 미설정 시 **매 실행마다** SEC에서 최신 company_tickers.json을 수신한 뒤 `SEC_UNIVERSE=top100|top200|top500`만큼 CIK 사용(기본 top200). TOP N은 상장·변동으로 달라지므로 캐시 없이 매번 수신 후 진행.

## 내부 API (수집기 → Spring)

### POST /api/v1/internal/collected-news

Yahoo 등 외부 수집기가 수집한 뉴스·이벤트를 일괄 등록한다.

- **인증**: `X-Internal-Data-Key` 헤더에 `investment.data.internal-api-key` 와 동일한 값 전달. 미전달·불일치 시 401/403.
- **요청 본문**: `{ "items": [ { "source", "market", "itemType", "title", "summary", "url", "collectedAt", "symbol", "eventType" } ] }`
- **응답**: `{ "received": N, "saved": M }` (200). 중복(SOURCE+URL) 항목은 저장하지 않고 saved에 미포함.

## 스케줄 및 수동 수집

- **DART 공시 / SEC EDGAR 공시**: **Python investment-data-collector**에서 수행. 스케줄은 `POST /dart-collect`, `POST /sec-collect` 호출 또는 `SCHEDULE_DART_SEC=1`로 기동 시 10분(DART)/15분(SEC) 주기. 수동 수집은 **Spring POST /api/v1/news/collect** 가 **Python 수집기 API**(POST /dart-collect, POST /sec-collect)를 호출하는 구조. `investment.data.us.collector-url` 설정 필요.
- **Yahoo**: Cron 또는 수동으로 `collectors/yahoo_collector.py` 실행 후 Spring 내부 API로 전달.

## 배치와의 관계 (스케줄 및 실행 주체)

- **KRX/US 일봉 수집**의 **스케줄**은 Backend의 `BatchJobScheduler`(cron)에서만 관리된다. Backend가 정해진 시각에 해당 Job을 트리거한다.
- **KRX 일봉**: **실행 주체는 Backend 내부**(`KrxCollectionService`)이다. 1차로 KRX Open API를 호출하고, 실패(빈 결과) 시 설정이 되어 있으면 2차로 한투 API 일봉(inquire-daily-itemchartprice, 수정주가)을 종목별 조회해 TB_DAILY_STOCK에 저장한다. 수집 결과는 로그에 source=KRX / KOREA_INVESTMENT_FALLBACK / NONE으로 기록된다. **기준일자(basDt/basDd)**: KRX API 명세에 따라 요청·로그에는 **yyyyMMdd**(하이픈 없음) 사용. 트리거 API 파라미터는 REST 관례상 yyyy-MM-dd 수용.
- **US 일봉**: **실행 주체는 Backend → data-collector**이다. Backend가 `investment.data.us.collector-url`로 설정된 Python 수집기의 `POST /us-daily`를 호출하고, 수집 결과를 파싱해 DB에 저장한다.
- 향후 KRX 일봉도 data-collector로 이전할 경우, US와 동일하게 Backend cron이 트리거하고 Backend가 data-collector URL을 호출하는 패턴을 적용할 수 있다. 자세한 역할 분배 원칙은 프로젝트 루트 `plans/infra/20260221-1730_batch-role-assignment.md` 참조.

## 참고

- **KRX API 상세 명세**: [12-krx-api-spec.md](12-krx-api-spec.md), [12-krx-api-spec/](12-krx-api-spec/README.md) (유가증권 일별매매정보 등 Request/Response 필드)
- [뉴스·공시 수집·연동 설계](../02-architecture/13-news-collection-design.md)
- [보안 설정 참조](../07-security/02-security-configuration-reference.md) (investment.data, DART/KRX/내부 API 키)
- 프로젝트 루트 `plans/infra/20260221-1730_batch-role-assignment.md` (4개 레이어, Job 매트릭스, 이전 후보)
