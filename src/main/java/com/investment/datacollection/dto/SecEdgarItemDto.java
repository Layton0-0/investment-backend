package com.investment.datacollection.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SEC EDGAR 제출(공시) 항목 DTO
 * data.sec.gov/submissions/CIK{cik}.json 의 filings.recent 컬럼형 배열에서 매핑.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecEdgarItemDto {

    private String accessionNumber;
    private String form;
    private String filingDate;
    private String primaryDocument;
    private String cik;
    private String companyName;
}
