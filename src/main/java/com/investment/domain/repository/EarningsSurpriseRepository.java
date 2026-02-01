package com.investment.domain.repository;

import com.investment.domain.entity.EarningsSurprise;
import com.investment.domain.entity.EarningsSurpriseId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 어닝 서프라이즈 (TB_EARNINGS_SURPRISE) Repository.
 */
public interface EarningsSurpriseRepository extends JpaRepository<EarningsSurprise, EarningsSurpriseId> {

    @Query("SELECT e FROM EarningsSurprise e WHERE e.market = :market AND e.reportDt >= :fromDt ORDER BY e.surpriseScore DESC")
    List<EarningsSurprise> findByMarketAndReportDtGreaterThanEqualOrderBySurpriseScoreDesc(
            @Param("market") String market,
            @Param("fromDt") LocalDate fromDt);
}
