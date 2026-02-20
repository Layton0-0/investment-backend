package com.investment.risk.service;

import com.investment.risk.dto.StressScenario;
import com.investment.risk.dto.StressTestResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 스트레스 테스트 서비스.
 * 과거 위기 시나리오에 대한 포트폴리오 영향을 분석합니다.
 */
public interface StressTestService {

    /**
     * 단일 시나리오에 대한 스트레스 테스트 실행.
     *
     * @param portfolio 포트폴리오 (symbol → (value, assetClass))
     * @param scenarioCode 시나리오 코드 (FINANCIAL_CRISIS_2008, COVID_CRASH_2020 등)
     * @return 스트레스 테스트 결과
     */
    StressTestResult runStressTest(Map<String, PortfolioPosition> portfolio, String scenarioCode);

    /**
     * 단일 시나리오에 대한 스트레스 테스트 실행 (직접 시나리오 지정).
     *
     * @param portfolio 포트폴리오
     * @param scenario 시나리오 객체
     * @return 스트레스 테스트 결과
     */
    StressTestResult runStressTest(Map<String, PortfolioPosition> portfolio, StressScenario scenario);

    /**
     * 모든 기본 시나리오에 대한 배치 스트레스 테스트 실행.
     *
     * @param portfolio 포트폴리오
     * @return 각 시나리오별 결과 목록
     */
    List<StressTestResult> runAllStressTests(Map<String, PortfolioPosition> portfolio);

    /**
     * 사용자 정의 시나리오 생성.
     *
     * @param code 시나리오 코드
     * @param name 시나리오 이름
     * @param description 설명
     * @param assetClassShocks 자산군별 충격률
     * @return 생성된 시나리오
     */
    StressScenario createCustomScenario(
            String code,
            String name,
            String description,
            Map<String, BigDecimal> assetClassShocks);

    /**
     * 지원하는 기본 시나리오 목록 반환.
     *
     * @return 시나리오 코드 목록
     */
    List<String> getSupportedScenarioCodes();

    /**
     * 시나리오 상세 정보 반환.
     *
     * @param scenarioCode 시나리오 코드
     * @return 시나리오 정보 (없으면 null)
     */
    StressScenario getScenario(String scenarioCode);

    /**
     * 포트폴리오 포지션 정보.
     */
    record PortfolioPosition(
            String symbol,
            String market,
            String assetClass,
            BigDecimal value,
            BigDecimal quantity,
            BigDecimal currentPrice
    ) {
        public PortfolioPosition(String symbol, String market, BigDecimal value) {
            this(symbol, market, "EQUITY", value, null, null);
        }

        public PortfolioPosition(String symbol, String market, String assetClass, BigDecimal value) {
            this(symbol, market, assetClass, value, null, null);
        }
    }
}
