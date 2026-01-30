package com.investment.domain.repository;

import com.investment.domain.entity.StrategyPosition;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 파이프라인 매수 포지션 Repository.
 */
public interface StrategyPositionRepository extends JpaRepository<StrategyPosition, Long> {

    List<StrategyPosition> findByAccountNoAndExitDtIsNullOrderByEntryDtAsc(String accountNo);

    List<StrategyPosition> findByAccountNoOrderByEntryDtDesc(String accountNo, Pageable pageable);
}
