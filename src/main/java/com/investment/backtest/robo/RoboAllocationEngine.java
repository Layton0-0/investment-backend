package com.investment.backtest.robo;

import com.investment.backtest.robo.dto.RoboAllocationResult;
import com.investment.domain.entity.DailyStock;
import com.investment.domain.repository.DailyStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 로보 어드바이저 공통 할당 엔진 — 모멘텀 스코어·MA 필터·변동성 역가중으로 목표 비중 산출.
 * 백테스트와 실전 리밸런싱이 동일 규칙을 사용하도록 단일 진입점 제공.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoboAllocationEngine {

    private static final String MARKET_US = "US";

    private final DailyStockRepository dailyStockRepository;

    /**
     * 기준일(asOfDate) 시점의 목표 비중 산출.
     *
     * @param asOfDate               기준일
     * @param assetSymbols           자산 유니버스
     * @param momentumMonths         모멘텀 기간 (개월)
     * @param maWindowDays           이동평균 창 (일)
     * @param topN                   모멘텀 상위 N개만 투자
     * @param volatilityLookbackDays 변동성 계산 lookback (일)
     * @return 목표 비중 및 현금 비중
     */
    public RoboAllocationResult computeTargetWeights(
            LocalDate asOfDate,
            List<String> assetSymbols,
            int momentumMonths,
            int maWindowDays,
            int topN,
            int volatilityLookbackDays) {

        int momentumDays = Math.max(21 * momentumMonths, 21);
        LocalDate fromMomentum = asOfDate.minusDays(momentumDays + maWindowDays);
        LocalDate fromVol = asOfDate.minusDays(volatilityLookbackDays + 1);
        log.debug("로보 할당 엔진(일반) 시작: asOfDate={}, symbols={}, fromMomentum={}, fromVol={}", asOfDate,
                assetSymbols.size(), fromMomentum, fromVol);

        Map<String, BigDecimal> momentumBySymbol = new HashMap<>();
        Map<String, BigDecimal> maBySymbol = new HashMap<>();
        Map<String, BigDecimal> volatilityBySymbol = new HashMap<>();
        Map<String, BigDecimal> closeBySymbol = new HashMap<>();

        for (String symbol : assetSymbols) {
            List<DailyStock> series = dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                    symbol, MARKET_US, fromMomentum, asOfDate);
            if (series.isEmpty()) {
                log.trace("로보 할당 엔진: 심볼 일봉 없음 — symbol={}, asOfDate={}, fromMomentum={} (TB_DAILY_STOCK 해당 구간 데이터 없음)",
                        symbol, asOfDate, fromMomentum);
                continue;
            }
            List<BigDecimal> closes = series.stream()
                    .map(DailyStock::getClosePrice)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (closes.size() < 2) {
                log.trace("로보 할당 엔진: 종가 행 부족 — symbol={}, asOfDate={}, closeCount={} (모멘텀 계산 불가)", symbol, asOfDate,
                        closes.size());
                continue;
            }
            BigDecimal latestClose = closes.get(closes.size() - 1);
            closeBySymbol.put(symbol, latestClose);

            int n = closes.size();
            BigDecimal firstClose = closes.get(0);
            if (firstClose == null || firstClose.compareTo(BigDecimal.ZERO) <= 0) {
                log.trace("로보 할당 엔진: 첫 종가 없음/0 — symbol={}, asOfDate={}", symbol, asOfDate);
                continue;
            }
            BigDecimal momentum = latestClose.subtract(firstClose).divide(firstClose, 6, RoundingMode.HALF_UP);
            momentumBySymbol.put(symbol, momentum);

            int maStart = Math.max(0, n - maWindowDays);
            List<BigDecimal> maCloses = closes.subList(maStart, n);
            BigDecimal ma = maCloses.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(maCloses.size()), 6, RoundingMode.HALF_UP);
            maBySymbol.put(symbol, ma);

            List<DailyStock> volSeries = dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                    symbol, MARKET_US, fromVol, asOfDate);
            if (volSeries.size() >= 2) {
                List<BigDecimal> prices = volSeries.stream()
                        .map(DailyStock::getClosePrice)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                BigDecimal vol = computeDailyReturnStd(prices);
                volatilityBySymbol.put(symbol, vol != null ? vol : BigDecimal.valueOf(0.01));
            } else {
                volatilityBySymbol.put(symbol, BigDecimal.valueOf(0.01));
            }
        }

        List<String> ranked = momentumBySymbol.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        Map<String, BigDecimal> invVol = new LinkedHashMap<>();
        BigDecimal sumInvVol = BigDecimal.ZERO;
        for (String s : ranked) {
            BigDecimal vol = volatilityBySymbol.getOrDefault(s, BigDecimal.ONE);
            if (vol.compareTo(BigDecimal.ZERO) <= 0) {
                vol = BigDecimal.valueOf(0.0001);
            }
            BigDecimal inv = BigDecimal.ONE.divide(vol, 6, RoundingMode.HALF_UP);
            invVol.put(s, inv);
            sumInvVol = sumInvVol.add(inv);
        }
        if (sumInvVol.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("로보 할당 엔진(일반): 전액 현금 반환 — asOfDate={}, momentum유효심볼={}, ranked=0 (모든 심볼에서 일봉/모멘텀 부족)", asOfDate,
                    momentumBySymbol.size());
            return RoboAllocationResult.builder()
                    .asOfDate(asOfDate)
                    .weights(Collections.emptyMap())
                    .cashWeight(BigDecimal.ONE)
                    .build();
        }

        Map<String, BigDecimal> weights = new LinkedHashMap<>();
        BigDecimal cashWeight = BigDecimal.ZERO;
        for (String s : ranked) {
            BigDecimal w = invVol.get(s).divide(sumInvVol, 6, RoundingMode.HALF_UP);
            BigDecimal close = closeBySymbol.get(s);
            BigDecimal ma = maBySymbol.get(s);
            if (close != null && ma != null && close.compareTo(ma) >= 0) {
                weights.put(s, w);
            } else {
                cashWeight = cashWeight.add(w);
            }
        }

        return RoboAllocationResult.builder()
                .asOfDate(asOfDate)
                .weights(weights)
                .cashWeight(cashWeight)
                .build();
    }

    /**
     * 듀얼 모멘텀(노트) 모드: 절대 모멘텀(SPY 12M vs 무위험) 게이트 + 상대 모멘텀(섹터 ETF 6M 상위 2개).
     * SPY 12개월 수익률 &lt; 무위험 수익률이면 전액 현금. 충족 시 섹터 ETF 6개월 수익률 상위 2개만 변동성 역가중 배분.
     */
    public RoboAllocationResult computeTargetWeightsDualMomentumNote(
            LocalDate asOfDate,
            List<String> sectorSymbols,
            int absoluteMomentumMonths,
            BigDecimal riskFreeRatePct,
            String spySymbol,
            String riskFreeSymbol,
            int relativeMomentumMonths,
            int topNSector,
            int maWindowDays,
            int volatilityLookbackDays) {

        int absDays = Math.max(21 * absoluteMomentumMonths, 21);
        LocalDate fromAbs = asOfDate.minusDays(absDays + maWindowDays);
        log.debug("로보 할당 엔진(듀얼모멘텀) 절대 모멘텀: asOfDate={}, spySymbol={}, fromAbs={}", asOfDate, spySymbol, fromAbs);
        List<DailyStock> spySeries = dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                spySymbol, MARKET_US, fromAbs, asOfDate);
        BigDecimal spyReturn = null;
        if (spySeries.size() < 2) {
            log.trace("로보 할당 엔진(듀얼): SPY 일봉 부족 — spySymbol={}, asOfDate={}, fromAbs={}, count={}", spySymbol, asOfDate,
                    fromAbs, spySeries.size());
        } else {
            BigDecimal first = spySeries.get(0).getClosePrice();
            BigDecimal last = spySeries.get(spySeries.size() - 1).getClosePrice();
            if (first != null && last != null && first.compareTo(BigDecimal.ZERO) > 0) {
                spyReturn = last.subtract(first).divide(first, 6, RoundingMode.HALF_UP);
            } else {
                log.trace("로보 할당 엔진(듀얼): SPY 종가 없음 — spySymbol={}, asOfDate={}, first={}, last={}", spySymbol, asOfDate,
                        first, last);
            }
        }
        BigDecimal riskFreeReturn = riskFreeRatePct != null
                ? riskFreeRatePct.movePointLeft(2).multiply(BigDecimal.valueOf(absoluteMomentumMonths))
                        .divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        if (spyReturn == null || spyReturn.compareTo(riskFreeReturn) <= 0) {
            log.debug(
                    "로보 할당 엔진(듀얼모멘텀): 절대 게이트 미충족 — asOfDate={}, spySymbol={}, SPY 12M return={}, riskFree={}, 전액 현금 (SPY 일봉 부족 또는 수익률 <= 무위험)",
                    asOfDate, spySymbol, spyReturn, riskFreeReturn);
            return RoboAllocationResult.builder()
                    .asOfDate(asOfDate)
                    .weights(Collections.emptyMap())
                    .cashWeight(BigDecimal.ONE)
                    .build();
        }

        int relDays = Math.max(21 * relativeMomentumMonths, 21);
        LocalDate fromRel = asOfDate.minusDays(relDays + maWindowDays);
        LocalDate fromVol = asOfDate.minusDays(volatilityLookbackDays + 1);
        log.debug("로보 할당 엔진(듀얼모멘텀) 상대 모멘텀: asOfDate={}, fromRel={}, sectorSymbols={}", asOfDate, fromRel,
                sectorSymbols.size());
        Map<String, BigDecimal> momentumBySymbol = new HashMap<>();
        Map<String, BigDecimal> volatilityBySymbol = new HashMap<>();
        Map<String, BigDecimal> closeBySymbol = new HashMap<>();

        for (String symbol : sectorSymbols) {
            List<DailyStock> series = dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                    symbol, MARKET_US, fromRel, asOfDate);
            if (series.size() < 2) {
                log.trace("로보 할당 엔진(듀얼): 섹터 일봉 부족 — symbol={}, asOfDate={}, fromRel={}, count={}", symbol, asOfDate,
                        fromRel, series.size());
                continue;
            }
            BigDecimal first = series.get(0).getClosePrice();
            BigDecimal last = series.get(series.size() - 1).getClosePrice();
            if (first == null || last == null || first.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            momentumBySymbol.put(symbol, last.subtract(first).divide(first, 6, RoundingMode.HALF_UP));
            closeBySymbol.put(symbol, last);
            List<DailyStock> volSeries = dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                    symbol, MARKET_US, fromVol, asOfDate);
            if (volSeries.size() >= 2) {
                List<BigDecimal> prices = volSeries.stream()
                        .map(DailyStock::getClosePrice)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                BigDecimal vol = computeDailyReturnStd(prices);
                volatilityBySymbol.put(symbol, vol != null ? vol : BigDecimal.valueOf(0.01));
            } else {
                volatilityBySymbol.put(symbol, BigDecimal.valueOf(0.01));
            }
        }

        List<String> ranked = momentumBySymbol.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(topNSector)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (ranked.isEmpty()) {
            log.warn("로보 할당 엔진(듀얼모멘텀): 전액 현금 — asOfDate={}, sectorSymbols={}, 모멘텀 유효 0건 (섹터 일봉 구간 부족)", asOfDate,
                    sectorSymbols.size());
            return RoboAllocationResult.builder()
                    .asOfDate(asOfDate)
                    .weights(Collections.emptyMap())
                    .cashWeight(BigDecimal.ONE)
                    .build();
        }

        BigDecimal sumInvVol = BigDecimal.ZERO;
        Map<String, BigDecimal> invVol = new LinkedHashMap<>();
        for (String s : ranked) {
            BigDecimal vol = volatilityBySymbol.getOrDefault(s, BigDecimal.ONE);
            if (vol.compareTo(BigDecimal.ZERO) <= 0) {
                vol = BigDecimal.valueOf(0.0001);
            }
            BigDecimal inv = BigDecimal.ONE.divide(vol, 6, RoundingMode.HALF_UP);
            invVol.put(s, inv);
            sumInvVol = sumInvVol.add(inv);
        }
        Map<String, BigDecimal> weights = new LinkedHashMap<>();
        for (String s : ranked) {
            weights.put(s, invVol.get(s).divide(sumInvVol, 6, RoundingMode.HALF_UP));
        }
        return RoboAllocationResult.builder()
                .asOfDate(asOfDate)
                .weights(weights)
                .cashWeight(BigDecimal.ZERO)
                .build();
    }

    private BigDecimal computeDailyReturnStd(List<BigDecimal> prices) {
        if (prices == null || prices.size() < 2) {
            return null;
        }
        List<BigDecimal> returns = new ArrayList<>();
        for (int i = 1; i < prices.size(); i++) {
            BigDecimal prev = prices.get(i - 1);
            BigDecimal curr = prices.get(i);
            if (prev != null && curr != null && prev.compareTo(BigDecimal.ZERO) > 0) {
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
        double std = Math.sqrt(Math.max(0, variance.doubleValue()));
        return BigDecimal.valueOf(std);
    }
}
