package com.investment.setting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 설정 화면 계좌 한번에 조회 응답 DTO (모의·실 두 블록)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettingsAccountsResponseDto {

    /**
     * 모의계좌(serverType=1) 블록
     */
    private SettingsAccountBlockDto virtual;

    /**
     * 실계좌(serverType=0) 블록
     */
    private SettingsAccountBlockDto real;
}
