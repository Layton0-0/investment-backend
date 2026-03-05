# [해외주식] 주문·계좌 API 세부 명세

원본: `[해외주식] 주문_계좌.xlsx`

## 공통 Request Header

| 항목 | 필수 | 타입 | 설명 |
|------|------|------|------|
| content-type | Y | string | application/json; charset=utf-8 |
| authorization | Y | string | Bearer {access_token} |
| appkey | Y | string | 앱키 |
| appsecret | Y | string | 앱시크릿키 |
| tr_id | Y | string | 각 API별 실전/모의 TR_ID |
| custtype | Y | string | B: 법인, P: 개인 |
| personalseckey | N | string | [법인 필수] 고객식별키 |

## 이미 구현된 API (09 가이드 참조)

- **해외주식 체결기준현재잔고**: Path `/uapi/overseas-stock/v1/trading/inquire-present-balance`, TR_ID 실전 CTRP6504R / 모의 VTRP6504R, GET. 미국(840) 체결잔고·보유종목.
- **해외주식 주문(매수/매도)**: Path `/uapi/overseas-stock/v1/trading/order`, TR_ID 실전 매수 TTTT1002U/매도 TTTT1006U, 모의 매수 VTTT1002U/매도 VTTT1006U, POST.

## API 목록 (원본 xlsx 시트별 상세 참조)

각 API별 **Header·Request Parameter·Body·Response** 의미는 원본 xlsx `[해외주식] 주문_계좌.xlsx` 해당 시트의 Layout을 참조하세요.

| API 명(시트명) | Method | 비고 |
|---------------|--------|------|
| 해외주식 주문 | POST | 구현: 09 가이드 |
| 해외주식 정정취소주문 | POST | xlsx 시트 참조 |
| 해외주식 예약주문접수 / 예약주문접수취소 | POST | |
| 해외주식 매수가능금액조회 | GET | |
| 해외주식 미체결내역 | GET | |
| 해외주식 잔고 | GET | |
| 해외주식 주문체결내역 | GET | |
| 해외주식 체결기준현재잔고 | GET | 구현: 09 가이드(CTRP6504R/VTRP6504R) |
| 해외주식 예약주문조회 | GET | |
| 해외주식 결제기준잔고 | GET | |
| 해외주식 일별거래내역 | GET | |
| 해외주식 기간손익 | GET | |
| 해외증거금 통화별조회 | GET | |
| 해외주식 미국주간주문 / 미국주간정정취소 | POST | |
| 해외주식 지정가주문번호조회 / 지정가체결내역조회 | GET | |
