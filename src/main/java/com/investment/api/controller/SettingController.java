package com.investment.api.controller;

import com.investment.auth.service.AuthService;
import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.ops.service.AuditLogService;
import com.investment.setting.dto.SettingsAccountsResponseDto;
import com.investment.setting.dto.SettingsAccountsUpdateRequestDto;
import com.investment.setting.dto.TradingSettingDto;
import com.investment.setting.service.TradingSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * 거래 설정 및 계좌 설정 REST API
 */
@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@Tag(name = "설정", description = "계좌/API 설정·거래 설정 API")
public class SettingController {

    private final TradingSettingService tradingSettingService;
    private final AuthService authService;
    private final AuditLogService auditLogService;

    /**
     * 설정 화면용 계좌 정보 한번에 조회 (모의·실 두 블록)
     */
    @GetMapping("/accounts")
    @Operation(summary = "계좌 설정 조회", description = "모의·실 계좌 API 키·계좌번호 마스킹 정보를 한번에 조회합니다")
    public ResponseEntity<SettingsAccountsResponseDto> getSettingsAccounts(Authentication authentication) {
        String userId = getUserId(authentication);
        SettingsAccountsResponseDto dto = authService.getSettingsAccounts(userId);
        return ResponseEntity.ok(dto);
    }

    /**
     * 설정 화면용 계좌 정보 한번에 수정 (모의·실 두 블록)
     */
    @PutMapping("/accounts")
    @Operation(summary = "계좌 설정 저장", description = "모의·실 계좌 API 키·계좌번호를 한번에 저장합니다. API 키 변경 시 currentPassword 필요")
    public ResponseEntity<SettingsAccountsResponseDto> updateSettingsAccounts(
            Authentication authentication,
            @RequestBody(required = false) SettingsAccountsUpdateRequestDto request) {
        String userId = getUserId(authentication);
        if (request == null) {
            return ResponseEntity.badRequest().build();
        }
        SettingsAccountsResponseDto dto = authService.updateSettingsAccounts(userId, request);
        return ResponseEntity.ok(dto);
    }

    private static String getUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new DomainException(ErrorCode.UNAUTHORIZED, "인증되지 않은 사용자입니다");
        }
        return authentication.getName();
    }

    /** 계좌번호 path (예: 69569325-01). 없으면 204 No Content (404 미발생, 기본값 폼 표시용). */
    @GetMapping("/{accountNo}")
    @Operation(summary = "거래 설정 조회", description = "계좌별 거래 설정. 없으면 204 No Content")
    public ResponseEntity<TradingSettingDto> getSetting(
            @PathVariable("accountNo") @NotBlank String accountNo) {
        return tradingSettingService.getSettingOptional(accountNo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PutMapping("/{accountNo}")
    public ResponseEntity<TradingSettingDto> updateSetting(
            Authentication authentication,
            @PathVariable("accountNo") @NotBlank String accountNo,
            @RequestBody @Valid TradingSettingDto dto) {
        TradingSettingDto setting = tradingSettingService.saveSetting(accountNo, dto);
        String userId = authentication != null ? authentication.getName() : null;
        if (userId != null) {
            auditLogService.record(AuditLogService.EVENT_SETTING_CHANGE, userId, accountNo,
                    "거래 설정 저장", AuditLogService.RESULT_SUCCESS, null);
        }
        return ResponseEntity.ok(setting);
    }
}
