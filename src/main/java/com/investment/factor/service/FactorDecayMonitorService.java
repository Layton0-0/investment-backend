package com.investment.factor.service;

import com.investment.alert.EmergencyAlertService;
import com.investment.domain.entity.DailyStock;
import com.investment.domain.entity.SignalScore;
import com.investment.domain.repository.DailyStockRepository;
import com.investment.domain.repository.SignalScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 팩터별 최근 N개월 성과 추적. 5일 수익률 기준 Sharpe 미달 시 DEGRADED 알림.
 * StrategyGovernanceCheck와 연동해 halt 판단 보강에 활용 가능.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FactorDecayMonitorService {

    private static final String COMPONENT_FACTOR_DECAY = "FactorDecay";
    private static final int FORWARD_DAYS = 5;
    private static final double SHARPE_THRESHOLD = 0.5;

    private final SignalScoreRepository signalScoreRepository;
    private final DailyStockRepository dailyStockRepository;
    private final EmergencyAlertService emergencyAlertService;

    @Value("${investment.governance.factor-decay-lookback-months:3}")
    private int lookbackMonths = 3;

    @Value("${investment.governance.factor-decay-sharpe-min:0.5}")
    private double sharpeMin = SHARPE_THRESHOLD;

    /** 마지막 checkAndSendAlerts 실행에서 열화된 팩터 (market:factorType). StrategyGovernanceCheck 보강용 */
    private volatile List<String> lastDegradedFactorKeys = List.of();

    /**
     * 팩터별 5일 수익률 기반 Sharpe 계산 후, 임계값 미달 시 Discord 알림 발송.
     */
    public void checkAndSendAlerts() {
        LocalDate end = LocalDate.now().minusDays(FORWARD_DAYS + 1);
        LocalDate start = end.minusMonths(lookbackMonths);
        List<String> degradedMessages = new ArrayList<>();
        List<String> degradedKeys = new ArrayList<>();

        for (String market : List.of("KR", "US")) {
            List<SignalScore> signals = signalScoreRepository.findByMarketAndBasDtBetween(market, start, end);
            if (signals.isEmpty()) {
                log.debug("팩터 열화: 시그널 없음, market={}, {} ~ {}", market, start, end);
                continue;
            }
            Map<String, List<SignalScore>> byFactor = signals.stream()
                    .collect(Collectors.groupingBy(SignalScore::getFactorType));

            for (Map.Entry<String, List<SignalScore>> e : byFactor.entrySet()) {
                String factorType = e.getKey();
                List<Double> returns5d = computeForwardReturns5d(e.getValue(), market, end);
                if (returns5d.size() < 5) {
                    log.debug("팩터 열화: 샘플 부족 factorType={}, market={}, n={}", factorType, market, returns5d.size());
                    continue;
                }
                double sharpe = computeSharpe(returns5d);
                if (sharpe < sharpeMin) {
                    degradedKeys.add(market + ":" + factorType);
                    String msg = String.format(
                            "**[팩터 열화]** %s (%s)\n최근 %d개월 5일 수익률 Sharpe: %.3f (기준 최소: %.2f)\n권장: 해당 팩터 비중 축소 또는 원인 분석.",
                            factorType, market, lookbackMonths, sharpe, sharpeMin);
                    degradedMessages.add(msg);
                    log.info("팩터 열화: factorType={}, market={}, sharpe={}", factorType, market, sharpe);
                }
            }
        }

        lastDegradedFactorKeys = List.copyOf(degradedKeys);

        if (!degradedMessages.isEmpty()) {
            String fullMessage = String.join("\n\n", degradedMessages);
            emergencyAlertService.sendRiskEventAlert("WARNING", COMPONENT_FACTOR_DECAY, fullMessage);
            log.info("Factor decay alert sent: {} degraded factor(s)", degradedMessages.size());
        }
    }

    /**
     * 각 시그널의 basDt 시점 종가 대비 5일 후 수익률 목록 (데이터 있는 것만).
     */
    List<Double> computeForwardReturns5d(List<SignalScore> signals, String market, LocalDate maxBasDt) {
        List<Double> out = new ArrayList<>();
        for (SignalScore s : signals) {
            LocalDate basDt = s.getBasDt();
            if (basDt.plusDays(FORWARD_DAYS).isAfter(maxBasDt)) {
                continue;
            }
            BigDecimal close0 = getClose(s.getSymbol(), market, basDt);
            BigDecimal close5 = getClose(s.getSymbol(), market, basDt.plusDays(FORWARD_DAYS));
            if (close0 != null && close5 != null && close0.compareTo(BigDecimal.ZERO) > 0) {
                double ret = close5.subtract(close0).divide(close0, 6, RoundingMode.HALF_UP).doubleValue();
                out.add(ret);
            }
        }
        return out;
    }

    private BigDecimal getClose(String symbol, String market, LocalDate dt) {
        List<DailyStock> list = dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                symbol, market, dt, dt);
        return list.isEmpty() || list.get(0).getClosePrice() == null
                ? null
                : list.get(0).getClosePrice();
    }

    private double computeSharpe(List<Double> returns) {
        if (returns.isEmpty()) {
            return 0.0;
        }
        double mean = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = returns.stream().mapToDouble(r -> Math.pow(r - mean, 2)).average().orElse(0.0);
        double std = Math.sqrt(variance);
        if (std < 1e-10) {
            return 0.0;
        }
        return mean / std;
    }

    /**
     * 최근 checkAndSendAlerts 실행에서 열화된 팩터 키 목록 (market:factorType). StrategyGovernanceCheck 보강용.
     */
    public List<String> getDegradedFactorTypes() {
        return lastDegradedFactorKeys;
    }
}
