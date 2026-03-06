package com.investment.api.controller;

import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.common.util.AccountNumberUtil;
import com.investment.risk.dto.PortfolioRiskMetricsDto;
import com.investment.risk.dto.RiskHistoryItemDto;
import com.investment.risk.dto.PerformanceAttributionDto;
import com.investment.risk.dto.RiskLimitsDto;
import com.investment.risk.dto.RiskSummaryDto;
import com.investment.risk.service.PerformanceAttributionService;
import com.investment.risk.service.RiskReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

/**
 * 리스크 리포트 API: 킬스위치·일일 손실 한도·리스크 게이트·계좌별 MDD 등 조회.
 */
@Tag(name = "Risk Report", description = "리스크 리포트 (킬스위치·일일 손실 한도·게이트·MDD)")
@RestController
@RequestMapping("/api/v1/risk")
@RequiredArgsConstructor
public class RiskReportController {

    private final RiskReportService riskReportService;
    private final PerformanceAttributionService performanceAttributionService;

    @Operation(summary = "성과 귀인", description = "팩터/전략별 수익 기여도. 청산 포지션 기준 실현 손익 집계.")
    @GetMapping("/attribution")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PerformanceAttributionDto> getAttribution(Principal principal) {
        String userId = getUserId(principal);
        PerformanceAttributionDto dto = performanceAttributionService.getAttribution(userId);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "리스크 요약", description = "킬스위치·리스크 게이트·계좌별 일일 손실 한도·MDD 요약을 반환합니다.")
    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RiskSummaryDto> getSummary(Principal principal) {
        String userId = getUserId(principal);
        RiskSummaryDto dto = riskReportService.getSummary(userId);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "리스크 한도 설정", description = "일일 손실 한도·VIX 임계값 등 한도 설정 요약을 반환합니다.")
    @GetMapping("/limits")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RiskLimitsDto> getLimits() {
        RiskLimitsDto dto = riskReportService.getLimits();
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "포트폴리오 리스크 메트릭", description = "단일 계좌의 VaR/CVaR/MDD·Sharpe/Sortino. 데이터 없으면 200 + 빈 DTO (404 미발생).")
    @GetMapping("/portfolio-metrics")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PortfolioRiskMetricsDto> getPortfolioRiskMetrics(
            Principal principal,
            @Parameter(description = "계좌번호 (형식: 8자리-2자리)") @RequestParam String accountNo) {
        if (!AccountNumberUtil.validateAccountNumberFormat(accountNo)) {
            return ResponseEntity.badRequest().build();
        }
        String userId = getUserId(principal);
        PortfolioRiskMetricsDto dto = riskReportService.getPortfolioRiskMetrics(userId, accountNo);
        if (dto == null) {
            return ResponseEntity.ok(PortfolioRiskMetricsDto.builder().build());
        }
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "리스크 이력", description = "게이트 축소·손실 한도 도달 이력을 반환합니다. 1차는 빈 목록.")
    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RiskHistoryItemDto>> getHistory(
            Principal principal,
            @Parameter(description = "시작일 (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "종료일 (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        String userId = getUserId(principal);
        LocalDate fromDate = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate toDate = to != null ? to : LocalDate.now();
        List<RiskHistoryItemDto> list = riskReportService.getHistory(userId, fromDate, toDate);
        return ResponseEntity.ok(list);
    }

    private static String getUserId(Principal principal) {
        String name = null;
        if (principal != null && principal.getName() != null) {
            name = principal.getName();
        } else {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null) {
                name = auth.getName();
            }
        }
        if (name == null) {
            throw new DomainException(ErrorCode.UNAUTHORIZED, "인증되지 않은 사용자입니다");
        }
        return name;
    }
}
