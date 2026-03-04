package com.investment.factor.service;

import com.investment.account.dto.AccountPositionDto;
import com.investment.account.dto.BalanceAndPositionsDto;
import com.investment.account.service.AccountService;
import com.investment.common.security.LogMaskingUtil;
import com.investment.core.engine.portfolio.Rebalancer;
import com.investment.marketdata.dto.CurrentPriceDto;
import com.investment.marketdata.service.RealtimeMarketDataService;
import com.investment.order.dto.OrderRequestDto;
import com.investment.order.service.OrderService;
import com.investment.tradingportfolio.service.RebalanceSuggestionsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 동적 리밸런싱 (P2-2): 현재 비중 vs 목표 비중(로보 US) 절대 차이가 drift-tolerance-pct를 초과하면 리밸런스 실행.
 * 목표 비중은 RebalanceSuggestionsService(로보 어드바이저)와 동일 소스 사용.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriftRebalancingService {

    private final AccountService accountService;
    private final RebalanceSuggestionsService rebalanceSuggestionsService;
    private final Rebalancer rebalancer;
    private final OrderService orderService;
    private final RealtimeMarketDataService realtimeMarketDataService;

    @Value("${investment.pipeline.drift-tolerance-pct:0.05}")
    private BigDecimal driftTolerancePct;

    @Value("${investment.pipeline.drift-rebalance-enabled:true}")
    private boolean driftRebalanceEnabled;

    /**
     * 드리프트가 임계값을 초과하면 리밸런스 매매 목록 반환. 미초과 또는 US가 아니면 빈 목록.
     * 테스트 및 검사 전용.
     */
    public List<Rebalancer.RebalanceItem> getRebalanceListIfDriftExceeded(String userId, String accountNo, String market) {
        if (userId == null || accountNo == null || !"US".equalsIgnoreCase(Optional.ofNullable(market).orElse(""))) {
            return List.of();
        }
        BalanceAndPositionsDto balanceAndPositions = accountService.getBalanceAndPositionsWithUserId(userId, accountNo);
        if (balanceAndPositions == null) {
            return List.of();
        }
        BigDecimal totalValue = resolveTotalValue(balanceAndPositions);
        if (totalValue == null || totalValue.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }
        Map<String, BigDecimal> currentWeights = currentWeightsUs(balanceAndPositions, totalValue);
        Map<String, BigDecimal> targetWeights = rebalanceSuggestionsService.getTargetWeightsForUs();
        if (targetWeights.isEmpty()) {
            return List.of();
        }
        BigDecimal maxDrift = computeMaxDrift(currentWeights, targetWeights);
        if (maxDrift == null || maxDrift.compareTo(driftTolerancePct) <= 0) {
            return List.of();
        }
        return rebalancer.computeRebalanceList(
                LocalDate.now(), accountNo, "US", targetWeights, totalValue, userId);
    }

    /**
     * US 계좌에 대해 드리프트 검사 후, 임계값 초과 시 리밸런스 주문 실행.
     * KR 또는 비활성화 시 아무 작업도 하지 않음.
     */
    public void checkAndExecuteDriftRebalance(String userId, String accountNo, String market) {
        if (!"US".equalsIgnoreCase(Optional.ofNullable(market).orElse(""))) {
            log.debug("드리프트 리밸런스 스킵: market={} (US만 지원)", market);
            return;
        }
        if (!driftRebalanceEnabled) {
            log.debug("드리프트 리밸런스 비활성화: accountNo={}", LogMaskingUtil.maskAccountNo(accountNo));
            return;
        }
        List<Rebalancer.RebalanceItem> items = getRebalanceListIfDriftExceeded(userId, accountNo, market);
        if (items.isEmpty()) {
            return;
        }
        log.info("드리프트 리밸런스 실행: accountNo={}, items={}",
                LogMaskingUtil.maskAccountNo(accountNo), items.size());
        for (Rebalancer.RebalanceItem item : items) {
            executeRebalanceItem(userId, accountNo, item);
        }
    }

    private void executeRebalanceItem(String userId, String accountNo, Rebalancer.RebalanceItem item) {
        BigDecimal notional = item.notional();
        if (notional == null || notional.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        CurrentPriceDto priceDto = realtimeMarketDataService.getCurrentPriceBlocking(item.symbol());
        BigDecimal price = priceDto != null && priceDto.getCurrentPrice() != null
                && priceDto.getCurrentPrice().compareTo(BigDecimal.ZERO) > 0
                ? priceDto.getCurrentPrice()
                : null;
        if (price == null) {
            log.debug("드리프트 리밸런스 스킵: 시세 없음, symbol={}", item.symbol());
            return;
        }
        int qty = notional.divide(price, 0, RoundingMode.DOWN).intValue();
        if (qty <= 0) {
            return;
        }
        try {
            OrderRequestDto request = OrderRequestDto.builder()
                    .accountNo(accountNo)
                    .symbol(item.symbol())
                    .orderType("BUY".equalsIgnoreCase(item.side())
                            ? OrderRequestDto.OrderType.BUY
                            : OrderRequestDto.OrderType.SELL)
                    .quantity(qty)
                    .price(price)
                    .market("US")
                    .build();
            orderService.executeOrderForPipeline(request, userId);
            log.info("드리프트 리밸런스 주문: accountNo={}, symbol={}, side={}, qty={}",
                    LogMaskingUtil.maskAccountNo(accountNo), item.symbol(), item.side(), qty);
        } catch (Exception e) {
            log.warn("드리프트 리밸런스 주문 실패: symbol={}, error={}", item.symbol(), e.getMessage());
        }
    }

    private static BigDecimal resolveTotalValue(BalanceAndPositionsDto dto) {
        if (dto == null || dto.getBalance() == null) {
            return null;
        }
        if (dto.getBalance().getTotalAssetValue() != null
                && dto.getBalance().getTotalAssetValue().compareTo(BigDecimal.ZERO) > 0) {
            return dto.getBalance().getTotalAssetValue();
        }
        return dto.getBalance().getTotalBalance();
    }

    private static Map<String, BigDecimal> currentWeightsUs(BalanceAndPositionsDto dto, BigDecimal totalValue) {
        if (dto == null || dto.getPositions() == null || totalValue == null
                || totalValue.compareTo(BigDecimal.ZERO) <= 0) {
            return Map.of();
        }
        return dto.getPositions().stream()
                .filter(p -> "US".equalsIgnoreCase(Optional.ofNullable(p.getMarket()).orElse("")))
                .collect(Collectors.toMap(AccountPositionDto::getSymbol,
                        p -> p.getTotalValue() != null && p.getTotalValue().compareTo(BigDecimal.ZERO) > 0
                                ? p.getTotalValue().divide(totalValue, 6, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO,
                        (a, b) -> a));
    }

    private static BigDecimal computeMaxDrift(Map<String, BigDecimal> currentWeights,
            Map<String, BigDecimal> targetWeights) {
        Set<String> symbols = new java.util.HashSet<>(currentWeights.keySet());
        symbols.addAll(targetWeights.keySet());
        BigDecimal max = BigDecimal.ZERO;
        for (String symbol : symbols) {
            BigDecimal c = currentWeights.getOrDefault(symbol, BigDecimal.ZERO);
            BigDecimal t = targetWeights.getOrDefault(symbol, BigDecimal.ZERO);
            BigDecimal diff = c.subtract(t).abs();
            if (diff.compareTo(max) > 0) {
                max = diff;
            }
        }
        return max;
    }
}
