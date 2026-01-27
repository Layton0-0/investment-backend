package com.investment.api.controller;

import com.investment.setting.dto.TradingSettingDto;
import com.investment.setting.service.TradingSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * 거래 설정 REST API
 */
@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingController {
    
    private final TradingSettingService tradingSettingService;
    
    @GetMapping("/{accountNo}")
    public ResponseEntity<TradingSettingDto> getSetting(
            @PathVariable @NotBlank String accountNo) {
        TradingSettingDto setting = tradingSettingService.getSetting(accountNo);
        return ResponseEntity.ok(setting);
    }
    
    @PutMapping("/{accountNo}")
    public ResponseEntity<TradingSettingDto> updateSetting(
            @PathVariable @NotBlank String accountNo,
            @RequestBody @Valid TradingSettingDto dto) {
        TradingSettingDto setting = tradingSettingService.saveSetting(accountNo, dto);
        return ResponseEntity.ok(setting);
    }
}
