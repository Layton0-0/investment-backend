package com.investment.ai.client.impl;

import com.investment.ai.dto.PredictionRequestDto;
import com.investment.ai.dto.PredictionResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FastApiPredictionClient")
class FastApiPredictionClientTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @InjectMocks
    private FastApiPredictionClient client;

    @Test
    @DisplayName("predictPriceFallback 예외 시 Mono.empty 반환")
    void predictPriceFallback_returnsEmpty() {
        PredictionRequestDto request = PredictionRequestDto.builder()
                .symbol("005930")
                .predictionMinutes(60)
                .lookbackDays(30)
                .requestedAt(LocalDateTime.now())
                .build();

        Mono<PredictionResponseDto> result = client.predictPriceFallback(request, new RuntimeException("timeout"));

        StepVerifier.create(result)
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    @DisplayName("predictBatchFallback 예외 시 빈 리스트 Mono 반환")
    void predictBatchFallback_returnsEmptyList() {
        List<PredictionRequestDto> requests = List.of(
                PredictionRequestDto.builder()
                        .symbol("005930")
                        .predictionMinutes(60)
                        .lookbackDays(30)
                        .requestedAt(LocalDateTime.now())
                        .build()
        );

        Mono<List<PredictionResponseDto>> result = client.predictBatchFallback(requests, new RuntimeException("503"));

        StepVerifier.create(result)
                .assertNext(list -> assertTrue(list.isEmpty()))
                .verifyComplete();
    }

    @Test
    @DisplayName("getProviderName fastapi-prediction 반환")
    void getProviderName_returnsFastApiPrediction() {
        assertEquals("fastapi-prediction", client.getProviderName());
    }
}
