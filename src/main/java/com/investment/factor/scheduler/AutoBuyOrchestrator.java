package com.investment.factor.scheduler;

import com.investment.backtest.robo.RoboRebalanceScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 통합 자동매수 오케스트레이터.
 * 한 번 호출 시 (1) 로보(ETF) (2) 파이프라인(개별종목) 순서로 실행.
 * 각 스케줄러가 내부에서 리스크 게이트·일일 손실 한도 검사를 수행하므로, 오케스트레이터는 순서만 보장한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoBuyOrchestrator {

    private final RoboRebalanceScheduler roboRebalanceScheduler;
    private final PipelineExecutionScheduler pipelineExecutionScheduler;

    /**
     * 통합 자동매수 실행. dryRun이 null이면 서버 설정(auto-execute 등)에 따르고, non-null이면 해당 값으로 로보·파이프라인 각각 전달.
     *
     * @param dryRun true=실제 주문 없음, false=실제 주문 가능, null=설정 따름
     */
    public void run(Boolean dryRun) {
        log.info("자동매수(통합) 시작: dryRun={}", dryRun);

        boolean roboDryRun = dryRun != null ? dryRun : true;
        roboRebalanceScheduler.runNow(roboDryRun);

        pipelineExecutionScheduler.runNow(dryRun);

        log.info("자동매수(통합) 완료: dryRun={}", dryRun);
    }
}
