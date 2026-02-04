package com.investment.batch.tasklet;

import com.investment.datacollection.service.UsMarketCollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class UsDailyCollectTasklet implements Tasklet {

    private final UsMarketCollectionService usMarketCollectionService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        LocalDate basDt = LocalDate.now();
        var params = chunkContext.getStepContext().getJobParameters();
        if (params != null && params.containsKey("basDt")) {
            String v = params.get("basDt") != null ? String.valueOf(params.get("basDt")) : null;
            if (v != null && !v.isBlank()) {
                basDt = LocalDate.parse(v);
            }
        }
        usMarketCollectionService.collectAndSave(basDt);
        return RepeatStatus.FINISHED;
    }
}
