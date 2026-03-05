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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 성과 귀인(Performance Attribution): 사용자 계좌별 청산 포지션 실현 PnL을
 * signalType(팩터)·strategyType(전략)별로 집계하고 기여율(합 100%)을 산출.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceAttributionService {

    private static final String UNKNOWN = "UNKNOWN";
    private static final int SCALE = 4;

    private final TradingSettingRepository tradingSettingRepository;
    private final StrategyPositionRepository strategyPositionRepository;

    /**
     * 사용자 기준 성과 귀인. 해당 사용자의 모든 계좌에 대한 청산 포지션 집계.
     */
    @Transactional(readOnly = true)
    public PerformanceAttributionDto getAttribution(String userId) {
        List<String> accountNos = tradingSettingRepository.findByUserIdOrderByAccountNo(userId).stream()
                .map(s -> s.getAccountNo())
                .distinct()
                .collect(Collectors.toList());
        if (accountNos.isEmpty()) {
            return PerformanceAttributionDto.builder()
                    .totalRealizedPnl(BigDecimal.ZERO)
                    .bySignalType(Map.of())
                    .byStrategyType(Map.of())
                    .build();
        }

        Map<String, BigDecimal> pnlBySignal = new HashMap<>();
        Map<String, BigDecimal> pnlByStrategy = new HashMap<>();
        BigDecimal totalPnl = BigDecimal.ZERO;

        for (String accountNo : accountNos) {
            List<StrategyPosition> closed = strategyPositionRepository
                    .findByAccountNoAndExitDtIsNotNullOrderByExitDtDesc(accountNo);
            for (StrategyPosition p : closed) {
                if (p.getExitPrice() == null) continue;
                BigDecimal pnl = p.getExitPrice().subtract(p.getEntryPrice())
                        .multiply(BigDecimal.valueOf(p.getQuantity()));
                totalPnl = totalPnl.add(pnl);

                String signal = p.getSignalType() != null && !p.getSignalType().isBlank()
                        ? p.getSignalType() : UNKNOWN;
                pnlBySignal.merge(signal, pnl, BigDecimal::add);

                String strategy = p.getStrategyType() != null ? p.getStrategyType().name() : UNKNOWN;
                pnlByStrategy.merge(strategy, pnl, BigDecimal::add);
            }
        }

        Map<String, BigDecimal> bySignalPct = toPercentMap(pnlBySignal, totalPnl);
        Map<String, BigDecimal> byStrategyPct = toPercentMap(pnlByStrategy, totalPnl);

        return PerformanceAttributionDto.builder()
                .totalRealizedPnl(totalPnl.setScale(SCALE, RoundingMode.HALF_UP))
                .bySignalType(bySignalPct)
                .byStrategyType(byStrategyPct)
                .build();
    }

    private static Map<String, BigDecimal> toPercentMap(Map<String, BigDecimal> pnlByKey, BigDecimal totalPnl) {
        if (totalPnl == null || totalPnl.compareTo(BigDecimal.ZERO) == 0) {
            return pnlByKey.keySet().stream()
                    .collect(Collectors.toMap(k -> k, k -> BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP)));
        }
        Map<String, BigDecimal> out = new HashMap<>();
        for (Map.Entry<String, BigDecimal> e : pnlByKey.entrySet()) {
            BigDecimal pct = e.getValue().divide(totalPnl, SCALE + 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
            out.put(e.getKey(), pct);
        }
        return out;
    }
}
