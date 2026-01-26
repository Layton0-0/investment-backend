package com.investment.domain.repository;

import com.investment.domain.entity.TradingSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TradingSettingRepository extends JpaRepository<TradingSetting, String> {
    
    Optional<TradingSetting> findByAccountNo(String accountNo);
}
