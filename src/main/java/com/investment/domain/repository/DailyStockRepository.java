package com.investment.domain.repository;

import com.investment.domain.entity.DailyStock;
import com.investment.domain.entity.DailyStockId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * KRX 일별 시세 Repository.
 */
public interface DailyStockRepository extends JpaRepository<DailyStock, DailyStockId> {

    List<DailyStock> findByBasDtAndMarketOrderBySymbol(LocalDate basDt, String market, Pageable pageable);

    List<DailyStock> findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
            String symbol, String market, LocalDate from, LocalDate to);

    @Query("SELECT d FROM DailyStock d WHERE d.market = :market AND d.basDt BETWEEN :fromDt AND :toDt ORDER BY d.basDt, d.symbol")
    List<DailyStock> findByMarketAndBasDtBetween(
            @Param("market") String market,
            @Param("fromDt") LocalDate fromDt,
            @Param("toDt") LocalDate toDt);

    /** 상관관계 분석: 시장·종목 목록·기간별 일봉 조회 (수익률 계산용). */
    @Query("SELECT d FROM DailyStock d WHERE d.market = :market AND d.symbol IN :symbols AND d.basDt BETWEEN :fromDt AND :toDt ORDER BY d.basDt ASC, d.symbol")
    List<DailyStock> findByMarketAndSymbolInAndBasDtBetweenOrderByBasDtAsc(
            @Param("market") String market,
            @Param("symbols") List<String> symbols,
            @Param("fromDt") LocalDate fromDt,
            @Param("toDt") LocalDate toDt);

    @Query("SELECT d FROM DailyStock d WHERE d.basDt = :basDt AND d.market = :market AND d.trdVal IS NOT NULL AND d.trdVal >= :minTrdVal ORDER BY d.symbol")
    List<DailyStock> findByBasDtAndMarketAndTrdValGreaterThanEqual(
            @Param("basDt") LocalDate basDt,
            @Param("market") String market,
            @Param("minTrdVal") long minTrdVal);

    boolean existsByBasDtAndSymbol(LocalDate basDt, String symbol);

    /** 시장별 최근 기준일 (데이터 파이프라인 상태 API용). */
    @Query("SELECT MAX(d.basDt) FROM DailyStock d WHERE d.market = :market")
    Optional<LocalDate> findMaxBasDtByMarket(@Param("market") String market);

    /** 기준일 일봉 건수 (자동매매 준비 상태 API용). */
    long countByBasDt(LocalDate basDt);
}
