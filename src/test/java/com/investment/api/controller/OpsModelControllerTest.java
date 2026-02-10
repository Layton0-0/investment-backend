package com.investment.api.controller;

import com.investment.account.service.AccountService;
import com.investment.common.security.JwtAuthenticationFilter;
import com.investment.common.security.RateLimitFilter;
import com.investment.config.SecurityHeadersConfig;
import com.investment.ops.dto.OpsModelStatusDto;
import com.investment.ops.service.OpsModelStatusService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OpsModelController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("OpsModelController")
class OpsModelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OpsModelStatusService opsModelStatusService;
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
    @DisplayName("GET /api/v1/ops/model/status ADMIN 역할 시 200 및 modelReady·serviceUrl·lastCheckAt 반환")
    @WithMockUser(username = "admin1", roles = "ADMIN")
    void getStatus_withAdmin_returnsOk() throws Exception {
        Instant now = Instant.now();
        OpsModelStatusDto dto = OpsModelStatusDto.builder()
                .modelReady(true)
                .serviceUrl("configured")
                .lastCheckAt(now)
                .build();
        when(opsModelStatusService.getStatus()).thenReturn(dto);

        mockMvc.perform(get("/api/v1/ops/model/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelReady").value(true))
                .andExpect(jsonPath("$.serviceUrl").value("configured"))
                .andExpect(jsonPath("$.lastCheckAt").exists());
    }

    @Test
    @DisplayName("GET /api/v1/ops/model/status 모델 비가용 시 modelReady false")
    @WithMockUser(username = "admin1", roles = "ADMIN")
    void getStatus_modelNotReady_returnsFalse() throws Exception {
        OpsModelStatusDto dto = OpsModelStatusDto.builder()
                .modelReady(false)
                .serviceUrl("configured")
                .lastCheckAt(Instant.now())
                .build();
        when(opsModelStatusService.getStatus()).thenReturn(dto);

        mockMvc.perform(get("/api/v1/ops/model/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelReady").value(false));
    }
}
