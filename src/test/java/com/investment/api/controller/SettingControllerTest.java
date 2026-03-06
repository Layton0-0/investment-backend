package com.investment.api.controller;

import com.investment.account.service.AccountService;
import com.investment.auth.service.AuthService;
import com.investment.common.security.JwtAuthenticationFilter;
import com.investment.common.security.RateLimitFilter;
import com.investment.config.SecurityHeadersConfig;
import com.investment.ops.service.AuditLogService;
import com.investment.setting.dto.QuickStartRequestDto;
import com.investment.setting.dto.QuickStartResponseDto;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        when(tradingSettingService.getSettingOptional("12345678-01")).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/api/v1/settings/12345678-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultCurrency").value("KRW"));
    }

    @Test
    @DisplayName("GET /api/v1/settings/{accountNo} 설정 없으면 204 No Content (기본값 폼 표시용)")
    void getSetting_returns204WhenNotFound() throws Exception {
        when(tradingSettingService.getSettingOptional("99999999")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/settings/99999999"))
                .andExpect(status().isNoContent());
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
        when(tradingSettingService.saveSetting(eq("12345678-01"), any(TradingSettingDto.class))).thenReturn(request);

        mockMvc.perform(put("/api/v1/settings/12345678-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultCurrency").value("KRW"));
    }

    @Test
    @WithMockUser(username = "user-1")
    @DisplayName("POST /api/v1/settings/quick-start 인증 시 200 및 success 반환")
    void quickStart_returnsOk() throws Exception {
        QuickStartRequestDto request = QuickStartRequestDto.builder()
                .maxInvestmentAmount(new BigDecimal("1000000"))
                .build();
        TradingSettingDto settingDto = TradingSettingDto.builder()
                .maxInvestmentAmount(new BigDecimal("1000000"))
                .minInvestmentAmount(new BigDecimal("100000"))
                .defaultCurrency("KRW")
                .autoTradingEnabled(true)
                .pipelineAutoExecute(true)
                .build();
        QuickStartResponseDto response = QuickStartResponseDto.builder()
                .success(true)
                .message("자동투자가 시작되었습니다.")
                .setting(settingDto)
                .build();
        when(tradingSettingService.quickStart(eq("user-1"), any(QuickStartRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/settings/quick-start")
                        .with(SecurityMockMvcRequestPostProcessors.user("user-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("자동투자가 시작되었습니다."))
                .andExpect(jsonPath("$.setting.autoTradingEnabled").value(true))
                .andExpect(jsonPath("$.setting.pipelineAutoExecute").value(true));
    }
}
