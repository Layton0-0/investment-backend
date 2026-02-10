package com.investment.ai.client.impl;

import com.investment.ai.client.AiPredictionClient;
import com.investment.ai.dto.PredictionRequestDto;
import com.investment.ai.dto.PredictionResponseDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

/**
 * Python FastAPI 기반 AI 예측 서비스를 제공하는 클라이언트
 * 
 * 이 클라이언트는 Python FastAPI 서버와 통신하여 AI 예측 기능을 제공합니다.
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
                log.debug("AI 예측 요청: symbol={}, predictionMinutes={}, modelType={}",
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
                                                        // 네트워크 오류만 재시도
                                                        return !(throwable instanceof IllegalArgumentException);
                                                })
                                                .doBeforeRetry(retrySignal -> log.warn(
                                                                "AI 예측 서비스 재시도: attempt={}, symbol={}",
                                                                retrySignal.totalRetries() + 1, request.getSymbol())))
                                .doOnError(error -> log.error("AI 예측 실패: symbol={}, error={}",
                                                request.getSymbol(), error.getMessage()))
                                .doOnSuccess(response -> log.debug(
                                                "AI 예측 성공: symbol={}, predictedPrice={}, confidence={}",
                                                response.getSymbol(), response.getPredictedPrice(),
                                                response.getConfidence()));
        }

        @SuppressWarnings("unused")
        public Mono<PredictionResponseDto> predictPriceFallback(PredictionRequestDto request, Exception e) {
                log.warn("AI 예측 서비스 fallback: symbol={}, error={}", request.getSymbol(), e.getMessage());
                return Mono.empty();
        }

        @Override
        @CircuitBreaker(name = "aiPredictionService", fallbackMethod = "predictBatchFallback")
        public Mono<List<PredictionResponseDto>> predictBatch(List<PredictionRequestDto> requests) {
                log.debug("AI 배치 예측 요청: count={}", requests.size());

                return getWebClient()
                                .post()
                                .uri("/api/v1/predict/batch")
                                .bodyValue(requests)
                                .retrieve()
                                .bodyToMono(new ParameterizedTypeReference<List<PredictionResponseDto>>() { })
                                .timeout(Duration.ofMillis(timeoutMs * requests.size()))
                                .retryWhen(Retry.backoff(maxRetryAttempts, Duration.ofMillis(100))
                                                .doBeforeRetry(retrySignal -> log.warn("AI 배치 예측 재시도: attempt={}",
                                                                retrySignal.totalRetries() + 1)))
                                .doOnError(error -> log.error("AI 배치 예측 실패: error={}", error.getMessage()))
                                .doOnSuccess(responses -> log.debug("AI 배치 예측 성공: count={}", responses.size()));
        }

        @SuppressWarnings("unused")
        public Mono<List<PredictionResponseDto>> predictBatchFallback(List<PredictionRequestDto> requests,
                        Exception e) {
                log.warn("AI 배치 예측 fallback: count={}, error={}", requests.size(), e.getMessage());
                return Mono.just(List.of());
        }

        @Override
        public Mono<Boolean> isModelReady() {
                return getWebClient()
                                .get()
                                .uri("/api/v1/health")
                                .retrieve()
                                .toBodilessEntity()
                                .map(r -> r.getStatusCode().is2xxSuccessful())
                                .timeout(Duration.ofMillis(1000))
                                .onErrorReturn(false)
                                .doOnError(error -> log.warn("AI 서비스 헬스 체크 실패: {}", error.getMessage()));
        }

        @Override
        public String getProviderName() {
                return "fastapi-prediction";
        }

}
