package com.investment.batch.tasklet;

import com.investment.factor.scheduler.FactorCalculationScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FactorCalculationTasklet implements Tasklet {

    private final FactorCalculationScheduler factorCalculationScheduler;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        factorCalculationScheduler.runFactorCalculation();
        return RepeatStatus.FINISHED;
    }
}
