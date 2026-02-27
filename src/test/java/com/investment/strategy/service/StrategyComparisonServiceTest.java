package com.investment.strategy.service;

import com.investment.domain.entity.GovernanceCheckResult;
import com.investment.domain.repository.GovernanceCheckResultRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StrategyComparisonService")
class StrategyComparisonServiceTest {

    @Mock
    private GovernanceCheckResultRepository governanceCheckResultRepository;

    @InjectMocks
    private StrategyComparisonService strategyComparisonService;

    @Test
    @DisplayName("getComparison 결과 없으면 빈 목록")
    void getComparison_empty_returnsEmpty() {
        when(governanceCheckResultRepository.findAllByOrderByRunAtDesc(any())).thenReturn(List.of());

        List<?> result = strategyComparisonService.getComparison(null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getComparison market=KR 시 해당 시장만 반환")
    void getComparison_withMarketKR_filtersByMarket() {
        GovernanceCheckResult r = GovernanceCheckResult.of(
                Instant.now(), "KR", "SHORT_TERM",
                new BigDecimal("-8"), new BigDecimal("1.2"), false,
                LocalDate.now().minusMonths(3), LocalDate.now());
        when(governanceCheckResultRepository.findAllByOrderByRunAtDesc(any())).thenReturn(List.of(r));

        var result = strategyComparisonService.getComparison("KR");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMarket()).isEqualTo("KR");
        assertThat(result.get(0).getStrategyType()).isEqualTo("SHORT_TERM");
        assertThat(result.get(0).getDescription()).isEqualTo("단기");
        assertThat(result.get(0).getMddPct()).isEqualByComparingTo(new BigDecimal("-8"));
        assertThat(result.get(0).getSharpeRatio()).isEqualByComparingTo(new BigDecimal("1.2"));
    }
}
