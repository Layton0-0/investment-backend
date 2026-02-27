package com.investment.api.controller;

import com.investment.ops.dto.AutoTradingReadinessDto;
import com.investment.ops.service.AutoTradingReadinessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ops 자동매매 준비 상태: 09:10 실행 전 점검용 (ADMIN 전용).
 */
@Tag(name = "Ops 자동매매 준비 상태", description = "자동매매 가동 전 점검용 (ADMIN 전용)")
@RestController
@RequestMapping("/api/v1/ops/auto-trading-readiness")
@RequiredArgsConstructor
public class OpsAutoTradingReadinessController {

    private final AutoTradingReadinessService autoTradingReadinessService;

    @Operation(summary = "자동매매 준비 상태", description = "자동투자 ON 계좌 수, 전일 일봉·시그널 건수, 활성 거버넌스 halt 수를 한 번에 조회합니다. 09:10 전 점검용.")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AutoTradingReadinessDto> getReadiness() {
        AutoTradingReadinessDto dto = autoTradingReadinessService.getReadiness();
        return ResponseEntity.ok(dto);
    }
}
