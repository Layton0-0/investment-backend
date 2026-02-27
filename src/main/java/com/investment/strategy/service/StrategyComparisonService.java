package com.investment.strategy.service;

import com.investment.domain.entity.GovernanceCheckResult;
import com.investment.domain.repository.GovernanceCheckResultRepository;
import com.investment.strategy.domain.StrategyType;
import com.investment.strategy.dto.StrategyComparisonItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 전략 비교 — (market, strategyType)별 최신 백테스트 메트릭(MDD, Sharpe) 조회.
 * 데이터 소스: TB_GOVERNANCE_CHECK_RESULT. CAGR는 현재 미저장으로 null.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyComparisonService {

    private static final int FETCH_SIZE = 200;
    private static final Map<String, String> STRATEGY_TYPE_LABELS = Map.of(
            "SHORT_TERM", "단기",
            "MEDIUM_TERM", "중기",
            "LONG_TERM", "장기"
    );

    private final GovernanceCheckResultRepository governanceCheckResultRepository;

    /**
     * 전략별 최신 백테스트 결과 비교 목록. market이 있으면 해당 시장만, 없으면 KR+US 모두.
     */
    public List<StrategyComparisonItemDto> getComparison(String market) {
        List<GovernanceCheckResult> recent = governanceCheckResultRepository
                .findAllByOrderByRunAtDesc(PageRequest.of(0, FETCH_SIZE));
        Map<String, GovernanceCheckResult> latestByKey = recent.stream()
                .collect(Collectors.toMap(
                        r -> r.getMarket() + ":" + r.getStrategyType(),
                        r -> r,
                        (a, b) -> a.getRunAt().isAfter(b.getRunAt()) ? a : b
                ));
        Stream<StrategyComparisonItemDto> stream = latestByKey.values().stream()
                .filter(r -> market == null || market.isBlank() || market.equalsIgnoreCase(r.getMarket()))
                .map(this::toItem)
                .sorted(Comparator.comparing(StrategyComparisonItemDto::getMarket)
                        .thenComparing(StrategyComparisonItemDto::getStrategyType));
        return stream.collect(Collectors.toList());
    }

    private StrategyComparisonItemDto toItem(GovernanceCheckResult r) {
        String label = STRATEGY_TYPE_LABELS.getOrDefault(r.getStrategyType(), r.getStrategyType());
        return StrategyComparisonItemDto.builder()
                .market(r.getMarket())
                .strategyType(r.getStrategyType())
                .description(label)
                .cagr(null)
                .mddPct(r.getMddPct())
                .sharpeRatio(r.getSharpeRatio())
                .lastRunAt(r.getRunAt())
                .build();
    }
}
