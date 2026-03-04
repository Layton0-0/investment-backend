package com.investment.setting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 원클릭 자동투자 시작 응답.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuickStartResponseDto {

    private boolean success;
    private String message;
    private TradingSettingDto setting;
}
