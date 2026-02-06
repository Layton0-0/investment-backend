package com.investment.core.engine.portfolio;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Phase 1: 포트폴리오 컴포넌트 스텁. Phase 2에서 실제 구현체로 교체.
 */
@Configuration
public class StubPortfolioComponents {

    @Bean
    @ConditionalOnMissingBean(TaxAwareOptimizer.class)
    public TaxAwareOptimizer taxAwareOptimizer() {
        return (asOfDate, market, rawWeights) -> rawWeights;
    }

    @Bean
    @ConditionalOnMissingBean(Rebalancer.class)
    public Rebalancer rebalancer() {
        return (asOfDate, accountNo, market, targetWeights, totalValue) -> List.of();
    }
}
