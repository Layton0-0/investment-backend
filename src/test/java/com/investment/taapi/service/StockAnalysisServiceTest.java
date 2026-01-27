package com.investment.taapi.service;

import com.investment.marketdata.client.IndicatorResponse;
import com.investment.marketdata.client.MarketDataClient;
import com.investment.taapi.dto.StockAnalysisDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * StockAnalysisService 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StockAnalysisService 테스트")
class StockAnalysisServiceTest {
    
    @Mock
    private MarketDataClient marketDataClient;
    
    @InjectMocks
    private StockAnalysisService stockAnalysisService;
    
    private Map<String, IndicatorResponse> indicators;
    
    @BeforeEach
    void setUp() {
        indicators = new HashMap<>();
    }
    
    @Test
    @DisplayName("종목 분석 성공 - 모든 지표 포함")
    void analyzeStock_성공_모든_지표() {
        // given
        String symbol = "005930"; // 삼성전자
        String interval = "1d";
        
        // RSI 응답
        IndicatorResponse rsiResponse = IndicatorResponse.builder()
                .value(new BigDecimal("55.5"))
                .build();
        
        // MACD 응답
        IndicatorResponse macdResponse = IndicatorResponse.builder()
                .valueMacd(new BigDecimal("1.2"))
                .valueMacdSignal(new BigDecimal("1.0"))
                .valueMacdHist(new BigDecimal("0.2"))
                .build();
        
        // EMA 응답
        IndicatorResponse emaResponse = IndicatorResponse.builder()
                .values(new BigDecimal[]{
                        new BigDecimal("50000"),  // EMA20
                        new BigDecimal("51000"),  // EMA60
                        new BigDecimal("52000")   // EMA120
                })
                .build();
        
        indicators.put("rsi", rsiResponse);
        indicators.put("macd", macdResponse);
        indicators.put("ema", emaResponse);
        
        when(marketDataClient.getBulkIndicators(eq(symbol), eq(interval), anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(indicators));
        when(marketDataClient.getProviderName()).thenReturn("Korea Investment");
        
        // when
        Mono<StockAnalysisDto> result = stockAnalysisService.analyzeStock(symbol, interval);
        
        // then
        StepVerifier.create(result)
                .assertNext(analysis -> {
                    assertNotNull(analysis);
                    assertEquals(symbol, analysis.getSymbol());
                    assertEquals("삼성전자", analysis.getName());
                    assertEquals(new BigDecimal("55.5"), analysis.getRsi());
                    assertEquals(new BigDecimal("1.2"), analysis.getMacd());
                    assertEquals(new BigDecimal("1.0"), analysis.getMacdSignal());
                    assertEquals(new BigDecimal("0.2"), analysis.getMacdHist());
                    assertEquals(new BigDecimal("50000"), analysis.getEma20());
                    assertEquals(new BigDecimal("51000"), analysis.getEma60());
                    assertEquals(new BigDecimal("52000"), analysis.getEma120());
                    assertEquals(new BigDecimal("50000"), analysis.getCurrentPrice());
                    assertFalse(analysis.isGoldenCross()); // EMA20 < EMA60
                    assertFalse(analysis.isBreakout());
                })
                .verifyComplete();
        
        verify(marketDataClient).getBulkIndicators(eq(symbol), eq(interval), eq("rsi"), eq("macd"), eq("ema"));
    }
    
    @Test
    @DisplayName("종목 분석 성공 - 골든크로스 발생")
    void analyzeStock_성공_골든크로스() {
        // given
        String symbol = "005930";
        String interval = "1d";
        
        IndicatorResponse rsiResponse = IndicatorResponse.builder()
                .value(new BigDecimal("60.0"))
                .build();
        
        IndicatorResponse macdResponse = IndicatorResponse.builder()
                .valueMacd(new BigDecimal("2.0"))
                .valueMacdSignal(new BigDecimal("1.5"))
                .valueMacdHist(new BigDecimal("0.5"))
                .build();
        
        // EMA20 > EMA60 (골든크로스)
        IndicatorResponse emaResponse = IndicatorResponse.builder()
                .values(new BigDecimal[]{
                        new BigDecimal("52000"),  // EMA20
                        new BigDecimal("51000"),  // EMA60
                        new BigDecimal("50000")   // EMA120
                })
                .build();
        
        indicators.put("rsi", rsiResponse);
        indicators.put("macd", macdResponse);
        indicators.put("ema", emaResponse);
        
        when(marketDataClient.getBulkIndicators(eq(symbol), eq(interval), anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(indicators));
        when(marketDataClient.getProviderName()).thenReturn("Korea Investment");
        
        // when
        Mono<StockAnalysisDto> result = stockAnalysisService.analyzeStock(symbol, interval);
        
        // then
        StepVerifier.create(result)
                .assertNext(analysis -> {
                    assertTrue(analysis.isGoldenCross());
                    assertTrue(analysis.isBreakout());
                })
                .verifyComplete();
    }
    
    @Test
    @DisplayName("종목 분석 성공 - 일부 지표만 포함")
    void analyzeStock_성공_일부_지표() {
        // given
        String symbol = "005930";
        String interval = "1d";
        
        IndicatorResponse rsiResponse = IndicatorResponse.builder()
                .value(new BigDecimal("50.0"))
                .build();
        
        indicators.put("rsi", rsiResponse);
        // macd, ema는 없음
        
        when(marketDataClient.getBulkIndicators(eq(symbol), eq(interval), anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(indicators));
        when(marketDataClient.getProviderName()).thenReturn("Korea Investment");
        
        // when
        Mono<StockAnalysisDto> result = stockAnalysisService.analyzeStock(symbol, interval);
        
        // then
        StepVerifier.create(result)
                .assertNext(analysis -> {
                    assertNotNull(analysis);
                    assertEquals(new BigDecimal("50.0"), analysis.getRsi());
                    assertNull(analysis.getMacd());
                    assertNull(analysis.getEma20());
                })
                .verifyComplete();
    }
    
    @Test
    @DisplayName("종목 분석 실패 - 에러 발생 시 기본값 반환")
    void analyzeStock_실패_에러_발생() {
        // given
        String symbol = "005930";
        String interval = "1d";
        
        when(marketDataClient.getBulkIndicators(eq(symbol), eq(interval), anyString(), anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("API 호출 실패")));
        
        // when
        Mono<StockAnalysisDto> result = stockAnalysisService.analyzeStock(symbol, interval);
        
        // then
        StepVerifier.create(result)
                .assertNext(analysis -> {
                    assertNotNull(analysis);
                    assertEquals(symbol, analysis.getSymbol());
                    assertEquals("삼성전자", analysis.getName());
                    // 에러 발생 시 기본값만 설정됨
                })
                .verifyComplete();
    }
    
    @Test
    @DisplayName("종목 분석 성공 - 단일 EMA 값")
    void analyzeStock_성공_단일_EMA() {
        // given
        String symbol = "005930";
        String interval = "1d";
        
        IndicatorResponse rsiResponse = IndicatorResponse.builder()
                .value(new BigDecimal("50.0"))
                .build();
        
        // 단일 EMA 값 (values 배열이 아닌 value 사용)
        IndicatorResponse emaResponse = IndicatorResponse.builder()
                .value(new BigDecimal("50000"))
                .build();
        
        indicators.put("rsi", rsiResponse);
        indicators.put("ema", emaResponse);
        
        when(marketDataClient.getBulkIndicators(eq(symbol), eq(interval), anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(indicators));
        when(marketDataClient.getProviderName()).thenReturn("Korea Investment");
        
        // when
        Mono<StockAnalysisDto> result = stockAnalysisService.analyzeStock(symbol, interval);
        
        // then
        StepVerifier.create(result)
                .assertNext(analysis -> {
                    assertEquals(new BigDecimal("50000"), analysis.getEma20());
                    assertEquals(new BigDecimal("50000"), analysis.getCurrentPrice());
                })
                .verifyComplete();
    }
}
