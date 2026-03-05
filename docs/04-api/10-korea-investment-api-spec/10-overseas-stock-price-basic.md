# [해외주식] 기본시세 API 세부 명세

원본: `[해외주식] 기본시세.xlsx`

## 공통 Request Header

| 항목 | 필수 | 타입 | 설명 |
|------|------|------|------|
| content-type | Y | string | application/json; charset=utf-8 |
| authorization | Y | string | Bearer {access_token} |
| appkey | Y | string | 앱키 |
| appsecret | Y | string | 앱시크릿키 |
| tr_id | Y | string | 각 API별 TR_ID |
| custtype | Y | string | B: 법인, P: 개인 |
| personalseckey | N | string | [법인 필수] 고객식별키 |

## API 목록 (원본 xlsx 시트별 상세 참조)

각 API별 **Header·Request Parameter·Body·Response** 의미는 원본 xlsx `[해외주식] 기본시세.xlsx` 해당 시트의 Layout을 참조하세요.

| API 명(시트명) | Method | 비고 |
|---------------|--------|------|
| 해외주식 현재가상세 | GET | xlsx 시트 참조 |
| 해외주식 현재가 호가 | GET | |
| 해외주식 현재체결가 | GET | |
| 해외주식 체결추이 | GET | |
| 해외주식분봉조회 | GET | |
| 해외지수분봉조회 | GET | |
| 해외주식 기간별시세 | GET | |
| 해외주식 종목_지수_환율기간별시세(일_주_월_년) | GET | |
| 해외주식조건검색 | GET | |
| 해외결제일자조회 | GET | |
| 해외주식 상품기본정보 | GET | |
| 해외주식 업종별시세 | GET | |
| 해외주식 업종별코드조회 | GET | |
