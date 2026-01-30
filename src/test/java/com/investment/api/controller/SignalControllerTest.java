package com.investment.api.controller;

import com.investment.common.security.JwtAuthenticationFilter;
import com.investment.common.security.RateLimitFilter;
import com.investment.config.SecurityHeadersConfig;
import com.investment.factor.dto.SignalScoreDto;
import com.investment.factor.dto.SignalScorePageResponseDto;
import com.investment.factor.service.SignalScoreService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SignalController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SignalController")
class SignalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SignalScoreService signalScoreService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private RateLimitFilter rateLimitFilter;
    @MockBean
    private SecurityHeadersConfig securityHeadersConfig;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/v1/signals 시그널 목록 조회 성공")
    void getSignals_returnsOk() throws Exception {
        SignalScorePageResponseDto response = SignalScorePageResponseDto.builder()
                .content(List.of(
                        SignalScoreDto.builder()
                                .basDt(LocalDate.of(2026, 1, 29))
                                .symbol("005930")
                                .market("KR")
                                .factorType("DISPARITY")
                                .score(BigDecimal.valueOf(102.5))
                                .build()))
                .page(SignalScorePageResponseDto.PageMeta.builder()
                        .number(0)
                        .size(20)
                        .totalElements(1)
                        .totalPages(1)
                        .build())
                .build();
        when(signalScoreService.getSignals(any(), any(), any(), any(), eq(0), eq(20)))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/signals")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].symbol").value("005930"))
                .andExpect(jsonPath("$.content[0].factorType").value("DISPARITY"))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(20));
    }

    @Test
    @DisplayName("GET /api/v1/signals basDt market 파라미터 전달 시 서비스에 전달한다")
    void getSignals_withFilters_callsServiceWithFilters() throws Exception {
        SignalScorePageResponseDto response = SignalScorePageResponseDto.builder()
                .content(List.of())
                .page(SignalScorePageResponseDto.PageMeta.builder().number(0).size(20).totalElements(0).totalPages(0).build())
                .build();
        when(signalScoreService.getSignals(eq(LocalDate.of(2026, 1, 29)), eq("KR"), isNull(), isNull(), anyInt(), anyInt()))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/signals")
                        .param("basDt", "2026-01-29")
                        .param("market", "KR"))
                .andExpect(status().isOk());
    }
}
