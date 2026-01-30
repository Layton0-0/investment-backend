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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 실시간 시세 조회 서비스
 *
 * 한국투자증권 API를 사용하여 실시간 시세 정보를 조회합니다.
 * 현재가 조회 API를 활용하여 실시간 시세를 제공합니다.
 * 단일/다중 종목 조회 모두 동일한 캐시(종목별 5분 TTL)를 사용하며,
 * 다중 종목은 병렬 조회로 응답 시간을 단축합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeMarketDataService {

    private final KoreaInvestmentMarketDataClient marketDataClient;

    /**
     * 단일 종목 실시간 현재가 조회 (동기, 캐시·Circuit Breaker 적용)
     * 캐시 미스 시에만 API 호출하며, 결과는 종목별 5분 TTL로 캐시됩니다.
     *
     * @param symbol 종목 코드 (6자리 또는 종목명)
     * @return 현재가 정보 (API 실패·fallback 시 null)
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_CURRENT_PRICE, key = "#symbol", unless = "#result == null")
    @CircuitBreaker(name = "marketDataService", fallbackMethod = "getCurrentPriceBlockingFallback")
    public CurrentPriceDto getCurrentPriceBlocking(String symbol) {
        log.debug("실시간 현재가 조회: symbol={}", symbol);
        return marketDataClient.getCurrentPrice(symbol).blockOptional().orElse(null);
    }

    @SuppressWarnings("unused")
    public CurrentPriceDto getCurrentPriceBlockingFallback(String symbol, Exception e) {
        log.warn("시장 데이터 API fallback: symbol={}, error={}", symbol, e.getMessage());
        return null;
    }

    /**
     * 단일 종목 실시간 현재가 조회 (리액티브)
     *
     * @param symbol 종목 코드 (6자리 또는 종목명)
     * @return 현재가 정보
     */
    @Transactional(readOnly = true)
    public Mono<CurrentPriceDto> getCurrentPrice(String symbol) {
        return Mono.fromCallable(() -> getCurrentPriceBlocking(symbol));
    }

    /**
     * 여러 종목의 실시간 현재가 조회 (병렬·동일 캐시 사용)
     * 종목별 캐시를 사용하며 캐시 미스인 종목만 병렬로 API 호출합니다.
     *
     * @param symbols 종목 코드 목록
     * @return 현재가 정보 목록 (실패한 종목은 제외)
     */
    @Transactional(readOnly = true)
    public Mono<List<CurrentPriceDto>> getCurrentPrices(List<String> symbols) {
        log.debug("실시간 현재가 일괄 조회: symbols={}", symbols);
        if (symbols == null || symbols.isEmpty()) {
            return Mono.just(List.of());
        }

        List<CompletableFuture<CurrentPriceDto>> futures = symbols.stream()
                .map(symbol -> CompletableFuture.supplyAsync(() -> getCurrentPriceBlocking(symbol)))
                .collect(Collectors.toList());

        CompletableFuture<List<CurrentPriceDto>> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));

        return Mono.fromFuture(all);
    }
}
