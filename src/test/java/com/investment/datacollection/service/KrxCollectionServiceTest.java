package com.investment.datacollection.service;

import com.investment.config.DataCollectionProperties;
import com.investment.datacollection.client.KrxApiClient;
import com.investment.domain.entity.DailyStock;
import com.investment.domain.repository.DailyStockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("KrxCollectionService")
class KrxCollectionServiceTest {

    @Mock
    private KrxApiClient krxApiClient;

    @Mock
    private DailyStockRepository dailyStockRepository;

    @Mock
    private DataCollectionProperties dataCollectionProperties;

    @Mock
    private KoreaInvestmentKrxFallbackSupplier koreaInvestmentKrxFallbackSupplier;

    private KrxCollectionService service;

    @BeforeEach
    void setUp() {
        DataCollectionProperties.Krx krx = new DataCollectionProperties.Krx();
        krx.setKoreaInvestmentFallbackEnabled(false);
        lenient().when(dataCollectionProperties.getKrx()).thenReturn(krx);
        service = new KrxCollectionService(krxApiClient, dailyStockRepository, dataCollectionProperties);
        service.setKoreaInvestmentKrxFallbackSupplier(null); // 기본은 폴백 없음
    }

    @Test
    @DisplayName("KRX API가 데이터를 반환하면 파싱 후 저장하고 건수를 반환한다")
    void collectAndSave_whenKrxReturnsData_savesAndReturnsCount() {
        LocalDate basDt = LocalDate.of(2026, 3, 4);
        List<Map<String, Object>> rows = List.of(
                Map.of(
                        "isinCd", "KR7005930003",
                        "open", "71000",
                        "high", "72000",
                        "low", "70500",
                        "close", "71500",
                        "accTrdv", "1000000",
                        "accTrdval", "71500000000L"
                ));
        when(krxApiClient.fetchDailyStockKospi(basDt)).thenReturn(rows);
        when(dailyStockRepository.findByBasDtAndMarketAndSymbolIn(any(LocalDate.class), any(), anyList())).thenReturn(Collections.emptyList());

        int result = service.collectAndSave(basDt);

        assertThat(result).isEqualTo(1);
        verify(dailyStockRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("KRX API가 빈 결과를 반환하고 폴백이 비활성화면 0을 반환한다")
    void collectAndSave_whenKrxEmptyAndNoFallback_returnsZero() {
        LocalDate basDt = LocalDate.of(2026, 3, 4);
        when(krxApiClient.fetchDailyStockKospi(basDt)).thenReturn(Collections.emptyList());
        // 폴백 미사용이므로 repository.findByBasDtAndMarketAndSymbolIn 스텁 불필요

        int result = service.collectAndSave(basDt);

        assertThat(result).isEqualTo(0);
        verify(dailyStockRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("KRX API가 빈 결과를 반환하고 폴백이 활성화되어 데이터가 있으면 폴백으로 저장한다")
    void collectAndSave_whenKrxEmptyAndFallbackReturnsData_savesFallback() {
        LocalDate basDt = LocalDate.of(2026, 3, 4);
        when(krxApiClient.fetchDailyStockKospi(basDt)).thenReturn(Collections.emptyList());
        dataCollectionProperties.getKrx().setKoreaInvestmentFallbackEnabled(true);
        service.setKoreaInvestmentKrxFallbackSupplier(koreaInvestmentKrxFallbackSupplier);

        DailyStock fallbackStock = DailyStock.builder()
                .basDt(basDt)
                .symbol("005930")
                .market("KR")
                .openPrice(new BigDecimal("71000"))
                .highPrice(new BigDecimal("72000"))
                .lowPrice(new BigDecimal("70500"))
                .closePrice(new BigDecimal("71500"))
                .volume(1000000L)
                .trdVal(71500000000L)
                .createdAt(LocalDateTime.now())
                .build();
        when(koreaInvestmentKrxFallbackSupplier.fetchForDate(basDt)).thenReturn(List.of(fallbackStock));
        when(dailyStockRepository.findByBasDtAndMarketAndSymbolIn(any(LocalDate.class), any(), anyList())).thenReturn(Collections.emptyList());

        int result = service.collectAndSave(basDt);

        assertThat(result).isEqualTo(1);
        verify(dailyStockRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("전일 대비 50% 이상 변동 시 이상치 경고 후에도 저장은 수행된다")
    void collectAndSave_whenOutlierDetected_stillSavesAndLogsWarning() {
        LocalDate basDt = LocalDate.of(2026, 3, 4);
        LocalDate prev = basDt.minusDays(1);
        // 전일 종가 2000, 당일 고가 72000 저가 70500 → 범위 1500, 1500/2000 = 0.75 > 0.5
        List<Map<String, Object>> rows = List.of(
                Map.of(
                        "isinCd", "KR7005930003",
                        "open", "71000",
                        "high", "72000",
                        "low", "70500",
                        "close", "71500",
                        "accTrdv", "1000000",
                        "accTrdval", "71500000000L"
                ));
        when(krxApiClient.fetchDailyStockKospi(basDt)).thenReturn(rows);
        DailyStock prevDay = DailyStock.builder()
                .basDt(prev)
                .symbol("KR7005930003")
                .market("KR")
                .closePrice(new BigDecimal("2000"))
                .build();
        when(dailyStockRepository.findByBasDtAndMarketAndSymbolIn(eq(prev), eq("KR"), anyList()))
                .thenReturn(List.of(prevDay));

        int result = service.collectAndSave(basDt);

        assertThat(result).isEqualTo(1);
        verify(dailyStockRepository).saveAll(anyList());
    }
}
