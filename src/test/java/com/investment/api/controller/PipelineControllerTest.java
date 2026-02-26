package com.investment.api.controller;

import com.investment.common.security.JwtAuthenticationFilter;
import com.investment.common.security.RateLimitFilter;
import com.investment.config.SecurityHeadersConfig;
import com.investment.factor.dto.PipelineSummaryDto;
import com.investment.factor.service.PipelineSummaryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PipelineController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("PipelineController")
class PipelineControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private PipelineSummaryService pipelineSummaryService;
        @MockBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;
        @MockBean
        private RateLimitFilter rateLimitFilter;
        @MockBean
        private SecurityHeadersConfig securityHeadersConfig;

        @Test
        @DisplayName("GET /api/v1/pipeline/summary 요약 조회 성공")
        void getSummary_returnsOk() throws Exception {
                PipelineSummaryDto dto = PipelineSummaryDto.builder()
                                .basDt(LocalDate.of(2026, 2, 4))
                                .universeCountKr(10)
                                .universeCountUs(20)
                                .signalCountKr(3)
                                .signalCountUs(4)
                                .allocationSummary("단기 2,000만 · 중기 4,000만 · 장기 4,000만")
                                .openPositionCount(1)
                                .build();
                when(pipelineSummaryService.getSummary(eq(LocalDate.of(2026, 2, 4)), eq("12345678-12")))
                                .thenReturn(dto);

                mockMvc.perform(get("/api/v1/pipeline/summary")
                                .param("accountNo", "12345678-12")
                                .param("basDt", "2026-02-04"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.universeCountKr").value(10))
                                .andExpect(jsonPath("$.signalCountUs").value(4))
                                .andExpect(jsonPath("$.openPositionCount").value(1))
                                .andExpect(jsonPath("$.allocationSummary").value("단기 2,000만 · 중기 4,000만 · 장기 4,000만"));
        }
}
