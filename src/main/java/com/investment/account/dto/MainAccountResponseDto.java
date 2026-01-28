package com.investment.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 메인 계좌 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MainAccountResponseDto {

    /**
     * 계좌 ID
     */
    private String accountId;

    /**
     * 계좌번호 (복호화된 값, API 호출에 사용)
     */
    private String accountNo;

    /**
     * 마스킹된 계좌번호 (표시용)
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
}
