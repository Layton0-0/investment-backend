# 한국투자증권 Open API 전체 명세서

## 1. 문서 목적 및 사용 원칙

- 이 문서는 **한국투자증권 API**(계좌·주문·시세·인증 포함)를 개발·연동할 때 **기준 명세 참조**로 사용한다.
- **카테고리별 상세 명세**(각 API별 Header·Request Parameter·Body·Response 의미)는 **[10-korea-investment-api-spec/](10-korea-investment-api-spec/README.md)** 폴더의 세부 문서를 참조한다.
- 상세 요청/응답 필드는 **세부 폴더** 또는 **원본 xlsx**(`investment-backend/docs/korea-investment-api/` 내) 또는 **한국투자증권 MCP**로 확인한다.
- 구현 후 이 문서의 "이미 구현된 API 요약" 표에 path·TR_ID·메서드·한 줄 설명을 반영해 유지한다.

---

## 2. 원본 API 명세서(xlsx) 목록

상세 스펙(Header·Request Parameter·Body·Response)은 **[10-korea-investment-api-spec/](10-korea-investment-api-spec/README.md)** 내 카테고리별 md에서 확인한다. 원본은 아래 xlsx이며, **Excel Reader MCP**의 `sheet_names`·`read_excel`로 시트 목록·내용을 조회할 수 있다.  
경로 기준: `investment-backend/docs/korea-investment-api/` (프로젝트 루트 기준).


| 파일명                | 카테고리 | 용도                             |
| ------------------ | ---- | ------------------------------ |
| OAuth인증.xlsx       | 인증   | 접근토큰 발급/폐기, Hashkey, 웹소켓 접속키 등 |
| [국내주식] 종목정보.xlsx   | 국내주식 | 종목 정보 조회                       |
| [국내주식] 기본시세.xlsx   | 국내주식 | 기본 시세 조회                       |
| [국내주식] 시세분석.xlsx   | 국내주식 | 시세·분석 API                      |
| [국내주식] 실시간시세.xlsx  | 국내주식 | 실시간 시세                         |
| [국내주식] 순위분석.xlsx   | 국내주식 | 순위·투자자 매매동향 등                  |
| [국내주식] ELW 시세.xlsx | 국내주식 | ELW 시세                         |
| [국내주식] 업종_기타.xlsx  | 국내주식 | 업종·기타 API                      |
| [국내주식] 주문_계좌.xlsx  | 국내주식 | 주문·계좌(잔고, 매수/매도가능, 체결조회 등)     |
| [해외주식] 기본시세.xlsx   | 해외주식 | 해외 기본 시세                       |
| [해외주식] 시세분석.xlsx   | 해외주식 | 해외 시세·분석                       |
| [해외주식] 실시간시세.xlsx  | 해외주식 | 해외 실시간 시세                      |
| [해외주식] 주문_계좌.xlsx  | 해외주식 | 해외 주문·계좌                       |


**OAuth인증.xlsx 시트 예시** (Excel Reader MCP 기준): API 목록, 접근토큰발급(P), 접근토큰폐기(P), Hashkey, 실시간(웹소켓) 접속키 발급.

---

## 3. 종목/지수 코드(code 폴더)

경로: `investment-backend/docs/korea-investment-api/code/`.  
형식·용도·필드 의미: **[10-korea-investment-api-spec/14-code-folder.md](10-korea-investment-api-spec/14-code-folder.md)** 참조.  
API 파라미터(종목코드·거래소코드 등)는 아래 파일 또는 한국투자증권 MCP로 검증한다.


| 파일              | 설명                       |
| --------------- | ------------------------ |
| kospi_code.mst  | 코스피 종목코드 (고정폭 텍스트)       |
| kosdaq_code.mst | 코스닥 종목코드 (고정폭 텍스트)       |
| konex_code.mst  | 코넥스 종목코드 (고정폭 텍스트)       |
| elw_code.mst    | ELW 종목코드                 |
| idxcode.mst     | 지수·업종 코드 (종합, 대형주, 섹터 등) |
| frgn_code.mst   | 해외 종목·지수·거래소 (티커 등)      |
| AMSMST.COD      | 미국 AMEX 거래소 마스터          |
| NYSMST.COD      | 미국 NYSE 거래소 마스터          |
| NASMST.COD      | 미국 NASDAQ 거래소 마스터        |


---

## 4. 이미 구현된 API 요약 (계좌·주문·인증 중심)

09 가이드 기준. 상세는 [09-korea-investment-api-guide.md](09-korea-investment-api-guide.md) 참조.

### 4.1 인증


| API     | path           | TR_ID (실전/모의) | HTTP | 용도                                    |
| ------- | -------------- | ------------- | ---- | ------------------------------------- |
| 접근토큰 발급 | /oauth2/tokenP | (동일)          | POST | OAuth 2.0 client_credentials, 24시간 유효 |


### 4.2 계좌·잔고


| API           | path                                                       | TR_ID (실전)                    | TR_ID (모의)            | HTTP | 용도                |
| ------------- | ---------------------------------------------------------- | ----------------------------- | --------------------- | ---- | ----------------- |
| 주식잔고조회        | /uapi/domestic-stock/v1/trading/inquire-balance            | TTTC8434R                     | VTTC8434R             | GET  | 국내 잔고·보유종목        |
| 해외 현재잔고(체결기준) | /uapi/overseas-stock/v1/trading/inquire-present-balance    | CTRP6504R                     | VTRP6504R             | GET  | 미국(840) 체결잔고·보유종목 |
| 매수가능조회        | /uapi/domestic-stock/v1/trading/inquire-psbl-order         | TTTC8908R                     | VTTC8908R             | GET  | 종목별 매수 가능 금액/수량   |
| 매도가능수량조회      | /uapi/domestic-stock/v1/trading/inquire-psbl-order2        | TTTC8901R                     | VTTC8901R             | GET  | 종목별 매도 가능 수량      |
| 주식일별주문체결조회    | /uapi/domestic-stock/v1/trading/inquire-daily-ccld         | TTTC0081R / CTSC9215R(3개월 이전) | VTTC0081R / VTSC9215R | GET  | 일별 주문 체결 내역       |
| 투자계좌자산현황조회    | /uapi/domestic-stock/v1/trading/inquire-account-balance    | CTRP6548R                     | (모의 미지원, 잔고조회로 폴백)    | GET  | 계좌 자산 현황          |
| 기간별손익일별합산조회   | /uapi/domestic-stock/v1/trading/inquire-period-profit-loss | TTTC8708R                     | VTTC8708R             | GET  | 기간별 일별 손익         |


### 4.3 주문


| API       | path                                       | TR_ID (실전) | TR_ID (모의) | HTTP | 용도       |
| --------- | ------------------------------------------ | ---------- | ---------- | ---- | -------- |
| 국내 주문(매수) | /uapi/domestic-stock/v1/trading/order-cash | TTTC0012U  | VTTC0012U  | POST | 국내 주식 매수 |
| 국내 주문(매도) | /uapi/domestic-stock/v1/trading/order-cash | TTTC0011U  | VTTC0011U  | POST | 국내 주식 매도 |
| 해외 주문(매수) | /uapi/overseas-stock/v1/trading/order      | TTTT1002U  | VTTT1002U  | POST | 미국 주식 매수 |
| 해외 주문(매도) | /uapi/overseas-stock/v1/trading/order      | TTTT1006U  | VTTT1006U  | POST | 미국 주식 매도 |


### 4.4 시세·순위·기타


| API             | path                                                                | TR_ID (실전)    | TR_ID (모의)    | HTTP | 용도         |
| --------------- | ------------------------------------------------------------------- | ------------- | ------------- | ---- | ---------- |
| 주식현재가 일봉차트      | /uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice     | FHKST03010100 | (동일)          | GET  | 일/주/월봉 차트  |
| 거래량순위           | /uapi/domestic-stock/v1/quotations/volume-rank                      | FHPST01710000 | FHKST01710000 | GET  | 거래대금 상위 종목 |
| 시장별 투자자매매동향(일별) | /uapi/domestic-stock/v1/quotations/inquire-investor-daily-by-market | FHPST03010100 | FHKST03010100 | GET  | 시장 수급·분위   |


---

## 5. 신규 API 추가 시 절차

1. 이 명세서에서 해당 도메인(국내/해외/인증)의 **원본 xlsx**를 확인한다.
2. **xlsx** 또는 **한국투자증권 MCP**로 path, tr_id, 메서드(GET/POST), 필수 파라미터를 확인한 뒤 구현한다.
3. 구현 후 이 문서 **§4 구현된 API 요약** 표에 path·TR_ID·메서드·한 줄 설명을 추가한다.
4. 상세 구현 규칙·예시는 [09-korea-investment-api-guide.md](09-korea-investment-api-guide.md)에 유지·보강한다.

---

## 6. 참조 링크

- **세부 명세 폴더(Header/Parameter/Body/Response 전 항목)**: [10-korea-investment-api-spec/](10-korea-investment-api-spec/README.md)
- **원본 명세 폴더**: [investment-backend/docs/korea-investment-api/](../korea-investment-api/)
- **구현 가이드**: [09-korea-investment-api-guide.md](09-korea-investment-api-guide.md)
- **MCP 규칙**: [.cursor/rules/MCP.mdc](../../../.cursor/rules/MCP.mdc), [decisions.md §14](../decisions.md#14-한국투자증권-api-요청-방식-및-mcp-사용)
- **한국투자증권 API 포털**: [https://apiportal.koreainvestment.com/](https://apiportal.koreainvestment.com/)

