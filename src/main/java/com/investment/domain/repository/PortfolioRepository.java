package com.investment.domain.repository;

import com.investment.domain.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, String> {
    
    List<Portfolio> findByAccountNo(String accountNo);
    
    Optional<Portfolio> findByAccountNoAndSymbol(String accountNo, String symbol);
    
    @Query("SELECT SUM(p.currentPrice * p.quantity) FROM Portfolio p WHERE p.accountNo = :accountNo")
    java.math.BigDecimal getTotalPortfolioValue(@Param("accountNo") String accountNo);
    
    @Query("SELECT DISTINCT p.accountNo FROM Portfolio p ORDER BY p.accountNo")
    List<String> findDistinctAccountNos();
}
