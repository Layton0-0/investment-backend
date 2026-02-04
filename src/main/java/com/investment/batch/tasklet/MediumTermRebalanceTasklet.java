package com.investment.batch.tasklet;

import com.investment.factor.scheduler.MediumTermRebalanceScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MediumTermRebalanceTasklet implements Tasklet {

    private final MediumTermRebalanceScheduler mediumTermRebalanceScheduler;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        mediumTermRebalanceScheduler.runMonthlyRebalance();
        return RepeatStatus.FINISHED;
    }
}
