package com.investment.risk.service;

import com.investment.risk.dto.MonteCarloVarResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Monte Carlo 시뮬레이션 기반 VaR/CVaR 계산 서비스.
 * 10,000+ 시나리오 시뮬레이션으로 꼬리 위험(tail risk)을 정밀하게 측정합니다.
 */
public interface MonteCarloVarService {

    /**
     * Monte Carlo VaR 계산 (동기).
     *
     * @param dailyReturns    포트폴리오 또는 종목별 일별 수익률 목록
     * @param confidenceLevel 신뢰수준 (예: 0.95 = 95%)
     * @param scenarios       시뮬레이션 시나리오 수 (기본 10,000)
     * @return VaR 결과 (%)
     */
    MonteCarloVarResult calculateVaR(List<BigDecimal> dailyReturns, BigDecimal confidenceLevel, int scenarios);

    /**
     * Monte Carlo CVaR (Expected Shortfall) 계산 (동기).
     *
     * @param dailyReturns    일별 수익률 목록
     * @param confidenceLevel 신뢰수준
     * @param scenarios       시뮬레이션 시나리오 수
     * @return CVaR 결과 (%)
     */
    MonteCarloVarResult calculateCVaR(List<BigDecimal> dailyReturns, BigDecimal confidenceLevel, int scenarios);

    /**
     * 포트폴리오 레벨 Monte Carlo VaR (상관관계 반영).
     *
     * @param symbolReturns   종목별 일별 수익률 맵 (symbol -> returns)
     * @param weights         종목별 비중 맵 (symbol -> weight)
     * @param confidenceLevel 신뢰수준
     * @param scenarios       시뮬레이션 시나리오 수
     * @return 포트폴리오 VaR 결과
     */
    MonteCarloVarResult calculatePortfolioVaR(Map<String, List<BigDecimal>> symbolReturns,
                                              Map<String, BigDecimal> weights,
                                              BigDecimal confidenceLevel,
                                              int scenarios);

    /**
     * Monte Carlo VaR 계산 (비동기).
     *
     * @param dailyReturns    일별 수익률 목록
     * @param confidenceLevel 신뢰수준
     * @param scenarios       시뮬레이션 시나리오 수
     * @return CompletableFuture<MonteCarloVarResult>
     */
    CompletableFuture<MonteCarloVarResult> calculateVaRAsync(List<BigDecimal> dailyReturns,
                                                              BigDecimal confidenceLevel,
                                                              int scenarios);

    /**
     * Monte Carlo CVaR 계산 (비동기).
     *
     * @param dailyReturns    일별 수익률 목록
     * @param confidenceLevel 신뢰수준
     * @param scenarios       시뮬레이션 시나리오 수
     * @return CompletableFuture<MonteCarloVarResult>
     */
    CompletableFuture<MonteCarloVarResult> calculateCVaRAsync(List<BigDecimal> dailyReturns,
                                                               BigDecimal confidenceLevel,
                                                               int scenarios);

    /**
     * 기본 설정으로 VaR 95% 계산.
     *
     * @param dailyReturns 일별 수익률 목록
     * @return VaR 95% 결과
     */
    default MonteCarloVarResult calculateVaR95(List<BigDecimal> dailyReturns) {
        return calculateVaR(dailyReturns, new BigDecimal("0.95"), 10000);
    }

    /**
     * 기본 설정으로 CVaR 95% 계산.
     *
     * @param dailyReturns 일별 수익률 목록
     * @return CVaR 95% 결과
     */
    default MonteCarloVarResult calculateCVaR95(List<BigDecimal> dailyReturns) {
        return calculateCVaR(dailyReturns, new BigDecimal("0.95"), 10000);
    }
}
