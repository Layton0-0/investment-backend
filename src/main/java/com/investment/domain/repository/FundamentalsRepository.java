package com.investment.domain.repository;

import com.investment.domain.entity.Fundamentals;
import com.investment.domain.entity.FundamentalsId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 재무 데이터 (TB_FUNDAMENTALS) Repository.
 */
public interface FundamentalsRepository extends JpaRepository<Fundamentals, FundamentalsId> {

    Optional<Fundamentals> findByBasDtAndSymbolAndMarket(LocalDate basDt, String symbol, String market);
}
