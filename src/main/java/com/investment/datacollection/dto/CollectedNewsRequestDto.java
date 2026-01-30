package com.investment.datacollection.dto;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 수집 뉴스 일괄 등록 요청 (내부 API)
 */
@Getter
@Setter
@NoArgsConstructor
public class CollectedNewsRequestDto {

    @Valid
    private List<CollectedNewsItemDto> items;
}
