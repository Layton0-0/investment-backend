package com.investment.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * 계좌인증 요청 DTO (회원가입 전 API Key·계좌번호 유효성 검증)
 */
@Getter
@Setter
public class AccountVerifyRequestDto {

    /**
     * 증권사 구분 (BrokerType code, 예: KOREA_INVESTMENT).
     * 현재 계좌인증은 한국투자증권만 지원하며, 향후 확장 시 사용.
     */
    private String brokerType;

    @NotBlank(message = "API Key는 필수입니다")
    private String appKey;

    @NotBlank(message = "API Secret은 필수입니다")
    private String appSecret;

    /**
     * 서버 타입 ("1": 모의투자, "0": 실거래)
     */
    @NotBlank(message = "서버 타입은 필수입니다")
    @Pattern(regexp = "^[01]$", message = "서버 타입은 0(실거래) 또는 1(모의투자)이어야 합니다")
    private String serverType;

    @NotBlank(message = "계좌번호는 필수입니다")
    @Pattern(regexp = "^\\d{8}-\\d{2}$", message = "계좌번호 형식이 올바르지 않습니다. 형식: 숫자8자리-숫자2자리 (예: 12345678-12)")
    private String accountNo;
}
