package com.investment.factor.service;

import com.investment.domain.entity.DailyStock;
import com.investment.domain.entity.Fundamentals;
import com.investment.domain.entity.OrderFlow;
import com.investment.domain.entity.SignalScore;
import com.investment.domain.repository.DailyStockRepository;
import com.investment.domain.repository.FundamentalsRepository;
import com.investment.domain.repository.OrderFlowRepository;
import com.investment.domain.repository.SignalScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

/**
 * 팩터(시그널) 계산 엔진.
 * 이격도(Disparity), 변동성 돌파(Volatility Breakout), 유동성(Liquidity) 산출 후 TB_SIGNAL_SCORE 저장.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FactorCalculationService {

    public static final String FACTOR_DISPARITY = "DISPARITY";
    public static final String FACTOR_VOLATILITY_BREAKOUT = "VOLATILITY_BREAKOUT";
    public static final String FACTOR_LIQUIDITY = "LIQUIDITY";
    public static final String FACTOR_SMART_MONEY_INTENSITY = "SMART_MONEY_INTENSITY";
    public static final String FACTOR_DUAL_MOMENTUM = "DUAL_MOMENTUM";
    public static final String FACTOR_QUALITY_GROWTH = "QUALITY_GROWTH";
    private static final String MARKET_KR = "KR";

    private final DailyStockRepository dailyStockRepository;
    private final SignalScoreRepository signalScoreRepository;
    private final UniverseFilterService universeFilterService;
    private final OrderFlowRepository orderFlowRepository;
    private final FundamentalsRepository fundamentalsRepository;

    /** 이격도 계산용 이동평균 일수 */
    @Value("${investment.factor.disparity-ma-days:20}")
    private int disparityMaDays = 20;

    /** 변동성 돌파 k 계수 (0.5 이하 권장) */
    @Value("${investment.factor.volatility-breakout-k:0.5}")
    private BigDecimal volatilityBreakoutK = new BigDecimal("0.5");

    /** 변동성 돌파 k 동적 적용 여부 (한국장 9:00~10:00 변동성 반영) */
    @Value("${investment.factor.volatility-breakout-k-dynamic:true}")
    private boolean volatilityBreakoutKDynamic = true;

    /** 변동성 돌파 k 동적 조정 범위 (최소 k, 최대 k) */
    @Value("${investment.factor.volatility-breakout-k-min:0.3}")
    private BigDecimal volatilityBreakoutKMin = new BigDecimal("0.3");

    @Value("${investment.factor.volatility-breakout-k-max:0.7}")
    private BigDecimal volatilityBreakoutKMax = new BigDecimal("0.7");

    /** 유동성 최소 거래대금 (원). 5일 평균 또는 일일 기준 */
    @Value("${investment.factor.liquidity-min-trd-val:1000000000}")
    private long liquidityMinTrdVal = 1_000_000_000L;

    /** 미국 듀얼 모멘텀 기간(거래일 수). 쉼표 구분, 최근 1개월 가중치 높게 (예: 21,63,126) */
    @Value("${investment.factor.dual-momentum-period-days:21,63,126}")
    private String dualMomentumPeriodDays = "21,63,126";

    /** 미국 듀얼 모멘텀 기간별 가중치. 쉼표 구분, 합=1 권장 (예: 0.5,0.3,0.2) */
    @Value("${investment.factor.dual-momentum-weights:0.5,0.3,0.2}")
    private String dualMomentumWeights = "0.5,0.3,0.2";

    /** 수급 강도: 순매수/시총 비율 임계값 (0.005 = 0.5% 초과 시 시그널). TB_ORDER_FLOW 수집 후 적용 */
    @Value("${investment.factor.smart-money-intensity-threshold-pct:0.005}")
    private double smartMoneyIntensityThresholdPct = 0.005;

    /**
     * 기준일 시장(KR) 종목에 대해 팩터 계산 후 저장.
     * 매일 장 시작 전 호출 시 basDt는 전일(또는 최근 거래일)로 두고, 해당 일의 일별 데이터가 이미 수집되어 있어야 함.
     *
     * @param basDt 기준일 (일별 데이터가 있는 날)
     * @return 저장된 시그널 건수
     */
    @Transactional
    public int calculateAndSave(LocalDate basDt) {
        return calculateAndSave(basDt, MARKET_KR);
    }

    /**
     * 기준일·시장에 대해 팩터 계산 후 저장.
     *
     * @param basDt   기준일
     * @param market 시장 (KR, US)
     * @return 저장된 시그널 건수
     */
    @Transactional
    public int calculateAndSave(LocalDate basDt, String market) {
        LocalDate fromDt = basDt.minusDays(disparityMaDays + 5);
        List<DailyStock> allInRange = dailyStockRepository.findByMarketAndBasDtBetween(market, fromDt, basDt);
        if (allInRange.isEmpty()) {
            log.debug("팩터 계산: basDt={}, market={}, 일별 데이터 없음", basDt, market);
            return 0;
        }
        List<String> symbols = allInRange.stream()
                .filter(d -> d.getBasDt().equals(basDt))
                .map(DailyStock::getSymbol)
                .distinct()
                .collect(Collectors.toList());
        if (symbols.isEmpty()) {
            log.debug("팩터 계산: basDt={}, market={}, 당일 종목 없음", basDt, market);
            return 0;
        }
        List<String> universeSymbols = universeFilterService.getSymbols(basDt, market);
        if (!universeSymbols.isEmpty()) {
            symbols = symbols.stream().filter(universeSymbols::contains).collect(Collectors.toList());
            log.debug("팩터 계산: 유니버스 필터 적용, basDt={}, market={}, symbols={}", basDt, market, symbols.size());
        }
        if (symbols.isEmpty()) {
            log.debug("팩터 계산: basDt={}, market={}, 유니버스 통과 종목 없음", basDt, market);
            return 0;
        }
        // 미국 듀얼 모멘텀: 시장 모멘텀 = 유니버스 종목들의 가중 수익률 평균 (종목 모멘텀 > 시장 모멘텀 조건용)
        BigDecimal marketMomentumUs = null;
        if ("US".equals(market)) {
            List<BigDecimal> momentums = new ArrayList<>();
            for (String sym : symbols) {
                List<DailyStock> hist = allInRange.stream()
                        .filter(d -> sym.equals(d.getSymbol()))
                        .sorted((a, b) -> a.getBasDt().compareTo(b.getBasDt()))
                        .collect(Collectors.toList());
                BigDecimal mom = computeWeightedMomentum(hist, basDt);
                if (mom != null) {
                    momentums.add(mom);
                }
            }
            if (!momentums.isEmpty()) {
                marketMomentumUs = momentums.stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(momentums.size()), 6, RoundingMode.HALF_UP);
                log.debug("듀얼 모멘텀 시장 평균: basDt={}, symbols={}, marketMomentum={}%", basDt, momentums.size(), marketMomentumUs);
            }
        }
        List<SignalScore> toSave = new ArrayList<>();
        for (String symbol : symbols) {
            List<DailyStock> history = allInRange.stream()
                    .filter(d -> symbol.equals(d.getSymbol()))
                    .sorted((a, b) -> a.getBasDt().compareTo(b.getBasDt()))
                    .collect(Collectors.toList());
            addDisparity(basDt, market, symbol, history, toSave);
            addVolatilityBreakout(basDt, market, symbol, history, toSave);
            addLiquidity(basDt, market, symbol, history, toSave);
            
            // 시장별 추가 팩터
            if (MARKET_KR.equals(market)) {
                addSmartMoneyIntensity(basDt, market, symbol, history, toSave);
            } else if ("US".equals(market)) {
                addDualMomentum(basDt, market, symbol, history, marketMomentumUs, toSave);
                addQualityGrowth(basDt, market, symbol, history, toSave);
            }
        }
        if (!toSave.isEmpty()) {
            signalScoreRepository.saveAll(toSave);
            log.info("팩터 계산 완료: basDt={}, market={}, saved={}", basDt, market, toSave.size());
        }
        return toSave.size();
    }

    private void addDisparity(LocalDate basDt, String market, String symbol, List<DailyStock> history, List<SignalScore> out) {
        if (history.size() < disparityMaDays) {
            return;
        }
        List<DailyStock> forMa = history.stream()
                .filter(d -> !d.getBasDt().isAfter(basDt))
                .sorted((a, b) -> b.getBasDt().compareTo(a.getBasDt()))
                .limit(disparityMaDays)
                .collect(Collectors.toList());
        if (forMa.size() < disparityMaDays) {
            return;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (DailyStock d : forMa) {
            if (d.getClosePrice() != null) {
                sum = sum.add(d.getClosePrice());
            }
        }
        BigDecimal ma = sum.divide(BigDecimal.valueOf(disparityMaDays), 6, RoundingMode.HALF_UP);
        DailyStock latest = forMa.get(0); // basDt 일자 (가장 최근)
        if (latest.getClosePrice() == null || ma.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        BigDecimal disparity = latest.getClosePrice().multiply(BigDecimal.valueOf(100)).divide(ma, 6, RoundingMode.HALF_UP);
        out.add(SignalScore.builder()
                .basDt(basDt)
                .symbol(symbol)
                .factorType(FACTOR_DISPARITY)
                .market(market)
                .score(disparity)
                .metadata("maDays=" + disparityMaDays)
                .build());
    }

    private void addVolatilityBreakout(LocalDate basDt, String market, String symbol, List<DailyStock> history, List<SignalScore> out) {
        if (history.size() < 2) {
            return;
        }
        DailyStock today = history.stream().filter(d -> d.getBasDt().equals(basDt)).findFirst().orElse(null);
        DailyStock prev = history.stream().filter(d -> d.getBasDt().isBefore(basDt)).max((a, b) -> a.getBasDt().compareTo(b.getBasDt())).orElse(null);
        if (today == null || prev == null || today.getOpenPrice() == null || prev.getHighPrice() == null || prev.getLowPrice() == null) {
            return;
        }
        BigDecimal range = prev.getHighPrice().subtract(prev.getLowPrice());
        
        // k 값 동적 적용 (변동성에 따라 조정)
        BigDecimal k = volatilityBreakoutK;
        if (volatilityBreakoutKDynamic && MARKET_KR.equals(market)) {
            k = calculateDynamicK(symbol, history, basDt);
        }
        
        BigDecimal target = today.getOpenPrice().add(range.multiply(k));
        out.add(SignalScore.builder()
                .basDt(basDt)
                .symbol(symbol)
                .factorType(FACTOR_VOLATILITY_BREAKOUT)
                .market(market)
                .score(target)
                .metadata("k=" + k + ",targetPrice,dynamic=" + volatilityBreakoutKDynamic)
                .build());
    }

    /**
     * 변동성 돌파 k 값 동적 계산.
     * 최근 N일의 변동성(평균 Range)을 분석해 높은 변동성일수록 낮은 k, 낮은 변동성일수록 높은 k 사용.
     * 
     * TODO: 한국장 9:00~10:00 시간대별 변동성 분석은 장중 데이터 수집 후 구현 예정.
     * 현재는 일봉 데이터 기반 평균 변동성으로 근사합니다.
     *
     * @param symbol 종목 코드
     * @param history 일별 시세 이력
     * @param basDt 기준일
     * @return 동적 조정된 k 값
     */
    private BigDecimal calculateDynamicK(String symbol, List<DailyStock> history, LocalDate basDt) {
        // 최근 5일의 평균 Range 계산
        List<DailyStock> recent = history.stream()
                .filter(d -> !d.getBasDt().isAfter(basDt))
                .sorted((a, b) -> b.getBasDt().compareTo(a.getBasDt()))
                .limit(5)
                .collect(Collectors.toList());
        
        if (recent.size() < 3) {
            return volatilityBreakoutK; // 데이터 부족 시 기본값
        }

        BigDecimal sumRange = BigDecimal.ZERO;
        int count = 0;
        for (DailyStock d : recent) {
            if (d.getHighPrice() != null && d.getLowPrice() != null) {
                BigDecimal range = d.getHighPrice().subtract(d.getLowPrice());
                if (d.getClosePrice() != null && d.getClosePrice().compareTo(BigDecimal.ZERO) > 0) {
                    // Range를 종가 대비 비율로 정규화
                    BigDecimal rangePct = range.divide(d.getClosePrice(), 6, RoundingMode.HALF_UP);
                    sumRange = sumRange.add(rangePct);
                    count++;
                }
            }
        }

        if (count == 0) {
            return volatilityBreakoutK;
        }

        BigDecimal avgVolatility = sumRange.divide(BigDecimal.valueOf(count), 6, RoundingMode.HALF_UP);
        
        // 변동성에 따라 k 조정: 높은 변동성 → 낮은 k, 낮은 변동성 → 높은 k
        // 변동성 임계값: 평균 0.02(2%) 이상이면 높은 변동성, 0.01(1%) 이하면 낮은 변동성
        BigDecimal highVolThreshold = new BigDecimal("0.02");
        BigDecimal lowVolThreshold = new BigDecimal("0.01");
        
        BigDecimal adjustedK;
        if (avgVolatility.compareTo(highVolThreshold) >= 0) {
            // 높은 변동성: k를 최소값으로
            adjustedK = volatilityBreakoutKMin;
        } else if (avgVolatility.compareTo(lowVolThreshold) <= 0) {
            // 낮은 변동성: k를 최대값으로
            adjustedK = volatilityBreakoutKMax;
        } else {
            // 중간 변동성: 선형 보간
            BigDecimal ratio = avgVolatility.subtract(lowVolThreshold)
                    .divide(highVolThreshold.subtract(lowVolThreshold), 6, RoundingMode.HALF_UP);
            adjustedK = volatilityBreakoutKMax.subtract(
                    volatilityBreakoutKMax.subtract(volatilityBreakoutKMin).multiply(ratio));
        }

        log.debug("변동성 돌파 k 동적 조정: symbol={}, avgVolatility={}, k={} -> {}", 
                symbol, avgVolatility, volatilityBreakoutK, adjustedK);
        
        return adjustedK;
    }

    /**
     * 한국 수급 강도(Smart Money Intensity) 팩터 계산.
     * TB_ORDER_FLOW에 데이터가 있으면 (5일 누적 순매수/시총) 비율(%)을 점수로 저장. 임계값 초과 시 양수 시그널.
     * 데이터가 없으면 0 저장 (fallback).
     *
     * @param basDt 기준일
     * @param market 시장 (KR)
     * @param symbol 종목 코드
     * @param history 일별 시세 이력 (미사용, TB_ORDER_FLOW 사용)
     * @param out 시그널 점수 목록 (추가될)
     */
    private void addSmartMoneyIntensity(LocalDate basDt, String market, String symbol, List<DailyStock> history, List<SignalScore> out) {
        var opt = orderFlowRepository.findByBasDtAndSymbolAndMarket(basDt, symbol, market);
        if (opt.isEmpty()) {
            log.debug("Smart Money Intensity: basDt={}, symbol={}, 수급 데이터 없음", basDt, symbol);
            out.add(SignalScore.builder()
                    .basDt(basDt)
                    .symbol(symbol)
                    .factorType(FACTOR_SMART_MONEY_INTENSITY)
                    .market(market)
                    .score(BigDecimal.ZERO)
                    .metadata("dataUnavailable")
                    .build());
            return;
        }
        OrderFlow of = opt.get();
        if (of.getMarketCap() == null || of.getMarketCap() <= 0 || of.getNetBuyAmt5d() == null) {
            out.add(SignalScore.builder()
                    .basDt(basDt)
                    .symbol(symbol)
                    .factorType(FACTOR_SMART_MONEY_INTENSITY)
                    .market(market)
                    .score(BigDecimal.ZERO)
                    .metadata("invalidData")
                    .build());
            return;
        }
        double ratio = (double) of.getNetBuyAmt5d() / of.getMarketCap();
        BigDecimal scorePct = BigDecimal.valueOf(ratio * 100).setScale(4, RoundingMode.HALF_UP);
        out.add(SignalScore.builder()
                .basDt(basDt)
                .symbol(symbol)
                .factorType(FACTOR_SMART_MONEY_INTENSITY)
                .market(market)
                .score(scorePct)
                .metadata("netBuy5d=" + of.getNetBuyAmt5d() + ",marketCap=" + of.getMarketCap() + ",thresholdPct=" + smartMoneyIntensityThresholdPct * 100)
                .build());
    }

    /**
     * 기간별 수익률 가중합으로 모멘텀 스코어 산출 (TB_DAILY_STOCK 기반).
     * config: dual-momentum-period-days (예: 21,63,126), dual-momentum-weights (예: 0.5,0.3,0.2).
     * 각 기간 수익률 = (close_at_basDt - close_at_basDt_minus_days) / close_at_basDt_minus_days * 100 (%).
     *
     * @param history basDt 포함 과거 일별 시세 (basDt 기준 이전 거래일 포함)
     * @param basDt 기준일
     * @return 가중 모멘텀 (% 단위), 데이터 부족 시 null
     */
    private BigDecimal computeWeightedMomentum(List<DailyStock> history, LocalDate basDt) {
        if (history == null || history.isEmpty()) {
            return null;
        }
        int[] periodDays = parseIntArray(dualMomentumPeriodDays, new int[]{21, 63, 126});
        double[] weights = parseDoubleArray(dualMomentumWeights, new double[]{0.5, 0.3, 0.2});
        if (periodDays.length != weights.length || periodDays.length == 0) {
            return null;
        }
        List<DailyStock> sorted = history.stream()
                .filter(d -> !d.getBasDt().isAfter(basDt))
                .sorted((a, b) -> b.getBasDt().compareTo(a.getBasDt()))
                .collect(Collectors.toList());
        if (sorted.isEmpty()) {
            return null;
        }
        BigDecimal closeNow = getCloseOnOrBefore(sorted, basDt);
        if (closeNow == null || closeNow.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        BigDecimal weightedSum = BigDecimal.ZERO;
        for (int i = 0; i < periodDays.length; i++) {
            LocalDate pastDt = basDt.minusDays(periodDays[i]);
            BigDecimal closePast = getCloseOnOrBefore(sorted, pastDt);
            if (closePast == null || closePast.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            BigDecimal periodReturnPct = closeNow.subtract(closePast)
                    .divide(closePast, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            weightedSum = weightedSum.add(periodReturnPct.multiply(BigDecimal.valueOf(weights[i])));
        }
        return weightedSum.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal getCloseOnOrBefore(List<DailyStock> sortedDesc, LocalDate onOrBefore) {
        return sortedDesc.stream()
                .filter(d -> !d.getBasDt().isAfter(onOrBefore))
                .findFirst()
                .map(DailyStock::getClosePrice)
                .orElse(null);
    }

    private int[] parseIntArray(String raw, int[] defaultVal) {
        if (!StringUtils.hasText(raw)) {
            return defaultVal;
        }
        String[] parts = raw.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                out[i] = Integer.parseInt(parts[i].trim());
            } catch (NumberFormatException e) {
                return defaultVal;
            }
        }
        return out;
    }

    private double[] parseDoubleArray(String raw, double[] defaultVal) {
        if (!StringUtils.hasText(raw)) {
            return defaultVal;
        }
        String[] parts = raw.split(",");
        double[] out = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                out[i] = Double.parseDouble(parts[i].trim());
            } catch (NumberFormatException e) {
                return defaultVal;
            }
        }
        return out;
    }

    /**
     * 미국 듀얼 모멘텀 팩터 계산.
     * 기간별 수익률 가중합으로 모멘텀 스코어 산출, 종목 모멘텀 > 시장 모멘텀 시 양수 점수(매수 시그널).
     * 시장 모멘텀 = 유니버스 종목들의 가중 수익률 평균. score = 종목 모멘텀 − 시장 모멘텀 (%).
     *
     * @param basDt 기준일
     * @param market 시장 (US)
     * @param symbol 종목 코드
     * @param history 일별 시세 이력
     * @param marketMomentumUs 시장 모멘텀 (% 단위), null이면 0으로 간주
     * @param out 시그널 점수 목록 (추가될)
     */
    private void addDualMomentum(LocalDate basDt, String market, String symbol, List<DailyStock> history,
                                 BigDecimal marketMomentumUs, List<SignalScore> out) {
        BigDecimal momentum = computeWeightedMomentum(history, basDt);
        if (momentum == null) {
            log.debug("듀얼 모멘텀: basDt={}, symbol={}, 데이터 부족으로 스킵", basDt, symbol);
            out.add(SignalScore.builder()
                    .basDt(basDt)
                    .symbol(symbol)
                    .factorType(FACTOR_DUAL_MOMENTUM)
                    .market(market)
                    .score(BigDecimal.ZERO)
                    .metadata("insufficientData")
                    .build());
            return;
        }
        BigDecimal marketMom = marketMomentumUs != null ? marketMomentumUs : BigDecimal.ZERO;
        BigDecimal score = momentum.subtract(marketMom).setScale(4, RoundingMode.HALF_UP);
        out.add(SignalScore.builder()
                .basDt(basDt)
                .symbol(symbol)
                .factorType(FACTOR_DUAL_MOMENTUM)
                .market(market)
                .score(score)
                .metadata("momentum=" + momentum + ",marketMom=" + marketMom)
                .build());
    }

    /**
     * 미국 퀄리티-성장(PEG & Rule of 40) 팩터 계산.
     * TB_FUNDAMENTALS에 데이터가 있으면 PEG 점수(낮을수록 좋음) + Rule of 40(매출증가율+영업이익률) 합산 점수 저장.
     * 데이터가 없으면 0 저장 (fallback).
     *
     * @param basDt 기준일
     * @param market 시장 (US)
     * @param symbol 종목 코드
     * @param history 일별 시세 이력 (미사용, TB_FUNDAMENTALS 사용)
     * @param out 시그널 점수 목록 (추가될)
     */
    private void addQualityGrowth(LocalDate basDt, String market, String symbol, List<DailyStock> history, List<SignalScore> out) {
        var opt = fundamentalsRepository.findByBasDtAndSymbolAndMarket(basDt, symbol, market);
        if (opt.isEmpty()) {
            log.debug("퀄리티-성장: basDt={}, symbol={}, 재무 데이터 없음", basDt, symbol);
            out.add(SignalScore.builder()
                    .basDt(basDt)
                    .symbol(symbol)
                    .factorType(FACTOR_QUALITY_GROWTH)
                    .market(market)
                    .score(BigDecimal.ZERO)
                    .metadata("dataUnavailable")
                    .build());
            return;
        }
        Fundamentals f = opt.get();
        BigDecimal pegScore = BigDecimal.ZERO;
        if (f.getPeg() != null && f.getPeg().compareTo(BigDecimal.ZERO) > 0) {
            // PEG 낮을수록 좋음: 2 - peg (peg < 2일 때 양수), 상한 2
            pegScore = BigDecimal.valueOf(2).subtract(f.getPeg()).max(BigDecimal.ZERO).min(new BigDecimal("2"));
        }
        BigDecimal rule40 = BigDecimal.ZERO;
        if (f.getRevenueGrowthPct() != null) {
            rule40 = rule40.add(f.getRevenueGrowthPct());
        }
        if (f.getOperatingMarginPct() != null) {
            rule40 = rule40.add(f.getOperatingMarginPct());
        }
        BigDecimal combined = pegScore.add(rule40).setScale(4, RoundingMode.HALF_UP);
        out.add(SignalScore.builder()
                .basDt(basDt)
                .symbol(symbol)
                .factorType(FACTOR_QUALITY_GROWTH)
                .market(market)
                .score(combined)
                .metadata("pegScore=" + pegScore + ",rule40=" + rule40)
                .build());
    }

    private void addLiquidity(LocalDate basDt, String market, String symbol, List<DailyStock> history, List<SignalScore> out) {
        DailyStock onBasDt = history.stream().filter(d -> d.getBasDt().equals(basDt)).findFirst().orElse(null);
        if (onBasDt == null || onBasDt.getTrdVal() == null) {
            return;
        }
        long trdVal = onBasDt.getTrdVal();
        int pass = trdVal >= liquidityMinTrdVal ? 1 : 0;
        out.add(SignalScore.builder()
                .basDt(basDt)
                .symbol(symbol)
                .factorType(FACTOR_LIQUIDITY)
                .market(market)
                .score(BigDecimal.valueOf(pass))
                .metadata("trdVal=" + trdVal + ",min=" + liquidityMinTrdVal)
                .build());
    }
}
