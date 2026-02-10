package com.investment.api.controller;

import com.investment.ops.dto.AuditLogListResponseDto;
import com.investment.ops.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Ops 감사 로그: 이벤트 이력 조회 API (ADMIN 전용).
 */
@Tag(name = "Ops 감사 로그", description = "감사 로그 조회 (ADMIN 전용)")
@RestController
@RequestMapping("/api/v1/ops/audit")
@RequiredArgsConstructor
public class OpsAuditController {

    private final AuditLogService auditLogService;

    @Operation(summary = "감사 로그 목록", description = "페이징·이벤트유형·기간 필터로 감사 이력을 조회합니다.")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuditLogListResponseDto> getAuditLogs(
            @Parameter(description = "페이지 (0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (1~100)") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "필터: SETTING_CHANGE, MANUAL_TRIGGER, REAL_ACCOUNT_GUARD_BLOCKED") @RequestParam(required = false) String eventType,
            @Parameter(description = "기간 시작 (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "기간 종료 (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Instant fromInstant = from != null ? from.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant() : null;
        Instant toInstant = to != null ? ZonedDateTime.of(to.atTime(23, 59, 59, 999_999_999), ZoneId.of("Asia/Seoul")).toInstant() : null;
        AuditLogListResponseDto dto = auditLogService.findPage(page, size, eventType, fromInstant, toInstant);
        return ResponseEntity.ok(dto);
    }
}
