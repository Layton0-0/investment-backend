package com.investment.tradingportfolio.service;

import com.investment.account.dto.BalanceAndPositionsDto;
import com.investment.account.service.AccountService;
import com.investment.backtest.robo.RoboAllocationEngine;
import com.investment.backtest.robo.dto.RoboAllocationResult;
import com.investment.config.RoboBacktestProperties;
import com.investment.core.engine.portfolio.Rebalancer;
import com.investment.domain.entity.TradingSetting;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.tradingportfolio.dto.RebalanceSuggestionsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 리밸런싱 제안 (뷰 전용): 목표 비중 vs 현재 보유 차이로 매수/매도 제안 목록 반환.
 * 로보 어드바이저 목표 비중을 사용한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RebalanceSuggestionsService {

    private final Rebalancer rebalancer;
    private final AccountService accountService;
    private final RoboAllocationEngine roboAllocationEngine;
    private final RoboBacktestProperties roboBacktestProperties;
    private final TradingSettingRepository tradingSettingRepository;

    /**
     * US 시장 로보 어드바이저 목표 비중 (동적 리밸런싱 등 동일 소스 사용).
     *
     * @return symbol → weight (합 1 미만이면 나머지 현금)
     */
    public Map<String, BigDecimal> getTargetWeightsForUs() {
        return computeRoboTargetWeights();
    }

    /**
     * 계좌·시장에 대한 리밸런싱 제안 (로보 목표 비중 기준).
     *
     * @param userId    사용자 ID
     * @param accountNo 계좌번호
     * @param market    시장 (KR/US). US만 로보 목표 비중 지원.
     * @return 제안 목록 또는 데이터 없으면 빈 목록
     */
    public RebalanceSuggestionsDto getSuggestions(String userId, String accountNo, String market) {
        if (userId == null || accountNo == null) {
            return empty(market);
        }
        BalanceAndPositionsDto balanceAndPositions = accountService.getBalanceAndPositionsWithUserId(userId, accountNo);
        if (balanceAndPositions == null) {
            return empty(market);
        }
        BigDecimal totalValue = balanceAndPositions.getBalance() != null
                && balanceAndPositions.getBalance().getTotalAssetValue() != null
                ? balanceAndPositions.getBalance().getTotalAssetValue()
                : balanceAndPositions.getBalance() != null && balanceAndPositions.getBalance().getTotalBalance() != null
                ? balanceAndPositions.getBalance().getTotalBalance()
                : BigDecimal.ZERO;
        if (totalValue.compareTo(BigDecimal.ZERO) <= 0) {
            return empty(market);
        }
        String marketNorm = Optional.ofNullable(market).orElse("US").toUpperCase();
        if (!"US".equals(marketNorm)) {
            return RebalanceSuggestionsDto.fromRebalanceItems(marketNorm, totalValue, List.of());
        }
        Map<String, BigDecimal> targetWeights = computeRoboTargetWeights();
        List<Rebalancer.RebalanceItem> items = rebalancer.computeRebalanceList(
                LocalDate.now(), accountNo, marketNorm, targetWeights, totalValue, userId);
        return RebalanceSuggestionsDto.fromRebalanceItems(marketNorm, totalValue, items);
    }

    private Map<String, BigDecimal> computeRoboTargetWeights() {
        LocalDate today = LocalDate.now();
        String mode = roboBacktestProperties.getDualMomentumMode();
        boolean useNote = "DUAL_MOMENTUM_NOTE".equalsIgnoreCase(mode);
        List<String> symbols = useNote ? roboBacktestProperties.getSectorEtfSymbolList()
                : roboBacktestProperties.getAssetSymbolList();
        int maWindowDays = roboBacktestProperties.getMaWindowDays();
        int volLookback = roboBacktestProperties.getVolatilityLookbackDays();
        RoboAllocationResult allocation;
        if (useNote) {
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
            allocation = roboAllocationEngine.computeTargetWeights(
                    today, symbols,
                    roboBacktestProperties.getMomentumMonths(),
                    maWindowDays,
                    roboBacktestProperties.getTopN(),
                    volLookback);
        }
        return (allocation != null && allocation.getWeights() != null) ? allocation.getWeights() : Map.of();
    }

    private static RebalanceSuggestionsDto empty(String market) {
        return RebalanceSuggestionsDto.fromRebalanceItems(
                Optional.ofNullable(market).orElse("US"), BigDecimal.ZERO, List.of());
    }
}
