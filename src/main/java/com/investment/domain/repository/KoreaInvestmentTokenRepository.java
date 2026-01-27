package com.investment.domain.repository;

import com.investment.domain.entity.KoreaInvestmentToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 한국투자증권 토큰 리포지토리
 */
@Repository
public interface KoreaInvestmentTokenRepository extends JpaRepository<KoreaInvestmentToken, String> {
    
    /**
     * 사용자 ID로 토큰 조회
     */
    Optional<KoreaInvestmentToken> findByUserId(String userId);
    
    /**
     * 사용자 ID로 토큰 삭제
     */
    void deleteByUserId(String userId);
}
