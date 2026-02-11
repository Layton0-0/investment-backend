package com.investment.api.controller;

import com.investment.account.service.AccountService;
import com.investment.analysis.dto.AnalysisRequestDto;
import com.investment.analysis.dto.AnalysisResponseDto;
import com.investment.analysis.dto.CorrelationAnalysisResponseDto;
import com.investment.analysis.dto.SectorAnalysisResponseDto;
import com.investment.analysis.service.AnalysisService;
import com.investment.analysis.service.CorrelationAnalysisService;
import com.investment.analysis.service.SectorAnalysisService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    private SectorAnalysisService sectorAnalysisService;
    @MockBean
    private CorrelationAnalysisService correlationAnalysisService;
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

    @Test
    @DisplayName("GET /api/v1/analysis/sector symbols 파라미터로 섹터 분석 반환")
    void getSectorAnalysis_withSymbols_returnsOk() throws Exception {
        SectorAnalysisResponseDto response = SectorAnalysisResponseDto.builder()
                .market("US")
                .totalValue(BigDecimal.valueOf(2))
                .sectors(List.of(
                        SectorAnalysisResponseDto.SectorWeightItem.builder()
                                .sectorCode("TECH")
                                .sectorName("TECH")
                                .weightPct(BigDecimal.valueOf(50))
                                .notionalValue(BigDecimal.ONE)
                                .build()))
                .build();
        when(sectorAnalysisService.getSectorAnalysisBySymbols(eq("US"), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/analysis/sector").param("symbols", "AAPL,MSFT").param("market", "US"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.market").value("US"))
                .andExpect(jsonPath("$.sectors.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/analysis/correlation symbols 파라미터로 상관관계 분석 반환")
    void getCorrelationAnalysis_withSymbols_returnsOk() throws Exception {
        CorrelationAnalysisResponseDto response = CorrelationAnalysisResponseDto.builder()
                .market("US")
                .symbols(List.of("AAPL", "MSFT"))
                .fromDate(LocalDate.now().minusDays(60))
                .toDate(LocalDate.now())
                .matrix(List.of(
                        List.of(1.0, 0.5),
                        List.of(0.5, 1.0)))
                .build();
        when(correlationAnalysisService.getCorrelationBySymbols(any(), eq("US"), any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/analysis/correlation")
                        .param("symbols", "AAPL,MSFT")
                        .param("market", "US"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.market").value("US"))
                .andExpect(jsonPath("$.symbols.length()").value(2))
                .andExpect(jsonPath("$.matrix.length()").value(2));
    }
}
