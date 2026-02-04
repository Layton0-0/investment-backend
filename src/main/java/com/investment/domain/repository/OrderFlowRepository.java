package com.investment.domain.repository;

import com.investment.domain.entity.OrderFlow;
import com.investment.domain.entity.OrderFlowId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 수급 데이터 (TB_ORDER_FLOW) Repository.
 */
public interface OrderFlowRepository extends JpaRepository<OrderFlow, OrderFlowId> {

    Optional<OrderFlow> findByBasDtAndSymbolAndMarket(LocalDate basDt, String symbol, String market);

    /** 최근 N일 수급 조회 (5일 연속 순매수 판별용). basDt 기준 이전 거래일 포함, basDt 내림차순 */
    List<OrderFlow> findBySymbolAndMarketAndBasDtBetweenOrderByBasDtDesc(String symbol, String market, LocalDate from,
            LocalDate to);
}
