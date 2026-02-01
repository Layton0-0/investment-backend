package com.investment.factor.service;

import com.investment.domain.entity.StrategyPosition;
import com.investment.domain.repository.StrategyPositionRepository;
import com.investment.domain.repository.UniverseRepository;
import com.investment.factor.dto.OpenPositionItemDto;
import com.investment.factor.dto.PipelineSummaryDto;
import com.investment.factor.dto.SignalScoreDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 파이프라인 4단계 요약 조회 서비스 (자동투자 현황용).
 * 유니버스 수·시그널 건수(KR/US)·보유 포지션 수·목록을 한 번에 조회.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineSummaryService {

    private static final int SIGNAL_LIST_SIZE = 10;

    private final UniverseRepository universeRepository;
    private final SignalScoreService signalScoreService;
    private final StrategyPositionRepository strategyPositionRepository;

    /**
     * 기준일·계좌에 대한 파이프라인 요약 조회.
     *
     * @param basDt     기준일 (유니버스·시그널 기준)
     * @param accountNo 계좌번호 (보유 포지션용, null이면 0건·빈 목록)
     * @return 파이프라인 요약 DTO
     */
    public PipelineSummaryDto getSummary(LocalDate basDt, String accountNo) {
        long universeCountKr = 0L;
        long universeCountUs = 0L;
        long signalCountKr = 0L;
        long signalCountUs = 0L;
        List<SignalScoreDto> signalListKr = Collections.emptyList();
        List<SignalScoreDto> signalListUs = Collections.emptyList();
        int openPositionCount = 0;
        List<OpenPositionItemDto> openPositionList = Collections.emptyList();

        try {
            universeCountKr = universeRepository.countByBasDtAndMarket(basDt, "KR");
            universeCountUs = universeRepository.countByBasDtAndMarket(basDt, "US");
        } catch (Exception e) {
            log.debug("유니버스 건수 조회 실패(스킵): {}", e.getMessage());
        }

        try {
            signalCountKr = signalScoreService.countSignals(basDt, "KR");
            signalCountUs = signalScoreService.countSignals(basDt, "US");
            signalListKr = signalScoreService.getSignals(basDt, "KR", null, null, 0, SIGNAL_LIST_SIZE).getContent();
            signalListUs = signalScoreService.getSignals(basDt, "US", null, null, 0, SIGNAL_LIST_SIZE).getContent();
        } catch (Exception e) {
            log.debug("시그널 조회 실패(스킵): {}", e.getMessage());
        }

        if (accountNo != null && !accountNo.trim().isEmpty()) {
            try {
                openPositionCount = (int) strategyPositionRepository.countByAccountNoAndExitDtIsNull(accountNo);
                List<StrategyPosition> positions = strategyPositionRepository.findByAccountNoAndExitDtIsNullOrderByEntryDtAsc(accountNo);
                openPositionList = positions.stream()
                        .map(this::toOpenPositionItemDto)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                log.debug("보유 포지션 조회 실패(스킵): {}", e.getMessage());
            }
        }

        return PipelineSummaryDto.builder()
                .basDt(basDt)
                .universeCountKr(universeCountKr)
                .universeCountUs(universeCountUs)
                .signalCountKr(signalCountKr)
                .signalCountUs(signalCountUs)
                .openPositionCount(openPositionCount)
                .signalListKr(signalListKr)
                .signalListUs(signalListUs)
                .openPositionList(openPositionList)
                .build();
    }

    private OpenPositionItemDto toOpenPositionItemDto(StrategyPosition p) {
        return OpenPositionItemDto.builder()
                .positionId(p.getId())
                .symbol(p.getSymbol())
                .market(p.getMarket())
                .quantity(p.getQuantity())
                .entryPrice(p.getEntryPrice())
                .entryDt(p.getEntryDt())
                .build();
    }
}
