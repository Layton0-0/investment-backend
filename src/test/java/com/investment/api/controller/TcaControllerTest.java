package com.investment.api.controller;

import com.investment.common.security.JwtAuthenticationFilter;
import com.investment.common.security.RateLimitFilter;
import com.investment.config.SecurityHeadersConfig;
import com.investment.core.engine.execution.TransactionCostAnalyzer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TcaController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("TcaController")
class TcaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionCostAnalyzer transactionCostAnalyzer;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private RateLimitFilter rateLimitFilter;
    @MockBean
    private SecurityHeadersConfig securityHeadersConfig;

    @Test
    @DisplayName("GET /market-impact - 충격 1% 이하 시 recommendAlgoExecution false")
    void getMarketImpact_smallOrder_recommendAlgoFalse() throws Exception {
        when(transactionCostAnalyzer.estimateMarketImpact(eq(1000), eq(10_000_000L), any()))
                .thenReturn(new BigDecimal("0.002")); // 0.2%

        mockMvc.perform(get("/api/v1/tca/market-impact")
                        .param("orderQuantity", "1000")
                        .param("avgDailyVolume", "10000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendAlgoExecution").value(false))
                .andExpect(jsonPath("$.marketImpactPct").value(0.2));
    }

    @Test
    @DisplayName("GET /market-impact - 충격 1% 초과 시 recommendAlgoExecution true")
    void getMarketImpact_largeOrder_recommendAlgoTrue() throws Exception {
        when(transactionCostAnalyzer.estimateMarketImpact(eq(12_000_000), eq(10_000_000L), any()))
                .thenReturn(new BigDecimal("0.011")); // 1.1%

        mockMvc.perform(get("/api/v1/tca/market-impact")
                        .param("orderQuantity", "12000000")
                        .param("avgDailyVolume", "10000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendAlgoExecution").value(true))
                .andExpect(jsonPath("$.marketImpactPct").value(1.1));
    }
}
