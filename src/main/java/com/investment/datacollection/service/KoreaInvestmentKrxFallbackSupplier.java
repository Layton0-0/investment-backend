package com.investment.datacollection.service;

import com.investment.config.DataCollectionProperties;
import com.investment.datacollection.client.KoreaInvestmentDailyChartClient;
import com.investment.domain.entity.DailyStock;
import com.investment.domain.repository.DailyStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * KRX 일봉 수집 실패 시 한투 API로 해당 일자 일봉을 종목별 조회하여 보조 수집.
 * investment.data.krx.korea-investment-fallback-enabled=true 이고 fallback userId가 설정된 경우에만 빈 등록.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "investment.data.krx",
        name = "korea-investment-fallback-enabled",
        havingValue = "true"
)
public class KoreaInvestmentKrxFallbackSupplier {

    private static final String MARKET_KR = "KR";
    /** KRX 명세: 기준일자 yyyyMMdd (하이픈 없음) */
    private static final DateTimeFormatter BAS_DT_LOG = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final DataCollectionProperties dataCollectionProperties;
    private final KoreaInvestmentDailyChartClient dailyChartClient;
    private final DailyStockRepository dailyStockRepository;

    /**
     * 해당 기준일 일봉을 한투 API로 종목별 조회하여 수집.
     * 종목 목록은 설정( fallbackSymbolsSource / fallbackSymbols ) 또는 전일 TB_DAILY_STOCK 기준.
     *
     * @param basDt 기준일
     * @return 수집된 DailyStock 목록 (빈 목록 가능)
     */
    public List<DailyStock> fetchForDate(LocalDate basDt) {
        String userId = dataCollectionProperties.getKrx().getKoreaInvestmentFallbackUserId();
        if (userId == null || userId.isBlank()) {
            log.warn("KRX 폴백: 한투 폴백 사용자 미설정");
            return List.of();
        }

        List<String> symbols = resolveSymbols(basDt);
        if (symbols.isEmpty()) {
            log.warn("KRX 폴백: 조회할 종목 목록 없음, basDt={}", basDt.format(BAS_DT_LOG));
            return List.of();
        }

        List<DailyStock> result = new ArrayList<>();
        for (String symbol : symbols) {
            Optional<DailyStock> one = dailyChartClient.fetchDailyForSymbol(symbol, basDt, userId);
            one.ifPresent(result::add);
            // 한투 API 제한(모의 2건/초 등) 고려하여 종목당 약 500ms 간격
            if (symbols.size() > 1) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        log.info("KRX 폴백 한투 API 수집 완료: basDt={}, symbolsRequested={}, collected={}", basDt.format(BAS_DT_LOG), symbols.size(), result.size());
        return result;
    }

    private List<String> resolveSymbols(LocalDate basDt) {
        String source = dataCollectionProperties.getKrx().getFallbackSymbolsSource();
        if (source != null && source.equalsIgnoreCase("CONFIG")) {
            String configSymbols = dataCollectionProperties.getKrx().getFallbackSymbols();
            if (configSymbols == null || configSymbols.isBlank()) return List.of();
            return Arrays.stream(configSymbols.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        // PREVIOUS_DAY: 전일 TB_DAILY_STOCK KR 종목
        LocalDate prev = basDt.minusDays(1);
        return dailyStockRepository.findDistinctSymbolsByBasDtAndMarket(prev, MARKET_KR);
    }
}
