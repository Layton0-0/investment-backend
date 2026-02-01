package com.investment.factor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 파이프라인 4단계 요약 DTO (자동투자 현황용).
 * 유니버스 수·시그널 건수(KR/US)·보유 포지션 수·목록.
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
    private int openPositionCount;
    private List<SignalScoreDto> signalListKr;
    private List<SignalScoreDto> signalListUs;
    private List<OpenPositionItemDto> openPositionList;
}
