package com.investment.datacollection.service;

import com.investment.config.DataCollectionProperties;
import com.investment.datacollection.client.KrxApiClient;
import com.investment.domain.entity.DailyStock;
import com.investment.domain.repository.DailyStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * KRX 일별 시세 수집 → TB_DAILY_STOCK 저장.
 * 1차 KRX API, 실패 시 2차 한투 API 차트 보조 소스 폴백. 양쪽 실패 시 로그 및 알림.
 * <p>수정주가 정책(ADR 19): 일봉 저장·팩터·백테스트는 수정주가만 사용. KRX 유가증권 일별매매정보
 * 원천이며, 수정주가 반영 여부는 KRX 공식 문서 참조. 한투 API 일봉 조회 시에는 FID_ORG_ADJ_PRC=0(수정주가) 사용.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KrxCollectionService {

    private static final String MARKET_KR = "KR";
    private static final DateTimeFormatter KRX_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    /** 전일 대비 50% 이상 변동 시 이상치 경고 */
    private static final double OUTLIER_CHANGE_RATIO = 0.50;

    private final KrxApiClient krxApiClient;
    private final DailyStockRepository dailyStockRepository;
    private final DataCollectionProperties dataCollectionProperties;

    @Autowired(required = false)
    private KoreaInvestmentKrxFallbackSupplier koreaInvestmentKrxFallbackSupplier;

    /** 테스트용 폴백 주입 (일반 실행에서는 @Autowired로 주입) */
    void setKoreaInvestmentKrxFallbackSupplier(KoreaInvestmentKrxFallbackSupplier supplier) {
        this.koreaInvestmentKrxFallbackSupplier = supplier;
    }

    /**
     * 기준일 KRX 유가증권 일별매매정보 수집 후 저장.
     * 1차: KRX API → 실패(빈 결과) 시 2차: 한투 API 폴백(설정 시). 수집 결과 로그에 성공/실패/폴백 상태 기록.
     *
     * @param basDt 기준일
     * @return 저장 건수
     */
    @Transactional
    public int collectAndSave(LocalDate basDt) {
        List<Map<String, Object>> rows = krxApiClient.fetchDailyStockKospi(basDt);
        if (!rows.isEmpty()) {
            int saved = parseAndSave(rows, basDt, "KRX");
            if (saved > 0) {
                log.info("KRX 일별 수집 완료: basDt={}, source=KRX, saved={}", basDt, saved);
                return saved;
            }
        }

        log.info("KRX 일별 수집 스킵 또는 결과 없음: basDt={}, rows=0 (AUTH_KEY 미설정 또는 API 빈 응답)", basDt);

        if (dataCollectionProperties.getKrx().isKoreaInvestmentFallbackEnabled()
                && koreaInvestmentKrxFallbackSupplier != null) {
            List<DailyStock> fallbackEntities = koreaInvestmentKrxFallbackSupplier.fetchForDate(basDt);
            if (!fallbackEntities.isEmpty()) {
                validateOutliersAndWarn(fallbackEntities, basDt);
                dailyStockRepository.saveAll(fallbackEntities);
                log.info("KRX 일별 수집 완료(폴백): basDt={}, source=KOREA_INVESTMENT_FALLBACK, saved={}", basDt, fallbackEntities.size());
                return fallbackEntities.size();
            }
        }

        log.warn("KRX 일별 수집 실패: basDt={}, source=NONE (1차 KRX 빈 결과, 2차 한투 폴백 미사용 또는 수집 0건). 수동 확인 또는 트리거 권장.", basDt);
        return 0;
    }

    private int parseAndSave(List<Map<String, Object>> rows, LocalDate basDt, String source) {
        List<DailyStock> entities = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            try {
                DailyStock e = mapToDailyStock(row, basDt);
                if (e != null) {
                    entities.add(e);
                }
            } catch (Exception ex) {
                log.warn("KRX 행 파싱 스킵: basDt={}, source={}, row={}, error={}", basDt, source, row.keySet(), ex.getMessage());
            }
        }
        if (entities.isEmpty()) {
            return 0;
        }
        validateOutliersAndWarn(entities, basDt);
        dailyStockRepository.saveAll(entities);
        return entities.size();
    }

    /**
     * 수집된 일봉에 대해 전일 대비 50% 이상 변동 시 경고 로그.
     */
    private void validateOutliersAndWarn(List<DailyStock> entities, LocalDate basDt) {
        LocalDate prev = basDt.minusDays(1);
        for (DailyStock e : entities) {
            if (e.getClosePrice() == null) continue;
            List<DailyStock> prevList = dailyStockRepository.findByBasDtAndMarketAndSymbolIn(prev, MARKET_KR, List.of(e.getSymbol()));
            if (prevList.isEmpty()) continue;
            BigDecimal prevClose = prevList.get(0).getClosePrice();
            if (prevClose == null || prevClose.compareTo(BigDecimal.ZERO) == 0) continue;
            BigDecimal high = e.getHighPrice();
            BigDecimal low = e.getLowPrice();
            if (high != null && low != null) {
                BigDecimal range = high.subtract(low);
                BigDecimal changeRatio = range.divide(prevClose, 4, RoundingMode.HALF_UP).abs();
                if (changeRatio.compareTo(BigDecimal.valueOf(OUTLIER_CHANGE_RATIO)) > 0) {
                    log.warn("KRX 일봉 이상치 경고: basDt={}, symbol={}, 전일종가={}, 고가={}, 저가={}, 변동비율={}",
                            basDt, e.getSymbol(), prevClose, high, low, changeRatio);
                }
            }
        }
    }

    /**
     * 기간 백필: from ~ to 각 거래일마다 collectAndSave 호출.
     * 스트레스 구간(2020-02~04, 2022-01~06) 등 과거 일봉 수집용.
     *
     * @param from 시작일
     * @param to   종료일
     * @return 수집·저장한 총 건수
     */
    @Transactional
    public int collectAndSaveRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            log.warn("KRX 기간 백필 스킵: from > to, from={}, to={}", from, to);
            return 0;
        }
        int total = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            total += collectAndSave(d);
        }
        log.info("KRX 기간 백필 완료: from={}, to={}, totalSaved={}", from, to, total);
        return total;
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
