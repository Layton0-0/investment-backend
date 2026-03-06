# KRX Open API 필요 목록 (신청용)

## 개요

한국거래소 Open API(https://openapi.krx.co.kr)는 **서비스(API)별로 이용신청을 해야** 사용할 수 있다.  
이 문서는 본 프로젝트에서 사용 중이거나 추후 필요할 수 있는 KRX API를 명칭·API(경로/BO_ID)·용도 순으로 나열한다.  
실제 신청 절차·키 발급은 [KRX Open API](https://openapi.krx.co.kr) 사이트에서 진행한다.

---

## 1. 현재 사용 중인 API

| 번호 | API 명칭 (한글) | API 명칭 (영문/서비스) | 경로 / BO_ID | 용도 | 비고 |
|------|-----------------|------------------------|--------------|------|------|
| 1 | 유가증권 일별매매정보 | stk_bydd_trd | **Server endpoint**: `https://data-dbg.krx.co.kr/svc/apis/sto/stk_bydd_trd`<br>Request: InBlock_1 `basDd`(string, 기준일자 yyyyMMdd)<br>Response: OutBlock_1 리스트<br>Header: `AUTH_KEY` | KR 일별 시세 수집 → TB_DAILY_STOCK 저장, 유니버스·팩터 계산 입력 | KrxApiClient·KrxCollectionService에서 사용. **서비스 이용신청 필요.** ('10년01월04일 데이터부터 제공) |

- **인증**: Request 헤더에 인증키 값을 **AUTH_KEY** 필드에 추가하여 전달. (KRX 공식 안내)  
- **설정**: `investment.data.krx.auth-key`(환경변수 `KRX_AUTH_KEY`), `investment.data.krx.base-url`(기본 `https://data-dbg.krx.co.kr`).

#### 유가증권 일별매매정보 응답(OutBlock_1) 필드

| Name | Type | Description |
|------|------|--------------|
| BAS_DD | string | 기준일자 |
| ISU_CD | string | 종목코드 |
| ISU_NM | string | 종목명 |
| MKT_NM | string | 시장구분 |
| SECT_TP_NM | string | 소속부 |
| TDD_CLSPRC | string | 종가 |
| CMPPREVDD_PRC | string | 대비 |
| FLUC_RT | string | 등락률 |
| TDD_OPNPRC | string | 시가 |
| TDD_HGPRC | string | 고가 |
| TDD_LWPRC | string | 저가 |
| ACC_TRDVOL | string | 거래량 |
| ACC_TRDVAL | string | 거래대금 |
| MKTCAP | string | 시가총액 |
| LIST_SHRS | string | 상장주식수 |  
- **참고**: [데이터 수집 API](../04-api/10-data-collection-api.md), [로컬 설정](../08-setup-guides/01-local-setup-complete.md).

#### 테스트·명세서 확인용 KRX 사이트

| 구분 | URL | 비고 |
|------|-----|------|
| **주식 서비스 목록** (이용신청·API 목록) | [KRX Data Marketplace - 주식](https://openapi.krx.co.kr/contents/OPP/USES/service/OPPUSES002_S1.cmd) | 서비스별 이용신청, API ID 확인 |
| **API 인증키 발급** | KRX 사이트 → 마이페이지 → API 인증키 신청/발급내역 | 위 서비스 이용신청 후 동일 키로 호출 |

- 위 주식 목록에서 **유가증권 일별매매정보**(또는 동일 API ID) 서비스를 이용신청한 뒤, 발급된 인증키를 `KRX_AUTH_KEY`에 설정하면 `KrxApiClient`에서 호출 가능.  
- **상세 요청/응답 명세**: [12-krx-api-spec.md](../04-api/12-krx-api-spec.md) 및 [12-krx-api-spec/](../04-api/12-krx-api-spec/README.md) (원본 docx 기반 문서화).

---

## 2. 추후 확장 시 검토할 API

유니버스 필터·시그널에서 **업종/섹터·지수** 데이터를 쓸 계획인 경우, KRX Open API에 해당 서비스가 있으면 **별도 이용신청** 후 연동한다.  
(예: Sector Relative Strength용 업종별 수익률·종목-업종 매핑 등. 실제 서비스명·BO_ID는 KRX Open API 목록에서 확인.)

| 번호 | 용도 (예상) | 비고 |
|------|-------------|------|
| - | 업종/섹터 코드·수익률 | UniverseFilterService Sector RS 적용 시. KRX에 업종/지수 관련 서비스가 있으면 신청 후 BO_ID·경로를 이 문서에 추가. |
| - | 지수 일별 (KOSPI 등) | 전략·벤치마크용. 필요 시 서비스 목록에서 확인 후 신청. |

---

## 3. 정리

- **반드시 신청해야 하는 것**: 위 **§1 현재 사용 중인 API** 1건 (유가증권 일별매매정보, `https://data-dbg.krx.co.kr/svc/apis/sto/stk_bydd_trd`).
- **추가 신청**: §2는 기능 확장 시 KRX 사이트에서 서비스 목록을 확인한 뒤, 사용할 API를 골라 각각 이용신청한다.
- **키·설정**: 발급받은 인증키는 `KRX_AUTH_KEY`로 설정하고, 로그에는 마스킹하여 출력한다. ([보안 설정 참조](../07-security/02-security-configuration-reference.md))
