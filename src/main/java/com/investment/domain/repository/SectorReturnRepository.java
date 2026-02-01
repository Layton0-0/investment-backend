package com.investment.domain.repository;

import com.investment.domain.entity.SectorReturn;
import com.investment.domain.entity.SectorReturnId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * 업종별 수익률 (TB_SECTOR_RETURN) Repository.
 */
public interface SectorReturnRepository extends JpaRepository<SectorReturn, SectorReturnId> {

    List<SectorReturn> findByBasDtAndMarketOrderByReturnPctDesc(LocalDate basDt, String market);
}
