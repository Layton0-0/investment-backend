package com.investment.api.controller;

import com.investment.common.security.JwtAuthenticationFilter;
import com.investment.common.security.RateLimitFilter;
import com.investment.config.SecurityHeadersConfig;
import com.investment.strategy.domain.StrategyStatus;
import com.investment.strategy.domain.StrategyType;
import com.investment.strategy.dto.StrategyDto;
import com.investment.strategy.service.StrategyManagementService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StrategyApiController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("StrategyApiController")
class StrategyApiControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private StrategyManagementService strategyManagementService;
        @MockBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;
        @MockBean
        private RateLimitFilter rateLimitFilter;
        @MockBean
        private SecurityHeadersConfig securityHeadersConfig;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        @DisplayName("GET /api/v1/strategies/{accountNo} 전략 목록 조회 성공")
        void getStrategies_returnsOk() throws Exception {
                StrategyDto dto = StrategyDto.builder()
                                .strategyId("s1")
                                .accountNo("12345678-12")
                                .market("KR")
                                .strategyType(StrategyType.SHORT_TERM)
                                .status(StrategyStatus.ACTIVE)
                                .build();
                when(strategyManagementService.getStrategies(eq("12345678-12"), any()))
                                .thenReturn(List.of(dto));

                mockMvc.perform(get("/api/v1/strategies/12345678-12"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].strategyId").value("s1"))
                                .andExpect(jsonPath("$[0].market").value("KR"));
        }

        @Test
        @DisplayName("GET /api/v1/strategies/{accountNo}/{strategyType} 전략 상세 조회 성공")
        void getStrategy_returnsOk() throws Exception {
                StrategyDto dto = StrategyDto.builder()
                                .strategyId("s1")
                                .accountNo("12345678-12")
                                .market("KR")
                                .strategyType(StrategyType.SHORT_TERM)
                                .status(StrategyStatus.ACTIVE)
                                .build();
                when(strategyManagementService.getStrategy(eq("12345678-12"), any(), eq(StrategyType.SHORT_TERM)))
                                .thenReturn(dto);

                mockMvc.perform(get("/api/v1/strategies/12345678-12/SHORT_TERM"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.strategyId").value("s1"))
                                .andExpect(jsonPath("$.strategyType").value("SHORT_TERM"));
        }

        @Test
        @DisplayName("GET /api/v1/strategies/{accountNo} market=US 파라미터 시 시장별 조회")
        void getStrategies_withMarketUS_callsServiceWithMarket() throws Exception {
                StrategyDto dto = StrategyDto.builder()
                                .strategyId("s2")
                                .accountNo("12345678-12")
                                .market("US")
                                .strategyType(StrategyType.MEDIUM_TERM)
                                .status(StrategyStatus.ACTIVE)
                                .build();
                when(strategyManagementService.getStrategies(eq("12345678-12"), eq("US")))
                                .thenReturn(List.of(dto));

                mockMvc.perform(get("/api/v1/strategies/12345678-12").param("market", "US"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].market").value("US"));
        }

        @Test
        @DisplayName("GET /api/v1/strategies/{accountNo}/{strategyType} market=KR 파라미터 시 전략 상세 조회")
        void getStrategy_withMarketKR_returnsOk() throws Exception {
                StrategyDto dto = StrategyDto.builder()
                                .strategyId("s1")
                                .accountNo("12345678-12")
                                .market("KR")
                                .strategyType(StrategyType.SHORT_TERM)
                                .status(StrategyStatus.ACTIVE)
                                .build();
                when(strategyManagementService.getStrategy(eq("12345678-12"), eq("KR"), eq(StrategyType.SHORT_TERM)))
                                .thenReturn(dto);

                mockMvc.perform(get("/api/v1/strategies/12345678-12/SHORT_TERM").param("market", "KR"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.market").value("KR"));
        }

        @Test
        @DisplayName("POST /api/v1/strategies 전략 생성/수정 성공")
        void createOrUpdateStrategy_returnsOk() throws Exception {
                StrategyDto request = StrategyDto.builder()
                                .accountNo("12345678-12")
                                .market("KR")
                                .strategyType(StrategyType.SHORT_TERM)
                                .status(StrategyStatus.ACTIVE)
                                .maxInvestmentAmount(new BigDecimal("1000000"))
                                .build();
                StrategyDto response = StrategyDto.builder()
                                .strategyId("new-id")
                                .accountNo("12345678-12")
                                .market("KR")
                                .strategyType(StrategyType.SHORT_TERM)
                                .status(StrategyStatus.ACTIVE)
                                .build();
                when(strategyManagementService.saveStrategy(any(StrategyDto.class))).thenReturn(response);

                mockMvc.perform(post("/api/v1/strategies")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.strategyId").value("new-id"));
        }

        @Test
        @DisplayName("PUT /api/v1/strategies/{accountNo}/{strategyType}/status 전략 상태 변경 성공")
        void updateStrategyStatus_returnsOk() throws Exception {
                com.investment.strategy.dto.StrategyStatusUpdateDto updateDto =
                                com.investment.strategy.dto.StrategyStatusUpdateDto.builder()
                                                .status(StrategyStatus.PAUSED)
                                                .build();
                StrategyDto response = StrategyDto.builder()
                                .strategyId("s1")
                                .accountNo("12345678-12")
                                .market("KR")
                                .strategyType(StrategyType.SHORT_TERM)
                                .status(StrategyStatus.PAUSED)
                                .build();
                when(strategyManagementService.updateStrategyStatus(eq("12345678-12"), any(), eq(StrategyType.SHORT_TERM), any()))
                                .thenReturn(response);

                mockMvc.perform(put("/api/v1/strategies/12345678-12/SHORT_TERM/status")
                                .param("market", "KR")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateDto)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("PAUSED"));
        }

        @Test
        @DisplayName("POST /api/v1/strategies/{accountNo}/{strategyType}/activate 전략 활성화 성공")
        void activateStrategy_returnsOk() throws Exception {
                StrategyDto response = StrategyDto.builder()
                                .strategyId("s1")
                                .accountNo("12345678-12")
                                .market("KR")
                                .strategyType(StrategyType.SHORT_TERM)
                                .status(StrategyStatus.ACTIVE)
                                .build();
                when(strategyManagementService.activateStrategy(eq("12345678-12"), any(), eq(StrategyType.SHORT_TERM)))
                                .thenReturn(response);

                mockMvc.perform(post("/api/v1/strategies/12345678-12/SHORT_TERM/activate").param("market", "KR"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("POST /api/v1/strategies/{accountNo}/{strategyType}/stop 전략 중지 성공")
        void stopStrategy_returnsOk() throws Exception {
                StrategyDto response = StrategyDto.builder()
                                .strategyId("s1")
                                .accountNo("12345678-12")
                                .market("KR")
                                .strategyType(StrategyType.SHORT_TERM)
                                .status(StrategyStatus.STOPPED)
                                .build();
                when(strategyManagementService.stopStrategy(eq("12345678-12"), any(), eq(StrategyType.SHORT_TERM)))
                                .thenReturn(response);

                mockMvc.perform(post("/api/v1/strategies/12345678-12/SHORT_TERM/stop").param("market", "KR"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("STOPPED"));
        }
}
