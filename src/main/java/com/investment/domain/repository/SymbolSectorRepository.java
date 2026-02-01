package com.investment.domain.repository;

import com.investment.domain.entity.SymbolSector;
import com.investment.domain.entity.SymbolSectorId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 종목-업종 매핑 (TB_SYMBOL_SECTOR) Repository.
 */
public interface SymbolSectorRepository extends JpaRepository<SymbolSector, SymbolSectorId> {

    List<SymbolSector> findByMarketAndSectorCodeIn(String market, List<String> sectorCodes);
}
