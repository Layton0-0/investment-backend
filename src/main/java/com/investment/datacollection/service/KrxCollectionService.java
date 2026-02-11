package com.investment.datacollection.service;

import com.investment.datacollection.client.KrxApiClient;
import com.investment.domain.entity.DailyStock;
import com.investment.domain.repository.DailyStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * KRX 일별 시세 수집 → TB_DAILY_STOCK 저장.
 * API 응답 OutBlock_1의 Map 키는 서비스별로 상이할 수 있으므로 여러 키명을 시도해 파싱한다.
 * <p>수정주가 정책(ADR 19): 일봉 저장·팩터·백테스트는 수정주가만 사용. KRX 유가증권 일별매매정보
 * 원천이며, 수정주가 반영 여부는 KRX 공식 문서 참조. 한투 API 일봉 조회 시에는 FID_ORG_ADJ_PRC=0(수정주가) 사용.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KrxCollectionService {

    private static final String MARKET_KR = "KR";
    private static final DateTimeFormatter KRX_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final KrxApiClient krxApiClient;
    private final DailyStockRepository dailyStockRepository;

    /**
     * 기준일 KRX 유가증권 일별매매정보 수집 후 저장.
     *
     * @param basDt 기준일
     * @return 저장 건수
     */
    @Transactional
    public int collectAndSave(LocalDate basDt) {
        List<Map<String, Object>> rows = krxApiClient.fetchDailyStockKospi(basDt);
        if (rows.isEmpty()) {
            log.debug("KRX 일별 수집: basDt={}, rows=0", basDt);
            return 0;
        }
        List<DailyStock> entities = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            try {
                DailyStock e = mapToDailyStock(row, basDt);
                if (e != null) {
                    entities.add(e);
                }
            } catch (Exception e) {
                log.warn("KRX 행 파싱 스킵: basDt={}, row={}, error={}", basDt, row.keySet(), e.getMessage());
            }
        }
        if (entities.isEmpty()) {
            log.debug("KRX 일별 수집: basDt={}, parsed=0", basDt);
            return 0;
        }
        dailyStockRepository.saveAll(entities);
        log.info("KRX 일별 수집 완료: basDt={}, saved={}", basDt, entities.size());
        return entities.size();
    }

    /**
     * Map 한 행을 DailyStock으로 변환.
     * KRX API 필드명 변형(camelCase/snake_case/한글 등)에 대응하기 위해 여러 키를 시도한다.
     */
    private DailyStock mapToDailyStock(Map<String, Object> row, LocalDate basDt) {
        String symbol = getString(row, "isinCd", "isin_cd", "stockCd", "stock_cd", "symbol", "종목코드");
        if (symbol == null || symbol.isBlank()) {
            return null;
        }
        symbol = symbol.trim();
        BigDecimal openPrice = getDecimal(row, "open", "openPrice", "open_price", "시가");
        BigDecimal highPrice = getDecimal(row, "high", "highPrice", "high_price", "고가");
        BigDecimal lowPrice = getDecimal(row, "low", "lowPrice", "low_price", "저가");
        BigDecimal closePrice = getDecimal(row, "close", "closePrice", "close_price", "종가");
        Long volume = getLong(row, "volume", "accTrdv", "acc_trdv", "거래량");
        Long trdVal = getLong(row, "accTrdval", "acc_trdval", "trdVal", "trd_val", "거래대금");

        return DailyStock.builder()
                .basDt(basDt)
                .symbol(symbol)
                .market(MARKET_KR)
                .openPrice(openPrice)
                .highPrice(highPrice)
                .lowPrice(lowPrice)
                .closePrice(closePrice)
                .volume(volume)
                .trdVal(trdVal)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private static String getString(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object v = row.get(key);
            if (v != null) {
                return v.toString().trim();
            }
        }
        return null;
    }

    private static BigDecimal getDecimal(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object v = row.get(key);
            if (v != null && !v.toString().isBlank()) {
                try {
                    return new BigDecimal(v.toString().replaceAll(",", "").trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    private static Long getLong(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object v = row.get(key);
            if (v != null && !v.toString().isBlank()) {
                try {
                    return Long.parseLong(v.toString().replaceAll(",", "").trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }
}
