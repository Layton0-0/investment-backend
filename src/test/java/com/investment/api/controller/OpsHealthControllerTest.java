package com.investment.api.controller;

import com.investment.account.service.AccountService;
import com.investment.common.security.JwtAuthenticationFilter;
import com.investment.common.security.RateLimitFilter;
import com.investment.config.SecurityHeadersConfig;
import com.investment.ops.dto.OpsHealthDto;
import com.investment.ops.service.OpsHealthService;
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

@WebMvcTest(OpsHealthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("OpsHealthController")
class OpsHealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OpsHealthService opsHealthService;
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
    @DisplayName("GET /api/v1/ops/health ADMIN 역할 시 200 및 db·redis·predictionService·lastCheckedAt 반환")
    @WithMockUser(username = "admin1", roles = "ADMIN")
    void getHealth_withAdmin_returnsOk() throws Exception {
        OpsHealthDto dto = OpsHealthDto.builder()
                .db("UP")
                .redis("UP")
                .predictionService("UP")
                .lastCheckedAt(Instant.now())
                .build();
        when(opsHealthService.getHealth()).thenReturn(dto);

        mockMvc.perform(get("/api/v1/ops/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.db").value("UP"))
                .andExpect(jsonPath("$.redis").value("UP"))
                .andExpect(jsonPath("$.predictionService").value("UP"))
                .andExpect(jsonPath("$.lastCheckedAt").exists());
    }

    @Test
    @DisplayName("GET /api/v1/ops/health Redis 미설정 시 redis UNKNOWN")
    @WithMockUser(username = "admin1", roles = "ADMIN")
    void getHealth_redisUnknown_returnsDto() throws Exception {
        OpsHealthDto dto = OpsHealthDto.builder()
                .db("UP")
                .redis("UNKNOWN")
                .predictionService("DOWN")
                .lastCheckedAt(Instant.now())
                .build();
        when(opsHealthService.getHealth()).thenReturn(dto);

        mockMvc.perform(get("/api/v1/ops/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redis").value("UNKNOWN"))
                .andExpect(jsonPath("$.predictionService").value("DOWN"));
    }
}
