package com.investment.marketdata.websocket;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * WebSocket 수신 시 RealtimeMarketDataService.updateFromWebSocket 호출 검증.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketPriceCacheListener")
class WebSocketPriceCacheListenerTest {

    @Mock
    private RealtimeMarketDataService realtimeMarketDataService;

    private WebSocketPriceCacheListener listener;

    @BeforeEach
    void setUp() {
        listener = new WebSocketPriceCacheListener(realtimeMarketDataService);
    }

    @Test
    @DisplayName("호가 데이터(H0STASP0) 수신 시 updateFromWebSocket 호출")
    void onWebSocketData_quoteData_callsUpdateFromWebSocket() {
        WebSocketDataEvent event = new WebSocketDataEvent(
                this, "user1|1", "H0STASP0", "005930", "005930|75100|0|0|75200|75000");
        listener.onWebSocketData(event);

        ArgumentCaptor<CurrentPriceDto> dtoCaptor = ArgumentCaptor.forClass(CurrentPriceDto.class);
        verify(realtimeMarketDataService).updateFromWebSocket(eq("005930"), dtoCaptor.capture());
        CurrentPriceDto captured = dtoCaptor.getValue();
        assertThat(captured.getSymbol()).isEqualTo("005930");
        assertThat(captured.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("75100"));
    }

    @Test
    @DisplayName("체결 데이터(H0STCNT0) 수신 시 updateFromWebSocket 호출")
    void onWebSocketData_executionData_callsUpdateFromWebSocket() {
        WebSocketDataEvent event = new WebSocketDataEvent(
                this, "user1|1", "H0STCNT0", "005930", "005930|75200");
        listener.onWebSocketData(event);

        ArgumentCaptor<CurrentPriceDto> dtoCaptor = ArgumentCaptor.forClass(CurrentPriceDto.class);
        verify(realtimeMarketDataService).updateFromWebSocket(eq("005930"), dtoCaptor.capture());
        assertThat(dtoCaptor.getValue().getCurrentPrice()).isEqualByComparingTo(new BigDecimal("75200"));
    }

    @Test
    @DisplayName("체결통보(H0STCNI0) 등 다른 trId는 updateFromWebSocket 미호출")
    void onWebSocketData_ccnlNotice_doesNotCallUpdate() {
        WebSocketDataEvent event = new WebSocketDataEvent(
                this, "user1|1", "H0STCNI0", "", "005930|75100");
        listener.onWebSocketData(event);
        // isQuoteData(), isExecutionData() 둘 다 false → 파싱/update 스킵
        verify(realtimeMarketDataService, never()).updateFromWebSocket(eq("005930"), org.mockito.ArgumentMatchers.any(CurrentPriceDto.class));
    }
}
