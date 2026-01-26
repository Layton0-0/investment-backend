package com.investment.marketdata.client.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.investment.marketdata.client.IndicatorResponse;
import com.investment.marketdata.client.MarketDataClient;
import com.investment.marketdata.config.MarketDataProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Finnhub 시장 데이터 클라이언트 구현체
 * https://finnhub.io/docs/api
 * 
 * Finnhub는 기술적 지표를 직접 제공합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "investment.market-data.provider", havingValue = "finnhub", matchIfMissing = true)
public class FinnhubMarketDataClient implements MarketDataClient {
    
    private final MarketDataProperties properties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    private static final String FINNHUB_BASE_URL = "https://finnhub.io/api/v1";
    
    @Override
    public Mono<IndicatorResponse> getIndicator(String indicator, String symbol, String interval) {
        // API 키 검증
        String apiKey = properties.getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("Finnhub API 키가 설정되지 않았습니다. investment.market-data.api-key를 확인하세요.");
            return Mono.just(createErrorResponse("API 키가 설정되지 않았습니다"));
        }
        
        // VWAP는 Finnhub가 직접 제공하지 않으므로 에러 반환
        if ("vwap".equalsIgnoreCase(indicator)) {
            log.debug("Finnhub는 VWAP를 직접 제공하지 않습니다: symbol={}", symbol);
            return Mono.just(createErrorResponse("Finnhub는 VWAP 지표를 제공하지 않습니다"));
        }
        
        log.debug("Finnhub API 호출: indicator={}, symbol={}, interval={}", indicator, symbol, interval);
        
        String baseUrl = properties.getBaseUrl() != null ? properties.getBaseUrl() : FINNHUB_BASE_URL;
        
        // Finnhub Technical Indicator API: /indicator
        // 파라미터: symbol, resolution, from, to, indicator, indicator_fields
        // 무료 플랜 제한: 데이터 범위를 줄여 API 호출 부하 감소
        long to = Instant.now().getEpochSecond();
        long from = to - convertIntervalToSeconds(interval) * 100; // 최근 100개 캔들 데이터 (200 -> 100으로 축소)
        
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .pathSegment("indicator")
                .queryParam("symbol", symbol)
                .queryParam("resolution", convertInterval(interval))
                .queryParam("from", from)
                .queryParam("to", to)
                .queryParam("indicator", indicator)
                .queryParam("token", apiKey);
        
        // indicator_fields 설정 (지표별 파라미터)
        // Finnhub API 문서에 따르면 indicator_fields는 JSON 형식으로 전달합니다
        // 예: indicator_fields={"timeperiod":14}
        Map<String, String> indicatorFields = getIndicatorFields(indicator);
        if (!indicatorFields.isEmpty()) {
            try {
                // JSON 형식으로 변환
                String indicatorFieldsJson = objectMapper.writeValueAsString(indicatorFields);
                uriBuilder.queryParam("indicator_fields", indicatorFieldsJson);
            } catch (Exception e) {
                log.warn("indicator_fields JSON 변환 실패: {}, 기본값 사용", e.getMessage());
            }
        }
        
        URI uri = uriBuilder.build().toUri();
        
        return webClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofMillis(properties.getTimeout()))
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                        .filter(throwable -> {
                            if (throwable instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                                org.springframework.web.reactive.function.client.WebClientResponseException ex = 
                                        (org.springframework.web.reactive.function.client.WebClientResponseException) throwable;
                                // 429 에러는 재시도하지 않음
                                if (ex.getStatusCode().value() == 429) {
                                    log.warn("Finnhub API Rate Limit 도달: symbol={}, 재시도하지 않습니다", symbol);
                                    return false;
                                }
                                return ex.getStatusCode().is5xxServerError();
                            }
                            return false;
                        }))
                .map(this::convertToIndicatorResponse)
                .doOnError(error -> {
                    if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                        org.springframework.web.reactive.function.client.WebClientResponseException ex = 
                                (org.springframework.web.reactive.function.client.WebClientResponseException) error;
                        if (ex.getStatusCode().value() == 429) {
                            log.warn("Finnhub API Rate Limit: symbol={}, 모의 데이터 사용을 권장합니다", symbol);
                        }
                    }
                    log.error("Finnhub API 호출 실패: indicator={}, symbol={}, uri={}", 
                            indicator, symbol, uri, error);
                })
                .onErrorReturn(createErrorResponse("API 호출 실패"));
    }
    
    @Override
    public Mono<Map<String, IndicatorResponse>> getBulkIndicators(
            String symbol, String interval, String... indicators) {
        
        log.debug("Finnhub Bulk API 호출: symbol={}, interval={}, indicators={}", symbol, interval, indicators);
        
        // Finnhub는 Bulk API를 제공하지 않으므로, 각 지표를 순차적으로 호출
        // 무료 플랜: 분당 60회 API 호출 제한 (초당 1회)
        // Rate limit을 피하기 위해 요청 간격을 최소 1초 이상으로 설정
        Map<String, Mono<IndicatorResponse>> indicatorMonos = new HashMap<>();
        
        for (int i = 0; i < indicators.length; i++) {
            String indicator = indicators[i];
            // 각 요청 사이에 최소 1초 간격 (무료 플랜 제한 준수)
            Mono<IndicatorResponse> indicatorMono = getIndicator(indicator, symbol, interval)
                    .delayElement(Duration.ofSeconds(1 + i)); // 1초, 2초, 3초... 순차 지연
            indicatorMonos.put(indicator, indicatorMono);
        }
        
        // 모든 Mono를 순차적으로 수집하여 Map으로 변환
        // Flux를 사용하여 모든 지표를 수집
        return Flux.fromIterable(indicatorMonos.entrySet())
                .flatMap(entry -> entry.getValue()
                        .map(response -> Map.entry(entry.getKey(), response))
                        .onErrorReturn(Map.entry(entry.getKey(), createErrorResponse("지표 조회 실패"))))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .onErrorReturn(new HashMap<>());
    }
    
    @Override
    public String getProviderName() {
        return "Finnhub";
    }
    
    /**
     * Finnhub 응답을 IndicatorResponse로 변환
     */
    private IndicatorResponse convertToIndicatorResponse(Map<String, Object> response) {
        if (response == null) {
            return createErrorResponse("응답이 null입니다");
        }
        
        IndicatorResponse.IndicatorResponseBuilder builder = IndicatorResponse.builder();
        
        // Finnhub 응답 형식: {"indicator_name": [값 배열], "t": [타임스탬프 배열]}
        // 각 지표별로 다른 필드명을 가질 수 있음
        
        // RSI
        if (response.containsKey("rsi")) {
            List<Double> rsiValues = getDoubleList(response, "rsi");
            if (rsiValues != null && !rsiValues.isEmpty()) {
                builder.value(BigDecimal.valueOf(rsiValues.get(rsiValues.size() - 1)));
            }
        }
        
        // MACD
        if (response.containsKey("macd")) {
            List<Double> macdValues = getDoubleList(response, "macd");
            if (macdValues != null && !macdValues.isEmpty()) {
                builder.valueMacd(BigDecimal.valueOf(macdValues.get(macdValues.size() - 1)));
            }
        }
        if (response.containsKey("macdSignal")) {
            List<Double> signalValues = getDoubleList(response, "macdSignal");
            if (signalValues != null && !signalValues.isEmpty()) {
                builder.valueMacdSignal(BigDecimal.valueOf(signalValues.get(signalValues.size() - 1)));
            }
        }
        if (response.containsKey("macdHist")) {
            List<Double> histValues = getDoubleList(response, "macdHist");
            if (histValues != null && !histValues.isEmpty()) {
                builder.valueMacdHist(BigDecimal.valueOf(histValues.get(histValues.size() - 1)));
            }
        }
        
        // EMA (여러 기간)
        if (response.containsKey("ema")) {
            List<Double> emaValues = getDoubleList(response, "ema");
            if (emaValues != null && !emaValues.isEmpty()) {
                BigDecimal[] emaArray = emaValues.stream()
                        .map(BigDecimal::valueOf)
                        .toArray(BigDecimal[]::new);
                builder.values(emaArray);
            }
        }
        
        // Bollinger Bands
        if (response.containsKey("upperband")) {
            List<Double> upperValues = getDoubleList(response, "upperband");
            if (upperValues != null && !upperValues.isEmpty()) {
                builder.valueUpperBand(BigDecimal.valueOf(upperValues.get(upperValues.size() - 1)));
            }
        }
        if (response.containsKey("middleband")) {
            List<Double> middleValues = getDoubleList(response, "middleband");
            if (middleValues != null && !middleValues.isEmpty()) {
                builder.valueMiddleBand(BigDecimal.valueOf(middleValues.get(middleValues.size() - 1)));
            }
        }
        if (response.containsKey("lowerband")) {
            List<Double> lowerValues = getDoubleList(response, "lowerband");
            if (lowerValues != null && !lowerValues.isEmpty()) {
                builder.valueLowerBand(BigDecimal.valueOf(lowerValues.get(lowerValues.size() - 1)));
            }
        }
        
        // ATR
        if (response.containsKey("atr")) {
            List<Double> atrValues = getDoubleList(response, "atr");
            if (atrValues != null && !atrValues.isEmpty()) {
                builder.valueAtr(BigDecimal.valueOf(atrValues.get(atrValues.size() - 1)));
            }
        }
        
        // VWAP (Finnhub는 직접 제공하지 않으므로 계산 필요)
        // 현재는 생략
        
        return builder.build();
    }
    
    /**
     * interval을 Finnhub resolution으로 변환
     */
    private String convertInterval(String interval) {
        if (interval == null) {
            return "D";
        }
        
        String lowerInterval = interval.toLowerCase();
        
        if (lowerInterval.equals("1m") || lowerInterval.equals("1min")) {
            return "1";
        }
        if (lowerInterval.equals("5m") || lowerInterval.equals("5min")) {
            return "5";
        }
        if (lowerInterval.equals("15m") || lowerInterval.equals("15min")) {
            return "15";
        }
        if (lowerInterval.equals("30m") || lowerInterval.equals("30min")) {
            return "30";
        }
        if (lowerInterval.equals("1h") || lowerInterval.equals("1hour") || lowerInterval.equals("1hr")) {
            return "60";
        }
        if (lowerInterval.equals("1d") || lowerInterval.equals("1day")) {
            return "D";
        }
        if (lowerInterval.equals("1w") || lowerInterval.equals("1week")) {
            return "W";
        }
        if (lowerInterval.equals("1M") || lowerInterval.equals("1month")) {
            return "M";
        }
        
        return "D"; // 기본값: 일봉
    }
    
    /**
     * interval을 초 단위로 변환
     */
    private long convertIntervalToSeconds(String interval) {
        if (interval == null) {
            return 86400; // 1일
        }
        
        String lowerInterval = interval.toLowerCase();
        
        if (lowerInterval.equals("1m") || lowerInterval.equals("1min")) {
            return 60;
        }
        if (lowerInterval.equals("5m") || lowerInterval.equals("5min")) {
            return 300;
        }
        if (lowerInterval.equals("15m") || lowerInterval.equals("15min")) {
            return 900;
        }
        if (lowerInterval.equals("30m") || lowerInterval.equals("30min")) {
            return 1800;
        }
        if (lowerInterval.equals("1h") || lowerInterval.equals("1hour") || lowerInterval.equals("1hr")) {
            return 3600;
        }
        if (lowerInterval.equals("1d") || lowerInterval.equals("1day")) {
            return 86400;
        }
        if (lowerInterval.equals("1w") || lowerInterval.equals("1week")) {
            return 604800;
        }
        if (lowerInterval.equals("1M") || lowerInterval.equals("1month")) {
            return 2592000; // 약 30일
        }
        
        return 86400; // 기본값: 1일
    }
    
    /**
     * 지표별 필드 설정
     */
    private Map<String, String> getIndicatorFields(String indicator) {
        Map<String, String> fields = new HashMap<>();
        
        switch (indicator.toLowerCase()) {
            case "rsi":
                fields.put("timeperiod", "14");
                break;
            case "macd":
                fields.put("fastperiod", "12");
                fields.put("slowperiod", "26");
                fields.put("signalperiod", "9");
                break;
            case "ema":
                fields.put("timeperiod", "20");
                break;
            case "bbands":
                fields.put("timeperiod", "20");
                fields.put("nbdevup", "2");
                fields.put("nbdevdn", "2");
                break;
            case "atr":
                fields.put("timeperiod", "14");
                break;
        }
        
        return fields;
    }
    
    @SuppressWarnings("unchecked")
    private List<Double> getDoubleList(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof List) {
            return (List<Double>) value;
        }
        return null;
    }
    
    private IndicatorResponse createErrorResponse(String error) {
        return IndicatorResponse.builder()
                .error(error)
                .build();
    }
}
