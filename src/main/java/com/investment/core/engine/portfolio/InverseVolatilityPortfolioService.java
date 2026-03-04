package com.investment.core.engine.portfolio;

import com.investment.domain.entity.DailyStock;
import com.investment.domain.repository.DailyStockRepository;
import com.investment.factor.dto.PositionRecommendationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 역변동성 가중(Inverse Volatility Weighting) 포트폴리오 구성.
 * TB_DAILY_STOCK 20일 수익률 표준편차 기반 weight_i = (1/sigma_i) / sum(1/sigma_j).
 * 전략별(SHORT 20%, MEDIUM 40%, LONG 40%) 배분은 호출부(PositionSizingService 등)에서 적용.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InverseVolatilityPortfolioService {

    private static final int VOLATILITY_LOOKBACK_DAYS = 20;
    /** 변동성 0 또는 미계산 시 사용할 최소 분모(역가중 방지). */
    private static final BigDecimal MIN_SIGMA = new BigDecimal("0.0001");

    private final DailyStockRepository dailyStockRepository;

    /** 종목당 최대 할당 비율 (캡). 0.2 = 20% */
    @Value("${investment.portfolio.inverse-volatility.max-allocation-pct:0.2}")
    private BigDecimal maxAllocationPct = new BigDecimal("0.2");

    /**
     * 권장 포지션 목록에 역변동성 가중을 적용하여 금액·수량을 재계산.
     * 20일 수익률 표준편차로 sigma 산출 후 weight_i = (1/sigma_i) / sum(1/sigma_j).
     *
     * @param recommendations ATR 등으로 산출된 권장 목록 (동일 전략 내)
     * @param totalCapital    총 투자 가능 자산 (또는 전략별 예산)
     * @param basDt           기준일
     * @param market          시장 (KR, US)
     * @return 역변동성 가중 적용된 권장 목록
     */
    public List<PositionRecommendationDto> applyInverseVolatilityWeights(
            List<PositionRecommendationDto> recommendations,
            BigDecimal totalCapital,
            LocalDate basDt,
            String market) {
        if (recommendations == null || recommendations.isEmpty() || recommendations.size() <= 1) {
            return recommendations != null ? recommendations : List.of();
        }
        if (totalCapital == null || totalCapital.compareTo(BigDecimal.ZERO) <= 0) {
            return recommendations;
        }

        LocalDate fromDt = basDt.minusDays(VOLATILITY_LOOKBACK_DAYS + 5);
        Map<String, BigDecimal> sigmaBySymbol = new ConcurrentHashMap<>();
        for (PositionRecommendationDto dto : recommendations) {
            List<DailyStock> history = dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                    dto.getSymbol(), market, fromDt, basDt);
            BigDecimal sigma = computeReturnStdDev20(history);
            sigmaBySymbol.put(dto.getSymbol(), sigma.compareTo(BigDecimal.ZERO) > 0 ? sigma : MIN_SIGMA);
        }

        BigDecimal invSum = sigmaBySymbol.values().stream()
                .map(s -> BigDecimal.ONE.divide(s, 10, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (invSum.compareTo(BigDecimal.ZERO) == 0) {
            return recommendations;
        }

        BigDecimal maxAllocation = totalCapital.multiply(maxAllocationPct).setScale(0, RoundingMode.DOWN);
        List<PositionRecommendationDto> result = new ArrayList<>();
        for (PositionRecommendationDto dto : recommendations) {
            BigDecimal sigma = sigmaBySymbol.getOrDefault(dto.getSymbol(), MIN_SIGMA);
            BigDecimal weight = BigDecimal.ONE.divide(sigma, 10, RoundingMode.HALF_UP).divide(invSum, 10, RoundingMode.HALF_UP);
            BigDecimal targetAmt = totalCapital.multiply(weight).setScale(0, RoundingMode.DOWN).min(maxAllocation);
            if (dto.getEntryPrice() == null || dto.getEntryPrice().compareTo(BigDecimal.ZERO) <= 0) {
                result.add(dto);
                continue;
            }
            long qty = targetAmt.divide(dto.getEntryPrice(), 0, RoundingMode.DOWN).longValue();
            if (qty <= 0) {
                continue;
            }
            result.add(PositionRecommendationDto.builder()
                    .basDt(dto.getBasDt())
                    .symbol(dto.getSymbol())
                    .market(dto.getMarket())
                    .recommendedAmt(dto.getEntryPrice().multiply(BigDecimal.valueOf(qty)))
                    .recommendedQty(qty)
                    .method(dto.getMethod() != null ? dto.getMethod() + "+INV_VOL" : "INV_VOL")
                    .entryPrice(dto.getEntryPrice())
                    .stopLoss(dto.getStopLoss())
                    .build());
        }
        return result.stream().filter(d -> d.getRecommendedQty() > 0).toList();
    }

    /**
     * 20일 수익률 표준편차 계산 (최근 20일 사용).
     */
    BigDecimal computeReturnStdDev20(List<DailyStock> history) {
        if (history == null || history.size() < 2) {
            return BigDecimal.ZERO;
        }
        List<DailyStock> sorted = history.stream()
                .sorted((a, b) -> a.getBasDt().compareTo(b.getBasDt()))
                .toList();
        int start = Math.max(0, sorted.size() - VOLATILITY_LOOKBACK_DAYS - 1);
        List<BigDecimal> returns = new ArrayList<>();
        for (int i = start + 1; i < sorted.size(); i++) {
            BigDecimal prev = sorted.get(i - 1).getClosePrice();
            BigDecimal curr = sorted.get(i).getClosePrice();
            if (prev == null || curr == null || prev.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            returns.add(curr.subtract(prev).divide(prev, 10, RoundingMode.HALF_UP));
        }
        if (returns.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal mean = returns.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 10, RoundingMode.HALF_UP);
        BigDecimal variance = returns.stream()
                .map(r -> r.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 10, RoundingMode.HALF_UP);
        double std = Math.sqrt(Math.max(0, variance.doubleValue()));
        return BigDecimal.valueOf(std);
    }
}
