package com.investment.factor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 시그널/팩터 점수 DTO (API 응답용).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignalScoreDto {

    private LocalDate basDt;
    private String symbol;
    private String market;
    private String factorType;
    private BigDecimal score;
    private String metadata;
    private LocalDateTime createdAt;
}
