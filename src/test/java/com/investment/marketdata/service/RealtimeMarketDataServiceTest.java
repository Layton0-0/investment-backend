package com.investment.marketdata.service;

import com.investment.marketdata.client.impl.KoreaInvestmentMarketDataClient;
import com.investment.marketdata.dto.CurrentPriceDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 실시간 시세 조회 서비스 테스트
 */
@ExtendWith(MockitoExtension.class)
class RealtimeMarketDataServiceTest {
    
    @Mock
    private KoreaInvestmentMarketDataClient marketDataClient;
    
    private RealtimeMarketDataService realtimeMarketDataService;
    
    @BeforeEach
    void setUp() {
        realtimeMarketDataService = new RealtimeMarketDataService(marketDataClient);
    }
    
    @Test
    void 서비스_생성_성공() {
        assertNotNull(realtimeMarketDataService);
    }
    
    @Test
    void getCurrentPriceBlocking_WebSocket에_값이_있으면_해당_값_반환() {
        // given: WebSocket으로 수신한 현재가를 먼저 반영
        CurrentPriceDto wsPrice = CurrentPriceDto.builder()
                .symbol("005930")
                .currentPrice(new BigDecimal("75100"))
                .queriedAt(LocalDateTime.now())
                .build();
        realtimeMarketDataService.updateFromWebSocket("005930", wsPrice);

        // when
        CurrentPriceDto result = realtimeMarketDataService.getCurrentPriceBlocking("005930");

        // then: REST 호출 없이 WebSocket 값이 반환됨
        assertNotNull(result);
        assertEquals("005930", result.getSymbol());
        assertEquals(new BigDecimal("75100"), result.getCurrentPrice());
    }

    @Test
    void 현재가_조회_성공() {
        // given
        CurrentPriceDto mockPrice = CurrentPriceDto.builder()
                .symbol("005930")
                .name("삼성전자")
                .currentPrice(new BigDecimal("75000"))
                .changeRate(new BigDecimal("1.5"))
                .changeAmount(new BigDecimal("750"))
                .previousClose(new BigDecimal("49250"))
                .openPrice(new BigDecimal("49300"))
                .highPrice(new BigDecimal("50200"))
                .lowPrice(new BigDecimal("49200"))
                .volume(1000000L)
                .tradingValue(new BigDecimal("50000000000"))
                .marketCap(new BigDecimal("1000000000000"))
                .listedShares(20000000L)
                .queriedAt(LocalDateTime.now())
                .build();
        
        when(marketDataClient.getCurrentPrice(anyString()))
                .thenReturn(Mono.just(mockPrice));
        
        // when
        CurrentPriceDto result = realtimeMarketDataService.getCurrentPrice("005930")
                .block(java.time.Duration.ofSeconds(5));
        
        // then
        assertNotNull(result);
        assertEquals("005930", result.getSymbol());
        assertEquals("삼성전자", result.getName());
        assertEquals(new BigDecimal("75000"), result.getCurrentPrice());
    }
    
    // 실제 API 호출 테스트는 통합 테스트에서 수행
}
