package com.investment.factor.execution;

import com.investment.domain.entity.DailyStock;
import com.investment.domain.entity.StrategyPosition;
import com.investment.domain.repository.DailyStockRepository;
import com.investment.domain.repository.StrategyPositionRepository;
import com.investment.factor.util.TechnicalIndicatorUtil;
import com.investment.strategy.domain.StrategyType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 4단계 청산 규칙 — 기간별 적용.
 * SHORT_TERM: -3% Trailing Stop. MEDIUM_TERM: -10% 손절. LONG_TERM: 스텁(매도 시그널
 * 없음).
 * (공통) ATR Trailing Stop, Time-Cut은 SHORT_TERM 등에서만 사용.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExitRuleService {

    private static final int ATR_DAYS = 14;

    private final StrategyPositionRepository strategyPositionRepository;
    private final DailyStockRepository dailyStockRepository;
    private final ExitRuleEvaluator exitRuleEvaluator;

    /** ATR Trailing Stop 배수 (기본값 2.0~2.5) */
    @Value("${investment.pipeline.atr-trailing-stop-multiplier:2.0}")
    private BigDecimal atrTrailingStopMultiplier = new BigDecimal("2.0");

    /**
     * 계좌의 보유 포지션 중 청산 대상(매도 시그널) 목록 반환.
     * 현재가·당일 고가를 파라미터로 넘기면 장중 실시간 시세를 사용하고, null이면 DailyStock 종가/고가 사용.
     *
     * @param accountNo            계좌번호
     * @param currentPriceBySymbol 종목별 현재가 (symbol -> price). null이면 DailyStock 종가
     *                             사용
     * @return 청산 대상 목록
     */
    @Transactional
    public List<ExitSignal> getSellSignals(String accountNo, Map<String, BigDecimal> currentPriceBySymbol) {
        return getSellSignals(accountNo, currentPriceBySymbol, null);
    }

    /**
     * 계좌의 보유 포지션 중 청산 대상(매도 시그널) 목록 반환.
     * todayHighBySymbol이 있으면 장중 당일 고가로 trailingHigh 갱신, 없으면 DailyStock 고가 사용.
     *
     * @param accountNo            계좌번호
     * @param currentPriceBySymbol 종목별 현재가 (symbol -> price). null이면 DailyStock 종가
     *                             사용
     * @param todayHighBySymbol    종목별 당일 고가 (symbol -> high). null이면 DailyStock 고가
     *                             사용
     * @return 청산 대상 목록
     */
    @Transactional
    public List<ExitSignal> getSellSignals(String accountNo, Map<String, BigDecimal> currentPriceBySymbol,
            Map<String, BigDecimal> todayHighBySymbol) {
        List<StrategyPosition> openPositions = strategyPositionRepository
                .findByAccountNoAndExitDtIsNullOrderByEntryDtAsc(accountNo);
        List<ExitSignal> signals = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (StrategyPosition pos : openPositions) {
            // 현재가 조회 (파라미터 또는 DailyStock 종가)
            BigDecimal currentPrice = getCurrentPrice(pos, currentPriceBySymbol, today);
            if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                log.debug("청산 평가 스킵: symbol={}, 현재가 없음", pos.getSymbol());
                continue;
            }

            // 당일 고가 조회 및 trailingHigh 갱신 (장중 연동: todayHighBySymbol 우선, 없으면 DailyStock)
            BigDecimal todayHigh = getTodayHighForPosition(pos, today, todayHighBySymbol);
            if (todayHigh != null) {
                pos.updateTrailingHigh(todayHigh);
            }
            // 전저점(priorLow) 갱신: 진입 후 최저가 (한국 KR 전저점 이탈 손절용)
            BigDecimal todayLow = getTodayLowForPosition(pos, today, todayHighBySymbol);
            BigDecimal priorLowCandidate = pos.getPriorLow() != null ? pos.getPriorLow() : pos.getEntryPrice();
            if (currentPrice.compareTo(priorLowCandidate) < 0) {
                pos.updatePriorLow(currentPrice);
            }
            if (todayLow != null && todayLow.compareTo(BigDecimal.ZERO) > 0
                    && todayLow.compareTo(priorLowCandidate) < 0) {
                pos.updatePriorLow(todayLow);
            }
            strategyPositionRepository.save(pos);

            // 한국(KR) 단기/스윙: RSI(14) 계산 (RSI≥70 익절용)
            BigDecimal rsi = null;
            if ("KR".equalsIgnoreCase(pos.getMarket()) && pos.getStrategyType() == StrategyType.SHORT_TERM) {
                rsi = computeRsiForPosition(pos, today);
            }

            ExitRuleInput input = ExitRuleInput.builder()
                    .entryPrice(pos.getEntryPrice())
                    .trailingHigh(pos.getTrailingHigh())
                    .priorLow(pos.getPriorLow())
                    .entryDt(pos.getEntryDt())
                    .strategyType(pos.getStrategyType())
                    .market(pos.getMarket())
                    .currentPrice(currentPrice)
                    .todayHigh(todayHigh)
                    .today(today)
                    .timeCutDays(pos.getTimeCutDays())
                    .targetReturnPct(pos.getTargetReturnPct())
                    .atrMultiplier(pos.getAtrMultiplier())
                    .rsi(rsi)
                    .build();
            ExitRuleResult result = exitRuleEvaluator.evaluate(input);
            if (result.isShouldExit()) {
                log.info("청산 시그널: symbol={}, reason={}, currentPrice={}",
                        pos.getSymbol(), result.getReason(), currentPrice);
                signals.add(ExitSignal.builder()
                        .positionId(pos.getId())
                        .symbol(pos.getSymbol())
                        .market(pos.getMarket())
                        .quantity(pos.getQuantity())
                        .reason(result.getReason())
                        .currentPrice(currentPrice)
                        .build());
            }
        }
        return signals;
    }

    /**
     * 현재가 조회 (파라미터 우선, 없으면 DailyStock 종가 사용)
     */
    private BigDecimal getCurrentPrice(StrategyPosition pos, Map<String, BigDecimal> currentPriceBySymbol,
            LocalDate today) {
        if (currentPriceBySymbol != null && currentPriceBySymbol.containsKey(pos.getSymbol())) {
            return currentPriceBySymbol.get(pos.getSymbol());
        }
        // DailyStock 종가 사용
        List<DailyStock> todayData = dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                pos.getSymbol(), pos.getMarket(), today, today);
        if (!todayData.isEmpty() && todayData.get(0).getClosePrice() != null) {
            return todayData.get(0).getClosePrice();
        }
        // 전일 종가 사용
        List<DailyStock> yesterdayData = dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                pos.getSymbol(), pos.getMarket(), today.minusDays(1), today.minusDays(1));
        if (!yesterdayData.isEmpty() && yesterdayData.get(0).getClosePrice() != null) {
            return yesterdayData.get(0).getClosePrice();
        }
        return null;
    }

    /**
     * 당일 고가 조회. todayHighBySymbol에 값이 있으면 사용(장중 실시간), 없으면 DailyStock 사용.
     */
    private BigDecimal getTodayHighForPosition(StrategyPosition pos, LocalDate today,
            Map<String, BigDecimal> todayHighBySymbol) {
        if (todayHighBySymbol != null && todayHighBySymbol.containsKey(pos.getSymbol())) {
            BigDecimal high = todayHighBySymbol.get(pos.getSymbol());
            if (high != null && high.compareTo(BigDecimal.ZERO) > 0) {
                return high;
            }
        }
        List<DailyStock> todayData = dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                pos.getSymbol(), pos.getMarket(), today, today);
        if (!todayData.isEmpty() && todayData.get(0).getHighPrice() != null) {
            return todayData.get(0).getHighPrice();
        }
        List<DailyStock> yesterdayData = dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                pos.getSymbol(), pos.getMarket(), today.minusDays(1), today.minusDays(1));
        if (!yesterdayData.isEmpty() && yesterdayData.get(0).getHighPrice() != null) {
            return yesterdayData.get(0).getHighPrice();
        }
        return null;
    }

    /**
     * 당일 저가 조회. 전저점 갱신용. (장중 todayLowBySymbol 미지원 시 DailyStock 저가 사용)
     */
    private BigDecimal getTodayLowForPosition(StrategyPosition pos, LocalDate today,
            Map<String, BigDecimal> todayHighBySymbol) {
        List<DailyStock> todayData = dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                pos.getSymbol(), pos.getMarket(), today, today);
        if (!todayData.isEmpty() && todayData.get(0).getLowPrice() != null) {
            return todayData.get(0).getLowPrice();
        }
        return null;
    }

    /**
     * 포지션 종목의 RSI(14) 계산. 진입일~기준일 일봉으로 산출. 한국(KR) RSI≥70 익절용.
     */
    private BigDecimal computeRsiForPosition(StrategyPosition pos, LocalDate today) {
        LocalDate from = pos.getEntryDt().isBefore(today.minusDays(30)) ? today.minusDays(30) : pos.getEntryDt();
        List<DailyStock> history = dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                pos.getSymbol(), pos.getMarket(), from, today);
        return TechnicalIndicatorUtil.computeRsi(history).orElse(null);
    }

    /**
     * ATR Trailing Stop 평가 (기간별 규칙 미사용 시 참고용).
     * trailing_high - current_price >= ATR × multiplier 이면 매도 시그널.
     *
     * @param pos          포지션
     * @param currentPrice 현재가
     * @param today        기준일
     * @return 매도 시그널 (조건 미충족 시 null)
     */
    private ExitSignal evaluateAtrTrailingStop(StrategyPosition pos, BigDecimal currentPrice, LocalDate today) {
        if (pos.getTrailingHigh() == null || pos.getTrailingHigh().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        if (pos.getAtrMultiplier() == null || pos.getAtrMultiplier().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        // ATR 계산
        LocalDate fromDt = today.minusDays(ATR_DAYS + 5);
        List<DailyStock> history = dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                pos.getSymbol(), pos.getMarket(), fromDt, today);
        if (history.size() < 2) {
            log.debug("ATR Trailing Stop 평가 스킵: symbol={}, 데이터 부족", pos.getSymbol());
            return null;
        }

        BigDecimal atr = computeAtr(history);
        if (atr.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        // ATR Trailing Stop 평가: (trailing_high - current_price) >= (ATR × multiplier)
        BigDecimal priceDrop = pos.getTrailingHigh().subtract(currentPrice);
        BigDecimal stopThreshold = atr.multiply(pos.getAtrMultiplier());

        if (priceDrop.compareTo(stopThreshold) >= 0) {
            log.info("ATR Trailing Stop 시그널: symbol={}, trailingHigh={}, currentPrice={}, drop={}, threshold={}",
                    pos.getSymbol(), pos.getTrailingHigh(), currentPrice, priceDrop, stopThreshold);
            return ExitSignal.builder()
                    .positionId(pos.getId())
                    .symbol(pos.getSymbol())
                    .market(pos.getMarket())
                    .quantity(pos.getQuantity())
                    .reason("ATR_TRAILING_STOP")
                    .currentPrice(currentPrice)
                    .build();
        }

        return null;
    }

    /**
     * ATR 계산 (14일, PositionSizingService와 동일 로직)
     */
    private BigDecimal computeAtr(List<DailyStock> history) {
        if (history.size() < 2) {
            return BigDecimal.ZERO;
        }
        List<DailyStock> sorted = history.stream()
                .sorted(Comparator.comparing(DailyStock::getBasDt))
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

    @lombok.Getter
    @lombok.Builder
    @lombok.AllArgsConstructor
    public static class ExitSignal {
        private Long positionId;
        private String symbol;
        private String market;
        private int quantity;
        private String reason;
        private BigDecimal currentPrice;
    }
}
