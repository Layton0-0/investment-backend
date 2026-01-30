package com.investment.domain.repository;

import com.investment.domain.entity.Universe;
import com.investment.domain.entity.UniverseId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 유니버스(감시 대상 종목) Repository.
 */
public interface UniverseRepository extends JpaRepository<Universe, UniverseId> {

    List<Universe> findByBasDtAndMarketOrderBySymbol(LocalDate basDt, String market);

    @Modifying
    @Query("DELETE FROM Universe u WHERE u.basDt = :basDt AND u.market = :market")
    void deleteByBasDtAndMarket(@Param("basDt") LocalDate basDt, @Param("market") String market);

    long countByBasDtAndMarket(LocalDate basDt, String market);
}
