package com.investment.factor.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 단기(최근 5영업일) Hit Ratio·Profit Factor 열화 시 해당 전략 배정 자본을 절반으로 제한.
 * 레짐 전환에 대한 거버넌스 반응 속도를 높인다.
 *
 * @see PipelineExecutionScheduler
 * @see RiskGateService
 */
@Service
public class CapitalDrawdownConstraintService {

    @Value("${investment.risk.capital-drawdown-constraint-enabled:false}")
    private boolean enabled = false;

    /**
     * (market, strategyType)별 자본 배율. 1.0 = 제한 없음, 0.5 = 절반으로 제한.
     * 단기(5일) 메트릭이 장기(60일) 대비 2σ 이상 열화 시 0.5 반환.
     * 데이터·집계 연동 전에는 1.0 반환.
     */
    public BigDecimal getCapitalMultiplier(String market, String strategyType) {
        if (!enabled || market == null || strategyType == null) {
            return BigDecimal.ONE;
        }
        // TODO: 최근 5영업일 vs 60일 Hit Ratio·Profit Factor 비교, 2σ 하락 시 0.5
        return BigDecimal.ONE;
    }
}
