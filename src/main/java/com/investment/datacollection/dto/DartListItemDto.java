package com.investment.datacollection.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Open DART 공시 목록 항목 (list[].item)
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DartListItemDto {

    @JsonProperty("corp_cls")
    private String corpCls;

    @JsonProperty("corp_name")
    private String corpName;

    @JsonProperty("corp_code")
    private String corpCode;

    @JsonProperty("stock_code")
    private String stockCode;

    @JsonProperty("report_nm")
    private String reportNm;

    @JsonProperty("rcept_no")
    private String rceptNo;

    @JsonProperty("flr_nm")
    private String flrNm;

    @JsonProperty("rcept_dt")
    private String rceptDt;

    @JsonProperty("rm")
    private String rm;
}
