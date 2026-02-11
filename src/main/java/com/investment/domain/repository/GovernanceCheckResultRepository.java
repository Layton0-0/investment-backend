package com.investment.domain.repository;

import com.investment.domain.entity.GovernanceCheckResult;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 전략 거버넌스 검사 결과 저장소.
 */
public interface GovernanceCheckResultRepository extends JpaRepository<GovernanceCheckResult, Long> {

    /**
     * 최근 RUN_AT 기준 정렬하여 limit건 조회.
     */
    List<GovernanceCheckResult> findAllByOrderByRunAtDesc(Pageable pageable);
}
