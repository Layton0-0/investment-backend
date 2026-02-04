package com.investment.strategy.engine;

import com.investment.strategy.engine.MacroEconomicStrategyEngine.MacroEconomicIndicators;

import java.util.Optional;

/**
 * 거시경제 지표(VIX 등) 제공.
 * 리스크 게이트에서 사용. 미구성 시 Optional.empty() 반환(기존 null/스텁 동작 유지).
 */
public interface MacroIndicatorProvider {

    /**
     * 현재 거시경제 지표 조회.
     * VIX·금리 등은 외부 API/스크립트 연동 시 설정.
     *
     * @return 지표 (미제공 시 empty)
     */
    Optional<MacroEconomicIndicators> getCurrentIndicators();
}
