package com.investment.datacollection.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * KRX Open API 유가증권 일별매매정보(stk_bydd_trd) 응답.
 * OutBlock_1 행: BAS_DD(기준일자), ISU_CD(종목코드), ISU_NM(종목명), MKT_NM(시장구분),
 * SECT_TP_NM(소속부), TDD_CLSPRC(종가), CMPPREVDD_PRC(대비), FLUC_RT(등락률),
 * TDD_OPNPRC(시가), TDD_HGPRC(고가), TDD_LWPRC(저가), ACC_TRDVOL(거래량),
 * ACC_TRDVAL(거래대금), MKTCAP(시가총액), LIST_SHRS(상장주식수).
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
