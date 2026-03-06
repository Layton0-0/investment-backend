# 한국투자증권 API 로깅 체계

## 1. 목적

- 한국투자증권 Open API 호출·응답을 **동일한 포맷**으로 기록해 트러블슈팅·감사·성능 분석을 용이하게 한다.
- **민감정보**(appkey, appsecret, 계좌번호, 토큰, userId 등)는 반드시 마스킹하여 로그에 남기지 않는다. (공개 저장소·[logging-masking.mdc](../../../.cursor/rules/logging-masking.mdc) 준수)

## 2. 적용 범위

| 클라이언트 | 용도 | 로깅 적용 |
|-----------|------|----------|
| KoreaInvestmentAccountClient | 계좌·잔고·매수가능·매도가능·주문체결·자산현황·기간별손익 등 | 요청(path, tr_id, 마스킹 query) / 응답(rt_cd, msg_cd, msg1, output 요약) / 에러(status, body 요약) |
| KoreaInvestmentMarketDataClient | 현재가·일봉차트 | 동일 |
| KoreaInvestmentOrderClient | 국내/해외 주문 | 동일 |
| KoreaInvestmentTokenClient | 접근토큰·approval_key 발급 | 요청(path) / 응답 성공·실패(status, body 요약) |
| KoreaInvestmentRankClientImpl | 거래량순위·투자자매매동향 | 동일 |

## 3. 로그 레벨 및 포맷

### 3.1 요청 로그 (DEBUG)

- **시점**: API 호출 직전
- **포맷**: `[KIS-API] 요청 apiName={} path={} trId={} queryKeys={}`
- **내용**: API 이름, path, tr_id, query 파라미터 **키 목록만** (값은 민감 시 마스킹, 상세 값은 DEBUG에서만 제한적)
- **민감정보**: CANO, ACNT_PRDT_CD, authorization, appkey, appsecret, hashkey 등은 값 노출 금지. `LogMaskingUtil` 사용.

### 3.2 응답 성공 로그 (DEBUG)

- **시점**: 응답 수신 후, rt_cd="0" 등 정상 판단 시
- **포맷**: `[KIS-API] 응답 apiName={} status={} rt_cd={} msg_cd={} msg1={} outputKeys={} outputSize={}`
- **내용**: HTTP status, rt_cd, msg_cd, msg1, output 최상위 키 목록 또는 output 배열 길이
- **민감정보**: output 내 계좌번호·토큰 등은 요약만(예: outputCount) 또는 마스킹.

### 3.3 응답 비정상 로그 (WARN)

- **시점**: rt_cd != "0", output 없음, HTTP 4xx/5xx
- **포맷**: `[KIS-API] 오류 apiName={} status={} rt_cd={} msg_cd={} msg1={} bodySummary={}`
- **내용**: status, rt_cd, msg_cd, msg1, 응답 body 앞 200자 또는 요약(민감 필드 제외)
- **404**: path/tr_id 불일치 가능성 있으므로 body 전체를 WARN에 남겨 원인 파악 가능하게 함(이미 구현된 현재가 404 로깅 유지).

### 3.4 예외/실패 로그 (ERROR)

- **시점**: 예외 발생, 타임아웃, 재시도 포기
- **포맷**: `[KIS-API] 실패 apiName={} error={}`
- **내용**: API 이름, 예외 메시지(스택은 DEBUG 또는 ERROR 1회)
- **민감정보**: 예외 메시지 내 URL·계좌·키 등은 마스킹 후 로그.

## 4. 공통 유틸 (KoreaInvestmentApiLogging)

- **위치**: `com.investment.common.logging.KoreaInvestmentApiLogging`
- **역할**: 위 포맷에 맞춰 요청/응답/에러 로그를 남기는 **정적 메서드** 제공. 모든 한국투자증권 API 클라이언트에서 공통 사용.
- **메서드**:
  - `logRequest(apiName, path, trId, queryParamKeys)` — DEBUG
  - `logResponseSuccess(apiName, status, rtCd, msgCd, msg1, outputSummary)` — DEBUG
  - `logResponseError(apiName, status, rtCd, msgCd, msg1, bodySummary)` — WARN
  - `logFailure(apiName, throwable)` — ERROR (메시지), DEBUG(스택)
- **output 요약**: output이 Map이면 key Set 문자열, Array면 size. 민감 필드 값은 넣지 않음.

## 5. 기존 로그와의 정렬

- **AccountClient**: 기존 `logApiRequest`/`logApiResponse`(local 프로필 전용)는 유지하되, **공통 포맷**은 `KoreaInvestmentApiLogging`으로 추가. local이 아닐 때도 DEBUG로 요청 path/tr_id, 응답 rt_cd/msg1 수준은 남김.
- **MarketDataClient**: 현재가/차트 등 기존 log.debug/warn/error를 `KoreaInvestmentApiLogging` 호출로 통일하고, 응답 성공 시에도 rt_cd, output 요약을 DEBUG로 남김.
- **OrderClient**: 주문 요청/성공/실패 시 동일 포맷 적용.
- **TokenClient**: 토큰/approval_key 발급 요청 path, 응답 성공/실패(status, body 요약)를 동일 포맷으로.

## 6. 참조

- [10-korea-investment-api-spec.md](../10-korea-investment-api-spec.md) — 전체 API 요약
- [09-korea-investment-api-guide.md](../09-korea-investment-api-guide.md) — 구현 가이드
- [logging-masking.mdc](../../../.cursor/rules/logging-masking.mdc) — 민감정보 마스킹 규칙
- [02-security-configuration-reference.md](../../07-security/02-security-configuration-reference.md) — 로깅·마스킹 메서드
