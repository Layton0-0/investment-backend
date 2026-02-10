package com.investment.api.controller;

import com.investment.account.service.AccountService;
import com.investment.auth.service.AuthService;
import com.investment.common.security.JwtAuthenticationFilter;
import com.investment.common.security.RateLimitFilter;
import com.investment.config.SecurityHeadersConfig;
import com.investment.ops.service.AuditLogService;
import com.investment.setting.dto.TradingSettingDto;
import com.investment.setting.service.TradingSettingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SettingController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SettingController")
class SettingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradingSettingService tradingSettingService;
    @MockBean
    private AuthService authService;
    @MockBean
    private AuditLogService auditLogService;
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
    @DisplayName("GET /api/v1/settings/{accountNo} 거래 설정 조회 성공")
    void getSetting_returnsOk() throws Exception {
        TradingSettingDto dto = TradingSettingDto.builder()
                .maxInvestmentAmount(BigDecimal.valueOf(10_000_000))
                .minInvestmentAmount(BigDecimal.valueOf(100_000))
                .defaultCurrency("KRW")
                .autoTradingEnabled(true)
                .build();
        when(tradingSettingService.getSetting("12345678")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/settings/12345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultCurrency").value("KRW"));
    }

    @Test
    @DisplayName("PUT /api/v1/settings/{accountNo} 거래 설정 저장 성공")
    void updateSetting_returnsOk() throws Exception {
        TradingSettingDto request = TradingSettingDto.builder()
                .maxInvestmentAmount(BigDecimal.valueOf(10_000_000))
                .minInvestmentAmount(BigDecimal.valueOf(100_000))
                .defaultCurrency("KRW")
                .autoTradingEnabled(false)
                .build();
        when(tradingSettingService.saveSetting(eq("12345678"), any(TradingSettingDto.class))).thenReturn(request);

        mockMvc.perform(put("/api/v1/settings/12345678")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultCurrency").value("KRW"));
    }
}
