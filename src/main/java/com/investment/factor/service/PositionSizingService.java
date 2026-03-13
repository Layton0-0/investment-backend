package com.investment.factor.service;

import com.investment.core.engine.portfolio.InverseVolatilityPortfolioService;
import com.investment.domain.entity.DailyStock;
import com.investment.domain.entity.SignalScore;
import com.investment.domain.repository.DailyStockRepository;
import com.investment.domain.repository.SignalScoreRepository;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.risk.service.RiskReportService;
import com.investment.news.service.NewsSignalService;
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
    private final NewsSignalService newsSignalService;
    private final FrictionCostService frictionCostService;
    private final CorrelationPenaltyService correlationPenaltyService;
    private final InverseVolatilityPortfolioService inverseVolatilityPortfolioService;
    private final TradingSettingRepository tradingSettingRepository;
    private final RiskReportService riskReportService;
    private final RiskGateService riskGateService;

    /** 1회 매매당 총자산 대비 리스크 비율 (예: 0.01 = 1%) */
    @Value("${investment.factor.position-risk-pct:0.01}")
    private BigDecimal positionRiskPct = new BigDecimal("0.01");

    /** Half-Kelly 승률 (기본값 0.6 = 60%). 전략별 미설정 시 사용 */
    @Value("${investment.factor.kelly-p:0.6}")
    private BigDecimal kellyP = new BigDecimal("0.6");

    /** Half-Kelly 손익비 (기본값 2.0 = 2:1). 전략별 미설정 시 사용 */
    @Value("${investment.factor.kelly-b:2.0}")
    private BigDecimal kellyB = new BigDecimal("2.0");

    /**
     * 한국(KR) Hunter: 수급(Smart Money) 임계값 초과 시 모멘텀 경로. 점수는 비율×100 저장됨 (0.5% = 0.5)
     */
    @Value("${investment.factor.smart-money-intensity-threshold-pct:0.005}")
    private double smartMoneyIntensityThresholdPct = 0.005;

    /** 시초가/변동성 돌파 시 최소 거래대금(원). KR 단기 09:00~10:00 슬리피지 방어용 (기본 300억) */
    @Value("${investment.factor.liquidity-min-trd-val-opening:30000000000}")
    private long liquidityMinTrdValOpening = 30_000_000_000L;

    /** 켈리 비활성 시 사용. true면 Half-Kelly, false면 고정 비율만 */
    @Value("${investment.factor.kelly-enabled:false}")
    private boolean kellyEnabled = false;

    /** 켈리 비활성 시 1종목당 자산 대비 최대 비율 (%) */
    @Value("${investment.factor.kelly-fixed-allocation-pct:2}")
    private BigDecimal kellyFixedAllocationPct = new BigDecimal("2");

    /** 미국(US): 갭 상승 N% 이상 시 진입 스킵 (기본 5) */
    @Value("${investment.factor.us-gap-up-skip-pct:5}")
    private BigDecimal usGapUpSkipPct = new BigDecimal("5");

    /** KR 단기: 볼륨 스파이크+돌파 조합 시 유니버스에 변동성 돌파(VOLATILITY_BREAKOUT) 시그널 있는 종목만 포함. true 시 Case A/B 결과와 교집합 */
    @Value("${investment.factor.kr-short-term-breakout-required:false}")
    private boolean krShortTermBreakoutRequired = false;

    /**
     * 전략별 Half-Kelly p·b (백테스트 winRate·profitFactor 연동용). 빈 문자열이면 기본
     * kelly-p/kelly-b 사용
     */
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

    /** 종목당 최대 비중 (%, 0이면 미적용) */
    @Value("${investment.factor.position-cap-per-symbol-pct:10}")
    private BigDecimal positionCapPerSymbolPct = new BigDecimal("10");

    /** 리스크 기반 포지션 사이징: 종목당 비중 상한 적용 여부 (기본 false) */
    @Value("${investment.factor.risk-based-cap-enabled:false}")
    private boolean riskBasedCapEnabled = false;

    /** 리스크 기반 캡 적용 시 종목당 최대 비중 (0.05 = 5%) */
    @Value("${investment.factor.risk-based-cap-max-pct:0.05}")
    private BigDecimal riskBasedCapMaxPct = new BigDecimal("0.05");

    /** 일일 최대 신규 매수 종목 수 (0이면 미적용) */
    @Value("${investment.factor.max-new-positions-per-day:10}")
    private int maxNewPositionsPerDay = 10;

    /** 포트폴리오 모드: inverse-volatility 시 InverseVolatilityPortfolioService 사용, 그 외 기존 내부 역변동성 로직 */
    @Value("${investment.portfolio.mode:stub}")
    private String portfolioMode = "stub";

    /**
     * 기준일·시장에 대한 포지션 권장 목록 산출 (기본 SHORT_TERM).
     */
    public List<PositionRecommendationDto> getRecommendations(LocalDate basDt, String market, BigDecimal totalCapital) {
        return getRecommendations(basDt, market, StrategyType.SHORT_TERM, totalCapital, null);
    }

    /**
     * 기준일·시장·기간별 포지션 권장 목록 산출 (드로다운 회복 미적용).
     */
    public List<PositionRecommendationDto> getRecommendations(LocalDate basDt, String market,
            StrategyType strategyType, BigDecimal totalCapital) {
        return getRecommendations(basDt, market, strategyType, totalCapital, null);
    }

    /**
     * 기준일·시장·기간별 포지션 권장 목록 산출.
     * accountNo가 있으면 해당 사용자 MDD로 드로다운 회복 모드 적용 시 권장 금액 스케일(P6-1).
     *
     * @param basDt        기준일
     * @param market       시장 (KR, US)
     * @param strategyType 기간 (SHORT_TERM, MEDIUM_TERM, LONG_TERM)
     * @param totalCapital 총 투자 가능 자산 (원)
     * @param accountNo    계좌번호 (null이면 드로다운 회복 미적용)
     * @return 권장 포지션 목록
     */
    public List<PositionRecommendationDto> getRecommendations(LocalDate basDt, String market,
            StrategyType strategyType, BigDecimal totalCapital, String accountNo) {
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
        // KR 단기: 시초가/변동성 돌파 시 유동성 opening 임계 적용 (슬리피지 방어)
        if ("KR".equalsIgnoreCase(market) && strategyType == StrategyType.SHORT_TERM) {
            List<DailyStock> openingLiquidity = dailyStockRepository.findByBasDtAndMarketAndTrdValGreaterThanEqual(
                    basDt, market, liquidityMinTrdValOpening);
            Set<String> openingSymbols = openingLiquidity.stream().map(DailyStock::getSymbol)
                    .collect(Collectors.toSet());
            symbols = symbols.stream().filter(openingSymbols::contains).collect(Collectors.toSet());
            if (symbols.isEmpty()) {
                return List.of();
            }
        }
        // 미국(US): 갭 상승 N% 이상 시 진입 스킵 (Look-ahead 완화)
        if ("US".equalsIgnoreCase(market) && usGapUpSkipPct != null && usGapUpSkipPct.compareTo(BigDecimal.ZERO) > 0) {
            symbols = filterByUsGapUpSkip(symbols, basDt, market);
            if (symbols.isEmpty()) {
                return List.of();
            }
        }
        List<String> symbolList = new ArrayList<>(symbols);
        Map<String, BigDecimal> signalScores = newsSignalService.getSymbolScoresWithSignalNews(market, basDt);
        if (!signalScores.isEmpty()) {
            symbolList.sort((a, b) -> {
                BigDecimal scoreA = signalScores.getOrDefault(a, BigDecimal.ZERO);
                BigDecimal scoreB = signalScores.getOrDefault(b, BigDecimal.ZERO);
                return scoreB.compareTo(scoreA);
            });
        }

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
            // 수수료·세금 반영: 2:1 R:R 가정 시 기대 gross 수익률이 왕복 비용률을 상회할 때만 진입
            BigDecimal roundTripRate = frictionCostService.getRoundTripCostRate(market);
            BigDecimal minGrossReturn2to1 = riskPerShare.multiply(BigDecimal.valueOf(2)).divide(entry, 6, RoundingMode.HALF_UP);
            if (minGrossReturn2to1.compareTo(roundTripRate) <= 0) {
                continue;
            }
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
            // 변동성 역가중: investment.portfolio.mode=inverse-volatility 이면 InverseVolatilityPortfolioService 사용
            if ("inverse-volatility".equalsIgnoreCase(portfolioMode)) {
                out = inverseVolatilityPortfolioService.applyInverseVolatilityWeights(out, totalCapital, basDt, market);
            } else {
                out = applyInverseVolatilityWeighting(out, totalCapital, fromDt, market);
            }
            // 리스크 기반 포지션 사이징: 종목당 비중 상한 (설정 시)
            if (riskBasedCapEnabled && riskBasedCapMaxPct != null && riskBasedCapMaxPct.compareTo(BigDecimal.ZERO) > 0) {
                out = applyRiskBasedCap(out, totalCapital);
            }
            // 단일 섹터 비중 30% 상한 (P2-3). TB_SYMBOL_SECTOR 기반, 초과분 비례 축소
            out = correlationPenaltyService.applySectorConcentrationLimit(out, totalCapital, market);
            // 포트폴리오 상관관계 패널티: 고상관 쌍 시 비중 스케일 다운 (설정 시)
            out = correlationPenaltyService.applyPenalty(out, totalCapital, market, basDt);
            // 일일 최대 신규 매수 종목 수 상한
            if (maxNewPositionsPerDay > 0 && out.size() > maxNewPositionsPerDay) {
                out = new ArrayList<>(out.subList(0, maxNewPositionsPerDay));
            }
            // 종목당 최대 비중 상한
            if (positionCapPerSymbolPct != null && positionCapPerSymbolPct.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal capAmt = totalCapital.multiply(positionCapPerSymbolPct)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN);
                List<PositionRecommendationDto> capped = new ArrayList<>();
                for (PositionRecommendationDto rec : out) {
                    BigDecimal amt = rec.getRecommendedAmt().min(capAmt);
                    if (rec.getEntryPrice() == null || rec.getEntryPrice().compareTo(BigDecimal.ZERO) <= 0) {
                        capped.add(rec);
                        continue;
                    }
                    long qty = amt.divide(rec.getEntryPrice(), 0, RoundingMode.DOWN).longValue();
                    if (qty <= 0) {
                        continue;
                    }
                    capped.add(PositionRecommendationDto.builder()
                            .basDt(rec.getBasDt())
                            .symbol(rec.getSymbol())
                            .market(rec.getMarket())
                            .recommendedAmt(amt)
                            .recommendedQty(qty)
                            .method(rec.getMethod())
                            .entryPrice(rec.getEntryPrice())
                            .stopLoss(rec.getStopLoss())
                            .build());
                }
                out = capped;
            }
            // P6-1 드로다운 회복 모드: MDD -10% 초과 시 신규 매수 권장 금액 50% 스케일
            if (accountNo != null && !accountNo.isBlank()) {
                String userId = tradingSettingRepository.findByAccountNo(accountNo)
                        .map(com.investment.domain.entity.TradingSetting::getUserId)
                        .orElse(null);
                if (userId != null) {
                    BigDecimal maxMdd = riskReportService.getMaxMddPctForUser(userId);
                    if (riskGateService.isDrawdownRecoveryMode(maxMdd)) {
                        BigDecimal scale = riskGateService.getDrawdownRecoveryScale();
                        if (scale != null && scale.compareTo(BigDecimal.ONE) < 0) {
                            log.info("드로다운 회복 모드: userId={}, maxMdd={}, scale={} 적용", userId, maxMdd, scale);
                            List<PositionRecommendationDto> scaled = new ArrayList<>();
                            for (PositionRecommendationDto rec : out) {
                                BigDecimal amt = rec.getRecommendedAmt().multiply(scale).setScale(0, RoundingMode.DOWN);
                                if (rec.getEntryPrice() == null || rec.getEntryPrice().compareTo(BigDecimal.ZERO) <= 0) {
                                    scaled.add(rec);
                                    continue;
                                }
                                long qty = amt.divide(rec.getEntryPrice(), 0, RoundingMode.DOWN).longValue();
                                if (qty <= 0) continue;
                                scaled.add(PositionRecommendationDto.builder()
                                        .basDt(rec.getBasDt())
                                        .symbol(rec.getSymbol())
                                        .market(rec.getMarket())
                                        .recommendedAmt(amt)
                                        .recommendedQty(qty)
                                        .method(rec.getMethod())
                                        .entryPrice(rec.getEntryPrice())
                                        .stopLoss(rec.getStopLoss())
                                        .build());
                            }
                            out = scaled;
                        }
                    }
                }
            }
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
        if (strategyType == null)
            return null;
        return switch (strategyType) {
            case SHORT_TERM -> kellyPShortTerm;
            case MEDIUM_TERM -> kellyPMediumTerm;
            case LONG_TERM -> kellyPLongTerm;
        };
    }

    private String resolveKellyBString(StrategyType strategyType) {
        if (strategyType == null)
            return null;
        return switch (strategyType) {
            case SHORT_TERM -> kellyBShortTerm;
            case MEDIUM_TERM -> kellyBMediumTerm;
            case LONG_TERM -> kellyBLongTerm;
        };
    }

    /** 한국(KR) Hunter 시그널 팩터 타입 상수 (FactorCalculationService와 동일) */
    private static final String FACTOR_SMART_MONEY_INTENSITY = "SMART_MONEY_INTENSITY";
    private static final String FACTOR_CONTRARIAN_RSI = "CONTRARIAN_RSI";
    private static final String FACTOR_VOLATILITY_BREAKOUT = "VOLATILITY_BREAKOUT";

    /**
     * 기간별 시그널 필터: 통과한 종목 심볼만 반환.
     * 한국(KR) SHORT_TERM: Case A(모멘텀) ∪ Case B(역발상) 분기 적용.
     */
    private Set<String> filterSymbolsByStrategyType(List<SignalScore> signals, LocalDate basDt, String market,
            StrategyType strategyType) {
        Set<String> allSymbols = signals.stream().map(SignalScore::getSymbol).collect(Collectors.toSet());
        if (strategyType == null || strategyType == StrategyType.LONG_TERM) {
            return allSymbols;
        }
        if (strategyType == StrategyType.SHORT_TERM) {
            if ("KR".equalsIgnoreCase(market)) {
                return filterSymbolsKrShortTerm(signals, basDt, market);
            }
            LocalDate fromDt = basDt.minusDays(30);
            return allSymbols.stream()
                    .filter(symbol -> {
                        List<DailyStock> history = dailyStockRepository
                                .findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
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
     * 한국(KR) 단기: Hunter 분기 — Case A(모멘텀) ∪ Case B(역발상).
     * Case A: 수급 강함(Smart Money 임계 초과) → RSI&gt;60 &amp; MACD 필터.
     * Case B: 역발상(RSI&lt;40, CONTRARIAN_RSI 점수 양수). P/B 데이터 없으면 RSI만 적용.
     */
    private Set<String> filterSymbolsKrShortTerm(List<SignalScore> signals, LocalDate basDt, String market) {
        BigDecimal smartMoneyThreshold = BigDecimal.valueOf(smartMoneyIntensityThresholdPct * 100); // 0.5% → 0.5
        Set<String> caseASymbols = new java.util.HashSet<>();
        Set<String> caseBSymbols = new java.util.HashSet<>();

        Map<String, BigDecimal> smartMoneyBySymbol = signals.stream()
                .filter(s -> FACTOR_SMART_MONEY_INTENSITY.equals(s.getFactorType()) && s.getScore() != null)
                .collect(Collectors.toMap(SignalScore::getSymbol, SignalScore::getScore, (a, b) -> a.max(b)));
        Map<String, BigDecimal> contrarianRsiBySymbol = signals.stream()
                .filter(s -> FACTOR_CONTRARIAN_RSI.equals(s.getFactorType()) && s.getScore() != null)
                .collect(Collectors.toMap(SignalScore::getSymbol, SignalScore::getScore, (a, b) -> a.max(b)));

        // Case A: 수급 임계 초과 종목에 대해 모멘텀 필터(RSI>60 & MACD)
        for (Map.Entry<String, BigDecimal> e : smartMoneyBySymbol.entrySet()) {
            if (e.getValue().compareTo(smartMoneyThreshold) > 0) {
                caseASymbols.add(e.getKey());
            }
        }
        LocalDate fromDt = basDt.minusDays(30);
        Set<String> momentumPass = new java.util.HashSet<>();
        for (String symbol : caseASymbols) {
            List<DailyStock> history = dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                    symbol, market, fromDt, basDt);
            if (history.size() >= 20) {
                boolean rsiOk = TechnicalIndicatorUtil.isRsiAbove(history, 14, new BigDecimal("60"));
                boolean macdOk = TechnicalIndicatorUtil.isMacdAboveSignal(history);
                if (rsiOk && macdOk) {
                    momentumPass.add(symbol);
                }
            }
        }

        // Case B: 역발상(RSI<40) — CONTRARIAN_RSI 점수 양수. P/B는 데이터 확보 후 적용.
        for (Map.Entry<String, BigDecimal> e : contrarianRsiBySymbol.entrySet()) {
            if (e.getValue().compareTo(BigDecimal.ZERO) > 0) {
                caseBSymbols.add(e.getKey());
            }
        }

        Set<String> union = new java.util.HashSet<>(momentumPass);
        union.addAll(caseBSymbols);

        // 볼륨 스파이크+돌파 조합: 변동성 돌파 시그널이 있는 종목만 단기 유니버스에 포함 (교집합)
        if (krShortTermBreakoutRequired) {
            Set<String> breakoutSymbols = signals.stream()
                    .filter(s -> FACTOR_VOLATILITY_BREAKOUT.equals(s.getFactorType()))
                    .map(SignalScore::getSymbol)
                    .collect(Collectors.toSet());
            union = union.stream().filter(breakoutSymbols::contains).collect(Collectors.toSet());
        }
        return union;
    }

    /**
     * 미국(US): 전일 종가 대비 갭 상승 N% 이상 종목 제외 (진입 스킵).
     */
    private Set<String> filterByUsGapUpSkip(Set<String> symbols, LocalDate basDt, String market) {
        if (symbols.isEmpty() || usGapUpSkipPct == null || usGapUpSkipPct.compareTo(BigDecimal.ZERO) <= 0) {
            return symbols;
        }
        LocalDate prev = basDt.minusDays(1);
        return symbols.stream()
                .filter(symbol -> {
                    List<DailyStock> two = dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                            symbol, market, prev, basDt);
                    if (two.size() < 2) {
                        return true; // 데이터 부족 시 포함
                    }
                    BigDecimal prevClose = two.get(0).getClosePrice();
                    BigDecimal todayClose = two.get(two.size() - 1).getClosePrice();
                    if (prevClose == null || todayClose == null || prevClose.compareTo(BigDecimal.ZERO) <= 0) {
                        return true;
                    }
                    BigDecimal gapPct = todayClose.subtract(prevClose).divide(prevClose, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                    return gapPct.compareTo(usGapUpSkipPct) < 0; // 갭 < N% 만 포함
                })
                .collect(Collectors.toSet());
    }

    /**
     * Half-Kelly 공식으로 포지션 사이징 조정.
     * 켈리 공식: f* = (bp - q) / b. p·b는 전략별 설정(백테스트 winRate·profitFactor 연동) 또는 기본값
     * 사용.
     * Half-Kelly: f*의 50%만 사용하여 파산 위험 방지.
     *
     * @param recommendations 기존 권장 포지션 목록
     * @param totalCapital    총 투자 가능 자산
     * @param strategyType    기간별 전략 (전략별 p·b 적용)
     * @return Half-Kelly 조정된 포지션 목록
     */
    private List<PositionRecommendationDto> applyHalfKelly(
            List<PositionRecommendationDto> recommendations, BigDecimal totalCapital, StrategyType strategyType) {
        if (recommendations.isEmpty()) {
            return recommendations;
        }

        // 초기 운용: 켈리 비활성 시 고정 자산 비율만 적용 (kelly-enabled=false)
        if (!kellyEnabled && kellyFixedAllocationPct != null
                && kellyFixedAllocationPct.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal fixedAmt = totalCapital.multiply(kellyFixedAllocationPct.movePointLeft(2));
            return recommendations.stream()
                    .map(dto -> {
                        if (dto.getEntryPrice().compareTo(BigDecimal.ZERO) <= 0) {
                            return dto;
                        }
                        BigDecimal capAmt = dto.getRecommendedAmt().min(fixedAmt);
                        long qty = capAmt.divide(dto.getEntryPrice(), 0, RoundingMode.DOWN).longValue();
                        if (qty <= 0) {
                            return dto;
                        }
                        return PositionRecommendationDto.builder()
                                .basDt(dto.getBasDt())
                                .symbol(dto.getSymbol())
                                .market(dto.getMarket())
                                .recommendedAmt(dto.getEntryPrice().multiply(BigDecimal.valueOf(qty)))
                                .recommendedQty(qty)
                                .method(METHOD_ATR + "+FIXED")
                                .entryPrice(dto.getEntryPrice())
                                .stopLoss(dto.getStopLoss())
                                .build();
                    })
                    .collect(Collectors.toList());
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
            if (tr2.compareTo(tr) > 0)
                tr = tr2;
            if (tr3.compareTo(tr) > 0)
                tr = tr3;
            sum = sum.add(tr);
        }
        int count = sorted.size() - start;
        return count > 0 ? sum.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    /**
     * 리스크 기반 포지션 사이징: 종목당 비중 상한(cap) 적용.
     * 설정된 risk-based-cap-max-pct(예: 5%)를 초과하는 권장 금액을 캡한다.
     *
     * @param recommendations Half-Kelly·변동성 역가중 적용 후 목록
     * @param totalCapital    총 투자 가능 자산
     * @return 캡 적용된 포지션 목록
     */
    private List<PositionRecommendationDto> applyRiskBasedCap(
            List<PositionRecommendationDto> recommendations, BigDecimal totalCapital) {
        if (recommendations.isEmpty() || totalCapital == null || totalCapital.compareTo(BigDecimal.ZERO) <= 0
                || riskBasedCapMaxPct == null || riskBasedCapMaxPct.compareTo(BigDecimal.ZERO) <= 0) {
            return recommendations;
        }
        BigDecimal capAmt = totalCapital.multiply(riskBasedCapMaxPct).setScale(0, RoundingMode.DOWN);
        List<PositionRecommendationDto> capped = new ArrayList<>();
        for (PositionRecommendationDto rec : recommendations) {
            BigDecimal amt = rec.getRecommendedAmt().min(capAmt);
            if (rec.getEntryPrice() == null || rec.getEntryPrice().compareTo(BigDecimal.ZERO) <= 0) {
                capped.add(rec);
                continue;
            }
            long qty = amt.divide(rec.getEntryPrice(), 0, RoundingMode.DOWN).longValue();
            if (qty <= 0) {
                continue;
            }
            capped.add(PositionRecommendationDto.builder()
                    .basDt(rec.getBasDt())
                    .symbol(rec.getSymbol())
                    .market(rec.getMarket())
                    .recommendedAmt(rec.getEntryPrice().multiply(BigDecimal.valueOf(qty)))
                    .recommendedQty(qty)
                    .method(rec.getMethod())
                    .entryPrice(rec.getEntryPrice())
                    .stopLoss(rec.getStopLoss())
                    .build());
        }
        return capped;
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
                    BigDecimal weight = BigDecimal.ONE.divide(sigma, 6, RoundingMode.HALF_UP).divide(weightSum, 6,
                            RoundingMode.HALF_UP);
                    BigDecimal cappedAmt = totalCapital.multiply(weight).min(maxAllocation);
                    if (dto.getEntryPrice().compareTo(BigDecimal.ZERO) <= 0)
                        return dto;
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
        if (history.size() < 2)
            return BigDecimal.ZERO;
        List<DailyStock> sorted = history.stream()
                .sorted((a, b) -> a.getBasDt().compareTo(b.getBasDt()))
                .collect(Collectors.toList());
        List<BigDecimal> returns = new ArrayList<>();
        for (int i = 1; i < sorted.size(); i++) {
            BigDecimal prev = sorted.get(i - 1).getClosePrice();
            BigDecimal curr = sorted.get(i).getClosePrice();
            if (prev == null || curr == null || prev.compareTo(BigDecimal.ZERO) == 0)
                continue;
            returns.add(curr.subtract(prev).divide(prev, 6, RoundingMode.HALF_UP));
        }
        if (returns.isEmpty())
            return BigDecimal.ZERO;
        BigDecimal mean = returns.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 6, RoundingMode.HALF_UP);
        BigDecimal variance = returns.stream()
                .map(r -> r.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 6, RoundingMode.HALF_UP);
        double std = Math.sqrt(variance.doubleValue());
        return BigDecimal.valueOf(std);
    }
}
