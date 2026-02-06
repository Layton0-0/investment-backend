package com.investment.batch.tasklet;

import com.investment.factor.scheduler.AutoBuyOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AutoBuyTasklet implements Tasklet {

    private final AutoBuyOrchestrator autoBuyOrchestrator;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        Boolean dryRun = null;
        var params = chunkContext.getStepContext().getJobParameters();
        if (params != null && params.containsKey("dryRun")) {
            Object v = params.get("dryRun");
            if (v != null)
                dryRun = Boolean.parseBoolean(String.valueOf(v));
        }
        autoBuyOrchestrator.run(dryRun);
        return RepeatStatus.FINISHED;
    }
}
