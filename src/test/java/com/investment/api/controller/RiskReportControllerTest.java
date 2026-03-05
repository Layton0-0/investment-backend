package com.investment.api.controller;

import com.investment.account.service.AccountService;
import com.investment.common.security.JwtAuthenticationFilter;
import com.investment.common.security.RateLimitFilter;
import com.investment.config.SecurityHeadersConfig;
import com.investment.risk.dto.RiskAccountSummaryDto;
import com.investment.risk.dto.RiskLimitsDto;
import com.investment.risk.dto.RiskSummaryDto;
import com.investment.risk.service.PerformanceAttributionService;
import com.investment.risk.service.RiskReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RiskReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("RiskReportController")
class RiskReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RiskReportService riskReportService;
    @MockBean
    private PerformanceAttributionService performanceAttributionService;
    @MockBean
    private AccountService accountService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private RateLimitFilter rateLimitFilter;
    @MockBean
    private SecurityHeadersConfig securityHeadersConfig;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/v1/risk/summary 인증 시 200 및 요약 반환")
    @WithMockUser(username = "user1")
    void getSummary_returnsOk() throws Exception {
        RiskSummaryDto dto = RiskSummaryDto.builder()
                .killSwitchActive(false)
                .regimeGateEnabled(true)
                .riskGateAllowsNewBuy(true)
                .riskGateSizeMultiplier(new BigDecimal("1.0"))
                .accounts(List.of(RiskAccountSummaryDto.builder()
                        .accountNoMasked("****1234")
                        .serverType("1")
                        .openingBalance(new BigDecimal("10000000"))
                        .currentValue(new BigDecimal("10500000"))
                        .newBuyBlockedByDailyLoss(false)
                        .mdd(new BigDecimal("0.05"))
                        .peakValue(new BigDecimal("11000000"))
                        .build()))
                .build();
        when(riskReportService.getSummary(eq("user1"))).thenReturn(dto);

        setSecurityContextUser("user1");
        try {
            mockMvc.perform(get("/api/v1/risk/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.killSwitchActive").value(false))
                    .andExpect(jsonPath("$.regimeGateEnabled").value(true))
                    .andExpect(jsonPath("$.accounts[0].accountNoMasked").value("****1234"))
                    .andExpect(jsonPath("$.accounts[0].newBuyBlockedByDailyLoss").value(false));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("GET /api/v1/risk/summary 미인증 시 401")
    void getSummary_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/risk/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/risk/limits 인증 시 200 및 한도 반환")
    @WithMockUser
    void getLimits_returnsOk() throws Exception {
        RiskLimitsDto dto = RiskLimitsDto.builder()
                .regimeGateEnabled(true)
                .vixThreshold(new BigDecimal("30"))
                .reduceSizeOnHighVolPct(new BigDecimal("50"))
                .dailyLossLimitPct(new BigDecimal("5"))
                .build();
        when(riskReportService.getLimits()).thenReturn(dto);

        mockMvc.perform(get("/api/v1/risk/limits")
                        .with(SecurityMockMvcRequestPostProcessors.user("test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyLossLimitPct").value(5))
                .andExpect(jsonPath("$.vixThreshold").value(30));
    }

    @Test
    @DisplayName("GET /api/v1/risk/history 인증 시 200 및 목록 반환")
    @WithMockUser(username = "user1")
    void getHistory_returnsOk() throws Exception {
        when(riskReportService.getHistory(eq("user1"), any(), any())).thenReturn(List.of());

        setSecurityContextUser("user1");
        try {
            mockMvc.perform(get("/api/v1/risk/history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private static void setSecurityContextUser(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, AuthorityUtils.createAuthorityList("ROLE_USER")));
    }
}
