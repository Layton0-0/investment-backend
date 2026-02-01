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
     * 사용자 ID와 서버 타입으로 토큰 조회 (모의/실전별 토큰)
     *
     * @param userId     사용자 ID
     * @param serverType 서버 타입 ("1": 모의투자, "0": 실거래)
     */
    Optional<KoreaInvestmentToken> findByUserIdAndServerType(String userId, String serverType);

    /**
     * 사용자 ID로 토큰 조회 (호환용, serverType 미지정 시 첫 번째 반환)
     *
     * @deprecated 서버 타입별 조회 시 {@link #findByUserIdAndServerType(String, String)} 사용
     */
    @Deprecated
    Optional<KoreaInvestmentToken> findByUserId(String userId);

    /**
     * 사용자 ID로 토큰 삭제
     */
    void deleteByUserId(String userId);
}
