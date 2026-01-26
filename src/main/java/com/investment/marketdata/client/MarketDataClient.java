package com.investment.marketdata.client;

import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 시장 데이터 클라이언트 인터페이스
 * 다양한 시장 데이터 제공자를 추상화합니다.
 */
public interface MarketDataClient {
    
    /**
     * 단일 지표 조회
     * 
     * @param indicator 지표명 (rsi, macd, ema 등)
     * @param symbol 종목 코드 (예: AAPL, TSLA)
     * @param interval 시간 간격 (1h, 1d 등)
     * @return 지표 응답
     */
    Mono<IndicatorResponse> getIndicator(String indicator, String symbol, String interval);
    
    /**
     * Bulk 요청으로 여러 지표를 한 번에 조회
     * 
     * @param symbol 종목 코드
     * @param interval 시간 간격
     * @param indicators 조회할 지표 목록
     * @return 지표 응답 맵 (지표명 -> 응답)
     */
    Mono<Map<String, IndicatorResponse>> getBulkIndicators(
            String symbol, String interval, String... indicators);
    
    /**
     * 제공자 이름
     */
    String getProviderName();
}
