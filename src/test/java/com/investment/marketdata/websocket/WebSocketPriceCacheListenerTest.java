package com.investment.marketdata.websocket;

import com.investment.marketdata.config.MarketDataProperties;
import com.investment.marketdata.dto.CurrentPriceDto;
import com.investment.marketdata.service.RealtimeMarketDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketPriceCacheListener")
class WebSocketPriceCacheListenerTest {

    @Mock
    private RealtimeMarketDataService realtimeMarketDataService;

    private MarketDataProperties marketDataProperties;
    private WebSocketPriceCacheListener listener;

    @BeforeEach
    void setUp() {
        marketDataProperties = new MarketDataProperties();
        var korea = new MarketDataProperties.KoreaInvestmentProperties();
        var ws = new MarketDataProperties.KoreaInvestmentProperties.WebSocketProperties();
        ws.setQuoteTrId("H0STASP0");
        ws.setExecutionTrId("H0STCNT0");
        korea.setWebsocket(ws);
        marketDataProperties.setKoreaInvestment(korea);
        listener = new WebSocketPriceCacheListener(marketDataProperties, realtimeMarketDataService,
                new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @Test
    @DisplayName("호가 tr_id + 파이프 데이터 시 updateFromWebSocket 호출")
    void onWebSocketData_quoteTrId_pipedData_callsUpdateFromWebSocket() {
        WebSocketDataEvent event = new WebSocketDataEvent(this, "u1|1", "H0STASP0", "",
                "005930|70000|69900|70100");
        listener.onWebSocketData(event);

        ArgumentCaptor<String> symbolCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CurrentPriceDto> dtoCaptor = ArgumentCaptor.forClass(CurrentPriceDto.class);
        verify(realtimeMarketDataService).updateFromWebSocket(symbolCaptor.capture(), dtoCaptor.capture());
        assertThat(symbolCaptor.getValue()).isEqualTo("005930");
        assertThat(dtoCaptor.getValue().getCurrentPrice()).isEqualByComparingTo(new BigDecimal("70000"));
    }

    @Test
    @DisplayName("체결 tr_id + JSON 데이터 시 updateFromWebSocket 호출")
    void onWebSocketData_executionTrId_jsonData_callsUpdateFromWebSocket() {
        String json = "{\"body\":{\"pdno\":\"005930\",\"stck_prpr\":\"71500\"}}";
        WebSocketDataEvent event = new WebSocketDataEvent(this, "u1|1", "H0STCNT0", "", json);
        listener.onWebSocketData(event);

        ArgumentCaptor<String> symbolCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CurrentPriceDto> dtoCaptor = ArgumentCaptor.forClass(CurrentPriceDto.class);
        verify(realtimeMarketDataService).updateFromWebSocket(symbolCaptor.capture(), dtoCaptor.capture());
        assertThat(symbolCaptor.getValue()).isEqualTo("005930");
        assertThat(dtoCaptor.getValue().getCurrentPrice()).isEqualByComparingTo(new BigDecimal("71500"));
    }

    @Test
    @DisplayName("다른 tr_id 시 updateFromWebSocket 미호출")
    void onWebSocketData_otherTrId_doesNotCallUpdate() {
        WebSocketDataEvent event = new WebSocketDataEvent(this, "u1|1", "H0STCNI0", "", "005930|70000");
        listener.onWebSocketData(event);
        verify(realtimeMarketDataService, never()).updateFromWebSocket(any(String.class), any());
    }
}
