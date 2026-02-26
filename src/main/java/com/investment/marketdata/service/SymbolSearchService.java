package com.investment.marketdata.service;

import com.investment.marketdata.dto.SymbolSearchItemDto;
import com.investment.marketdata.util.StockCodeConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 종목 통합 검색 서비스.
 * KR: StockCodeConverter 매핑 기반, US: 주요 종목 정적 목록 기반.
 */
@Slf4j
@Service
public class SymbolSearchService {

    /** US 주요 종목 (symbol -> name). 검색용. */
    private static final Map<String, String> US_SYMBOL_NAMES = new LinkedHashMap<>();
    static {
        US_SYMBOL_NAMES.put("AAPL", "Apple Inc.");
        US_SYMBOL_NAMES.put("MSFT", "Microsoft Corporation");
        US_SYMBOL_NAMES.put("GOOGL", "Alphabet Inc. (Google)");
        US_SYMBOL_NAMES.put("AMZN", "Amazon.com Inc.");
        US_SYMBOL_NAMES.put("META", "Meta Platforms Inc.");
        US_SYMBOL_NAMES.put("NVDA", "NVIDIA Corporation");
        US_SYMBOL_NAMES.put("TSLA", "Tesla Inc.");
        US_SYMBOL_NAMES.put("JPM", "JPMorgan Chase & Co.");
        US_SYMBOL_NAMES.put("V", "Visa Inc.");
        US_SYMBOL_NAMES.put("SPY", "SPDR S&P 500 ETF");
        US_SYMBOL_NAMES.put("QQQ", "Invesco QQQ Trust");
    }

    /**
     * 종목 검색. q가 비어 있으면 전체 목록(제한), 아니면 종목명/코드에 q가 포함된 항목만 반환.
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
            for (Map.Entry<String, String> e : US_SYMBOL_NAMES.entrySet()) {
                if (matches(query, e.getKey(), e.getValue())) {
                    result.add(SymbolSearchItemDto.builder()
                            .symbol(e.getKey())
                            .name(e.getValue())
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
