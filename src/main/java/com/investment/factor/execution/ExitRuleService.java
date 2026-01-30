package com.investment.factor.execution;

import com.investment.domain.entity.StrategyPosition;
import com.investment.domain.repository.StrategyPositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 4단계 청산 규칙 — ATR Trailing Stop, Time-Cut 평가.
 * 보유 포지션에 대해 매도 시그널 여부 판단.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExitRuleService {

    private final StrategyPositionRepository strategyPositionRepository;

    /**
     * 계좌의 보유 포지션 중 청산 대상(매도 시그널) 목록 반환.
     *
     * @param accountNo           계좌번호
     * @param currentPriceBySymbol 종목별 현재가 (symbol -> price). 없으면 해당 종목은 스킵
     * @return 청산 대상 목록
     */
    public List<ExitSignal> getSellSignals(String accountNo, Map<String, BigDecimal> currentPriceBySymbol) {
        List<StrategyPosition> openPositions = strategyPositionRepository.findByAccountNoAndExitDtIsNullOrderByEntryDtAsc(accountNo);
        List<ExitSignal> signals = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (StrategyPosition pos : openPositions) {
            BigDecimal currentPrice = currentPriceBySymbol != null ? currentPriceBySymbol.get(pos.getSymbol()) : null;
            if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            long daysHeld = java.time.temporal.ChronoUnit.DAYS.between(pos.getEntryDt(), today);
            BigDecimal returnPct = currentPrice.subtract(pos.getEntryPrice())
                    .divide(pos.getEntryPrice(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            if (pos.getTimeCutDays() > 0 && daysHeld >= pos.getTimeCutDays()) {
                if (pos.getTargetReturnPct() != null && returnPct.compareTo(pos.getTargetReturnPct()) < 0) {
                    signals.add(ExitSignal.builder()
                            .positionId(pos.getId())
                            .symbol(pos.getSymbol())
                            .market(pos.getMarket())
                            .quantity(pos.getQuantity())
                            .reason("TIME_CUT")
                            .currentPrice(currentPrice)
                            .build());
                }
            }
        }
        return signals;
    }

    @lombok.Getter
    @lombok.Builder
    @lombok.AllArgsConstructor
    public static class ExitSignal {
        private Long positionId;
        private String symbol;
        private String market;
        private int quantity;
        private String reason;
        private BigDecimal currentPrice;
    }
}
