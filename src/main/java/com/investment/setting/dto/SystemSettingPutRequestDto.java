package com.investment.setting.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 시스템 설정 1건 수정 요청.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettingPutRequestDto {

    @NotBlank(message = "키는 필수입니다")
    private String key;

    /** 문자열로 전달. Boolean: "true"/"false", BigDecimal: 숫자 문자열 */
    private String value;
}
