package com.investment.core.engine.portfolio;

import com.investment.account.dto.AccountBalanceDto;
import com.investment.account.dto.AccountPositionDto;
import com.investment.account.dto.BalanceAndPositionsDto;
import com.investment.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Phase 2: 현재 보유 vs 목표 비중 차이로 매매 리스트 생성.
 * userId로 잔고·포지션 조회 후 시장별 필터링하여 리밸런싱 항목 산출.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "investment.portfolio.mode", havingValue = "inverse-volatility")
@RequiredArgsConstructor
public class RebalancerImpl implements Rebalancer {

    private final AccountService accountService;

    @Override
    public List<RebalanceItem> computeRebalanceList(LocalDate asOfDate, String accountNo, String market,
            Map<String, BigDecimal> targetWeights, BigDecimal totalValue) {
        return List.of();
    }

    @Override
    public List<RebalanceItem> computeRebalanceList(LocalDate asOfDate, String accountNo, String market,
            Map<String, BigDecimal> targetWeights, BigDecimal totalValue, String userId) {
        if (userId == null || targetWeights == null || totalValue == null || totalValue.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }
        BalanceAndPositionsDto balanceAndPositions = accountService.getBalanceAndPositionsWithUserId(userId, accountNo);
        if (balanceAndPositions == null) {
            return List.of();
        }
        BigDecimal total = totalValue;
        if (balanceAndPositions.getBalance().getTotalAssetValue() != null
                && balanceAndPositions.getBalance().getTotalAssetValue().compareTo(BigDecimal.ZERO) > 0) {
            total = balanceAndPositions.getBalance().getTotalAssetValue();
        } else if (balanceAndPositions.getBalance().getTotalBalance() != null) {
            total = balanceAndPositions.getBalance().getTotalBalance();
        }
        String marketNorm = Optional.ofNullable(market).orElse("KR").toUpperCase();
        Map<String, BigDecimal> currentValueBySymbol = balanceAndPositions.getPositions().stream()
                .filter(p -> marketNorm.equals(Optional.ofNullable(p.getMarket()).orElse("KR").toUpperCase()))
                .collect(Collectors.toMap(AccountPositionDto::getSymbol,
                        p -> p.getTotalValue() != null ? p.getTotalValue() : BigDecimal.ZERO,
                        BigDecimal::add));

        List<RebalanceItem> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> targetEntry : targetWeights.entrySet()) {
            String symbol = targetEntry.getKey();
            BigDecimal targetWeight = targetEntry.getValue() != null ? targetEntry.getValue() : BigDecimal.ZERO;
            BigDecimal currentValue = currentValueBySymbol.getOrDefault(symbol, BigDecimal.ZERO);
            BigDecimal currentWeight = total.compareTo(BigDecimal.ZERO) > 0
                    ? currentValue.divide(total, 6, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal targetValue = targetWeight.multiply(total).setScale(2, RoundingMode.HALF_UP);
            BigDecimal diffValue = targetValue.subtract(currentValue);
            if (diffValue.compareTo(BigDecimal.ZERO) > 0) {
                result.add(new RebalanceItem(symbol, "BUY", null, diffValue));
            } else if (diffValue.compareTo(BigDecimal.ZERO) < 0) {
                result.add(new RebalanceItem(symbol, "SELL", null, diffValue.abs()));
            }
        }
        for (String symbol : currentValueBySymbol.keySet()) {
            if (!targetWeights.containsKey(symbol) || targetWeights.get(symbol).compareTo(BigDecimal.ZERO) == 0) {
                BigDecimal currentValue = currentValueBySymbol.get(symbol);
                if (currentValue.compareTo(BigDecimal.ZERO) > 0) {
                    result.add(new RebalanceItem(symbol, "SELL", null, currentValue));
                }
            }
        }
        return result;
    }
}
