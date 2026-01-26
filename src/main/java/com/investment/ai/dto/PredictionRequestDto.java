package com.investment.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

/**
 * AI 예측 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionRequestDto {
    
    /**
     * 종목 코드 (예: AAPL, TSLA, 005930)
     */
    @NotBlank(message = "종목 코드는 필수입니다")
    private String symbol;
    
    /**
     * 예측 기간 (분 단위)
     * 예: 60 = 1시간 후, 1440 = 1일 후
     */
    @NotNull(message = "예측 기간은 필수입니다")
    @Positive(message = "예측 기간은 양수여야 합니다")
    private Integer predictionMinutes;
    
    /**
     * 모델 타입 (lstm, transformer, ensemble)
     * 기본값: ensemble
     */
    private String modelType = "ensemble";
    
    /**
     * 과거 데이터 기간 (일 단위)
     * 기본값: 30일
     */
    @Builder.Default
    private Integer lookbackDays = 30;
    
    /**
     * 요청 시각 (캐싱 키 생성용)
     */
    private LocalDateTime requestedAt;
}
