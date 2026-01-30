package com.investment.datacollection.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * KRX Open API 일별매매정보 응답 (구조는 서비스별 상이할 수 있음)
 * 1단계: 연동·파싱용. 저장은 2단계에서 별도 테이블 검토.
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
