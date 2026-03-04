package com.investment.core.engine.portfolio;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Phase 1: 포트폴리오 컴포넌트 스텁. Phase 2에서 실제 구현체(InverseVolatilityPortfolioService 등) 사용 시
 * investment.portfolio.mode=inverse-volatility 로 두면 이 스텁은 비활성화되고 TaxAwareOptimizerImpl/RebalancerImpl 사용.
 */
@Configuration
@ConditionalOnProperty(name = "investment.portfolio.mode", havingValue = "stub", matchIfMissing = true)
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
