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

/**
 * 시그널/팩터 점수 Repository.
 */
public interface SignalScoreRepository extends JpaRepository<SignalScore, SignalScoreId> {

    Page<SignalScore> findByBasDtAndMarket(LocalDate basDt, String market, Pageable pageable);

    Page<SignalScore> findByBasDtAndMarketAndSymbol(LocalDate basDt, String market, String symbol, Pageable pageable);

    Page<SignalScore> findByBasDtAndMarketAndFactorType(LocalDate basDt, String market, String factorType, Pageable pageable);

    @Query("SELECT s FROM SignalScore s WHERE (:basDt IS NULL OR s.basDt = :basDt) AND (:market IS NULL OR s.market = :market) AND (:symbol IS NULL OR s.symbol = :symbol) AND (:factorType IS NULL OR s.factorType = :factorType)")
    Page<SignalScore> findByFilters(
            @Param("basDt") LocalDate basDt,
            @Param("market") String market,
            @Param("symbol") String symbol,
            @Param("factorType") String factorType,
            Pageable pageable);

    List<SignalScore> findByBasDtAndMarketOrderBySymbol(LocalDate basDt, String market, Pageable pageable);

    long countByBasDtAndMarket(LocalDate basDt, String market);
}
