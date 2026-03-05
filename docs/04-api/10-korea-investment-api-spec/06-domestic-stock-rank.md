# [국내주식] 순위분석 API 세부 명세

원본: `[국내주식] 순위분석.xlsx`

## 공통 Request Header

| 항목 | 필수 | 타입 | 설명 |
|------|------|------|------|
| content-type | Y | string | application/json; charset=utf-8 |
| authorization | Y | string | Bearer {access_token} |
| appkey | Y | string | 앱키 |
| appsecret | Y | string | 앱시크릿키 |
| tr_id | Y | string | 각 API별 TR_ID |
| custtype | Y | string | B: 법인, P: 개인 |

## API 목록

이미 구현된 API(09 가이드 참조): 거래량순위(FHPST01710000/FHKST01710000), 시장별 투자자매매동향(일별)(FHPST03010100/FHKST03010100).

그 외 API의 **Header·Request Parameter·Body·Response** 의미는 원본 xlsx `[국내주식] 순위분석.xlsx` 해당 시트의 Layout을 참조하세요.
