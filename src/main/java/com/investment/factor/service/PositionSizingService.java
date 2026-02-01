package com.investment.factor.service;

import com.investment.domain.entity.DailyStock;
import com.investment.domain.entity.SignalScore;
import com.investment.domain.repository.DailyStockRepository;
import com.investment.domain.repository.SignalScoreRepository;
import com.investment.factor.dto.PositionRecommendationDto;
import com.investment.factor.util.TechnicalIndicatorUtil;
import com.investment.strategy.domain.StrategyType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private static final String METHOD_KELLY = "KELLY";

    private final SignalScoreRepository signalScoreRepository;
    private final DailyStockRepository dailyStockRepository;

    /** 1회 매매당 총자산 대비 리스크 비율 (예: 0.01 = 1%) */
    @Value("${investment.factor.position-risk-pct:0.01}")
    private BigDecimal positionRiskPct = new BigDecimal("0.01");

    /** Half-Kelly 승률 (기본값 0.6 = 60%). 전략별 미설정 시 사용 */
    @Value("${investment.factor.kelly-p:0.6}")
    private BigDecimal kellyP = new BigDecimal("0.6");

    /** Half-Kelly 손익비 (기본값 2.0 = 2:1). 전략별 미설정 시 사용 */
    @Value("${investment.factor.kelly-b:2.0}")
    private BigDecimal kellyB = new BigDecimal("2.0");

    /** 전략별 Half-Kelly p·b (백테스트 winRate·profitFactor 연동용). 빈 문자열이면 기본 kelly-p/kelly-b 사용 */
    @Value("${investment.factor.kelly-p-short-term:}")
    private String kellyPShortTerm = "";
    @Value("${investment.factor.kelly-b-short-term:}")
    private String kellyBShortTerm = "";
    @Value("${investment.factor.kelly-p-medium-term:}")
    private String kellyPMediumTerm = "";
    @Value("${investment.factor.kelly-b-medium-term:}")
    private String kellyBMediumTerm = "";
    @Value("${investment.factor.kelly-p-long-term:}")
    private String kellyPLongTerm = "";
    @Value("${investment.factor.kelly-b-long-term:}")
    private String kellyBLongTerm = "";

    /**
     * 기준일·시장에 대한 포지션 권장 목록 산출 (기본 SHORT_TERM).
     */
    public List<PositionRecommendationDto> getRecommendations(LocalDate basDt, String market, BigDecimal totalCapital) {
        return getRecommendations(basDt, market, StrategyType.SHORT_TERM, totalCapital);
    }

    /**
     * 기준일·시장·기간별 포지션 권장 목록 산출.
     * SHORT_TERM: RSI&gt;60 &amp; MACD&gt;Signal 필터. MEDIUM_TERM: 시그널 점수 상위 10%. LONG_TERM: 전체.
     *
     * @param basDt        기준일
     * @param market       시장 (KR, US)
     * @param strategyType 기간 (SHORT_TERM, MEDIUM_TERM, LONG_TERM)
     * @param totalCapital 총 투자 가능 자산 (원)
     * @return 권장 포지션 목록
     */
    public List<PositionRecommendationDto> getRecommendations(LocalDate basDt, String market,
            StrategyType strategyType, BigDecimal totalCapital) {
        if (totalCapital == null || totalCapital.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }
        List<SignalScore> signals = signalScoreRepository.findByBasDtAndMarketOrderBySymbol(
                basDt, market, PageRequest.of(0, 5000));
        if (signals.isEmpty()) {
            return List.of();
        }
        Set<String> symbols = filterSymbolsByStrategyType(signals, basDt, market, strategyType);
        if (symbols.isEmpty()) {
            return List.of();
        }
        List<String> symbolList = new ArrayList<>(symbols);

        LocalDate fromDt = basDt.minusDays(ATR_DAYS + 5);
        List<PositionRecommendationDto> out = new ArrayList<>();
        for (String symbol : symbolList) {
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
            // Half-Kelly 적용 (전략별 p·b 사용, 백테스트 연동 시 설정)
            out = applyHalfKelly(out, totalCapital, strategyType);
            // 변동성 역가중 적용
            out = applyInverseVolatilityWeighting(out, totalCapital, fromDt, market);
        }
        return out;
    }

    /**
     * 전략별 Half-Kelly 승률(p) 반환. 전략별 설정이 없으면 기본 kellyP.
     */
    private BigDecimal getKellyP(StrategyType strategyType) {
        String raw = resolveKellyPString(strategyType);
        if (raw == null || raw.isBlank()) {
            return kellyP;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            return kellyP;
        }
    }

    /**
     * 전략별 Half-Kelly 손익비(b) 반환. 전략별 설정이 없으면 기본 kellyB.
     */
    private BigDecimal getKellyB(StrategyType strategyType) {
        String raw = resolveKellyBString(strategyType);
        if (raw == null || raw.isBlank()) {
            return kellyB;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            return kellyB;
        }
    }

    private String resolveKellyPString(StrategyType strategyType) {
        if (strategyType == null) return null;
        return switch (strategyType) {
            case SHORT_TERM -> kellyPShortTerm;
            case MEDIUM_TERM -> kellyPMediumTerm;
            case LONG_TERM -> kellyPLongTerm;
        };
    }

    private String resolveKellyBString(StrategyType strategyType) {
        if (strategyType == null) return null;
        return switch (strategyType) {
            case SHORT_TERM -> kellyBShortTerm;
            case MEDIUM_TERM -> kellyBMediumTerm;
            case LONG_TERM -> kellyBLongTerm;
        };
    }

    /**
     * 기간별 시그널 필터: 통과한 종목 심볼만 반환.
     */
    private Set<String> filterSymbolsByStrategyType(List<SignalScore> signals, LocalDate basDt, String market,
            StrategyType strategyType) {
        Set<String> allSymbols = signals.stream().map(SignalScore::getSymbol).collect(Collectors.toSet());
        if (strategyType == null || strategyType == StrategyType.LONG_TERM) {
            return allSymbols;
        }
        if (strategyType == StrategyType.SHORT_TERM) {
            LocalDate fromDt = basDt.minusDays(30);
            return allSymbols.stream()
                    .filter(symbol -> {
                        List<DailyStock> history = dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                                symbol, market, fromDt, basDt);
                        if (history.size() < 20) {
                            return false;
                        }
                        boolean rsiOk = TechnicalIndicatorUtil.isRsiAbove(history, 14, new BigDecimal("60"));
                        boolean macdOk = TechnicalIndicatorUtil.isMacdAboveSignal(history);
                        return rsiOk && macdOk;
                    })
                    .collect(Collectors.toSet());
        }
        if (strategyType == StrategyType.MEDIUM_TERM) {
            Map<String, BigDecimal> scoreBySymbol = signals.stream()
                    .collect(Collectors.groupingBy(SignalScore::getSymbol,
                            Collectors.reducing(BigDecimal.ZERO, SignalScore::getScore,
                                    (a, b) -> a.add(b != null ? b : BigDecimal.ZERO))));
            List<String> sorted = scoreBySymbol.entrySet().stream()
                    .sorted(Comparator.comparing(e -> e.getValue(), Comparator.reverseOrder()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            int topCount = Math.max(1, (int) Math.ceil(sorted.size() * 0.1));
            return sorted.stream().limit(topCount).collect(Collectors.toSet());
        }
        return allSymbols;
    }

    /**
     * Half-Kelly 공식으로 포지션 사이징 조정.
     * 켈리 공식: f* = (bp - q) / b. p·b는 전략별 설정(백테스트 winRate·profitFactor 연동) 또는 기본값 사용.
     * Half-Kelly: f*의 50%만 사용하여 파산 위험 방지.
     *
     * @param recommendations 기존 권장 포지션 목록
     * @param totalCapital 총 투자 가능 자산
     * @param strategyType 기간별 전략 (전략별 p·b 적용)
     * @return Half-Kelly 조정된 포지션 목록
     */
    private List<PositionRecommendationDto> applyHalfKelly(
            List<PositionRecommendationDto> recommendations, BigDecimal totalCapital, StrategyType strategyType) {
        if (recommendations.isEmpty()) {
            return recommendations;
        }

        BigDecimal p = getKellyP(strategyType);
        BigDecimal b = getKellyB(strategyType);

        // 켈리 공식 계산: f* = (bp - q) / b
        BigDecimal q = BigDecimal.ONE.subtract(p); // 패배율
        BigDecimal numerator = b.multiply(p).subtract(q);
        BigDecimal kellyFraction = numerator.divide(b, 6, RoundingMode.HALF_UP);

        // Half-Kelly: 50%만 적용
        BigDecimal halfKellyFraction = kellyFraction.multiply(new BigDecimal("0.5"));

        // 음수 또는 0이면 Kelly 적용 안 함
        if (halfKellyFraction.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("Half-Kelly 계산 결과 음수 또는 0: kellyFraction={}, halfKelly={}, p={}, b={}, strategyType={}",
                    kellyFraction, halfKellyFraction, p, b, strategyType);
            return recommendations;
        }

        // 최대 할당 비율 제한 (예: 20%)
        BigDecimal maxAllocationPct = new BigDecimal("0.2");
        BigDecimal effectiveFraction = halfKellyFraction.min(maxAllocationPct);

        log.debug("Half-Kelly 적용: p={}, b={}, kellyFraction={}, halfKelly={}, effectiveFraction={}, strategyType={}",
                p, b, kellyFraction, halfKellyFraction, effectiveFraction, strategyType);

        // 각 포지션에 Half-Kelly 비율 적용
        return recommendations.stream()
                .map(dto -> {
                    BigDecimal kellyAmt = totalCapital.multiply(effectiveFraction);
                    // 기존 ATR 기반 금액과 Kelly 기반 금액 중 작은 값 사용
                    BigDecimal finalAmt = dto.getRecommendedAmt().min(kellyAmt);
                    
                    if (dto.getEntryPrice().compareTo(BigDecimal.ZERO) <= 0) {
                        return dto;
                    }
                    
                    long qty = finalAmt.divide(dto.getEntryPrice(), 0, RoundingMode.DOWN).longValue();
                    if (qty <= 0) {
                        return dto;
                    }

                    // Kelly가 적용되었는지 메서드에 표시
                    String method = finalAmt.compareTo(dto.getRecommendedAmt()) < 0 
                            ? METHOD_KELLY + "+" + METHOD_ATR 
                            : dto.getMethod();

                    return PositionRecommendationDto.builder()
                            .basDt(dto.getBasDt())
                            .symbol(dto.getSymbol())
                            .market(dto.getMarket())
                            .recommendedAmt(dto.getEntryPrice().multiply(BigDecimal.valueOf(qty)))
                            .recommendedQty(qty)
                            .method(method)
                            .entryPrice(dto.getEntryPrice())
                            .stopLoss(dto.getStopLoss())
                            .build();
                })
                .filter(dto -> dto.getRecommendedQty() > 0)
                .collect(Collectors.toList());
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
