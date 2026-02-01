package com.investment.domain.repository;

import com.investment.domain.entity.OrderFlow;
import com.investment.domain.entity.OrderFlowId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 수급 데이터 (TB_ORDER_FLOW) Repository.
 */
public interface OrderFlowRepository extends JpaRepository<OrderFlow, OrderFlowId> {

    Optional<OrderFlow> findByBasDtAndSymbolAndMarket(LocalDate basDt, String symbol, String market);
}
