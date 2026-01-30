package com.investment.marketdata.service;

import com.investment.config.CacheConfig;
import com.investment.marketdata.client.impl.KoreaInvestmentMarketDataClient;
import com.investment.marketdata.dto.CurrentPriceDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 실시간 시세 조회 서비스
 * 
 * 한국투자증권 API를 사용하여 실시간 시세 정보를 조회합니다.
 * 현재가 조회 API를 활용하여 실시간 시세를 제공합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeMarketDataService {

    private final KoreaInvestmentMarketDataClient marketDataClient;

    /**
     * 단일 종목 실시간 현재가 조회
     * 
     * @param symbol 종목 코드 (6자리 또는 종목명)
     * @return 현재가 정보
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_CURRENT_PRICE, key = "#symbol", unless = "#result == null")
    @CircuitBreaker(name = "marketDataService", fallbackMethod = "getCurrentPriceFallback")
    public Mono<CurrentPriceDto> getCurrentPrice(String symbol) {
        log.debug("실시간 현재가 조회: symbol={}", symbol);
        return marketDataClient.getCurrentPrice(symbol);
    }

    @SuppressWarnings("unused")
    public Mono<CurrentPriceDto> getCurrentPriceFallback(String symbol, Exception e) {
        log.warn("시장 데이터 API fallback: symbol={}, error={}", symbol, e.getMessage());
        return Mono.empty();
    }

    /**
     * 여러 종목의 실시간 현재가 조회
     * 
     * @param symbols 종목 코드 목록
     * @return 현재가 정보 목록
     */
    @Transactional(readOnly = true)
    public Mono<List<CurrentPriceDto>> getCurrentPrices(List<String> symbols) {
        log.debug("실시간 현재가 일괄 조회: symbols={}", symbols);

        List<Mono<CurrentPriceDto>> monos = symbols.stream()
                .map(marketDataClient::getCurrentPrice)
                .collect(java.util.stream.Collectors.toList());

        return Mono.zip(monos, results -> {
            List<CurrentPriceDto> prices = new java.util.ArrayList<>();
            for (Object result : results) {
                if (result instanceof CurrentPriceDto) {
                    prices.add((CurrentPriceDto) result);
                }
            }
            return prices;
        });
    }
}
