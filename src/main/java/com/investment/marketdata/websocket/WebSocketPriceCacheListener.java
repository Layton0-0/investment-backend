package com.investment.marketdata.websocket;

import com.investment.marketdata.dto.CurrentPriceDto;
import com.investment.marketdata.service.RealtimeMarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * WebSocket 실시간 데이터 수신 시 현재가 캐시를 갱신하여
 * 청산/단타 판단이 REST 5분 캐시 대신 최신가를 사용하도록 한다.
 *
 * @see RealtimeMarketDataService#updateFromWebSocket(String, CurrentPriceDto)
 * @see investment-backend/docs/02-architecture/14-multi-account-realtime-streaming.md
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "investment.market-data.korea-investment.websocket.enabled",
        havingValue = "true")
public class WebSocketPriceCacheListener {

    private static final Pattern NUMERIC = Pattern.compile("^-?[0-9]+(\\.[0-9]+)?$");

    private final RealtimeMarketDataService realtimeMarketDataService;

    @EventListener
    public void onWebSocketData(WebSocketDataEvent event) {
        if (event == null || event.getData() == null || event.getData().isBlank()) {
            return;
        }
        if (!event.isQuoteData() && !event.isExecutionData()) {
            return;
        }
        try {
            CurrentPriceDto dto = parseToCurrentPrice(event.getData());
            if (dto != null && dto.getSymbol() != null && dto.getCurrentPrice() != null) {
                realtimeMarketDataService.updateFromWebSocket(dto.getSymbol(), dto);
            }
        } catch (Exception e) {
            log.trace("WebSocket 가격 파싱 스킵: trId={}, dataLen={}, error={}",
                    event.getTrId(), event.getData().length(), e.getMessage());
        }
    }

    /**
     * 한투 실시간 파이프 형식 파싱. 데이터가 "종목코드|현재가|..." 형태일 때 symbol, currentPrice 추출.
     * 고가/저가 등 추가 필드 위치는 API 스펙에 따라 확장 가능.
     */
    private CurrentPriceDto parseToCurrentPrice(String data) {
        if (data == null || data.isBlank()) {
            return null;
        }
        String[] parts = data.split("\\|");
        if (parts.length < 2) {
            return null;
        }
        String symbol = parts[0].trim();
        if (symbol.isEmpty()) {
            return null;
        }
        String priceStr = parts[1].trim();
        if (!NUMERIC.matcher(priceStr).matches()) {
            return null;
        }
        BigDecimal currentPrice = new BigDecimal(priceStr);
        BigDecimal highPrice = parts.length > 4 && NUMERIC.matcher(parts[4].trim()).matches()
                ? new BigDecimal(parts[4].trim()) : null;
        BigDecimal lowPrice = parts.length > 5 && NUMERIC.matcher(parts[5].trim()).matches()
                ? new BigDecimal(parts[5].trim()) : null;
        return CurrentPriceDto.builder()
                .symbol(symbol)
                .currentPrice(currentPrice)
                .highPrice(highPrice)
                .lowPrice(lowPrice)
                .queriedAt(LocalDateTime.now())
                .build();
    }
}
