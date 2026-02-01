package com.investment.setting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 설정 화면 계좌 한번에 수정 요청 DTO (모의·실 두 블록)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettingsAccountsUpdateRequestDto {

    /**
     * API 키 변경 시 필요한 현재 비밀번호
     */
    private String currentPassword;

    /**
     * 모의계좌(serverType=1) 블록 (변경할 항목만)
     */
    private SettingsAccountBlockRequestDto virtual;

    /**
     * 실계좌(serverType=0) 블록 (변경할 항목만)
     */
    private SettingsAccountBlockRequestDto real;
}
