# KRX Open API 전체 명세서

## 1. 문서 목적 및 사용 원칙

- 이 문서는 **한국거래소(KRX) Open API**(https://openapi.krx.co.kr)를 개발·연동할 때 **기준 명세 참조**로 사용한다.
- **API별 상세 명세**(Request Parameter·Response 블록·필드 의미)는 **[12-krx-api-spec/](12-krx-api-spec/README.md)** 폴더의 세부 문서를 참조한다.
- 원본 명세는 `investment-backend/docs/krx-api-docs/` 내 docx(Spec.docx, Spec (1)~(5).docx)이며, 이 문서는 이를 마크다운으로 정리한 것이다.
- 구현 후 이 문서의 "이미 구현된 API 요약" 표에 path·HTTP·용도를 반영해 유지한다.

---

## 2. 원본 API 명세서(docx) 목록

상세 스펙은 **12-krx-api-spec/** 내 개별 md에서 확인한다. 원본 docx 경로: `investment-backend/docs/krx-api-docs/`.

| 파일명 | API 명칭 | 경로 (BO/서비스) | 용도 |
|--------|----------|------------------|------|
| Spec.docx | 유가증권 일별매매정보 | /svc/apis/sto/stk_bydd_trd | 유가증권시장 주권 일별 매매정보 ('10년01월04일~) |
| Spec (1).docx | ETF 일별매매정보 | /svc/apis/etp/etf_bydd_trd | ETF(상장지수펀드) 일별 매매정보 |
| Spec (2).docx | KOSDAQ 시리즈 일별시세정보 | /svc/apis/idx/kosdaq_dd_trd | KOSDAQ 시리즈 지수 일별 시세 |
| Spec (3).docx | KRX 시리즈 일별시세정보 | /svc/apis/idx/krx_dd_trd | KRX 시리즈 지수 일별 시세 |
| Spec (4).docx | KOSPI 시리즈 일별시세정보 | /svc/apis/idx/kospi_dd_trd | KOSPI 시리즈 지수 일별 시세 |
| Spec (5).docx | 코스닥 일별매매정보 | /svc/apis/sto/ksq_bydd_trd | 코스닥시장 주권 일별 매매정보 ('10년01월04일~) |

- **Base URL**: `https://data-dbg.krx.co.kr` (설정 가능 시 `investment.data.krx.base-url`).
- **인증**: Request 헤더에 **AUTH_KEY** 필드로 인증키 전달. 서비스별 이용신청 후 발급.

---

## 3. 이미 구현된 API 요약

[10-data-collection-api.md](10-data-collection-api.md), [08-setup-guides/04-krx-api-required.md](../08-setup-guides/04-krx-api-required.md) 기준. 명세 필드명(OutBlock_1)은 [12-krx-api-spec/](12-krx-api-spec/README.md) 개별 문서 참조.

| API | path | HTTP | 용도 |
|-----|------|------|------|
| 유가증권 일별매매정보 | /svc/apis/sto/stk_bydd_trd | GET | KR 일별 시세 수집 → TB_DAILY_STOCK (KOSPI) |
| 코스닥 일별매매정보 | /svc/apis/sto/ksq_bydd_trd | GET | KR 일별 시세 수집 → TB_DAILY_STOCK (KOSDAQ) |

- **KrxApiClient**: `fetchDailyStockKospi`, `fetchDailyStockKosdaq`에서 위 두 API 호출. 동일 인증키(AUTH_KEY), 요청 basDd(yyyyMMdd). **KrxCollectionService**: 두 결과 합쳐 파싱 후 저장(OutBlock_1 필드명 ISU_CD, TDD_OPNPRC 등 명세 반영). 인증키 미설정 시 조회 스킵, 실패 시 한투 API 일봉 폴백 가능(설정 시).

---

## 4. 신규 API 추가 시 절차

1. 이 명세서에서 해당 API의 **원본 docx** 또는 **12-krx-api-spec/** 세부 문서를 확인한다.
2. KRX Open API 사이트에서 해당 **서비스 이용신청** 후 인증키를 발급받고, `KRX_AUTH_KEY`(또는 서비스별 키)에 설정한다.
3. path·쿼리(basDd 등)·헤더(AUTH_KEY)·응답 블록(OutBlock_1 등)에 맞춰 클라이언트를 구현한다.
4. 구현 후 이 문서 **§3 구현된 API 요약** 표에 path·HTTP·용도를 추가한다.

---

## 5. 참조 링크

- **세부 명세 폴더**: [12-krx-api-spec/](12-krx-api-spec/README.md)
- **원본 명세 폴더**: [investment-backend/docs/krx-api-docs/](../krx-api-docs/)
- **데이터 수집 API·설정**: [10-data-collection-api.md](10-data-collection-api.md)
- **KRX API 필요 목록(신청용)**: [08-setup-guides/04-krx-api-required.md](../08-setup-guides/04-krx-api-required.md)
- **KRX Open API**: https://openapi.krx.co.kr
