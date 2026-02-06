package com.investment.domain.repository;

import com.investment.domain.entity.NewsItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface NewsItemRepository extends JpaRepository<NewsItem, String> {

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
}
