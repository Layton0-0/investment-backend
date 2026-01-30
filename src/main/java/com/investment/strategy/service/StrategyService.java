package com.investment.strategy.service;

import com.investment.analysis.dto.AnalysisResponseDto;
import com.investment.analysis.service.AnalysisService;
import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.common.security.LogMaskingUtil;
import com.investment.domain.entity.Strategy;
import com.investment.domain.repository.StrategyRepository;
import com.investment.order.dto.OrderRequestDto;
import com.investment.strategy.domain.StrategyType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 전략 서비스
 * 단기/중기/장기별 전략을 적용합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyService {

    private final AnalysisService analysisService;
    private final StrategyRepository strategyRepository;

    private static final String DEFAULT_MARKET = "KR";

    /**
     * 전략에 따른 자동 매매 결정 (시장 기본 KR)
     */
    @Transactional(readOnly = true)
    public OrderRequestDto decideTradingAction(String accountNo, String symbol, StrategyType strategyType) {
        return decideTradingAction(accountNo, symbol, DEFAULT_MARKET, strategyType);
    }

    /**
     * 전략에 따른 자동 매매 결정
     *
     * @param accountNo    계좌번호
     * @param symbol       종목코드
     * @param market       시장 (KR, US). null이면 KR
     * @param strategyType 전략 타입
     * @return 주문 요청 DTO (매수/매도 결정 시), null (보유 결정 시)
     */
    @Transactional(readOnly = true)
    public OrderRequestDto decideTradingAction(String accountNo, String symbol, String market,
            StrategyType strategyType) {
        String m = (market != null && !market.isBlank()) ? market : DEFAULT_MARKET;
        log.info("자동 매매 결정 요청: accountNo={}, symbol={}, market={}, strategyType={}",
                LogMaskingUtil.maskAccountNo(accountNo), symbol, m, strategyType);

        Strategy strategy = strategyRepository.findByAccountNoAndMarketAndStrategyType(accountNo, m, strategyType)
                .orElseThrow(() -> new DomainException(
                        ErrorCode.SETTING_NOT_FOUND,
                        String.format("전략을 찾을 수 없습니다: accountNo=%s, market=%s, strategyType=%s", accountNo, m,
                                strategyType)));

        // 중지된 전략은 실행하지 않음
        if (strategy.isStopped()) {
            log.debug("전략이 중지되어 있습니다: accountNo={}, strategyType={}", LogMaskingUtil.maskAccountNo(accountNo),
                    strategyType);
            return null;
        }

        // 활성 상태가 아니면 실행하지 않음
        if (!strategy.isActive()) {
            log.debug("전략이 활성 상태가 아닙니다: accountNo={}, strategyType={}, status={}",
                    LogMaskingUtil.maskAccountNo(accountNo), strategyType, strategy.getStatus());
            return null;
        }

        // 전략 타입에 따른 분석 기간 설정
        int periodDays = getAnalysisPeriod(strategyType);

        // AI 분석 수행
        AnalysisResponseDto analysis = analysisService.analyze(
                com.investment.analysis.dto.AnalysisRequestDto.builder()
                        .symbol(symbol)
                        .periodDays(periodDays)
                        .build());

        // 전략에 따른 매매 결정
        BigDecimal confidenceThreshold = strategy.getConfidenceThreshold() != null ? strategy.getConfidenceThreshold()
                : new BigDecimal("0.7");

        if ("BUY".equals(analysis.getRecommendation()) &&
                analysis.getConfidence().compareTo(confidenceThreshold) >= 0) {

            // 매수 결정
            BigDecimal investmentAmount = calculateInvestmentAmount(strategy, analysis);
            if (investmentAmount.compareTo(strategy.getMinInvestmentAmount()) < 0) {
                log.debug("최소 투자금액 미달: amount={}, min={}",
                        investmentAmount, strategy.getMinInvestmentAmount());
                return null;
            }

            // TODO: 현재가 조회 필요
            BigDecimal currentPrice = analysis.getCurrentPrice();
            if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("현재가가 유효하지 않습니다: symbol={}, currentPrice={}", symbol, currentPrice);
                return null;
            }
            Integer quantity = investmentAmount.divide(currentPrice, 0, RoundingMode.DOWN).intValue();

            if (quantity > 0) {
                return OrderRequestDto.builder()
                        .accountNo(accountNo)
                        .symbol(symbol)
                        .orderType(OrderRequestDto.OrderType.BUY)
                        .quantity(quantity)
                        .price(currentPrice)
                        .build();
            }

        } else if ("SELL".equals(analysis.getRecommendation()) &&
                analysis.getConfidence().compareTo(confidenceThreshold) >= 0) {

            // 매도 결정
            // TODO: 보유 수량 조회 필요
            log.debug("매도 신호이지만 보유 수량 확인 필요: symbol={}", symbol);
        }

        return null;
    }

    /**
     * 전략 타입에 따른 분석 기간 반환
     */
    private int getAnalysisPeriod(StrategyType strategyType) {
        switch (strategyType) {
            case SHORT_TERM:
                return 7; // 단기: 7일
            case MEDIUM_TERM:
                return 30; // 중기: 30일
            case LONG_TERM:
                return 90; // 장기: 90일
            default:
                return 30;
        }
    }

    /**
     * 투자 금액 계산
     */
    private BigDecimal calculateInvestmentAmount(Strategy strategy, AnalysisResponseDto analysis) {
        BigDecimal baseAmount = strategy.getMaxInvestmentAmount();

        // 신뢰도에 따른 조정
        BigDecimal confidenceMultiplier = analysis.getConfidence();
        BigDecimal adjustedAmount = baseAmount.multiply(confidenceMultiplier);

        // 리스크 레벨에 따른 조정
        if (strategy.getRiskLevel() != null) {
            adjustedAmount = adjustedAmount.multiply(strategy.getRiskLevel());
        }

        // 최대 투자금액 초과 방지
        if (adjustedAmount.compareTo(strategy.getMaxInvestmentAmount()) > 0) {
            adjustedAmount = strategy.getMaxInvestmentAmount();
        }

        return adjustedAmount;
    }

    /**
     * 여러 종목에 대한 자동 매매 결정 (시장 기본 KR)
     */
    public List<OrderRequestDto> decideTradingActionsForSymbols(String accountNo, List<String> symbols,
            StrategyType strategyType) {
        return decideTradingActionsForSymbols(accountNo, symbols, DEFAULT_MARKET, strategyType);
    }

    /**
     * 여러 종목에 대한 자동 매매 결정
     */
    public List<OrderRequestDto> decideTradingActionsForSymbols(String accountNo, List<String> symbols, String market,
            StrategyType strategyType) {
        log.info("다중 종목 자동 매매 결정 요청: accountNo={}, symbols={}, market={}, strategyType={}",
                LogMaskingUtil.maskAccountNo(accountNo), symbols, market, strategyType);

        return symbols.stream()
                .map(symbol -> decideTradingAction(accountNo, symbol, market, strategyType))
                .filter(order -> order != null)
                .collect(Collectors.toList());
    }
}
