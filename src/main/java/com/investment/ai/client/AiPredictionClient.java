package com.investment.ai.client;

import com.investment.ai.dto.PredictionRequestDto;
import com.investment.ai.dto.PredictionResponseDto;
import reactor.core.publisher.Mono;

/**
 * AI 예측 서비스 클라이언트 인터페이스
 * 
 * 외부 AI/ML 서비스 (Python FastAPI 등)와 통신하는 인터페이스입니다.
 * 다양한 AI 모델 (LSTM, Transformer 등)의 예측 결과를 통합합니다.
 */
public interface AiPredictionClient {
    
    /**
     * 종목 가격 예측 수행
     * 
     * @param request 예측 요청 (종목 코드, 예측 기간 등)
     * @return 예측 결과 (가격, 신뢰도, 변동성 등)
     */
    Mono<PredictionResponseDto> predictPrice(PredictionRequestDto request);
    
    /**
     * 배치 예측 수행 (여러 종목 동시)
     * 
     * @param requests 예측 요청 목록
     * @return 예측 결과 목록
     */
    Mono<java.util.List<PredictionResponseDto>> predictBatch(
            java.util.List<PredictionRequestDto> requests);
    
    /**
     * 모델 상태 확인
     * 
     * @return 모델이 사용 가능한지 여부
     */
    Mono<Boolean> isModelReady();
    
    /**
     * 서비스 제공자 이름
     */
    String getProviderName();
}
