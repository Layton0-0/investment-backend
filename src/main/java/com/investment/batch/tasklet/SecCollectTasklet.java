package com.investment.batch.tasklet;

import com.investment.datacollection.service.SecCollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecCollectTasklet implements Tasklet {

    private final SecCollectionService secCollectionService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        secCollectionService.collectAndSave();
        return RepeatStatus.FINISHED;
    }
}
