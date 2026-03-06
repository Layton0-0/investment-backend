package com.investment.datacollection.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * KRX Open API 일별매매정보 응답 (stk_bydd_trd, ksq_bydd_trd 동일 OutBlock_1).
 * 명세: docs/04-api/12-krx-api-spec/01-stk-bydd-trd.md, 06-ksq-bydd-trd.md.
 * OutBlock_1: BAS_DD, ISU_CD, ISU_NM, MKT_NM, SECT_TP_NM, TDD_CLSPRC, CMPPREVDD_PRC,
 * FLUC_RT, TDD_OPNPRC, TDD_HGPRC, TDD_LWPRC, ACC_TRDVOL, ACC_TRDVAL, MKTCAP, LIST_SHRS.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KrxDailyStockResponseDto {

    @JsonProperty("OutBlock_1")
    private List<Map<String, Object>> outBlock1;

    @JsonProperty("result")
    private String result;

    @JsonProperty("message")
    private String message;
}
