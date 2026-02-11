package com.investment.api.controller;

import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.risk.dto.DashboardPerformanceSummaryDto;
import com.investment.risk.dto.RiskSummaryDto;
import com.investment.risk.service.RiskReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * 대시보드 전용 API.
 * 성과 요약 등 대시보드 카드/차트용 데이터 제공.
 */
@Tag(name = "Dashboard", description = "대시보드 (성과 요약 등)")
@RestController("apiDashboardController")
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final RiskReportService riskReportService;

    @Operation(summary = "성과 요약", description = "총 평가액·MDD·Sharpe·VaR 등 대시보드 카드용 요약을 반환합니다.")
    @GetMapping("/performance-summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DashboardPerformanceSummaryDto> getPerformanceSummary(Principal principal) {
        String userId = getUserId(principal);
        RiskSummaryDto summary = riskReportService.getSummary(userId);
        DashboardPerformanceSummaryDto dto = DashboardPerformanceSummaryDto.builder()
                .totalCurrentValue(summary.getTotalCurrentValue())
                .maxMddPct(summary.getMaxMddPct())
                .sharpeRatio(summary.getSharpeRatio())
                .sortinoRatio(summary.getSortinoRatio())
                .var95Pct(summary.getVar95Pct())
                .cvar95Pct(summary.getCvar95Pct())
                .build();
        return ResponseEntity.ok(dto);
    }

    private static String getUserId(Principal principal) {
        String name = principal != null ? principal.getName() : null;
        if (name == null) {
            Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            name = auth != null ? auth.getName() : null;
        }
        if (name == null) {
            throw new DomainException(ErrorCode.UNAUTHORIZED, "인증되지 않은 사용자입니다");
        }
        return name;
    }
}
