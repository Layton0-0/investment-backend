package com.investment.ops.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

/**
 * 자동매매 준비 상태 API 응답.
 * 09:10 자동매수 실행 전 관리자 점검용.
 */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "자동매매 준비 상태 (자동투자 ON 계좌 수, 전일 데이터 건수, 활성 halt 수)")
public class AutoTradingReadinessDto {

    @Schema(description = "기준일 (파이프라인에서 사용하는 basDt, 전일)", example = "2026-02-26")
    LocalDate basDt;

    @Schema(description = "자동투자 ON인 거래설정(계좌) 수")
    long autoTradingOnAccountCount;

    @Schema(description = "기준일 TB_DAILY_STOCK 건수 (KR+US)")
    long dailyStockRowCount;

    @Schema(description = "기준일 TB_DAILY_STOCK KR 건수 (한국 시그널 0원인 규명용)")
    Long dailyStockRowCountKr;

    @Schema(description = "기준일 TB_DAILY_STOCK US 건수")
    Long dailyStockRowCountUs;

    @Schema(description = "기준일 TB_SIGNAL_SCORE 건수")
    long signalScoreRowCount;

    @Schema(description = "기준일 TB_SIGNAL_SCORE KR 건수 (한국 시그널 0원인 규명용)")
    Long signalScoreRowCountKr;

    @Schema(description = "기준일 TB_SIGNAL_SCORE US 건수")
    Long signalScoreRowCountUs;

    @Schema(description = "활성 거버넌스 halt 수 (해당 조합은 파이프라인 run 스킵)")
    int activeGovernanceHaltCount;
}
