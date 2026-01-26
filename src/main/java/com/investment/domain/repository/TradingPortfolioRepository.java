package com.investment.domain.repository;

import com.investment.domain.entity.TradingPortfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface TradingPortfolioRepository extends JpaRepository<TradingPortfolio, String> {
    
    Optional<TradingPortfolio> findByTradingDate(LocalDate tradingDate);
    
    @Query("SELECT tp FROM TradingPortfolio tp WHERE tp.tradingDate = :date")
    Optional<TradingPortfolio> findTodayPortfolio(@Param("date") LocalDate date);
    
    @Query("SELECT tp FROM TradingPortfolio tp ORDER BY tp.tradingDate DESC")
    java.util.List<TradingPortfolio> findLatestPortfolios();
}
