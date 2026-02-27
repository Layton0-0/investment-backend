package com.investment.api.controller;

import com.investment.common.security.JwtAuthenticationFilter;
import com.investment.common.security.RateLimitFilter;
import com.investment.config.SecurityHeadersConfig;
import com.investment.ops.dto.AutoTradingReadinessDto;
import com.investment.ops.service.AutoTradingReadinessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OpsAutoTradingReadinessController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("OpsAutoTradingReadinessController")
class OpsAutoTradingReadinessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AutoTradingReadinessService autoTradingReadinessService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private RateLimitFilter rateLimitFilter;
    @MockBean
    private SecurityHeadersConfig securityHeadersConfig;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/v1/ops/auto-trading-readiness ADMIN 시 200 및 DTO 반환")
    @WithMockUser(username = "admin1", roles = "ADMIN")
    void getReadiness_withAdmin_returnsOk() throws Exception {
        LocalDate basDt = LocalDate.now().minusDays(1);
        AutoTradingReadinessDto dto = AutoTradingReadinessDto.builder()
                .basDt(basDt)
                .autoTradingOnAccountCount(2L)
                .dailyStockRowCount(100L)
                .signalScoreRowCount(50L)
                .activeGovernanceHaltCount(0)
                .build();
        when(autoTradingReadinessService.getReadiness()).thenReturn(dto);

        mockMvc.perform(get("/api/v1/ops/auto-trading-readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autoTradingOnAccountCount").value(2))
                .andExpect(jsonPath("$.dailyStockRowCount").value(100))
                .andExpect(jsonPath("$.signalScoreRowCount").value(50))
                .andExpect(jsonPath("$.activeGovernanceHaltCount").value(0));

        verify(autoTradingReadinessService).getReadiness();
    }

    @Test
    @DisplayName("GET /api/v1/ops/auto-trading-readiness 비인증 시 403")
    void getReadiness_withoutAuth_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/ops/auto-trading-readiness"))
                .andExpect(status().isForbidden());
    }
}
