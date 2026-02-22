package com.investment.marketdata.service;

import com.investment.domain.entity.DailyStock;
import com.investment.domain.repository.DailyStockRepository;
import com.investment.marketdata.dto.DailyChartPointDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 일봉 차트 조회 서비스.
 * TB_DAILY_STOCK 기반으로 KR/US 일봉을 조회해 프론트 차트용 DTO로 변환합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyChartService {

    private static final int MAX_DAYS = 365;

    private final DailyStockRepository dailyStockRepository;

    /**
     * 종목·시장·기간별 일봉 차트 데이터 조회.
     * from/to 미지정 시 최근 1년. 최대 365일 제한.
     *
     * @param symbol 종목 코드
     * @param market 시장 (KR, US)
     * @param from   시작일 (optional)
     * @param to     종료일 (optional)
     * @return 일봉 목록 (없으면 빈 목록)
     */
    @Transactional(readOnly = true)
    public List<DailyChartPointDto> getDailyChart(String symbol, String market, LocalDate from, LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(MAX_DAYS);
        if (start.isAfter(end)) {
            start = end.minusDays(MAX_DAYS);
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
        if (days > MAX_DAYS) {
            start = end.minusDays(MAX_DAYS - 1);
        }
        List<DailyStock> list = dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                symbol, market != null ? market : "KR", start, end);
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    private DailyChartPointDto toDto(DailyStock e) {
        return DailyChartPointDto.builder()
                .date(e.getBasDt() != null ? e.getBasDt().toString() : null)
                .open(e.getOpenPrice())
                .high(e.getHighPrice())
                .low(e.getLowPrice())
                .close(e.getClosePrice())
                .volume(e.getVolume())
                .build();
    }
}
