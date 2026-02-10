package com.investment.api.controller;

import com.investment.ops.dto.OpsModelStatusDto;
import com.investment.ops.service.OpsModelStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ops 모델/예측 상태 API (ADMIN 전용).
 * 예측 서비스(AI) 상태·버전·실패율 등 노출.
 */
@Tag(name = "Ops 모델/예측", description = "예측 모델 상태 (ADMIN 전용)")
@RestController
@RequestMapping("/api/v1/ops/model")
@RequiredArgsConstructor
public class OpsModelController {

    private final OpsModelStatusService opsModelStatusService;

    @Operation(summary = "모델/예측 상태", description = "예측 서비스 헬스·설정 URL 표시(마스킹)·마지막 체크 시각을 반환합니다.")
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OpsModelStatusDto> getStatus() {
        OpsModelStatusDto dto = opsModelStatusService.getStatus();
        return ResponseEntity.ok(dto);
    }
}
