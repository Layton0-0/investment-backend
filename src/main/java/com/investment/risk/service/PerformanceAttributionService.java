package com.investment.risk.service;

import com.investment.domain.entity.StrategyPosition;
import com.investment.domain.repository.StrategyPositionRepository;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.risk.dto.PerformanceAttributionDto;
import com.investment.strategy.domain.StrategyType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 성과 귀인: 팩터/전략별 수익 기여도 분석.
 * TB_STRATEGY_POSITION 청산 포지션(exitDt not null) 기준으로 실현 PnL을 팩터(signalType)·전략(strategyType)별로 집계.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceAttributionService {

    private final StrategyPositionRepository strategyPositionRepository;
    private final TradingSettingRepository tradingSettingRepository;

    private static final int SCALE = 4;
    private static final RoundingMode ROUND = RoundingMode.HALF_UP;

    /**
     * 사용자 계좌별 청산 포지션을 합쳐 팩터/전략별 기여도 산출.
     * totalRealizedPnl = 0이면 기여율 0%로 반환.
     */
    @Transactional(readOnly = true)
    public PerformanceAttributionDto getAttribution(String userId) {
        List<StrategyPosition> closed = new ArrayList<>();
        for (var setting : tradingSettingRepository.findByUserIdOrderByAccountNo(userId)) {
            closed.addAll(strategyPositionRepository.findByAccountNoAndExitDtIsNotNullOrderByExitDtDesc(setting.getAccountNo()));
        }

        BigDecimal totalPnl = BigDecimal.ZERO;
        Map<String, BigDecimal> pnlByFactor = new LinkedHashMap<>();
        Map<String, BigDecimal> pnlByStrategy = new LinkedHashMap<>();

        for (StrategyPosition p : closed) {
            if (p.getExitPrice() == null) continue;
            BigDecimal pnl = p.getExitPrice().subtract(p.getEntryPrice())
                    .multiply(BigDecimal.valueOf(p.getQuantity()))
                    .setScale(SCALE, ROUND);
            totalPnl = totalPnl.add(pnl);

            String factor = p.getSignalType() != null && !p.getSignalType().isBlank()
                    ? p.getSignalType() : "UNKNOWN";
            pnlByFactor.merge(factor, pnl, BigDecimal::add);

            String strategy = p.getStrategyType() != null ? p.getStrategyType().name() : StrategyType.SHORT_TERM.name();
            pnlByStrategy.merge(strategy, pnl, BigDecimal::add);
        }

        List<PerformanceAttributionDto.FactorContribution> byFactor = new ArrayList<>();
        List<PerformanceAttributionDto.StrategyContribution> byStrategy = new ArrayList<>();

        if (totalPnl.compareTo(BigDecimal.ZERO) != 0) {
            for (Map.Entry<String, BigDecimal> e : pnlByFactor.entrySet()) {
                BigDecimal pct = e.getValue().divide(totalPnl.abs(), SCALE, ROUND).multiply(BigDecimal.valueOf(100));
                byFactor.add(new PerformanceAttributionDto.FactorContribution(e.getKey(), e.getValue(), pct));
            }
            for (Map.Entry<String, BigDecimal> e : pnlByStrategy.entrySet()) {
                BigDecimal pct = e.getValue().divide(totalPnl.abs(), SCALE, ROUND).multiply(BigDecimal.valueOf(100));
                byStrategy.add(new PerformanceAttributionDto.StrategyContribution(e.getKey(), e.getValue(), pct));
            }
        } else {
            for (String f : pnlByFactor.keySet()) {
                byFactor.add(new PerformanceAttributionDto.FactorContribution(f, pnlByFactor.get(f), BigDecimal.ZERO));
            }
            for (String s : pnlByStrategy.keySet()) {
                byStrategy.add(new PerformanceAttributionDto.StrategyContribution(s, pnlByStrategy.get(s), BigDecimal.ZERO));
            }
        }

        return PerformanceAttributionDto.builder()
                .totalRealizedPnl(totalPnl)
                .byFactor(byFactor)
                .byStrategy(byStrategy)
                .build();
    }
}
