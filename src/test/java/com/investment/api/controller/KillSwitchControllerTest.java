package com.investment.api.controller;

import com.investment.account.service.AccountService;
import com.investment.common.security.JwtAuthenticationFilter;
import com.investment.common.security.RateLimitFilter;
import com.investment.config.SecurityHeadersConfig;
import com.investment.risk.service.TradingHaltService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(KillSwitchController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("KillSwitchController")
class KillSwitchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradingHaltService tradingHaltService;
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
    @DisplayName("GET /api/v1/system/kill-switch 상태 조회")
    @WithMockUser
    void getKillSwitch_returnsOk() throws Exception {
        when(tradingHaltService.isHaltAllOrders()).thenReturn(false);

        mockMvc.perform(get("/api/v1/system/kill-switch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.haltAllOrders").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/system/kill-switch halt true")
    @WithMockUser
    void getKillSwitch_whenHalt_returnsTrue() throws Exception {
        when(tradingHaltService.isHaltAllOrders()).thenReturn(true);

        mockMvc.perform(get("/api/v1/system/kill-switch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.haltAllOrders").value(true));
    }

    @Test
    @DisplayName("PUT /api/v1/system/kill-switch 설정")
    @WithMockUser(roles = "ADMIN")
    void putKillSwitch_setsAndReturns() throws Exception {
        when(tradingHaltService.isHaltAllOrders()).thenReturn(true);

        mockMvc.perform(put("/api/v1/system/kill-switch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"haltAllOrders\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.haltAllOrders").value(true));
    }
}
