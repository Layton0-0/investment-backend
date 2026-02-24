package com.investment.strategy.service;

import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.domain.entity.Strategy;
import com.investment.domain.entity.TradingSetting;
import com.investment.domain.repository.StrategyRepository;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.strategy.domain.StrategyStatus;
import com.investment.strategy.domain.StrategyType;
import com.investment.strategy.dto.StrategyDto;
import com.investment.strategy.dto.StrategyStatusUpdateDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StrategyManagementService")
class StrategyManagementServiceTest {

        @Mock
        private StrategyRepository strategyRepository;

        @Mock
        private TradingSettingRepository tradingSettingRepository;

        @InjectMocks
        private StrategyManagementService strategyManagementService;

        @Test
        @DisplayName("getStrategies 계좌별 전략 목록 조회 성공")
        void getStrategies_returnsList() throws Exception {
                String accountNo = "12345678-12";
                Strategy strategy = Strategy.builder()
                                .accountNo(accountNo)
                                .market("KR")
                                .strategyType(StrategyType.SHORT_TERM)
                                .status(StrategyStatus.ACTIVE)
                                .build();
                java.lang.reflect.Field idField = Strategy.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(strategy, "strat-1");
                when(strategyRepository.findByAccountNo(accountNo)).thenReturn(List.of(strategy));

                List<StrategyDto> result = strategyManagementService.getStrategies(accountNo, null);

                assertNotNull(result);
                assertEquals(1, result.size());
                assertEquals("strat-1", result.get(0).getStrategyId());
                assertEquals("KR", result.get(0).getMarket());
                assertEquals(StrategyType.SHORT_TERM, result.get(0).getStrategyType());
        }

        @Test
        @DisplayName("getStrategies market 지정 시 시장별 조회 (기존 전략 있으면 그대로 반환)")
        void getStrategies_withMarket_filtersByMarket() {
                String accountNo = "12345678-12";
                Strategy one = Strategy.builder()
                                .accountNo(accountNo)
                                .market("US")
                                .strategyType(StrategyType.MEDIUM_TERM)
                                .status(StrategyStatus.ACTIVE)
                                .build();
                when(strategyRepository.findByAccountNoAndMarket(accountNo, "US")).thenReturn(List.of(one));

                List<StrategyDto> result = strategyManagementService.getStrategies(accountNo, "US");

                assertNotNull(result);
                assertEquals(1, result.size());
                verify(strategyRepository).findByAccountNoAndMarket(accountNo, "US");
                verify(tradingSettingRepository, never()).findByAccountNo(any());
        }

        @Test
        @DisplayName("getStrategies market 지정 시 비어 있으면 시스템 기본 3건 ensure 후 반환")
        void getStrategies_withMarket_empty_ensuresDefaultsAndReturnsThree() throws Exception {
                String accountNo = "12345678-12";
                when(strategyRepository.findByAccountNoAndMarket(accountNo, "KR")).thenReturn(List.of());
                when(tradingSettingRepository.findByAccountNo(accountNo)).thenReturn(Optional.empty());
                when(strategyRepository.findByAccountNoAndMarketAndStrategyType(eq(accountNo), eq("KR"), any()))
                                .thenReturn(Optional.empty());
                Strategy saved = Strategy.builder()
                                .accountNo(accountNo)
                                .market("KR")
                                .strategyType(StrategyType.SHORT_TERM)
                                .status(StrategyStatus.ACTIVE)
                                .build();
                java.lang.reflect.Field idField = Strategy.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(saved, "gen-1");
                when(strategyRepository.save(any(Strategy.class))).thenReturn(saved);

                List<Strategy> three = List.of(
                                Strategy.builder().accountNo(accountNo).market("KR").strategyType(StrategyType.SHORT_TERM).status(StrategyStatus.ACTIVE).build(),
                                Strategy.builder().accountNo(accountNo).market("KR").strategyType(StrategyType.MEDIUM_TERM).status(StrategyStatus.ACTIVE).build(),
                                Strategy.builder().accountNo(accountNo).market("KR").strategyType(StrategyType.LONG_TERM).status(StrategyStatus.ACTIVE).build());
                when(strategyRepository.findByAccountNoAndMarket(accountNo, "KR"))
                                .thenReturn(List.of())
                                .thenReturn(three);

                List<StrategyDto> result = strategyManagementService.getStrategies(accountNo, "KR");

                assertNotNull(result);
                assertEquals(3, result.size());
                verify(strategyRepository, times(2)).findByAccountNoAndMarket(accountNo, "KR");
                verify(strategyRepository, atLeastOnce()).save(any(Strategy.class));
        }

        @Test
        @DisplayName("getStrategy 전략 없을 때 DomainException")
        void getStrategy_notFound_throwsDomainException() {
                when(strategyRepository.findByAccountNoAndMarketAndStrategyType(eq("12345678-12"), eq("KR"),
                                eq(StrategyType.SHORT_TERM)))
                                .thenReturn(Optional.empty());

                DomainException ex = assertThrows(DomainException.class,
                                () -> strategyManagementService.getStrategy("12345678-12", "KR",
                                                StrategyType.SHORT_TERM));

                assertEquals(ErrorCode.SETTING_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("saveStrategy 새 전략 저장 성공")
        void saveStrategy_newStrategy_saves() {
                StrategyDto dto = StrategyDto.builder()
                                .accountNo("12345678-12")
                                .market("KR")
                                .strategyType(StrategyType.SHORT_TERM)
                                .status(StrategyStatus.ACTIVE)
                                .maxInvestmentAmount(new BigDecimal("1000000"))
                                .build();
                when(strategyRepository.findByAccountNoAndMarketAndStrategyType(any(), any(), any()))
                                .thenReturn(Optional.empty());
                Strategy saved = Strategy.builder()
                                .accountNo(dto.getAccountNo())
                                .market("KR")
                                .strategyType(dto.getStrategyType())
                                .status(dto.getStatus())
                                .maxInvestmentAmount(dto.getMaxInvestmentAmount())
                                .build();
                try {
                        java.lang.reflect.Field idField = Strategy.class.getDeclaredField("id");
                        idField.setAccessible(true);
                        idField.set(saved, "new-id");
                } catch (Exception e) {
                        throw new RuntimeException(e);
                }
                when(strategyRepository.save(any(Strategy.class))).thenReturn(saved);

                StrategyDto result = strategyManagementService.saveStrategy(dto);

                assertNotNull(result);
                assertEquals("new-id", result.getStrategyId());
                verify(strategyRepository).save(any(Strategy.class));
        }

        @Test
        @DisplayName("updateStrategyStatus ACTIVE로 변경 성공")
        void updateStrategyStatus_toActive_updates() throws Exception {
                String accountNo = "12345678-12";
                Strategy strategy = Strategy.builder()
                                .accountNo(accountNo)
                                .market("KR")
                                .strategyType(StrategyType.SHORT_TERM)
                                .status(StrategyStatus.STOPPED)
                                .build();
                java.lang.reflect.Field idField = Strategy.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(strategy, "s1");
                when(strategyRepository.findByAccountNoAndMarketAndStrategyType(accountNo, "KR",
                                StrategyType.SHORT_TERM))
                                .thenReturn(Optional.of(strategy));
                when(strategyRepository.save(any(Strategy.class))).thenReturn(strategy);

                StrategyStatusUpdateDto dto = StrategyStatusUpdateDto.builder().status(StrategyStatus.ACTIVE).build();
                StrategyDto result = strategyManagementService.updateStrategyStatus(accountNo, "KR",
                                StrategyType.SHORT_TERM, dto);

                assertNotNull(result);
                verify(strategyRepository).save(any(Strategy.class));
        }
}
