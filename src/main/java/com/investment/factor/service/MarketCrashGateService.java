package com.investment.factor.service;

import com.investment.config.RiskProperties;
import com.investment.domain.entity.DailyStock;
import com.investment.domain.repository.DailyStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * 시장 급락 게이트 — 벤치마크 지수(예: SPY) 전일 대비 일일 낙폭이 임계값 이상이면 당일 신규 매수 중단.
 * 설계 원칙 "시장 급락 -5% 시 현금화"의 최소 구현(당일 매수 중단만 적용).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketCrashGateService {

    private final RiskProperties riskProperties;
    private final DailyStockRepository dailyStockRepository;

    /**
     * 시장 급락 게이트 통과 여부. 벤치마크 전일 일일 수익률이 -threshold% 이하이면 신규 매수 불가.
     * 데이터 부재·예외 시 허용(fail-open).
     *
     * @return true면 신규 매수 허용, false면 시장 급락으로 스킵
     */
    public boolean isNewBuyAllowed() {
        if (!riskProperties.isMarketCrashGateEnabled()) {
            return true;
        }
        BigDecimal thresholdPct = riskProperties.getMarketCrashDailyDropPct();
        if (thresholdPct == null || thresholdPct.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        String symbol = riskProperties.getMarketCrashBenchmarkSymbol();
        String market = riskProperties.getMarketCrashBenchmarkMarket();
        if (symbol == null || symbol.isBlank()) {
            return true;
        }
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate dayBefore = yesterday.minusDays(1);
        List<DailyStock> series = dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                symbol, market != null ? market : "US", dayBefore, yesterday);
        if (series == null || series.size() < 2) {
            log.debug("시장 급락 게이트: 벤치마크 데이터 부족 symbol={}, market={}, 건수={}",
                    symbol, market, series != null ? series.size() : 0);
            return true;
        }
        DailyStock prev = series.get(0);
        DailyStock curr = series.get(1);
        BigDecimal prevClose = prev.getClosePrice();
        BigDecimal currClose = curr.getClosePrice();
        if (prevClose == null || currClose == null || prevClose.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("시장 급락 게이트: 종가 없음 symbol={}", symbol);
            return true;
        }
        BigDecimal dailyReturnPct = currClose.subtract(prevClose)
                .divide(prevClose, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        if (dailyReturnPct.compareTo(thresholdPct.negate()) <= 0) {
            log.warn("시장 급락 게이트: 신규 매수 중단 symbol={}, market={}, 전일수익률={}%, 임계값={}%",
                    symbol, market, dailyReturnPct, thresholdPct);
            return false;
        }
        return true;
    }
}
