package com.investment.taapi.service;

import com.investment.taapi.dto.StockAnalysisDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * StockScreeningService 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StockScreeningService 테스트")
class StockScreeningServiceTest {
    
    @Mock
    private StockAnalysisService stockAnalysisService;
    
    @InjectMocks
    private StockScreeningService stockScreeningService;
    
    @Test
    @DisplayName("주식 스크리닝 성공 - 상위 종목 선정")
    void screenStocks_성공() {
        // given
        String interval = "1d";
        int limit = 3;
        
        // 종목 1: 높은 점수
        StockAnalysisDto stock1 = StockAnalysisDto.builder()
                .symbol("005930") // 삼성전자
                .name("삼성전자")
                .rsi(new BigDecimal("50.0"))
                .macdHist(new BigDecimal("1.0"))
                .goldenCross(true)
                .breakout(true)
                .build();
        
        // 종목 2: 중간 점수
        StockAnalysisDto stock2 = StockAnalysisDto.builder()
                .symbol("000660") // SK하이닉스
                .name("SK하이닉스")
                .rsi(new BigDecimal("45.0"))
                .macdHist(new BigDecimal("0.5"))
                .goldenCross(false)
                .breakout(false)
                .build();
        
        // 종목 3: 낮은 점수
        StockAnalysisDto stock3 = StockAnalysisDto.builder()
                .symbol("035420") // NAVER
                .name("NAVER")
                .rsi(new BigDecimal("70.0")) // RSI가 높아서 점수 감점
                .macdHist(new BigDecimal("0.0"))
                .goldenCross(false)
                .breakout(false)
                .build();
        
        when(stockAnalysisService.analyzeStock("005930", interval))
                .thenReturn(Mono.just(stock1));
        when(stockAnalysisService.analyzeStock("000660", interval))
                .thenReturn(Mono.just(stock2));
        when(stockAnalysisService.analyzeStock("035420", interval))
                .thenReturn(Mono.just(stock3));
        when(stockAnalysisService.analyzeStock("051910", interval))
                .thenReturn(Mono.just(StockAnalysisDto.builder()
                        .symbol("051910")
                        .name("LG화학")
                        .rsi(new BigDecimal("40.0"))
                        .build()));
        when(stockAnalysisService.analyzeStock("035720", interval))
                .thenReturn(Mono.just(StockAnalysisDto.builder()
                        .symbol("035720")
                        .name("카카오")
                        .rsi(new BigDecimal("60.0"))
                        .build()));
        
        // when
        Mono<List<StockAnalysisDto>> result = stockScreeningService.screenStocks(interval, limit);
        
        // then
        StepVerifier.create(result)
                .assertNext(stocks -> {
                    assertNotNull(stocks);
                    assertTrue(stocks.size() <= limit);
                    // RSI가 있는 종목만 필터링됨
                    stocks.forEach(stock -> assertNotNull(stock.getRsi()));
                    // 점수 순으로 정렬됨 (높은 점수부터)
                    if (stocks.size() > 1) {
                        for (int i = 0; i < stocks.size() - 1; i++) {
                            BigDecimal currentScore = stocks.get(i).getExpectedReturn();
                            BigDecimal nextScore = stocks.get(i + 1).getExpectedReturn();
                            if (currentScore != null && nextScore != null) {
                                assertTrue(currentScore.compareTo(nextScore) >= 0);
                            }
                        }
                    }
                })
                .verifyComplete();
        
        verify(stockAnalysisService, atLeastOnce()).analyzeStock(anyString(), eq(interval));
    }
    
    @Test
    @DisplayName("주식 스크리닝 성공 - 일부 종목 분석 실패")
    void screenStocks_성공_일부_실패() {
        // given
        String interval = "1d";
        int limit = 2;
        
        StockAnalysisDto stock1 = StockAnalysisDto.builder()
                .symbol("005930")
                .name("삼성전자")
                .rsi(new BigDecimal("50.0"))
                .build();
        
        when(stockAnalysisService.analyzeStock("005930", interval))
                .thenReturn(Mono.just(stock1));
        when(stockAnalysisService.analyzeStock("000660", interval))
                .thenReturn(Mono.error(new RuntimeException("분석 실패")));
        when(stockAnalysisService.analyzeStock("035420", interval))
                .thenReturn(Mono.just(StockAnalysisDto.builder()
                        .symbol("035420")
                        .name("NAVER")
                        .rsi(new BigDecimal("55.0"))
                        .build()));
        when(stockAnalysisService.analyzeStock("051910", interval))
                .thenReturn(Mono.just(StockAnalysisDto.builder()
                        .symbol("051910")
                        .name("LG화학")
                        .rsi(new BigDecimal("45.0"))
                        .build()));
        when(stockAnalysisService.analyzeStock("035720", interval))
                .thenReturn(Mono.just(StockAnalysisDto.builder()
                        .symbol("035720")
                        .name("카카오")
                        .rsi(new BigDecimal("60.0"))
                        .build()));
        
        // when
        Mono<List<StockAnalysisDto>> result = stockScreeningService.screenStocks(interval, limit);
        
        // then
        StepVerifier.create(result)
                .assertNext(stocks -> {
                    assertNotNull(stocks);
                    // 실패한 종목은 제외되고 성공한 종목만 포함
                    assertTrue(stocks.size() > 0);
                    stocks.forEach(stock -> {
                        assertNotNull(stock.getRsi());
                        assertNotEquals("000660", stock.getSymbol()); // 실패한 종목은 제외
                    });
                })
                .verifyComplete();
    }
    
    @Test
    @DisplayName("주식 스크리닝 성공 - RSI가 없는 종목 필터링")
    void screenStocks_성공_RSI_없는_종목_필터링() {
        // given
        String interval = "1d";
        int limit = 2;
        
        // RSI가 없는 종목
        StockAnalysisDto stockWithoutRsi = StockAnalysisDto.builder()
                .symbol("005930")
                .name("삼성전자")
                .build(); // RSI 없음
        
        // RSI가 있는 종목
        StockAnalysisDto stockWithRsi = StockAnalysisDto.builder()
                .symbol("000660")
                .name("SK하이닉스")
                .rsi(new BigDecimal("50.0"))
                .build();
        
        when(stockAnalysisService.analyzeStock("005930", interval))
                .thenReturn(Mono.just(stockWithoutRsi));
        when(stockAnalysisService.analyzeStock("000660", interval))
                .thenReturn(Mono.just(stockWithRsi));
        when(stockAnalysisService.analyzeStock("035420", interval))
                .thenReturn(Mono.just(StockAnalysisDto.builder()
                        .symbol("035420")
                        .name("NAVER")
                        .rsi(new BigDecimal("55.0"))
                        .build()));
        when(stockAnalysisService.analyzeStock("051910", interval))
                .thenReturn(Mono.just(StockAnalysisDto.builder()
                        .symbol("051910")
                        .name("LG화학")
                        .rsi(new BigDecimal("45.0"))
                        .build()));
        when(stockAnalysisService.analyzeStock("035720", interval))
                .thenReturn(Mono.just(StockAnalysisDto.builder()
                        .symbol("035720")
                        .name("카카오")
                        .rsi(new BigDecimal("60.0"))
                        .build()));
        
        // when
        Mono<List<StockAnalysisDto>> result = stockScreeningService.screenStocks(interval, limit);
        
        // then
        StepVerifier.create(result)
                .assertNext(stocks -> {
                    assertNotNull(stocks);
                    // RSI가 없는 종목은 필터링됨
                    stocks.forEach(stock -> {
                        assertNotNull(stock.getRsi());
                        assertNotEquals("005930", stock.getSymbol()); // RSI가 없는 종목은 제외
                    });
                })
                .verifyComplete();
    }
    
    @Test
    @DisplayName("주식 스크리닝 성공 - 빈 결과")
    void screenStocks_성공_빈_결과() {
        // given
        String interval = "1d";
        int limit = 2;
        
        // 모든 종목이 RSI 없음
        when(stockAnalysisService.analyzeStock(anyString(), eq(interval)))
                .thenReturn(Mono.just(StockAnalysisDto.builder()
                        .symbol("005930")
                        .name("삼성전자")
                        .build())); // RSI 없음
        
        // when
        Mono<List<StockAnalysisDto>> result = stockScreeningService.screenStocks(interval, limit);
        
        // then
        StepVerifier.create(result)
                .assertNext(stocks -> {
                    assertNotNull(stocks);
                    assertTrue(stocks.isEmpty()); // RSI가 없는 종목만 있으면 빈 리스트
                })
                .verifyComplete();
    }
}
