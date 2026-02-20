package com.investment.risk.service;

import com.investment.config.MonteCarloProperties;
import com.investment.risk.dto.MonteCarloVarResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Monte Carlo 시뮬레이션 기반 VaR/CVaR 계산 구현체.
 * 정규분포 및 Student-t 분포(팻테일) 지원.
 * Cholesky 분해로 포트폴리오 상관관계 반영.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonteCarloVarServiceImpl implements MonteCarloVarService {

    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final MonteCarloProperties properties;

    @Override
    public MonteCarloVarResult calculateVaR(List<BigDecimal> dailyReturns, BigDecimal confidenceLevel, int scenarios) {
        long startTime = System.currentTimeMillis();

        if (dailyReturns == null || dailyReturns.size() < properties.getMinSamples()) {
            return MonteCarloVarResult.insufficientData(dailyReturns != null ? dailyReturns.size() : 0);
        }

        try {
            double[] returns = toDoubleArray(dailyReturns);
            double mean = calculateMean(returns);
            double stdDev = calculateStdDev(returns, mean);

            double[] simulatedReturns = runSimulation(mean, stdDev, scenarios);
            Arrays.sort(simulatedReturns);

            double alpha = 1.0 - confidenceLevel.doubleValue();
            int varIndex = (int) Math.ceil(simulatedReturns.length * alpha) - 1;
            varIndex = Math.max(0, varIndex);

            double varValue = -simulatedReturns[varIndex];

            long elapsedMs = System.currentTimeMillis() - startTime;
            log.debug("Monte Carlo VaR 계산 완료: {}% ({}개 시나리오, {}ms)",
                    String.format("%.4f", varValue * 100), scenarios, elapsedMs);

            return MonteCarloVarResult.builder()
                    .value(BigDecimal.valueOf(varValue * 100).setScale(4, RoundingMode.HALF_UP))
                    .confidenceLevel(confidenceLevel)
                    .scenarios(scenarios)
                    .method(getMethodName())
                    .degreesOfFreedom(properties.getDistribution() == MonteCarloProperties.Distribution.STUDENT_T
                            ? properties.getDegreesOfFreedom() : null)
                    .sampleCount(dailyReturns.size())
                    .elapsedTimeMs(elapsedMs)
                    .calculatedAt(Instant.now())
                    .meanReturn(BigDecimal.valueOf(mean * 100).setScale(6, RoundingMode.HALF_UP))
                    .stdDeviation(BigDecimal.valueOf(stdDev * 100).setScale(6, RoundingMode.HALF_UP))
                    .maxLoss(BigDecimal.valueOf(-simulatedReturns[0] * 100).setScale(4, RoundingMode.HALF_UP))
                    .minLoss(BigDecimal.valueOf(-simulatedReturns[simulatedReturns.length - 1] * 100).setScale(4, RoundingMode.HALF_UP))
                    .valid(true)
                    .build();
        } catch (Exception e) {
            log.error("Monte Carlo VaR 계산 실패", e);
            return MonteCarloVarResult.error("Calculation failed: " + e.getMessage());
        }
    }

    @Override
    public MonteCarloVarResult calculateCVaR(List<BigDecimal> dailyReturns, BigDecimal confidenceLevel, int scenarios) {
        long startTime = System.currentTimeMillis();

        if (dailyReturns == null || dailyReturns.size() < properties.getMinSamples()) {
            return MonteCarloVarResult.insufficientData(dailyReturns != null ? dailyReturns.size() : 0);
        }

        try {
            double[] returns = toDoubleArray(dailyReturns);
            double mean = calculateMean(returns);
            double stdDev = calculateStdDev(returns, mean);

            double[] simulatedReturns = runSimulation(mean, stdDev, scenarios);
            Arrays.sort(simulatedReturns);

            double alpha = 1.0 - confidenceLevel.doubleValue();
            int cutoffIndex = (int) Math.ceil(simulatedReturns.length * alpha);
            cutoffIndex = Math.max(1, cutoffIndex);

            double sum = 0.0;
            for (int i = 0; i < cutoffIndex; i++) {
                sum += -simulatedReturns[i];
            }
            double cvarValue = sum / cutoffIndex;

            long elapsedMs = System.currentTimeMillis() - startTime;
            log.debug("Monte Carlo CVaR 계산 완료: {}% ({}개 시나리오, {}ms)",
                    String.format("%.4f", cvarValue * 100), scenarios, elapsedMs);

            return MonteCarloVarResult.builder()
                    .value(BigDecimal.valueOf(cvarValue * 100).setScale(4, RoundingMode.HALF_UP))
                    .confidenceLevel(confidenceLevel)
                    .scenarios(scenarios)
                    .method(getMethodName() + "_ES")
                    .degreesOfFreedom(properties.getDistribution() == MonteCarloProperties.Distribution.STUDENT_T
                            ? properties.getDegreesOfFreedom() : null)
                    .sampleCount(dailyReturns.size())
                    .elapsedTimeMs(elapsedMs)
                    .calculatedAt(Instant.now())
                    .meanReturn(BigDecimal.valueOf(mean * 100).setScale(6, RoundingMode.HALF_UP))
                    .stdDeviation(BigDecimal.valueOf(stdDev * 100).setScale(6, RoundingMode.HALF_UP))
                    .maxLoss(BigDecimal.valueOf(-simulatedReturns[0] * 100).setScale(4, RoundingMode.HALF_UP))
                    .minLoss(BigDecimal.valueOf(-simulatedReturns[simulatedReturns.length - 1] * 100).setScale(4, RoundingMode.HALF_UP))
                    .valid(true)
                    .build();
        } catch (Exception e) {
            log.error("Monte Carlo CVaR 계산 실패", e);
            return MonteCarloVarResult.error("Calculation failed: " + e.getMessage());
        }
    }

    @Override
    public MonteCarloVarResult calculatePortfolioVaR(Map<String, List<BigDecimal>> symbolReturns,
                                                      Map<String, BigDecimal> weights,
                                                      BigDecimal confidenceLevel,
                                                      int scenarios) {
        long startTime = System.currentTimeMillis();

        if (symbolReturns == null || symbolReturns.isEmpty()) {
            return MonteCarloVarResult.error("No symbol returns provided");
        }

        try {
            List<String> symbols = new ArrayList<>(symbolReturns.keySet());
            int n = symbols.size();

            int minLength = symbolReturns.values().stream()
                    .mapToInt(List::size)
                    .min()
                    .orElse(0);

            if (minLength < properties.getMinSamples()) {
                return MonteCarloVarResult.insufficientData(minLength);
            }

            double[][] returnsMatrix = new double[n][minLength];
            double[] weightArray = new double[n];

            for (int i = 0; i < n; i++) {
                String symbol = symbols.get(i);
                List<BigDecimal> returns = symbolReturns.get(symbol);
                weightArray[i] = weights.getOrDefault(symbol, BigDecimal.ZERO).doubleValue();

                for (int j = 0; j < minLength; j++) {
                    returnsMatrix[i][j] = returns.get(j).doubleValue();
                }
            }

            double[] means = new double[n];
            double[] stdDevs = new double[n];
            for (int i = 0; i < n; i++) {
                means[i] = calculateMean(returnsMatrix[i]);
                stdDevs[i] = calculateStdDev(returnsMatrix[i], means[i]);
            }

            double[][] correlationMatrix = calculateCorrelationMatrix(returnsMatrix);
            double[][] choleskyMatrix = choleskyDecomposition(correlationMatrix);

            double[] portfolioReturns = runPortfolioSimulation(
                    means, stdDevs, weightArray, choleskyMatrix, scenarios);

            Arrays.sort(portfolioReturns);

            double alpha = 1.0 - confidenceLevel.doubleValue();
            int varIndex = (int) Math.ceil(portfolioReturns.length * alpha) - 1;
            varIndex = Math.max(0, varIndex);

            double varValue = -portfolioReturns[varIndex];

            long elapsedMs = System.currentTimeMillis() - startTime;
            log.debug("Portfolio Monte Carlo VaR 계산 완료: {}% ({} 종목, {}개 시나리오, {}ms)",
                    String.format("%.4f", varValue * 100), n, scenarios, elapsedMs);

            double portfolioMean = 0.0;
            double portfolioStdDev = 0.0;
            for (int i = 0; i < n; i++) {
                portfolioMean += weightArray[i] * means[i];
            }
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    portfolioStdDev += weightArray[i] * weightArray[j] * stdDevs[i] * stdDevs[j] * correlationMatrix[i][j];
                }
            }
            portfolioStdDev = Math.sqrt(portfolioStdDev);

            return MonteCarloVarResult.builder()
                    .value(BigDecimal.valueOf(varValue * 100).setScale(4, RoundingMode.HALF_UP))
                    .confidenceLevel(confidenceLevel)
                    .scenarios(scenarios)
                    .method(getMethodName() + "_PORTFOLIO")
                    .degreesOfFreedom(properties.getDistribution() == MonteCarloProperties.Distribution.STUDENT_T
                            ? properties.getDegreesOfFreedom() : null)
                    .sampleCount(minLength)
                    .elapsedTimeMs(elapsedMs)
                    .calculatedAt(Instant.now())
                    .meanReturn(BigDecimal.valueOf(portfolioMean * 100).setScale(6, RoundingMode.HALF_UP))
                    .stdDeviation(BigDecimal.valueOf(portfolioStdDev * 100).setScale(6, RoundingMode.HALF_UP))
                    .maxLoss(BigDecimal.valueOf(-portfolioReturns[0] * 100).setScale(4, RoundingMode.HALF_UP))
                    .minLoss(BigDecimal.valueOf(-portfolioReturns[portfolioReturns.length - 1] * 100).setScale(4, RoundingMode.HALF_UP))
                    .valid(true)
                    .build();
        } catch (Exception e) {
            log.error("Portfolio Monte Carlo VaR 계산 실패", e);
            return MonteCarloVarResult.error("Portfolio calculation failed: " + e.getMessage());
        }
    }

    @Override
    @Async
    public CompletableFuture<MonteCarloVarResult> calculateVaRAsync(List<BigDecimal> dailyReturns,
                                                                     BigDecimal confidenceLevel,
                                                                     int scenarios) {
        return CompletableFuture.completedFuture(calculateVaR(dailyReturns, confidenceLevel, scenarios));
    }

    @Override
    @Async
    public CompletableFuture<MonteCarloVarResult> calculateCVaRAsync(List<BigDecimal> dailyReturns,
                                                                      BigDecimal confidenceLevel,
                                                                      int scenarios) {
        return CompletableFuture.completedFuture(calculateCVaR(dailyReturns, confidenceLevel, scenarios));
    }

    private double[] runSimulation(double mean, double stdDev, int scenarios) {
        Random random = properties.isFixedSeed()
                ? new Random(properties.getSeedValue())
                : new Random();

        double[] results = new double[scenarios];
        int threads = properties.getParallelThreads();

        if (scenarios > 1000 && threads > 1) {
            ExecutorService executor = Executors.newFixedThreadPool(threads);
            int chunkSize = scenarios / threads;

            List<CompletableFuture<double[]>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                int start = t * chunkSize;
                int end = (t == threads - 1) ? scenarios : start + chunkSize;
                long seed = properties.isFixedSeed() ? properties.getSeedValue() + t : System.nanoTime() + t;

                futures.add(CompletableFuture.supplyAsync(() ->
                        simulateChunk(mean, stdDev, start, end, new Random(seed)), executor));
            }

            int idx = 0;
            for (CompletableFuture<double[]> future : futures) {
                double[] chunk = future.join();
                System.arraycopy(chunk, 0, results, idx, chunk.length);
                idx += chunk.length;
            }
            executor.shutdown();
        } else {
            for (int i = 0; i < scenarios; i++) {
                results[i] = generateReturn(random, mean, stdDev);
            }
        }

        return results;
    }

    private double[] simulateChunk(double mean, double stdDev, int start, int end, Random random) {
        int size = end - start;
        double[] chunk = new double[size];
        for (int i = 0; i < size; i++) {
            chunk[i] = generateReturn(random, mean, stdDev);
        }
        return chunk;
    }

    private double generateReturn(Random random, double mean, double stdDev) {
        if (properties.getDistribution() == MonteCarloProperties.Distribution.STUDENT_T) {
            return generateStudentT(random, mean, stdDev, properties.getDegreesOfFreedom());
        }
        return random.nextGaussian() * stdDev + mean;
    }

    private double generateStudentT(Random random, double mean, double stdDev, int df) {
        double normal = random.nextGaussian();
        double chi2 = 0.0;
        for (int i = 0; i < df; i++) {
            double g = random.nextGaussian();
            chi2 += g * g;
        }
        double t = normal / Math.sqrt(chi2 / df);
        double scaleFactor = Math.sqrt((double) (df - 2) / df);
        return mean + stdDev * t * scaleFactor;
    }

    private double[] runPortfolioSimulation(double[] means, double[] stdDevs, double[] weights,
                                            double[][] choleskyMatrix, int scenarios) {
        Random random = properties.isFixedSeed()
                ? new Random(properties.getSeedValue())
                : new Random();

        int n = means.length;
        double[] results = new double[scenarios];

        for (int s = 0; s < scenarios; s++) {
            double[] z = new double[n];
            for (int i = 0; i < n; i++) {
                z[i] = generateReturn(random, 0, 1);
            }

            double[] correlatedZ = new double[n];
            for (int i = 0; i < n; i++) {
                correlatedZ[i] = 0.0;
                for (int j = 0; j <= i; j++) {
                    correlatedZ[i] += choleskyMatrix[i][j] * z[j];
                }
            }

            double portfolioReturn = 0.0;
            for (int i = 0; i < n; i++) {
                double assetReturn = means[i] + stdDevs[i] * correlatedZ[i];
                portfolioReturn += weights[i] * assetReturn;
            }
            results[s] = portfolioReturn;
        }

        return results;
    }

    private double[][] calculateCorrelationMatrix(double[][] returnsMatrix) {
        int n = returnsMatrix.length;
        double[][] corr = new double[n][n];

        double[] means = new double[n];
        double[] stdDevs = new double[n];
        for (int i = 0; i < n; i++) {
            means[i] = calculateMean(returnsMatrix[i]);
            stdDevs[i] = calculateStdDev(returnsMatrix[i], means[i]);
        }

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    corr[i][j] = 1.0;
                } else if (j > i) {
                    double cov = 0.0;
                    int m = returnsMatrix[i].length;
                    for (int k = 0; k < m; k++) {
                        cov += (returnsMatrix[i][k] - means[i]) * (returnsMatrix[j][k] - means[j]);
                    }
                    cov /= (m - 1);
                    corr[i][j] = cov / (stdDevs[i] * stdDevs[j]);
                    corr[j][i] = corr[i][j];
                }
            }
        }

        return corr;
    }

    private double[][] choleskyDecomposition(double[][] matrix) {
        int n = matrix.length;
        double[][] L = new double[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j <= i; j++) {
                double sum = 0.0;
                for (int k = 0; k < j; k++) {
                    sum += L[i][k] * L[j][k];
                }
                if (i == j) {
                    double val = matrix[i][i] - sum;
                    L[i][j] = val > 0 ? Math.sqrt(val) : 0.0;
                } else {
                    L[i][j] = L[j][j] > 0 ? (matrix[i][j] - sum) / L[j][j] : 0.0;
                }
            }
        }

        return L;
    }

    private double[] toDoubleArray(List<BigDecimal> list) {
        return list.stream()
                .mapToDouble(BigDecimal::doubleValue)
                .toArray();
    }

    private double calculateMean(double[] values) {
        double sum = 0.0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.length;
    }

    private double calculateStdDev(double[] values, double mean) {
        double sumSquares = 0.0;
        for (double v : values) {
            double diff = v - mean;
            sumSquares += diff * diff;
        }
        return Math.sqrt(sumSquares / (values.length - 1));
    }

    private String getMethodName() {
        return "MONTE_CARLO_" + properties.getDistribution().name();
    }
}
