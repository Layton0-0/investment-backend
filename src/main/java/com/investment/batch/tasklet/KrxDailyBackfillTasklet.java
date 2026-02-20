package com.investment.batch.tasklet;

import com.investment.datacollection.service.KrxCollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * KRX 일별 시세 기간 백필. Job 파라미터 fromDate, toDate (yyyy-MM-dd)로 구간 수집.
 */
@Component
@RequiredArgsConstructor
public class KrxDailyBackfillTasklet implements Tasklet {

    private final KrxCollectionService krxCollectionService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        var params = chunkContext.getStepContext().getJobParameters();
        String fromStr = params != null && params.get("fromDate") != null
                ? String.valueOf(params.get("fromDate")) : null;
        String toStr = params != null && params.get("toDate") != null
                ? String.valueOf(params.get("toDate")) : null;
        if (fromStr == null || fromStr.isBlank() || toStr == null || toStr.isBlank()) {
            throw new IllegalStateException("KRX 백필에는 Job 파라미터 fromDate, toDate (yyyy-MM-dd)가 필요합니다.");
        }
        LocalDate from = LocalDate.parse(fromStr);
        LocalDate to = LocalDate.parse(toStr);
        krxCollectionService.collectAndSaveRange(from, to);
        return RepeatStatus.FINISHED;
    }
}
