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
        boolean dryRun = false;
        var params = chunkContext.getStepContext().getJobParameters();
        if (params != null && params.containsKey("dryRun")) {
            String v = params.get("dryRun") != null ? String.valueOf(params.get("dryRun")) : null;
            if (v != null)
                dryRun = Boolean.parseBoolean(v);
        }
        roboRebalanceScheduler.runNow(dryRun);
        return RepeatStatus.FINISHED;
    }
}
