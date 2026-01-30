package com.investment.auth.dto;

import com.investment.common.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 회원가입 요청 DTO
 */
@Getter
@Setter
public class SignupRequestDto {

    @NotBlank(message = "사용자 ID는 필수입니다")
    @Size(min = 4, max = 50, message = "사용자 ID는 4자 이상 50자 이하여야 합니다")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "사용자 ID는 영문, 숫자, 언더스코어만 사용할 수 있습니다")
    private String username;

    @NotBlank(message = "비밀번호는 필수입니다")
    @ValidPassword
    private String password;

    @NotBlank(message = "사용증권명은 필수입니다")
    private String brokerType; // BrokerType enum의 code 값

    @NotBlank(message = "API Key는 필수입니다")
    private String appKey;

    @NotBlank(message = "API Secret은 필수입니다")
    private String appSecret;

    private String serverType = "1"; // 기본값: 모의투자 ("1": 모의투자, "0": 실거래)

    /**
     * 계좌번호 (필수)
     * 형식: "숫자8자리-숫자2자리" (예: "12345678-12")
     * - 앞 8자리: CANO (계좌번호)
     * - 뒤 2자리: ACNT_PRDT_CD (계좌상품코드)
     */
    @NotBlank(message = "계좌번호는 필수입니다")
    @Pattern(regexp = "^\\d{8}-\\d{2}$", message = "계좌번호 형식이 올바르지 않습니다. 형식: 숫자8자리-숫자2자리 (예: 12345678-12)")
    private String accountNo;

    /**
     * 계좌인증 시 미리 발급받은 한국투자증권 접근 토큰.
     * 값이 있으면 회원가입 시 재발급하지 않고 이 토큰을 저장합니다. (한국투자증권만 해당)
     */
    private String preIssuedAccessToken;
}
