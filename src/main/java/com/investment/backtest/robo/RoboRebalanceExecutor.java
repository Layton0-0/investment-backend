package com.investment.backtest.robo;

import com.investment.account.dto.AccountPositionDto;
import com.investment.account.dto.BalanceAndPositionsDto;
import com.investment.account.service.AccountService;
import com.investment.backtest.robo.dto.RoboAllocationResult;
import com.investment.common.security.LogMaskingUtil;
import com.investment.config.RoboBacktestProperties;
import com.investment.domain.entity.TradingSetting;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.marketdata.dto.CurrentPriceDto;
import com.investment.marketdata.service.RealtimeMarketDataService;
import com.investment.order.dto.OrderRequestDto;
import com.investment.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 로보 어드바이저 리밸런싱 실행 — 목표 비중 산출 후 ETF 주문 생성·실행.
 * execute-orders=true 시 현재 보유(해외) 비중 조회 후 목표 비중과 비교해 US ETF 매수/매도 주문 실행.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoboRebalanceExecutor {

    private final RoboAllocationEngine roboAllocationEngine;
    private final RoboBacktestProperties roboBacktestProperties;
    private final TradingSettingRepository tradingSettingRepository;
    private final AccountService accountService;
    private final RealtimeMarketDataService realtimeMarketDataService;
    private final OrderService orderService;

    /**
     * 오늘 기준 목표 비중 산출 후 실행.
     * execute-orders=true 시 목표 비중과 현재 보유(US) 비중 차이로 ETF 매수/매도 주문 생성·OrderService
     * 호출.
     *
     * @param accountNo  계좌번호
     * @param totalValue 포트폴리오 총 평가액 (USD, 리밸런싱 기준)
     */
    public void executeRebalance(String accountNo, BigDecimal totalValue) {
        LocalDate today = LocalDate.now();
        String mode = roboBacktestProperties.getDualMomentumMode();
        boolean useDualMomentumNote = "DUAL_MOMENTUM_NOTE".equalsIgnoreCase(mode);
        List<String> symbols = useDualMomentumNote ? roboBacktestProperties.getSectorEtfSymbolList()
                : roboBacktestProperties.getAssetSymbolList();
        int maWindowDays = roboBacktestProperties.getMaWindowDays();
        int volLookback = roboBacktestProperties.getVolatilityLookbackDays();

        RoboAllocationResult allocation;
        if (useDualMomentumNote) {
            allocation = roboAllocationEngine.computeTargetWeightsDualMomentumNote(
                    today,
                    symbols,
                    12,
                    roboBacktestProperties.getRiskFreeRatePct(),
                    roboBacktestProperties.getAbsoluteMomentumSymbol(),
                    roboBacktestProperties.getRiskFreeSymbol(),
                    roboBacktestProperties.getMomentumMonthsRelative(),
                    roboBacktestProperties.getTopNSector(),
                    maWindowDays,
                    volLookback);
        } else {
            int momentumMonths = roboBacktestProperties.getMomentumMonths();
            int topN = roboBacktestProperties.getTopN();
            allocation = roboAllocationEngine.computeTargetWeights(
                    today, symbols, momentumMonths, maWindowDays, topN, volLookback);
        }

        log.info("로보 리밸런싱 목표 비중: accountNo={}, asOfDate={}, weights={}, cashWeight={}",
                LogMaskingUtil.maskAccountNo(accountNo), today, allocation.getWeights(), allocation.getCashWeight());

        if (!roboBacktestProperties.isExecuteOrders() || totalValue == null
                || totalValue.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        String userId = tradingSettingRepository.findByAccountNo(accountNo)
                .map(TradingSetting::getUserId)
                .orElse(null);
        if (userId == null) {
            log.warn("로보 리밸런싱 스킵: userId 없음, accountNo={}", LogMaskingUtil.maskAccountNo(accountNo));
            return;
        }

        BalanceAndPositionsDto balanceAndPositions = accountService.getBalanceAndPositionsWithUserId(userId, accountNo);
        Map<String, BigDecimal> currentValueBySymbol = Optional.ofNullable(balanceAndPositions)
                .map(BalanceAndPositionsDto::getPositions)
                .orElse(List.of()).stream()
                .filter(p -> "US".equals(p.getMarket()))
                .collect(Collectors.toMap(AccountPositionDto::getSymbol,
                        p -> p.getTotalValue() != null ? p.getTotalValue() : BigDecimal.ZERO, (a, b) -> a));

        BigDecimal minOrderUsd = roboBacktestProperties.getMinOrderAmountUsd() != null
                ? roboBacktestProperties.getMinOrderAmountUsd()
                : new BigDecimal("50");

        Map<String, BigDecimal> weights = allocation.getWeights();
        if (weights == null) {
            return;
        }

        for (Map.Entry<String, BigDecimal> e : weights.entrySet()) {
            String symbol = e.getKey();
            BigDecimal targetWeight = e.getValue();
            if (targetWeight == null || targetWeight.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal targetValue = totalValue.multiply(targetWeight).setScale(2, RoundingMode.HALF_UP);
            BigDecimal currentValue = currentValueBySymbol.getOrDefault(symbol, BigDecimal.ZERO);
            if (currentValue == null) {
                currentValue = BigDecimal.ZERO;
            }
            BigDecimal diff = targetValue.subtract(currentValue);

            if (diff.abs().compareTo(minOrderUsd) < 0) {
                continue;
            }

            CurrentPriceDto priceDto = realtimeMarketDataService.getCurrentPriceBlocking(symbol);
            BigDecimal price = priceDto != null && priceDto.getCurrentPrice() != null
                    && priceDto.getCurrentPrice().compareTo(BigDecimal.ZERO) > 0
                            ? priceDto.getCurrentPrice()
                            : null;
            if (price == null) {
                log.debug("로보 리밸런싱 스킵: 시세 없음, symbol={}", symbol);
                continue;
            }

            int qty = diff.divide(price, 0, RoundingMode.DOWN).abs().intValue();
            if (qty <= 0) {
                continue;
            }

            try {
                if (diff.compareTo(BigDecimal.ZERO) > 0) {
                    OrderRequestDto request = OrderRequestDto.builder()
                            .accountNo(accountNo)
                            .symbol(symbol)
                            .orderType(OrderRequestDto.OrderType.BUY)
                            .quantity(qty)
                            .price(price)
                            .market("US")
                            .build();
                    orderService.executeOrderForPipeline(request, userId);
                    log.info("로보 리밸런싱 매수 실행: accountNo={}, symbol={}, qty={}, price={}",
                            LogMaskingUtil.maskAccountNo(accountNo), symbol, qty, price);
                } else {
                    OrderRequestDto request = OrderRequestDto.builder()
                            .accountNo(accountNo)
                            .symbol(symbol)
                            .orderType(OrderRequestDto.OrderType.SELL)
                            .quantity(qty)
                            .price(price)
                            .market("US")
                            .build();
                    orderService.executeOrderForPipeline(request, userId);
                    log.info("로보 리밸런싱 매도 실행: accountNo={}, symbol={}, qty={}, price={}",
                            LogMaskingUtil.maskAccountNo(accountNo), symbol, qty, price);
                }
            } catch (Exception ex) {
                log.warn("로보 리밸런싱 주문 실패: symbol={}, error={}", symbol, ex.getMessage());
            }
        }
    }
}
