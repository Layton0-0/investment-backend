package com.investment.analysis.service;

import com.investment.account.dto.AccountPositionDto;
import com.investment.account.dto.BalanceAndPositionsDto;
import com.investment.account.service.AccountService;
import com.investment.analysis.dto.SectorAnalysisResponseDto;
import com.investment.domain.entity.SectorReturn;
import com.investment.domain.entity.SymbolSector;
import com.investment.domain.repository.SectorReturnRepository;
import com.investment.domain.repository.SymbolSectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 포트폴리오·종목 목록의 섹터 비중 및 업종 수익률 분석.
 * TB_SYMBOL_SECTOR·TB_SECTOR_RETURN 기반. 데이터 없으면 UNKNOWN 섹터로 집계.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SectorAnalysisService {

    private final AccountService accountService;
    private final SymbolSectorRepository symbolSectorRepository;
    private final SectorReturnRepository sectorReturnRepository;

    private static final String UNKNOWN = "UNKNOWN";

    /**
     * 계좌 포지션 기준 섹터 비중 분석 (인증 사용자).
     *
     * @param userId    사용자 ID
     * @param accountNo 계좌번호
     * @return 섹터별 비중·평가액·최근 업종 수익률
     */
    public SectorAnalysisResponseDto getSectorAnalysisByAccount(String userId, String accountNo) {
        if (userId == null || accountNo == null) {
            return emptyResponse(null);
        }
        BalanceAndPositionsDto balanceAndPositions = accountService.getBalanceAndPositionsWithUserId(userId, accountNo);
        if (balanceAndPositions == null || balanceAndPositions.getPositions() == null
                || balanceAndPositions.getPositions().isEmpty()) {
            return emptyResponse(null);
        }
        BigDecimal totalValue = balanceAndPositions.getPositions().stream()
                .map(AccountPositionDto::getTotalValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String market = inferMarketFromPositions(balanceAndPositions.getPositions());
        return buildSectorResponse(market, balanceAndPositions.getPositions(), totalValue);
    }

    /**
     * 종목·평가액 목록 기준 섹터 비중 분석 (symbol -> value).
     *
     * @param market       시장 (KR/US)
     * @param symbolValues 심볼별 평가액
     * @return 섹터별 비중·평가액
     */
    public SectorAnalysisResponseDto getSectorAnalysisBySymbols(String market,
                                                                Map<String, BigDecimal> symbolValues) {
        if (symbolValues == null || symbolValues.isEmpty()) {
            return emptyResponse(market);
        }
        String marketNorm = Optional.ofNullable(market).orElse("US").toUpperCase();
        List<AccountPositionDto> positions = symbolValues.entrySet().stream()
                .map(e -> AccountPositionDto.builder()
                        .symbol(e.getKey())
                        .name("")
                        .quantity(0)
                        .averagePrice(BigDecimal.ZERO)
                        .currentPrice(BigDecimal.ZERO)
                        .totalValue(e.getValue() != null ? e.getValue() : BigDecimal.ZERO)
                        .profitLoss(BigDecimal.ZERO)
                        .profitLossRate(BigDecimal.ZERO)
                        .currency("KRW")
                        .market(marketNorm)
                        .build())
                .collect(Collectors.toList());
        BigDecimal totalValue = positions.stream()
                .map(AccountPositionDto::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return buildSectorResponse(marketNorm, positions, totalValue);
    }

    private String inferMarketFromPositions(List<AccountPositionDto> positions) {
        return positions.stream()
                .map(AccountPositionDto::getMarket)
                .filter(Objects::nonNull)
                .findFirst()
                .map(s -> s.toUpperCase())
                .orElse("US");
    }

    private SectorAnalysisResponseDto emptyResponse(String market) {
        return SectorAnalysisResponseDto.builder()
                .market(Optional.ofNullable(market).orElse("US"))
                .totalValue(BigDecimal.ZERO)
                .sectors(List.of())
                .build();
    }

    private SectorAnalysisResponseDto buildSectorResponse(String market, List<AccountPositionDto> positions,
                                                          BigDecimal totalValue) {
        List<String> symbols = positions.stream().map(AccountPositionDto::getSymbol).distinct().collect(Collectors.toList());
        List<SymbolSector> symbolSectors = symbolSectorRepository.findByMarketAndSymbolIn(market, symbols);
        Map<String, String> symbolToSector = symbolSectors.stream()
                .collect(Collectors.toMap(SymbolSector::getSymbol, SymbolSector::getSectorCode, (a, b) -> a));

        Map<String, BigDecimal> sectorToValue = new HashMap<>();
        for (AccountPositionDto p : positions) {
            BigDecimal value = p.getTotalValue() != null ? p.getTotalValue() : BigDecimal.ZERO;
            String sector = symbolToSector.getOrDefault(p.getSymbol(), UNKNOWN);
            sectorToValue.merge(sector, value, BigDecimal::add);
        }

        LocalDate latestBasDt = LocalDate.now();
        List<SectorReturn> sectorReturns = sectorReturnRepository.findByBasDtAndMarketOrderByReturnPctDesc(latestBasDt, market);
        if (sectorReturns.isEmpty()) {
            sectorReturns = sectorReturnRepository.findByBasDtAndMarketOrderByReturnPctDesc(latestBasDt.minusDays(1), market);
        }
        Map<String, BigDecimal> sectorReturnPct = sectorReturns.stream()
                .collect(Collectors.toMap(SectorReturn::getSectorCode, SectorReturn::getReturnPct, (a, b) -> a));

        List<SectorAnalysisResponseDto.SectorWeightItem> items = sectorToValue.entrySet().stream()
                .map(e -> {
                    BigDecimal value = e.getValue();
                    BigDecimal weightPct = totalValue.compareTo(BigDecimal.ZERO) > 0
                            ? value.divide(totalValue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                            : BigDecimal.ZERO;
                    return SectorAnalysisResponseDto.SectorWeightItem.builder()
                            .sectorCode(e.getKey())
                            .sectorName(UNKNOWN.equals(e.getKey()) ? "미분류" : e.getKey())
                            .weightPct(weightPct.setScale(2, RoundingMode.HALF_UP))
                            .notionalValue(value.setScale(2, RoundingMode.HALF_UP))
                            .returnPct(sectorReturnPct.get(e.getKey()))
                            .build();
                })
                .sorted(Comparator.comparing(SectorAnalysisResponseDto.SectorWeightItem::getWeightPct).reversed())
                .collect(Collectors.toList());

        return SectorAnalysisResponseDto.builder()
                .market(market)
                .totalValue(totalValue != null ? totalValue.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .sectors(items)
                .build();
    }
}
