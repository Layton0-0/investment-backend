package com.investment.strategy.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 동적 결정된 전략 비중 (단기/중기/장기). 합=1.
 */
@Getter
@Builder
public class StrategyWeights {

    private final BigDecimal shortPct;
    private final BigDecimal midPct;
    private final BigDecimal longPct;
    /** 적용된 시장 레짐 이름 (감사/로깅용) */
    private final String regime;
}
