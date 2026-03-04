package com.investment.factor.scheduler;

import com.investment.domain.entity.StrategyPosition;
import com.investment.domain.repository.StrategyPositionRepository;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.factor.service.MediumTermMomentumService;
import com.investment.marketdata.dto.CurrentPriceDto;
import com.investment.marketdata.service.RealtimeMarketDataService;
import com.investment.order.dto.OrderRequestDto;
import com.investment.order.service.OrderService;
import com.investment.strategy.domain.StrategyType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 중기(MEDIUM_TERM) 월 1회 리밸런싱. 모멘텀 순위 재계산 후 하락 종목 EXIT, 상위 10% ENTRY 시그널 생성.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MediumTermRebalanceScheduler {

    private static final String EXIT_REASON_MOMENTUM_RANK_DROP = "MOMENTUM_RANK_DROP";

    private final StrategyPositionRepository strategyPositionRepository;
    private final TradingSettingRepository tradingSettingRepository;
    private final MediumTermMomentumService mediumTermMomentumService;
    private final RealtimeMarketDataService realtimeMarketDataService;
    private final OrderService orderService;

    /**
     * 매월 1일 08:30 KST 실행. MEDIUM_TERM 보유 포지션에 대해 모멘텀 순위 재계산 후 순위 하락 종목 매도·포지션 종료.
     */
    @Transactional
    public void runMonthlyRebalance() {
        List<String> accountNos = strategyPositionRepository.findDistinctAccountNosWithOpenPositions();
        LocalDate today = LocalDate.now();
        for (String accountNo : accountNos) {
            try {
                runRebalanceForAccount(accountNo, today);
            } catch (Exception e) {
                log.warn("중기 리밸런싱 실패: accountNo={}", accountNo, e);
            }
        }
    }

    private void runRebalanceForAccount(String accountNo, LocalDate today) {
        List<StrategyPosition> positions = strategyPositionRepository
                .findByAccountNoAndStrategyTypeAndExitDtIsNullOrderByEntryDtAsc(accountNo, StrategyType.MEDIUM_TERM);
        if (positions.isEmpty()) {
            return;
        }

        String userId = tradingSettingRepository.findByAccountNo(accountNo)
                .map(com.investment.domain.entity.TradingSetting::getUserId)
                .orElse(null);
        if (userId == null) {
            log.warn("중기 리밸런싱 스킵: userId 없음, accountNo={}", accountNo);
            return;
        }

        Set<String> markets = positions.stream().map(StrategyPosition::getMarket).filter(Objects::nonNull).collect(Collectors.toSet());
        if (markets.isEmpty()) {
            markets = Set.of("KR");
        }

        for (String market : markets) {
            MediumTermMomentumService.MomentumRankingResult result = mediumTermMomentumService.computeMomentumRanking(market, today);
            Set<String> top10 = result.getTop10PercentSymbols();
            List<StrategyPosition> inMarket = positions.stream().filter(p -> market.equals(p.getMarket())).collect(Collectors.toList());

            List<StrategyPosition> toExit = inMarket.stream()
                    .filter(p -> !top10.contains(p.getSymbol()))
                    .collect(Collectors.toList());
            if (!toExit.isEmpty()) {
                executeExitsForRankDrop(accountNo, userId, toExit, today);
            }

            Set<String> currentSymbols = inMarket.stream().map(StrategyPosition::getSymbol).collect(Collectors.toSet());
            List<String> entryCandidates = result.getOrderedSymbols().stream()
                    .filter(top10::contains)
                    .filter(s -> !currentSymbols.contains(s))
                    .limit(20)
                    .collect(Collectors.toList());
            if (!entryCandidates.isEmpty()) {
                log.info("중기 리밸런싱 ENTRY 후보: accountNo={}, market={}, symbols={}", accountNo, market, entryCandidates);
            }
        }
    }

    private void executeExitsForRankDrop(String accountNo, String userId, List<StrategyPosition> toExit, LocalDate today) {
        List<String> symbols = toExit.stream().map(StrategyPosition::getSymbol).distinct().collect(Collectors.toList());
        List<CurrentPriceDto> prices = realtimeMarketDataService.getCurrentPrices(symbols).blockOptional().orElse(List.of());
        Map<String, BigDecimal> priceMap = prices.stream()
                .filter(d -> d.getSymbol() != null && d.getCurrentPrice() != null)
                .collect(Collectors.toMap(CurrentPriceDto::getSymbol, CurrentPriceDto::getCurrentPrice, (a, b) -> a));

        for (StrategyPosition pos : toExit) {
            BigDecimal currentPrice = priceMap.get(pos.getSymbol());
            if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                log.debug("중기 리밸런싱 EXIT 스킵: 시세 없음, symbol={}", pos.getSymbol());
                continue;
            }
            try {
                OrderRequestDto sell = OrderRequestDto.builder()
                        .accountNo(accountNo)
                        .symbol(pos.getSymbol())
                        .market(pos.getMarket() != null ? pos.getMarket() : "KR")
                        .orderType(OrderRequestDto.OrderType.SELL)
                        .quantity(pos.getQuantity())
                        .price(currentPrice)
                        .exitRuleType(EXIT_REASON_MOMENTUM_RANK_DROP)
                        .build();
                orderService.executeOrderForPipeline(sell, userId);
                pos.close(today, currentPrice);
                pos.setExitRuleType(EXIT_REASON_MOMENTUM_RANK_DROP);
                strategyPositionRepository.save(pos);
                log.info("중기 리밸런싱 EXIT: positionId={}, symbol={}, reason={}", pos.getId(), pos.getSymbol(), EXIT_REASON_MOMENTUM_RANK_DROP);
            } catch (Exception e) {
                log.warn("중기 리밸런싱 EXIT 주문 실패: positionId={}, symbol={}", pos.getId(), pos.getSymbol(), e);
            }
        }
    }
}
