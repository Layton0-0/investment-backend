package com.investment.backtest.robo;

import com.investment.backtest.dto.DateEquityPoint;
import com.investment.backtest.robo.dto.*;
import com.investment.config.FrictionCostProperties;
import com.investment.config.RoboBacktestProperties;
import com.investment.domain.entity.DailyStock;
import com.investment.domain.repository.DailyStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.*;

/**
 * 로보 어드바이저 백테스트 엔진 — 과거 일봉으로 동적 자산배분 재생, 메트릭·수익 곡선·리밸런싱 이력 산출.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoboBacktestService {

    private static final String MARKET_US = "US";

    private final RoboAllocationEngine roboAllocationEngine;
    private final RoboBacktestProperties roboBacktestProperties;
    private final FrictionCostProperties frictionCostProperties;
    private final DailyStockRepository dailyStockRepository;

    /**
     * 로보 어드바이저 백테스트 실행.
     */
    public RoboBacktestResult run(RoboBacktestRequest request) {
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("startDate must be <= endDate");
        }
        if (request.getInitialCapital() == null || request.getInitialCapital().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("initialCapital must be > 0");
        }

        log.info("로보 백테스트 시작: start={}, end={}, initialCapital={}", request.getStartDate(), request.getEndDate(),
                request.getInitialCapital());

        String dualMode = request.getDualMomentumMode() != null ? request.getDualMomentumMode()
                : roboBacktestProperties.getDualMomentumMode();
        boolean useDualMomentumNote = "DUAL_MOMENTUM_NOTE".equalsIgnoreCase(dualMode);

        List<String> symbols = useDualMomentumNote ? roboBacktestProperties.getSectorEtfSymbolList()
                : resolveAssetSymbols(request);
        int momentumMonths = request.getMomentumMonths() != null ? request.getMomentumMonths()
                : roboBacktestProperties.getMomentumMonths();
        int maWindowDays = request.getMaWindowDays() != null ? request.getMaWindowDays()
                : roboBacktestProperties.getMaWindowDays();
        int topN = request.getTopN() != null ? request.getTopN() : roboBacktestProperties.getTopN();
        int volLookback = request.getVolatilityLookbackDays() != null ? request.getVolatilityLookbackDays()
                : roboBacktestProperties.getVolatilityLookbackDays();
        String rebalanceFreq = request.getRebalanceFrequency() != null ? request.getRebalanceFrequency()
                : roboBacktestProperties.getRebalanceFrequency();
        BigDecimal riskFreePct = request.getRiskFreeRatePct() != null ? request.getRiskFreeRatePct()
                : roboBacktestProperties.getRiskFreeRatePct();
        Map<String, BigDecimal> benchmarkWeights = resolveBenchmarkWeights(request);
        Set<String> allSymbolsForDataCheck = new HashSet<>(symbols);
        allSymbolsForDataCheck.addAll(benchmarkWeights.keySet());
        log.debug(
                "로보 백테스트 파라미터: dualMomentum={}, symbolsCount={}, momentumMonths={}, maWindowDays={}, topN={}, rebalanceFreq={}, benchmarkSymbols={}",
                dualMode, symbols.size(), momentumMonths, maWindowDays, topN, rebalanceFreq, benchmarkWeights.keySet());

        String warningMessage = null;
        if (!hasAnyUsDailyDataInRange(allSymbolsForDataCheck, request.getStartDate(), request.getEndDate())) {
            warningMessage = "선택 기간에 US 일봉 데이터가 없습니다. 데이터 수집 후 다시 시도하세요.";
            log.warn("로보 백테스트: 구간 내 US 일봉 없음 — start={}, end={}, 검사한 심볼={} (데이터 수집 또는 구간 확인 필요)",
                    request.getStartDate(),
                    request.getEndDate(), allSymbolsForDataCheck);
        }
        boolean useRequestFeeOverride = request.getCommPct() != null || request.getSlipPct() != null;
        String executionPriceMode = request.getRebalanceExecutionPrice() != null
                ? request.getRebalanceExecutionPrice()
                : (roboBacktestProperties.getRebalanceExecutionPrice() != null
                        ? roboBacktestProperties.getRebalanceExecutionPrice()
                        : "CLOSE");
        boolean useNextOpen = "NEXT_OPEN".equalsIgnoreCase(executionPriceMode);

        List<LocalDate> rebalanceDates = enumerateRebalanceDates(request.getStartDate(), request.getEndDate(),
                rebalanceFreq);

        BigDecimal cash = request.getInitialCapital();
        Map<String, BigDecimal> holdings = new HashMap<>(); // symbol -> quantity (shares)
        List<DateEquityPoint> equityCurve = new ArrayList<>();
        List<DateEquityPoint> benchmarkCurve = new ArrayList<>();
        List<RebalanceSnapshotDto> rebalanceHistory = new ArrayList<>();
        double totalTurnoverPct = 0;
        int rebalanceCount = 0;

        BigDecimal benchmarkValue = request.getInitialCapital();
        Map<String, BigDecimal> prevBenchmarkPrices = new HashMap<>();

        LocalDate current = request.getStartDate();
        LocalDate end = request.getEndDate();
        Set<LocalDate> rebalanceSet = new HashSet<>(rebalanceDates);

        while (!current.isAfter(end)) {
            if (rebalanceSet.contains(current)) {
                RoboAllocationResult allocation;
                if (useDualMomentumNote) {
                    allocation = roboAllocationEngine.computeTargetWeightsDualMomentumNote(
                            current,
                            symbols,
                            12,
                            riskFreePct,
                            roboBacktestProperties.getAbsoluteMomentumSymbol(),
                            roboBacktestProperties.getRiskFreeSymbol(),
                            roboBacktestProperties.getMomentumMonthsRelative(),
                            roboBacktestProperties.getTopNSector(),
                            maWindowDays,
                            volLookback);
                } else {
                    allocation = roboAllocationEngine.computeTargetWeights(
                            current, symbols, momentumMonths, maWindowDays, topN, volLookback);
                }
                if (allocation.getWeights() == null || allocation.getWeights().isEmpty()) {
                    log.debug("로보 백테스트 리밸런싱 전액 현금: date={}, dualMomentum={} — 할당 엔진이 종목 비중 0건 반환(모멘텀/MA 미충족 또는 데이터 부족)",
                            current, useDualMomentumNote);
                }

                BigDecimal portfolioValue = cash;
                for (Map.Entry<String, BigDecimal> e : holdings.entrySet()) {
                    BigDecimal price = getClose(e.getKey(), current);
                    if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                        portfolioValue = portfolioValue.add(e.getValue().multiply(price));
                    }
                }

                Map<String, BigDecimal> currentWeights = new HashMap<>();
                if (portfolioValue.compareTo(BigDecimal.ZERO) > 0) {
                    for (Map.Entry<String, BigDecimal> e : holdings.entrySet()) {
                        BigDecimal price = getClose(e.getKey(), current);
                        if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal val = e.getValue().multiply(price);
                            currentWeights.put(e.getKey(), val.divide(portfolioValue, 6, RoundingMode.HALF_UP));
                        }
                    }
                }
                BigDecimal cashW = allocation.getCashWeight() != null ? allocation.getCashWeight() : BigDecimal.ZERO;
                currentWeights.put("CASH", cashW);

                BigDecimal turnoverPct = BigDecimal.ZERO;
                for (String s : allocation.getWeights().keySet()) {
                    BigDecimal tw = allocation.getWeights().get(s);
                    BigDecimal cw = currentWeights.getOrDefault(s, BigDecimal.ZERO);
                    turnoverPct = turnoverPct.add(tw.subtract(cw).abs());
                }
                turnoverPct = turnoverPct.divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                totalTurnoverPct += turnoverPct.doubleValue();
                rebalanceCount++;

                LocalDate executionDate = useNextOpen ? current.plusDays(1) : current;
                Map<String, BigDecimal> targetWeights = allocation.getWeights();
                BigDecimal tradeValue = BigDecimal.ZERO;
                for (String sym : targetWeights.keySet()) {
                    BigDecimal targetW = targetWeights.get(sym);
                    BigDecimal targetVal = portfolioValue.multiply(targetW);
                    BigDecimal price = getExecutionPrice(sym, executionDate, useNextOpen);
                    BigDecimal currentVal = BigDecimal.ZERO;
                    if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal qty = holdings.getOrDefault(sym, BigDecimal.ZERO);
                        currentVal = qty.multiply(price);
                    }
                    tradeValue = tradeValue.add(targetVal.subtract(currentVal).abs());
                }
                for (String sym : new HashSet<>(holdings.keySet())) {
                    if (!targetWeights.containsKey(sym)) {
                        BigDecimal price = getExecutionPrice(sym, executionDate, useNextOpen);
                        if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                            tradeValue = tradeValue.add(holdings.get(sym).multiply(price));
                        }
                    }
                }

                BigDecimal cost;
                if (useRequestFeeOverride) {
                    BigDecimal commPct = request.getCommPct() != null ? request.getCommPct()
                            : roboBacktestProperties.getCommPct();
                    BigDecimal slipPct = request.getSlipPct() != null ? request.getSlipPct()
                            : roboBacktestProperties.getSlipPct();
                    BigDecimal costPct = commPct.add(slipPct).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
                    cost = tradeValue.multiply(costPct);
                } else {
                    FrictionCostProperties.UsaStockFee usaStock = frictionCostProperties.getUsa().getStock();
                    FrictionCostProperties.CurrencyFee usaCurrency = frictionCostProperties.getUsa().getCurrency();
                    BigDecimal roundTripPct = usaStock.getCommission().add(usaStock.getCommission())
                            .add(usaStock.getSecFee())
                            .add(usaStock.getSlippage()).add(usaStock.getSlippage())
                            .add(usaCurrency.getExchangeRateSpread()).add(usaCurrency.getExchangeRateSpread());
                    BigDecimal tafCost = computeRoboTafCost(holdings, targetWeights, portfolioValue, executionDate,
                            useNextOpen, usaStock.getTafPerShareUsd());
                    cost = tradeValue.multiply(roundTripPct).add(tafCost);
                }
                BigDecimal totalAfterCost = portfolioValue.subtract(cost);

                holdings.clear();
                for (Map.Entry<String, BigDecimal> e : targetWeights.entrySet()) {
                    BigDecimal w = e.getValue();
                    BigDecimal targetVal = totalAfterCost.multiply(w);
                    BigDecimal price = getExecutionPrice(e.getKey(), executionDate, useNextOpen);
                    if (price != null && price.compareTo(BigDecimal.ZERO) > 0
                            && targetVal.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal qty = targetVal.divide(price, 6, RoundingMode.DOWN);
                        if (qty.compareTo(BigDecimal.ZERO) > 0) {
                            holdings.put(e.getKey(), qty);
                        }
                    }
                }
                cash = totalAfterCost.multiply(cashW).setScale(2, RoundingMode.HALF_UP);

                rebalanceHistory.add(RebalanceSnapshotDto.builder()
                        .date(current)
                        .weights(new LinkedHashMap<>(allocation.getWeights()))
                        .cashWeight(cashW)
                        .turnoverPct(turnoverPct)
                        .build());
            }

            BigDecimal portfolioValue = cash;
            for (Map.Entry<String, BigDecimal> e : holdings.entrySet()) {
                BigDecimal price = getClose(e.getKey(), current);
                if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                    portfolioValue = portfolioValue.add(e.getValue().multiply(price));
                }
            }
            equityCurve.add(DateEquityPoint.builder().date(current).equity(portfolioValue).build());

            BigDecimal benchReturn = BigDecimal.ONE;
            for (Map.Entry<String, BigDecimal> bw : benchmarkWeights.entrySet()) {
                BigDecimal price = getClose(bw.getKey(), current);
                if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal prevPrice = prevBenchmarkPrices.get(bw.getKey());
                    if (prevPrice != null && prevPrice.compareTo(BigDecimal.ZERO) > 0) {
                        benchReturn = benchReturn.add(bw.getValue()
                                .multiply(price.subtract(prevPrice).divide(prevPrice, 6, RoundingMode.HALF_UP)));
                    }
                    prevBenchmarkPrices.put(bw.getKey(), price);
                }
            }
            benchmarkValue = benchmarkValue.multiply(benchReturn);
            benchmarkCurve.add(DateEquityPoint.builder().date(current).equity(benchmarkValue).build());

            current = current.plusDays(1);
        }

        BigDecimal finalEquity = equityCurve.isEmpty() ? request.getInitialCapital()
                : equityCurve.get(equityCurve.size() - 1).getEquity();
        BigDecimal totalReturnPct = request.getInitialCapital().compareTo(BigDecimal.ZERO) != 0
                ? finalEquity.subtract(request.getInitialCapital())
                        .divide(request.getInitialCapital(), 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        long days = java.time.temporal.ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        double years = Math.max(1e-6, days / 365.25);
        BigDecimal cagr = BigDecimal.ZERO;
        if (request.getInitialCapital().compareTo(BigDecimal.ZERO) > 0 && finalEquity.compareTo(BigDecimal.ZERO) > 0) {
            double ratio = finalEquity.doubleValue() / request.getInitialCapital().doubleValue();
            double cagrDouble = (Math.pow(ratio, 1.0 / years) - 1.0) * 100;
            cagr = BigDecimal.valueOf(cagrDouble).setScale(4, RoundingMode.HALF_UP);
        }

        BigDecimal mddPct = computeMddPct(equityCurve);
        BigDecimal sharpeRatio = computeSharpe(equityCurve, riskFreePct);
        BigDecimal calmarRatio = (mddPct != null && mddPct.abs().compareTo(BigDecimal.ZERO) > 0)
                ? cagr.divide(mddPct.abs(), 4, RoundingMode.HALF_UP)
                : null;

        BigDecimal turnoverAnnual = rebalanceCount > 0 && years > 0
                ? BigDecimal.valueOf(totalTurnoverPct / years).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal benchFinal = benchmarkCurve.isEmpty() ? request.getInitialCapital()
                : benchmarkCurve.get(benchmarkCurve.size() - 1).getEquity();
        BigDecimal benchmarkCagr = BigDecimal.ZERO;
        if (request.getInitialCapital().compareTo(BigDecimal.ZERO) > 0 && benchFinal.compareTo(BigDecimal.ZERO) > 0) {
            double br = benchFinal.doubleValue() / request.getInitialCapital().doubleValue();
            benchmarkCagr = BigDecimal.valueOf((Math.pow(br, 1.0 / years) - 1.0) * 100).setScale(4,
                    RoundingMode.HALF_UP);
        }
        BigDecimal benchmarkMddPct = computeMddPct(benchmarkCurve);

        if (warningMessage == null && isEquityCurveFlat(equityCurve, request.getInitialCapital())) {
            warningMessage = "수익 곡선이 평평합니다. US 일봉 데이터 구간을 확인하세요.";
            log.warn("로보 백테스트: 수익 곡선 평평 — start={}, end={}, initialCapital={}, finalEquity={} (일봉 부재 또는 전액 현금 배분 가능성)",
                    request.getStartDate(), request.getEndDate(), request.getInitialCapital(), finalEquity);
        }

        log.info("로보 백테스트 완료: start={}, end={}, finalEquity={}, totalReturnPct={}, rebalanceCount={}",
                request.getStartDate(), request.getEndDate(), finalEquity, totalReturnPct, rebalanceCount);
        return RoboBacktestResult.builder()
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .initialCapital(request.getInitialCapital())
                .finalEquity(finalEquity)
                .totalReturnPct(totalReturnPct)
                .cagr(cagr)
                .mddPct(mddPct)
                .sharpeRatio(sharpeRatio)
                .calmarRatio(calmarRatio)
                .turnover(turnoverAnnual)
                .benchmarkCagr(benchmarkCagr)
                .benchmarkMddPct(benchmarkMddPct)
                .equityCurve(equityCurve)
                .benchmarkCurve(benchmarkCurve)
                .rebalanceHistory(rebalanceHistory)
                .warningMessage(warningMessage)
                .build();
    }

    /**
     * 요청 구간 내에 주어진 심볼 중 하나라도 US 일봉이 1건 이상 있는지 검사.
     */
    private boolean hasAnyUsDailyDataInRange(Set<String> symbols, LocalDate start, LocalDate end) {
        for (String symbol : symbols) {
            List<DailyStock> list = dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                    symbol, MARKET_US, start, end);
            if (!list.isEmpty()) {
                log.debug("로보 백테스트 데이터 검사: 구간 내 일봉 있음 symbol={}, start={}, end={}, count={}", symbol, start, end,
                        list.size());
                return true;
            }
        }
        log.debug("로보 백테스트 데이터 검사: 모든 심볼에서 일봉 없음 symbols={}, start={}, end={}", symbols, start, end);
        return false;
    }

    /**
     * 수익 곡선이 전 구간 동일 값(또는 전부 0)인지 여부.
     */
    private boolean isEquityCurveFlat(List<DateEquityPoint> curve, BigDecimal initialCapital) {
        if (curve == null || curve.size() <= 1) {
            return false;
        }
        BigDecimal first = curve.get(0).getEquity();
        if (first == null) {
            return false;
        }
        for (int i = 1; i < curve.size(); i++) {
            BigDecimal e = curve.get(i).getEquity();
            if (e == null || e.compareTo(first) != 0) {
                return false;
            }
        }
        return true;
    }

    private List<String> resolveAssetSymbols(RoboBacktestRequest request) {
        if (request.getAssetSymbols() != null && !request.getAssetSymbols().isEmpty()) {
            return request.getAssetSymbols();
        }
        return roboBacktestProperties.getAssetSymbolList();
    }

    private Map<String, BigDecimal> resolveBenchmarkWeights(RoboBacktestRequest request) {
        if (request.getBenchmarkWeights() != null && !request.getBenchmarkWeights().isEmpty()) {
            return request.getBenchmarkWeights();
        }
        String s = roboBacktestProperties.getBenchmarkWeights();
        if (s == null || s.isBlank()) {
            return Map.of("SPY", new BigDecimal("0.6"), "TLT", new BigDecimal("0.4"));
        }
        Map<String, BigDecimal> out = new LinkedHashMap<>();
        for (String part : s.split(",")) {
            String[] kv = part.trim().split("=");
            if (kv.length == 2) {
                try {
                    out.put(kv[0].trim(), new BigDecimal(kv[1].trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return out.isEmpty() ? Map.of("SPY", new BigDecimal("0.6"), "TLT", new BigDecimal("0.4")) : out;
    }

    private List<LocalDate> enumerateRebalanceDates(LocalDate start, LocalDate end, String frequency) {
        List<LocalDate> dates = new ArrayList<>();
        if ("QUARTERLY".equalsIgnoreCase(frequency)) {
            int startYear = start.getYear();
            int endYear = end.getYear();
            Month[] quarterEnds = { Month.MARCH, Month.JUNE, Month.SEPTEMBER, Month.DECEMBER };
            for (int y = startYear; y <= endYear; y++) {
                for (Month m : quarterEnds) {
                    LocalDate last = YearMonth.of(y, m).atEndOfMonth();
                    if (!last.isBefore(start) && !last.isAfter(end)) {
                        dates.add(last);
                    }
                }
            }
        } else {
            YearMonth ym = YearMonth.from(start);
            YearMonth endYm = YearMonth.from(end);
            while (!ym.isAfter(endYm)) {
                LocalDate last = ym.atEndOfMonth();
                if (!last.isBefore(start) && !last.isAfter(end)) {
                    dates.add(last);
                }
                ym = ym.plusMonths(1);
            }
        }
        Collections.sort(dates);
        return dates;
    }

    /**
     * 리밸런싱 시 매도 수량 합계에 TAF(주당 USD)를 곱한 비용. 미국 ETF 매도 시만 적용.
     */
    private BigDecimal computeRoboTafCost(Map<String, BigDecimal> holdings,
            Map<String, BigDecimal> targetWeights,
            BigDecimal portfolioValue,
            LocalDate executionDate,
            boolean useNextOpen,
            BigDecimal tafPerShareUsd) {
        if (tafPerShareUsd == null || tafPerShareUsd.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal totalSoldQty = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> e : holdings.entrySet()) {
            BigDecimal oldQty = e.getValue();
            if (oldQty == null || oldQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal price = getExecutionPrice(e.getKey(), executionDate, useNextOpen);
            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal targetW = targetWeights.get(e.getKey());
            BigDecimal newQty;
            if (targetW == null || targetW.compareTo(BigDecimal.ZERO) <= 0) {
                newQty = BigDecimal.ZERO;
            } else {
                BigDecimal targetVal = portfolioValue.multiply(targetW);
                newQty = targetVal.divide(price, 6, RoundingMode.DOWN).max(BigDecimal.ZERO);
            }
            BigDecimal soldQty = oldQty.subtract(newQty).max(BigDecimal.ZERO);
            totalSoldQty = totalSoldQty.add(soldQty);
        }
        return totalSoldQty.multiply(tafPerShareUsd).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal getClose(String symbol, LocalDate date) {
        List<DailyStock> list = dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(symbol,
                MARKET_US, date, date);
        if (list.isEmpty()) {
            log.trace("로보 백테스트 종가 없음: symbol={}, date={} — TB_DAILY_STOCK 해당 일자 데이터 없음", symbol, date);
            return null;
        }
        BigDecimal close = list.get(0).getClosePrice();
        if (close == null) {
            log.trace("로보 백테스트 종가 null: symbol={}, date={} — 일봉 행은 있으나 closePrice 없음", symbol, date);
            return null;
        }
        return close;
    }

    private BigDecimal getOpen(String symbol, LocalDate date) {
        List<DailyStock> list = dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(symbol,
                MARKET_US, date, date);
        if (list.isEmpty()) {
            log.trace("로보 백테스트 시가 없음: symbol={}, date={} — TB_DAILY_STOCK 해당 일자 데이터 없음", symbol, date);
            return null;
        }
        BigDecimal open = list.get(0).getOpenPrice();
        return open != null && open.compareTo(BigDecimal.ZERO) > 0 ? open : list.get(0).getClosePrice();
    }

    /** 리밸런싱 실행가: useNextOpen이면 시가(없으면 종가), 아니면 당일 종가 */
    private BigDecimal getExecutionPrice(String symbol, LocalDate date, boolean useNextOpen) {
        if (useNextOpen) {
            BigDecimal open = getOpen(symbol, date);
            return open != null ? open : getClose(symbol, date);
        }
        return getClose(symbol, date);
    }

    private BigDecimal computeMddPct(List<DateEquityPoint> curve) {
        if (curve == null || curve.isEmpty())
            return null;
        BigDecimal peak = curve.get(0).getEquity();
        BigDecimal mdd = BigDecimal.ZERO;
        for (DateEquityPoint p : curve) {
            if (p.getEquity().compareTo(peak) > 0)
                peak = p.getEquity();
            if (peak.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal dd = peak.subtract(p.getEquity()).divide(peak, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                if (dd.compareTo(mdd) > 0)
                    mdd = dd;
            }
        }
        return mdd.negate();
    }

    private BigDecimal computeSharpe(List<DateEquityPoint> curve, BigDecimal riskFreePct) {
        if (curve == null || curve.size() < 2)
            return null;
        List<BigDecimal> returns = new ArrayList<>();
        for (int i = 1; i < curve.size(); i++) {
            BigDecimal prev = curve.get(i - 1).getEquity();
            BigDecimal curr = curve.get(i).getEquity();
            if (prev.compareTo(BigDecimal.ZERO) > 0) {
                returns.add(curr.subtract(prev).divide(prev, 6, RoundingMode.HALF_UP));
            }
        }
        if (returns.isEmpty())
            return null;
        BigDecimal rfDaily = riskFreePct != null
                ? riskFreePct.divide(BigDecimal.valueOf(100 * 252), 6, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal mean = returns.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 6, RoundingMode.HALF_UP).subtract(rfDaily);
        BigDecimal variance = returns.stream()
                .map(r -> r.subtract(rfDaily).subtract(mean).multiply(r.subtract(rfDaily).subtract(mean)))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 6, RoundingMode.HALF_UP);
        double std = Math.sqrt(Math.max(0, variance.doubleValue()));
        if (std < 1e-12)
            return null;
        return BigDecimal.valueOf(mean.doubleValue() / std * Math.sqrt(252)).setScale(4, RoundingMode.HALF_UP);
    }
}
