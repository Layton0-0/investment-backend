package com.investment.ai.client.impl;

import com.investment.ai.client.AiPredictionClient;
import com.investment.ai.dto.PredictionRequestDto;
import com.investment.ai.dto.PredictionResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

/**
 * Python FastAPI Í∏∞Î∞ò AI ?àÏ∏° ?úÎπÑ???¥Îùº?¥Ïñ∏??
 * 
 * ?∏Î? Python FastAPI ?úÎπÑ?§Ï? ?µÏã†?òÏó¨ AI ?àÏ∏°???òÌñâ?©Îãà??
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FastApiPredictionClient implements AiPredictionClient {
    
    private final WebClient.Builder webClientBuilder;
    
    @Value("${investment.ai.prediction-service.url:http://localhost:8000}")
    private String predictionServiceUrl;
    
    @Value("${investment.ai.prediction-service.timeout:5000}")
    private int timeoutMs;
    
    @Value("${investment.ai.prediction-service.retry-max-attempts:3}")
    private int maxRetryAttempts;
    
    private WebClient webClient;
    
    private WebClient getWebClient() {
        if (webClient == null) {
            webClient = webClientBuilder
                    .baseUrl(predictionServiceUrl)
                    .build();
        }
        return webClient;
    }
    
    @Override
    public Mono<PredictionResponseDto> predictPrice(PredictionRequestDto request) {
        log.debug("AI ?àÏ∏° ?îÏ≤≠: symbol={}, predictionMinutes={}, modelType={}", 
                request.getSymbol(), request.getPredictionMinutes(), request.getModelType());
        
        return getWebClient()
                .post()
                .uri("/api/v1/predict")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PredictionResponseDto.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .retryWhen(Retry.backoff(maxRetryAttempts, Duration.ofMillis(100))
                        .filter(throwable -> {
                            // ?¨Ïãú??Í∞Ä?•Ìïú ?àÏô∏Îß??ÑÌÑ∞Îß?
                            return !(throwable instanceof IllegalArgumentException);
                        })
                        .doBeforeRetry(retrySignal -> 
                                log.warn("AI ?àÏ∏° ?úÎπÑ???¨Ïãú?? attempt={}, symbol={}", 
                                        retrySignal.totalRetries() + 1, request.getSymbol())))
                .doOnError(error -> 
                        log.error("AI ?àÏ∏° ?§Ìå®: symbol={}, error={}", 
                                request.getSymbol(), error.getMessage()))
                .doOnSuccess(response -> 
                        log.debug("AI ?àÏ∏° ?±Í≥µ: symbol={}, predictedPrice={}, confidence={}", 
                                response.getSymbol(), response.getPredictedPrice(), response.getConfidence()));
    }
    
    @Override
    public Mono<List<PredictionResponseDto>> predictBatch(List<PredictionRequestDto> requests) {
        log.debug("AI Î∞∞Ïπò ?àÏ∏° ?îÏ≤≠: count={}", requests.size());
        
        return getWebClient()
                .post()
                .uri("/api/v1/predict/batch")
                .bodyValue(requests)
                .retrieve()
                .bodyToFlux(PredictionResponseDto.class)
                .collectList()
                .timeout(Duration.ofMillis(timeoutMs * requests.size()))
                .retryWhen(Retry.backoff(maxRetryAttempts, Duration.ofMillis(100))
                        .doBeforeRetry(retrySignal -> 
                                log.warn("AI Î∞∞Ïπò ?àÏ∏° ?¨Ïãú?? attempt={}", 
                                        retrySignal.totalRetries() + 1)))
                .doOnError(error -> 
                        log.error("AI Î∞∞Ïπò ?àÏ∏° ?§Ìå®: error={}", error.getMessage()))
                .doOnSuccess(responses -> 
                        log.debug("AI Î∞∞Ïπò ?àÏ∏° ?±Í≥µ: count={}", responses.size()));
    }
    
    @Override
    public Mono<Boolean> isModelReady() {
        return getWebClient()
                .get()
                .uri("/api/v1/health")
                .retrieve()
                .bodyToMono(String.class)
                .map("ok"::equalsIgnoreCase)
                .timeout(Duration.ofMillis(1000))
                .onErrorReturn(false)
                .doOnError(error -> log.warn("AI ?úÎπÑ???ÅÌÉú ?ïÏù∏ ?§Ìå®: {}", error.getMessage()));
    }
    
    @Override
    public String getProviderName() {
        return "fastapi-prediction";
    }
}
