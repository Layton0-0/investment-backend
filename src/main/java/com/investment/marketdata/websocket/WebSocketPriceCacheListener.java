package com.investment.marketdata.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investment.marketdata.config.MarketDataProperties;
import com.investment.marketdata.dto.CurrentPriceDto;
import com.investment.marketdata.service.RealtimeMarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * WebSocket 실시간 호가·체결 데이터 수신 시 현재가 캐시를 갱신하는 리스너.
 * tr_id가 설정의 quote-tr-id 또는 execution-tr-id와 일치하면 payload에서 종목코드·현재가를 추출하여
 * RealtimeMarketDataService.updateFromWebSocket(symbol, dto)를 호출한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketPriceCacheListener {

    private final MarketDataProperties marketDataProperties;
    private final RealtimeMarketDataService realtimeMarketDataService;
    private final ObjectMapper objectMapper;

    @EventListener
    public void onWebSocketData(WebSocketDataEvent event) {
        if (event == null || event.getTrId() == null) {
            return;
        }
        String trId = event.getTrId();
        var ws = marketDataProperties.getKoreaInvestment().getWebsocket();
        if (!trId.equals(ws.getQuoteTrId()) && !trId.equals(ws.getExecutionTrId())) {
            return;
        }
        String data = event.getData();
        if (data == null || data.isBlank()) {
            return;
        }
        try {
            Optional<SymbolPrice> parsed = parseSymbolAndPrice(data);
            if (parsed.isEmpty()) {
                return;
            }
            SymbolPrice sp = parsed.get();
            CurrentPriceDto dto = CurrentPriceDto.builder()
                    .symbol(sp.symbol)
                    .currentPrice(sp.currentPrice)
                    .queriedAt(LocalDateTime.now())
                    .build();
            realtimeMarketDataService.updateFromWebSocket(sp.symbol, dto);
            log.trace("WebSocket 현재가 캐시 갱신: trId={}, symbol={}, price={}", trId, sp.symbol, sp.currentPrice);
        } catch (Exception e) {
            log.debug("WebSocket 현재가 파싱/갱신 스킵: trId={}, error={}", trId, e.getMessage());
        }
    }

    private Optional<SymbolPrice> parseSymbolAndPrice(String data) {
        String trimmed = data.trim();
        if (trimmed.startsWith("{")) {
            return parseJsonSymbolPrice(trimmed);
        }
        if (trimmed.contains("|")) {
            return parsePipedSymbolPrice(trimmed);
        }
        return Optional.empty();
    }

    private Optional<SymbolPrice> parseJsonSymbolPrice(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode body = root.path("body");
            if (body.isMissingNode()) {
                body = root;
            }
            if (body.isArray() && body.size() > 0) {
                body = body.get(0);
            }
            String symbol = pathText(body, "pdno", "stock_code", "종목코드", "stck_shrn_iscd");
            String priceStr = pathText(body, "stck_prpr", "prpr", "현재가", "price");
            if (symbol == null || symbol.isBlank() || priceStr == null || priceStr.isBlank()) {
                return Optional.empty();
            }
            BigDecimal price = new BigDecimal(priceStr.replaceAll(",", ""));
            return Optional.of(new SymbolPrice(symbol.trim(), price));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Optional<SymbolPrice> parsePipedSymbolPrice(String data) {
        String[] parts = data.split("\\|", -1);
        if (parts.length < 2) {
            return Optional.empty();
        }
        String symbol = null;
        BigDecimal price = null;
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i].trim();
            if (p.matches("\\d{6}")) {
                symbol = p;
            } else if (price == null && p.matches("[\\d,]+(\\.\\d+)?")) {
                try {
                    price = new BigDecimal(p.replaceAll(",", ""));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (symbol != null && price != null && price.compareTo(BigDecimal.ZERO) > 0) {
            return Optional.of(new SymbolPrice(symbol, price));
        }
        return Optional.empty();
    }

    private static String pathText(JsonNode node, String... keys) {
        for (String key : keys) {
            if (node.has(key)) {
                String v = node.path(key).asText(null);
                if (v != null && !v.isEmpty()) {
                    return v;
                }
            }
        }
        return null;
    }

    private static final class SymbolPrice {
        final String symbol;
        final BigDecimal currentPrice;

        SymbolPrice(String symbol, BigDecimal currentPrice) {
            this.symbol = symbol;
            this.currentPrice = currentPrice;
        }
    }
}
