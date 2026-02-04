package com.investment.backtest;

import com.investment.backtest.dto.*;
import com.investment.config.FrictionCostProperties;
import com.investment.domain.entity.DailyStock;
import com.investment.domain.repository.DailyStockRepository;
import com.investment.factor.dto.PositionRecommendationDto;
import com.investment.factor.execution.ExitRuleEvaluator;
import com.investment.factor.execution.ExitRuleInput;
import com.investment.factor.execution.ExitRuleResult;
import com.investment.factor.service.PositionSizingService;
import com.investment.strategy.domain.StrategyType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 백테스트 엔진 — 과거 일봉·시그널로 4단계 파이프라인 재생, 메트릭 산출.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BacktestService {

    private static final int DEFAULT_TIME_CUT_DAYS = 5;
    private static final BigDecimal DEFAULT_TARGET_RETURN_PCT = new BigDecimal("3.0");
    private static final BigDecimal DEFAULT_ATR_MULTIPLIER = new BigDecimal("2.0");

    private final PositionSizingService positionSizingService;
    private final ExitRuleEvaluator exitRuleEvaluator;
    private final DailyStockRepository dailyStockRepository;
    private final FrictionCostProperties frictionCostProperties;

    /**
     * 백테스트 실행.
     *
     * @param request startDate, endDate, market, strategyType, initialCapital
     * @return 결과 (메트릭, 수익 곡선, 거래 목록)
     */
    public BacktestRunResult run(BacktestRunRequest request) {
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("startDate must be <= endDate");
        }
        if (request.getInitialCapital() == null || request.getInitialCapital().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("initialCapital must be > 0");
        }
        StrategyType strategyType = parseStrategyType(request.getStrategyType());

        BigDecimal cash = request.getInitialCapital();
        List<BacktestPosition> positions = new ArrayList<>();
        List<BacktestTradeDto> trades = new ArrayList<>();
        List<DateEquityPoint> equityCurve = new ArrayList<>();

        LocalDate start = request.getStartDate();
        LocalDate end = request.getEndDate();

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            // 1) 당일 고가로 trailing high 갱신
            for (BacktestPosition pos : positions) {
                BigDecimal todayHigh = getHigh(pos.getSymbol(), pos.getMarket(), date);
                if (todayHigh != null) {
                    pos.updateTrailingHigh(todayHigh);
                }
            }

            // 2) 청산 평가
            Iterator<BacktestPosition> it = positions.iterator();
            while (it.hasNext()) {
                BacktestPosition pos = it.next();
                BigDecimal close = getClose(pos.getSymbol(), pos.getMarket(), date);
                BigDecimal high = getHigh(pos.getSymbol(), pos.getMarket(), date);
                BigDecimal low = getLow(pos.getSymbol(), pos.getMarket(), date);
                if (close == null) {
                    continue;
                }
                pos.updateTrailingHigh(high);
                pos.updatePriorLow(close);
                if (low != null) {
                    pos.updatePriorLow(low);
                }
                ExitRuleInput input = ExitRuleInput.builder()
                        .entryPrice(pos.getEntryPrice())
                        .trailingHigh(pos.getTrailingHigh() != null ? pos.getTrailingHigh() : pos.getEntryPrice())
                        .priorLow(pos.getPriorLow() != null ? pos.getPriorLow() : pos.getEntryPrice())
                        .entryDt(pos.getEntryDt())
                        .strategyType(pos.getStrategyType())
                        .market(pos.getMarket())
                        .currentPrice(close)
                        .todayHigh(high)
                        .today(date)
                        .timeCutDays(pos.getTimeCutDays())
                        .targetReturnPct(pos.getTargetReturnPct())
                        .atrMultiplier(pos.getAtrMultiplier())
                        .rsi(null)
                        .build();
                ExitRuleResult result = exitRuleEvaluator.evaluate(input);
                if (result.isShouldExit()) {
                    BigDecimal cost = pos.getEntryPrice().multiply(BigDecimal.valueOf(pos.getQuantity()));
                    BigDecimal exitValue = close.multiply(BigDecimal.valueOf(pos.getQuantity()));
                    BigDecimal feeBuy = computeBuyFrictionCost(pos.getMarket(), cost);
                    BigDecimal feeSell = computeSellFrictionCost(pos.getMarket(), exitValue);
                    BigDecimal taf = computeTafCost(pos.getMarket(), pos.getQuantity());
                    BigDecimal totalFriction = feeBuy.add(feeSell).add(taf);
                    BigDecimal pnl = exitValue.subtract(cost).subtract(totalFriction);
                    BigDecimal pnlPct = pos.getEntryPrice().compareTo(BigDecimal.ZERO) != 0
                            ? pnl.divide(cost, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                            : BigDecimal.ZERO;
                    cash = cash.add(exitValue).subtract(feeSell).subtract(taf);
                    trades.add(BacktestTradeDto.builder()
                            .symbol(pos.getSymbol())
                            .market(pos.getMarket())
                            .strategyType(pos.getStrategyType().name())
                            .entryDt(pos.getEntryDt())
                            .exitDt(date)
                            .entryPrice(pos.getEntryPrice())
                            .exitPrice(close)
                            .quantity(pos.getQuantity())
                            .pnl(pnl)
                            .pnlPct(pnlPct)
                            .totalFrictionCost(totalFriction)
                            .exitReason(result.getReason())
                            .build());
                    it.remove();
                }
            }

            // 3) 현재 자산 (cash + 포지션 평가)
            BigDecimal mtm = cash;
            for (BacktestPosition pos : positions) {
                BigDecimal close = getClose(pos.getSymbol(), pos.getMarket(), date);
                if (close != null) {
                    mtm = mtm.add(close.multiply(BigDecimal.valueOf(pos.getQuantity())));
                } else {
                    mtm = mtm.add(pos.getEntryPrice().multiply(BigDecimal.valueOf(pos.getQuantity())));
                }
            }
            equityCurve.add(DateEquityPoint.builder().date(date).equity(mtm).build());

            // 4) 매수: 권장 포지션
            List<PositionRecommendationDto> recommendations = positionSizingService.getRecommendations(
                    date, request.getMarket(), strategyType, mtm);
            for (PositionRecommendationDto rec : recommendations) {
                if (rec.getEntryPrice() == null || rec.getEntryPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                long affordable = rec.getEntryPrice().compareTo(BigDecimal.ZERO) > 0
                        ? cash.divide(rec.getEntryPrice(), 0, RoundingMode.DOWN).longValue()
                        : 0;
                if (affordable <= 0) {
                    continue;
                }
                long buyQty = Math.min(rec.getRecommendedQty(), affordable);
                if (buyQty <= 0) {
                    continue;
                }
                BigDecimal cost = rec.getEntryPrice().multiply(BigDecimal.valueOf(buyQty));
                BigDecimal feeBuy = computeBuyFrictionCost(rec.getMarket(), cost);
                cash = cash.subtract(cost).subtract(feeBuy);
                BigDecimal entryPrice = rec.getEntryPrice();
                // Time-Cut은 SHORT_TERM 전용
                int timeCutDays = (strategyType == StrategyType.SHORT_TERM) ? DEFAULT_TIME_CUT_DAYS : 0;
                BigDecimal targetReturnPct = (strategyType == StrategyType.SHORT_TERM) ? DEFAULT_TARGET_RETURN_PCT
                        : null;
                positions.add(BacktestPosition.builder()
                        .symbol(rec.getSymbol())
                        .market(rec.getMarket())
                        .strategyType(strategyType)
                        .entryDt(date)
                        .entryPrice(entryPrice)
                        .quantity((int) buyQty)
                        .trailingHigh(entryPrice)
                        .priorLow(entryPrice)
                        .timeCutDays(timeCutDays)
                        .targetReturnPct(targetReturnPct)
                        .atrMultiplier(DEFAULT_ATR_MULTIPLIER)
                        .build());
            }
        }

        // 최종 자산: cash + 미청산 포지션 평가 (end 일자 종가)
        BigDecimal finalEquity = cash;
        for (BacktestPosition pos : positions) {
            BigDecimal close = getClose(pos.getSymbol(), pos.getMarket(), end);
            if (close == null) {
                close = getCloseLatest(pos.getSymbol(), pos.getMarket(), end);
            }
            if (close == null) {
                close = pos.getEntryPrice();
            }
            finalEquity = finalEquity.add(close.multiply(BigDecimal.valueOf(pos.getQuantity())));
        }

        BigDecimal totalReturnPct = request.getInitialCapital().compareTo(BigDecimal.ZERO) != 0
                ? finalEquity.subtract(request.getInitialCapital())
                        .divide(request.getInitialCapital(), 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        long days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
        double years = Math.max(1e-6, days / 365.25);
        BigDecimal cagr = BigDecimal.ZERO;
        if (request.getInitialCapital().compareTo(BigDecimal.ZERO) > 0 && finalEquity.compareTo(BigDecimal.ZERO) > 0) {
            double ratio = finalEquity.doubleValue() / request.getInitialCapital().doubleValue();
            double cagrDouble = (Math.pow(ratio, 1.0 / years) - 1.0) * 100;
            cagr = BigDecimal.valueOf(cagrDouble).setScale(4, RoundingMode.HALF_UP);
        }

        BigDecimal mddPct = computeMddPct(equityCurve);
        BigDecimal sharpeRatio = computeSharpe(equityCurve);
        BigDecimal sortinoRatio = computeSortino(equityCurve);
        BigDecimal calmarRatio = (mddPct != null && mddPct.abs().compareTo(BigDecimal.ZERO) > 0)
                ? cagr.divide(mddPct.abs(), 4, RoundingMode.HALF_UP)
                : null;

        int winningTrades = (int) trades.stream()
                .filter(t -> t.getPnl() != null && t.getPnl().compareTo(BigDecimal.ZERO) > 0).count();
        int losingTrades = (int) trades.stream()
                .filter(t -> t.getPnl() != null && t.getPnl().compareTo(BigDecimal.ZERO) < 0).count();
        BigDecimal avgWin = winningTrades > 0
                ? trades.stream().filter(t -> t.getPnl() != null && t.getPnl().compareTo(BigDecimal.ZERO) > 0)
                        .map(BacktestTradeDto::getPnl).reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(winningTrades), 4, RoundingMode.HALF_UP)
                : null;
        BigDecimal avgLoss = losingTrades > 0
                ? trades.stream().filter(t -> t.getPnl() != null && t.getPnl().compareTo(BigDecimal.ZERO) < 0)
                        .map(BacktestTradeDto::getPnl).reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(losingTrades), 4, RoundingMode.HALF_UP)
                : null;
        BigDecimal winRate = trades.isEmpty() ? null
                : BigDecimal.valueOf(winningTrades).divide(BigDecimal.valueOf(trades.size()), 4, RoundingMode.HALF_UP);
        BigDecimal sumWins = trades.stream()
                .filter(t -> t.getPnl() != null && t.getPnl().compareTo(BigDecimal.ZERO) > 0)
                .map(BacktestTradeDto::getPnl).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sumLosses = trades.stream()
                .filter(t -> t.getPnl() != null && t.getPnl().compareTo(BigDecimal.ZERO) < 0)
                .map(BacktestTradeDto::getPnl).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal profitFactor = (sumLosses != null && sumLosses.abs().compareTo(BigDecimal.ZERO) > 0)
                ? sumWins.divide(sumLosses.abs(), 4, RoundingMode.HALF_UP)
                : (sumWins.compareTo(BigDecimal.ZERO) > 0 ? sumWins : null);

        return BacktestRunResult.builder()
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .market(request.getMarket())
                .strategyType(request.getStrategyType())
                .initialCapital(request.getInitialCapital())
                .finalEquity(finalEquity)
                .totalReturnPct(totalReturnPct)
                .cagr(cagr)
                .mddPct(mddPct)
                .sharpeRatio(sharpeRatio)
                .sortinoRatio(sortinoRatio)
                .calmarRatio(calmarRatio)
                .winRate(winRate)
                .avgWin(avgWin)
                .avgLoss(avgLoss)
                .profitFactor(profitFactor)
                .tradeCount(trades.size())
                .winningTrades(winningTrades)
                .losingTrades(losingTrades)
                .equityCurve(equityCurve)
                .trades(trades)
                .build();
    }

    /** 매수 시 마찰 비용: notional * (commission + slippage). */
    private BigDecimal computeBuyFrictionCost(String market, BigDecimal notional) {
        if (notional == null || notional.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (isUsMarket(market)) {
            FrictionCostProperties.UsaStockFee usa = frictionCostProperties.getUsa().getStock();
            BigDecimal rate = usa.getCommission().add(usa.getSlippage());
            return notional.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        }
        FrictionCostProperties.StockFee kr = frictionCostProperties.getKorea().getStock();
        BigDecimal rate = kr.getCommission().add(kr.getSlippage());
        return notional.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    /** 매도 시 마찰 비용: notional * (commission + tax(KR) or secFee(US) + slippage). */
    private BigDecimal computeSellFrictionCost(String market, BigDecimal notional) {
        if (notional == null || notional.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (isUsMarket(market)) {
            FrictionCostProperties.UsaStockFee usa = frictionCostProperties.getUsa().getStock();
            BigDecimal rate = usa.getCommission().add(usa.getSecFee()).add(usa.getSlippage());
            return notional.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        }
        FrictionCostProperties.StockFee kr = frictionCostProperties.getKorea().getStock();
        BigDecimal rate = kr.getCommission().add(kr.getTax()).add(kr.getSlippage());
        return notional.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    /** TAF 비용 (미국 매도 시만): qty * tafPerShareUsd (USD). */
    private BigDecimal computeTafCost(String market, int quantity) {
        if (!isUsMarket(market) || quantity <= 0) {
            return BigDecimal.ZERO;
        }
        return frictionCostProperties.getUsa().getStock().getTafPerShareUsd()
                .multiply(BigDecimal.valueOf(quantity))
                .setScale(4, RoundingMode.HALF_UP);
    }

    private boolean isUsMarket(String market) {
        return market != null && "US".equalsIgnoreCase(market.trim());
    }

    private StrategyType parseStrategyType(String s) {
        if (s == null) {
            return StrategyType.SHORT_TERM;
        }
        try {
            return StrategyType.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return StrategyType.SHORT_TERM;
        }
    }

    private BigDecimal getClose(String symbol, String market, LocalDate date) {
        List<DailyStock> list = dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(symbol, market,
                date, date);
        return list.isEmpty() || list.get(0).getClosePrice() == null ? null : list.get(0).getClosePrice();
    }

    private BigDecimal getHigh(String symbol, String market, LocalDate date) {
        List<DailyStock> list = dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(symbol, market,
                date, date);
        return list.isEmpty() || list.get(0).getHighPrice() == null ? null : list.get(0).getHighPrice();
    }

    private BigDecimal getLow(String symbol, String market, LocalDate date) {
        List<DailyStock> list = dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(symbol, market,
                date, date);
        return list.isEmpty() || list.get(0).getLowPrice() == null ? null : list.get(0).getLowPrice();
    }

    private BigDecimal getCloseLatest(String symbol, String market, LocalDate beforeOrEqual) {
        List<DailyStock> list = dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(symbol, market,
                beforeOrEqual.minusDays(60), beforeOrEqual);
        return list.isEmpty() || list.get(list.size() - 1).getClosePrice() == null ? null
                : list.get(list.size() - 1).getClosePrice();
    }

    private BigDecimal computeMddPct(List<DateEquityPoint> curve) {
        if (curve == null || curve.isEmpty()) {
            return null;
        }
        BigDecimal peak = curve.get(0).getEquity();
        BigDecimal mdd = BigDecimal.ZERO;
        for (DateEquityPoint p : curve) {
            if (p.getEquity().compareTo(peak) > 0) {
                peak = p.getEquity();
            }
            if (peak.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal drawdown = peak.subtract(p.getEquity()).divide(peak, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                if (drawdown.compareTo(mdd) > 0) {
                    mdd = drawdown;
                }
            }
        }
        return mdd.negate();
    }

    private BigDecimal computeSharpe(List<DateEquityPoint> curve) {
        if (curve == null || curve.size() < 2) {
            return null;
        }
        List<BigDecimal> returns = new ArrayList<>();
        for (int i = 1; i < curve.size(); i++) {
            BigDecimal prev = curve.get(i - 1).getEquity();
            BigDecimal curr = curve.get(i).getEquity();
            if (prev.compareTo(BigDecimal.ZERO) > 0) {
                returns.add(curr.subtract(prev).divide(prev, 6, RoundingMode.HALF_UP));
            }
        }
        if (returns.isEmpty()) {
            return null;
        }
        BigDecimal mean = returns.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 6, RoundingMode.HALF_UP);
        BigDecimal variance = returns.stream()
                .map(r -> r.subtract(mean).multiply(r.subtract(mean)))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 6, RoundingMode.HALF_UP);
        double std = Math.sqrt(variance.doubleValue());
        if (std < 1e-12) {
            return null;
        }
        double sharpe = mean.doubleValue() / std * Math.sqrt(252);
        return BigDecimal.valueOf(sharpe).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal computeSortino(List<DateEquityPoint> curve) {
        if (curve == null || curve.size() < 2) {
            return null;
        }
        List<BigDecimal> returns = new ArrayList<>();
        for (int i = 1; i < curve.size(); i++) {
            BigDecimal prev = curve.get(i - 1).getEquity();
            BigDecimal curr = curve.get(i).getEquity();
            if (prev.compareTo(BigDecimal.ZERO) > 0) {
                returns.add(curr.subtract(prev).divide(prev, 6, RoundingMode.HALF_UP));
            }
        }
        if (returns.isEmpty()) {
            return null;
        }
        BigDecimal mean = returns.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 6, RoundingMode.HALF_UP);
        List<BigDecimal> negative = returns.stream().filter(r -> r.compareTo(BigDecimal.ZERO) < 0)
                .collect(Collectors.toList());
        if (negative.isEmpty()) {
            return mean.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(999.0) : null;
        }
        BigDecimal downVariance = negative.stream()
                .map(r -> r.multiply(r))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(negative.size()), 6, RoundingMode.HALF_UP);
        double downStd = Math.sqrt(downVariance.doubleValue());
        if (downStd < 1e-12) {
            return null;
        }
        double sortino = mean.doubleValue() / downStd * Math.sqrt(252);
        return BigDecimal.valueOf(sortino).setScale(4, RoundingMode.HALF_UP);
    }
}
