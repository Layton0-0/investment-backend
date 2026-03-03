package com.investment.batch.tasklet;

import com.investment.backtest.robo.RoboRebalanceScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoboRebalanceTasklet implements Tasklet {

    private final RoboRebalanceScheduler roboRebalanceScheduler;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        roboRebalanceScheduler.runNow();
        return RepeatStatus.FINISHED;
    }
}
