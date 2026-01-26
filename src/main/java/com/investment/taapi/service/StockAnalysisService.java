package com.investment.taapi.service;

import com.investment.marketdata.client.IndicatorResponse;
import com.investment.marketdata.client.MarketDataClient;
import com.investment.taapi.dto.StockAnalysisDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 주식 분석 서비스
 * 시장 데이터 클라이언트를 사용하여 기술적 지표를 조회하고 분석합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockAnalysisService {
    
    private final MarketDataClient marketDataClient;
    
    /**
     * 종목의 기술적 분석 수행
     * 
     * @param symbol 종목 코드 (예: AAPL, TSLA)
     * @param interval 시간 간격 (1h, 1d 등)
     * @return 분석 결과
     */
    public Mono<StockAnalysisDto> analyzeStock(String symbol, String interval) {
        log.debug("주식 분석 시작: symbol={}, interval={}, provider={}", 
                symbol, interval, marketDataClient.getProviderName());
        
        // Bulk API로 여러 지표를 한 번에 조회
        // Finnhub 무료 플랜 제한에 맞춰 필수 지표만 조회 (rsi, macd, ema)
        // vwap: Finnhub가 직접 제공하지 않음
        // bbands, atr: 무료 플랜 제한을 고려하여 제외
        return marketDataClient.getBulkIndicators(symbol, interval,
                "rsi", "macd", "ema")
                .map(indicators -> {
                    StockAnalysisDto analysis = StockAnalysisDto.builder()
                            .symbol(symbol)
                            .name(getStockName(symbol))
                            .build();
                    
                    // RSI
                    IndicatorResponse rsiResponse = indicators.get("rsi");
                    if (rsiResponse != null && rsiResponse.getValue() != null) {
                        analysis.setRsi(rsiResponse.getValue());
                    }
                    
                    // MACD
                    IndicatorResponse macdResponse = indicators.get("macd");
                    if (macdResponse != null) {
                        if (macdResponse.getValueMacd() != null) {
                            analysis.setMacd(macdResponse.getValueMacd());
                        }
                        if (macdResponse.getValueMacdSignal() != null) {
                            analysis.setMacdSignal(macdResponse.getValueMacdSignal());
                        }
                        if (macdResponse.getValueMacdHist() != null) {
                            analysis.setMacdHist(macdResponse.getValueMacdHist());
                        }
                    }
                    
                    // EMA (20, 60, 120)
                    IndicatorResponse emaResponse = indicators.get("ema");
                    if (emaResponse != null && emaResponse.getValues() != null && emaResponse.getValues().length >= 3) {
                        analysis.setEma20(emaResponse.getValues()[0]);
                        analysis.setEma60(emaResponse.getValues()[1]);
                        analysis.setEma120(emaResponse.getValues()[2]);
                    } else if (emaResponse != null && emaResponse.getValue() != null) {
                        // 단일 EMA 값이 있는 경우 (iTick 등)
                        analysis.setEma20(emaResponse.getValue());
                    }
                    
                    // 현재가가 없으면 EMA20을 현재가로 사용
                    if (analysis.getCurrentPrice() == null && analysis.getEma20() != null) {
                        analysis.setCurrentPrice(analysis.getEma20());
                    }
                    
                    // 골든크로스 확인 (EMA20 > EMA60)
                    if (analysis.getEma20() != null && analysis.getEma60() != null) {
                        analysis.setGoldenCross(analysis.getEma20().compareTo(analysis.getEma60()) > 0);
                    }
                    
                    // 돌파 확인: 무료 플랜 제한으로 볼린저밴드 제외
                    // 대신 EMA20이 EMA60을 상향 돌파한 경우를 돌파로 간주
                    if (analysis.getEma20() != null && analysis.getEma60() != null) {
                        analysis.setBreakout(analysis.getEma20().compareTo(analysis.getEma60()) > 0);
                    }
                    
                    log.debug("주식 분석 완료: symbol={}, rsi={}, macd={}, provider={}", 
                            symbol, analysis.getRsi(), analysis.getMacd(), marketDataClient.getProviderName());
                    
                    return analysis;
                })
                .onErrorReturn(createDefaultAnalysis(symbol));
    }
    
    /**
     * 종목명 조회 (간단한 매핑)
     */
    private String getStockName(String symbol) {
        Map<String, String> nameMap = Map.of(
                "AAPL", "Apple Inc.",
                "TSLA", "Tesla Inc.",
                "NVDA", "NVIDIA Corporation",
                "MSFT", "Microsoft Corporation",
                "AMD", "Advanced Micro Devices",
                "GOOGL", "Alphabet Inc.",
                "AMZN", "Amazon.com Inc.",
                "META", "Meta Platforms Inc.",
                "SPY", "SPDR S&P 500 ETF",
                "QQQ", "Invesco QQQ Trust"
        );
        return nameMap.getOrDefault(symbol, symbol);
    }
    
    /**
     * 기본 분석 결과 생성 (에러 시)
     */
    private StockAnalysisDto createDefaultAnalysis(String symbol) {
        return StockAnalysisDto.builder()
                .symbol(symbol)
                .name(getStockName(symbol))
                .build();
    }
}
