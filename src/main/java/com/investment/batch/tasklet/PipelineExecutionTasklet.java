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
        Boolean dryRun = null;
        var params = chunkContext.getStepContext().getJobParameters();
        if (params != null && params.containsKey("dryRun")) {
            String v = params.get("dryRun") != null ? String.valueOf(params.get("dryRun")) : null;
            if (v != null)
                dryRun = Boolean.parseBoolean(v);
        }
        pipelineExecutionScheduler.runNow(dryRun);
        return RepeatStatus.FINISHED;
    }
}
