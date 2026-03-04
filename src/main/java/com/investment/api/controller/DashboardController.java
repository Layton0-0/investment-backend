package com.investment.api.controller;

import com.investment.account.dto.ProfitLossDto;
import com.investment.account.service.AccountService;
import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.domain.repository.TradingSettingRepository;
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

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

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
    private final TradingSettingRepository tradingSettingRepository;
    private final AccountService accountService;

    @Operation(summary = "성과 요약", description = "총 평가액·MDD·Sharpe·VaR·일일 손익·리스크 수준 등 대시보드 카드용 요약을 반환합니다.")
    @GetMapping("/performance-summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DashboardPerformanceSummaryDto> getPerformanceSummary(Principal principal) {
        String userId = getUserId(principal);
        RiskSummaryDto summary = riskReportService.getSummary(userId);
        BigDecimal dailyProfitLoss = sumDailyProfitLossForUser(userId);
        DashboardPerformanceSummaryDto dto = DashboardPerformanceSummaryDto.builder()
                .totalCurrentValue(summary.getTotalCurrentValue())
                .maxMddPct(summary.getMaxMddPct())
                .sharpeRatio(summary.getSharpeRatio())
                .sortinoRatio(summary.getSortinoRatio())
                .var95Pct(summary.getVar95Pct())
                .cvar95Pct(summary.getCvar95Pct())
                .dailyProfitLoss(dailyProfitLoss)
                .riskLevel(summary.getRiskLevel())
                .build();
        return ResponseEntity.ok(dto);
    }

    /**
     * 현재 사용자 계좌별 당일 손익을 조회해 합산.
     * 계좌별 API 실패 시 해당 계좌는 0으로 간주.
     */
    private BigDecimal sumDailyProfitLossForUser(String userId) {
        List<com.investment.domain.entity.TradingSetting> settings = tradingSettingRepository.findByUserIdOrderByAccountNo(userId);
        if (settings == null || settings.isEmpty()) {
            return null;
        }
        LocalDate today = LocalDate.now();
        BigDecimal sum = BigDecimal.ZERO;
        for (com.investment.domain.entity.TradingSetting setting : settings) {
            try {
                ProfitLossDto pl = accountService.getPeriodProfitLoss(setting.getAccountNo(), today, today);
                if (pl != null && pl.getDailyProfitLossList() != null) {
                    for (ProfitLossDto.DailyProfitLossDto daily : pl.getDailyProfitLossList()) {
                        if (today.equals(daily.getDate()) && daily.getProfitLoss() != null) {
                            sum = sum.add(daily.getProfitLoss());
                        }
                    }
                }
            } catch (Exception e) {
                // 계좌별 실패 시 해당 계좌만 제외하고 합산 계속
            }
        }
        return sum;
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
