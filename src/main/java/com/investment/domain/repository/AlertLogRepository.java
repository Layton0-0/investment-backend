package com.investment.domain.repository;

import com.investment.domain.entity.AlertLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Ops 알림 이력 조회.
 */
public interface AlertLogRepository extends JpaRepository<AlertLog, Long> {

    Page<AlertLog> findAllByOrderByOccurredAtDesc(Pageable pageable);

    Page<AlertLog> findByLevelOrderByOccurredAtDesc(String level, Pageable pageable);
}
