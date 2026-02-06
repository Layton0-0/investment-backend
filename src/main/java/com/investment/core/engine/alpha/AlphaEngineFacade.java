package com.investment.core.engine.alpha;

import com.investment.order.dto.OrderRequestDto;
import com.investment.strategy.domain.StrategyType;
import com.investment.strategy.service.StrategyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Alpha 엔진 파사드 — 기존 StrategyService에 위임.
 * Phase 1: 단일 진입점 제공. Phase 2+에서 ML 시그널·포트폴리오 최적화 연동.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlphaEngineFacade implements AlphaEngine {

    private final StrategyService strategyService;

    @Override
    public OrderRequestDto decideTradingAction(String accountNo, String symbol, String market, StrategyType strategyType) {
        return strategyService.decideTradingAction(accountNo, symbol, market, strategyType);
    }
}
