package com.investment.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 섹터 분석 API 응답 DTO.
 * 포트폴리오 또는 종목 목록의 섹터별 비중·수익 기여도를 반환한다.
 */
@Schema(description = "섹터 분석 결과")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectorAnalysisResponseDto {

    @Schema(description = "기준 시장 (KR/US)")
    private String market;

    @Schema(description = "총 평가액 (계좌 기준일 때)")
    private BigDecimal totalValue;

    @Schema(description = "섹터별 비중·수익률 목록")
    private List<SectorWeightItem> sectors;

    @Schema(description = "섹터별 비중 1건")
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectorWeightItem {
        @Schema(description = "섹터 코드")
        private String sectorCode;
        @Schema(description = "섹터명 (표시용, 미매핑 시 null)")
        private String sectorName;
        @Schema(description = "비중 (%)")
        private BigDecimal weightPct;
        @Schema(description = "해당 섹터 평가액")
        private BigDecimal notionalValue;
        @Schema(description = "최근 업종 수익률 (%) - TB_SECTOR_RETURN 기준일 있으면")
        private BigDecimal returnPct;
    }
}
