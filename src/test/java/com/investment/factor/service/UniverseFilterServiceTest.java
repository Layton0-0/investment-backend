package com.investment.factor.service;

import com.investment.domain.entity.DailyStock;
import com.investment.domain.entity.Universe;
import com.investment.domain.repository.DailyStockRepository;
import com.investment.domain.repository.EarningsSurpriseRepository;
import com.investment.domain.repository.SectorReturnRepository;
import com.investment.domain.repository.SymbolSectorRepository;
import com.investment.domain.repository.UniverseRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("UniverseFilterService")
class UniverseFilterServiceTest {

    @Mock
    private DailyStockRepository dailyStockRepository;
    @Mock
    private UniverseRepository universeRepository;
    @Mock
    private SectorReturnRepository sectorReturnRepository;
    @Mock
    private SymbolSectorRepository symbolSectorRepository;
    @Mock
    private EarningsSurpriseRepository earningsSurpriseRepository;

    @InjectMocks
    private UniverseFilterService universeFilterService;

    @Captor
    private ArgumentCaptor<List<Universe>> saveAllCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(universeFilterService, "liquidityMinTrdVal", 1_000_000_000L);
        ReflectionTestUtils.setField(universeFilterService, "sectorRsTopN", 5);
        ReflectionTestUtils.setField(universeFilterService, "earningsSurpriseLookbackDays", 90);
        ReflectionTestUtils.setField(universeFilterService, "earningsSurpriseTopPct", 0.2);
        lenient().when(sectorReturnRepository.findByBasDtAndMarketOrderByReturnPctDesc(any(LocalDate.class), anyString())).thenReturn(List.of());
        lenient().when(earningsSurpriseRepository.findByMarketAndReportDtGreaterThanEqualOrderBySurpriseScoreDesc(anyString(), any(LocalDate.class))).thenReturn(List.of());
    }

    @Test
    @DisplayName("유동성 통과 종목만 유니버스에 저장")
    void run_liquidityFilter_savesPassedStocks() {
        // given
        LocalDate basDt = LocalDate.of(2026, 1, 30);
        String market = "KR";

        DailyStock passed = DailyStock.builder()
                .basDt(basDt)
                .symbol("005930")
                .market(market)
                .trdVal(2_000_000_000L) // 유동성 통과
                .build();

        DailyStock failed = DailyStock.builder()
                .basDt(basDt)
                .symbol("000001")
                .market(market)
                .trdVal(500_000_000L) // 유동성 미통과
                .build();

        when(dailyStockRepository.findByBasDtAndMarketAndTrdValGreaterThanEqual(
                eq(basDt), eq(market), eq(1_000_000_000L)))
                .thenReturn(List.of(passed));

        doNothing().when(universeRepository).deleteByBasDtAndMarket(basDt, market);
        when(universeRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        // when
        int count = universeFilterService.run(basDt, market);

        // then
        assertThat(count).isEqualTo(1);
        verify(universeRepository).saveAll(saveAllCaptor.capture());
        List<Universe> saved = saveAllCaptor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getSymbol()).isEqualTo("005930");
        verify(universeRepository).deleteByBasDtAndMarket(basDt, market);
    }

    @Test
    @DisplayName("한국 시장 - Sector RS 필터 적용 (데이터 없으면 유동성만)")
    void run_krMarket_appliesSectorRSFallback() {
        // given
        LocalDate basDt = LocalDate.of(2026, 1, 30);
        String market = "KR";

        DailyStock stock = DailyStock.builder()
                .basDt(basDt)
                .symbol("005930")
                .market(market)
                .trdVal(2_000_000_000L)
                .build();

        when(dailyStockRepository.findByBasDtAndMarketAndTrdValGreaterThanEqual(
                eq(basDt), eq(market), anyLong()))
                .thenReturn(List.of(stock));

        doNothing().when(universeRepository).deleteByBasDtAndMarket(basDt, market);
        when(universeRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        // when
        int count = universeFilterService.run(basDt, market);

        // then
        assertThat(count).isEqualTo(1); // 스텁이므로 유동성 통과 종목만 반환
    }

    @Test
    @DisplayName("미국 시장 - Post-Earnings Drift 필터 적용 (데이터 없으면 유동성만)")
    void run_usMarket_appliesPostEarningsFallback() {
        // given
        LocalDate basDt = LocalDate.of(2026, 1, 30);
        String market = "US";

        DailyStock stock = DailyStock.builder()
                .basDt(basDt)
                .symbol("AAPL")
                .market(market)
                .trdVal(5_000_000_000L)
                .build();

        when(dailyStockRepository.findByBasDtAndMarketAndTrdValGreaterThanEqual(
                eq(basDt), eq(market), anyLong()))
                .thenReturn(List.of(stock));

        doNothing().when(universeRepository).deleteByBasDtAndMarket(basDt, market);
        when(universeRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        // when
        int count = universeFilterService.run(basDt, market);

        // then
        assertThat(count).isEqualTo(1); // 스텁이므로 유동성 통과 종목만 반환
    }

    @Test
    @DisplayName("유니버스 종목 코드 목록 조회")
    void getSymbols_returnsSymbolList() {
        // given
        LocalDate basDt = LocalDate.of(2026, 1, 30);
        String market = "KR";

        Universe universe1 = Universe.builder()
                .basDt(basDt)
                .market(market)
                .symbol("005930")
                .build();
        Universe universe2 = Universe.builder()
                .basDt(basDt)
                .market(market)
                .symbol("000660")
                .build();

        when(universeRepository.findByBasDtAndMarketOrderBySymbol(basDt, market))
                .thenReturn(List.of(universe1, universe2));

        // when
        List<String> symbols = universeFilterService.getSymbols(basDt, market);

        // then
        assertThat(symbols).hasSize(2);
        assertThat(symbols).containsExactly("005930", "000660");
    }
}
