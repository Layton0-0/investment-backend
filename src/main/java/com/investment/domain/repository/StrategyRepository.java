package com.investment.domain.repository;

import com.investment.domain.entity.Strategy;
import com.investment.strategy.domain.StrategyStatus;
import com.investment.strategy.domain.StrategyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StrategyRepository extends JpaRepository<Strategy, String> {
    
    List<Strategy> findByAccountNo(String accountNo);
    
    Optional<Strategy> findByAccountNoAndStrategyType(String accountNo, StrategyType strategyType);
    
    List<Strategy> findByAccountNoAndStatus(String accountNo, StrategyStatus status);
    
    List<Strategy> findByStatus(StrategyStatus status);
}
