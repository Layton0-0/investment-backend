package com.investment.datacollection.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Open DART 공시 목록 API 응답 (list.json)
 * 스펙: https://opendart.fss.or.kr/guide/main.do?apiGrpCd=DS001
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DartListResponseDto {

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("page_no")
    private Integer pageNo;

    @JsonProperty("page_count")
    private Integer pageCount;

    @JsonProperty("total_count")
    private Integer totalCount;

    @JsonProperty("total_page")
    private Integer totalPage;

    @JsonProperty("list")
    private List<DartListItemDto> list;

    public boolean isSuccess() {
        return "000".equals(status);
    }
}
