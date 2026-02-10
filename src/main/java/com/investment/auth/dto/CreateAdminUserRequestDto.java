package com.investment.auth.dto;

import com.investment.common.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 관리자 생성 요청 DTO (ADMIN 전용 API)
 */
@Getter
@Setter
public class CreateAdminUserRequestDto {

    @NotBlank(message = "사용자 ID는 필수입니다")
    @Size(min = 4, max = 50, message = "사용자 ID는 4자 이상 50자 이하여야 합니다")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "사용자 ID는 영문, 숫자, 언더스코어만 사용할 수 있습니다")
    private String username;

    @NotBlank(message = "비밀번호는 필수입니다")
    @ValidPassword
    private String password;

    @NotBlank(message = "역할은 필수입니다")
    @Pattern(regexp = "^(Admin)$", message = "역할은 Admin만 가능합니다")
    private String role;
}
