# [국내주식] 시세분석 API 세부 명세

원본: `[국내주식] 시세분석.xlsx`

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

## API 목록

각 API별 **Header·Request Parameter·Body·Response** 의미는 원본 xlsx `[국내주식] 시세분석.xlsx` 해당 시트의 Layout을 참조하세요.
