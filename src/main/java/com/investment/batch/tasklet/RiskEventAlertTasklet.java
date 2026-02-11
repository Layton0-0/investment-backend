package com.investment.batch.tasklet;

import com.investment.risk.service.RiskEventAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RiskEventAlertTasklet implements Tasklet {

    private final RiskEventAlertService riskEventAlertService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        riskEventAlertService.checkAndSendAlerts();
        return RepeatStatus.FINISHED;
    }
}
