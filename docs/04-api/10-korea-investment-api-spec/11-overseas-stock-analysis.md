# [해외주식] 시세분석 API 세부 명세

원본: `[해외주식] 시세분석.xlsx`

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

각 API별 **Header·Request Parameter·Body·Response** 의미는 원본 xlsx `[해외주식] 시세분석.xlsx` 해당 시트의 Layout을 참조하세요.

| API 명(시트명) | Method | 비고 |
|---------------|--------|------|
| 해외주식 가격급등락 | GET | xlsx 시트 참조 |
| 해외주식 거래량급증 | GET | |
| 해외주식 매수체결강도상위 | GET | |
| 해외주식 상승율_하락율 | GET | |
| 해외주식 신고_신저가 | GET | |
| 해외주식 거래량순위 | GET | |
| 해외주식 거래대금순위 | GET | |
| 해외주식 거래증가율순위 | GET | |
| 해외주식 거래회전율순위 | GET | |
| 해외주식 시가총액순위 | GET | |
| 해외주식 기간별권리조회 | GET | |
| 해외뉴스종합(제목) | GET | |
| 해외주식 권리종합 | GET | |
| 당사 해외주식담보대출 가능 종목 | GET | |
| 해외속보(제목) | GET | |
