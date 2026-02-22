package com.investment.api.controller;

import com.investment.account.service.AccountService;
import com.investment.common.security.JwtAuthenticationFilter;
import com.investment.common.security.RateLimitFilter;
import com.investment.config.SecurityHeadersConfig;
import com.investment.marketdata.dto.CurrentPriceDto;
import com.investment.marketdata.service.DailyChartService;
import com.investment.marketdata.service.RealtimeMarketDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MarketDataController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("MarketDataController")
class MarketDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RealtimeMarketDataService realtimeMarketDataService;
    @MockBean
    private DailyChartService dailyChartService;
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
    @DisplayName("GET /api/v1/market-data/current-price/{symbol} 단일 현재가 조회 성공")
    void getCurrentPrice_returnsOk() throws Exception {
        CurrentPriceDto dto = CurrentPriceDto.builder()
                .symbol("005930")
                .name("삼성전자")
                .currentPrice(BigDecimal.valueOf(75000))
                .queriedAt(LocalDateTime.now())
                .build();
        when(realtimeMarketDataService.getCurrentPrice("005930")).thenReturn(Mono.just(dto));

        mockMvc.perform(get("/api/v1/market-data/current-price/005930"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("005930"))
                .andExpect(jsonPath("$.currentPrice").value(75000));
    }

    @Test
    @DisplayName("POST /api/v1/market-data/current-prices 여러 종목 현재가 일괄 조회 성공")
    void getCurrentPrices_returnsOk() throws Exception {
        CurrentPriceDto dto = CurrentPriceDto.builder()
                .symbol("005930")
                .name("삼성전자")
                .currentPrice(BigDecimal.valueOf(75000))
                .queriedAt(LocalDateTime.now())
                .build();
        when(realtimeMarketDataService.getCurrentPrices(anyList())).thenReturn(Mono.just(List.of(dto)));

        mockMvc.perform(post("/api/v1/market-data/current-prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of("005930", "000660"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].symbol").value("005930"));
    }
}
