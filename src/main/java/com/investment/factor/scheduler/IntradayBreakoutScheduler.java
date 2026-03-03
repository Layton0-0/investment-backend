package com.investment.factor.scheduler;

import com.investment.domain.entity.StrategyPosition;
import com.investment.domain.entity.TradingSetting;
import com.investment.domain.repository.StrategyPositionRepository;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.factor.dto.BreakoutCandidateDto;
import com.investment.factor.service.DailyLossLimitService;
import com.investment.factor.service.IntradayBreakoutService;
import com.investment.factor.service.RiskGateService;
import com.investment.order.dto.OrderRequestDto;
import com.investment.order.service.OrderService;
import com.investment.setting.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 장중 변동성 돌파 스케줄러 (P2).
 * 09:00~10:00 구간에서 돌파 종목 1회 매수. 자동투자 ON 계좌만 대상.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntradayBreakoutScheduler {

    private final TradingSettingRepository tradingSettingRepository;
    private final IntradayBreakoutService intradayBreakoutService;
    private final StrategyPositionRepository strategyPositionRepository;
    private final RiskGateService riskGateService;
    private final DailyLossLimitService dailyLossLimitService;
    private final OrderService orderService;
    private final SystemSettingService systemSettingService;

    private boolean isBreakoutEnabled() {
        return Boolean.TRUE.equals(systemSettingService.getBoolean("intraday.breakoutEnabled"));
    }

    @Value("${investment.intraday.breakout-max-positions:3}")
    private int breakoutMaxPositions = 3;

    /** KR 시초가/변동성 돌파 시 주문구분(ORD_DVSN). 02=최유리, 03=IOC. 빈값이면 지정가(00). */
    @Value("${investment.pipeline.kr-opening-order-dvsn:}")
    private String krOpeningOrderDvsn = "";

    /** Spring Batch Job에서 호출. 활성 여부는 Admin 시스템 설정(intraday.breakoutEnabled)에서 조회. */
    public void runIntradayBreakout() {
        if (!isBreakoutEnabled()) {
            log.trace("장중 돌파 스킵: 비활성");
            return;
        }
        if (!systemSettingService.getBoolean("pipeline.autoExecute")) {
            log.trace("장중 돌파 스킵: auto-execute=false");
            return;
        }
        List<TradingSetting> settings = tradingSettingRepository.findAllByAutoTradingEnabledTrue();
        if (settings.isEmpty()) {
            return;
        }
        LocalDate today = LocalDate.now();
        RiskGateService.RiskGateResult riskResult = riskGateService.evaluate(null);
        BigDecimal sizeMultiplier = riskResult.getSizeMultiplier() != null
                ? riskResult.getSizeMultiplier()
                : BigDecimal.ONE;
        if (!riskResult.isAllowNewBuy()) {
            log.info("장중 돌파 스킵: 리스크 게이트 신규 매수 불가");
            return;
        }
        for (TradingSetting setting : settings) {
            String accountNo = setting.getAccountNo();
            BigDecimal capital = setting.getMaxInvestmentAmount();
            if (capital == null || capital.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            capital = capital.multiply(sizeMultiplier).setScale(0, RoundingMode.DOWN);
            if (!dailyLossLimitService.isNewBuyAllowed(accountNo)) {
                log.debug("장중 돌파 스킵: accountNo={}, 일일 손실 한도 초과", accountNo);
                continue;
            }
            try {
                runBreakoutForAccount(today, accountNo, capital);
            } catch (Exception e) {
                log.warn("장중 돌파 실패: accountNo={}, error={}", accountNo, e.getMessage());
            }
        }
    }

    private void runBreakoutForAccount(LocalDate today, String accountNo, BigDecimal capital) {
        List<BreakoutCandidateDto> candidates = intradayBreakoutService.getBreakoutCandidates(today, "KR", capital);
        if (candidates.isEmpty()) {
            return;
        }
        Set<String> heldSymbols = strategyPositionRepository.findByAccountNoAndExitDtIsNullOrderByEntryDtAsc(accountNo)
                .stream()
                .map(StrategyPosition::getSymbol)
                .collect(Collectors.toSet());
        List<BreakoutCandidateDto> toBuy = candidates.stream()
                .filter(c -> !heldSymbols.contains(c.getSymbol()))
                .limit(breakoutMaxPositions > 0 ? breakoutMaxPositions : Integer.MAX_VALUE)
                .collect(Collectors.toList());
        if (toBuy.isEmpty()) {
            return;
        }
        String userId = tradingSettingRepository.findByAccountNo(accountNo)
                .map(TradingSetting::getUserId)
                .orElse(null);
        if (userId == null) {
            log.warn("장중 돌파 스킵: userId 없음, accountNo={}", accountNo);
            return;
        }
        for (BreakoutCandidateDto c : toBuy) {
            try {
                String orderDvsn = (krOpeningOrderDvsn != null && !krOpeningOrderDvsn.isBlank()) ? krOpeningOrderDvsn : null;
                OrderRequestDto request = OrderRequestDto.builder()
                        .accountNo(accountNo)
                        .symbol(c.getSymbol())
                        .orderType(OrderRequestDto.OrderType.BUY)
                        .quantity((int) c.getRecommendedQty())
                        .price(c.getCurrentPrice())
                        .market(c.getMarket() != null ? c.getMarket() : "KR")
                        .orderDvsn(orderDvsn)
                        .build();
                orderService.executeOrderForPipeline(request, userId);
                log.info("장중 돌파 매수 실행: accountNo={}, symbol={}, qty={}, price={}",
                        accountNo, c.getSymbol(), c.getRecommendedQty(), c.getCurrentPrice());
            } catch (Exception e) {
                log.warn("장중 돌파 매수 실패: symbol={}, error={}", c.getSymbol(), e.getMessage());
            }
        }
    }
}
