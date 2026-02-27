package com.investment.api.controller;

import com.investment.ops.service.AuditLogService;
import com.investment.setting.dto.SystemSettingPutRequestDto;
import com.investment.setting.service.SystemSettingService;
import com.investment.setting.service.SystemSettingService.SystemSettingItemDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 서버 전역(시스템) 설정 API. ADMIN 전용.
 * DB 값 우선, 없으면 application.yml fallback.
 */
@Tag(name = "시스템 설정", description = "서버 전역 설정 조회·수정 (ADMIN)")
@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class SystemSettingController {

    private final SystemSettingService systemSettingService;
    private final AuditLogService auditLogService;

    @Operation(summary = "시스템 설정 목록 조회", description = "허용된 모든 키에 대한 DB/effective 값 반환. ADMIN 전용.")
    @GetMapping("/settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SystemSettingItemDto>> getSettings() {
        List<SystemSettingItemDto> list = systemSettingService.listAll();
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "시스템 설정 1건 수정", description = "키·값 저장. whitelist 키만 허용. ADMIN 전용.")
    @PutMapping("/settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SystemSettingItemDto> putSetting(
            Authentication authentication,
            @RequestBody @Valid SystemSettingPutRequestDto request) {
        String userId = authentication != null ? authentication.getName() : null;
        systemSettingService.put(request.getKey(), request.getValue(), userId);
        auditLogService.record(AuditLogService.EVENT_SETTING_CHANGE, userId, null,
                "시스템 설정 변경: " + request.getKey() + "=" + request.getValue(),
                AuditLogService.RESULT_SUCCESS, null);
        List<SystemSettingItemDto> list = systemSettingService.listAll();
        SystemSettingItemDto updated = list.stream()
                .filter(d -> d.key().equals(request.getKey()))
                .findFirst()
                .orElseThrow();
        return ResponseEntity.ok(updated);
    }
}
