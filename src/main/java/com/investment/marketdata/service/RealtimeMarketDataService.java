package com.investment.marketdata.service;

import com.investment.config.CacheConfig;
import com.investment.marketdata.client.impl.KoreaInvestmentMarketDataClient;
import com.investment.marketdata.dto.CurrentPriceDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 실시간 시세 조회 서비스
 *
 * 한국투자증권 API를 사용하여 실시간 시세 정보를 조회합니다.
 * 현재가 조회 API를 활용하여 실시간 시세를 제공합니다.
 * 단일/다중 종목 조회 모두 동일한 캐시(종목별 5분 TTL)를 사용하며,
 * 다중 종목은 병렬 조회로 응답 시간을 단축합니다.
 * WebSocket이 활성화된 경우, WebSocket 수신 가격을 우선 반영하여 청산 판단 시 지연을 줄입니다.
 */
@Slf4j
@Service
public class RealtimeMarketDataService {

    private final KoreaInvestmentMarketDataClient marketDataClient;

    /** WebSocket으로 수신한 최신가 (종목별). 청산/단타 시 REST 캐시보다 우선 사용. */
    private final ConcurrentHashMap<String, CurrentPriceDto> webSocketLivePrices = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private CacheManager cacheManager;

    public RealtimeMarketDataService(KoreaInvestmentMarketDataClient marketDataClient) {
        this.marketDataClient = marketDataClient;
    }

    /**
     * 단일 종목 실시간 현재가 조회 (동기, 캐시·Circuit Breaker 적용)
     * WebSocket에서 최신가가 있으면 우선 반환하고, 없으면 캐시 미스 시 API 호출 후 5분 TTL 캐시.
     *
     * @param symbol 종목 코드 (6자리 또는 종목명)
     * @return 현재가 정보 (API 실패·fallback 시 null)
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_CURRENT_PRICE, key = "#symbol", unless = "#result == null")
    @CircuitBreaker(name = "marketDataService", fallbackMethod = "getCurrentPriceBlockingFallback")
    public CurrentPriceDto getCurrentPriceBlocking(String symbol) {
        log.info("[현재가] getCurrentPriceBlocking 진입 symbol={}", symbol);
        CurrentPriceDto fromWs = webSocketLivePrices.get(symbol);
        if (fromWs != null) {
            log.info("[현재가] WebSocket 캐시 반환 symbol={} price={}", symbol, fromWs.getCurrentPrice());
            return fromWs;
        }
        log.info("[현재가] 캐시 미스, 한투 클라이언트 호출 symbol={}", symbol);
        CurrentPriceDto result = marketDataClient.getCurrentPrice(symbol).blockOptional().orElse(null);
        if (result == null) {
            log.warn("[현재가] 한투 클라이언트 null 반환 symbol={} (토큰/API오류·폴백 가능)", symbol);
        } else {
            log.info("[현재가] 한투 클라이언트 성공 symbol={} currentPrice={}", symbol, result.getCurrentPrice());
        }
        return result;
    }

    /**
     * WebSocket 수신 데이터로 현재가 갱신. 청산/단타 판단 시 REST 폴링보다 우선 사용.
     *
     * @param symbol 종목 코드
     * @param dto    현재가 DTO (null이면 해당 종목만 live 맵에서 제거)
     */
    public void updateFromWebSocket(String symbol, CurrentPriceDto dto) {
        if (symbol == null || symbol.isBlank()) {
            return;
        }
        if (dto == null) {
            webSocketLivePrices.remove(symbol);
            return;
        }
        webSocketLivePrices.put(symbol, dto);
        if (cacheManager != null) {
            var cache = cacheManager.getCache(CacheConfig.CACHE_CURRENT_PRICE);
            if (cache != null) {
                cache.put(symbol, dto);
            }
        }
        log.trace("WebSocket 현재가 갱신: symbol={}", symbol);
    }

    @SuppressWarnings("unused")
    public CurrentPriceDto getCurrentPriceBlockingFallback(String symbol, Exception e) {
        log.warn("[현재가] Circuit Breaker 폴백→404 symbol={} exception={} message={}", symbol, e.getClass().getSimpleName(), e.getMessage());
        if (log.isDebugEnabled()) {
            log.debug("[현재가] 폴백 예외 상세 symbol=" + symbol, e);
        }
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

        CompletableFuture<List<CurrentPriceDto>> all = CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));

        return Mono.fromFuture(all);
    }
}
