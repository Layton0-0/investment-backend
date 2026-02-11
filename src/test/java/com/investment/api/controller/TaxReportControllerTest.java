package com.investment.api.controller;

import com.investment.account.service.AccountService;
import com.investment.common.security.JwtAuthenticationFilter;
import com.investment.common.security.RateLimitFilter;
import com.investment.config.SecurityHeadersConfig;
import com.investment.report.service.TaxReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaxReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("TaxReportController")
class TaxReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaxReportService taxReportService;
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
    @DisplayName("GET /api/v1/report/tax/summary 인증 시 200 및 요약 본문 반환")
    @WithMockUser(username = "user1")
    void getSummary_authenticated_returnsOk() throws Exception {
        when(taxReportService.getSummary(any(), any())).thenAnswer(inv -> com.investment.report.dto.TaxReportSummaryDto.builder()
                .year(2026)
                .domesticRealizedGainLoss(BigDecimal.ZERO)
                .overseasRealizedGainLoss(null)
                .dividendTotal(null)
                .estimatedTax(null)
                .disclaimer("본 내용은 추정이며 세무 자문이 아닙니다.")
                .build());

        mockMvc.perform(get("/api/v1/report/tax/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.disclaimer").exists());
    }
}
