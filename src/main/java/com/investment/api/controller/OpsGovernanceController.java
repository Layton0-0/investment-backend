package com.investment.api.controller;

import com.investment.governance.GovernanceHaltService;
import com.investment.ops.dto.GovernanceCheckResultDto;
import com.investment.ops.dto.GovernanceHaltClearRequestDto;
import com.investment.ops.dto.GovernanceHaltDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Ops 전략 거버넌스: 검사 결과·halt 조회 및 halt 해제 (ADMIN 전용).
 */
@Tag(name = "Ops 전략 거버넌스", description = "전략 거버넌스 검사 결과·halt 조회/해제 (ADMIN 전용)")
@RestController
@RequestMapping("/api/v1/ops/governance")
@RequiredArgsConstructor
public class OpsGovernanceController {

    private final GovernanceHaltService governanceHaltService;

    @Operation(summary = "최근 검사 결과", description = "전략 거버넌스 검사 결과 이력을 RUN_AT 내림차순으로 조회합니다.")
    @GetMapping("/results")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<GovernanceCheckResultDto>> getResults(
            @Parameter(description = "최대 건수 (1~500)") @RequestParam(defaultValue = "20") int limit) {
        int capped = Math.min(500, Math.max(1, limit));
        List<GovernanceCheckResultDto> list = governanceHaltService.getRecentResults(capped);
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "활성 halt 목록", description = "현재 (market, strategyType)별 자동 매매 중단 중인 halt 목록을 조회합니다.")
    @GetMapping("/halts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<GovernanceHaltDto>> getHalts() {
        List<GovernanceHaltDto> list = governanceHaltService.getActiveHalts();
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "halt 해제", description = "지정한 (market, strategyType)의 governance halt를 해제합니다.")
    @PutMapping("/halts/{market}/{strategyType}/clear")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> clearHalt(
            @Parameter(description = "시장 (KR, US)") @PathVariable String market,
            @Parameter(description = "전략 유형 (SHORT_TERM, MEDIUM_TERM, LONG_TERM)") @PathVariable String strategyType,
            @RequestBody(required = false) GovernanceHaltClearRequestDto body) {
        String clearedBy = body != null && body.getClearedBy() != null ? body.getClearedBy() : "admin";
        governanceHaltService.clearHalt(market, strategyType, clearedBy);
        return ResponseEntity.noContent().build();
    }
}
