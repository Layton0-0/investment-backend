package com.investment.domain.repository;

import com.investment.domain.entity.TradingHalt;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Kill Switch 상태 저장소. 단일 행(id=1)만 사용.
 */
public interface TradingHaltRepository extends JpaRepository<TradingHalt, Integer> {
}
