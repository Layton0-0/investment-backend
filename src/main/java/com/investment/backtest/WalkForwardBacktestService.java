package com.investment.backtest;

import com.investment.backtest.dto.BacktestRunRequest;
import com.investment.backtest.dto.BacktestRunResult;
import com.investment.backtest.dto.WalkForwardBacktestRequest;
import com.investment.backtest.dto.WalkForwardBacktestResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Walk-Forward(롤링 Out-of-Sample) 백테스트.
 * <p>구간을 train/test 윈도우로 나누어 각 test 구간만 {@link BacktestService}로 실행한 뒤
 * fold별 메트릭을 집계한다. 전략 파라미터는 재추정하지 않고 기존 설정을 사용한다.
 * 오버피팅 완화·일반화 성능 추정용.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalkForwardBacktestService {

    private final BacktestService backtestService;

    /**
     * Walk-Forward 백테스트 실행.
     * <p>첫 test 구간: [startDate + trainDays, startDate + trainDays + testDays - 1].
     * 이후 stepDays만큼 이동하며 test 구간을 반복 실행. testEnd가 endDate를 초과할 때까지.
     *
     * @param request startDate, endDate, market, strategyType, initialCapital, trainDays, testDays, stepDays
     * @return fold별 결과·집계 메트릭
     */
    public WalkForwardBacktestResult run(WalkForwardBacktestRequest request) {
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("startDate must be <= endDate");
        }
        int trainDays = Math.max(1, request.getTrainDays());
        int testDays = Math.max(1, request.getTestDays());
        int stepDays = Math.max(1, request.getStepDays());

        LocalDate testStart = request.getStartDate().plusDays(trainDays);
        LocalDate testEnd = testStart.plusDays(testDays - 1);
        if (testEnd.isAfter(request.getEndDate())) {
            throw new IllegalArgumentException(
                    "At least one test window required: startDate + trainDays + testDays must be <= endDate");
        }

        List<BacktestRunResult> folds = new ArrayList<>();
        while (!testStart.isAfter(request.getEndDate())) {
            testEnd = testStart.plusDays(testDays - 1);
            if (testEnd.isAfter(request.getEndDate())) {
                break;
            }
            BacktestRunRequest runRequest = BacktestRunRequest.builder()
                    .startDate(testStart)
                    .endDate(testEnd)
                    .market(request.getMarket())
                    .strategyType(request.getStrategyType())
                    .initialCapital(request.getInitialCapital())
                    .build();
            BacktestRunResult result = backtestService.run(runRequest);
            folds.add(result);
            testStart = testStart.plusDays(stepDays);
        }

        if (folds.isEmpty()) {
            return WalkForwardBacktestResult.builder()
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .market(request.getMarket())
                    .strategyType(request.getStrategyType())
                    .trainDays(trainDays)
                    .testDays(testDays)
                    .stepDays(stepDays)
                    .foldCount(0)
                    .folds(List.of())
                    .build();
        }

        BigDecimal sumCagr = BigDecimal.ZERO;
        BigDecimal sumMdd = BigDecimal.ZERO;
        BigDecimal minSharpe = null;
        BigDecimal sumSharpe = BigDecimal.ZERO;
        int sharpeCount = 0;
        BigDecimal sumWinRate = BigDecimal.ZERO;
        int winRateCount = 0;
        BigDecimal sumPf = BigDecimal.ZERO;
        int pfCount = 0;

        for (BacktestRunResult r : folds) {
            if (r.getCagr() != null) {
                sumCagr = sumCagr.add(r.getCagr());
            }
            if (r.getMddPct() != null) {
                sumMdd = sumMdd.add(r.getMddPct());
            }
            if (r.getSharpeRatio() != null) {
                if (minSharpe == null || r.getSharpeRatio().compareTo(minSharpe) < 0) {
                    minSharpe = r.getSharpeRatio();
                }
                sumSharpe = sumSharpe.add(r.getSharpeRatio());
                sharpeCount++;
            }
            if (r.getWinRate() != null) {
                sumWinRate = sumWinRate.add(r.getWinRate());
                winRateCount++;
            }
            if (r.getProfitFactor() != null) {
                sumPf = sumPf.add(r.getProfitFactor());
                pfCount++;
            }
        }

        int n = folds.size();
        BigDecimal avgCagr = n > 0 ? sumCagr.divide(BigDecimal.valueOf(n), 4, RoundingMode.HALF_UP) : null;
        BigDecimal avgMddPct = n > 0 ? sumMdd.divide(BigDecimal.valueOf(n), 4, RoundingMode.HALF_UP) : null;
        BigDecimal avgSharpeRatio = sharpeCount > 0
                ? sumSharpe.divide(BigDecimal.valueOf(sharpeCount), 4, RoundingMode.HALF_UP) : null;
        BigDecimal avgWinRate = winRateCount > 0
                ? sumWinRate.divide(BigDecimal.valueOf(winRateCount), 4, RoundingMode.HALF_UP) : null;
        BigDecimal avgProfitFactor = pfCount > 0
                ? sumPf.divide(BigDecimal.valueOf(pfCount), 4, RoundingMode.HALF_UP) : null;

        return WalkForwardBacktestResult.builder()
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .market(request.getMarket())
                .strategyType(request.getStrategyType())
                .trainDays(trainDays)
                .testDays(testDays)
                .stepDays(stepDays)
                .foldCount(folds.size())
                .folds(folds)
                .avgCagr(avgCagr)
                .avgMddPct(avgMddPct)
                .minSharpeRatio(minSharpe)
                .avgSharpeRatio(avgSharpeRatio)
                .avgWinRate(avgWinRate)
                .avgProfitFactor(avgProfitFactor)
                .build();
    }
}
