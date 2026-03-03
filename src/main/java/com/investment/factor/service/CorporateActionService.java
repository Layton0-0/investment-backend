package com.investment.factor.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 기업 이벤트(액면분할·배당락·티커 변경 등)로 인한 가격 왜곡을 파이프라인 진입 전에 차단.
 * 유니버스/팩터 단계에서 해당 종목을 제외할 때 사용.
 *
 * @see UniverseFilterService
 */
@Service
public class CorporateActionService {

    /**
     * 기준일·시장에서 파이프라인에서 제외할 종목 코드 집합.
     * (당일/익일 액면분할·배당락 등 이벤트 종목. 데이터 소스 연동 전에는 빈 집합.)
     *
     * @param basDt  기준일
     * @param market 시장 (KR, US)
     * @return 제외할 종목 코드 집합 (비어 있으면 제외 없음)
     */
    public Set<String> getSymbolsToExclude(LocalDate basDt, String market) {
        if (basDt == null || market == null || market.isBlank()) {
            return Collections.emptySet();
        }
        // TODO: 외부 API(Polygon, Alpaca 등) 또는 TB_CORPORATE_ACTION 연동
        return Collections.emptySet();
    }

    /**
     * 유니버스 종목 목록에서 기업 이벤트 제외 대상 제거.
     */
    public List<String> filterExcluded(List<String> symbols, LocalDate basDt, String market) {
        if (symbols == null || symbols.isEmpty()) {
            return symbols;
        }
        Set<String> exclude = getSymbolsToExclude(basDt, market);
        if (exclude.isEmpty()) {
            return symbols;
        }
        return symbols.stream()
                .filter(s -> !exclude.contains(s))
                .collect(Collectors.toList());
    }
}
