package com.investment.core.engine.risk;

import com.investment.account.dto.AccountBalanceDto;
import com.investment.account.dto.AccountPositionDto;
import com.investment.account.dto.BalanceAndPositionsDto;
import com.investment.account.service.AccountService;
import com.investment.order.dto.OrderRequestDto;
import com.investment.risk.service.PortfolioPeakService;
import com.investment.risk.service.TradingHaltService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Phase 2: Pre-Trade 컴플라이언스 실제 구현.
 * - Kill Switch: halt_all_orders 시 모든 주문 거부
 * - 단일 종목 비중 상한: 주문 후 해당 종목 비중 > 10% 시 거부
 * - MDD 게이트: MDD > 15% 시 신규 매수만 차단
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "investment.compliance.use-stub", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
public class PreTradeComplianceEngine implements ComplianceEngine {

    private static final BigDecimal SINGLE_NAME_WEIGHT_LIMIT = new BigDecimal("0.10");
    private static final BigDecimal MDD_THRESHOLD = new BigDecimal("0.15");

    private final TradingHaltService tradingHaltService;
    private final PortfolioPeakService portfolioPeakService;
    private final AccountService accountService;

    @Override
    public ComplianceResult preTradeCheck(OrderRequestDto request, String userId) {
        if (tradingHaltService.isHaltAllOrders()) {
            log.warn("PreTrade 거부: Kill Switch 활성화");
            return ComplianceResult.reject("긴급 차단(Kill Switch)이 활성화되어 주문이 불가합니다.");
        }

        BalanceAndPositionsDto balanceAndPositions = accountService.getBalanceAndPositionsWithUserId(userId,
                request.getAccountNo());
        if (balanceAndPositions == null) {
            log.debug("잔고·포지션 조회 실패로 컴플라이언스 스킵(승인)");
            return ComplianceResult.approve();
        }

        AccountBalanceDto balance = balanceAndPositions.getBalance();
        BigDecimal totalValue = totalPortfolioValue(balance);
        if (totalValue.compareTo(BigDecimal.ZERO) <= 0) {
            return ComplianceResult.approve();
        }

        BigDecimal orderAmount = request.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));

        if (request.getOrderType() == OrderRequestDto.OrderType.BUY) {
            BigDecimal mdd = portfolioPeakService.getOrUpdatePeakAndComputeMdd(
                    request.getAccountNo(), totalValue, LocalDate.now());
            if (mdd.compareTo(MDD_THRESHOLD) > 0) {
                log.warn("PreTrade 거부: MDD 초과 accountNo={}, mdd={}", request.getAccountNo(), mdd);
                return ComplianceResult.reject(
                        String.format("최대 낙폭(MDD) %.1f%% 초과로 신규 매수가 제한됩니다.", mdd.multiply(BigDecimal.valueOf(100)).doubleValue()));
            }

            BigDecimal currentSymbolValue = getPositionValueForSymbol(
                    balanceAndPositions.getPositions(), request.getSymbol(), request.getMarketOrKr());
            BigDecimal newSymbolValue = currentSymbolValue.add(orderAmount);
            BigDecimal newTotalAfterBuy = totalValue.add(orderAmount);
            BigDecimal newWeight = newTotalAfterBuy.compareTo(BigDecimal.ZERO) > 0
                    ? newSymbolValue.divide(newTotalAfterBuy, 6, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            if (newWeight.compareTo(SINGLE_NAME_WEIGHT_LIMIT) > 0) {
                log.warn("PreTrade 거부: 단일 종목 비중 초과 symbol={}, newWeight={}", request.getSymbol(), newWeight);
                return ComplianceResult.reject(
                        String.format("단일 종목 비중 상한(10%%) 초과: 주문 후 비중 %.1f%%", newWeight.multiply(BigDecimal.valueOf(100)).doubleValue()));
            }
        } else {
            BigDecimal currentSymbolValue = getPositionValueForSymbol(
                    balanceAndPositions.getPositions(), request.getSymbol(), request.getMarketOrKr());
            BigDecimal newSymbolValue = currentSymbolValue.subtract(orderAmount);
            BigDecimal newTotalAfterSell = totalValue.subtract(orderAmount);
            if (newTotalAfterSell.compareTo(BigDecimal.ZERO) <= 0) {
                return ComplianceResult.approve();
            }
            BigDecimal newWeight = newSymbolValue.divide(newTotalAfterSell, 6, RoundingMode.HALF_UP);
            if (newWeight.compareTo(SINGLE_NAME_WEIGHT_LIMIT) > 0) {
                log.warn("PreTrade 거부: 매도 후에도 단일 종목 비중 초과 symbol={}, newWeight={}", request.getSymbol(), newWeight);
                return ComplianceResult.reject(
                        String.format("단일 종목 비중 상한(10%%) 초과: 매도 후 비중 %.1f%%", newWeight.multiply(BigDecimal.valueOf(100)).doubleValue()));
            }
        }

        return ComplianceResult.approve();
    }

    private static BigDecimal totalPortfolioValue(AccountBalanceDto balance) {
        if (balance.getTotalAssetValue() != null && balance.getTotalAssetValue().compareTo(BigDecimal.ZERO) > 0) {
            return balance.getTotalAssetValue();
        }
        return balance.getTotalBalance() != null ? balance.getTotalBalance() : BigDecimal.ZERO;
    }

    private static BigDecimal getPositionValueForSymbol(
            java.util.List<AccountPositionDto> positions, String symbol, String market) {
        if (positions == null) {
            return BigDecimal.ZERO;
        }
        return positions.stream()
                .filter(p -> symbol.equals(p.getSymbol()) && marketEquals(p.getMarket(), market))
                .map(AccountPositionDto::getTotalValue)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static boolean marketEquals(String posMarket, String requestMarket) {
        String p = Optional.ofNullable(posMarket).orElse("KR").toUpperCase();
        String r = Optional.ofNullable(requestMarket).orElse("KR").toUpperCase();
        return p.equals(r);
    }
}
