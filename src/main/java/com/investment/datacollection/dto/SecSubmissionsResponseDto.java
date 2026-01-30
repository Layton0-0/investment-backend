package com.investment.datacollection.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * SEC data.sec.gov/submissions/CIK{cik}.json 응답 DTO
 * filings.recent 는 컬럼형 배열(accessionNumber[], filingDate[], form[], primaryDocument[] 등).
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecSubmissionsResponseDto {

    private String cik;
    private String name;

    @JsonProperty("filings")
    private FilingsWrapper filings;

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FilingsWrapper {
        @JsonProperty("recent")
        private RecentFilings recent;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RecentFilings {
        private List<String> accessionNumber;
        private List<String> form;
        private List<String> filingDate;
        private List<String> primaryDocument;
    }
}
