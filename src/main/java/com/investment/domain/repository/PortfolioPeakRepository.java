package com.investment.domain.repository;

import com.investment.domain.entity.PortfolioPeak;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 계좌별 평가액 피크. MDD 계산용.
 */
public interface PortfolioPeakRepository extends JpaRepository<PortfolioPeak, String> {

    Optional<PortfolioPeak> findByAccountNo(String accountNo);
}
