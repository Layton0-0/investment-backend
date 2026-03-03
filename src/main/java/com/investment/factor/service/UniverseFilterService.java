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
import com.investment.marketdata.client.KoreaInvestmentRankClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final CorporateActionService corporateActionService;

    /** 순위 API(거래량 순위) 연동. provider=korea-investment일 때만 빈 존재. 미존재 시 volume rank 필터 스킵 */
    @Autowired(required = false)
    private KoreaInvestmentRankClient rankClient;

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

    /** 한국(KR) 저평가 P/B 구간 (바닥). 데이터 소스 확정 후 유니버스/시그널 필터 적용. 현재 스텁 */
    @Value("${investment.factor.pb-value-min:0.8}")
    private double pbValueMin = 0.8;

    @Value("${investment.factor.pb-value-max:0.9}")
    private double pbValueMax = 0.9;

    /** KR 유니버스에 거래량 순위(순위분석 API) 교집합 적용 여부. true 시 volume-rank-user-id 필요 */
    @Value("${investment.factor.volume-rank-enabled:false}")
    private boolean volumeRankEnabled = false;

    @Value("${investment.factor.volume-rank-user-id:}")
    private String volumeRankUserId = "";

    /** 거래량 순위 상위 N건만 유니버스와 교집합 (volume-rank-enabled 시) */
    @Value("${investment.factor.volume-rank-limit:200}")
    private int volumeRankLimit = 200;

    /**
     * 기준일·시장에 대해 유니버스 필터 실행.
     * 유동성 필터 + (한국) Sector Relative Strength 필터 적용.
     *
     * @param basDt  기준일
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
            // 한국: 선택적 거래량 순위(순위분석 API) 교집합
            List<DailyStock> afterVolumeRank = filterByVolumeRankIfEnabled(liquidityPassed);
            // Sector Relative Strength 필터
            List<String> sectorPassed = filterBySectorRelativeStrength(basDt, market, afterVolumeRank);
            // P/B 저평가 필터 (0.8~0.9): 데이터 소스 확정 후 적용. 현재 스텁(통과)
            finalSymbols = filterByPbValue(basDt, market, sectorPassed);
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

        // 기업 이벤트(액면분할·배당락 등) 제외
        finalSymbols = corporateActionService.filterExcluded(finalSymbols, basDt, market);

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
     * KR 유니버스에 거래량 순위(순위분석 API) 교집합 적용.
     * volume-rank-enabled=true, volume-rank-user-id 설정, rankClient 존재 시에만 적용.
     * API 미설정·실패 시 유동성 통과 종목 그대로 반환.
     */
    private List<DailyStock> filterByVolumeRankIfEnabled(List<DailyStock> liquidityPassed) {
        if (!volumeRankEnabled || volumeRankUserId == null || volumeRankUserId.isBlank() || rankClient == null) {
            return liquidityPassed;
        }
        try {
            List<String> rankSymbols = rankClient.getVolumeRank(volumeRankUserId, "1", "J", volumeRankLimit)
                    .stream()
                    .map(com.investment.marketdata.dto.VolumeRankItemDto::getSymbol)
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.toList());
            if (rankSymbols.isEmpty()) {
                return liquidityPassed;
            }
            Set<String> rankSet = rankSymbols.stream().collect(Collectors.toSet());
            List<DailyStock> filtered = liquidityPassed.stream()
                    .filter(d -> rankSet.contains(d.getSymbol()))
                    .collect(Collectors.toList());
            if (filtered.isEmpty()) {
                log.debug("거래량 순위 교집합: 유니버스와 겹치는 종목 없음, 유동성 통과 종목만 사용. basDt=KR");
                return liquidityPassed;
            }
            log.debug("거래량 순위 교집합 적용: 유동성 {} -> {} 종목", liquidityPassed.size(), filtered.size());
            return filtered;
        } catch (Exception e) {
            log.warn("거래량 순위 API 호출 실패, 유동성 통과 종목만 사용: {}", e.getMessage());
            return liquidityPassed;
        }
    }

    /**
     * 한국 Sector Relative Strength 필터.
     * TB_SECTOR_RETURN·TB_SYMBOL_SECTOR에 데이터가 있으면 상위 N개 업종 내 종목만 반환.
     * 데이터가 없으면 유동성 통과 종목만 반환 (fallback).
     *
     * @param basDt           기준일
     * @param market          시장 (KR)
     * @param liquidityPassed 유동성 통과 종목 목록
     * @return Sector RS 통과 종목 코드 목록
     */
    private List<String> filterBySectorRelativeStrength(LocalDate basDt, String market,
            List<DailyStock> liquidityPassed) {
        List<String> liquiditySymbols = liquidityPassed.stream().map(DailyStock::getSymbol).distinct()
                .collect(Collectors.toList());
        List<SectorReturn> sectorReturns = sectorReturnRepository.findByBasDtAndMarketOrderByReturnPctDesc(basDt,
                market);
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
        Set<String> symbolsInTopSectors = symbolSectors.stream().map(SymbolSector::getSymbol)
                .collect(Collectors.toSet());
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
     * @param basDt           기준일
     * @param market          시장 (US)
     * @param liquidityPassed 유동성 통과 종목 목록
     * @return Post-Earnings Drift 통과 종목 코드 목록
     */
    private List<String> filterByPostEarningsDrift(LocalDate basDt, String market, List<DailyStock> liquidityPassed) {
        List<String> liquiditySymbols = liquidityPassed.stream().map(DailyStock::getSymbol).distinct()
                .collect(Collectors.toList());
        LocalDate fromDt = basDt.minusDays(earningsSurpriseLookbackDays);
        List<EarningsSurprise> surprises = earningsSurpriseRepository
                .findByMarketAndReportDtGreaterThanEqualOrderBySurpriseScoreDesc(market, fromDt);
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
     * 한국(KR) 저평가 P/B 필터. P/B가 pbValueMin~pbValueMax(0.8~0.9) 구간인 종목만 통과.
     * TODO: KOSPI/종목 P/B 데이터 소스 확정 후 TB 또는 외부 API 연동. 현재는 데이터 없음 시 입력 그대로 반환(스텁).
     *
     * @param basDt   기준일
     * @param market  시장 (KR)
     * @param symbols Sector RS 통과 종목 목록
     * @return P/B 구간 통과 종목 (데이터 없으면 입력 그대로)
     */
    private List<String> filterByPbValue(LocalDate basDt, String market, List<String> symbols) {
        // P/B 데이터 소스 미연동 시 필터 없이 통과
        log.debug("P/B 필터: 데이터 소스 미연동(스텁), basDt={}, market={}, count={}", basDt, market, symbols.size());
        return symbols;
    }

    /**
     * 기준일·시장의 유니버스 종목 코드 목록 조회.
     *
     * @param basDt  기준일
     * @param market 시장
     * @return 종목 코드 목록 (비어 있으면 전체 DailyStock 대상으로 할 수 있도록 호출부에서 fallback)
     */
    public List<String> getSymbols(LocalDate basDt, String market) {
        return universeRepository.findByBasDtAndMarketOrderBySymbol(basDt, market).stream()
                .map(Universe::getSymbol)
                .collect(Collectors.toList());
    }
}
