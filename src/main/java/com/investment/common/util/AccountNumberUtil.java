package com.investment.common.util;

import java.util.regex.Pattern;

/**
 * 한국투자증권 계좌번호 유틸리티 클래스
 * 
 * 계좌번호 형식: "숫자8자리-숫자2자리" (예: "12345678-12")
 * - CANO: 앞 8자리 숫자 (계좌번호)
 * - ACNT_PRDT_CD: 뒤 2자리 숫자 (계좌상품코드)
 */
public final class AccountNumberUtil {

    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("^\\d{8}-\\d{2}$");

    private AccountNumberUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 계좌번호 형식 검증
     * 
     * @param accountNo 계좌번호 (예: "12345678-12")
     * @return 형식이 올바르면 true
     */
    public static boolean validateAccountNumberFormat(String accountNo) {
        if (accountNo == null || accountNo.trim().isEmpty()) {
            return false;
        }
        return ACCOUNT_NUMBER_PATTERN.matcher(accountNo.trim()).matches();
    }

    /**
     * 계좌번호를 CANO와 ACNT_PRDT_CD로 분리
     * 
     * @param accountNo 계좌번호 (예: "12345678-12")
     * @return AccountNumberParts 객체 (cano: "12345678", acntPrdtCd: "12")
     * @throws IllegalArgumentException 계좌번호 형식이 올바르지 않은 경우
     */
    public static AccountNumberParts parseAccountNumber(String accountNo) {
        if (accountNo == null || accountNo.trim().isEmpty()) {
            throw new IllegalArgumentException("계좌번호가 비어있습니다");
        }

        String trimmed = accountNo.trim();

        if (!validateAccountNumberFormat(trimmed)) {
            throw new IllegalArgumentException(
                    String.format("계좌번호 형식이 올바르지 않습니다. 형식: 숫자8자리-숫자2자리 (예: 12345678-12), 입력값: %s", trimmed));
        }

        String[] parts = trimmed.split("-");
        if (parts.length != 2) {
            throw new IllegalArgumentException(
                    String.format("계좌번호 형식이 올바르지 않습니다. 형식: 숫자8자리-숫자2자리 (예: 12345678-12), 입력값: %s", trimmed));
        }

        String cano = parts[0];
        String acntPrdtCd = parts[1];

        if (cano.length() != 8 || acntPrdtCd.length() != 2) {
            throw new IllegalArgumentException(
                    String.format("계좌번호 형식이 올바르지 않습니다. CANO는 8자리, ACNT_PRDT_CD는 2자리여야 합니다. 입력값: %s", trimmed));
        }

        return new AccountNumberParts(cano, acntPrdtCd);
    }

    /**
     * CANO와 ACNT_PRDT_CD를 계좌번호 형식으로 조합
     * 
     * @param cano       계좌번호 (8자리)
     * @param acntPrdtCd 계좌상품코드 (2자리)
     * @return 계좌번호 (예: "12345678-12")
     * @throws IllegalArgumentException 파라미터가 올바르지 않은 경우
     */
    public static String formatAccountNumber(String cano, String acntPrdtCd) {
        if (cano == null || cano.trim().isEmpty()) {
            throw new IllegalArgumentException("CANO가 비어있습니다");
        }
        if (acntPrdtCd == null || acntPrdtCd.trim().isEmpty()) {
            throw new IllegalArgumentException("ACNT_PRDT_CD가 비어있습니다");
        }

        String trimmedCano = cano.trim();
        String trimmedAcntPrdtCd = acntPrdtCd.trim();

        if (!trimmedCano.matches("^\\d{8}$")) {
            throw new IllegalArgumentException(
                    String.format("CANO는 8자리 숫자여야 합니다. 입력값: %s", trimmedCano));
        }
        if (!trimmedAcntPrdtCd.matches("^\\d{2}$")) {
            throw new IllegalArgumentException(
                    String.format("ACNT_PRDT_CD는 2자리 숫자여야 합니다. 입력값: %s", trimmedAcntPrdtCd));
        }

        return trimmedCano + "-" + trimmedAcntPrdtCd;
    }

    /**
     * 계좌번호 파싱 결과를 담는 내부 클래스
     */
    public static class AccountNumberParts {
        private final String cano;
        private final String acntPrdtCd;

        public AccountNumberParts(String cano, String acntPrdtCd) {
            this.cano = cano;
            this.acntPrdtCd = acntPrdtCd;
        }

        public String getCano() {
            return cano;
        }

        public String getAcntPrdtCd() {
            return acntPrdtCd;
        }
    }
}
