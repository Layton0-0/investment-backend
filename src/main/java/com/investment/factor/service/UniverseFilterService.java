package com.investment.factor.service;

import com.investment.domain.entity.DailyStock;
import com.investment.domain.entity.Universe;
import com.investment.domain.repository.DailyStockRepository;
import com.investment.domain.repository.UniverseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
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

    /** 유동성 최소 거래대금 (원). 공통 Liquidity Cut-off */
    @Value("${investment.factor.liquidity-min-trd-val:1000000000}")
    private long liquidityMinTrdVal = 1_000_000_000L;

    /**
     * 기준일·시장에 대해 유니버스 필터 실행.
     * 기존 유니버스 삭제 후 유동성 통과 종목만 저장.
     *
     * @param basDt   기준일
     * @param market 시장 (KR, US)
     * @return 저장된 유니버스 종목 수
     */
    @Transactional
    public int run(LocalDate basDt, String market) {
        universeRepository.deleteByBasDtAndMarket(basDt, market);
        List<DailyStock> passed = dailyStockRepository.findByBasDtAndMarketAndTrdValGreaterThanEqual(
                basDt, market, liquidityMinTrdVal);
        if (passed.isEmpty()) {
            log.debug("유니버스 필터: basDt={}, market={}, 통과 종목 없음", basDt, market);
            return 0;
        }
        List<Universe> toSave = passed.stream()
                .map(d -> Universe.builder()
                        .basDt(d.getBasDt())
                        .market(d.getMarket())
                        .symbol(d.getSymbol())
                        .build())
                .collect(Collectors.toList());
        universeRepository.saveAll(toSave);
        log.info("유니버스 필터 완료: basDt={}, market={}, count={}", basDt, market, toSave.size());
        return toSave.size();
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
