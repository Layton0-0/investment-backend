package com.investment.api.controller;

import com.investment.account.service.AccountService;
import com.investment.common.security.JwtAuthenticationFilter;
import com.investment.common.security.RateLimitFilter;
import com.investment.config.SecurityHeadersConfig;
import com.investment.ops.dto.DataPipelineSourceStatusDto;
import com.investment.ops.dto.DataPipelineStatusDto;
import com.investment.ops.service.DataPipelineStatusService;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OpsDataPipelineController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("OpsDataPipelineController")
class OpsDataPipelineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DataPipelineStatusService dataPipelineStatusService;
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
    @DisplayName("GET /api/v1/ops/data-pipeline/status ADMIN 역할 시 200 및 본문 반환")
    @WithMockUser(username = "admin1", roles = "ADMIN")
    void getStatus_withAdmin_returnsOk() throws Exception {
        DataPipelineStatusDto dto = DataPipelineStatusDto.builder()
                .sources(List.of(
                        DataPipelineSourceStatusDto.builder()
                                .sourceId("DART")
                                .displayName("DART 공시")
                                .lastRunTime(LocalDateTime.now().minusHours(1))
                                .lastBaselineDate(LocalDate.now())
                                .status("OK")
                                .errorSummary(null)
                                .build()))
                .updatedAt(LocalDateTime.now())
                .build();
        when(dataPipelineStatusService.getStatus()).thenReturn(dto);

        mockMvc.perform(get("/api/v1/ops/data-pipeline/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sources").isArray())
                .andExpect(jsonPath("$.sources[0].sourceId").value("DART"))
                .andExpect(jsonPath("$.sources[0].status").value("OK"))
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    /**
     * USER만 있을 때 403은 통합 테스트 또는 실제 SecurityConfig 로드 시 검증.
     * WebMvcTest 슬라이스에서는 Method Security 설정이 로드되지 않을 수 있어 제외.
     */
}
