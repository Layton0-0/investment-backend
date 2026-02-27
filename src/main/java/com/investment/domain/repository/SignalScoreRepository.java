package com.investment.domain.repository;

import com.investment.domain.entity.SignalScore;
import com.investment.domain.entity.SignalScoreId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 시그널/팩터 점수 Repository.
 */
public interface SignalScoreRepository extends JpaRepository<SignalScore, SignalScoreId> {

    Optional<SignalScore> findByBasDtAndSymbolAndFactorTypeAndMarket(
            LocalDate basDt, String symbol, String factorType, String market);

    Page<SignalScore> findByBasDtAndMarket(LocalDate basDt, String market, Pageable pageable);

    Page<SignalScore> findByBasDtAndMarketAndSymbol(LocalDate basDt, String market, String symbol, Pageable pageable);

    Page<SignalScore> findByBasDtAndMarketAndFactorType(LocalDate basDt, String market, String factorType, Pageable pageable);

    // COALESCE 사용: PostgreSQL에서 "? IS NULL" 시 파라미터 타입 추론 불가(42P18) 방지
    @Query("SELECT s FROM SignalScore s WHERE s.basDt = COALESCE(:basDt, s.basDt) AND s.market = COALESCE(:market, s.market) AND s.symbol = COALESCE(:symbol, s.symbol) AND s.factorType = COALESCE(:factorType, s.factorType)")
    Page<SignalScore> findByFilters(
            @Param("basDt") LocalDate basDt,
            @Param("market") String market,
            @Param("symbol") String symbol,
            @Param("factorType") String factorType,
            Pageable pageable);

    List<SignalScore> findByBasDtAndMarketOrderBySymbol(LocalDate basDt, String market, Pageable pageable);

    long countByBasDtAndMarket(LocalDate basDt, String market);

    /** 기준일 시그널 건수 (자동매매 준비 상태 API용). */
    long countByBasDt(LocalDate basDt);
}
