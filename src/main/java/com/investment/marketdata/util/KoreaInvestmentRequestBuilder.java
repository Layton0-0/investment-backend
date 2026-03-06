package com.investment.marketdata.util;

import com.investment.common.util.AccountNumberUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

/**
 * 한국투자증권 API 요청 생성 유틸리티 클래스
 * 
 * 한국투자증권 Open API 호출 시 공통으로 사용되는 헤더 및 requestBody를 생성합니다.
 * 
 * 공통 필수 헤더:
 * - authorization: Bearer {access_token}
 * - appkey: {app_key} (Required='Y')
 * - appsecret: {app_secret} (Required='Y')
 * - tr_id: {tr_id}
 * - Content-Type: application/json
 * 
 * 공통 requestBody 파라미터:
 * - 계좌 관련 API: CANO (계좌번호), ACNT_PRDT_CD (계좌상품코드, 기본값: "01")
 */
public final class KoreaInvestmentRequestBuilder {

    private KoreaInvestmentRequestBuilder() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 공통 헤더 생성 (한국투자증권 REST API 공식 명세 준수).
     * <p>헤더명: authorization(Bearer 토큰), appkey, appsecret, tr_id.
     * 토큰 발급 API Body의 secretkey와 구분 — REST 호출 시 헤더는 {@code appsecret} 사용.</p>
     *
     * @param accessToken OAuth 2.0 Access Token (Bearer 뒤에 붙일 값)
     * @param appKey      App Key (Required='Y')
     * @param appSecret   App Secret (Required='Y', 헤더명 appsecret)
     * @param trId        거래 ID (API별로 고유한 값)
     * @return 공통 헤더가 설정된 HttpHeaders
     */
    public static HttpHeaders createCommonHeaders(String accessToken, String appKey,
            String appSecret, String trId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("authorization", "Bearer " + accessToken);
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", trId);
        return headers;
    }

    /**
     * 계좌 관련 API 공통 파라미터 Map 생성
     * 
     * 계좌 관련 조회 API(주식잔고조회, 매수가능조회, 매도가능수량조회, 주문체결조회 등)는
     * 한국투자증권 스펙상 GET 메서드 + query parameter로 호출합니다.
     * 이 메서드가 반환한 Map은 요청 body가 아니라 URI query parameter로 사용합니다.
     * 
     * 공통 파라미터:
     * - CANO: 계좌번호 (8자리)
     * - ACNT_PRDT_CD: 계좌상품코드 (2자리)
     * 
     * 계좌번호 형식: "숫자8자리-숫자2자리" (예: "12345678-12")
     * - CANO: 앞 8자리 숫자
     * - ACNT_PRDT_CD: 뒤 2자리 숫자
     * 
     * @param accountNo        계좌번호 (형식: "12345678-12")
     * @param additionalParams API별 고유 파라미터 (추가/수정할 파라미터)
     * @return 공통 파라미터와 추가 파라미터가 포함된 Map (조회 API는 query parameter로 사용)
     * @throws IllegalArgumentException 계좌번호 형식이 올바르지 않은 경우
     */
    public static Map<String, String> createAccountRequestBody(String accountNo,
            Map<String, String> additionalParams) {
        Map<String, String> requestBody = new HashMap<>();

        // 계좌번호 파싱 (CANO와 ACNT_PRDT_CD로 분리). 빈 값이면 한투 API가 OPSQ2001(INPUT_FIELD_NAME CANO) 반환하므로 반드시 유효한 값 필요.
        if (accountNo == null || accountNo.trim().isEmpty()) {
            throw new IllegalArgumentException("계좌번호가 비어있습니다. CANO/ACNT_PRDT_CD 필수입니다.");
        }
        AccountNumberUtil.AccountNumberParts parts = AccountNumberUtil.parseAccountNumber(accountNo);
        requestBody.put("CANO", parts.getCano());
        requestBody.put("ACNT_PRDT_CD", parts.getAcntPrdtCd());

        // 추가/수정 파라미터 병합 (additionalParams가 null이 아닌 경우)
        if (additionalParams != null) {
            requestBody.putAll(additionalParams);
        }

        return requestBody;
    }

    /**
     * 계좌 관련 API 공통 파라미터 Map 생성 (추가 파라미터 없음)
     * 
     * @param accountNo 계좌번호
     * @return 공통 파라미터만 포함된 Map (조회 API는 query parameter로 사용)
     */
    public static Map<String, String> createAccountRequestBody(String accountNo) {
        return createAccountRequestBody(accountNo, null);
    }

    /**
     * 시세 관련 API 파라미터 Map 생성
     * 
     * 시세 관련 조회 API(차트 조회, 현재가 조회 등)는 한국투자증권 스펙상
     * GET 메서드 + query parameter로 호출합니다.
     * 이 메서드가 반환한 Map은 URI query parameter로 사용합니다.
     * 
     * @param params API별 파라미터 맵
     * @return 파라미터가 포함된 Map (조회 API는 query parameter로 사용)
     */
    public static Map<String, String> createMarketDataRequestBody(Map<String, String> params) {
        Map<String, String> requestBody = new HashMap<>();

        if (params != null) {
            requestBody.putAll(params);
        }

        return requestBody;
    }
}
