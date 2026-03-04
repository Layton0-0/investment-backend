package com.investment.risk.service;

import com.investment.domain.entity.StrategyPosition;
import com.investment.domain.entity.TradingSetting;
import com.investment.domain.repository.StrategyPositionRepository;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.risk.dto.PerformanceAttributionDto;
import com.investment.strategy.domain.StrategyType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PerformanceAttributionService")
class PerformanceAttributionServiceTest {

    @Mock
    private StrategyPositionRepository strategyPositionRepository;

    @Mock
    private TradingSettingRepository tradingSettingRepository;

    @InjectMocks
    private PerformanceAttributionService service;

    @Test
    @DisplayName("기여도 합계 100% (팩터·전략별)")
    void getAttribution_contributionSumsTo100() {
        TradingSetting setting = TradingSetting.builder()
                .accountNo("1234567890")
                .userId("user1")
                .maxInvestmentAmount(new BigDecimal("100000000"))
                .minInvestmentAmount(new BigDecimal("10000"))
                .defaultCurrency("KRW")
                .build();
        when(tradingSettingRepository.findByUserIdOrderByAccountNo(anyString()))
                .thenReturn(List.of(setting));
        StrategyPosition p1 = closedPosition("A", StrategyType.SHORT_TERM, new BigDecimal("10000"), new BigDecimal("11000"), 10); // pnl 10000
        StrategyPosition p2 = closedPosition("B", StrategyType.SHORT_TERM, new BigDecimal("20000"), new BigDecimal("22000"), 5);  // pnl 10000
        when(strategyPositionRepository.findByAccountNoAndExitDtIsNotNullOrderByExitDtDesc("1234567890"))
                .thenReturn(List.of(p1, p2));

        PerformanceAttributionDto dto = service.getAttribution("user1");

        assertThat(dto.getTotalRealizedPnl()).isEqualByComparingTo("20000"); // 1000*10 + 2000*5
        BigDecimal factorSum = dto.getByFactor().stream()
                .map(PerformanceAttributionDto.FactorContribution::getContributionPct)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(factorSum).isEqualByComparingTo("100");
        BigDecimal strategySum = dto.getByStrategy().stream()
                .map(PerformanceAttributionDto.StrategyContribution::getContributionPct)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(strategySum).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("청산 포지션 없으면 total 0·빈 기여")
    void getAttribution_noClosed_returnsZero() {
        TradingSetting setting = TradingSetting.builder()
                .accountNo("1234567890")
                .userId("user1")
                .maxInvestmentAmount(new BigDecimal("100000000"))
                .minInvestmentAmount(new BigDecimal("10000"))
                .defaultCurrency("KRW")
                .build();
        when(tradingSettingRepository.findByUserIdOrderByAccountNo(anyString()))
                .thenReturn(List.of(setting));
        when(strategyPositionRepository.findByAccountNoAndExitDtIsNotNullOrderByExitDtDesc("1234567890"))
                .thenReturn(List.of());

        PerformanceAttributionDto dto = service.getAttribution("user1");

        assertThat(dto.getTotalRealizedPnl()).isEqualByComparingTo("0");
    }

    private static StrategyPosition closedPosition(String signalType, StrategyType strategyType,
            BigDecimal entry, BigDecimal exit, int qty) {
        StrategyPosition p = StrategyPosition.builder()
                .accountNo("123")
                .symbol("005930")
                .market("KR")
                .strategyType(strategyType)
                .entryDt(LocalDate.now().minusDays(10))
                .entryPrice(entry)
                .quantity(qty)
                .atrMultiplier(new BigDecimal("2"))
                .timeCutDays(5)
                .exitDt(LocalDate.now())
                .exitPrice(exit)
                .signalType(signalType)
                .build();
        return p;
    }
}
