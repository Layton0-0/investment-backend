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

        @Query("SELECT n FROM NewsItem n WHERE " +
                        "(:market IS NULL OR n.market = :market) AND " +
                        "(:source IS NULL OR n.source = :source) AND " +
                        "(:symbol IS NULL OR n.symbol = :symbol) AND " +
                        "(:fromAt IS NULL OR n.collectedAt >= :fromAt) AND " +
                        "(:toAt IS NULL OR n.collectedAt <= :toAt) " +
                        "ORDER BY n.collectedAt DESC")
        Page<NewsItem> findByFilters(
                        @Param("market") String market,
                        @Param("source") String source,
                        @Param("symbol") String symbol,
                        @Param("fromAt") LocalDateTime fromAt,
                        @Param("toAt") LocalDateTime toAt,
                        Pageable pageable);
}
