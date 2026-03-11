package com.investment.datacollection.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investment.alert.EmergencyAlertService;
import com.investment.common.logging.KoreaInvestmentApiLogging;
import com.investment.config.DataCollectionProperties;
import com.investment.domain.entity.DailyStock;
import com.investment.domain.repository.DailyStockRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * US 시장 일별 시세 수집 → TB_DAILY_STOCK 저장.
 * investment.data.us.collector-url 설정 시 Docker us-daily-collector 서비스를 HTTP로
 * 호출.
 * 미설정 시 yfinance-script-path로 로컬 스크립트 실행. 둘 다 미설정 시 스텁(0 반환).
 * <p>수정주가 정책(ADR 19): 저장·팩터·백테스트는 수정주가만 사용. yfinance는 adjusted OHLC를
 * 반환하므로(스크립트에서 auto_adjust=True) 저장되는 가격은 수정주가이다.
 */
@Slf4j
@Service
public class UsMarketCollectionService {

    private static final String MARKET_US = "US";

    private final DailyStockRepository dailyStockRepository;
    private final DataCollectionProperties dataCollectionProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired(required = false)
    private EmergencyAlertService emergencyAlertService;

    public UsMarketCollectionService(DailyStockRepository dailyStockRepository,
                                    DataCollectionProperties dataCollectionProperties) {
        this.dailyStockRepository = dailyStockRepository;
        this.dataCollectionProperties = dataCollectionProperties;
    }

    /** 기간 수집 시 최대 일수 (과부하 방지) */
    private static final int MAX_RANGE_DAYS = 365;

    /**
     * 기준일 US 시장 일별 시세 수집 후 저장.
     * yfinance 스크립트 경로가 설정된 경우 스크립트 실행 → stdout JSON 파싱 → DailyStock(MARKET=US) 저장.
     *
     * @param basDt 기준일
     * @return 저장 건수 (스크립트 미설정 또는 실패 시 0)
     */
    @Transactional
    public int collectAndSave(LocalDate basDt) {
        return collectAndSave(basDt, null);
    }

    /**
     * 기준일 US 시장 일별 시세 수집 후 저장. 심볼 오버라이드 시 해당 종목만 수집.
     *
     * @param basDt           기준일
     * @param symbolsOverride 수집 대상 심볼 (null 또는 비어 있으면 설정값 사용)
     * @return 저장 건수
     */
    @Transactional
    public int collectAndSave(LocalDate basDt, List<String> symbolsOverride) {
        List<String> symbolsList = resolveSymbolsList(symbolsOverride);
        log.info("US 시장 일별 수집 시작: basDt={}, symbols={}, count={}", basDt, symbolsList, symbolsList.size());

        String collectorUrl = dataCollectionProperties.getUs().getCollectorUrl();
        if (collectorUrl != null && !collectorUrl.isBlank()) {
            List<DailyStock> entities = fetchViaCollectorUrl(basDt, symbolsOverride);
            if (entities.isEmpty()) {
                log.warn("US 시장 일별 수집(HTTP) 결과 없음: basDt={}, symbols={} — 수집기 응답 빈 배열 또는 오류", basDt, symbolsList);
                boolean skipAlert = dataCollectionProperties.getUs().isSkipFailureAlertOnWeekend()
                        && (basDt.getDayOfWeek() == DayOfWeek.SATURDAY || basDt.getDayOfWeek() == DayOfWeek.SUNDAY);
                if (dataCollectionProperties.getUs().isFailureAlertEnabled() && emergencyAlertService != null && !skipAlert) {
                    emergencyAlertService.sendRiskEventAlert("WARNING", "UsDailyCollector",
                            "US 시장 일별 수집 결과 없음: basDt=" + basDt + ", symbols count=" + symbolsList.size());
                }
                return 0;
            }
            dailyStockRepository.saveAll(entities);
            log.info("US 시장 일별 수집(HTTP) 완료: basDt={}, saved={}", basDt, entities.size());
            return entities.size();
        }

        String scriptPath = dataCollectionProperties.getUs().getYfinanceScriptPath();
        if (scriptPath == null || scriptPath.isBlank()) {
            log.warn(
                    "US 시장 일별 수집 스킵: collector-url·yfinance-script-path 미설정 — application.yml에 investment.data.us.collector-url 또는 yfinance-script-path 설정 필요");
            return 0;
        }

        Path path = Paths.get(scriptPath);
        if (!path.toFile().exists()) {
            log.warn("US 시장 일별 수집 스킵: 스크립트 파일 없음 path={}, basDt={}", path.toAbsolutePath(), basDt);
            return 0;
        }

        String symbols = resolveSymbolsString(symbolsOverride);
        List<DailyStock> entities = runScriptAndParse(basDt, path, symbols);
        if (entities.isEmpty()) {
            log.warn("US 시장 일별 수집 결과 없음: basDt={}, symbols={} — 스크립트 0건 반환 또는 JSON 파싱 실패", basDt, symbolsList);
            return 0;
        }
        dailyStockRepository.saveAll(entities);
        log.info("US 시장 일별 수집 완료: basDt={}, saved={}", basDt, entities.size());
        return entities.size();
    }

    /** collector-url로 POST 후 JSON 파싱 → DailyStock 목록. 연결/타임아웃/5xx 시 재시도(지수 백오프). */
    private List<DailyStock> fetchViaCollectorUrl(LocalDate basDt, List<String> symbolsOverride) {
        String baseUrl = dataCollectionProperties.getUs().getCollectorUrl().trim().replaceAll("/+$", "");
        String url = baseUrl + "/us-daily";
        List<String> symbolsList = resolveSymbolsList(symbolsOverride);
        if (symbolsList.isEmpty()) {
            log.warn("US 일별 수집(HTTP) 스킵: 수집 대상 심볼 없음 — symbolsOverride 비어있고 설정값(us.symbols)도 없음, basDt={}", basDt);
            return List.of();
        }
        int retryMax = Math.max(0, dataCollectionProperties.getUs().getRetryMax());
        long retryInitialMs = Math.max(0L, dataCollectionProperties.getUs().getRetryInitialMs());
        int maxAttempts = 1 + retryMax;
        Exception lastException = null;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            if (attempt > 0) {
                long delayMs = retryInitialMs * (1L << (attempt - 1));
                log.info("US 일별 수집(HTTP) 재시도: attempt={}/{}, basDt={}, delayMs={}", attempt + 1, maxAttempts, basDt, delayMs);
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("US 일별 수집(HTTP) 재시도 대기 중 인터럽트");
                    return List.of();
                }
            }
            try {
                List<DailyStock> result = doFetchViaCollectorUrlOnce(url, basDt, symbolsList, symbolsOverride);
                if (attempt > 0 && !result.isEmpty()) {
                    log.info("US 일별 수집(HTTP) 재시도 성공: attempt={}, basDt={}, count={}", attempt + 1, basDt, result.size());
                }
                return result;
            } catch (Exception e) {
                lastException = e;
                log.warn("US 일별 수집(HTTP) 시도 실패: attempt={}/{}, basDt={}, error={}",
                        attempt + 1, maxAttempts, basDt, e.getMessage());
            }
        }
        if (lastException != null) {
            log.error("US 일별 수집(HTTP) 모든 재시도 실패: url={}, basDt={}, attempts={}",
                    url, basDt, maxAttempts, lastException);
        }
        return List.of();
    }

    private List<DailyStock> doFetchViaCollectorUrlOnce(String url, LocalDate basDt,
            List<String> symbolsList, List<String> symbolsOverride) {
        log.debug("US 일별 수집(HTTP) 요청: url={}, basDt={}, symbols={}", url, basDt, symbolsList);
        KoreaInvestmentApiLogging.logApiCallInfo("US일별수집기", "US 일별 시세", url, "POST");
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("bas_dt", basDt.toString());
        body.put("symbols", symbolsList);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<java.util.Map<String, Object>> request = new HttpEntity<>(body, headers);
        RestTemplate rest = new RestTemplate();
        ResponseEntity<String> response = rest.postForEntity(url, request, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            log.warn("US 일별 수집(HTTP) 비정상 응답: url={}, basDt={}, status={}, bodyLength={}",
                    url, basDt, response.getStatusCode(),
                    response.getBody() != null ? response.getBody().length() : 0);
            throw new IllegalStateException("HTTP " + response.getStatusCode());
        }
        if (response.getBody() == null || response.getBody().isBlank()) {
            log.warn("US 일별 수집(HTTP) 응답 본문 없음: url={}, basDt={}", url, basDt);
            return List.of();
        }
        List<DailyStock> parsed = parseJsonToDailyStocks(response.getBody(), basDt);
        int bodyLen = response.getBody() != null ? response.getBody().length() : 0;
        if (parsed.isEmpty()) {
            log.info("US 일별 수집(HTTP) 응답 빈 배열: basDt={}, responseBodyLength={} (휴장일이면 2에 가까움)", basDt, bodyLen);
            if (!response.getBody().trim().startsWith("[]")) {
                log.warn("US 일별 수집(HTTP) 파싱 후 0건: url={}, basDt={}, bodyPreview={}",
                        url, basDt, response.getBody().length() > 200 ? response.getBody().substring(0, 200) + "..."
                                : response.getBody());
            }
        }
        return parsed;
    }

    private String resolveSymbolsString(List<String> symbolsOverride) {
        List<String> list = resolveSymbolsList(symbolsOverride);
        return list.isEmpty() ? String.join(",", getDefaultUsSymbols()) : String.join(",", list);
    }

    /** 퀀트용 기본 US 유니버스: 지수·섹터 ETF + 대표 주식 (듀얼모멘텀·벤치마크·유동성) */
    private static List<String> getDefaultUsSymbols() {
        return List.of(
                "SPY", "QQQ", "IWM", "TLT", "IEF", "BIL", "GLD", "DBC",
                "XLK", "XLF", "XLE", "XLV", "XLY", "XLP", "XLB", "XLI", "XLC",
                "AAPL", "MSFT", "GOOGL", "AMZN", "META", "TSLA", "NVDA",
                "JPM", "V", "JNJ", "WMT", "UNH", "HD", "PG", "MA", "BAC", "XOM", "CVX"
        );
    }

    private List<String> resolveSymbolsList(List<String> symbolsOverride) {
        if (symbolsOverride != null && !symbolsOverride.isEmpty()) {
            List<String> list = symbolsOverride.stream().filter(s -> s != null && !s.isBlank()).toList();
            if (!list.isEmpty())
                return list;
        }
        String symbols = dataCollectionProperties.getUs().getSymbols();
        if (symbols == null || symbols.isBlank()) {
            return getDefaultUsSymbols();
        }
        return java.util.Arrays.stream(symbols.split(",")).map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
    }

    /**
     * 기간 내 US 시장 일별 시세 수집 후 저장. (거래일만 순회하지 않고 calendar day 기준, 최대
     * {@value MAX_RANGE_DAYS}일)
     *
     * @param from            시작일 (포함)
     * @param to              종료일 (포함)
     * @param symbolsOverride 수집 대상 심볼 (null 또는 비어 있으면 설정값 사용)
     * @return [수집한 일수, 총 저장 건수]
     */
    public int[] collectAndSaveRange(LocalDate from, LocalDate to, List<String> symbolsOverride) {
        if (from.isAfter(to)) {
            log.warn("US 시장 기간 수집 스킵: from > to, from={}, to={}", from, to);
            return new int[] { 0, 0 };
        }
        long limit = Math.min(java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1, MAX_RANGE_DAYS);
        LocalDate end = from.plusDays(limit - 1);
        if (end.isAfter(to)) {
            end = to;
        }
        log.info("US 시장 기간 수집 시작: from={}, to={}, limitDays={}, symbolsOverride={}",
                from, end, limit, symbolsOverride != null ? symbolsOverride.size() : 0);
        int daysCollected = 0;
        int totalSaved = 0;
        for (LocalDate d = from; !d.isAfter(end); d = d.plusDays(1)) {
            int saved = collectAndSave(d, symbolsOverride);
            if (saved > 0) {
                daysCollected++;
                totalSaved += saved;
            }
        }
        log.info("US 시장 기간 수집 완료: from={}, to={}, daysCollected={}, totalSaved={}", from, end, daysCollected,
                totalSaved);
        return new int[] { daysCollected, totalSaved };
    }

    private List<DailyStock> runScriptAndParse(LocalDate basDt, Path scriptPath, String symbols) {
        String pythonCmd = dataCollectionProperties.getUs().getPythonCommand();
        if (pythonCmd == null || pythonCmd.isBlank()) {
            pythonCmd = "python";
        }
        ProcessBuilder pb = new ProcessBuilder(
                pythonCmd,
                scriptPath.toAbsolutePath().toString(),
                "--bas-dt", basDt.toString(),
                "--symbols", symbols);
        pb.redirectErrorStream(false);
        Path dir = scriptPath.getParent();
        if (dir != null && dir.toFile().exists()) {
            pb.directory(dir.toFile());
        }

        try {
            Process process = pb.start();
            StringBuilder stdout = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stdout.append(line);
                }
            }
            int exitCode = process.waitFor();
            String stdoutStr = stdout.toString();
            if (exitCode != 0) {
                log.warn(
                        "US yfinance 스크립트 비정상 종료: exitCode={}, basDt={}, symbols={}, stdoutLength={} — 스크립트 경로·Python 환경·네트워크 확인",
                        exitCode, basDt, symbols, stdoutStr.length());
                log.debug("US yfinance 스크립트 stdout: {}",
                        stdoutStr.length() > 500 ? stdoutStr.substring(0, 500) + "..." : stdoutStr);
                return List.of();
            }
            List<DailyStock> parsed = parseJsonToDailyStocks(stdoutStr, basDt);
            if (parsed.isEmpty() && !stdoutStr.trim().startsWith("[]")) {
                log.warn("US yfinance 스크립트 파싱 후 0건: basDt={}, stdoutPreview={}",
                        basDt, stdoutStr.length() > 200 ? stdoutStr.substring(0, 200) + "..." : stdoutStr);
            }
            return parsed;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("US yfinance 스크립트 중단(InterruptedException): basDt={}, symbols={}", basDt, symbols, e);
            return List.of();
        } catch (Exception e) {
            log.error("US yfinance 스크립트 실행 실패: basDt={}, symbols={}, error={}, cause={}",
                    basDt, symbols, e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : "none", e);
            return List.of();
        }
    }

    private List<DailyStock> parseJsonToDailyStocks(String json, LocalDate basDt) {
        List<DailyStock> out = new ArrayList<>();
        if (json == null || json.isBlank()) {
            log.warn("US 일별 JSON 파싱 스킵: basDt={}, 원본이 null 또는 빈 문자열", basDt);
            return out;
        }
        try {
            List<Map<String, Object>> rows = objectMapper.readValue(json, new TypeReference<>() {
            });
            log.debug("US 일별 JSON 파싱: basDt={}, rawRows={}", basDt, rows != null ? rows.size() : 0);
            if (rows == null) {
                return out;
            }
            int skipped = 0;
            for (Map<String, Object> row : rows) {
                DailyStock e = mapToDailyStock(row, basDt);
                if (e != null) {
                    out.add(e);
                } else {
                    skipped++;
                }
            }
            if (skipped > 0) {
                log.debug("US 일별 JSON 행 일부 스킵: basDt={}, skipped={}, reason=symbol 비어있거나 필수 필드 없음", basDt, skipped);
            }
        } catch (Exception e) {
            log.warn("US 일별 JSON 파싱 실패: basDt={}, error={}, jsonPreview={}",
                    basDt, e.getMessage(), json.length() > 150 ? json.substring(0, 150) + "..." : json);
        }
        return out;
    }

    private DailyStock mapToDailyStock(Map<String, Object> row, LocalDate basDt) {
        String symbol = getString(row, "symbol");
        if (symbol == null || symbol.isBlank()) {
            log.trace("US 일별 행 스킵: symbol 없음, row={}", row);
            return null;
        }
        BigDecimal openPrice = getDecimal(row, "open");
        BigDecimal highPrice = getDecimal(row, "high");
        BigDecimal lowPrice = getDecimal(row, "low");
        BigDecimal closePrice = getDecimal(row, "close");
        Long volume = getLong(row, "volume");
        Long trdVal = getLong(row, "trdVal");
        if (trdVal == null && volume != null && closePrice != null) {
            trdVal = closePrice.multiply(BigDecimal.valueOf(volume)).longValue();
        }
        return DailyStock.builder()
                .basDt(basDt)
                .symbol(symbol.trim())
                .market(MARKET_US)
                .openPrice(openPrice)
                .highPrice(highPrice)
                .lowPrice(lowPrice)
                .closePrice(closePrice)
                .volume(volume)
                .trdVal(trdVal)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private static String getString(Map<String, Object> row, String key) {
        Object v = row.get(key);
        return v != null ? v.toString().trim() : null;
    }

    private static BigDecimal getDecimal(Map<String, Object> row, String key) {
        Object v = row.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return BigDecimal.valueOf(((Number) v).doubleValue());
        }
        try {
            return new BigDecimal(v.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long getLong(Map<String, Object> row, String key) {
        Object v = row.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.parseLong(v.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
