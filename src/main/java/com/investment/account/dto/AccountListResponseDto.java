package com.investment.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 계좌 목록 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountListResponseDto {

    /**
     * 계좌 목록
     */
    private List<UserAccountDto> accounts;

    /**
     * 메인 계좌 ID (있는 경우)
     */
    private String mainAccountId;

    /**
     * 총 계좌 수
     */
    private Integer totalCount;
}
