# OAuth 인증 API 세부 명세

원본: `OAuth인증.xlsx`

---

## 1. 접근토큰 발급 (API ID: 인증-001)

본인 계좌에 필요한 인증 절차로, 인증을 통해 접근 토큰을 부여받아 오픈API 활용이 가능합니다.

- **Path**: `/oauth2/tokenP`
- **Method**: POST
- **실전 Domain**: `https://openapi.koreainvestment.com:9443`
- **모의 Domain**: `https://openapivts.koreainvestment.com:29443`

### 1.1 Header

| 항목 | 필수 | 타입 | 설명 |
|------|------|------|------|
| (없음) | - | - | Request Header 항목 없음. Body만 전송. |

### 1.2 Request Parameter

쿼리 파라미터 없음. 모든 값은 Request Body로 전송.

### 1.3 Body

| 필드 | 한글명 | 타입 | 필수 | Length | 설명 |
|------|--------|------|------|--------|------|
| grant_type | 권한부여 Type | string | Y | 18 | `client_credentials` 고정 |
| appkey | 앱키 | string | Y | 36 | 한국투자증권 홈페이지에서 발급받은 appkey (절대 노출 금지) |
| appsecret | 앱시크릿키 | string | Y | 180 | 한국투자증권 홈페이지에서 발급받은 appsecret (절대 노출 금지) |

### 1.4 Response

| 필드 | 한글명 | 타입 | 필수 | 설명 |
|------|--------|------|------|------|
| access_token | 접근토큰 | string | Y | OAuth 토큰이 필요한 API 호출 시 사용. Bearer 토큰. 일반/법인 고객별 유효기간 상이(1일/3개월 등). 6시간 이내 재호출 시 직전 토큰값 리턴 가능. |
| token_type | 접근토큰유형 | string | Y | `Bearer`. API 호출 시 "Bearer {access_token}" 형식으로 헤더에 입력. |
| expires_in | 접근토큰 유효기간 | number | Y | 유효기간(초). 예: 86400, 7776000 |
| access_token_token_expired | 접근토큰 유효기간(일시) | string | Y | 유효기간(년:월:일 시:분:초). 예: "2022-08-30 08:10:10" |

**참고**: 접근토큰 유효기간은 24시간(1일 1회 발급 원칙). 갱신 발급 주기는 6시간(6시간 이내는 기존 발급키로 응답). '23.4.28 이후 잦은 토큰 발급 제어로 일정 시간 이내 재호출 시 직전 토큰값 리턴.

---

## 2. 접근토큰 폐기 (API ID: 인증-002)

부여받은 접근토큰을 더 이상 활용하지 않을 때 사용합니다.

- **Path**: `/oauth2/revokeP`
- **Method**: POST
- **실전 Domain**: `https://openapi.koreainvestment.com:9443`
- **모의 Domain**: `https://openapivts.koreainvestment.com:29443`

### 2.1 Header

| 항목 | 필수 | 타입 | 설명 |
|------|------|------|------|
| (없음) | - | - | Request Header 항목 없음. Body만 전송. |

### 2.2 Request Parameter

쿼리 파라미터 없음.

### 2.3 Body

| 필드 | 한글명 | 타입 | 필수 | Length | 설명 |
|------|--------|------|------|--------|------|
| appkey | 고객 앱Key | string | Y | 36 | 한국투자증권 홈페이지에서 발급받은 appkey (절대 노출 금지) |
| appsecret | 고객 앱Secret | string | Y | 180 | 한국투자증권 홈페이지에서 발급받은 appsecret (절대 노출 금지) |
| token | 접근토큰 | string | Y | 286 | 폐기할 Access token (일반: 1일 유효, 법인: 3개월 유효 등) |

### 2.4 Response

| 필드 | 한글명 | 타입 | 필수 | 설명 |
|------|--------|------|------|------|
| code | 응답코드 | string | N | HTTP 응답코드. 예: 200 |
| message | 응답메세지 | string | N | 예: "접근토큰 폐기에 성공하였습니다" (최대 450자) |

---

## 3. Hashkey (API ID: Hashkey)

요청 값을 변조 방지하기 위한 해시키 발급. POST 주문/정정/취소 API의 body 암호화에 사용. 비필수(미사용 시에도 POST 호출 가능).

- **Path**: `/uapi/hashkey`
- **Method**: POST
- **실전 Domain**: `https://openapi.koreainvestment.com:9443`
- **모의 Domain**: `https://openapivts.koreainvestment.com:29443`

### 3.1 Header

| 항목 | 필수 | 타입 | Length | 설명 |
|------|------|------|--------|------|
| content-type | N | string | 40 | `application/json; charset=utf-8` |
| appkey | Y | string | 36 | 한국투자증권 홈페이지에서 발급받은 appkey (절대 노출 금지) |
| appsecret | Y | string | 180 | 한국투자증권 홈페이지에서 발급받은 appsecret (절대 노출 금지) |

### 3.2 Request Parameter

쿼리 파라미터 없음.

### 3.3 Body

| 필드 | 한글명 | 타입 | 필수 | 설명 |
|------|--------|------|------|------|
| JsonBody | 요청값 | object | Y | 실제 POST API로 보낼 body 값 전체. 예: CANO, ACNT_PRDT_CD, OVRS_EXCG_CD, 주문 관련 필드 등. 이 값을 hashkey API로 보내 HASH를 받아 해당 POST 요청의 hashkey 헤더로 사용. |

### 3.4 Response

| 필드 | 한글명 | 타입 | 필수 | 설명 |
|------|--------|------|------|------|
| BODY | 요청값 | object | Y | 요청한 JsonBody 그대로 반환 |
| HASH | 해쉬키 | string | Y | 256자 이내. Client가 요청하는 Request Body를 hashkey API로 생성한 Hash값. POST API 호출 시 헤더 등에 사용. |

---

## 4. 실시간(웹소켓) 접속키 발급 (API ID: 실시간-000)

웹소켓 이용 시 appkey/appsecret 대신 헤더에 넣어 사용하는 접속키 발급.

- **Path**: `/oauth2/Approval`
- **Method**: POST
- **실전 Domain**: `https://openapi.koreainvestment.com:9443`
- **모의 Domain**: `https://openapivts.koreainvestment.com:29443`

### 4.1 Header

| 항목 | 필수 | 타입 | Length | 설명 |
|------|------|------|--------|------|
| content-type | N | string | 20 | `application/json; utf-8` |

### 4.2 Request Parameter

쿼리 파라미터 없음.

### 4.3 Body

| 필드 | 한글명 | 타입 | 필수 | Length | 설명 |
|------|--------|------|------|--------|------|
| grant_type | 권한부여타입 | string | Y | 18 | `client_credentials` |
| appkey | 앱키 | string | Y | 36 | 한국투자증권 홈페이지에서 발급받은 appkey (절대 노출 금지) |
| secretkey | 시크릿키 | string | Y | 180 | appsecret와 동일(용어만 다름). 절대 노출 금지. |

### 4.4 Response

| 필드 | 한글명 | 타입 | 필수 | 설명 |
|------|--------|------|------|------|
| approval_key | 웹소켓 접속키 | string | Y | 286자 이내. 웹소켓 이용 시 appkey·appsecret 대신 헤더에 넣어 API 호출. 유효기간 24시간이지만 세션 연결 시 1회만 사용하므로 세션 유지 시 365일 수신 가능. |
