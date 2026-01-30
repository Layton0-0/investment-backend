package com.investment.factor.service;

import com.investment.domain.entity.DailyStock;
import com.investment.domain.entity.SignalScore;
import com.investment.domain.repository.DailyStockRepository;
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
    private static final String MARKET_KR = "KR";

    private final DailyStockRepository dailyStockRepository;
    private final SignalScoreRepository signalScoreRepository;
    private final UniverseFilterService universeFilterService;

    /** 이격도 계산용 이동평균 일수 */
    @Value("${investment.factor.disparity-ma-days:20}")
    private int disparityMaDays = 20;

    /** 변동성 돌파 k 계수 (0.5 이하 권장) */
    @Value("${investment.factor.volatility-breakout-k:0.5}")
    private BigDecimal volatilityBreakoutK = new BigDecimal("0.5");

    /** 유동성 최소 거래대금 (원). 5일 평균 또는 일일 기준 */
    @Value("${investment.factor.liquidity-min-trd-val:1000000000}")
    private long liquidityMinTrdVal = 1_000_000_000L;

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
        List<SignalScore> toSave = new ArrayList<>();
        for (String symbol : symbols) {
            List<DailyStock> history = allInRange.stream()
                    .filter(d -> symbol.equals(d.getSymbol()))
                    .sorted((a, b) -> a.getBasDt().compareTo(b.getBasDt()))
                    .collect(Collectors.toList());
            addDisparity(basDt, market, symbol, history, toSave);
            addVolatilityBreakout(basDt, market, symbol, history, toSave);
            addLiquidity(basDt, market, symbol, history, toSave);
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
        BigDecimal target = today.getOpenPrice().add(range.multiply(volatilityBreakoutK));
        out.add(SignalScore.builder()
                .basDt(basDt)
                .symbol(symbol)
                .factorType(FACTOR_VOLATILITY_BREAKOUT)
                .market(market)
                .score(target)
                .metadata("k=" + volatilityBreakoutK + ",targetPrice")
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
