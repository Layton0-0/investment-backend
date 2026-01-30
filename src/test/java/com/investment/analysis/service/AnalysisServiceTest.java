package com.investment.analysis.service;

import com.investment.analysis.dto.AnalysisRequestDto;
import com.investment.analysis.dto.AnalysisResponseDto;
import com.investment.taapi.dto.StockAnalysisDto;
import com.investment.taapi.service.StockAnalysisService;
import com.investment.ai.client.AiPredictionClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalysisService")
class AnalysisServiceTest {

    @Mock
    private AiPredictionClient aiPredictionClient;

    @Mock
    private StockAnalysisService stockAnalysisService;

    @InjectMocks
    private AnalysisService analysisService;

    @Test
    @DisplayName("analyze AI 비활성화 시 기술적 분석만으로 응답 생성")
    void analyze_aiDisabled_usesTechnicalAnalysisOnly() {
        ReflectionTestUtils.setField(analysisService, "aiServiceEnabled", false);

        AnalysisRequestDto request = AnalysisRequestDto.builder()
                .symbol("005930")
                .periodDays(30)
                .build();

        StockAnalysisDto technicalDto = StockAnalysisDto.builder()
                .symbol("005930")
                .currentPrice(BigDecimal.valueOf(75000))
                .rsi(BigDecimal.valueOf(45))
                .macd(BigDecimal.valueOf(100))
                .macdSignal(BigDecimal.valueOf(90))
                .ema20(BigDecimal.valueOf(74000))
                .expectedReturn(BigDecimal.valueOf(5))
                .build();

        when(stockAnalysisService.analyzeStock(eq("005930"), anyString()))
                .thenReturn(Mono.just(technicalDto));

        AnalysisResponseDto result = analysisService.analyze(request);

        assertNotNull(result);
        assertEquals("005930", result.getSymbol());
        assertNotNull(result.getCurrentPrice());
        assertNotNull(result.getTargetPrice());
        assertNotNull(result.getRecommendation());
        assertTrue(List.of("BUY", "SELL", "HOLD").contains(result.getRecommendation()));
        assertNotNull(result.getAnalyzedAt());
    }

    @Test
    @DisplayName("analyze 기술적 분석만 있을 때(AI empty) 기술적 분석 기반 응답")
    void analyze_technicalOnly_returnsTechnicalBasedResponse() {
        ReflectionTestUtils.setField(analysisService, "aiServiceEnabled", true);

        AnalysisRequestDto request = AnalysisRequestDto.builder()
                .symbol("005930")
                .periodDays(30)
                .build();

        StockAnalysisDto technicalDto = StockAnalysisDto.builder()
                .symbol("005930")
                .currentPrice(BigDecimal.valueOf(75000))
                .rsi(BigDecimal.valueOf(25))
                .macd(BigDecimal.valueOf(100))
                .macdSignal(BigDecimal.valueOf(90))
                .ema20(BigDecimal.valueOf(74000))
                .expectedReturn(BigDecimal.valueOf(3))
                .build();

        when(stockAnalysisService.analyzeStock(eq("005930"), anyString()))
                .thenReturn(Mono.just(technicalDto));
        when(aiPredictionClient.predictPrice(any())).thenReturn(Mono.empty());

        AnalysisResponseDto result = analysisService.analyze(request);

        assertNotNull(result);
        assertEquals("005930", result.getSymbol());
        assertEquals(0, result.getCurrentPrice().compareTo(BigDecimal.valueOf(75000)));
        assertEquals("BUY", result.getRecommendation());
    }
}
