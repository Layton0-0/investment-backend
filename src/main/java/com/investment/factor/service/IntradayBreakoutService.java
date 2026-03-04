package com.investment.factor.service;

import com.investment.domain.entity.DailyStock;
import com.investment.domain.entity.Universe;
import com.investment.factor.dto.BreakoutCandidateDto;
import com.investment.domain.repository.DailyStockRepository;
import com.investment.domain.repository.UniverseRepository;
import com.investment.marketdata.dto.CurrentPriceDto;
import com.investment.marketdata.service.RealtimeMarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 장중 변동성 돌파 — 당일 시가 + (전일 High−Low)×k 이상인 종목 진입 후보 산출 (P2).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntradayBreakoutService {

    private final UniverseRepository universeRepository;
    private final DailyStockRepository dailyStockRepository;
    private final RealtimeMarketDataService realtimeMarketDataService;
    private final FactorCalculationService factorCalculationService;

    @Value("${investment.factor.volatility-breakout-k:0.5}")
    private BigDecimal volatilityBreakoutK = new BigDecimal("0.5");

    /** 장중 돌파 시 1종목당 투입 비율 (총자산 대비, 예: 0.005 = 0.5%) */
    @Value("${investment.intraday.breakout-position-pct:0.005}")
    private BigDecimal breakoutPositionPct = new BigDecimal("0.005");

    /**
     * 장중 변동성 돌파 충족 종목 후보 반환.
     * 감시 유니버스 = 전일 유니버스, Target = 당일 시가 + (전일 High−Low)×k, 현재가 ≥ Target 시 진입 후보.
     *
     * @param today   당일
     * @param market  시장 (KR)
     * @param capital 투자 가능 자산 (원)
     * @return 진입 후보 목록 (이미 보유 종목 제외는 스케줄러에서 처리)
     */
    public List<BreakoutCandidateDto> getBreakoutCandidates(LocalDate today, String market, BigDecimal capital) {
        if (capital == null || capital.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }
        LocalDate yesterday = today.minusDays(1);
        List<Universe> universe = universeRepository.findByBasDtAndMarketOrderBySymbol(yesterday, market);
        if (universe.isEmpty()) {
            log.debug("장중 돌파: 전일 유니버스 없음, market={}", market);
            return List.of();
        }
        List<String> symbols = universe.stream().map(Universe::getSymbol).distinct().collect(Collectors.toList());

        List<BreakoutCandidateDto> candidates = new ArrayList<>();
        for (String symbol : symbols) {
            List<DailyStock> prevList = dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                    symbol, market, yesterday, yesterday);
            if (prevList.isEmpty()) {
                continue;
            }
            DailyStock prev = prevList.get(0);
            if (prev.getHighPrice() == null || prev.getLowPrice() == null) {
                continue;
            }
            BigDecimal range = prev.getHighPrice().subtract(prev.getLowPrice());
            if (range.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal target = null;
            BigDecimal openPrice = null;
            BigDecimal currentPrice = null;
            try {
                List<CurrentPriceDto> prices = realtimeMarketDataService.getCurrentPrices(List.of(symbol))
                        .blockOptional().orElse(List.of());
                if (prices.isEmpty() || prices.get(0).getCurrentPrice() == null) {
                    continue;
                }
                CurrentPriceDto dto = prices.get(0);
                currentPrice = dto.getCurrentPrice();
                openPrice = dto.getOpenPrice() != null ? dto.getOpenPrice() : prev.getClosePrice();
                if (openPrice == null || openPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                BigDecimal k = factorCalculationService.getVolatilityBreakoutK(symbol, market, today);
                target = openPrice.add(range.multiply(k));
                if (currentPrice.compareTo(target) < 0) {
                    continue;
                }
            } catch (Exception e) {
                log.debug("장중 돌파 시세 조회 스킵: symbol={}, error={}", symbol, e.getMessage());
                continue;
            }
            BigDecimal amt = capital.multiply(breakoutPositionPct).setScale(0, RoundingMode.DOWN);
            if (amt.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            long qty = currentPrice.compareTo(BigDecimal.ZERO) > 0
                    ? amt.divide(currentPrice, 0, RoundingMode.DOWN).longValue()
                    : 0;
            if (qty <= 0) {
                continue;
            }
            candidates.add(BreakoutCandidateDto.builder()
                    .symbol(symbol)
                    .market(market)
                    .currentPrice(currentPrice)
                    .targetPrice(target)
                    .recommendedQty(qty)
                    .build());
        }
        log.debug("장중 돌파 후보: today={}, market={}, count={}", today, market, candidates.size());
        return candidates;
    }
}
