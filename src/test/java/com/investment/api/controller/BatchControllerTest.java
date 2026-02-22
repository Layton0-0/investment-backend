package com.investment.api.controller;

import com.investment.account.service.AccountService;
import com.investment.batch.dto.BatchJobDto;
import com.investment.batch.service.BatchManagementService;
import com.investment.common.security.JwtAuthenticationFilter;
import com.investment.config.SecurityHeadersConfig;
import com.investment.common.security.RateLimitFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BatchController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("BatchController")
class BatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BatchManagementService batchManagementService;
    @MockBean
    private AccountService accountService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private RateLimitFilter rateLimitFilter;
    @MockBean
    private SecurityHeadersConfig securityHeadersConfig;

    @Test
    @WithMockUser
    @DisplayName("GET /api/v1/batch/jobs 인증 시 200 및 배열 반환")
    void getJobs_authenticated_returnsOk() throws Exception {
        BatchJobDto job = BatchJobDto.builder()
                .id("test-job")
                .name("테스트 배치")
                .status("ACTIVE")
                .build();
        when(batchManagementService.getAllBatchJobs()).thenReturn(List.of(job));

        mockMvc.perform(get("/api/v1/batch/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value("test-job"))
                .andExpect(jsonPath("$[0].name").value("테스트 배치"));
    }
}
