package com.investment.api.controller;

import com.investment.ops.service.AuditLogService;
import com.investment.setting.service.SystemSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 서버 전역 설정 조회·저장 (ADMIN 전용).
 * DB 우선, 없으면 application.yml fallback.
 */
@Tag(name = "시스템 설정", description = "서버 전역 설정 (ADMIN 전용)")
@RestController
@RequestMapping("/api/v1/system/settings")
@RequiredArgsConstructor
public class SystemSettingController {

    private final SystemSettingService systemSettingService;
    private final AuditLogService auditLogService;

    @Operation(summary = "시스템 설정 목록", description = "허용된 모든 키에 대한 DB 값·적용값 조회")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SystemSettingService.SystemSettingItemDto>> list() {
        return ResponseEntity.ok(systemSettingService.listAll());
    }

    @Operation(summary = "시스템 설정 1건 저장", description = "whitelist 키만 허용. 저장 후 캐시 무효화.")
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SystemSettingService.SystemSettingItemDto> put(@RequestBody Map<String, String> body) {
        String key = body.get("key");
        String value = body.get("value");
        if (key == null || key.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String updatedBy = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : "anonymous";
        systemSettingService.put(key, value != null ? value : "", updatedBy);
        auditLogService.record(AuditLogService.EVENT_SETTING_CHANGE, updatedBy, null,
                "시스템 설정 변경: " + key, AuditLogService.RESULT_SUCCESS, null);
        List<SystemSettingService.SystemSettingItemDto> list = systemSettingService.listAll();
        SystemSettingService.SystemSettingItemDto updated = list.stream()
                .filter(d -> key.equals(d.key()))
                .findFirst()
                .orElse(null);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.ok().build();
    }
}
