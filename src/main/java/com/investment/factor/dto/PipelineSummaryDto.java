package com.investment.factor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 파이프라인 4단계 요약 DTO (자동투자 현황용).
 * 유니버스 수·시그널 건수(KR/US)·자금 배분 요약·보유 포지션 수·목록.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineSummaryDto {

    private LocalDate basDt;
    private long universeCountKr;
    private long universeCountUs;
    private long signalCountKr;
    private long signalCountUs;
    /** 3단계 자금 관리 배분 요약 (예: "단기 2,000만 · 중기 4,000만 · 장기 4,000만"). 설정 없으면 null. */
    private String allocationSummary;
    /** 3단계 자금 배분 비율 문자열 (예: "단기 40% / 중기 35% / 장기 25%"). 설정 없으면 null. */
    private String allocationRatioSummary;
    private int openPositionCount;
    private List<SignalScoreDto> signalListKr;
    private List<SignalScoreDto> signalListUs;
    private List<OpenPositionItemDto> openPositionList;
    /** 파이프라인 마지막 실행 시각 (KST). 배치/수동 실행 시 갱신, 없으면 null */
    private LocalDateTime lastRunAt;
}
