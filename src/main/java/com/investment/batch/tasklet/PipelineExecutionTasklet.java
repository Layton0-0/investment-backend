package com.investment.batch.tasklet;

import com.investment.factor.scheduler.PipelineExecutionScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PipelineExecutionTasklet implements Tasklet {

    private final PipelineExecutionScheduler pipelineExecutionScheduler;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        pipelineExecutionScheduler.runNow();
        return RepeatStatus.FINISHED;
    }
}
