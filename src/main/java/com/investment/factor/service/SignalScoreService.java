package com.investment.factor.service;

import com.investment.domain.entity.SignalScore;
import com.investment.domain.repository.SignalScoreRepository;
import com.investment.factor.dto.SignalScoreDto;
import com.investment.factor.dto.SignalScorePageResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 시그널/팩터 점수 조회 서비스 (API용).
 */
@Service
@RequiredArgsConstructor
public class SignalScoreService {

    private final SignalScoreRepository signalScoreRepository;

    /**
     * 시그널/팩터 점수 목록 조회 (필터·페이징).
     *
     * @param basDt      기준일 (null이면 미필터)
     * @param market     시장 (null이면 미필터)
     * @param symbol     종목코드 (null이면 미필터)
     * @param factorType 팩터 유형 (null이면 미필터)
     * @param page       페이지 (0부터)
     * @param size       페이지 크기
     * @return 페이징 응답
     */
    public SignalScorePageResponseDto getSignals(LocalDate basDt, String market, String symbol, String factorType, int page, int size) {
        size = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(page, size);
        Page<SignalScore> result = signalScoreRepository.findByFilters(basDt, market, symbol, factorType, pageable);
        List<SignalScoreDto> content = result.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        SignalScorePageResponseDto.PageMeta meta = SignalScorePageResponseDto.PageMeta.builder()
                .number(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
        return SignalScorePageResponseDto.builder()
                .content(content)
                .page(meta)
                .build();
    }

    /**
     * 기준일·시장별 시그널 건수 조회 (자동투자 현황용).
     *
     * @param basDt  기준일
     * @param market 시장 (KR, US)
     * @return 시그널 건수 (종목×팩터 타입별 행 수)
     */
    public long countSignals(LocalDate basDt, String market) {
        return signalScoreRepository.countByBasDtAndMarket(basDt, market);
    }

    private SignalScoreDto toDto(SignalScore e) {
        return SignalScoreDto.builder()
                .basDt(e.getBasDt())
                .symbol(e.getSymbol())
                .market(e.getMarket())
                .factorType(e.getFactorType())
                .score(e.getScore())
                .metadata(e.getMetadata())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
