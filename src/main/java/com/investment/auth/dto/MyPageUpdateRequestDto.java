package com.investment.auth.dto;

import com.investment.common.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 마이페이지 수정 요청 DTO
 * 
 * 비밀번호 변경은 별도 API로 분리하는 것이 좋지만,
 * 여기서는 간단하게 함께 처리합니다.
 */
@Getter
@Setter
public class MyPageUpdateRequestDto {
    
    /**
     * 현재 비밀번호 (비밀번호 변경 또는 API 키 변경 시 필수)
     */
    private String currentPassword;
    
    /**
     * 새 비밀번호 (변경하지 않으면 null)
     */
    @ValidPassword
    private String password;
    
    /**
     * 사용증권명 (변경하지 않으면 null)
     */
    private String brokerType;
    
    /**
     * API Key (변경하지 않으면 null)
     */
    private String appKey;
    
    /**
     * API Secret (변경하지 않으면 null)
     */
    private String appSecret;
    
    /**
     * 서버 타입 (변경하지 않으면 null)
     */
    private String serverType;
}
