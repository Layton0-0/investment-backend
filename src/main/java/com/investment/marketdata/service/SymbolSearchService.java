package com.investment.marketdata.service;

import com.investment.config.CacheConfig;
import com.investment.domain.repository.DailyStockRepository;
import com.investment.marketdata.dto.SymbolSearchItemDto;
import com.investment.marketdata.util.StockCodeConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 종목 통합 검색 서비스.
 * KR: StockCodeConverter 매핑 기반, US: TB_DAILY_STOCK에 수집된 종목을 DB에서 조회(캐시 5분).
 */
@Slf4j
@Service
public class SymbolSearchService {

    private final DailyStockRepository dailyStockRepository;

    public SymbolSearchService(DailyStockRepository dailyStockRepository) {
        this.dailyStockRepository = dailyStockRepository;
    }

    /**
     * 시장별 종목 코드 목록 조회 (US: DB, 캐시 5분).
     */
    @Cacheable(value = CacheConfig.CACHE_SYMBOL_LIST, key = "#market")
    public List<String> getSymbolsByMarket(String market) {
        if (market == null || market.isBlank()) {
            return List.of();
        }
        return dailyStockRepository.findDistinctSymbolsByMarket(market.trim().toUpperCase());
    }

    /**
     * 종목 검색. q가 비어 있으면 전체 목록(제한), 아니면 종목명/코드에 q가 포함된 항목만 반환.
     * US: TB_DAILY_STOCK에 데이터가 있는 종목만 검색됨(수집 배치 실행 후 반영).
     *
     * @param q     검색어 (null/blank 시 전체)
     * @param market KR, US 또는 null/blank(전체)
     * @return 검색 결과 목록
     */
    public List<SymbolSearchItemDto> search(String q, String market) {
        String query = (q == null || q.isBlank()) ? null : q.trim().toLowerCase();
        String marketFilter = (market == null || market.isBlank()) ? null : market.trim().toUpperCase();

        List<SymbolSearchItemDto> result = new ArrayList<>();

        if (marketFilter == null || "KR".equals(marketFilter)) {
            Map<String, String> krMappings = StockCodeConverter.getAllMappings();
            for (Map.Entry<String, String> e : krMappings.entrySet()) {
                if (matches(query, e.getKey(), e.getValue())) {
                    result.add(SymbolSearchItemDto.builder()
                            .symbol(e.getKey())
                            .name(e.getValue())
                            .market("KR")
                            .build());
                }
            }
        }

        if (marketFilter == null || "US".equals(marketFilter)) {
            List<String> usSymbols = getSymbolsByMarket("US");
            for (String symbol : usSymbols) {
                if (matches(query, symbol, symbol)) {
                    result.add(SymbolSearchItemDto.builder()
                            .symbol(symbol)
                            .name(symbol)
                            .market("US")
                            .build());
                }
            }
        }

        return result.stream()
                .limit(100)
                .collect(Collectors.toList());
    }

    private static boolean matches(String query, String symbol, String name) {
        if (query == null || query.isEmpty()) {
            return true;
        }
        return (symbol != null && symbol.toLowerCase().contains(query))
                || (name != null && name.toLowerCase().contains(query));
    }
}
