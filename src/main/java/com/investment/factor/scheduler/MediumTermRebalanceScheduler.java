package com.investment.factor.scheduler;

import com.investment.domain.entity.StrategyPosition;
import com.investment.domain.repository.PortfolioRepository;
import com.investment.domain.repository.StrategyPositionRepository;
import com.investment.strategy.domain.StrategyType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 중기(MEDIUM_TERM) 월 1회 리밸런싱 스케줄 훅.
 * "모멘텀 순위 하락 시 교체" 로직은 스텁 — 추후 시그널·모멘텀 재계산 연동.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MediumTermRebalanceScheduler {

    private final PortfolioRepository portfolioRepository;
    private final StrategyPositionRepository strategyPositionRepository;

    /**
     * 매월 1일 08:30 KST 실행. Spring Batch Job에서 호출. MEDIUM_TERM 보유 포지션 조회 후 재평가 훅(스텁).
     */
    public void runMonthlyRebalance() {
        try {
            List<String> accountNos = portfolioRepository.findDistinctAccountNos();
            for (String accountNo : accountNos) {
                List<StrategyPosition> positions = strategyPositionRepository
                        .findByAccountNoAndStrategyTypeAndExitDtIsNullOrderByEntryDtAsc(accountNo,
                                StrategyType.MEDIUM_TERM);
                log.info("중기 리밸런싱 훅: accountNo={}, MEDIUM_TERM positions={}", accountNo, positions.size());
                // TODO: 모멘텀 순위 재계산
            }
        } catch (Exception e) {
            log.warn("중기 리밸런싱 훅 실패", e);
        }
    }
}
