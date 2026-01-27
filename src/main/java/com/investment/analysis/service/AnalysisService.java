package com.investment.analysis.service;

import com.investment.ai.client.AiPredictionClient;
import com.investment.ai.dto.PredictionRequestDto;
import com.investment.ai.dto.PredictionResponseDto;
import com.investment.analysis.dto.AnalysisRequestDto;
import com.investment.analysis.dto.AnalysisResponseDto;
import com.investment.config.CacheConfig;
import com.investment.taapi.service.StockAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 종목 분석 서비스
 * 
 * <p>기술적 분석과 AI 예측을 결합하여 종목에 대한 종합적인 분석을 수행합니다.
 * 기술적 분석은 TAAPI 서비스를 통해 RSI, MACD 등의 지표를 조회하고,
 * AI 예측은 외부 AI 서비스(예: OpenAI, 자체 모델 등)를 연동하여 구현합니다.</p>
 * 
 * <p>분석 결과는 캐시되어 동일한 종목에 대한 반복 요청 시 성능을 향상시킵니다.</p>
 * 
 * @author Investment System
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AiPredictionClient aiPredictionClient;
    private final StockAnalysisService stockAnalysisService;

    @Value("${investment.ai.prediction-service.enabled:true}")
    private boolean aiServiceEnabled;

    /**
     * 종목 분석 수행
     * 
     * <p>기술적 분석과 AI 예측을 결합하여 종목에 대한 종합적인 분석을 수행합니다.
     * AI 서비스가 활성화된 경우 AI 예측을 우선 사용하며,
     * AI 예측이 실패하거나 비활성화된 경우 기술적 분석 결과를 사용합니다.</p>
     * 
     * @param request 분석 요청 정보 (종목코드, 분석 기간)
     * @return 종합 분석 결과 (현재가, 목표가, 신뢰도, 예상 수익률, 추천 등)
     * @throws RuntimeException 분석 수행 중 오류가 발생한 경우
     */
    @Cacheable(value = CacheConfig.CACHE_ANALYSIS, key = "#request.symbol + '_' + #request.periodDays")
    public AnalysisResponseDto analyze(AnalysisRequestDto request) {
        log.info("종목 분석 요청: symbol={}, periodDays={}, aiEnabled={}", 
                request.getSymbol(), request.getPeriodDays(), aiServiceEnabled);
        
        try {
            // 1. 기술적 분석 수행
            var technicalAnalysis = stockAnalysisService.analyzeStock(
                    request.getSymbol(), "1d").block();
            
            // 2. AI 예측 수행 (활성화된 경우)
            PredictionResponseDto aiPrediction = null;
            if (aiServiceEnabled) {
                try {
                    var predictionRequest = PredictionRequestDto.builder()
                            .symbol(request.getSymbol())
                            .predictionMinutes(request.getPeriodDays() * 24 * 60)
                            .lookbackDays(request.getPeriodDays())
                            .requestedAt(LocalDateTime.now())
                            .build();
                    
                    aiPrediction = aiPredictionClient.predictPrice(predictionRequest)
                            .onErrorResume(error -> {
                                log.warn("AI 예측 실패, 기술적 분석으로 대체: symbol={}, error={}", 
                                        request.getSymbol(), error.getMessage());
                                return Mono.empty();
                            })
                            .block();
                } catch (Exception e) {
                    log.warn("AI 예측 서비스 오류, 기술적 분석으로 대체: symbol={}", 
                            request.getSymbol(), e);
                }
            }
            
            // 3. 종합 분석 결과 생성
            return buildAnalysisResponse(request, technicalAnalysis, aiPrediction);
                    
        } catch (Exception e) {
            log.error("종목 분석 실패: symbol={}", request.getSymbol(), e);
            throw new RuntimeException("종목 분석에 실패했습니다: " + e.getMessage(), e);
        }
    }
    
    /**
     * 종합 분석 결과 생성
     * 
     * <p>기술적 분석 결과와 AI 예측 결과를 결합하여 최종 분석 응답을 생성합니다.
     * AI 예측이 있는 경우 AI 예측을 우선 사용하며,
     * AI 예측이 없는 경우 기술적 분석 결과를 사용합니다.</p>
     * 
     * @param request 분석 요청 정보
     * @param technicalAnalysis 기술적 분석 결과
     * @param aiPrediction AI 예측 결과 (null 가능)
     * @return 종합 분석 결과
     */
    private AnalysisResponseDto buildAnalysisResponse(
            AnalysisRequestDto request,
            com.investment.taapi.dto.StockAnalysisDto technicalAnalysis,
            PredictionResponseDto aiPrediction) {
        
        var builder = AnalysisResponseDto.builder()
                .symbol(request.getSymbol())
                .analyzedAt(LocalDateTime.now());
        
        // AI 예측이 있는 경우
        if (aiPrediction != null) {
            builder.currentPrice(aiPrediction.getCurrentPrice())
                    .targetPrice(aiPrediction.getPredictedPrice())
                    .confidence(aiPrediction.getConfidence())
                    .expectedReturn(aiPrediction.getExpectedReturn())
                    .reasoning(buildReasoning(technicalAnalysis, aiPrediction));
            
            // AI 예측 방향에 따른 추천
            // TODO: AI 예측 결과를 기반으로 추천 로직 구현 필요
            builder.recommendation(determineRecommendation(aiPrediction));
        } else {
            // 기술적 분석만 사용
            if (technicalAnalysis != null) {
                BigDecimal currentPrice = technicalAnalysis.getCurrentPrice();
                BigDecimal targetPrice = calculateTargetPrice(currentPrice, technicalAnalysis.getExpectedReturn());
                
                builder.currentPrice(currentPrice)
                        .targetPrice(targetPrice)
                        .confidence(calculateTechnicalConfidence(technicalAnalysis))
                        .expectedReturn(technicalAnalysis.getExpectedReturn() != null 
                                ? technicalAnalysis.getExpectedReturn() 
                                : BigDecimal.ZERO)
                        .recommendation(determineTechnicalRecommendation(technicalAnalysis))
                        .reasoning("기술적 분석 기반 추천입니다.");
            } else {
                // 기본값 (폴백)
                builder.currentPrice(BigDecimal.ZERO)
                        .targetPrice(BigDecimal.ZERO)
                        .confidence(new BigDecimal("0.5"))
                        .expectedReturn(BigDecimal.ZERO)
                        .recommendation("HOLD")
                        .reasoning("분석 데이터가 부족합니다.");
            }
        }
        
        // 기술적 지표 추가
        builder.indicators(buildIndicators(technicalAnalysis, aiPrediction));
        
        return builder.build();
    }
    
    /**
     * 목표가 계산
     * 
     * <p>현재가와 예상 수익률을 기반으로 목표가를 계산합니다.</p>
     * 
     * @param currentPrice 현재가
     * @param expectedReturn 예상 수익률 (%)
     * @return 목표가
     */
    private BigDecimal calculateTargetPrice(BigDecimal currentPrice, BigDecimal expectedReturn) {
        if (currentPrice == null || expectedReturn == null) {
            return currentPrice != null ? currentPrice : BigDecimal.ZERO;
        }
        
        // 목표가 = 현재가 * (1 + 예상 수익률 / 100)
        return currentPrice.multiply(
                BigDecimal.ONE.add(expectedReturn.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP))
        ).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * 추천 결정 (AI 예측 기반)
     * 
     * <p>AI 예측 결과의 신뢰도와 예상 수익률을 기반으로 매수/매도/보유를 결정합니다.</p>
     * 
     * @param prediction AI 예측 결과
     * @return 추천 (BUY/SELL/HOLD)
     */
    private String determineRecommendation(PredictionResponseDto prediction) {
        if (prediction.getConfidence().compareTo(new BigDecimal("0.7")) < 0) {
            return "HOLD";
        }
        
        BigDecimal returnPercent = prediction.getExpectedReturn();
        if (returnPercent.compareTo(new BigDecimal("5")) > 0) {
            return "BUY";
        } else if (returnPercent.compareTo(new BigDecimal("-5")) < 0) {
            return "SELL";
        } else {
            return "HOLD";
        }
    }
    
    /**
     * 기술적 분석 기반 추천 결정
     * 
     * <p>RSI와 MACD 지표를 기반으로 매수/매도/보유를 결정합니다.</p>
     * 
     * @param analysis 기술적 분석 결과
     * @return 추천 (BUY/SELL/HOLD)
     */
    private String determineTechnicalRecommendation(
            com.investment.taapi.dto.StockAnalysisDto analysis) {
        // RSI 기반 추천
        if (analysis.getRsi() != null) {
            if (analysis.getRsi().compareTo(new BigDecimal("70")) > 0) {
                return "SELL";
            } else if (analysis.getRsi().compareTo(new BigDecimal("30")) < 0) {
                return "BUY";
            }
        }
        
        // MACD 기반 추천
        if (analysis.getMacd() != null && analysis.getMacdSignal() != null) {
            if (analysis.getMacd().compareTo(analysis.getMacdSignal()) > 0) {
                return "BUY";
            } else {
                return "SELL";
            }
        }
        
        return "HOLD";
    }
    
    /**
     * 기술적 분석 신뢰도 계산
     * 
     * <p>사용 가능한 기술적 지표의 개수와 종류를 기반으로 신뢰도를 계산합니다.
     * RSI, MACD, EMA20 등의 지표가 있을수록 신뢰도가 높아집니다.</p>
     * 
     * @param analysis 기술적 분석 결과
     * @return 신뢰도 (0.0 ~ 0.9)
     */
    private BigDecimal calculateTechnicalConfidence(
            com.investment.taapi.dto.StockAnalysisDto analysis) {
        int indicatorCount = 0;
        BigDecimal totalConfidence = BigDecimal.ZERO;
        
        if (analysis.getRsi() != null) {
            indicatorCount++;
            totalConfidence = totalConfidence.add(new BigDecimal("0.3"));
        }
        if (analysis.getMacd() != null) {
            indicatorCount++;
            totalConfidence = totalConfidence.add(new BigDecimal("0.3"));
        }
        if (analysis.getEma20() != null) {
            indicatorCount++;
            totalConfidence = totalConfidence.add(new BigDecimal("0.2"));
        }
        
        if (indicatorCount == 0) {
            return new BigDecimal("0.5");
        }
        
        return totalConfidence.min(new BigDecimal("0.9"));
    }
    
    /**
     * 분석 근거 생성
     * 
     * <p>AI 예측 결과와 기술적 분석 결과를 결합하여 분석 근거 문자열을 생성합니다.</p>
     * 
     * @param technicalAnalysis 기술적 분석 결과
     * @param aiPrediction AI 예측 결과
     * @return 분석 근거 문자열
     */
    private String buildReasoning(
            com.investment.taapi.dto.StockAnalysisDto technicalAnalysis,
            PredictionResponseDto aiPrediction) {
        StringBuilder reasoning = new StringBuilder();
        
        reasoning.append("AI 예측: ").append(aiPrediction.getDirection())
                .append(" 방향, 예상 수익률 ").append(aiPrediction.getExpectedReturn())
                .append("%, 신뢰도 ").append(aiPrediction.getConfidence().multiply(new BigDecimal("100")))
                .append("%. ");
        
        if (technicalAnalysis != null) {
            reasoning.append("기술적 분석: ");
            if (technicalAnalysis.getRsi() != null) {
                reasoning.append("RSI ").append(technicalAnalysis.getRsi()).append(". ");
            }
            if (technicalAnalysis.getMacd() != null) {
                reasoning.append("MACD ").append(technicalAnalysis.getMacd()).append(". ");
            }
        }
        
        if (aiPrediction.getMetadata() != null && 
            aiPrediction.getMetadata().getReasoning() != null) {
            reasoning.append(aiPrediction.getMetadata().getReasoning());
        }
        
        return reasoning.toString();
    }
    
    /**
     * 지표 목록 생성
     * 
     * <p>기술적 분석 지표와 AI 예측 지표를 결합하여 지표 목록을 생성합니다.</p>
     * 
     * @param technicalAnalysis 기술적 분석 결과
     * @param aiPrediction AI 예측 결과
     * @return 지표 목록
     */
    private List<AnalysisResponseDto.AnalysisIndicatorDto> buildIndicators(
            com.investment.taapi.dto.StockAnalysisDto technicalAnalysis,
            PredictionResponseDto aiPrediction) {
        List<AnalysisResponseDto.AnalysisIndicatorDto> indicators = new ArrayList<>();
        
        // 기술적 지표
        if (technicalAnalysis != null) {
            if (technicalAnalysis.getRsi() != null) {
                indicators.add(AnalysisResponseDto.AnalysisIndicatorDto.builder()
                        .name("RSI")
                        .value(technicalAnalysis.getRsi().toString())
                        .interpretation(interpretRsi(technicalAnalysis.getRsi()))
                        .build());
            }
            
            if (technicalAnalysis.getMacd() != null) {
                indicators.add(AnalysisResponseDto.AnalysisIndicatorDto.builder()
                        .name("MACD")
                        .value(technicalAnalysis.getMacd().toString())
                        .interpretation(interpretMacd(technicalAnalysis.getMacd(), 
                                technicalAnalysis.getMacdSignal()))
                        .build());
            }
        }
        
        // AI 예측 지표
        if (aiPrediction != null) {
            indicators.add(AnalysisResponseDto.AnalysisIndicatorDto.builder()
                    .name("AI 예측 신뢰도")
                    .value(aiPrediction.getConfidence()
                            .multiply(new BigDecimal("100"))
                            .setScale(2, RoundingMode.HALF_UP).toString() + "%")
                    .interpretation("AI 모델의 예측 신뢰도")
                    .build());
            
            if (aiPrediction.getVolatility() != null) {
                indicators.add(AnalysisResponseDto.AnalysisIndicatorDto.builder()
                        .name("예상 변동성")
                        .value(aiPrediction.getVolatility().toString())
                        .interpretation("예상 가격 변동성")
                        .build());
            }
        }
        
        return indicators;
    }
    
    /**
     * RSI 해석
     * 
     * <p>RSI 값을 기반으로 과매수/과매도/중립 상태를 판단합니다.</p>
     * 
     * @param rsi RSI 값
     * @return 해석 결과 (과매수/과매도/중립)
     */
    private String interpretRsi(BigDecimal rsi) {
        if (rsi.compareTo(new BigDecimal("70")) > 0) {
            return "과매수";
        } else if (rsi.compareTo(new BigDecimal("30")) < 0) {
            return "과매도";
        } else {
            return "중립";
        }
    }
    
    /**
     * MACD 해석
     * 
     * <p>MACD와 Signal 값을 비교하여 상승/하락 신호를 판단합니다.</p>
     * 
     * @param macd MACD 값
     * @param signal Signal 값
     * @return 해석 결과 (상승 신호/하락 신호/신호 없음)
     */
    private String interpretMacd(BigDecimal macd, BigDecimal signal) {
        if (signal == null) {
            return "신호 없음";
        }
        if (macd.compareTo(signal) > 0) {
            return "상승 신호";
        } else {
            return "하락 신호";
        }
    }
}
