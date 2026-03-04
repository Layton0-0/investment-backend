package com.investment.batch.tasklet;

import com.investment.factor.service.FactorDecayMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/**
 * 팩터 열화 검사 Job — 팩터별 5일 수익률 Sharpe 미달 시 Discord 알림 (월 1회 권장).
 */
@Component
@RequiredArgsConstructor
public class FactorDecayCheckTasklet implements Tasklet {

    private final FactorDecayMonitorService factorDecayMonitorService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        factorDecayMonitorService.checkAndSendAlerts();
        return RepeatStatus.FINISHED;
    }
}
