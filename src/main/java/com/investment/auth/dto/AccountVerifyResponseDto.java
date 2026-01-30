package com.investment.auth.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 계좌인증 응답 DTO
 * success 시 accessToken을 반환하여 회원가입 시 재발급 없이 사용할 수 있도록 함.
 */
@Getter
@Builder
public class AccountVerifyResponseDto {

    private boolean success;
    private String message;
    /** 계좌인증 시 발급된 한국투자증권 접근 토큰. 회원가입 시 전달하여 재발급을 방지. 성공 시에만 설정. */
    private String accessToken;
}
