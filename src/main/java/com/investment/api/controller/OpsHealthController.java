package com.investment.api.controller;

import com.investment.ops.dto.OpsHealthDto;
import com.investment.ops.service.OpsHealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ops 시스템 헬스 API (ADMIN 전용).
 * DB·Redis·예측 서비스 상태 요약.
 */
@Tag(name = "Ops 시스템 헬스", description = "시스템 헬스 요약 (ADMIN 전용)")
@RestController
@RequestMapping("/api/v1/ops/health")
@RequiredArgsConstructor
public class OpsHealthController {

    private final OpsHealthService opsHealthService;

    @Operation(summary = "시스템 헬스", description = "DB·Redis·예측 서비스 상태를 반환합니다.")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OpsHealthDto> getHealth() {
        OpsHealthDto dto = opsHealthService.getHealth();
        return ResponseEntity.ok(dto);
    }
}
