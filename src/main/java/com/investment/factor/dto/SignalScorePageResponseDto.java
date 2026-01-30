package com.investment.factor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 시그널/팩터 점수 목록 페이징 응답 DTO.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignalScorePageResponseDto {

    private List<SignalScoreDto> content;
    private PageMeta page;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageMeta {
        private int number;
        private int size;
        private long totalElements;
        private int totalPages;
    }
}
