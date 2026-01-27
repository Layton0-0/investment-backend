package com.investment.strategy.engine;

import com.investment.taapi.dto.StockAnalysisDto;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 정량적 규칙 엔진
 * 
 * 수치 기반 규칙에 따라 투자 결정을 수행합니다.
 * AI가 아닌 명확한 수치 기준과 공식에 따라 결정합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuantitativeRuleEngine {
    
    /**
     * 매수/매도/보유 결정
     * 
     * @param analysis 기술적 분석 결과
     * @param macroStrategy 거시경제 전략
     * @return 거래 결정
     */
    public TradingDecision decide(StockAnalysisDto analysis, 
                                  MacroEconomicStrategyEngine.InvestmentStrategy macroStrategy) {
        log.debug("정량적 규칙 기반 거래 결정: symbol={}", analysis.getSymbol());
        
        // 1. 점수 계산
        TradingScore score = calculateScore(analysis, macroStrategy);
        
        // 2. 점수 기반 결정
        TradingDecision.DecisionType decisionType = determineDecisionType(score);
        
        // 3. 진입/청산 가격 계산
        PriceLevels priceLevels = calculatePriceLevels(analysis);
        
        // 4. 포지션 사이즈 계산
        BigDecimal positionSize = calculatePositionSize(score, macroStrategy);
        
        return TradingDecision.builder()
                .decisionType(decisionType)
                .score(score)
                .priceLevels(priceLevels)
                .positionSizePercent(positionSize)
                .reasoning(buildReasoning(score, analysis))
                .build();
    }
    
    /**
     * 거래 점수 계산 (0.0 ~ 1.0)
     */
    private TradingScore calculateScore(StockAnalysisDto analysis,
                                        MacroEconomicStrategyEngine.InvestmentStrategy macroStrategy) {
        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal weightSum = BigDecimal.ZERO;
        
        // 1. RSI 점수 (가중치: 20%)
        if (analysis.getRsi() != null) {
            BigDecimal rsiScore = calculateRsiScore(analysis.getRsi());
            totalScore = totalScore.add(rsiScore.multiply(new BigDecimal("0.20")));
            weightSum = weightSum.add(new BigDecimal("0.20"));
        }
        
        // 2. MACD 점수 (가중치: 25%)
        if (analysis.getMacd() != null && analysis.getMacdSignal() != null) {
            BigDecimal macdScore = calculateMacdScore(analysis.getMacd(), analysis.getMacdSignal());
            totalScore = totalScore.add(macdScore.multiply(new BigDecimal("0.25")));
            weightSum = weightSum.add(new BigDecimal("0.25"));
        }
        
        // 3. EMA 점수 (가중치: 20%)
        if (analysis.getEma20() != null && analysis.getEma60() != null && 
            analysis.getCurrentPrice() != null) {
            BigDecimal emaScore = calculateEmaScore(analysis);
            totalScore = totalScore.add(emaScore.multiply(new BigDecimal("0.20")));
            weightSum = weightSum.add(new BigDecimal("0.20"));
        }
        
        // 4. 거래량 점수 (가중치: 15%)
        if (analysis.getVolumeChange() != null) {
            BigDecimal volumeScore = calculateVolumeScore(analysis.getVolumeChange());
            totalScore = totalScore.add(volumeScore.multiply(new BigDecimal("0.15")));
            weightSum = weightSum.add(new BigDecimal("0.15"));
        }
        
        // 5. VWAP 점수 (가중치: 10%)
        if (analysis.getVwap() != null && analysis.getCurrentPrice() != null) {
            BigDecimal vwapScore = calculateVwapScore(analysis.getCurrentPrice(), analysis.getVwap());
            totalScore = totalScore.add(vwapScore.multiply(new BigDecimal("0.10")));
            weightSum = weightSum.add(new BigDecimal("0.10"));
        }
        
        // 6. 거시경제 조정 (가중치: 10%)
        BigDecimal macroAdjustment = calculateMacroAdjustment(macroStrategy);
        totalScore = totalScore.add(macroAdjustment.multiply(new BigDecimal("0.10")));
        weightSum = weightSum.add(new BigDecimal("0.10"));
        
        // 정규화
        BigDecimal finalScore = weightSum.compareTo(BigDecimal.ZERO) > 0 ?
                totalScore.divide(weightSum, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        
        return TradingScore.builder()
                .totalScore(finalScore.setScale(4, RoundingMode.HALF_UP))
                .rsiScore(analysis.getRsi() != null ? calculateRsiScore(analysis.getRsi()) : null)
                .macdScore(analysis.getMacd() != null && analysis.getMacdSignal() != null ?
                        calculateMacdScore(analysis.getMacd(), analysis.getMacdSignal()) : null)
                .build();
    }
    
    /**
     * RSI 점수 계산 (0.0 ~ 1.0)
     * RSI < 30: 1.0 (과매도, 매수 신호)
     * RSI > 70: 0.0 (과매수, 매도 신호)
     * 30 <= RSI <= 70: 선형 보간
     */
    private BigDecimal calculateRsiScore(BigDecimal rsi) {
        if (rsi.compareTo(new BigDecimal("30")) < 0) {
            return BigDecimal.ONE; // 강한 매수 신호
        } else if (rsi.compareTo(new BigDecimal("70")) > 0) {
            return BigDecimal.ZERO; // 강한 매도 신호
        } else {
            // 선형 보간: (70 - RSI) / 40
            return new BigDecimal("70").subtract(rsi)
                    .divide(new BigDecimal("40"), 4, RoundingMode.HALF_UP);
        }
    }
    
    /**
     * MACD 점수 계산 (0.0 ~ 1.0)
     * MACD > Signal: 1.0 (상승 신호)
     * MACD < Signal: 0.0 (하락 신호)
     * 히스토그램 크기에 따라 조정
     */
    private BigDecimal calculateMacdScore(BigDecimal macd, BigDecimal signal) {
        BigDecimal diff = macd.subtract(signal);
        
        // 정규화: diff를 -1 ~ 1 범위로 변환
        BigDecimal normalized = diff.divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP)
                .add(new BigDecimal("0.5"));
        
        // 0.0 ~ 1.0 범위로 제한
        if (normalized.compareTo(BigDecimal.ONE) > 0) {
            normalized = BigDecimal.ONE;
        }
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            normalized = BigDecimal.ZERO;
        }
        
        return normalized;
    }
    
    /**
     * EMA 점수 계산 (0.0 ~ 1.0)
     * 골든크로스: 1.0
     * 데드크로스: 0.0
     * 정렬 상태에 따라 점수 부여
     */
    private BigDecimal calculateEmaScore(StockAnalysisDto analysis) {
        BigDecimal currentPrice = analysis.getCurrentPrice();
        BigDecimal ema20 = analysis.getEma20();
        BigDecimal ema60 = analysis.getEma60();
        
        if (currentPrice == null || ema20 == null || ema60 == null) {
            return new BigDecimal("0.5");
        }
        
        // 골든크로스: EMA20 > EMA60, 현재가 > EMA20
        if (ema20.compareTo(ema60) > 0 && currentPrice.compareTo(ema20) > 0) {
            return BigDecimal.ONE;
        }
        
        // 데드크로스: EMA20 < EMA60, 현재가 < EMA20
        if (ema20.compareTo(ema60) < 0 && currentPrice.compareTo(ema20) < 0) {
            return BigDecimal.ZERO;
        }
        
        // 중간 상태: 현재가와 EMA의 거리 비율
        BigDecimal distance = currentPrice.subtract(ema20);
        BigDecimal emaRange = ema20.subtract(ema60).abs();
        
        if (emaRange.compareTo(BigDecimal.ZERO) == 0) {
            return new BigDecimal("0.5");
        }
        
        BigDecimal ratio = distance.divide(emaRange, 4, RoundingMode.HALF_UP);
        return ratio.add(new BigDecimal("0.5"))
                .max(BigDecimal.ZERO)
                .min(BigDecimal.ONE);
    }
    
    /**
     * 거래량 점수 계산 (0.0 ~ 1.0)
     * 거래량 증가율이 클수록 높은 점수
     */
    private BigDecimal calculateVolumeScore(BigDecimal volumeChange) {
        // 150% 이상: 1.0
        // 100%: 0.5
        // 50% 이하: 0.0
        if (volumeChange.compareTo(new BigDecimal("150")) >= 0) {
            return BigDecimal.ONE;
        } else if (volumeChange.compareTo(new BigDecimal("50")) <= 0) {
            return BigDecimal.ZERO;
        } else {
            // 선형 보간: (volumeChange - 50) / 100
            return volumeChange.subtract(new BigDecimal("50"))
                    .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        }
    }
    
    /**
     * VWAP 점수 계산 (0.0 ~ 1.0)
     * 현재가 > VWAP: 높은 점수
     */
    private BigDecimal calculateVwapScore(BigDecimal currentPrice, BigDecimal vwap) {
        if (vwap.compareTo(BigDecimal.ZERO) == 0) {
            return new BigDecimal("0.5");
        }
        
        BigDecimal ratio = currentPrice.divide(vwap, 4, RoundingMode.HALF_UP);
        
        // 1.05 이상: 1.0
        // 1.0: 0.5
        // 0.95 이하: 0.0
        if (ratio.compareTo(new BigDecimal("1.05")) >= 0) {
            return BigDecimal.ONE;
        } else if (ratio.compareTo(new BigDecimal("0.95")) <= 0) {
            return BigDecimal.ZERO;
        } else {
            // 선형 보간: (ratio - 0.95) / 0.1
            return ratio.subtract(new BigDecimal("0.95"))
                    .divide(new BigDecimal("0.1"), 4, RoundingMode.HALF_UP);
        }
    }
    
    /**
     * 거시경제 조정 점수 계산
     */
    private BigDecimal calculateMacroAdjustment(
            MacroEconomicStrategyEngine.InvestmentStrategy macroStrategy) {
        
        if (macroStrategy == null) {
            return new BigDecimal("0.5");
        }
        
        // 리스크 레벨에 반비례
        // 리스크가 낮을수록 높은 점수
        return BigDecimal.ONE.subtract(macroStrategy.getRiskLevel());
    }
    
    /**
     * 점수 기반 결정 타입 결정
     */
    private TradingDecision.DecisionType determineDecisionType(TradingScore score) {
        BigDecimal totalScore = score.getTotalScore();
        
        // 매수: 0.7 이상
        if (totalScore.compareTo(new BigDecimal("0.7")) >= 0) {
            return TradingDecision.DecisionType.BUY;
        }
        
        // 매도: 0.3 이하
        if (totalScore.compareTo(new BigDecimal("0.3")) <= 0) {
            return TradingDecision.DecisionType.SELL;
        }
        
        // 보유: 0.3 ~ 0.7
        return TradingDecision.DecisionType.HOLD;
    }
    
    /**
     * 가격 레벨 계산
     */
    private PriceLevels calculatePriceLevels(StockAnalysisDto analysis) {
        BigDecimal currentPrice = analysis.getCurrentPrice();
        if (currentPrice == null) {
            currentPrice = analysis.getEma20();
        }
        if (currentPrice == null) {
            currentPrice = BigDecimal.ONE; // 기본값
        }
        
        // 진입가: 현재가 ± 1%
        BigDecimal entryMin = currentPrice.multiply(new BigDecimal("0.99"));
        BigDecimal entryMax = currentPrice.multiply(new BigDecimal("1.01"));
        
        // 손절가: ATR 기반 또는 현재가의 3% 하락
        BigDecimal stopLoss;
        if (analysis.getAtr() != null && analysis.getAtr().compareTo(BigDecimal.ZERO) > 0) {
            stopLoss = currentPrice.subtract(analysis.getAtr().multiply(new BigDecimal("2")));
            if (stopLoss.compareTo(currentPrice.multiply(new BigDecimal("0.95"))) < 0) {
                stopLoss = currentPrice.multiply(new BigDecimal("0.97"));
            }
        } else {
            stopLoss = currentPrice.multiply(new BigDecimal("0.97"));
        }
        
        // 목표가 1: R:R 2:1
        BigDecimal risk = currentPrice.subtract(stopLoss);
        BigDecimal target1 = currentPrice.add(risk.multiply(new BigDecimal("2")));
        
        // 목표가 2: R:R 3:1
        BigDecimal target2 = currentPrice.add(risk.multiply(new BigDecimal("3")));
        
        return PriceLevels.builder()
                .entryMin(entryMin.setScale(2, RoundingMode.HALF_UP))
                .entryMax(entryMax.setScale(2, RoundingMode.HALF_UP))
                .stopLoss(stopLoss.setScale(2, RoundingMode.HALF_UP))
                .target1(target1.setScale(2, RoundingMode.HALF_UP))
                .target2(target2.setScale(2, RoundingMode.HALF_UP))
                .build();
    }
    
    /**
     * 포지션 사이즈 계산 (총 자산의 %)
     */
    private BigDecimal calculatePositionSize(TradingScore score,
                                            MacroEconomicStrategyEngine.InvestmentStrategy macroStrategy) {
        
        // 기본 포지션 사이즈: 점수에 비례
        BigDecimal baseSize = score.getTotalScore().multiply(new BigDecimal("10"));
        
        // 거시경제 전략 조정
        if (macroStrategy != null && macroStrategy.getPositionSizing() != null) {
            BigDecimal macroSize = macroStrategy.getPositionSizing().getPositionSizePercent();
            baseSize = baseSize.add(macroSize).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        }
        
        // 최소/최대 제한
        return baseSize.max(new BigDecimal("2"))
                .min(new BigDecimal("20"))
                .setScale(1, RoundingMode.HALF_UP);
    }
    
    /**
     * 결정 근거 생성
     */
    private String buildReasoning(TradingScore score, StockAnalysisDto analysis) {
        StringBuilder reasoning = new StringBuilder();
        reasoning.append("정량적 규칙 기반 결정: ");
        reasoning.append("총점=").append(score.getTotalScore().multiply(new BigDecimal("100")))
                .append("점. ");
        
        if (score.getRsiScore() != null) {
            reasoning.append("RSI 점수=").append(score.getRsiScore().multiply(new BigDecimal("100")))
                    .append("점. ");
        }
        if (score.getMacdScore() != null) {
            reasoning.append("MACD 점수=").append(score.getMacdScore().multiply(new BigDecimal("100")))
                    .append("점. ");
        }
        
        return reasoning.toString();
    }
    
    /**
     * 거래 점수
     */
    @Data
    @Builder
    public static class TradingScore {
        private BigDecimal totalScore;  // 총점 (0.0 ~ 1.0)
        private BigDecimal rsiScore;
        private BigDecimal macdScore;
    }
    
    /**
     * 거래 결정
     */
    @Data
    @Builder
    public static class TradingDecision {
        private DecisionType decisionType;
        private TradingScore score;
        private PriceLevels priceLevels;
        private BigDecimal positionSizePercent;
        private String reasoning;
        
        public enum DecisionType {
            BUY,   // 매수
            SELL,  // 매도
            HOLD   // 보유
        }
    }
    
    /**
     * 가격 레벨
     */
    @Data
    @Builder
    public static class PriceLevels {
        private BigDecimal entryMin;    // 진입가 하한
        private BigDecimal entryMax;    // 진입가 상한
        private BigDecimal stopLoss;    // 손절가
        private BigDecimal target1;     // 목표가 1
        private BigDecimal target2;     // 목표가 2
    }
}
