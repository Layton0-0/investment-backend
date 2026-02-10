package com.investment.api.controller;

import com.investment.ops.dto.AlertListResponseDto;
import com.investment.ops.service.OpsAlertsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ops 알림센터: 알림 이력 조회 API (ADMIN 전용).
 */
@Tag(name = "Ops 알림센터", description = "알림 이력 조회 (ADMIN 전용)")
@RestController
@RequestMapping("/api/v1/ops/alerts")
@RequiredArgsConstructor
public class OpsAlertsController {

    private final OpsAlertsService opsAlertsService;

    @Operation(summary = "알림 목록", description = "페이징·레벨 필터로 알림 이력을 조회합니다.")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AlertListResponseDto> getAlerts(
            @Parameter(description = "페이지 (0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (1~100)") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "필터: INFO, WARNING, ERROR") @RequestParam(required = false) String level) {
        AlertListResponseDto dto = opsAlertsService.getAlerts(page, size, level);
        return ResponseEntity.ok(dto);
    }
}
