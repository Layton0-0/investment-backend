package com.investment.api.controller;

import com.investment.ops.dto.DataPipelineStatusDto;
import com.investment.ops.service.DataPipelineStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ops 전용 데이터 파이프라인 상태 API.
 * 원천별(DART/SEC/KRX/US) 수집 상태·최근 기준일·오류 요약 조회.
 */
@Tag(name = "Ops 데이터 파이프라인", description = "데이터 파이프라인 원천별 수집 상태 (ADMIN 전용)")
@RestController
@RequestMapping("/api/v1/ops/data-pipeline")
@RequiredArgsConstructor
public class OpsDataPipelineController {

    private final DataPipelineStatusService dataPipelineStatusService;

    @Operation(summary = "데이터 파이프라인 상태", description = "DART/SEC/KRX/US 원천별 마지막 실행 시각·최근 기준일·오류 요약을 반환합니다.")
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DataPipelineStatusDto> getStatus() {
        DataPipelineStatusDto dto = dataPipelineStatusService.getStatus();
        return ResponseEntity.ok(dto);
    }
}
