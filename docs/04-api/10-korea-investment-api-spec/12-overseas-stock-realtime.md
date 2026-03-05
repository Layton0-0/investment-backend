# [해외주식] 실시간시세 API 세부 명세

원본: `[해외주식] 실시간시세.xlsx`

## 공통 Request Header

| 항목 | 필수 | 타입 | 설명 |
|------|------|------|------|
| content-type | Y | string | application/json; charset=utf-8 |
| authorization | Y | string | Bearer {access_token} |
| appkey | Y | string | 앱키 |
| appsecret | Y | string | 앱시크릿키 |
| tr_id | Y | string | 각 API별 TR_ID |
| custtype | Y | string | B: 법인, P: 개인 |

## API 목록 (원본 xlsx 시트별 상세 참조)

각 API별 **Header·Request Parameter·Body·Response** 의미는 원본 xlsx `[해외주식] 실시간시세.xlsx` 해당 시트의 Layout을 참조하세요.

| API 명(시트명) | Method | 비고 |
|---------------|--------|------|
| 해외주식 실시간호가 | GET | xlsx 시트 참조 |
| 해외주식 지연호가(아시아) | GET | |
| 해외주식 실시간지연체결가 | GET | |
| 해외주식 실시간체결통보 | GET | |
