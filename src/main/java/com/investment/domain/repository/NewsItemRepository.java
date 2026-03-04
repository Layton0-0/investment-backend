package com.investment.domain.repository;

import com.investment.domain.entity.NewsItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NewsItemRepository extends JpaRepository<NewsItem, String> {

        /** 원천별 최근 수집 시각 (데이터 파이프라인 상태 API용). */
        @Query("SELECT MAX(n.collectedAt) FROM NewsItem n WHERE n.source = :source")
        Optional<LocalDateTime> findMaxCollectedAtBySource(@Param("source") String source);

        /**
         * 원천·URL 기준 중복 여부 (수집 저장 시 중복 방지용).
         * URL은 DB 인덱스가 255자이므로 255자 초과 시 동일 URL로 간주할 수 있음.
         */
        boolean existsBySourceAndUrl(String source, String url);

        @Query("SELECT n FROM NewsItem n WHERE " +
                        "n.market = COALESCE(:market, n.market) AND " +
                        "n.source = COALESCE(:source, n.source) AND " +
                        "n.itemType = COALESCE(:itemType, n.itemType) AND " +
                        "n.symbol = COALESCE(:symbol, n.symbol) AND " +
                        "n.title LIKE COALESCE(:titlePattern, n.title) AND " +
                        "n.collectedAt >= COALESCE(:fromAt, n.collectedAt) AND " +
                        "n.collectedAt <= COALESCE(:toAt, n.collectedAt) " +
                        "ORDER BY n.collectedAt DESC")
        Page<NewsItem> findByFilters(
                        @Param("market") String market,
                        @Param("source") String source,
                        @Param("itemType") String itemType,
                        @Param("symbol") String symbol,
                        @Param("titlePattern") String titlePattern,
                        @Param("fromAt") LocalDateTime fromAt,
                        @Param("toAt") LocalDateTime toAt,
                        Pageable pageable);

        /**
         * 시그널 반영 대상 공시 (DART 키워드 매칭·8-K) 최근 N일.
         * eventType 'DART_SIGNAL:%' 또는 '8K'이고, collectedAt >= since, market 일치.
         */
        @Query("SELECT n FROM NewsItem n WHERE (n.eventType LIKE 'DART_SIGNAL:%' OR n.eventType = '8K') " +
                        "AND n.collectedAt >= :since AND n.market = :market ORDER BY n.collectedAt DESC")
        List<NewsItem> findSignalRelevantSince(@Param("market") String market, @Param("since") LocalDateTime since);

        /** 시장·수집 시각 기준 최근 뉴스 (센티멘트 스코어링용, 최근 24시간 등). */
        @Query("SELECT n FROM NewsItem n WHERE n.market = :market AND n.collectedAt >= :since ORDER BY n.collectedAt DESC")
        List<NewsItem> findByMarketAndCollectedAtSince(@Param("market") String market, @Param("since") LocalDateTime since);
}
