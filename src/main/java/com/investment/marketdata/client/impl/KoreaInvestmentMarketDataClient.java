package com.investment.marketdata.client.impl;

import com.investment.common.security.EncryptionUtil;
import com.investment.domain.entity.BrokerType;
import com.investment.domain.entity.UserApiKey;
import com.investment.domain.repository.UserApiKeyRepository;
import com.investment.marketdata.client.IndicatorResponse;
import com.investment.marketdata.client.MarketDataClient;
import com.investment.marketdata.config.MarketDataProperties;
import com.investment.marketdata.service.KoreaInvestmentTokenService;
import com.investment.marketdata.util.StockCodeConverter;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 한국투자증권 API 시장 데이터 클라이언트 구현체
 * 
 * 한국투자증권 Open API는 REST API 기반으로 제공됩니다.
 * - OAuth 2.0 인증 (App Key, App Secret)
 * - Access Token 발급 후 API 호출
 * - 종목 코드: 6자리 숫자 (코스피/코스닥)
 * 
 * API 문서: https://apiportal.koreainvestment.com/
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "investment.market-data.provider", havingValue = "korea-investment")
public class KoreaInvestmentMarketDataClient implements MarketDataClient {
    
    private final MarketDataProperties properties;
    private final WebClient webClient;
    private final RateLimiterRegistry rateLimiterRegistry;
    private final KoreaInvestmentTokenService tokenService;
    private final UserApiKeyRepository userApiKeyRepository;
    private final EncryptionUtil encryptionUtil;
    
    // 한국투자증권 API Base URL
    private static final String BASE_URL_REAL = "https://openapi.koreainvestment.com:9443"; // 실거래
    private static final String BASE_URL_VIRTUAL = "https://openapivts.koreainvestment.com:29443"; // 모의투자
    
    // Rate Limiter 인스턴스 이름
    private static final String RATE_LIMITER_API_VIRTUAL = "koreaInvestmentApi";
    private static final String RATE_LIMITER_API_REAL = "koreaInvestmentApiReal";
    
    /**
     * 현재 사용자 ID 가져오기
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("인증되지 않은 사용자입니다");
        }
        return authentication.getName();
    }
    
    /**
     * API 호출용 Rate Limiter 가져오기
     * 서버 타입(실전투자/모의투자)에 따라 적절한 Rate Limiter 반환
     */
    private RateLimiter getApiRateLimiter(String serverType) {
        if ("0".equals(serverType)) {
            // 실전투자: 1초당 20건
            return rateLimiterRegistry.rateLimiter(RATE_LIMITER_API_REAL);
        } else {
            // 모의투자: 1초당 2건
            return rateLimiterRegistry.rateLimiter(RATE_LIMITER_API_VIRTUAL);
        }
    }
    
    @Override
    public Mono<IndicatorResponse> getIndicator(String indicator, String symbol, String interval) {
        log.debug("한국투자증권 API 호출: indicator={}, symbol={}, interval={}", indicator, symbol, interval);
        
        // 모의 데이터 사용 여부 확인
        if (properties.isUseMockData()) {
            log.debug("모의 데이터 사용: symbol={}", symbol);
            return Mono.just(createMockIndicatorResponse(indicator, symbol));
        }
        
        // 현재 사용자 ID 가져오기
        String userId = getCurrentUserId();
        
        // Access Token 확인 및 갱신
        return ensureAccessToken(userId)
                .flatMap(tokenInfo -> {
                    String token = tokenInfo.get("token");
                    String serverType = tokenInfo.get("serverType");
                    
                    // 종목 코드 변환 (6자리 종목코드)
                    String stockCode = StockCodeConverter.toStockCode(symbol);
                    
                    // 차트 데이터 조회 (한국투자증권 API는 차트 데이터를 제공하고, 클라이언트에서 지표 계산)
                    return getChartData(stockCode, interval, token, userId, serverType)
                            .map(chartData -> calculateIndicator(chartData, indicator))
                            .onErrorResume(error -> {
                                log.error("한국투자증권 API 호출 실패: indicator={}, symbol={}", indicator, symbol, error);
                                return Mono.just(createErrorResponse("API 호출 실패: " + error.getMessage()));
                            });
                })
                .timeout(Duration.ofMillis(properties.getTimeout()))
                .onErrorReturn(createErrorResponse("타임아웃 또는 오류 발생"));
    }
    
    @Override
    public Mono<Map<String, IndicatorResponse>> getBulkIndicators(
            String symbol, String interval, String... indicators) {
        
        log.debug("한국투자증권 Bulk API 호출: symbol={}, interval={}, indicators={}", symbol, interval, indicators);
        
        // 종목 코드 변환
        String stockCode = StockCodeConverter.toStockCode(symbol);
        
        // 현재 사용자 ID 가져오기
        String userId = getCurrentUserId();
        
        // Access Token 확인 및 갱신
        return ensureAccessToken(userId)
                .flatMap(tokenInfo -> {
                    String token = tokenInfo.get("token");
                    String serverType = tokenInfo.get("serverType");
                    
                    // 차트 데이터를 한 번만 조회
                    return getChartData(stockCode, interval, token, userId, serverType)
                            .map(chartData -> {
                                // 모든 지표를 계산
                                Map<String, IndicatorResponse> resultMap = new HashMap<>();
                                for (String indicator : indicators) {
                                    IndicatorResponse response = calculateIndicator(chartData, indicator);
                                    resultMap.put(indicator, response);
                                }
                                return resultMap;
                            })
                            .onErrorResume(error -> {
                                log.error("한국투자증권 Bulk API 호출 실패: symbol={}", symbol, error);
                                // 실패한 지표는 에러 응답으로 채움
                                Map<String, IndicatorResponse> errorMap = new HashMap<>();
                                for (String indicator : indicators) {
                                    errorMap.put(indicator, createErrorResponse("API 호출 실패"));
                                }
                                return Mono.just(errorMap);
                            });
                })
                .timeout(Duration.ofMillis(properties.getTimeout()))
                .onErrorReturn(new HashMap<>());
    }
    
    @Override
    public String getProviderName() {
        return "Korea Investment";
    }
    
    /**
     * Access Token 확인 및 갱신
     * DB에서 사용자별 토큰을 조회하고, 만료되었으면 자동으로 재발급합니다.
     */
    private Mono<Map<String, String>> ensureAccessToken(String userId) {
        try {
            // DB에서 토큰 조회
            String accessToken = tokenService.getAccessToken(userId);
            
            // 사용자 API 키 정보 조회 (서버 타입 확인용)
            List<UserApiKey> userApiKeys = userApiKeyRepository.findByUserId(userId);
            if (userApiKeys.isEmpty()) {
                throw new IllegalStateException("사용자 API 키를 찾을 수 없습니다: userId=" + userId);
            }
            UserApiKey userApiKey = userApiKeys.get(0);
            
            Map<String, String> tokenInfo = new HashMap<>();
            tokenInfo.put("token", accessToken);
            tokenInfo.put("serverType", userApiKey.getServerType());
            
            return Mono.just(tokenInfo);
        } catch (RuntimeException e) {
            // 토큰이 없거나 만료된 경우 재발급 시도
            log.warn("토큰 조회 실패, 재발급 시도: userId={}, error={}", userId, e.getMessage());
            
            return Mono.fromCallable(() -> {
                List<UserApiKey> userApiKeys = userApiKeyRepository.findByUserId(userId);
                if (userApiKeys.isEmpty()) {
                    throw new IllegalStateException("사용자 API 키를 찾을 수 없습니다: userId=" + userId);
                }
                UserApiKey userApiKey = userApiKeys.get(0);
                
                // 한국투자증권인 경우에만 토큰 발급
                if (userApiKey.getBrokerType() == BrokerType.KOREA_INVESTMENT) {
                    tokenService.issueTokenForUser(userApiKey);
                    String accessToken = tokenService.getAccessToken(userId);
                    
                    Map<String, String> tokenInfo = new HashMap<>();
                    tokenInfo.put("token", accessToken);
                    tokenInfo.put("serverType", userApiKey.getServerType());
                    return tokenInfo;
                } else {
                    throw new IllegalStateException("한국투자증권이 아닌 증권사입니다: " + userApiKey.getBrokerType());
                }
            });
        }
    }
    
    
    /**
     * 차트 데이터 조회
     * 한국투자증권 API의 주식 차트 조회 API를 사용합니다.
     * Rate Limiter 적용: 실전투자 1초당 20건, 모의투자 1초당 2건
     */
    private Mono<List<Map<String, Object>>> getChartData(String stockCode, String interval, 
                                                          String accessToken, String userId, String serverType) {
        // 사용자 API 키 정보 조회
        List<UserApiKey> userApiKeys = userApiKeyRepository.findByUserId(userId);
        if (userApiKeys.isEmpty()) {
            return Mono.error(new IllegalStateException("사용자 API 키를 찾을 수 없습니다: userId=" + userId));
        }
        UserApiKey userApiKey = userApiKeys.get(0);
        
        // API 키 복호화
        String appKey = encryptionUtil.decrypt(userApiKey.getAppKeyEncrypted());
        String appSecret = encryptionUtil.decrypt(userApiKey.getAppSecretEncrypted());
        
        String baseUrl = getBaseUrl(serverType);
        
        String trId = "FHKST03010100"; // 주식현재가 일봉차트 조회 TR ID
        
        // 날짜 설정 (최근 200일)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(200);
        String endDateStr = endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String startDateStr = startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // API 엔드포인트
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice")
                .build()
                .toUri();
        
        // 요청 헤더
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("authorization", "Bearer " + accessToken);
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", trId);
        
        // 요청 바디
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("FID_COND_MRKT_DIV_CODE", "J"); // J: 주식, ETF, ETN
        requestBody.put("FID_INPUT_ISCD", stockCode); // 종목코드
        requestBody.put("FID_INPUT_DATE_1", startDateStr); // 시작일자
        requestBody.put("FID_INPUT_DATE_2", endDateStr); // 종료일자
        requestBody.put("FID_PERIOD_DIV_CODE", convertIntervalToPeriodCode(interval)); // 기간분할코드
        
        log.debug("한국투자증권 차트 데이터 조회: stockCode={}, interval={}", stockCode, interval);
        
        // Rate Limiter 적용 (실전투자: 1초당 20건, 모의투자: 1초당 2건)
        RateLimiter rateLimiter = getApiRateLimiter(serverType);
        
        return Mono.fromCallable(() -> {
                    // Rate Limiter가 허용할 때까지 대기
                    rateLimiter.acquirePermission();
                    return null;
                })
                .flatMap(ignored -> webClient.post()
                        .uri(uri)
                        .headers(h -> h.addAll(headers))
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .timeout(Duration.ofMillis(properties.getTimeout()))
                        .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                                .filter(throwable -> {
                                    if (throwable instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                                        org.springframework.web.reactive.function.client.WebClientResponseException ex = 
                                                (org.springframework.web.reactive.function.client.WebClientResponseException) throwable;
                                // 401 에러는 토큰 갱신 후 재시도
                                if (ex.getStatusCode().value() == 401) {
                                    log.warn("401 에러 발생, 토큰 재발급 시도: userId={}", userId);
                                    // 토큰 재발급
                                    try {
                                        List<UserApiKey> apiKeys = userApiKeyRepository.findByUserId(userId);
                                        if (!apiKeys.isEmpty()) {
                                            tokenService.issueTokenForUser(apiKeys.get(0));
                                        }
                                    } catch (Exception e) {
                                        log.error("토큰 재발급 실패: userId={}", userId, e);
                                    }
                                    return true;
                                }
                                        return ex.getStatusCode().is5xxServerError();
                                    }
                                    return false;
                                }))
                        .map(response -> {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> output = (Map<String, Object>) ((Map<String, Object>) response).get("output2");
                            if (output == null) {
                                throw new IllegalStateException("차트 데이터를 받을 수 없습니다: " + response);
                            }
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> chartData = (List<Map<String, Object>>) output.get("output2");
                            return chartData != null ? chartData : List.<Map<String, Object>>of();
                        }))
                .onErrorMap(error -> {
                    log.error("한국투자증권 차트 데이터 조회 실패: stockCode={}", stockCode, error);
                    return new IllegalStateException("차트 데이터 조회 실패", error);
                });
    }
    
    /**
     * 차트 데이터로부터 지표 계산
     */
    private IndicatorResponse calculateIndicator(List<Map<String, Object>> chartData, String indicator) {
        if (chartData == null || chartData.isEmpty()) {
            return createErrorResponse("차트 데이터가 없습니다");
        }
        
        // 차트 데이터를 숫자 배열로 변환
        double[] closes = chartData.stream()
                .mapToDouble(d -> Double.parseDouble(d.get("stck_clpr").toString()))
                .toArray();
        double[] volumes = chartData.stream()
                .mapToDouble(d -> Double.parseDouble(d.get("acml_vol").toString()))
                .toArray();
        double[] highs = chartData.stream()
                .mapToDouble(d -> Double.parseDouble(d.get("stck_hgpr").toString()))
                .toArray();
        double[] lows = chartData.stream()
                .mapToDouble(d -> Double.parseDouble(d.get("stck_lwpr").toString()))
                .toArray();
        
        IndicatorResponse.IndicatorResponseBuilder builder = IndicatorResponse.builder();
        
        switch (indicator.toLowerCase()) {
            case "rsi":
                double rsi = calculateRSI(closes, 14);
                builder.value(BigDecimal.valueOf(rsi));
                break;
            case "macd":
                double[] macdResult = calculateMACD(closes);
                builder.valueMacd(BigDecimal.valueOf(macdResult[0]));
                builder.valueMacdSignal(BigDecimal.valueOf(macdResult[1]));
                builder.valueMacdHist(BigDecimal.valueOf(macdResult[2]));
                break;
            case "ema":
                double ema20 = calculateEMA(closes, 20);
                double ema60 = calculateEMA(closes, 60);
                double ema120 = calculateEMA(closes, 120);
                builder.values(new BigDecimal[]{
                        BigDecimal.valueOf(ema20),
                        BigDecimal.valueOf(ema60),
                        BigDecimal.valueOf(ema120)
                });
                break;
            case "bbands":
                double[] bbResult = calculateBollingerBands(closes, 20, 2);
                builder.valueUpperBand(BigDecimal.valueOf(bbResult[0]));
                builder.valueMiddleBand(BigDecimal.valueOf(bbResult[1]));
                builder.valueLowerBand(BigDecimal.valueOf(bbResult[2]));
                break;
            case "atr":
                double atr = calculateATR(highs, lows, closes, 14);
                builder.valueAtr(BigDecimal.valueOf(atr));
                break;
            case "vwap":
                double vwap = calculateVWAP(closes, volumes);
                builder.value(BigDecimal.valueOf(vwap));
                break;
            default:
                return createErrorResponse("지원하지 않는 지표: " + indicator);
        }
        
        return builder.build();
    }
    
    /**
     * RSI 계산
     */
    private double calculateRSI(double[] closes, int period) {
        if (closes.length < period + 1) {
            return 50.0; // 기본값
        }
        
        double[] gains = new double[closes.length - 1];
        double[] losses = new double[closes.length - 1];
        
        for (int i = 1; i < closes.length; i++) {
            double change = closes[i] - closes[i - 1];
            gains[i - 1] = change > 0 ? change : 0;
            losses[i - 1] = change < 0 ? -change : 0;
        }
        
        double avgGain = 0;
        double avgLoss = 0;
        
        for (int i = 0; i < period; i++) {
            avgGain += gains[i];
            avgLoss += losses[i];
        }
        avgGain /= period;
        avgLoss /= period;
        
        for (int i = period; i < gains.length; i++) {
            avgGain = (avgGain * (period - 1) + gains[i]) / period;
            avgLoss = (avgLoss * (period - 1) + losses[i]) / period;
        }
        
        if (avgLoss == 0) {
            return 100.0;
        }
        
        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }
    
    /**
     * MACD 계산
     */
    private double[] calculateMACD(double[] closes) {
        double ema12 = calculateEMA(closes, 12);
        double ema26 = calculateEMA(closes, 26);
        double macd = ema12 - ema26;
        
        // Signal은 MACD의 9일 EMA (간단화를 위해 MACD 값 사용)
        double signal = macd * 0.9; // 근사값
        double hist = macd - signal;
        
        return new double[]{macd, signal, hist};
    }
    
    /**
     * EMA 계산
     */
    private double calculateEMA(double[] closes, int period) {
        if (closes.length < period) {
            return closes[closes.length - 1];
        }
        
        double multiplier = 2.0 / (period + 1);
        double ema = closes[0];
        
        for (int i = 1; i < closes.length; i++) {
            ema = (closes[i] * multiplier) + (ema * (1 - multiplier));
        }
        
        return ema;
    }
    
    /**
     * Bollinger Bands 계산
     */
    private double[] calculateBollingerBands(double[] closes, int period, double numStdDev) {
        if (closes.length < period) {
            double price = closes[closes.length - 1];
            return new double[]{price, price, price};
        }
        
        // SMA 계산
        double sum = 0;
        for (int i = closes.length - period; i < closes.length; i++) {
            sum += closes[i];
        }
        double sma = sum / period;
        
        // 표준편차 계산
        double variance = 0;
        for (int i = closes.length - period; i < closes.length; i++) {
            variance += Math.pow(closes[i] - sma, 2);
        }
        double stdDev = Math.sqrt(variance / period);
        
        double upper = sma + (numStdDev * stdDev);
        double lower = sma - (numStdDev * stdDev);
        
        return new double[]{upper, sma, lower};
    }
    
    /**
     * ATR 계산
     */
    private double calculateATR(double[] highs, double[] lows, double[] closes, int period) {
        if (highs.length < period + 1) {
            return 0;
        }
        
        double[] trueRanges = new double[highs.length - 1];
        for (int i = 1; i < highs.length; i++) {
            double tr1 = highs[i] - lows[i];
            double tr2 = Math.abs(highs[i] - closes[i - 1]);
            double tr3 = Math.abs(lows[i] - closes[i - 1]);
            trueRanges[i - 1] = Math.max(tr1, Math.max(tr2, tr3));
        }
        
        double sum = 0;
        for (int i = 0; i < period; i++) {
            sum += trueRanges[i];
        }
        
        return sum / period;
    }
    
    /**
     * VWAP 계산
     */
    private double calculateVWAP(double[] closes, double[] volumes) {
        if (closes.length != volumes.length || closes.length == 0) {
            return closes.length > 0 ? closes[closes.length - 1] : 0;
        }
        
        double totalValue = 0;
        double totalVolume = 0;
        
        for (int i = 0; i < closes.length; i++) {
            totalValue += closes[i] * volumes[i];
            totalVolume += volumes[i];
        }
        
        return totalVolume > 0 ? totalValue / totalVolume : closes[closes.length - 1];
    }
    
    /**
     * interval을 한국투자증권 API의 기간분할코드로 변환
     */
    private String convertIntervalToPeriodCode(String interval) {
        if (interval == null) {
            return "D"; // 일봉
        }
        
        String lowerInterval = interval.toLowerCase();
        
        if (lowerInterval.equals("1d") || lowerInterval.equals("1day")) {
            return "D"; // 일봉
        }
        if (lowerInterval.equals("1w") || lowerInterval.equals("1week")) {
            return "W"; // 주봉
        }
        if (lowerInterval.equals("1m") || lowerInterval.equals("1month")) {
            return "M"; // 월봉
        }
        
        return "D"; // 기본값: 일봉
    }
    
    /**
     * Base URL 가져오기 (실거래/모의투자)
     */
    private String getBaseUrl(String serverType) {
        if ("1".equals(serverType)) {
            return BASE_URL_VIRTUAL; // 모의투자
        }
        return BASE_URL_REAL; // 실거래
    }
    
    /**
     * 모의 지표 응답 생성 (개발/테스트용)
     */
    private IndicatorResponse createMockIndicatorResponse(String indicator, String symbol) {
        IndicatorResponse.IndicatorResponseBuilder builder = IndicatorResponse.builder();
        
        switch (indicator.toLowerCase()) {
            case "rsi":
                builder.value(new BigDecimal("50.5"));
                break;
            case "macd":
                builder.valueMacd(new BigDecimal("1.2"));
                builder.valueMacdSignal(new BigDecimal("1.0"));
                builder.valueMacdHist(new BigDecimal("0.2"));
                break;
            case "ema":
                builder.values(new BigDecimal[]{
                        new BigDecimal("50000"),
                        new BigDecimal("51000"),
                        new BigDecimal("52000")
                });
                break;
            case "bbands":
                builder.valueUpperBand(new BigDecimal("55000"));
                builder.valueMiddleBand(new BigDecimal("50000"));
                builder.valueLowerBand(new BigDecimal("45000"));
                break;
            case "atr":
                builder.valueAtr(new BigDecimal("2000"));
                break;
            case "vwap":
                builder.value(new BigDecimal("50500"));
                break;
            default:
                return createErrorResponse("지원하지 않는 지표: " + indicator);
        }
        
        return builder.build();
    }
    
    /**
     * 에러 응답 생성
     */
    private IndicatorResponse createErrorResponse(String error) {
        return IndicatorResponse.builder()
                .error(error)
                .build();
    }
}
