package com.investment.batch.tasklet;

import com.investment.factor.scheduler.IntradayBreakoutScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IntradayBreakoutTasklet implements Tasklet {

    private final IntradayBreakoutScheduler intradayBreakoutScheduler;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        intradayBreakoutScheduler.runIntradayBreakout();
        return RepeatStatus.FINISHED;
    }
}
