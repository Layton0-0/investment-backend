package com.investment.core.engine.alpha;

import com.investment.order.dto.OrderRequestDto;
import com.investment.strategy.domain.StrategyType;

/**
 * Alpha(전략) 엔진 — 시그널 생성 및 매매 결정 진입점.
 * 기관급 아키텍처 2.0: Alpha - Risk - Execution 분리에서 Alpha 역할.
 */
public interface AlphaEngine {

    /**
     * 전략 타입에 따른 매매 결정.
     *
     * @param accountNo    계좌번호
     * @param symbol       종목코드
     * @param market       시장 (KR, US)
     * @param strategyType 전략 타입
     * @return 주문 요청 DTO (매수/매도 시), null (보유 시)
     */
    OrderRequestDto decideTradingAction(String accountNo, String symbol, String market, StrategyType strategyType);
}
