package com.investment.factor.zoo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Factor Zoo 서비스.
 * 다양한 팩터를 체계적으로 테스트하고 랭킹합니다.
 */
public interface FactorZooService {

    /**
     * 단일 팩터 테스트.
     *
     * @param factorCode 팩터 코드
     * @param market 시장 (KR, US)
     * @param startDate 테스트 시작일
     * @param endDate 테스트 종료일
     * @return 팩터 테스트 결과
     */
    FactorTestResult testFactor(String factorCode, String market, LocalDate startDate, LocalDate endDate);

    /**
     * 모든 팩터에 대한 성과 랭킹.
     *
     * @param market 시장 (KR, US)
     * @param startDate 테스트 시작일
     * @param endDate 테스트 종료일
     * @return IC 순으로 정렬된 팩터 테스트 결과 목록
     */
    List<FactorTestResult> rankFactors(String market, LocalDate startDate, LocalDate endDate);

    /**
     * 복합 팩터 점수 계산.
     *
     * @param symbol 종목 코드
     * @param market 시장
     * @param basDt 기준일
     * @param factorWeights 팩터별 가중치 (factorCode → weight)
     * @return 복합 팩터 점수
     */
    CombinedFactorScore getCombinedScore(String symbol, String market, LocalDate basDt,
                                         Map<String, BigDecimal> factorWeights);

    /**
     * 유니버스 내 종목들의 복합 팩터 점수 계산 및 랭킹.
     *
     * @param market 시장
     * @param basDt 기준일
     * @param factorWeights 팩터별 가중치
     * @param topN 상위 N개 종목
     * @return 복합 점수 순으로 정렬된 종목 목록
     */
    List<CombinedFactorScore> rankStocksByFactors(String market, LocalDate basDt,
                                                   Map<String, BigDecimal> factorWeights, int topN);

    /**
     * 팩터 정의 조회.
     *
     * @param factorCode 팩터 코드
     * @return 팩터 정의 (없으면 null)
     */
    FactorDefinition getFactorDefinition(String factorCode);

    /**
     * 모든 지원 팩터 목록 조회.
     *
     * @return 팩터 정의 목록
     */
    List<FactorDefinition> getAllFactorDefinitions();

    /**
     * 카테고리별 팩터 목록 조회.
     *
     * @param category 팩터 카테고리
     * @return 해당 카테고리의 팩터 정의 목록
     */
    List<FactorDefinition> getFactorsByCategory(FactorDefinition.FactorCategory category);

    /**
     * 팩터 코드 목록 조회.
     *
     * @return 지원하는 모든 팩터 코드
     */
    List<String> getSupportedFactorCodes();

    /**
     * 복합 팩터 점수 결과.
     */
    record CombinedFactorScore(
            String symbol,
            String market,
            LocalDate basDt,
            BigDecimal combinedScore,
            Map<String, BigDecimal> factorScores,
            Map<String, BigDecimal> factorWeights,
            int rank
    ) {}
}
