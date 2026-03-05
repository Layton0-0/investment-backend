# [국내주식] 주문·계좌 API 세부 명세

원본: `[국내주식] 주문_계좌.xlsx`

## 공통 Request Header (주문/계좌 API)

| 항목 | 필수 | 타입 | 설명 |
|------|------|------|------|
| content-type | Y/N | string | application/json; charset=utf-8 |
| authorization | Y | string | Bearer {access_token} (OAuth 접근토큰) |
| appkey | Y | string | 앱키 (절대 노출 금지) |
| appsecret | Y | string | 앱시크릿키 (절대 노출 금지) |
| tr_id | Y | string | 각 API별 실전/모의 TR_ID |
| custtype | Y/N | string | B: 법인, P: 개인 |
| personalseckey | N | string | [법인 필수] 고객식별키 |
| tr_cont | N | string | 연속 거래 여부 (다음 페이지 조회 시 N 등) |
| seq_no | N | string | [법인 필수] 001 |
| mac_address | N | string | 맥주소 |
| phone_number | N | string | [법인] 핸드폰번호 (하이픈 제거) |
| ip_addr | N | string | [법인] 공인 IP |
| gt_uid | N | string | [법인] 거래고유번호(UNIQUE) |

---

## 1. 주식잔고조회 (API ID: v1_국내주식-006)

주식 잔고 조회. 실전 최대 50건/회, 모의 최대 20건/회. 연속조회 가능. 단순 주문 준비는 매수/매도가능 조회 권장.

- **Path**: `/uapi/domestic-stock/v1/trading/inquire-balance`
- **Method**: GET
- **실전 TR_ID**: TTTC8434R
- **모의 TR_ID**: VTTC8434R

### 1.1 Header

위 **공통 Request Header** 참조. tr_id: TTTC8434R(실전) / VTTC8434R(모의).

### 1.2 Request Parameter (Query)

| 이름 | 한글명 | 타입 | 필수 | 설명 |
|------|--------|------|------|------|
| CANO | 종합계좌번호 | string | Y | 계좌번호 앞 8자리 |
| ACNT_PRDT_CD | 계좌상품코드 | string | Y | 계좌번호 뒤 2자리 |
| AFHR_FLPR_YN | 시간외단일가/거래소여부 | string | Y | N: 기본, Y: 시간외단일가, X: NXT 정규장 |
| OFL_YN | 오프라인여부 | string | N | 공란(Default) |
| INQR_DVSN | 조회구분 | string | Y | 01: 대출일별, 02: 종목별 |
| UNPR_DVSN | 단가구분 | string | Y | 01: 기본값 |
| FUND_STTL_ICLD_YN | 펀드결제분포함여부 | string | Y | N/Y |
| FNCG_AMT_AUTO_RDPT_YN | 융자금액자동상환여부 | string | Y | N: 기본값 |
| PRCS_DVSN | 처리구분 | string | Y | 00: 전일매매포함, 01: 전일매매미포함 |
| CTX_AREA_FK100 | 연속조회검색조건100 | string | N | 공란: 최초, 이전 output 값: 다음 페이지 |
| CTX_AREA_NK100 | 연속조회키100 | string | N | 공란: 최초, 이전 output 값: 다음 페이지 |

### 1.3 Body

GET API이므로 없음.

### 1.4 Response

| 필드 | 한글명 | 타입 | 설명 |
|------|--------|------|------|
| rt_cd | 성공 실패 여부 | string | 0: 성공 |
| msg_cd | 응답코드 | string | |
| msg1 | 응답메세지 | string | |
| ctx_area_fk100 | 연속조회검색조건100 | string | 다음 조회 시 Query에 사용 |
| ctx_area_nk100 | 연속조회키100 | string | 다음 조회 시 Query에 사용 |
| output1 | 응답상세1 | array | pdno, prdt_name, trad_dvsn_name, bfdy_buy_qty, bfdy_sll_qty, thdt_buyqty, thdt_sll_qty, hldg_qty, ord_psbl_qty, pchs_avg_pric, pchs_amt, prpr, evlu_amt, evlu_pfls_amt, evlu_pfls_rt, loan_dt, loan_amt, stln_slng_chgs, expd_dt, fltt_rt, bfdy_cprs_icdc, item_mgna_rt_name, grta_rt_name, sbst_pric, stck_loan_unpr 등 |
| output2 | 응답상세2 | array | dnca_tot_amt(예수금), nxdy_excc_amt(D+1 예수금), prvs_rcdl_excc_amt(D+2 예수금), cma_evlu_amt 등 |

---

## 2. 주식주문(현금) (API ID: v1_국내주식-001)

국내주식 현금 매수/매도. POST Body key는 대문자(CANO, ACNT_PRDT_CD 등). ORD_QTY·ORD_UNPR 등은 String 전달.

- **Path**: `/uapi/domestic-stock/v1/trading/order-cash`
- **Method**: POST
- **실전 TR_ID**: 매도 TTTC0011U, 매수 TTTC0012U
- **모의 TR_ID**: 매도 VTTC0011U, 매수 VTTC0012U

### 2.1 Header

위 **공통 Request Header** 참조. authorization에 "Bearer " 접두사 필수.

### 2.2 Request Parameter

POST이므로 Query 없음.

### 2.3 Body

| 필드 | 한글명 | 타입 | 필수 | 설명 |
|------|--------|------|------|------|
| CANO | 종합계좌번호 | string | Y | 8자리 |
| ACNT_PRDT_CD | 계좌상품코드 | string | Y | 2자리 |
| PDNO | 상품번호 | string | Y | 종목코드 6자리(ETN 7자리) |
| SLL_TYPE | 매도유형(매도 시) | string | N | 01:일반매도, 02:임의매매, 05:대차매도. 미입력 시 01 |
| ORD_DVSN | 주문구분 | string | Y | 00:지정가, 01:시장가, 02:조건부지정가, 03:최유리지정가, 04:최우선지정가, 05~07:시간외, 11~16:IOC/FOK 등 (KRX/NXT/SOR별 코드 상이) |
| ORD_QTY | 주문수량 | string | Y | 주문수량 |
| ORD_UNPR | 주문단가 | string | Y | 주문단가. 시장가 등은 "0" |
| CNDT_PRIC | 조건가격 | string | N | 스탑지정가(ORD_DVSN 22) 시 필수 |
| EXCG_ID_DVSN_CD | 거래소ID구분코드 | string | N | KRX/NXT/SOR. 미입력 시 KRX(모의는 KRX만) |

### 2.4 Response

| 필드 | 한글명 | 타입 | 설명 |
|------|--------|------|------|
| rt_cd | 성공 실패 여부 | string | 0: 성공 |
| msg_cd | 응답코드 | string | 예: APBK0013 |
| msg1 | 응답메세지 | string | 예: "주문 전송 완료 되었습니다." |
| output | 응답상세 | object | KRX_FWDG_ORD_ORGNO(거래소코드), ODNO(주문번호), ORD_TMD(주문시간) |

---

## 3. 매수가능조회 (API ID: v1_국내주식-007)

종목별 매수가능 금액/수량. 1건/회. 매수가능금액: 미수X → nrcvb_buy_amt, 미수O → max_buy_amt. 매수가능수량: 전량매수 가능수량은 ORD_DVSN 01(시장가)로 조회 권장.

- **Path**: `/uapi/domestic-stock/v1/trading/inquire-psbl-order`
- **Method**: GET
- **실전 TR_ID**: TTTC8908R
- **모의 TR_ID**: VTTC8908R

### 3.1 Header

공통 Request Header. tr_id: TTTC8908R / VTTC8908R.

### 3.2 Request Parameter (Query)

| 이름 | 한글명 | 타입 | 필수 | 설명 |
|------|--------|------|------|------|
| CANO | 종합계좌번호 | string | Y | 8자리 |
| ACNT_PRDT_CD | 계좌상품코드 | string | Y | 2자리 |
| PDNO | 상품번호 | string | Y | 6자리. PDNO·ORD_UNPR 공란 시 매수금액만 조회 |
| ORD_UNPR | 주문단가 | string | Y | 1주당 가격. 시장가 조회 시 공란 |
| ORD_DVSN | 주문구분 | string | Y | 00:지정가, 01:시장가 등. 전량매수 가능수량은 01 권장 |
| CMA_EVLU_AMT_ICLD_YN | CMA평가금액포함여부 | string | Y | Y/N |
| OVRS_ICLD_YN | 해외포함여부 | string | Y | Y/N |

### 3.3 Body

없음.

### 3.4 Response

| 필드 | 한글명 | 타입 | 설명 |
|------|--------|------|------|
| rt_cd, msg_cd, msg1 | 성공/코드/메시지 | string | |
| output | 응답상세 | object | ord_psbl_cash(주문가능현금), ord_psbl_sbst, ruse_psbl_amt, nrcvb_buy_amt(미수없는매수금액), nrcvb_buy_qty(미수없는매수수량), max_buy_amt(최대매수금액), max_buy_qty(최대매수수량), cma_evlu_amt 등 |

---

## 4. 매도가능수량조회 (API ID: 국내주식-165)

특정 종목 매도가능수량. output.ord_psbl_qty 확인.

- **Path**: `/uapi/domestic-stock/v1/trading/inquire-psbl-sell`
- **Method**: GET
- **실전 TR_ID**: TTTC8408R
- **모의 TR_ID**: 모의투자 미지원

### 4.1 Header

공통 Request Header. tr_id: TTTC8408R.

### 4.2 Request Parameter (Query)

| 이름 | 한글명 | 타입 | 필수 | 설명 |
|------|--------|------|------|------|
| CANO | 종합계좌번호 | string | Y | 8자리 |
| ACNT_PRDT_CD | 계좌상품코드 | string | Y | 2자리 |
| PDNO | 종목번호 | string | Y | 보유종목 코드(예: 000660) |

### 4.3 Body

없음.

### 4.4 Response

| 필드 | 한글명 | 타입 | 설명 |
|------|--------|------|------|
| rt_cd, msg_cd, msg1 | 성공/코드/메시지 | string | |
| output1 | 응답상세 | object | pdno, prdt_name, buy_qty, sll_qty, cblc_qty, nsvg_qty, ord_psbl_qty(주문가능수량), pchs_avg_pric, pchs_amt, now_pric, evlu_amt, evlu_pfls_amt, evlu_pfls_rt |

---

## 5. 주식일별주문체결조회 (API ID: v1_국내주식-005)

일별 주문·체결 내역. 3개월 이내/이전 TR_ID 상이. 실전 100건/회, 모의 15건/회. 3개월 이전은 장종료 후·기간 짧게 조회 권장.

- **Path**: `/uapi/domestic-stock/v1/trading/inquire-daily-ccld`
- **Method**: GET
- **실전 TR_ID**: 3개월 이내 TTTC0081R, 3개월 이전 CTSC9215R
- **모의 TR_ID**: 3개월 이내 VTTC0081R, 3개월 이전 VTSC9215R

### 5.1 Header

공통 Request Header. tr_id: 기간에 따라 위 TR_ID 사용.

### 5.2 Request Parameter (Query)

| 이름 | 한글명 | 타입 | 필수 | 설명 |
|------|--------|------|------|------|
| CANO | 종합계좌번호 | string | Y | 8자리 |
| ACNT_PRDT_CD | 계좌상품코드 | string | Y | 2자리 |
| INQR_STRT_DT | 조회시작일자 | string | Y | YYYYMMDD |
| INQR_END_DT | 조회종료일자 | string | Y | YYYYMMDD |
| SLL_BUY_DVSN_CD | 매도매수구분코드 | string | Y | 00:전체, 01:매도, 02:매수 |
| PDNO | 상품번호 | string | N | 6자리 |
| ORD_GNO_BRNO | 주문채번지점번호 | string | Y | 영업점코드 5자리 |
| ODNO | 주문번호 | string | N | 10자리 |
| CCLD_DVSN | 체결구분 | string | Y | 00:전체, 01:체결, 02:미체결 |
| INQR_DVSN | 조회구분 | string | Y | 00:역순, 01:정순 |
| INQR_DVSN_1 | 조회구분1 | string | Y | 없음:전체, 1:ELW, 2:프리보드 |
| INQR_DVSN_3 | 조회구분3 | string | Y | 00:전체, 01:현금, 02:신용 등 |
| EXCG_ID_DVSN_CD | 거래소ID구분코드 | string | Y | KRX/NXT/SOR/ALL |
| CTX_AREA_FK100 | 연속조회검색조건100 | string | Y | 공란:최초, 이전 output:다음 |
| CTX_AREA_NK100 | 연속조회키100 | string | Y | 공란:최초, 이전 output:다음 |

### 5.3 Body

없음.

### 5.4 Response

| 필드 | 한글명 | 타입 | 설명 |
|------|--------|------|------|
| rt_cd, msg_cd, msg1 | 성공/코드/메시지 | string | |
| output1 | 응답상세 | array | ord_dt, ord_gno_brno, odno, orgn_odno, ord_dvsn_name, sll_buy_dvsn_cd, pdno, prdt_name, ord_qty, ord_unpr, ord_tmd, tot_ccld_qty, avg_prvs, cncl_yn, tot_ccld_amt, loan_dt, ordr_empno, ord_dvsn_cd, cnc_cfrm_qty, rmn_qty, rjct_qty, ccld_cndt_name, inqr_ip_addr 등 |

---

## 6. 기간별손익일별합산조회 (API ID: v1_국내주식-052)

기간별 매매손익 일별 합산. HTS [0856] 기간별 매매손익 '일별' 화면 대응.

- **Path**: `/uapi/domestic-stock/v1/trading/inquire-period-profit-loss`
- **Method**: GET
- **실전 TR_ID**: TTTC8708R
- **모의 TR_ID**: 모의투자 미지원(xlsx 기준). 구현: 모의(serverType=1)일 때 API 호출하지 않고 `API_NOT_SUPPORTED` 예외 반환. 실전만 TTTC8708R로 호출.

### 6.1 Header

공통 Request Header. tr_id: TTTC8708R.

### 6.2 Request Parameter (Query)

| 이름 | 한글명 | 타입 | 필수 | 설명 |
|------|--------|------|------|------|
| ACNT_PRDT_CD | 계좌상품코드 | string | Y | 2자리 |
| CANO | 종합계좌번호 | string | Y | 8자리 |
| INQR_STRT_DT | 조회시작일자 | string | Y | YYYYMMDD |
| INQR_END_DT | 조회종료일자 | string | Y | YYYYMMDD |
| PDNO | 상품번호 | string | Y | 공란: 전체 |
| SORT_DVSN | 정렬구분 | string | Y | 00:최근순, 01:과거순, 02:최근순 |
| INQR_DVSN | 조회구분 | string | Y | 00 |
| CBLC_DVSN | 잔고구분 | string | Y | 00:전체 |
| CTX_AREA_FK100 / CTX_AREA_NK100 | 연속조회 | string | Y | 공란:최초, 이전 output:다음 |

### 6.3 Body

없음.

### 6.4 Response

| 필드 | 한글명 | 타입 | 설명 |
|------|--------|------|------|
| rt_cd, msg_cd, msg1 | 성공/코드/메시지 | string | |
| output1 | 응답상세1 | array | trad_dt(매매일자), buy_amt, sll_amt, rlzt_pfls(실현손익), fee, loan_int, tl_tax, pfls_rt, sll_qty1, buy_qty1 |
| output2 | 응답상세2 | object | sll_qty_smtl, sll_tr_amt_smtl, sll_fee_smtl, sll_tltx_smtl, sll_excc_amt_smtl, buy_qty_smtl, buy_tr_amt_smtl, buy_fee_smtl, buy_tax_smtl, buy_excc_amt_smtl, tot_qty, tot_tr_amt, tot_fee, tot_tltx, tot_excc_amt, tot_rlzt_pfls(총실현손익), loan_int |

---

## 7. 기타 API 목록 (원본 xlsx 시트 참조)

| API 명(시트명) | Path | Method | 비고 |
|---------------|------|--------|------|
| 주식주문(신용) | xlsx 시트 참조 | POST | |
| 주식주문(정정취소) | xlsx 시트 참조 | POST | |
| 주식정정취소가능주문조회 | xlsx 시트 참조 | GET | |
| 신용매수가능조회 | xlsx 시트 참조 | GET | |
| 주식예약주문 / 예약주문정정취소 / 예약주문조회 | xlsx 시트 참조 | POST/GET | |
| 퇴직연금(체결기준잔고/미체결/매수가능/예수금/잔고) | xlsx 시트 참조 | GET | |
| 주식잔고조회_실현손익 | xlsx 시트 참조 | GET | |
| 투자계좌자산현황조회 | xlsx 시트 참조 | GET | 구현: 09 가이드 참조(CTRP6548R) |
| 기간별매매손익현황조회 | xlsx 시트 참조 | GET | |
| 주식통합증거금 현황 | xlsx 시트 참조 | GET | |
| 기간별계좌권리현황조회 | xlsx 시트 참조 | GET | |

상세 Header·Parameter·Body·Response는 원본 xlsx `[국내주식] 주문_계좌.xlsx` 해당 시트 Layout을 참조하세요.
