package com.investment.auth.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 관리자 생성 응답 DTO
 */
@Getter
@Builder
public class CreateAdminUserResponseDto {
    private String userId;
    private String username;
    private String role;
}
