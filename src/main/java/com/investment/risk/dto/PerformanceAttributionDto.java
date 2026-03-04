package com.investment.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 성과 귀인: 팩터/전략별 수익 기여도.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceAttributionDto {

    /** 전체 실현 손익 (원). 청산 포지션 합계 */
    private BigDecimal totalRealizedPnl;

    /** 팩터(시그널)별 기여: 시그널 타입, 손익, 기여율(%) */
    private List<FactorContribution> byFactor;

    /** 전략별 기여: 전략 타입, 손익, 기여율(%) */
    private List<StrategyContribution> byStrategy;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FactorContribution {
        private String factor;
        private BigDecimal pnl;
        private BigDecimal contributionPct;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StrategyContribution {
        private String strategy;
        private BigDecimal pnl;
        private BigDecimal contributionPct;
    }
}
