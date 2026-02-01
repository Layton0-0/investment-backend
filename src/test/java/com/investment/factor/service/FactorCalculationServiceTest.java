package com.investment.factor.service;

import com.investment.domain.entity.DailyStock;
import com.investment.domain.entity.SignalScore;
import com.investment.domain.repository.DailyStockRepository;
import com.investment.domain.repository.FundamentalsRepository;
import com.investment.domain.repository.OrderFlowRepository;
import com.investment.domain.repository.SignalScoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("FactorCalculationService")
class FactorCalculationServiceTest {

    @Mock
    private DailyStockRepository dailyStockRepository;
    @Mock
    private SignalScoreRepository signalScoreRepository;
    @Mock
    private UniverseFilterService universeFilterService;
    @Mock
    private OrderFlowRepository orderFlowRepository;
    @Mock
    private FundamentalsRepository fundamentalsRepository;

    @Captor
    private ArgumentCaptor<List<SignalScore>> saveAllCaptor;

    @InjectMocks
    private FactorCalculationService factorCalculationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(factorCalculationService, "disparityMaDays", 20);
        ReflectionTestUtils.setField(factorCalculationService, "volatilityBreakoutK", new BigDecimal("0.5"));
        ReflectionTestUtils.setField(factorCalculationService, "volatilityBreakoutKDynamic", true);
        ReflectionTestUtils.setField(factorCalculationService, "volatilityBreakoutKMin", new BigDecimal("0.3"));
        ReflectionTestUtils.setField(factorCalculationService, "volatilityBreakoutKMax", new BigDecimal("0.7"));
        ReflectionTestUtils.setField(factorCalculationService, "liquidityMinTrdVal", 1_000_000_000L);
        lenient().when(orderFlowRepository.findByBasDtAndSymbolAndMarket(any(LocalDate.class), anyString(), anyString())).thenReturn(Optional.empty());
        lenient().when(fundamentalsRepository.findByBasDtAndSymbolAndMarket(any(LocalDate.class), anyString(), anyString())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("일별 데이터 없으면 0건 저장")
    void calculateAndSave_noData_returnsZero() {
        when(dailyStockRepository.findByMarketAndBasDtBetween(eq("KR"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        int saved = factorCalculationService.calculateAndSave(LocalDate.of(2026, 1, 29), "KR");

        assertThat(saved).isZero();
        verify(signalScoreRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("일별 데이터·이력 있으면 이격도·변동성돌파·유동성 시그널 저장")
    void calculateAndSave_withHistory_savesSignals() {
        LocalDate basDt = LocalDate.of(2026, 1, 29);
        List<DailyStock> history = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            LocalDate d = basDt.minusDays(24 - i);
            history.add(DailyStock.builder()
                    .basDt(d)
                    .symbol("005930")
                    .market("KR")
                    .openPrice(BigDecimal.valueOf(70000 + i * 100))
                    .highPrice(BigDecimal.valueOf(70500 + i * 100))
                    .lowPrice(BigDecimal.valueOf(69500 + i * 100))
                    .closePrice(BigDecimal.valueOf(70200 + i * 100))
                    .volume(1_000_000L)
                    .trdVal(70_000_000_000L)
                    .build());
        }
        when(dailyStockRepository.findByMarketAndBasDtBetween(eq("KR"), any(LocalDate.class), eq(basDt)))
                .thenReturn(history);
        when(universeFilterService.getSymbols(any(LocalDate.class), anyString())).thenReturn(List.of());
        when(signalScoreRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        int saved = factorCalculationService.calculateAndSave(basDt, "KR");

        verify(signalScoreRepository).saveAll(saveAllCaptor.capture());
        List<SignalScore> savedList = saveAllCaptor.getValue();
        assertThat(saved).isGreaterThan(0);
        assertThat(savedList).isNotEmpty();
        assertThat(savedList.stream().map(SignalScore::getFactorType).distinct().toList())
                .containsAnyOf(FactorCalculationService.FACTOR_DISPARITY,
                        FactorCalculationService.FACTOR_VOLATILITY_BREAKOUT,
                        FactorCalculationService.FACTOR_LIQUIDITY);
    }

    @Test
    @DisplayName("변동성 돌파 k 동적 적용 - 한국장 변동성에 따라 k 조정")
    void calculateAndSave_dynamicK_adjustsK() {
        LocalDate basDt = LocalDate.of(2026, 1, 29);
        List<DailyStock> history = new ArrayList<>();
        // 높은 변동성 데이터 (Range가 큰 경우)
        for (int i = 0; i < 25; i++) {
            LocalDate d = basDt.minusDays(24 - i);
            history.add(DailyStock.builder()
                    .basDt(d)
                    .symbol("005930")
                    .market("KR")
                    .openPrice(BigDecimal.valueOf(70000))
                    .highPrice(BigDecimal.valueOf(80000)) // 큰 Range
                    .lowPrice(BigDecimal.valueOf(60000))
                    .closePrice(BigDecimal.valueOf(75000))
                    .volume(1_000_000L)
                    .trdVal(70_000_000_000L)
                    .build());
        }
        when(dailyStockRepository.findByMarketAndBasDtBetween(eq("KR"), any(LocalDate.class), eq(basDt)))
                .thenReturn(history);
        when(universeFilterService.getSymbols(basDt, "KR")).thenReturn(List.of("005930"));
        when(signalScoreRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        int saved = factorCalculationService.calculateAndSave(basDt, "KR");

        verify(signalScoreRepository).saveAll(saveAllCaptor.capture());
        List<SignalScore> savedList = saveAllCaptor.getValue();
        assertThat(saved).isGreaterThan(0);
        // 변동성 돌파 시그널에서 동적 k가 적용되었는지 확인
        SignalScore volatilitySignal = savedList.stream()
                .filter(s -> FactorCalculationService.FACTOR_VOLATILITY_BREAKOUT.equals(s.getFactorType()))
                .findFirst()
                .orElse(null);
        assertThat(volatilitySignal).isNotNull();
        assertThat(volatilitySignal.getMetadata()).contains("dynamic=true");
    }

    @Test
    @DisplayName("한국 시장 - 수급 강도 팩터 스텁 저장")
    void calculateAndSave_krMarket_savesSmartMoneyIntensityStub() {
        LocalDate basDt = LocalDate.of(2026, 1, 29);
        List<DailyStock> history = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            LocalDate d = basDt.minusDays(24 - i);
            history.add(DailyStock.builder()
                    .basDt(d)
                    .symbol("005930")
                    .market("KR")
                    .openPrice(BigDecimal.valueOf(70000))
                    .highPrice(BigDecimal.valueOf(70500))
                    .lowPrice(BigDecimal.valueOf(69500))
                    .closePrice(BigDecimal.valueOf(70200))
                    .volume(1_000_000L)
                    .trdVal(70_000_000_000L)
                    .build());
        }
        when(dailyStockRepository.findByMarketAndBasDtBetween(eq("KR"), any(LocalDate.class), eq(basDt)))
                .thenReturn(history);
        when(universeFilterService.getSymbols(basDt, "KR")).thenReturn(List.of("005930"));
        when(signalScoreRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        int saved = factorCalculationService.calculateAndSave(basDt, "KR");

        verify(signalScoreRepository).saveAll(saveAllCaptor.capture());
        List<SignalScore> savedList = saveAllCaptor.getValue();
        assertThat(saved).isGreaterThan(0);
        // 수급 강도 스텁 시그널 확인
        SignalScore smartMoneySignal = savedList.stream()
                .filter(s -> FactorCalculationService.FACTOR_SMART_MONEY_INTENSITY.equals(s.getFactorType()))
                .findFirst()
                .orElse(null);
        assertThat(smartMoneySignal).isNotNull();
        assertThat(smartMoneySignal.getMetadata()).contains("dataUnavailable");
    }

    @Test
    @DisplayName("미국 시장 - 듀얼 모멘텀·퀄리티-성장 팩터 저장 (듀얼 모멘텀은 데이터 충분 시 실제 계산)")
    void calculateAndSave_usMarket_savesDualMomentumAndQualityGrowth() {
        LocalDate basDt = LocalDate.of(2026, 1, 29);
        List<DailyStock> history = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            LocalDate d = basDt.minusDays(24 - i);
            history.add(DailyStock.builder()
                    .basDt(d)
                    .symbol("AAPL")
                    .market("US")
                    .openPrice(BigDecimal.valueOf(150))
                    .highPrice(BigDecimal.valueOf(155))
                    .lowPrice(BigDecimal.valueOf(145))
                    .closePrice(BigDecimal.valueOf(150))
                    .volume(10_000_000L)
                    .trdVal(1_500_000_000L)
                    .build());
        }
        when(dailyStockRepository.findByMarketAndBasDtBetween(eq("US"), any(LocalDate.class), eq(basDt)))
                .thenReturn(history);
        when(universeFilterService.getSymbols(basDt, "US")).thenReturn(List.of("AAPL"));
        when(signalScoreRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        int saved = factorCalculationService.calculateAndSave(basDt, "US");

        verify(signalScoreRepository).saveAll(saveAllCaptor.capture());
        List<SignalScore> savedList = saveAllCaptor.getValue();
        assertThat(saved).isGreaterThan(0);
        // 듀얼 모멘텀: 실제 계산(metadata에 momentum=) 또는 데이터 부족(insufficientData)
        SignalScore dualMomentumSignal = savedList.stream()
                .filter(s -> FactorCalculationService.FACTOR_DUAL_MOMENTUM.equals(s.getFactorType()))
                .findFirst()
                .orElse(null);
        assertThat(dualMomentumSignal).isNotNull();
        assertThat(dualMomentumSignal.getMetadata().contains("momentum=") || dualMomentumSignal.getMetadata().contains("insufficientData")).isTrue();

        // 퀄리티-성장 스텁 시그널 확인
        SignalScore qualityGrowthSignal = savedList.stream()
                .filter(s -> FactorCalculationService.FACTOR_QUALITY_GROWTH.equals(s.getFactorType()))
                .findFirst()
                .orElse(null);
        assertThat(qualityGrowthSignal).isNotNull();
        assertThat(qualityGrowthSignal.getMetadata()).contains("dataUnavailable");
    }

    @Test
    @DisplayName("미국 시장 - 듀얼 모멘텀 실제 계산 (기간별 수익률 가중합·시장 대비)")
    void calculateAndSave_usMarket_dualMomentumRealCalculation() {
        LocalDate basDt = LocalDate.of(2026, 2, 1);
        List<DailyStock> history = new ArrayList<>();
        // 130일 이력: 21/63/126일 전 종가 확보 가능
        for (int i = 0; i < 130; i++) {
            LocalDate d = basDt.minusDays(129 - i);
            long close = 100 + (i / 10); // 상승 추세
            history.add(DailyStock.builder()
                    .basDt(d)
                    .symbol("AAPL")
                    .market("US")
                    .openPrice(BigDecimal.valueOf(close - 1))
                    .highPrice(BigDecimal.valueOf(close + 1))
                    .lowPrice(BigDecimal.valueOf(close - 2))
                    .closePrice(BigDecimal.valueOf(close))
                    .volume(10_000_000L)
                    .trdVal((long) close * 10_000_000)
                    .build());
        }
        when(dailyStockRepository.findByMarketAndBasDtBetween(eq("US"), any(LocalDate.class), eq(basDt)))
                .thenReturn(history);
        when(universeFilterService.getSymbols(basDt, "US")).thenReturn(List.of("AAPL"));
        when(signalScoreRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        ReflectionTestUtils.setField(factorCalculationService, "dualMomentumPeriodDays", "21,63,126");
        ReflectionTestUtils.setField(factorCalculationService, "dualMomentumWeights", "0.5,0.3,0.2");

        int saved = factorCalculationService.calculateAndSave(basDt, "US");

        verify(signalScoreRepository).saveAll(saveAllCaptor.capture());
        List<SignalScore> savedList = saveAllCaptor.getValue();
        SignalScore dualMomentumSignal = savedList.stream()
                .filter(s -> FactorCalculationService.FACTOR_DUAL_MOMENTUM.equals(s.getFactorType()))
                .findFirst()
                .orElse(null);
        assertThat(dualMomentumSignal).isNotNull();
        assertThat(dualMomentumSignal.getMetadata()).contains("momentum=");
        assertThat(dualMomentumSignal.getScore()).isNotNull();
    }
}
