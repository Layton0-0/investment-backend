package com.investment.api.controller;

import com.investment.common.security.JwtAuthenticationFilter;
import com.investment.common.security.RateLimitFilter;
import com.investment.config.SecurityHeadersConfig;
import com.investment.governance.GovernanceHaltService;
import com.investment.ops.dto.GovernanceCheckResultDto;
import com.investment.ops.dto.GovernanceHaltDto;
import com.investment.setting.service.SystemSettingService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
    private SystemSettingService systemSettingService;
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
    @DisplayName("GET /api/v1/ops/governance/status ADMIN 시 200 및 governanceEnabled 반환")
    @WithMockUser(username = "admin1", roles = "ADMIN")
    void getStatus_withAdmin_returnsOk() throws Exception {
        when(systemSettingService.getBoolean("governance.enabled")).thenReturn(true);

        mockMvc.perform(get("/api/v1/ops/governance/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.governanceEnabled").value(true));

        verify(systemSettingService).getBoolean("governance.enabled");
    }

    @Test
    @DisplayName("GET /api/v1/ops/governance/results ADMIN 시 200 및 목록 반환")
    @WithMockUser(username = "admin1", roles = "ADMIN")
    void getResults_withAdmin_returnsOk() throws Exception {
        GovernanceCheckResultDto dto = GovernanceCheckResultDto.builder()
                .id(1L)
                .market("KR")
                .strategyType("SHORT_TERM")
                .passed(false)
                .mddPct(new BigDecimal("-18"))
                .sharpeRatio(new BigDecimal("0.5"))
                .message("Degraded")
                .degraded(true)
                .build();
        when(governanceHaltService.getRecentResults(20)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/ops/governance/results").param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].market").value("KR"))
                .andExpect(jsonPath("$[0].passed").value(false))
                .andExpect(jsonPath("$[0].message").value("Degraded"))
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

    /**
     * 경로에 빈 세그먼트(//)가 있으면 Spring이 매핑하지 않아 404가 나올 수 있음.
     * 컨트롤러는 매핑된 요청에 대해 market/strategyType blank 시 400 INVALID_INPUT 반환.
     */
    @Test
    @DisplayName("PUT clear market 공백(빈 세그먼트)이면 400 또는 404")
    @WithMockUser(username = "admin1", roles = "ADMIN")
    void clearHalt_blankMarket_returns4xx() throws Exception {
        int status = mockMvc.perform(put("/api/v1/ops/governance/halts//SHORT_TERM/clear"))
                .andReturn().getResponse().getStatus();
        assertThat(status).isIn(400, 404);
        if (status == 400) {
            verify(governanceHaltService, never()).clearHalt(any(), any(), any());
        }
    }

    @Test
    @DisplayName("PUT clear strategyType 공백(빈 세그먼트)이면 400 또는 404")
    @WithMockUser(username = "admin1", roles = "ADMIN")
    void clearHalt_blankStrategyType_returns4xx() throws Exception {
        int status = mockMvc.perform(put("/api/v1/ops/governance/halts/KR//clear")
                        .contentType("application/json")
                        .content("{}"))
                .andReturn().getResponse().getStatus();
        assertThat(status).isIn(400, 404);
        if (status == 400) {
            verify(governanceHaltService, never()).clearHalt(any(), any(), any());
        }
    }

    @Test
    @DisplayName("GET results limit 초과 시 500으로 캡하여 서비스 호출")
    @WithMockUser(username = "admin1", roles = "ADMIN")
    void getResults_limitOver500_capsTo500() throws Exception {
        when(governanceHaltService.getRecentResults(500)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/ops/governance/results").param("limit", "1000"))
                .andExpect(status().isOk());

        verify(governanceHaltService).getRecentResults(500);
    }

    @Test
    @DisplayName("GET results limit 미지정 시 default 20")
    @WithMockUser(username = "admin1", roles = "ADMIN")
    void getResults_noLimit_usesDefault20() throws Exception {
        when(governanceHaltService.getRecentResults(20)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/ops/governance/results"))
                .andExpect(status().isOk());

        verify(governanceHaltService).getRecentResults(20);
    }
}
