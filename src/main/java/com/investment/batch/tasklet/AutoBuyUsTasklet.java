package com.investment.batch.tasklet;

import com.investment.factor.scheduler.PipelineExecutionScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/**
 * 미국장 유리 시간대 전용 파이프라인 실행 Tasklet.
 * 파이프라인 US 시장만 실행하며, TradingWindowService에 의해 US 윈도우 밖이면 run 스킵.
 */
@Component
@RequiredArgsConstructor
public class AutoBuyUsTasklet implements Tasklet {

    private final PipelineExecutionScheduler pipelineExecutionScheduler;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        pipelineExecutionScheduler.runNow("US");
        return RepeatStatus.FINISHED;
    }
}
