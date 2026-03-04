package com.investment.api.controller;

import com.investment.account.service.AccountService;
import com.investment.backtest.BacktestService;
import com.investment.backtest.WalkForwardBacktestService;
import com.investment.backtest.dto.BacktestRunRequest;
import com.investment.backtest.dto.BacktestRunResult;
import com.investment.backtest.dto.WalkForwardBacktestRequest;
import com.investment.backtest.dto.WalkForwardBacktestResult;
import com.investment.backtest.robo.RoboBacktestService;
import com.investment.backtest.robo.RoboPreExecutionResultStore;
import com.investment.common.security.JwtAuthenticationFilter;
import com.investment.common.security.RateLimitFilter;
import com.investment.config.RoboBacktestProperties;
import com.investment.config.SecurityHeadersConfig;
import com.investment.datacollection.service.UsMarketCollectionService;
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
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BacktestController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("BacktestController")
class BacktestControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private BacktestService backtestService;
        @MockBean
        private WalkForwardBacktestService walkForwardBacktestService;
        @MockBean
        private RoboBacktestService roboBacktestService;
        @MockBean
        private AccountService accountService;
        @MockBean
        private RoboPreExecutionResultStore roboPreExecutionResultStore;
        @MockBean
        private UsMarketCollectionService usMarketCollectionService;
        @MockBean
        private RoboBacktestProperties roboBacktestProperties;
        @MockBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;
        @MockBean
        private RateLimitFilter rateLimitFilter;
        @MockBean
        private SecurityHeadersConfig securityHeadersConfig;

        @Test
        @DisplayName("POST /api/v1/backtest 성공 시 200 및 결과 필드 반환")
        void run_returnsOkWithResult() throws Exception {
                BacktestRunRequest request = BacktestRunRequest.builder()
                                .startDate(LocalDate.of(2025, 1, 6))
                                .endDate(LocalDate.of(2025, 1, 10))
                                .market("KR")
                                .strategyType("SHORT_TERM")
                                .initialCapital(new BigDecimal("100000000"))
                                .build();

                BacktestRunResult result = BacktestRunResult.builder()
                                .startDate(request.getStartDate())
                                .endDate(request.getEndDate())
                                .market(request.getMarket())
                                .strategyType(request.getStrategyType())
                                .initialCapital(request.getInitialCapital())
                                .finalEquity(new BigDecimal("105000000"))
                                .totalReturnPct(new BigDecimal("5.0"))
                                .tradeCount(2)
                                .equityCurve(Collections.emptyList())
                                .trades(Collections.emptyList())
                                .build();

                when(backtestService.run(any(BacktestRunRequest.class))).thenReturn(result);

                mockMvc.perform(post("/api/v1/backtest")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.startDate").value("2025-01-06"))
                                .andExpect(jsonPath("$.endDate").value("2025-01-10"))
                                .andExpect(jsonPath("$.market").value("KR"))
                                .andExpect(jsonPath("$.finalEquity").value(105000000))
                                .andExpect(jsonPath("$.tradeCount").value(2));
        }

        @Test
        @DisplayName("POST /api/v1/backtest/walk-forward 성공 시 200 및 foldCount·avgCagr·avgMddPct·minSharpeRatio 반환")
        void runWalkForward_returnsOkWithAggregatedMetrics() throws Exception {
                WalkForwardBacktestRequest request = WalkForwardBacktestRequest.builder()
                                .startDate(LocalDate.of(2024, 1, 1))
                                .endDate(LocalDate.of(2024, 12, 31))
                                .market("KR")
                                .strategyType("SHORT_TERM")
                                .initialCapital(new BigDecimal("100000000"))
                                .trainDays(252)
                                .testDays(63)
                                .stepDays(63)
                                .build();

                WalkForwardBacktestResult result = WalkForwardBacktestResult.builder()
                                .startDate(request.getStartDate())
                                .endDate(request.getEndDate())
                                .market(request.getMarket())
                                .strategyType(request.getStrategyType())
                                .trainDays(252)
                                .testDays(63)
                                .stepDays(63)
                                .foldCount(2)
                                .avgCagr(new BigDecimal("22.5"))
                                .avgMddPct(new BigDecimal("-8.0"))
                                .minSharpeRatio(new BigDecimal("1.1"))
                                .avgSharpeRatio(new BigDecimal("1.2"))
                                .build();

                when(walkForwardBacktestService.run(any(WalkForwardBacktestRequest.class))).thenReturn(result);

                mockMvc.perform(post("/api/v1/backtest/walk-forward")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.foldCount").value(2))
                                .andExpect(jsonPath("$.avgCagr").value(22.5))
                                .andExpect(jsonPath("$.avgMddPct").value(-8.0))
                                .andExpect(jsonPath("$.minSharpeRatio").value(1.1));
        }
}
