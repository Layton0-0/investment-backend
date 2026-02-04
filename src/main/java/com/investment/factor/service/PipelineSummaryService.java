package com.investment.factor.service;

import com.investment.domain.entity.StrategyPosition;
import com.investment.domain.entity.TradingSetting;
import com.investment.domain.repository.StrategyPositionRepository;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.domain.repository.UniverseRepository;
import com.investment.factor.dto.OpenPositionItemDto;
import com.investment.factor.dto.PipelineSummaryDto;
import com.investment.factor.dto.SignalScoreDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 파이프라인 4단계 요약 조회 서비스 (자동투자 현황용).
 * 유니버스 수·시그널 건수(KR/US)·자금 배분 요약·보유 포지션 수·목록을 한 번에 조회.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineSummaryService {

    private static final int SIGNAL_LIST_SIZE = 10;
    private static final BigDecimal DEFAULT_SHORT = new BigDecimal("0.2");
    private static final BigDecimal DEFAULT_MEDIUM = new BigDecimal("0.4");
    private static final BigDecimal DEFAULT_LONG = new BigDecimal("0.4");

    private final UniverseRepository universeRepository;
    private final SignalScoreService signalScoreService;
    private final StrategyPositionRepository strategyPositionRepository;
    private final TradingSettingRepository tradingSettingRepository;

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
        String allocationSummary = null;

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
                List<StrategyPosition> positions = strategyPositionRepository
                        .findByAccountNoAndExitDtIsNullOrderByEntryDtAsc(accountNo);
                openPositionList = positions.stream()
                        .map(this::toOpenPositionItemDto)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                log.debug("보유 포지션 조회 실패(스킵): {}", e.getMessage());
            }
            allocationSummary = buildAllocationSummary(accountNo);
        }

        return PipelineSummaryDto.builder()
                .basDt(basDt)
                .universeCountKr(universeCountKr)
                .universeCountUs(universeCountUs)
                .signalCountKr(signalCountKr)
                .signalCountUs(signalCountUs)
                .allocationSummary(allocationSummary)
                .openPositionCount(openPositionCount)
                .signalListKr(signalListKr)
                .signalListUs(signalListUs)
                .openPositionList(openPositionList)
                .build();
    }

    /**
     * 계좌 거래 설정 기준 예상 배분(단기/중기/장기) 요약 문자열 생성.
     * 설정 없거나 최대 투자금 0이면 null.
     */
    private String buildAllocationSummary(String accountNo) {
        if (accountNo == null || accountNo.trim().isEmpty()) {
            return null;
        }
        return tradingSettingRepository.findByAccountNo(accountNo)
                .filter(s -> s.getMaxInvestmentAmount() != null
                        && s.getMaxInvestmentAmount().compareTo(BigDecimal.ZERO) > 0)
                .map(this::formatAllocationSummary)
                .orElse(null);
    }

    private String formatAllocationSummary(TradingSetting s) {
        BigDecimal max = s.getMaxInvestmentAmount();
        BigDecimal shortPct = s.getShortTermRatio() != null ? s.getShortTermRatio() : DEFAULT_SHORT;
        BigDecimal midPct = s.getMediumTermRatio() != null ? s.getMediumTermRatio() : DEFAULT_MEDIUM;
        BigDecimal longPct = s.getLongTermRatio() != null ? s.getLongTermRatio() : DEFAULT_LONG;
        BigDecimal shortAmt = max.multiply(shortPct).setScale(0, RoundingMode.DOWN);
        BigDecimal midAmt = max.multiply(midPct).setScale(0, RoundingMode.DOWN);
        BigDecimal longAmt = max.multiply(longPct).setScale(0, RoundingMode.DOWN);
        return "단기 " + formatAmountKr(shortAmt) + " · 중기 " + formatAmountKr(midAmt) + " · 장기 "
                + formatAmountKr(longAmt);
    }

    private String formatAmountKr(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            return "0";
        }
        long val = amount.longValue();
        if (val >= 100_000_000) {
            return (val / 100_000_000) + "억";
        }
        long man = val / 10_000;
        return String.format("%,d만", man);
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
