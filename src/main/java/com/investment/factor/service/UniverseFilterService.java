package com.investment.factor.service;

import com.investment.domain.entity.DailyStock;
import com.investment.domain.entity.EarningsSurprise;
import com.investment.domain.entity.SectorReturn;
import com.investment.domain.entity.SymbolSector;
import com.investment.domain.entity.Universe;
import com.investment.domain.repository.DailyStockRepository;
import com.investment.domain.repository.EarningsSurpriseRepository;
import com.investment.domain.repository.SectorReturnRepository;
import com.investment.domain.repository.SymbolSectorRepository;
import com.investment.domain.repository.UniverseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 1단계 유니버스 필터링.
 * 유동성(Liquidity Cut-off) 기준 통과 종목만 TB_UNIVERSE에 저장.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UniverseFilterService {

    private final DailyStockRepository dailyStockRepository;
    private final UniverseRepository universeRepository;
    private final SectorReturnRepository sectorReturnRepository;
    private final SymbolSectorRepository symbolSectorRepository;
    private final EarningsSurpriseRepository earningsSurpriseRepository;

    /** 유동성 최소 거래대금 (원). 공통 Liquidity Cut-off */
    @Value("${investment.factor.liquidity-min-trd-val:1000000000}")
    private long liquidityMinTrdVal = 1_000_000_000L;

    /** Sector RS: 상위 N개 업종만 유니버스에 포함 (미설정 시 5) */
    @Value("${investment.factor.sector-rs-top-n:5}")
    private int sectorRsTopN = 5;

    /** Post-Earnings Drift: 최근 N일 이내 실적 발표만 대상 (미설정 시 90) */
    @Value("${investment.factor.earnings-surprise-lookback-days:90}")
    private int earningsSurpriseLookbackDays = 90;

    /** Post-Earnings Drift: 상위 N% 종목만 유니버스에 포함 (0.2 = 20%) */
    @Value("${investment.factor.earnings-surprise-top-pct:0.2}")
    private double earningsSurpriseTopPct = 0.2;

    /**
     * 기준일·시장에 대해 유니버스 필터 실행.
     * 유동성 필터 + (한국) Sector Relative Strength 필터 적용.
     *
     * @param basDt   기준일
     * @param market 시장 (KR, US)
     * @return 저장된 유니버스 종목 수
     */
    @Transactional
    public int run(LocalDate basDt, String market) {
        universeRepository.deleteByBasDtAndMarket(basDt, market);
        
        // 1. 유동성 필터
        List<DailyStock> liquidityPassed = dailyStockRepository.findByBasDtAndMarketAndTrdValGreaterThanEqual(
                basDt, market, liquidityMinTrdVal);
        if (liquidityPassed.isEmpty()) {
            log.debug("유니버스 필터: basDt={}, market={}, 유동성 통과 종목 없음", basDt, market);
            return 0;
        }

        // 2. 시장별 추가 필터 적용
        List<String> finalSymbols;
        if ("KR".equals(market)) {
            // 한국: Sector Relative Strength 필터 (현재 스텁)
            finalSymbols = filterBySectorRelativeStrength(basDt, market, liquidityPassed);
        } else if ("US".equals(market)) {
            // 미국: Post-Earnings Drift 필터 (현재 스텁)
            finalSymbols = filterByPostEarningsDrift(basDt, market, liquidityPassed);
        } else {
            // 기타: 유동성만 적용
            finalSymbols = liquidityPassed.stream()
                    .map(DailyStock::getSymbol)
                    .distinct()
                    .collect(Collectors.toList());
        }

        if (finalSymbols.isEmpty()) {
            log.debug("유니버스 필터: basDt={}, market={}, 최종 통과 종목 없음", basDt, market);
            return 0;
        }

        List<Universe> toSave = finalSymbols.stream()
                .map(symbol -> Universe.builder()
                        .basDt(basDt)
                        .market(market)
                        .symbol(symbol)
                        .build())
                .collect(Collectors.toList());
        universeRepository.saveAll(toSave);
        log.info("유니버스 필터 완료: basDt={}, market={}, count={}", basDt, market, toSave.size());
        return toSave.size();
    }

    /**
     * 한국 Sector Relative Strength 필터.
     * TB_SECTOR_RETURN·TB_SYMBOL_SECTOR에 데이터가 있으면 상위 N개 업종 내 종목만 반환.
     * 데이터가 없으면 유동성 통과 종목만 반환 (fallback).
     *
     * @param basDt 기준일
     * @param market 시장 (KR)
     * @param liquidityPassed 유동성 통과 종목 목록
     * @return Sector RS 통과 종목 코드 목록
     */
    private List<String> filterBySectorRelativeStrength(LocalDate basDt, String market, List<DailyStock> liquidityPassed) {
        List<String> liquiditySymbols = liquidityPassed.stream().map(DailyStock::getSymbol).distinct().collect(Collectors.toList());
        List<SectorReturn> sectorReturns = sectorReturnRepository.findByBasDtAndMarketOrderByReturnPctDesc(basDt, market);
        if (sectorReturns.isEmpty()) {
            log.debug("Sector Relative Strength 필터: 데이터 없음, 유동성 통과 종목만 반환. basDt={}, market={}, count={}",
                    basDt, market, liquiditySymbols.size());
            return liquiditySymbols;
        }
        int topN = Math.min(sectorRsTopN, sectorReturns.size());
        List<String> topSectorCodes = sectorReturns.stream()
                .limit(topN)
                .map(SectorReturn::getSectorCode)
                .collect(Collectors.toList());
        List<SymbolSector> symbolSectors = symbolSectorRepository.findByMarketAndSectorCodeIn(market, topSectorCodes);
        Set<String> symbolsInTopSectors = symbolSectors.stream().map(SymbolSector::getSymbol).collect(Collectors.toSet());
        List<String> filtered = liquiditySymbols.stream()
                .filter(symbolsInTopSectors::contains)
                .collect(Collectors.toList());
        if (filtered.isEmpty()) {
            log.debug("Sector RS 필터: 상위 업종 내 유동성 종목 없음, 유동성 통과 종목만 반환. basDt={}, market={}", basDt, market);
            return liquiditySymbols;
        }
        log.debug("Sector Relative Strength 필터 적용: basDt={}, market={}, topSectors={}, passed={}",
                basDt, market, topN, filtered.size());
        return filtered;
    }

    /**
     * 미국 Post-Earnings Drift 필터.
     * TB_EARNINGS_SURPRISE에 데이터가 있으면 최근 N일 이내 실적 발표 중 상위 N% 종목만 반환.
     * 데이터가 없으면 유동성 통과 종목만 반환 (fallback).
     *
     * @param basDt 기준일
     * @param market 시장 (US)
     * @param liquidityPassed 유동성 통과 종목 목록
     * @return Post-Earnings Drift 통과 종목 코드 목록
     */
    private List<String> filterByPostEarningsDrift(LocalDate basDt, String market, List<DailyStock> liquidityPassed) {
        List<String> liquiditySymbols = liquidityPassed.stream().map(DailyStock::getSymbol).distinct().collect(Collectors.toList());
        LocalDate fromDt = basDt.minusDays(earningsSurpriseLookbackDays);
        List<EarningsSurprise> surprises = earningsSurpriseRepository.findByMarketAndReportDtGreaterThanEqualOrderBySurpriseScoreDesc(market, fromDt);
        if (surprises.isEmpty()) {
            log.debug("Post-Earnings Drift 필터: 데이터 없음, 유동성 통과 종목만 반환. basDt={}, market={}, count={}",
                    basDt, market, liquiditySymbols.size());
            return liquiditySymbols;
        }
        Set<String> liquiditySet = liquiditySymbols.stream().collect(Collectors.toSet());
        List<String> orderedBySurprise = new ArrayList<>();
        for (EarningsSurprise e : surprises) {
            if (liquiditySet.contains(e.getSymbol()) && !orderedBySurprise.contains(e.getSymbol())) {
                orderedBySurprise.add(e.getSymbol());
            }
        }
        if (orderedBySurprise.isEmpty()) {
            return liquiditySymbols;
        }
        int topCount = Math.max(1, (int) Math.ceil(orderedBySurprise.size() * earningsSurpriseTopPct));
        List<String> filtered = orderedBySurprise.stream().limit(topCount).collect(Collectors.toList());
        log.debug("Post-Earnings Drift 필터 적용: basDt={}, market={}, topPct={}, passed={}",
                basDt, market, earningsSurpriseTopPct, filtered.size());
        return filtered;
    }

    /**
     * 기준일·시장의 유니버스 종목 코드 목록 조회.
     *
     * @param basDt   기준일
     * @param market 시장
     * @return 종목 코드 목록 (비어 있으면 전체 DailyStock 대상으로 할 수 있도록 호출부에서 fallback)
     */
    public List<String> getSymbols(LocalDate basDt, String market) {
        return universeRepository.findByBasDtAndMarketOrderBySymbol(basDt, market).stream()
                .map(Universe::getSymbol)
                .collect(Collectors.toList());
    }
}
