package com.investment.batch.tasklet;

import com.investment.tradingportfolio.service.TradingPortfolioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradingPortfolioTasklet implements Tasklet {

    private final TradingPortfolioService tradingPortfolioService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        LocalDate targetDate = LocalDate.now();
        var params = chunkContext.getStepContext().getJobParameters();
        if (params != null && params.containsKey("date")) {
            String v = params.get("date") != null ? String.valueOf(params.get("date")) : null;
            if (v != null && !v.isBlank()) {
                targetDate = LocalDate.parse(v);
            }
        }
        tradingPortfolioService.generateDailyPortfolio(targetDate);
        return RepeatStatus.FINISHED;
    }
}
