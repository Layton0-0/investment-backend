package com.investment.batch.tasklet;

import com.investment.datacollection.service.DartCollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DartCollectTasklet implements Tasklet {

    private final DartCollectionService dartCollectionService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        dartCollectionService.collectAndSave();
        return RepeatStatus.FINISHED;
    }
}
