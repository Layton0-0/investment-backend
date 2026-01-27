package com.investment.auth.dto;

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
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다")
    private String password;
    
    @NotBlank(message = "사용증권명은 필수입니다")
    private String brokerType; // BrokerType enum의 code 값
    
    @NotBlank(message = "API Key는 필수입니다")
    private String appKey;
    
    @NotBlank(message = "API Secret은 필수입니다")
    private String appSecret;
    
    private String serverType = "1"; // 기본값: 모의투자 ("1": 모의투자, "0": 실거래)
}
