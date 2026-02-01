package com.investment.batch.controller;

import com.investment.auth.service.AuthService;
import com.investment.batch.dto.BatchJobDto;
import com.investment.batch.service.BatchManagementService;
import com.investment.common.security.JwtAuthenticationFilter;
import com.investment.common.security.RateLimitFilter;
import com.investment.config.SecurityHeadersConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BatchManagementController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("BatchManagementController")
class BatchManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BatchManagementService batchManagementService;
    @MockBean
    private AuthService authService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private RateLimitFilter rateLimitFilter;
    @MockBean
    private SecurityHeadersConfig securityHeadersConfig;

    @Test
    @DisplayName("GET /batch/api/jobs 배치 작업 목록 API 200")
    void getBatchJobs_returnsOk() throws Exception {
        BatchJobDto job = BatchJobDto.builder()
                .id("trading-portfolio-generator")
                .name("트레이딩 포트폴리오 생성")
                .status("ACTIVE")
                .build();
        when(batchManagementService.getAllBatchJobs()).thenReturn(List.of(job));

        mockMvc.perform(get("/batch/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value("trading-portfolio-generator"))
                .andExpect(jsonPath("$[0].name").value("트레이딩 포트폴리오 생성"));
    }
}
