package com.investment.strategy.engine;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 거시경제 전략 엔진
 * 
 * 거시경제 지표(금리, 인플레이션, GDP, 환율, VIX 등)를 기반으로
 * 투자 자산 운용 총괄이 직접 투자하듯이 전략을 결정합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MacroEconomicStrategyEngine {
    
    /**
     * 거시경제 환경 분석 및 투자 전략 결정
     * 
     * @param indicators 거시경제 지표
     * @return 투자 전략 결정 결과
     */
    public InvestmentStrategy decideStrategy(MacroEconomicIndicators indicators) {
        log.info("거시경제 전략 결정: indicators={}", indicators);
        
        // 1. 시장 레짐 판단
        MarketRegime regime = determineMarketRegime(indicators);
        
        // 2. 리스크 레벨 계산
        BigDecimal riskLevel = calculateRiskLevel(indicators, regime);
        
        // 3. 자산 배분 전략 결정
        AssetAllocation allocation = determineAssetAllocation(indicators, regime, riskLevel);
        
        // 4. 포지션 사이징 결정
        PositionSizing sizing = determinePositionSizing(indicators, regime, riskLevel);
        
        // 5. 진입/청산 타이밍 결정
        TimingStrategy timing = determineTimingStrategy(indicators, regime);
        
        return InvestmentStrategy.builder()
                .regime(regime)
                .riskLevel(riskLevel)
                .allocation(allocation)
                .positionSizing(sizing)
                .timing(timing)
                .reasoning(buildReasoning(indicators, regime, riskLevel))
                .build();
    }
    
    /**
     * 시장 레짐 판단
     */
    private MarketRegime determineMarketRegime(MacroEconomicIndicators indicators) {
        // VIX 기반 변동성 레짐
        if (indicators.getVix() != null) {
            if (indicators.getVix().compareTo(new BigDecimal("30")) > 0) {
                return MarketRegime.HIGH_VOLATILITY;
            } else if (indicators.getVix().compareTo(new BigDecimal("20")) > 0) {
                return MarketRegime.MODERATE_VOLATILITY;
            }
        }
        
        // 금리 환경 판단
        if (indicators.getInterestRate() != null) {
            if (indicators.getInterestRate().compareTo(new BigDecimal("5.0")) > 0) {
                return MarketRegime.HIGH_INTEREST_RATE;
            } else if (indicators.getInterestRate().compareTo(new BigDecimal("2.0")) < 0) {
                return MarketRegime.LOW_INTEREST_RATE;
            }
        }
        
        // 인플레이션 환경 판단
        if (indicators.getInflationRate() != null) {
            if (indicators.getInflationRate().compareTo(new BigDecimal("3.0")) > 0) {
                return MarketRegime.HIGH_INFLATION;
            } else if (indicators.getInflationRate().compareTo(new BigDecimal("1.0")) < 0) {
                return MarketRegime.LOW_INFLATION;
            }
        }
        
        // GDP 성장률 판단
        if (indicators.getGdpGrowthRate() != null) {
            if (indicators.getGdpGrowthRate().compareTo(new BigDecimal("3.0")) > 0) {
                return MarketRegime.GROWTH;
            } else if (indicators.getGdpGrowthRate().compareTo(new BigDecimal("0")) < 0) {
                return MarketRegime.RECESSION;
            }
        }
        
        return MarketRegime.NORMAL;
    }
    
    /**
     * 리스크 레벨 계산
     */
    private BigDecimal calculateRiskLevel(MacroEconomicIndicators indicators, MarketRegime regime) {
        BigDecimal riskLevel = new BigDecimal("0.5"); // 기본값
        
        // VIX 기반 조정
        if (indicators.getVix() != null) {
            if (indicators.getVix().compareTo(new BigDecimal("30")) > 0) {
                riskLevel = riskLevel.add(new BigDecimal("0.3"));
            } else if (indicators.getVix().compareTo(new BigDecimal("15")) < 0) {
                riskLevel = riskLevel.subtract(new BigDecimal("0.2"));
            }
        }
        
        // 금리 기반 조정
        if (indicators.getInterestRate() != null) {
            if (indicators.getInterestRate().compareTo(new BigDecimal("5.0")) > 0) {
                riskLevel = riskLevel.add(new BigDecimal("0.2"));
            }
        }
        
        // 레짐 기반 조정
        switch (regime) {
            case HIGH_VOLATILITY:
            case RECESSION:
                riskLevel = riskLevel.add(new BigDecimal("0.3"));
                break;
            case GROWTH:
            case LOW_VOLATILITY:
                riskLevel = riskLevel.subtract(new BigDecimal("0.2"));
                break;
        }
        
        // 0.0 ~ 1.0 범위로 제한
        if (riskLevel.compareTo(BigDecimal.ONE) > 0) {
            riskLevel = BigDecimal.ONE;
        }
        if (riskLevel.compareTo(BigDecimal.ZERO) < 0) {
            riskLevel = BigDecimal.ZERO;
        }
        
        return riskLevel.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * 자산 배분 전략 결정
     */
    private AssetAllocation determineAssetAllocation(
            MacroEconomicIndicators indicators, 
            MarketRegime regime, 
            BigDecimal riskLevel) {
        
        BigDecimal equityAllocation = new BigDecimal("60"); // 기본 주식 배분
        BigDecimal bondAllocation = new BigDecimal("30"); // 기본 채권 배분
        BigDecimal cashAllocation = new BigDecimal("10"); // 기본 현금 배분
        
        // 레짐별 조정
        switch (regime) {
            case HIGH_VOLATILITY:
                equityAllocation = equityAllocation.subtract(new BigDecimal("20"));
                cashAllocation = cashAllocation.add(new BigDecimal("20"));
                break;
            case RECESSION:
                equityAllocation = equityAllocation.subtract(new BigDecimal("30"));
                bondAllocation = bondAllocation.add(new BigDecimal("20"));
                cashAllocation = cashAllocation.add(new BigDecimal("10"));
                break;
            case GROWTH:
                equityAllocation = equityAllocation.add(new BigDecimal("20"));
                bondAllocation = bondAllocation.subtract(new BigDecimal("15"));
                cashAllocation = cashAllocation.subtract(new BigDecimal("5"));
                break;
            case LOW_INTEREST_RATE:
                equityAllocation = equityAllocation.add(new BigDecimal("10"));
                bondAllocation = bondAllocation.subtract(new BigDecimal("10"));
                break;
            case HIGH_INTEREST_RATE:
                equityAllocation = equityAllocation.subtract(new BigDecimal("10"));
                bondAllocation = bondAllocation.add(new BigDecimal("10"));
                break;
        }
        
        // 리스크 레벨 기반 조정
        BigDecimal riskAdjustment = riskLevel.subtract(new BigDecimal("0.5"))
                .multiply(new BigDecimal("20"));
        equityAllocation = equityAllocation.add(riskAdjustment);
        bondAllocation = bondAllocation.subtract(riskAdjustment.multiply(new BigDecimal("0.5")));
        cashAllocation = cashAllocation.subtract(riskAdjustment.multiply(new BigDecimal("0.5")));
        
        // 최소/최대 제한
        equityAllocation = equityAllocation.max(new BigDecimal("20"))
                .min(new BigDecimal("90"));
        bondAllocation = bondAllocation.max(new BigDecimal("0"))
                .min(new BigDecimal("50"));
        cashAllocation = cashAllocation.max(new BigDecimal("5"))
                .min(new BigDecimal("40"));
        
        // 합계가 100%가 되도록 정규화
        BigDecimal total = equityAllocation.add(bondAllocation).add(cashAllocation);
        equityAllocation = equityAllocation.divide(total, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        bondAllocation = bondAllocation.divide(total, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        cashAllocation = cashAllocation.divide(total, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        
        return AssetAllocation.builder()
                .equityPercent(equityAllocation.setScale(1, RoundingMode.HALF_UP))
                .bondPercent(bondAllocation.setScale(1, RoundingMode.HALF_UP))
                .cashPercent(cashAllocation.setScale(1, RoundingMode.HALF_UP))
                .build();
    }
    
    /**
     * 포지션 사이징 결정
     */
    private PositionSizing determinePositionSizing(
            MacroEconomicIndicators indicators,
            MarketRegime regime,
            BigDecimal riskLevel) {
        
        // 기본 포지션 사이즈 (총 자산의 %)
        BigDecimal basePositionSize = new BigDecimal("10");
        
        // 리스크 레벨 기반 조정
        BigDecimal riskAdjustment = BigDecimal.ONE.subtract(riskLevel)
                .multiply(new BigDecimal("5"));
        BigDecimal positionSize = basePositionSize.add(riskAdjustment);
        
        // 레짐별 조정
        switch (regime) {
            case HIGH_VOLATILITY:
            case RECESSION:
                positionSize = positionSize.multiply(new BigDecimal("0.5"));
                break;
            case GROWTH:
            case LOW_VOLATILITY:
                positionSize = positionSize.multiply(new BigDecimal("1.2"));
                break;
        }
        
        // 최소/최대 제한
        positionSize = positionSize.max(new BigDecimal("2"))
                .min(new BigDecimal("20"));
        
        // 최대 손실 한도 (포지션의 %)
        BigDecimal maxLossPercent = riskLevel.multiply(new BigDecimal("5"))
                .add(new BigDecimal("2"));
        maxLossPercent = maxLossPercent.max(new BigDecimal("2"))
                .min(new BigDecimal("7"));
        
        return PositionSizing.builder()
                .positionSizePercent(positionSize.setScale(1, RoundingMode.HALF_UP))
                .maxLossPercent(maxLossPercent.setScale(1, RoundingMode.HALF_UP))
                .build();
    }
    
    /**
     * 진입/청산 타이밍 전략 결정
     */
    private TimingStrategy determineTimingStrategy(
            MacroEconomicIndicators indicators,
            MarketRegime regime) {
        
        TimingStrategy.TimingType timingType = TimingStrategy.TimingType.NORMAL;
        BigDecimal urgency = new BigDecimal("0.5");
        
        // 레짐별 타이밍 조정
        switch (regime) {
            case HIGH_VOLATILITY:
                timingType = TimingStrategy.TimingType.CONSERVATIVE;
                urgency = new BigDecimal("0.3");
                break;
            case RECESSION:
                timingType = TimingStrategy.TimingType.DEFENSIVE;
                urgency = new BigDecimal("0.2");
                break;
            case GROWTH:
                timingType = TimingStrategy.TimingType.AGGRESSIVE;
                urgency = new BigDecimal("0.7");
                break;
        }
        
        return TimingStrategy.builder()
                .timingType(timingType)
                .urgency(urgency.setScale(2, RoundingMode.HALF_UP))
                .build();
    }
    
    /**
     * 전략 결정 근거 생성
     */
    private String buildReasoning(
            MacroEconomicIndicators indicators,
            MarketRegime regime,
            BigDecimal riskLevel) {
        
        StringBuilder reasoning = new StringBuilder();
        reasoning.append("거시경제 분석 결과: ");
        reasoning.append("시장 레짐=").append(regime).append(", ");
        reasoning.append("리스크 레벨=").append(riskLevel.multiply(new BigDecimal("100")))
                .append("%. ");
        
        if (indicators.getVix() != null) {
            reasoning.append("VIX=").append(indicators.getVix()).append(", ");
        }
        if (indicators.getInterestRate() != null) {
            reasoning.append("금리=").append(indicators.getInterestRate()).append("%, ");
        }
        if (indicators.getInflationRate() != null) {
            reasoning.append("인플레이션=").append(indicators.getInflationRate()).append("%, ");
        }
        if (indicators.getGdpGrowthRate() != null) {
            reasoning.append("GDP 성장률=").append(indicators.getGdpGrowthRate()).append("%. ");
        }
        
        return reasoning.toString();
    }
    
    /**
     * 시장 레짐
     */
    public enum MarketRegime {
        HIGH_VOLATILITY,      // 고변동성
        MODERATE_VOLATILITY,  // 중간 변동성
        LOW_VOLATILITY,       // 저변동성
        HIGH_INTEREST_RATE,   // 고금리
        LOW_INTEREST_RATE,    // 저금리
        HIGH_INFLATION,        // 고인플레이션
        LOW_INFLATION,        // 저인플레이션
        GROWTH,                // 성장
        RECESSION,            // 경기침체
        NORMAL                 // 정상
    }
    
    /**
     * 거시경제 지표
     */
    @Data
    @Builder
    public static class MacroEconomicIndicators {
        private BigDecimal vix;              // 변동성 지수
        private BigDecimal interestRate;      // 금리 (%)
        private BigDecimal inflationRate;     // 인플레이션율 (%)
        private BigDecimal gdpGrowthRate;     // GDP 성장률 (%)
        private BigDecimal unemploymentRate;  // 실업률 (%)
        private BigDecimal dollarIndex;      // 달러 인덱스 (DXY)
        private BigDecimal oilPrice;         // 유가
        private BigDecimal goldPrice;        // 금 가격
    }
    
    /**
     * 투자 전략
     */
    @Data
    @Builder
    public static class InvestmentStrategy {
        private MarketRegime regime;
        private BigDecimal riskLevel;
        private AssetAllocation allocation;
        private PositionSizing positionSizing;
        private TimingStrategy timing;
        private String reasoning;
    }
    
    /**
     * 자산 배분
     */
    @Data
    @Builder
    public static class AssetAllocation {
        private BigDecimal equityPercent;  // 주식 배분 (%)
        private BigDecimal bondPercent;    // 채권 배분 (%)
        private BigDecimal cashPercent;    // 현금 배분 (%)
    }
    
    /**
     * 포지션 사이징
     */
    @Data
    @Builder
    public static class PositionSizing {
        private BigDecimal positionSizePercent;  // 포지션 크기 (총 자산의 %)
        private BigDecimal maxLossPercent;       // 최대 손실 한도 (포지션의 %)
    }
    
    /**
     * 타이밍 전략
     */
    @Data
    @Builder
    public static class TimingStrategy {
        private TimingType timingType;
        private BigDecimal urgency;  // 긴급도 (0.0 ~ 1.0)
        
        public enum TimingType {
            AGGRESSIVE,    // 공격적
            NORMAL,        // 정상
            CONSERVATIVE,  // 보수적
            DEFENSIVE      // 방어적
        }
    }
}
