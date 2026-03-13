package com.investment.governance;

import com.investment.common.security.LogMaskingUtil;
import com.investment.domain.entity.GovernanceCheckResult;
import com.investment.domain.entity.GovernanceHalt;
import com.investment.domain.repository.GovernanceCheckResultRepository;
import com.investment.domain.repository.GovernanceHaltRepository;
import com.investment.ops.dto.GovernanceCheckResultDto;
import com.investment.ops.dto.GovernanceHaltDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 전략 거버넌스 halt 및 검사 결과 조회/해제.
 * 파이프라인 실행 시 isHalted로 스킵 여부 판단, Admin API에서 결과·halt 목록·해제 제공.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GovernanceHaltService {

    private static final DateTimeFormatter ISO_INSTANT = DateTimeFormatter.ISO_INSTANT;

    private final GovernanceHaltRepository governanceHaltRepository;
    private final GovernanceCheckResultRepository governanceCheckResultRepository;

    /**
     * 해당 (market, strategyType)이 현재 halt 상태인지.
     */
    public boolean isHalted(String market, String strategyType) {
        Optional<GovernanceHalt> opt = governanceHaltRepository.findByMarketAndStrategyType(market, strategyType);
        return opt.map(GovernanceHalt::isActive).orElse(false);
    }

    /**
     * 열화 시 halt 등록 (StrategyGovernanceCheckService에서만 호출).
     * 이미 활성 halt가 있으면 halted_at만 갱신.
     */
    @Transactional
    public void setHalt(String market, String strategyType, String reason) {
        Optional<GovernanceHalt> existing = governanceHaltRepository.findByMarketAndStrategyType(market, strategyType);
        if (existing.isPresent()) {
            GovernanceHalt h = existing.get();
            if (h.isActive()) {
                log.debug("Governance halt already active: market={}, strategyType={}", market, strategyType);
                return;
            }
            h.reactivate(reason);
            governanceHaltRepository.save(h);
            log.info("Governance halt reactivated: market={}, strategyType={}", market, strategyType);
        } else {
            governanceHaltRepository.save(GovernanceHalt.create(market, strategyType, reason));
            log.info("Governance halt set: market={}, strategyType={}, reason={}", market, strategyType, reason);
        }
    }

    /**
     * halt 해제 (Admin API에서 호출).
     */
    @Transactional
    public void clearHalt(String market, String strategyType, String clearedBy) {
        Optional<GovernanceHalt> opt = governanceHaltRepository.findByMarketAndStrategyType(market, strategyType);
        if (opt.isEmpty()) {
            log.debug("No governance halt to clear: market={}, strategyType={}", market, strategyType);
            return;
        }
        GovernanceHalt h = opt.get();
        if (!h.isActive()) {
            log.debug("Governance halt already cleared: market={}, strategyType={}", market, strategyType);
            return;
        }
        String who = clearedBy != null ? clearedBy : "admin";
        h.clear(who);
        governanceHaltRepository.save(h);
        log.info("Governance halt cleared: market={}, strategyType={}, clearedBy={}", market, strategyType, LogMaskingUtil.maskUserId(who));
    }

    /**
     * 현재 활성 halt 목록.
     */
    public List<GovernanceHaltDto> getActiveHalts() {
        return governanceHaltRepository.findByClearedAtIsNull().stream()
                .map(this::toHaltDto)
                .collect(Collectors.toList());
    }

    /**
     * 최근 검사 결과 (RUN_AT 내림차순, limit건).
     */
    public List<GovernanceCheckResultDto> getRecentResults(int limit) {
        return governanceCheckResultRepository.findAllByOrderByRunAtDesc(PageRequest.of(0, Math.min(limit, 500)))
                .stream()
                .map(this::toResultDto)
                .collect(Collectors.toList());
    }

    private GovernanceHaltDto toHaltDto(GovernanceHalt h) {
        return GovernanceHaltDto.builder()
                .market(h.getMarket())
                .strategyType(h.getStrategyType())
                .haltedAt(h.getHaltedAt() != null ? h.getHaltedAt().toString() : null)
                .reason(h.getReason())
                .build();
    }

    private GovernanceCheckResultDto toResultDto(GovernanceCheckResult r) {
        boolean degraded = "Y".equals(r.getDegraded());
        return GovernanceCheckResultDto.builder()
                .id(r.getId())
                .runAt(r.getRunAt() != null ? r.getRunAt().toString() : null)
                .market(r.getMarket())
                .strategyType(r.getStrategyType())
                .passed(!degraded)
                .mddPct(r.getMddPct())
                .sharpeRatio(r.getSharpeRatio())
                .message(degraded ? "Degraded" : "Passed")
                .degraded(degraded)
                .startDate(r.getStartDate())
                .endDate(r.getEndDate())
                .createdAt(r.getCreatedAt() != null ? r.getCreatedAt().toString() : null)
                .build();
    }
}
