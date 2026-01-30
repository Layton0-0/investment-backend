package com.investment.factor.execution;

import com.investment.domain.entity.StrategyPosition;
import com.investment.domain.repository.StrategyPositionRepository;
import com.investment.factor.dto.PositionRecommendationDto;
import com.investment.factor.service.PositionSizingService;
import com.investment.order.dto.OrderRequestDto;
import com.investment.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 4단계 파이프라인 실행 — 권장 포지션 주문 실행.
 * 1차: dry-run 모드 기본. auto-execute=false 시 주문 생성 없이 로그만.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineExecutor {

    private static final BigDecimal DEFAULT_ATR_MULTIPLIER = new BigDecimal("2.0");
    private static final int DEFAULT_TIME_CUT_DAYS = 5;
    private static final BigDecimal DEFAULT_TARGET_RETURN_PCT = new BigDecimal("3.0");

    private final PositionSizingService positionSizingService;
    private final OrderService orderService;
    private final StrategyPositionRepository strategyPositionRepository;

    @Value("${investment.pipeline.auto-execute:false}")
    private boolean autoExecute = false;

    /**
     * 기준일·시장에 대해 권장 포지션 산출 후, 설정에 따라 주문 실행.
     *
     * @param basDt        기준일
     * @param market       시장 (KR, US)
     * @param accountNo    계좌번호
     * @param totalCapital 총 투자 가능 자산 (원)
     * @param dryRun       true면 주문 실행 없이 권장 목록만 반환
     * @return 실행(또는 dry-run) 결과 요약
     */
    @Transactional
    public PipelineRunResult run(LocalDate basDt, String market, String accountNo, BigDecimal totalCapital, boolean dryRun) {
        List<PositionRecommendationDto> recommendations = positionSizingService.getRecommendations(basDt, market, totalCapital);
        List<PipelineRunResult.OrderResult> orderResults = new ArrayList<>();
        boolean actuallyExecute = autoExecute && !dryRun;

        for (PositionRecommendationDto rec : recommendations) {
            if (rec.getRecommendedQty() <= 0) continue;
            OrderRequestDto request = OrderRequestDto.builder()
                    .accountNo(accountNo)
                    .symbol(rec.getSymbol())
                    .orderType(OrderRequestDto.OrderType.BUY)
                    .quantity((int) rec.getRecommendedQty())
                    .price(rec.getEntryPrice())
                    .build();
            if (actuallyExecute) {
                try {
                    orderService.executeOrder(request);
                    StrategyPosition position = StrategyPosition.builder()
                            .accountNo(accountNo)
                            .symbol(rec.getSymbol())
                            .market(rec.getMarket())
                            .entryDt(basDt)
                            .entryPrice(rec.getEntryPrice())
                            .quantity((int) rec.getRecommendedQty())
                            .trailingHigh(rec.getEntryPrice())
                            .atrMultiplier(DEFAULT_ATR_MULTIPLIER)
                            .timeCutDays(DEFAULT_TIME_CUT_DAYS)
                            .targetReturnPct(DEFAULT_TARGET_RETURN_PCT)
                            .build();
                    strategyPositionRepository.save(position);
                    orderResults.add(PipelineRunResult.OrderResult.success(rec.getSymbol(), rec.getRecommendedQty(), rec.getEntryPrice()));
                } catch (Exception e) {
                    log.warn("파이프라인 주문 실패: symbol={}, error={}", rec.getSymbol(), e.getMessage());
                    orderResults.add(PipelineRunResult.OrderResult.failure(rec.getSymbol(), e.getMessage()));
                }
            } else {
                log.debug("파이프라인 dry-run: symbol={}, qty={}, price={}", rec.getSymbol(), rec.getRecommendedQty(), rec.getEntryPrice());
                orderResults.add(PipelineRunResult.OrderResult.dryRun(rec.getSymbol(), rec.getRecommendedQty(), rec.getEntryPrice()));
            }
        }
        return PipelineRunResult.builder()
                .basDt(basDt)
                .market(market)
                .dryRun(!actuallyExecute)
                .recommendationCount(recommendations.size())
                .orderResults(orderResults)
                .build();
    }

    @lombok.Getter
    @lombok.Builder
    @lombok.AllArgsConstructor
    public static class PipelineRunResult {
        private LocalDate basDt;
        private String market;
        private boolean dryRun;
        private int recommendationCount;
        private List<OrderResult> orderResults;

        @lombok.Getter
        @lombok.AllArgsConstructor
        public static class OrderResult {
            private String symbol;
            private long qty;
            private BigDecimal price;
            private boolean success;
            private String errorMessage;

            static OrderResult success(String symbol, long qty, BigDecimal price) {
                return new OrderResult(symbol, qty, price, true, null);
            }

            static OrderResult failure(String symbol, String errorMessage) {
                return new OrderResult(symbol, 0, null, false, errorMessage);
            }

            static OrderResult dryRun(String symbol, long qty, BigDecimal price) {
                return new OrderResult(symbol, qty, price, true, null);
            }
        }
    }
}
