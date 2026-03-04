package com.investment.api.controller;

import com.investment.account.service.AccountService;
import com.investment.common.security.JwtAuthenticationFilter;
import com.investment.common.security.RateLimitFilter;
import com.investment.config.SecurityHeadersConfig;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.risk.dto.DashboardPerformanceSummaryDto;
import com.investment.risk.dto.RiskSummaryDto;
import com.investment.risk.service.RiskReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("DashboardController")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RiskReportService riskReportService;
    @MockBean
    private TradingSettingRepository tradingSettingRepository;
    @MockBean
    private AccountService accountService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private RateLimitFilter rateLimitFilter;
    @MockBean
    private SecurityHeadersConfig securityHeadersConfig;

    @Test
    @DisplayName("GET /api/v1/dashboard/performance-summary 인증 시 200 및 성과 요약 반환")
    @WithMockUser(username = "user1")
    void getPerformanceSummary_returnsOk() throws Exception {
        RiskSummaryDto summary = RiskSummaryDto.builder()
                .totalCurrentValue(new BigDecimal("15000000"))
                .maxMddPct(new BigDecimal("0.12"))
                .sharpeRatio(new BigDecimal("1.2"))
                .sortinoRatio(new BigDecimal("1.5"))
                .var95Pct(new BigDecimal("1.65"))
                .cvar95Pct(new BigDecimal("2.06"))
                .riskLevel("중간")
                .build();
        when(riskReportService.getSummary(eq("user1"))).thenReturn(summary);
        when(tradingSettingRepository.findByUserIdOrderByAccountNo(anyString())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/dashboard/performance-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCurrentValue").value(15000000))
                .andExpect(jsonPath("$.maxMddPct").value(0.12))
                .andExpect(jsonPath("$.sharpeRatio").value(1.2))
                .andExpect(jsonPath("$.var95Pct").value(1.65))
                .andExpect(jsonPath("$.riskLevel").value("중간"));
    }

    @Test
    @DisplayName("GET /api/v1/dashboard/performance-summary 미인증 시 401")
    void getPerformanceSummary_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/performance-summary"))
                .andExpect(status().isUnauthorized());
    }
}
