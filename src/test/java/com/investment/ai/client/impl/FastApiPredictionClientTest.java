package com.investment.ai.client.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.investment.ai.dto.PredictionRequestDto;
import com.investment.ai.dto.PredictionResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FastApiPredictionClient")
class FastApiPredictionClientTest {

    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private WebClient.Builder webClientBuilder;

    @InjectMocks
    private FastApiPredictionClient client;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(client, "predictionServiceUrl", "http://localhost:8000");
        ReflectionTestUtils.setField(client, "timeoutMs", 5000);
        ReflectionTestUtils.setField(client, "maxRetryAttempts", 2);
    }

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

    @Test
    @DisplayName("predictBatch JSON 배열 응답을 List로 역직렬화")
    void predictBatch_deserializesJsonArrayToList() throws Exception {
        List<PredictionResponseDto> mockResponses = List.of(
                PredictionResponseDto.builder()
                        .symbol("005930")
                        .currentPrice(new java.math.BigDecimal("70000"))
                        .predictedPrice(new java.math.BigDecimal("71000"))
                        .expectedReturn(new java.math.BigDecimal("1.43"))
                        .confidence(new java.math.BigDecimal("0.75"))
                        .direction("UP")
                        .modelType("ensemble")
                        .predictedAt(LocalDateTime.now())
                        .predictionMinutes(60)
                        .build(),
                PredictionResponseDto.builder()
                        .symbol("000660")
                        .currentPrice(new java.math.BigDecimal("80000"))
                        .predictedPrice(new java.math.BigDecimal("79500"))
                        .expectedReturn(new java.math.BigDecimal("-0.63"))
                        .confidence(new java.math.BigDecimal("0.70"))
                        .direction("DOWN")
                        .modelType("ensemble")
                        .predictedAt(LocalDateTime.now())
                        .predictionMinutes(60)
                        .build()
        );
        String jsonArray = objectMapper.writeValueAsString(mockResponses);
        DataBuffer buffer = new DefaultDataBufferFactory().wrap(jsonArray.getBytes(StandardCharsets.UTF_8));
        ExchangeFunction exchangeFunction = request -> Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body(Flux.just(buffer))
                        .build()
        );
        WebClient testWebClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(testWebClient);

        List<PredictionRequestDto> requests = List.of(
                PredictionRequestDto.builder().symbol("005930").predictionMinutes(60).build(),
                PredictionRequestDto.builder().symbol("000660").predictionMinutes(60).build()
        );

        Mono<List<PredictionResponseDto>> result = client.predictBatch(requests);

        StepVerifier.create(result)
                .assertNext(list -> {
                    assertEquals(2, list.size());
                    assertEquals("005930", list.get(0).getSymbol());
                    assertEquals("000660", list.get(1).getSymbol());
                    assertEquals(new java.math.BigDecimal("71000"), list.get(0).getPredictedPrice());
                })
                .verifyComplete();
    }
}
