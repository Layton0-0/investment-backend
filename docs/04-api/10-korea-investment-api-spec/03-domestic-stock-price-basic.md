# [국내주식] 기본시세 API 세부 명세

원본: `[국내주식] 기본시세.xlsx`

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
| tr_cont, seq_no, mac_address, phone_number, ip_addr, gt_uid | N | string | 법인/연속조회 시 사용 |

## API 목록 (원본 xlsx 시트별 상세 참조)

각 API별 **Header·Request Parameter(Query)·Body·Response** 필드 의미는 원본 xlsx 해당 시트의 Layout(구분/Element/한글명/Type/Required/Length/Description)을 참조하세요.

| API 명(시트명) | Path | Method | 비고 |
|---------------|------|--------|------|
| 주식현재가 시세 | /uapi/domestic-stock/v1/quotations/... | GET | xlsx 시트 참조 |
| 주식현재가 시세2 | xlsx 시트 참조 | GET | |
| 주식현재가 체결 | xlsx 시트 참조 | GET | |
| 주식현재가 일자별 | xlsx 시트 참조 | GET | |
| 주식현재가 호가_예상체결 | xlsx 시트 참조 | GET | |
| 주식현재가 투자자 | xlsx 시트 참조 | GET | |
| 주식현재가 회원사 | xlsx 시트 참조 | GET | |
| 국내주식기간별시세(일_주_월_년) | xlsx 시트 참조 | GET | |
| 주식당일분봉조회 | xlsx 시트 참조 | GET | |
| 주식일별분봉조회 | xlsx 시트 참조 | GET | |
| 주식현재가 당일시간대별체결 | xlsx 시트 참조 | GET | |
| 주식현재가 시간외일자별주가 | xlsx 시트 참조 | GET | |
| 주식현재가 시간외시간별체결 | xlsx 시트 참조 | GET | |
| 국내주식 시간외현재가 | xlsx 시트 참조 | GET | |
| 국내주식 시간외호가 | xlsx 시트 참조 | GET | |
| 국내주식 장마감 예상체결가 | xlsx 시트 참조 | GET | |
| ETF_ETN 현재가 | xlsx 시트 참조 | GET | |
| ETF 구성종목시세 | xlsx 시트 참조 | GET | |
| NAV 비교추이(종목/일/분) | xlsx 시트 참조 | GET | |
