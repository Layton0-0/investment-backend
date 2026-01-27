package com.investment.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 마이페이지 응답 DTO
 * 
 * 보안을 위해 API Key와 Secret은 마스킹하여 반환합니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyPageResponseDto {
    
    private String userId;
    private String username;
    private String brokerType;
    private String brokerTypeName;
    
    /**
     * API Key 마스킹 (앞 4자리만 표시)
     * 예: "PSncYe4KU3cSzFZvTW71dmuHvHFfeyhqP3QM" -> "PSnc****************"
     */
    private String appKeyMasked;
    
    /**
     * API Secret 마스킹 (앞 4자리만 표시)
     */
    private String appSecretMasked;
    
    private String serverType;
    private String serverTypeName; // "모의투자" 또는 "실거래"
}
