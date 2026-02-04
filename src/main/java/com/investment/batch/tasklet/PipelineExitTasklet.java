package com.investment.batch.tasklet;

import com.investment.factor.scheduler.PipelineExitScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PipelineExitTasklet implements Tasklet {

    private final PipelineExitScheduler pipelineExitScheduler;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        pipelineExitScheduler.evaluateAndExecuteExits();
        return RepeatStatus.FINISHED;
    }
}
