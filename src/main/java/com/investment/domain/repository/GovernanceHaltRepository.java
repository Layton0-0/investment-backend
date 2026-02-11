package com.investment.domain.repository;

import com.investment.domain.entity.GovernanceHalt;
import com.investment.domain.entity.GovernanceHaltId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 전략 거버넌스 halt 저장소.
 */
public interface GovernanceHaltRepository extends JpaRepository<GovernanceHalt, GovernanceHaltId> {

    Optional<GovernanceHalt> findByMarketAndStrategyType(String market, String strategyType);

    /**
     * 활성 halt (CLEARED_AT IS NULL) 목록.
     */
    List<GovernanceHalt> findByClearedAtIsNull();
}
