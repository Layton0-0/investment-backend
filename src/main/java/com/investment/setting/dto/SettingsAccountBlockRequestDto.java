package com.investment.setting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 설정 화면 계좌 블록 수정 요청 DTO (모의/실 각 1블록)
 * 변경하지 않을 항목은 null.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettingsAccountBlockRequestDto {

    /**
     * API Key (변경 시에만)
     */
    private String appKey;

    /**
     * API Secret (변경 시에만)
     */
    private String appSecret;

    /**
     * 계좌번호 (변경 시에만)
     */
    private String accountNo;
}
