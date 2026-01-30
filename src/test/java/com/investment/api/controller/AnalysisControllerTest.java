package com.investment.api.controller;

import com.investment.analysis.dto.AnalysisRequestDto;
import com.investment.analysis.dto.AnalysisResponseDto;
import com.investment.analysis.service.AnalysisService;
import com.investment.common.security.JwtAuthenticationFilter;
import com.investment.common.security.RateLimitFilter;
import com.investment.config.SecurityHeadersConfig;
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
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalysisController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AnalysisController")
class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalysisService analysisService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private RateLimitFilter rateLimitFilter;
    @MockBean
    private SecurityHeadersConfig securityHeadersConfig;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/analysis 종목 분석 성공")
    void analyze_returnsOk() throws Exception {
        AnalysisRequestDto request = AnalysisRequestDto.builder()
                .symbol("005930")
                .periodDays(30)
                .build();
        AnalysisResponseDto response = AnalysisResponseDto.builder()
                .symbol("005930")
                .recommendation("BUY")
                .confidence(BigDecimal.valueOf(0.85))
                .targetPrice(BigDecimal.valueOf(80000))
                .currentPrice(BigDecimal.valueOf(75000))
                .expectedReturn(BigDecimal.valueOf(6.67))
                .analyzedAt(LocalDateTime.now())
                .build();
        when(analysisService.analyze(any(AnalysisRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("005930"))
                .andExpect(jsonPath("$.recommendation").value("BUY"));
    }
}
