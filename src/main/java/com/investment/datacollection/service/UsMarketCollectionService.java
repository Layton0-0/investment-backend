package com.investment.datacollection.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investment.config.DataCollectionProperties;
import com.investment.domain.entity.DailyStock;
import com.investment.domain.repository.DailyStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * US 시장 일별 시세 수집 → TB_DAILY_STOCK 저장.
 * investment.data.us.yfinance-script-path 설정 시 yfinance Python 스크립트를 실행해 수집.
 * 미설정 시 스텁(0 반환).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsMarketCollectionService {

    private static final String MARKET_US = "US";

    private final DailyStockRepository dailyStockRepository;
    private final DataCollectionProperties dataCollectionProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 기준일 US 시장 일별 시세 수집 후 저장.
     * yfinance 스크립트 경로가 설정된 경우 스크립트 실행 → stdout JSON 파싱 → DailyStock(MARKET=US) 저장.
     *
     * @param basDt 기준일
     * @return 저장 건수 (스크립트 미설정 또는 실패 시 0)
     */
    @Transactional
    public int collectAndSave(LocalDate basDt) {
        String scriptPath = dataCollectionProperties.getUs().getYfinanceScriptPath();
        if (scriptPath == null || scriptPath.isBlank()) {
            log.debug("US 시장 일별 수집 스킵: yfinance-script-path 미설정");
            return 0;
        }

        Path path = Paths.get(scriptPath);
        if (!path.toFile().exists()) {
            log.warn("US 시장 일별 수집 스킵: 스크립트 없음 path={}", path.toAbsolutePath());
            return 0;
        }

        String symbols = dataCollectionProperties.getUs().getSymbols();
        if (symbols == null) {
            symbols = "AAPL,MSFT,GOOGL,AMZN,META,TSLA,NVDA,JPM,V,JNJ";
        }

        List<DailyStock> entities = runScriptAndParse(basDt, path, symbols);
        if (entities.isEmpty()) {
            log.debug("US 시장 일별 수집: basDt={}, parsed=0", basDt);
            return 0;
        }
        dailyStockRepository.saveAll(entities);
        log.info("US 시장 일별 수집 완료: basDt={}, saved={}", basDt, entities.size());
        return entities.size();
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
                "--symbols", symbols
        );
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
            if (exitCode != 0) {
                log.warn("US yfinance 스크립트 비정상 종료: exitCode={}, basDt={}", exitCode, basDt);
                return List.of();
            }
            return parseJsonToDailyStocks(stdout.toString(), basDt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("US yfinance 스크립트 중단: basDt={}", basDt, e);
            return List.of();
        } catch (Exception e) {
            log.warn("US yfinance 스크립트 실행 실패: basDt={}, error={}", basDt, e.getMessage(), e);
            return List.of();
        }
    }

    private List<DailyStock> parseJsonToDailyStocks(String json, LocalDate basDt) {
        List<DailyStock> out = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = objectMapper.readValue(json, new TypeReference<>() {});
            for (Map<String, Object> row : rows) {
                DailyStock e = mapToDailyStock(row, basDt);
                if (e != null) {
                    out.add(e);
                }
            }
        } catch (Exception e) {
            log.warn("US 일별 JSON 파싱 실패: basDt={}, error={}", basDt, e.getMessage());
        }
        return out;
    }

    private DailyStock mapToDailyStock(Map<String, Object> row, LocalDate basDt) {
        String symbol = getString(row, "symbol");
        if (symbol == null || symbol.isBlank()) {
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
