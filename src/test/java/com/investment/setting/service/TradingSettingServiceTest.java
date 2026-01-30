package com.investment.setting.service;

import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.domain.entity.TradingSetting;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.setting.dto.TradingSettingDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradingSettingServiceTest {

        @Mock
        private TradingSettingRepository tradingSettingRepository;

        @InjectMocks
        private TradingSettingService tradingSettingService;

        private TradingSettingDto settingDto;

        @BeforeEach
        void setUp() {
                settingDto = TradingSettingDto.builder()
                                .maxInvestmentAmount(new BigDecimal("1000000"))
                                .minInvestmentAmount(new BigDecimal("10000"))
                                .defaultCurrency("USD")
                                .autoTradingEnabled(false)
                                .riskLevel(new BigDecimal("0.5"))
                                .build();
        }

        @Test
        void 설정_조회_성공() {
                // given
                TradingSetting setting = TradingSetting.builder()
                                .accountNo("1234567890")
                                .maxInvestmentAmount(settingDto.getMaxInvestmentAmount())
                                .minInvestmentAmount(settingDto.getMinInvestmentAmount())
                                .defaultCurrency(settingDto.getDefaultCurrency())
                                .autoTradingEnabled(settingDto.getAutoTradingEnabled())
                                .riskLevel(settingDto.getRiskLevel())
                                .build();

                when(tradingSettingRepository.findByAccountNo("1234567890"))
                                .thenReturn(Optional.of(setting));

                // when
                TradingSettingDto result = tradingSettingService.getSetting("1234567890");

                // then
                assertNotNull(result);
                assertEquals(settingDto.getMaxInvestmentAmount(), result.getMaxInvestmentAmount());
                assertEquals(settingDto.getMinInvestmentAmount(), result.getMinInvestmentAmount());
        }

        @Test
        void 설정_조회_실패_없는_설정() {
                // given
                when(tradingSettingRepository.findByAccountNo("1234567890"))
                                .thenReturn(Optional.empty());

                // when & then
                DomainException exception = assertThrows(DomainException.class,
                                () -> tradingSettingService.getSetting("1234567890"));

                assertEquals(ErrorCode.SETTING_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        void getSettingOptional_있으면_DTO_반환() {
                // given
                TradingSetting setting = TradingSetting.builder()
                                .accountNo("1234567890")
                                .maxInvestmentAmount(settingDto.getMaxInvestmentAmount())
                                .minInvestmentAmount(settingDto.getMinInvestmentAmount())
                                .defaultCurrency(settingDto.getDefaultCurrency())
                                .autoTradingEnabled(settingDto.getAutoTradingEnabled())
                                .riskLevel(settingDto.getRiskLevel())
                                .build();
                when(tradingSettingRepository.findByAccountNo("1234567890"))
                                .thenReturn(Optional.of(setting));

                // when
                Optional<TradingSettingDto> result = tradingSettingService.getSettingOptional("1234567890");

                // then
                assertTrue(result.isPresent());
                assertEquals(settingDto.getMaxInvestmentAmount(), result.get().getMaxInvestmentAmount());
                assertEquals(settingDto.getMinInvestmentAmount(), result.get().getMinInvestmentAmount());
        }

        @Test
        void getSettingOptional_없으면_empty() {
                // given
                when(tradingSettingRepository.findByAccountNo("1234567890"))
                                .thenReturn(Optional.empty());

                // when
                Optional<TradingSettingDto> result = tradingSettingService.getSettingOptional("1234567890");

                // then
                assertTrue(result.isEmpty());
        }

        @Test
        void 설정_저장_성공_신규() {
                // given
                when(tradingSettingRepository.findByAccountNo("1234567890"))
                                .thenReturn(Optional.empty());

                TradingSetting savedSetting = TradingSetting.builder()
                                .accountNo("1234567890")
                                .maxInvestmentAmount(settingDto.getMaxInvestmentAmount())
                                .minInvestmentAmount(settingDto.getMinInvestmentAmount())
                                .defaultCurrency(settingDto.getDefaultCurrency())
                                .autoTradingEnabled(settingDto.getAutoTradingEnabled())
                                .riskLevel(settingDto.getRiskLevel())
                                .build();

                when(tradingSettingRepository.save(any(TradingSetting.class)))
                                .thenReturn(savedSetting);

                // when
                TradingSettingDto result = tradingSettingService.saveSetting("1234567890", settingDto);

                // then
                assertNotNull(result);
                verify(tradingSettingRepository).save(any(TradingSetting.class));
        }

        @Test
        void 설정_저장_실패_최대금액_미만() {
                // given: 최대 < 최소
                TradingSettingDto invalidDto = TradingSettingDto.builder()
                                .maxInvestmentAmount(new BigDecimal("5000"))
                                .minInvestmentAmount(new BigDecimal("10000"))
                                .defaultCurrency("KRW")
                                .autoTradingEnabled(false)
                                .riskLevel(new BigDecimal("0.5"))
                                .build();

                // when & then
                DomainException exception = assertThrows(DomainException.class,
                                () -> tradingSettingService.saveSetting("1234567890", invalidDto));

                assertEquals(ErrorCode.INVALID_SETTING_VALUE, exception.getErrorCode());
                verify(tradingSettingRepository, never()).save(any(TradingSetting.class));
        }

        @Test
        void 설정_저장_실패_리스크레벨_범위초과() {
                // given: riskLevel 1.0 초과
                TradingSettingDto invalidRiskDto = TradingSettingDto.builder()
                                .maxInvestmentAmount(new BigDecimal("1000000"))
                                .minInvestmentAmount(new BigDecimal("10000"))
                                .defaultCurrency("KRW")
                                .autoTradingEnabled(false)
                                .riskLevel(new BigDecimal("1.5"))
                                .build();

                // when & then
                DomainException exception = assertThrows(DomainException.class,
                                () -> tradingSettingService.saveSetting("1234567890", invalidRiskDto));

                assertEquals(ErrorCode.INVALID_SETTING_VALUE, exception.getErrorCode());
        }
}
