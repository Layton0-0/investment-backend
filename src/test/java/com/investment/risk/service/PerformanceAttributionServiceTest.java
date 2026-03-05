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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PerformanceAttributionService")
class PerformanceAttributionServiceTest {

    @Mock
    private TradingSettingRepository tradingSettingRepository;

    @Mock
    private StrategyPositionRepository strategyPositionRepository;

    @InjectMocks
    private PerformanceAttributionService service;

    @Test
    @DisplayName("청산 포지션 없으면 totalPnl 0, 빈 기여 맵")
    void getAttribution_noClosedPositions_returnsZeroAndEmptyMaps() {
        TradingSetting s = TradingSetting.builder()
                .accountNo("12345").userId("user1")
                .maxInvestmentAmount(new BigDecimal("10000000")).minInvestmentAmount(BigDecimal.ZERO).defaultCurrency("KRW")
                .build();
        when(tradingSettingRepository.findByUserIdOrderByAccountNo("user1")).thenReturn(List.of(s));
        when(strategyPositionRepository.findByAccountNoAndExitDtIsNotNullOrderByExitDtDesc("12345"))
                .thenReturn(List.of());

        PerformanceAttributionDto dto = service.getAttribution("user1");

        assertThat(dto.getTotalRealizedPnl()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getBySignalType()).isEmpty();
        assertThat(dto.getByStrategyType()).isEmpty();
    }

    @Test
    @DisplayName("청산 포지션 있으면 signalType·strategyType별 기여율 합계 100%")
    void getAttribution_withClosedPositions_contributionSumsTo100() {
        TradingSetting s = TradingSetting.builder()
                .accountNo("12345").userId("user1")
                .maxInvestmentAmount(new BigDecimal("10000000")).minInvestmentAmount(BigDecimal.ZERO).defaultCurrency("KRW")
                .build();
        when(tradingSettingRepository.findByUserIdOrderByAccountNo("user1")).thenReturn(List.of(s));

        StrategyPosition p1 = StrategyPosition.builder()
                .accountNo("12345").symbol("005930").market("KR").strategyType(StrategyType.SHORT_TERM)
                .entryDt(LocalDate.now().minusDays(10)).entryPrice(new BigDecimal("70000")).quantity(10)
                .exitDt(LocalDate.now()).exitPrice(new BigDecimal("77000"))
                .signalType("VOLATILITY_BREAKOUT").createdAt(LocalDateTime.now()).atrMultiplier(new BigDecimal("2")).timeCutDays(5)
                .build();
        StrategyPosition p2 = StrategyPosition.builder()
                .accountNo("12345").symbol("000660").market("KR").strategyType(StrategyType.MEDIUM_TERM)
                .entryDt(LocalDate.now().minusDays(20)).entryPrice(new BigDecimal("100000")).quantity(5)
                .exitDt(LocalDate.now()).exitPrice(new BigDecimal("90000"))
                .signalType("DUAL_MOMENTUM").createdAt(LocalDateTime.now()).atrMultiplier(new BigDecimal("2")).timeCutDays(10)
                .build();
        when(strategyPositionRepository.findByAccountNoAndExitDtIsNotNullOrderByExitDtDesc("12345"))
                .thenReturn(List.of(p1, p2));

        PerformanceAttributionDto dto = service.getAttribution("user1");

        BigDecimal totalPnl = new BigDecimal("70000").add(new BigDecimal("-50000")); // 70*10 - 50*10 = 20000, second: (90-100)*5 = -50000 -> total 20000
        assertThat(dto.getTotalRealizedPnl()).isEqualByComparingTo(totalPnl);

        Map<String, BigDecimal> bySignal = dto.getBySignalType();
        Map<String, BigDecimal> byStrategy = dto.getByStrategyType();
        BigDecimal signalSum = bySignal.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal strategySum = byStrategy.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(signalSum).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(strategySum).isEqualByComparingTo(new BigDecimal("100"));
    }

    @Test
    @DisplayName("사용자 계좌 없으면 0·빈 맵 반환")
    void getAttribution_noSettings_returnsZeroAndEmpty() {
        when(tradingSettingRepository.findByUserIdOrderByAccountNo(anyString())).thenReturn(List.of());

        PerformanceAttributionDto dto = service.getAttribution("user1");

        assertThat(dto.getTotalRealizedPnl()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getBySignalType()).isEmpty();
        assertThat(dto.getByStrategyType()).isEmpty();
    }
}
