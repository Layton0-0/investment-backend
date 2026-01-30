package com.investment.factor.service;

import com.investment.domain.entity.DailyStock;
import com.investment.domain.entity.SignalScore;
import com.investment.domain.repository.DailyStockRepository;
import com.investment.domain.repository.SignalScoreRepository;
import com.investment.factor.dto.PositionRecommendationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 3단계 자금 관리 — 포지션 사이징.
 * ATR 기반 1회 리스크 비율·변동성 역가중 적용.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PositionSizingService {

    private static final int ATR_DAYS = 14;
    private static final BigDecimal ATR_MULTIPLIER = new BigDecimal("2.0");
    private static final String METHOD_ATR = "ATR";

    private final SignalScoreRepository signalScoreRepository;
    private final DailyStockRepository dailyStockRepository;

    /** 1회 매매당 총자산 대비 리스크 비율 (예: 0.01 = 1%) */
    @Value("${investment.factor.position-risk-pct:0.01}")
    private BigDecimal positionRiskPct = new BigDecimal("0.01");

    /**
     * 기준일·시장에 대한 포지션 권장 목록 산출.
     *
     * @param basDt        기준일
     * @param market       시장 (KR, US)
     * @param totalCapital 총 투자 가능 자산 (원)
     * @return 권장 포지션 목록
     */
    public List<PositionRecommendationDto> getRecommendations(LocalDate basDt, String market, BigDecimal totalCapital) {
        if (totalCapital == null || totalCapital.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }
        List<SignalScore> signals = signalScoreRepository.findByBasDtAndMarketOrderBySymbol(
                basDt, market, PageRequest.of(0, 5000));
        if (signals.isEmpty()) {
            return List.of();
        }
        List<String> symbols = signals.stream()
                .map(SignalScore::getSymbol)
                .distinct()
                .collect(Collectors.toList());

        LocalDate fromDt = basDt.minusDays(ATR_DAYS + 5);
        List<PositionRecommendationDto> out = new ArrayList<>();
        for (String symbol : symbols) {
            List<DailyStock> history = dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                    symbol, market, fromDt, basDt);
            if (history.isEmpty()) {
                continue;
            }
            DailyStock latest = history.get(history.size() - 1);
            if (latest.getClosePrice() == null) {
                continue;
            }
            BigDecimal entry = latest.getClosePrice();
            BigDecimal atr = computeAtr(history);
            BigDecimal stopLoss = entry.subtract(atr.multiply(ATR_MULTIPLIER));
            if (stopLoss.compareTo(BigDecimal.ZERO) <= 0 || entry.compareTo(stopLoss) <= 0) {
                continue;
            }
            BigDecimal riskPerShare = entry.subtract(stopLoss);
            BigDecimal riskAmount = totalCapital.multiply(positionRiskPct);
            long qty = riskAmount.divide(riskPerShare, 0, RoundingMode.DOWN).longValue();
            if (qty <= 0) {
                continue;
            }
            BigDecimal amt = entry.multiply(BigDecimal.valueOf(qty));
            out.add(PositionRecommendationDto.builder()
                    .basDt(basDt)
                    .symbol(symbol)
                    .market(market)
                    .recommendedAmt(amt)
                    .recommendedQty(qty)
                    .method(METHOD_ATR)
                    .entryPrice(entry)
                    .stopLoss(stopLoss)
                    .build());
        }
        if (!out.isEmpty()) {
            out = applyInverseVolatilityWeighting(out, totalCapital, fromDt, market);
        }
        return out;
    }

    private BigDecimal computeAtr(List<DailyStock> history) {
        if (history.size() < 2 || history.size() < ATR_DAYS + 1) {
            return BigDecimal.ZERO;
        }
        List<DailyStock> sorted = history.stream()
                .sorted((a, b) -> a.getBasDt().compareTo(b.getBasDt()))
                .collect(Collectors.toList());
        BigDecimal sum = BigDecimal.ZERO;
        int start = Math.max(1, sorted.size() - ATR_DAYS);
        for (int i = start; i < sorted.size(); i++) {
            DailyStock curr = sorted.get(i);
            DailyStock prev = sorted.get(i - 1);
            BigDecimal high = curr.getHighPrice() != null ? curr.getHighPrice() : curr.getClosePrice();
            BigDecimal low = curr.getLowPrice() != null ? curr.getLowPrice() : curr.getClosePrice();
            BigDecimal prevClose = prev.getClosePrice();
            if (high == null || low == null || prevClose == null) {
                continue;
            }
            BigDecimal tr = high.subtract(low);
            BigDecimal tr2 = high.subtract(prevClose).abs();
            BigDecimal tr3 = low.subtract(prevClose).abs();
            if (tr2.compareTo(tr) > 0) tr = tr2;
            if (tr3.compareTo(tr) > 0) tr = tr3;
            sum = sum.add(tr);
        }
        int count = sorted.size() - start;
        return count > 0 ? sum.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    private List<PositionRecommendationDto> applyInverseVolatilityWeighting(
            List<PositionRecommendationDto> recommendations, BigDecimal totalCapital,
            LocalDate fromDt, String market) {
        if (recommendations.size() <= 1) {
            return recommendations;
        }
        Map<String, BigDecimal> sigmaMap = new java.util.HashMap<>();
        for (PositionRecommendationDto dto : recommendations) {
            List<DailyStock> history = dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                    dto.getSymbol(), market, fromDt, dto.getBasDt());
            BigDecimal sigma = computeReturnStdDev(history);
            sigmaMap.put(dto.getSymbol(), sigma.compareTo(BigDecimal.ZERO) > 0 ? sigma : BigDecimal.ONE);
        }
        BigDecimal weightSum = sigmaMap.values().stream()
                .map(sigma -> BigDecimal.ONE.divide(sigma, 6, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (weightSum.compareTo(BigDecimal.ZERO) == 0) {
            return recommendations;
        }
        BigDecimal maxAllocation = totalCapital.multiply(new BigDecimal("0.2"));
        return recommendations.stream()
                .map(dto -> {
                    BigDecimal sigma = sigmaMap.getOrDefault(dto.getSymbol(), BigDecimal.ONE);
                    BigDecimal weight = BigDecimal.ONE.divide(sigma, 6, RoundingMode.HALF_UP).divide(weightSum, 6, RoundingMode.HALF_UP);
                    BigDecimal cappedAmt = totalCapital.multiply(weight).min(maxAllocation);
                    if (dto.getEntryPrice().compareTo(BigDecimal.ZERO) <= 0) return dto;
                    long qty = cappedAmt.divide(dto.getEntryPrice(), 0, RoundingMode.DOWN).longValue();
                    return PositionRecommendationDto.builder()
                            .basDt(dto.getBasDt())
                            .symbol(dto.getSymbol())
                            .market(dto.getMarket())
                            .recommendedAmt(dto.getEntryPrice().multiply(BigDecimal.valueOf(qty)))
                            .recommendedQty(qty)
                            .method(dto.getMethod())
                            .entryPrice(dto.getEntryPrice())
                            .stopLoss(dto.getStopLoss())
                            .build();
                })
                .filter(dto -> dto.getRecommendedQty() > 0)
                .collect(Collectors.toList());
    }

    private BigDecimal computeReturnStdDev(List<DailyStock> history) {
        if (history.size() < 2) return BigDecimal.ZERO;
        List<DailyStock> sorted = history.stream()
                .sorted((a, b) -> a.getBasDt().compareTo(b.getBasDt()))
                .collect(Collectors.toList());
        List<BigDecimal> returns = new ArrayList<>();
        for (int i = 1; i < sorted.size(); i++) {
            BigDecimal prev = sorted.get(i - 1).getClosePrice();
            BigDecimal curr = sorted.get(i).getClosePrice();
            if (prev == null || curr == null || prev.compareTo(BigDecimal.ZERO) == 0) continue;
            returns.add(curr.subtract(prev).divide(prev, 6, RoundingMode.HALF_UP));
        }
        if (returns.isEmpty()) return BigDecimal.ZERO;
        BigDecimal mean = returns.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(returns.size()), 6, RoundingMode.HALF_UP);
        BigDecimal variance = returns.stream()
                .map(r -> r.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 6, RoundingMode.HALF_UP);
        double std = Math.sqrt(variance.doubleValue());
        return BigDecimal.valueOf(std);
    }
}
