package com.investment.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 계좌 정보 DTO
 * 계좌번호는 마스킹하여 반환합니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccountDto {

    /**
     * 계좌 ID
     */
    private String accountId;

    /**
     * 마스킹된 계좌번호 (예: "****1234")
     */
    private String accountNoMasked;

    /**
     * 증권사 코드
     */
    private String brokerType;

    /**
     * 증권사명
     */
    private String brokerTypeName;

    /**
     * 계좌 별칭
     */
    private String accountName;

    /**
     * 메인 계좌 여부
     */
    private Boolean isDefault;

    /**
     * 활성화 여부
     */
    private Boolean isActive;
}
