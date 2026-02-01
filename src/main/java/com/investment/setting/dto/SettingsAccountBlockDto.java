package com.investment.setting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 설정 화면용 계좌 블록 DTO (모의/실 각 1블록)
 * API 키·계좌번호는 마스킹하여 반환.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettingsAccountBlockDto {

    /**
     * API Key 마스킹 (앞 4자리만)
     */
    private String appKeyMasked;

    /**
     * API Secret 마스킹
     */
    private String appSecretMasked;

    /**
     * 계좌번호 마스킹 (뒤 4자리만)
     */
    private String accountNoMasked;

    /**
     * 해당 서버타입에 API 키가 등록되어 있는지
     */
    private boolean hasApiKey;
}
