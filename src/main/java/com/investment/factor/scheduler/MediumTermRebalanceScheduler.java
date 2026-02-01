package com.investment.factor.scheduler;

import com.investment.domain.repository.PortfolioRepository;
import com.investment.domain.repository.StrategyPositionRepository;
import com.investment.strategy.domain.StrategyType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
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
     * 매월 1일 08:30 KST 실행. MEDIUM_TERM 보유 포지션 조회 후 재평가 훅(스텁).
     */
    @Scheduled(cron = "${investment.pipeline.medium-term-rebalance-cron:0 30 8 1 * *}")
    public void runMonthlyRebalance() {
        List<String> accountNos = portfolioRepository.findDistinctAccountNos();
        for (String accountNo : accountNos) {
            try {
                var positions = strategyPositionRepository.findByAccountNoAndStrategyTypeAndExitDtIsNullOrderByEntryDtAsc(
                        accountNo, StrategyType.MEDIUM_TERM);
                if (positions.isEmpty()) {
                    continue;
                }
                log.debug("중기 리밸런싱 훅: accountNo={}, MEDIUM_TERM 보유 {}건 (모멘텀 재평가 로직 스텁)",
                        accountNo, positions.size());
                // TODO: 모멘텀 순위 재계산 후 하락 종목 매도/교체
            } catch (Exception e) {
                log.warn("중기 리밸런싱 훅 실패: accountNo={}, error={}", accountNo, e.getMessage());
            }
        }
    }
}
