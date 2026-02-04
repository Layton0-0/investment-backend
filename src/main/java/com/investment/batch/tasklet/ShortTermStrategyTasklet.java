package com.investment.batch.tasklet;

import com.investment.strategy.scheduler.StrategyScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShortTermStrategyTasklet implements Tasklet {

    private final StrategyScheduler strategyScheduler;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        strategyScheduler.executeShortTermStrategies();
        return RepeatStatus.FINISHED;
    }
}
