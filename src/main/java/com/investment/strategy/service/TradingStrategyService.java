package com.investment.strategy.service;

import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.common.security.LogMaskingUtil;
import com.investment.domain.entity.TradingSetting;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.order.dto.OrderRequestDto;
import com.investment.strategy.engine.MacroEconomicStrategyEngine;
import com.investment.strategy.engine.QuantitativeRuleEngine;
import com.investment.taapi.dto.StockAnalysisDto;
import com.investment.taapi.service.StockAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 자동 매매 전략 서비스
 * 
 * <p>
 * 정량적 규칙 엔진과 거시경제 전략 엔진을 결합하여 자동 매매 결정을 수행합니다.
 * 기술적 분석 결과와 거시경제 지표를 종합하여 매수/매도/보유를 결정하며,
 * 결정된 경우 주문 요청 DTO를 생성합니다.
 * </p>
 * 
 * <p>
 * AI 예측 서비스와 연동하여 더 정확한 매매 결정을 내릴 수 있도록 설계되었습니다.
 * </p>
 * 
 * @author Investment System
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradingStrategyService {

    private final StockAnalysisService stockAnalysisService;
    private final TradingSettingRepository tradingSettingRepository;
    private final QuantitativeRuleEngine quantitativeRuleEngine;
    private final MacroEconomicStrategyEngine macroEconomicStrategyEngine;

    /**
     * 자동 매매 결정
     * 
     * <p>
     * 기술적 분석과 거시경제 지표를 종합하여 자동 매매 결정을 수행합니다.
     * 거래 설정에서 자동 매매가 비활성화된 경우 null을 반환합니다.
     * </p>
     * 
     * <p>
     * 결정 프로세스:
     * <ol>
     * <li>거래 설정 조회 및 자동 매매 활성화 여부 확인</li>
     * <li>기술적 분석 수행</li>
     * <li>거시경제 지표 조회 및 전략 결정</li>
     * <li>정량적 규칙 엔진을 통한 최종 매매 결정</li>
     * <li>매수/매도 결정 시 주문 요청 DTO 생성</li>
     * </ol>
     * </p>
     * 
     * @param accountNo 계좌번호
     * @param symbol    종목코드
     * @return 주문 요청 DTO (매수/매도 결정 시), null (보유 결정 시 또는 자동 매매 비활성화 시)
     * @throws DomainException 거래 설정을 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public OrderRequestDto decideTradingAction(String accountNo, String symbol) {
        log.info("자동 매매 결정 요청: accountNo={}, symbol={}", LogMaskingUtil.maskAccountNo(accountNo), symbol);

        // 1. 거래 설정 조회
        TradingSetting setting = tradingSettingRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> new DomainException(
                        ErrorCode.SETTING_NOT_FOUND,
                        "거래 설정을 찾을 수 없습니다: " + accountNo));

        if (!setting.getAutoTradingEnabled()) {
            log.debug("자동 매매가 비활성화되어 있습니다: accountNo={}", accountNo);
            return null;
        }

        // 2. 기술적 분석 수행
        // 주의: @Transactional 내에서 block() 사용은 데드락 위험이 있으나,
        // 이 메서드는 동기적으로 결과를 반환해야 하므로 불가피함
        // 향후 비동기 처리로 개선 필요
        StockAnalysisDto technicalAnalysis;
        try {
            technicalAnalysis = stockAnalysisService.analyzeStock(symbol, "1d")
                    .onErrorResume(error -> {
                        log.warn("기술적 분석 실패: symbol={}, error={}", symbol, error.getMessage());
                        return Mono.empty();
                    })
                    .block();
        } catch (Exception e) {
            log.error("기술적 분석 중 오류 발생: symbol={}", symbol, e);
            return null;
        }

        if (technicalAnalysis == null) {
            log.warn("기술적 분석 결과가 없습니다: symbol={}", symbol);
            return null;
        }

        // 3. 거시경제 지표 조회 및 전략 결정
        // TODO: 실제 거시경제 데이터 API 연동 필요 (현재는 더미 데이터 사용)
        MacroEconomicStrategyEngine.MacroEconomicIndicators indicators = MacroEconomicStrategyEngine.MacroEconomicIndicators
                .builder()
                .vix(new BigDecimal("20")) // 더미 데이터
                .interestRate(new BigDecimal("3.5")) // 더미 데이터
                .inflationRate(new BigDecimal("2.0")) // 더미 데이터
                .build();

        MacroEconomicStrategyEngine.InvestmentStrategy macroStrategy = macroEconomicStrategyEngine
                .decideStrategy(indicators);

        // 4. 정량적 규칙 엔진을 통한 최종 매매 결정
        QuantitativeRuleEngine.TradingDecision decision = quantitativeRuleEngine.decide(technicalAnalysis,
                macroStrategy);

        log.info("정량적 규칙 엔진 결정: symbol={}, decision={}, score={}",
                symbol, decision.getDecisionType(), decision.getScore().getTotalScore());

        // 5. 결정에 따른 주문 요청 DTO 생성
        if (decision.getDecisionType() == QuantitativeRuleEngine.TradingDecision.DecisionType.BUY) {
            return createBuyOrder(accountNo, symbol, setting, decision, technicalAnalysis);
        } else if (decision.getDecisionType() == QuantitativeRuleEngine.TradingDecision.DecisionType.SELL) {
            return createSellOrder(accountNo, symbol, setting, decision, technicalAnalysis);
        }

        // HOLD인 경우 null 반환
        return null;
    }

    /**
     * 매수 주문 생성
     * 
     * <p>
     * 매수 결정에 따라 주문 요청 DTO를 생성합니다.
     * 거래 설정의 최대 투자금액과 포지션 크기 비율을 기반으로 매수 수량을 계산합니다.
     * </p>
     * 
     * @param accountNo 계좌번호
     * @param symbol    종목코드
     * @param setting   거래 설정
     * @param decision  매매 결정
     * @param analysis  기술적 분석 결과
     * @return 매수 주문 요청 DTO (투자금액이 최소 투자금액 미만인 경우 null)
     */
    private OrderRequestDto createBuyOrder(String accountNo,
            String symbol,
            TradingSetting setting,
            QuantitativeRuleEngine.TradingDecision decision,
            StockAnalysisDto analysis) {

        BigDecimal currentPrice = analysis.getCurrentPrice();
        if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("현재가가 유효하지 않습니다: symbol={}, currentPrice={}", symbol, currentPrice);
            return null;
        }

        // 매수 결정: 포지션 크기 비율에 따른 투자금액 계산
        BigDecimal positionSizePercent = decision.getPositionSizePercent();
        BigDecimal investmentAmount = setting.getMaxInvestmentAmount()
                .multiply(positionSizePercent)
                .divide(new BigDecimal("100"), 2, RoundingMode.DOWN);

        // 최소 투자금액 검증
        if (investmentAmount.compareTo(setting.getMinInvestmentAmount()) < 0) {
            log.debug("최소 투자금액 미만: amount={}, min={}",
                    investmentAmount, setting.getMinInvestmentAmount());
            return null;
        }

        // 매수 수량 계산 (소수점 이하 버림)
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

        return null;
    }

    /**
     * 매도 주문 생성
     * 
     * <p>
     * 매도 결정에 따라 주문 요청 DTO를 생성합니다.
     * 현재는 보유 종목 정보가 없어 구현이 보류되어 있습니다.
     * </p>
     * 
     * <p>
     * TODO: 보유 종목 정보를 조회하여 매도 수량을 결정하는 로직 구현 필요
     * </p>
     * 
     * @param accountNo 계좌번호
     * @param symbol    종목코드
     * @param setting   거래 설정
     * @param decision  매매 결정
     * @param analysis  기술적 분석 결과
     * @return 매도 주문 요청 DTO (현재는 null 반환)
     */
    private OrderRequestDto createSellOrder(String accountNo,
            String symbol,
            TradingSetting setting,
            QuantitativeRuleEngine.TradingDecision decision,
            StockAnalysisDto analysis) {

        // TODO: 보유 종목 정보 조회 필요
        // 현재 보유 종목 정보가 없어 매도 주문을 생성할 수 없음
        log.debug("보유 종목 정보가 없어 매도 주문을 생성할 수 없습니다: symbol={}", symbol);
        return null;
    }

    /**
     * 여러 종목에 대한 자동 매매 결정
     * 
     * <p>
     * 여러 종목에 대해 순차적으로 자동 매매 결정을 수행합니다.
     * 각 종목에 대해 주문 요청이 생성된 경우에만 결과에 포함됩니다.
     * </p>
     * 
     * @param accountNo 계좌번호
     * @param symbols   종목코드 목록
     * @return 주문 요청 DTO 목록 (주문이 생성된 종목만 포함)
     */
    public List<OrderRequestDto> decideTradingActionsForSymbols(String accountNo, List<String> symbols) {
        log.info("다중 종목 자동 매매 결정 요청: accountNo={}, symbols={}", LogMaskingUtil.maskAccountNo(accountNo), symbols);

        return symbols.stream()
                .map(symbol -> decideTradingAction(accountNo, symbol))
                .filter(order -> order != null)
                .collect(Collectors.toList());
    }
}
