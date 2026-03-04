package com.investment.factor.service;

import com.investment.domain.entity.StrategyPosition;
import com.investment.domain.entity.TradingSetting;
import com.investment.strategy.domain.StrategyType;
import com.investment.domain.repository.StrategyPositionRepository;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.domain.repository.UniverseRepository;
import com.investment.factor.dto.PipelineSummaryDto;
import com.investment.factor.dto.SignalScorePageResponseDto;
import com.investment.marketdata.service.RealtimeMarketDataService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PipelineSummaryService")
class PipelineSummaryServiceTest {

        @Mock
        private UniverseRepository universeRepository;
        @Mock
        private SignalScoreService signalScoreService;
        @Mock
        private StrategyPositionRepository strategyPositionRepository;
        @Mock
        private TradingSettingRepository tradingSettingRepository;
        @Mock
        private RealtimeMarketDataService realtimeMarketDataService;

        @InjectMocks
        private PipelineSummaryService pipelineSummaryService;

        @Test
        @DisplayName("getSummary 기준일·계좌별 유니버스·시그널·보유 포지션 수 반환")
        void getSummary_returnsCountsAndLists() {
                LocalDate basDt = LocalDate.of(2026, 1, 30);
                String accountNo = "12345678-01";

                when(universeRepository.countByBasDtAndMarket(basDt, "KR")).thenReturn(120L);
                when(universeRepository.countByBasDtAndMarket(basDt, "US")).thenReturn(80L);
                when(signalScoreService.countSignals(basDt, "KR")).thenReturn(15L);
                when(signalScoreService.countSignals(basDt, "US")).thenReturn(10L);
                when(signalScoreService.getSignals(eq(basDt), eq("KR"), isNull(), isNull(), eq(0), eq(10)))
                                .thenReturn(SignalScorePageResponseDto.builder().content(Collections.emptyList())
                                                .build());
                when(signalScoreService.getSignals(eq(basDt), eq("US"), isNull(), isNull(), eq(0), eq(10)))
                                .thenReturn(SignalScorePageResponseDto.builder().content(Collections.emptyList())
                                                .build());
                when(strategyPositionRepository.countByAccountNoAndExitDtIsNull(accountNo)).thenReturn(2L);
                when(strategyPositionRepository.findByAccountNoAndExitDtIsNullOrderByEntryDtAsc(accountNo))
                                .thenReturn(List.of(
                                                StrategyPosition.builder()
                                                                .id(1L)
                                                                .accountNo(accountNo)
                                                                .symbol("005930")
                                                                .market("KR")
                                                                .strategyType(StrategyType.SHORT_TERM)
                                                                .entryDt(basDt.minusDays(1))
                                                                .entryPrice(new BigDecimal("70000"))
                                                                .quantity(10)
                                                                .build(),
                                                StrategyPosition.builder()
                                                                .id(2L)
                                                                .accountNo(accountNo)
                                                                .symbol("AAPL")
                                                                .market("US")
                                                                .strategyType(StrategyType.MEDIUM_TERM)
                                                                .entryDt(basDt.minusDays(2))
                                                                .entryPrice(new BigDecimal("180"))
                                                                .quantity(5)
                                                                .build()));
                when(tradingSettingRepository.findByAccountNo(accountNo)).thenReturn(Optional.empty());
                when(realtimeMarketDataService.getCurrentPrices(anyList())).thenReturn(Mono.just(Collections.emptyList()));

                PipelineSummaryDto result = pipelineSummaryService.getSummary(basDt, accountNo);

                assertThat(result.getBasDt()).isEqualTo(basDt);
                assertThat(result.getUniverseCountKr()).isEqualTo(120L);
                assertThat(result.getUniverseCountUs()).isEqualTo(80L);
                assertThat(result.getSignalCountKr()).isEqualTo(15L);
                assertThat(result.getSignalCountUs()).isEqualTo(10L);
                assertThat(result.getOpenPositionCount()).isEqualTo(2);
                assertThat(result.getOpenPositionList()).hasSize(2);
                assertThat(result.getOpenPositionList().get(0).getSymbol()).isEqualTo("005930");
                assertThat(result.getOpenPositionList().get(0).getMarket()).isEqualTo("KR");
                assertThat(result.getOpenPositionList().get(1).getSymbol()).isEqualTo("AAPL");
                assertThat(result.getOpenPositionList().get(1).getMarket()).isEqualTo("US");

                verify(universeRepository).countByBasDtAndMarket(basDt, "KR");
                verify(universeRepository).countByBasDtAndMarket(basDt, "US");
                verify(strategyPositionRepository).countByAccountNoAndExitDtIsNull(accountNo);
        }

        @Test
        @DisplayName("getSummary accountNo null이면 보유 포지션 0건·빈 목록·배분 요약 null")
        void getSummary_nullAccountNo_returnsZeroPositions() {
                LocalDate basDt = LocalDate.of(2026, 1, 30);

                when(universeRepository.countByBasDtAndMarket(any(), anyString())).thenReturn(0L);
                when(signalScoreService.countSignals(any(), anyString())).thenReturn(0L);
                when(signalScoreService.getSignals(any(), anyString(), isNull(), isNull(), eq(0), eq(10)))
                                .thenReturn(SignalScorePageResponseDto.builder().content(Collections.emptyList())
                                                .build());

                PipelineSummaryDto result = pipelineSummaryService.getSummary(basDt, null);

                assertThat(result.getOpenPositionCount()).isEqualTo(0);
                assertThat(result.getOpenPositionList()).isEmpty();
                assertThat(result.getAllocationSummary()).isNull();
                verify(strategyPositionRepository, never()).countByAccountNoAndExitDtIsNull(any());
                verify(strategyPositionRepository, never()).findByAccountNoAndExitDtIsNullOrderByEntryDtAsc(any());
                verify(tradingSettingRepository, never()).findByAccountNo(any());
        }

        @Test
        @DisplayName("getSummary 거래 설정 있으면 allocationSummary 반환")
        void getSummary_withTradingSetting_returnsAllocationSummary() {
                LocalDate basDt = LocalDate.of(2026, 1, 30);
                String accountNo = "12345678-01";
                TradingSetting setting = new TradingSetting(
                                accountNo, "user1",
                                new BigDecimal("100000000"), new BigDecimal("1000000"), "KR", false, false, null,
                                new BigDecimal("0.2"), new BigDecimal("0.4"), new BigDecimal("0.4"),
                                null, null);
                when(universeRepository.countByBasDtAndMarket(any(), anyString())).thenReturn(0L);
                when(signalScoreService.countSignals(any(), anyString())).thenReturn(0L);
                when(signalScoreService.getSignals(any(), anyString(), isNull(), isNull(), eq(0), eq(10)))
                                .thenReturn(SignalScorePageResponseDto.builder().content(Collections.emptyList())
                                                .build());
                when(strategyPositionRepository.countByAccountNoAndExitDtIsNull(accountNo)).thenReturn(0L);
                when(strategyPositionRepository.findByAccountNoAndExitDtIsNullOrderByEntryDtAsc(accountNo))
                                .thenReturn(Collections.emptyList());
                when(tradingSettingRepository.findByAccountNo(accountNo)).thenReturn(Optional.of(setting));

                PipelineSummaryDto result = pipelineSummaryService.getSummary(basDt, accountNo);

                assertThat(result.getAllocationSummary()).isNotNull();
                assertThat(result.getAllocationSummary()).contains("단기").contains("중기").contains("장기");
                assertThat(result.getAllocationSummary()).contains("2,000만").contains("4,000만");
                assertThat(result.getAllocationRatioSummary()).isNotNull();
                assertThat(result.getAllocationRatioSummary()).contains("단기 20%").contains("중기 40%").contains("장기 40%");
        }

        @Test
        @DisplayName("getSummary 유니버스 조회 예외 시 해당 블록 0으로 폴백")
        void getSummary_universeException_returnsZeroForFailingBlock() {
                LocalDate basDt = LocalDate.of(2026, 1, 30);
                when(universeRepository.countByBasDtAndMarket(basDt, "KR")).thenThrow(new RuntimeException("db error"));
                when(signalScoreService.countSignals(any(), anyString())).thenReturn(0L);
                when(signalScoreService.getSignals(any(), anyString(), isNull(), isNull(), eq(0), eq(10)))
                                .thenReturn(SignalScorePageResponseDto.builder().content(Collections.emptyList())
                                                .build());

                PipelineSummaryDto result = pipelineSummaryService.getSummary(basDt, null);

                assertThat(result.getUniverseCountKr()).isEqualTo(0);
                assertThat(result.getUniverseCountUs()).isEqualTo(0);
        }
}
