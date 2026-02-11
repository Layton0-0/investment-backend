package com.investment.api.controller;

import com.investment.common.security.JwtAuthenticationFilter;
import com.investment.common.security.RateLimitFilter;
import com.investment.config.SecurityHeadersConfig;
import com.investment.governance.GovernanceHaltService;
import com.investment.ops.dto.GovernanceCheckResultDto;
import com.investment.ops.dto.GovernanceHaltDto;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OpsGovernanceController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("OpsGovernanceController")
class OpsGovernanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GovernanceHaltService governanceHaltService;
    @MockBean
    private com.investment.account.service.AccountService accountService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private RateLimitFilter rateLimitFilter;
    @MockBean
    private SecurityHeadersConfig securityHeadersConfig;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/v1/ops/governance/results ADMIN 시 200 및 목록 반환")
    @WithMockUser(username = "admin1", roles = "ADMIN")
    void getResults_withAdmin_returnsOk() throws Exception {
        GovernanceCheckResultDto dto = GovernanceCheckResultDto.builder()
                .id(1L)
                .market("KR")
                .strategyType("SHORT_TERM")
                .mddPct(new BigDecimal("-18"))
                .sharpeRatio(new BigDecimal("0.5"))
                .degraded(true)
                .build();
        when(governanceHaltService.getRecentResults(20)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/ops/governance/results").param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].market").value("KR"))
                .andExpect(jsonPath("$[0].degraded").value(true));

        verify(governanceHaltService).getRecentResults(20);
    }

    @Test
    @DisplayName("GET /api/v1/ops/governance/halts ADMIN 시 200 및 목록 반환")
    @WithMockUser(username = "admin1", roles = "ADMIN")
    void getHalts_withAdmin_returnsOk() throws Exception {
        GovernanceHaltDto dto = GovernanceHaltDto.builder()
                .market("KR")
                .strategyType("SHORT_TERM")
                .reason("MDD degraded")
                .build();
        when(governanceHaltService.getActiveHalts()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/ops/governance/halts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].market").value("KR"))
                .andExpect(jsonPath("$[0].strategyType").value("SHORT_TERM"));

        verify(governanceHaltService).getActiveHalts();
    }

    @Test
    @DisplayName("PUT /api/v1/ops/governance/halts/{market}/{strategyType}/clear ADMIN 시 204")
    @WithMockUser(username = "admin1", roles = "ADMIN")
    void clearHalt_withAdmin_returns204() throws Exception {
        mockMvc.perform(put("/api/v1/ops/governance/halts/KR/SHORT_TERM/clear")
                        .contentType("application/json")
                        .content("{\"clearedBy\": \"admin1\"}"))
                .andExpect(status().isNoContent());

        verify(governanceHaltService).clearHalt("KR", "SHORT_TERM", "admin1");
    }

    @Test
    @DisplayName("PUT clear body 없으면 clearedBy admin으로 호출")
    @WithMockUser(username = "admin1", roles = "ADMIN")
    void clearHalt_noBody_usesAdmin() throws Exception {
        mockMvc.perform(put("/api/v1/ops/governance/halts/US/MEDIUM_TERM/clear"))
                .andExpect(status().isNoContent());

        verify(governanceHaltService).clearHalt("US", "MEDIUM_TERM", "admin");
    }
}
