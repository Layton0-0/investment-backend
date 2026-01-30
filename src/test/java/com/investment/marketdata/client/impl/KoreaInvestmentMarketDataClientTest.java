package com.investment.marketdata.client.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.investment.marketdata.client.IndicatorResponse;
import com.investment.marketdata.config.MarketDataProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * KoreaInvestmentMarketDataClient 테스트
 * 
 * 주의: 실제 API 호출을 모킹하는 것은 복잡하므로,
 * 모의 데이터 모드와 기본 기능에 집중합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KoreaInvestmentMarketDataClient 테스트")
class KoreaInvestmentMarketDataClientTest {

    @Mock
    private MarketDataProperties properties;

    @Mock
    private WebClient webClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private KoreaInvestmentMarketDataClient client;

    private MarketDataProperties.KoreaInvestmentProperties kiProps;

    @BeforeEach
    void setUp() {
        kiProps = new MarketDataProperties.KoreaInvestmentProperties();
        kiProps.setAppKey("test-app-key");
        kiProps.setAppSecret("test-app-secret");
        kiProps.setServerType("1"); // 모의투자

        lenient().when(properties.getKoreaInvestment()).thenReturn(kiProps);
        lenient().when(properties.getTimeout()).thenReturn(30000);
        lenient().when(properties.isUseMockData()).thenReturn(true);
    }

    @Test
    @DisplayName("모의 데이터 모드 - RSI 지표 조회")
    void getIndicator_모의데이터_RSI() {
        // given
        String indicator = "rsi";
        String symbol = "005930"; // 삼성전자
        String interval = "1d";

        when(properties.isUseMockData()).thenReturn(true);

        // when
        Mono<IndicatorResponse> result = client.getIndicator(indicator, symbol, interval);

        // then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertNotNull(response.getValue());
                    assertTrue(response.getValue().compareTo(BigDecimal.ZERO) > 0);
                    assertTrue(response.getValue().compareTo(new BigDecimal("100")) < 0);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("모의 데이터 모드 - MACD 지표 조회")
    void getIndicator_모의데이터_MACD() {
        // given
        String indicator = "macd";
        String symbol = "005930";
        String interval = "1d";

        when(properties.isUseMockData()).thenReturn(true);

        // when
        Mono<IndicatorResponse> result = client.getIndicator(indicator, symbol, interval);

        // then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertNotNull(response.getValueMacd());
                    assertNotNull(response.getValueMacdSignal());
                    assertNotNull(response.getValueMacdHist());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("모의 데이터 모드 - EMA 지표 조회")
    void getIndicator_모의데이터_EMA() {
        // given
        String indicator = "ema";
        String symbol = "005930";
        String interval = "1d";

        when(properties.isUseMockData()).thenReturn(true);

        // when
        Mono<IndicatorResponse> result = client.getIndicator(indicator, symbol, interval);

        // then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertNotNull(response.getValues());
                    assertEquals(3, response.getValues().length); // EMA20, EMA60, EMA120
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("모의 데이터 모드 - Bollinger Bands 지표 조회")
    void getIndicator_모의데이터_BBANDS() {
        // given
        String indicator = "bbands";
        String symbol = "005930";
        String interval = "1d";

        when(properties.isUseMockData()).thenReturn(true);

        // when
        Mono<IndicatorResponse> result = client.getIndicator(indicator, symbol, interval);

        // then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertNotNull(response.getValueUpperBand());
                    assertNotNull(response.getValueMiddleBand());
                    assertNotNull(response.getValueLowerBand());
                    // Upper > Middle > Lower
                    assertTrue(response.getValueUpperBand().compareTo(response.getValueMiddleBand()) > 0);
                    assertTrue(response.getValueMiddleBand().compareTo(response.getValueLowerBand()) > 0);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("모의 데이터 모드 - ATR 지표 조회")
    void getIndicator_모의데이터_ATR() {
        // given
        String indicator = "atr";
        String symbol = "005930";
        String interval = "1d";

        when(properties.isUseMockData()).thenReturn(true);

        // when
        Mono<IndicatorResponse> result = client.getIndicator(indicator, symbol, interval);

        // then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertNotNull(response.getValueAtr());
                    assertTrue(response.getValueAtr().compareTo(BigDecimal.ZERO) > 0);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("모의 데이터 모드 - VWAP 지표 조회")
    void getIndicator_모의데이터_VWAP() {
        // given
        String indicator = "vwap";
        String symbol = "005930";
        String interval = "1d";

        when(properties.isUseMockData()).thenReturn(true);

        // when
        Mono<IndicatorResponse> result = client.getIndicator(indicator, symbol, interval);

        // then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertNotNull(response.getValue());
                    assertTrue(response.getValue().compareTo(BigDecimal.ZERO) > 0);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("모의 데이터 모드 - 지원하지 않는 지표")
    void getIndicator_모의데이터_지원하지_않는_지표() {
        // given
        String indicator = "unknown";
        String symbol = "005930";
        String interval = "1d";

        when(properties.isUseMockData()).thenReturn(true);

        // when
        Mono<IndicatorResponse> result = client.getIndicator(indicator, symbol, interval);

        // then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertNotNull(response.getError());
                    assertTrue(response.getError().contains("지원하지 않는 지표"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("모의 데이터 모드 - Bulk 지표 조회")
    void getBulkIndicators_모의데이터() {
        // given
        String symbol = "005930";
        String interval = "1d";
        String[] indicators = { "rsi", "macd", "ema" };

        when(properties.isUseMockData()).thenReturn(true);

        // when
        Mono<java.util.Map<String, IndicatorResponse>> result = client.getBulkIndicators(symbol, interval, indicators);

        // then
        StepVerifier.create(result)
                .assertNext(map -> {
                    assertNotNull(map);
                    assertEquals(3, map.size());
                    assertTrue(map.containsKey("rsi"));
                    assertTrue(map.containsKey("macd"));
                    assertTrue(map.containsKey("ema"));
                    assertNotNull(map.get("rsi").getValue());
                    assertNotNull(map.get("macd").getValueMacd());
                    assertNotNull(map.get("ema").getValues());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Provider 이름 확인")
    void getProviderName() {
        // when
        String providerName = client.getProviderName();

        // then
        assertEquals("Korea Investment", providerName);
    }

    @Test
    @DisplayName("종목명을 종목코드로 변환")
    void getIndicator_종목명_변환() {
        // given
        String indicator = "rsi";
        String symbol = "삼성전자"; // 종목명
        String interval = "1d";

        when(properties.isUseMockData()).thenReturn(true);

        // when
        Mono<IndicatorResponse> result = client.getIndicator(indicator, symbol, interval);

        // then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertNotNull(response.getValue());
                })
                .verifyComplete();
    }
}
