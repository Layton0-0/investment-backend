package com.investment.batch.tasklet;

import com.investment.governance.StrategyGovernanceCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/**
 * 전략 거버넌스 검사 Job — 정기 백테스트·열화 시 Discord 알림.
 */
@Component
@RequiredArgsConstructor
public class StrategyGovernanceCheckTasklet implements Tasklet {

    private final StrategyGovernanceCheckService strategyGovernanceCheckService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        strategyGovernanceCheckService.checkAndSendAlerts();
        return RepeatStatus.FINISHED;
    }
}
