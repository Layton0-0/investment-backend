package com.investment.domain.repository;

import com.investment.domain.entity.StrategyPosition;
import com.investment.strategy.domain.StrategyType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

/**
 * 파이프라인 매수 포지션 Repository.
 */
public interface StrategyPositionRepository extends JpaRepository<StrategyPosition, Long> {

    List<StrategyPosition> findByAccountNoAndExitDtIsNullOrderByEntryDtAsc(String accountNo);

    List<StrategyPosition> findByAccountNoOrderByEntryDtDesc(String accountNo, Pageable pageable);

    List<StrategyPosition> findByAccountNoAndSymbolAndMarketAndExitDtIsNull(
            String accountNo, String symbol, String market);

    /**
     * 계좌·기간별 미청산(보유) 포지션 목록 (중기 리밸런싱 등).
     */
    List<StrategyPosition> findByAccountNoAndStrategyTypeAndExitDtIsNullOrderByEntryDtAsc(
            String accountNo, StrategyType strategyType);

    /**
     * 계좌별 미청산(보유) 포지션 건수 (자동투자 현황 요약용).
     */
    long countByAccountNoAndExitDtIsNull(String accountNo);

    /**
     * 보유 포지션이 있는 계좌번호 목록 (청산 스케줄러용).
     */
    @Query("SELECT DISTINCT p.accountNo FROM StrategyPosition p WHERE p.exitDt IS NULL")
    List<String> findDistinctAccountNosWithOpenPositions();

    /** 청산된 포지션 (성과 귀인용). exitDt 기준 내림차순. */
    List<StrategyPosition> findByAccountNoAndExitDtIsNotNullOrderByExitDtDesc(String accountNo);

    /** 기간별 청산 포지션 (성과 귀인 기간 필터). */
    List<StrategyPosition> findByAccountNoAndExitDtBetweenOrderByExitDtDesc(
            String accountNo, LocalDate exitDtStart, LocalDate exitDtEnd);
}
