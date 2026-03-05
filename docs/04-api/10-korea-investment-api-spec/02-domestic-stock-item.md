# [국내주식] 종목정보 API 세부 명세

원본: `[국내주식] 종목정보.xlsx`

## 공통 Request Header (종목정보 API 공통)

| 항목 | 필수 | 타입 | 설명 |
|------|------|------|------|
| content-type | Y | string | application/json; charset=utf-8 |
| authorization | Y | string | Bearer {access_token} (OAuth 접근토큰) |
| appkey | Y | string | 앱키 (절대 노출 금지) |
| appsecret | Y | string | 앱시크릿키 (절대 노출 금지) |
| tr_id | Y | string | 각 API별 실전/모의 TR_ID |
| custtype | Y | string | B: 법인, P: 개인 |
| personalseckey | N | string | [법인 필수] 고객식별키 |
| tr_cont | N | string | 연속 거래 여부 |
| seq_no | N | string | [법인 필수] 001 |
| mac_address | N | string | 맥주소 |
| phone_number | N | string | [법인] 핸드폰번호 (하이픈 제거) |
| ip_addr | N | string | [법인] 공인 IP |
| gt_uid | N | string | [법인] 거래고유번호(UNIQUE) |

---

## 1. 상품기본조회 (API ID: v1_국내주식-029)

- **Path**: `/uapi/domestic-stock/v1/quotations/search-info`
- **Method**: GET
- **실전 TR_ID**: CTPF1604R
- **모의 TR_ID**: 모의투자 미지원

### 1.1 Header

위 **공통 Request Header** 참조. tr_id: CTPF1604R.

### 1.2 Request Parameter (Query)

| 이름 | 한글명 | 타입 | 필수 | Length | 설명 |
|------|--------|------|------|--------|------|
| PDNO | 상품번호 | string | Y | 12 | 주식(예: 000660), 선물(예: KR4101SC0009), 미국(예: AAPL) 등 |
| PRDT_TYPE_CD | 상품유형코드 | string | Y | 3 | 300:주식, 301:선물옵션, 302:채권, 512:미국나스닥, 513:미국뉴욕, 529:미국아멕스, 515:일본, 501:홍콩 등 |

### 1.3 Body

GET API이므로 Request Body 없음.

### 1.4 Response

| 필드 | 한글명 | 타입 | 필수 | 설명 |
|------|--------|------|------|------|
| rt_cd | 성공 실패 여부 | string | Y | 0: 성공 |
| msg_cd | 응답코드 | string | Y | 응답코드 |
| msg1 | 응답메세지 | string | Y | 응답메세지 |
| output | 응답상세1 | object | Y | pdno, prdt_type_cd, prdt_name, prdt_name120, prdt_abrv_name, prdt_eng_name, std_pdno, shtn_pdno, prdt_sale_stat_cd, prdt_risk_grad_cd, prdt_clsf_cd, prdt_clsf_name, sale_strt_dt, sale_end_dt, wrap_asst_type_cd, ivst_prdt_type_cd, ivst_prdt_type_cd_name, frst_erlm_dt 등 |

---

## 2. 주식기본조회 (API ID: v1_국내주식-067)

국내주식 종목의 종목상세정보 조회.

- **Path**: `/uapi/domestic-stock/v1/quotations/search-stock-info`
- **Method**: GET
- **실전 TR_ID**: CTPF1002R
- **모의 TR_ID**: 모의투자 미지원

### 2.1 Header

위 **공통 Request Header** 참조. tr_id: CTPF1002R.

### 2.2 Request Parameter (Query)

| 이름 | 한글명 | 타입 | 필수 | Length | 설명 |
|------|--------|------|------|--------|------|
| PRDT_TYPE_CD | 상품유형코드 | string | Y | 3 | 300:주식/ETF/ETN/ELW, 301:선물옵션, 302:채권, 306:ELS |
| PDNO | 상품번호 | string | Y | 12 | 종목번호 6자리. ETN은 Q로 시작(예: Q500001) |

### 2.3 Body

GET API이므로 Request Body 없음.

### 2.4 Response

| 필드 | 한글명 | 타입 | 필수 | 설명 |
|------|--------|------|------|------|
| rt_cd | 성공 실패 여부 | string | Y | 0: 성공 |
| msg_cd | 응답코드 | string | Y | 응답코드 |
| msg1 | 응답메세지 | string | Y | 응답메세지 |
| output | 응답상세1 | object | Y | pdno, prdt_type_cd, mket_id_cd, scty_grp_id_cd, excg_dvsn_cd, setl_mmdd, lstg_stqt, lstg_cptl_amt, cpta, papr, issu_pric, kospi200_item_yn, scts_mket_lstg_dt, scts_mket_lstg_abol_dt, kosdaq_mket_lstg_dt, kosdaq_mket_lstg_abol_dt, frbd_mket_lstg_dt, frbd_mket_lstg_abol_dt, reits_kind_cd, etf_dvsn_cd, oilf_fund_yn, idx_bztp_lcls_cd 등 (시장ID, 증권그룹, 거래소구분, 상장주수, 자본금, 액면가, 상장일자 등) |

---

## 3. 기타 API 목록 (원본 xlsx 시트별 상세 참조)

| API 명(시트명) | Path | 실전 TR_ID | 모의 TR_ID | Method | 비고 |
|---------------|------|------------|------------|--------|------|
| 국내주식 대차대조표 | xlsx 시트 참조 | 시트 참조 | 시트 참조 | GET | Header/Query/Response는 동일 패턴 |
| 국내주식 손익계산서 | xlsx 시트 참조 | 시트 참조 | 시트 참조 | GET | |
| 국내주식 재무비율 | xlsx 시트 참조 | 시트 참조 | 시트 참조 | GET | |
| 국내주식 수익성비율 | xlsx 시트 참조 | 시트 참조 | 시트 참조 | GET | |
| 국내주식 기타주요비율 | xlsx 시트 참조 | 시트 참조 | 시트 참조 | GET | |
| 국내주식 안정성비율 | xlsx 시트 참조 | 시트 참조 | 시트 참조 | GET | |
| 국내주식 성장성비율 | xlsx 시트 참조 | 시트 참조 | 시트 참조 | GET | |
| 국내주식 당사 신용가능종목 | xlsx 시트 참조 | 시트 참조 | 시트 참조 | GET | |
| 예탁원정보(배당일정) | xlsx 시트 참조 | 시트 참조 | 시트 참조 | GET | |
| 예탁원정보(주식매수청구일정) | xlsx 시트 참조 | 시트 참조 | 시트 참조 | GET | |
| 예탁원정보(합병_분할일정) | xlsx 시트 참조 | 시트 참조 | 시트 참조 | GET | |
| 예탁원정보(액면교체일정) | xlsx 시트 참조 | 시트 참조 | 시트 참조 | GET | |
| 예탁원정보(자본감소일정) | xlsx 시트 참조 | 시트 참조 | 시트 참조 | GET | |
| 예탁원정보(상장정보일정) | xlsx 시트 참조 | 시트 참조 | 시트 참조 | GET | |
| 예탁원정보(공모주청약일정) | xlsx 시트 참조 | 시트 참조 | 시트 참조 | GET | |
| 예탁원정보(실권주일정) | xlsx 시트 참조 | 시트 참조 | 시트 참조 | GET | |
| 예탁원정보(의무예치일정) | xlsx 시트 참조 | 시트 참조 | 시트 참조 | GET | |
| 예탁원정보(유상증자일정) | xlsx 시트 참조 | 시트 참조 | 시트 참조 | GET | |
| 예탁원정보(무상증자일정) | xlsx 시트 참조 | 시트 참조 | 시트 참조 | GET | |
| 예탁원정보(주주총회일정) | xlsx 시트 참조 | 시트 참조 | 시트 참조 | GET | |
| 국내주식 종목추정실적 | xlsx 시트 참조 | 시트 참조 | 시트 참조 | GET | |
| 당사 대주가능 종목 | xlsx 시트 참조 | 시트 참조 | 시트 참조 | GET | |
| 국내주식 종목투자의견 | xlsx 시트 참조 | 시트 참조 | 시트 참조 | GET | |
| 국내주식 증권사별 투자의견 | xlsx 시트 참조 | 시트 참조 | 시트 참조 | GET | |

상세 Header·Request Parameter·Body·Response 필드 의미는 원본 xlsx `[국내주식] 종목정보.xlsx` 해당 시트의 Layout(구분/Element/한글명/Type/Required/Length/Description)을 참조하세요.
