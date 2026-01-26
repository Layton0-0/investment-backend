package com.investment.strategy.service;

import com.investment.analysis.dto.AnalysisResponseDto;
import com.investment.analysis.service.AnalysisService;
import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.domain.entity.TradingSetting;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.order.dto.OrderRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 거래 전략 서비스
 * AI 분석 결과를 바탕으로 자동 매매 결정을 수행합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradingStrategyService {
    
    private final AnalysisService analysisService;
    private final TradingSettingRepository tradingSettingRepository;
    
    /**
     * 자동 매매 결정
     * 
     * @param accountNo 계좌번호
     * @param symbol 종목코드
     * @return 주문 요청 DTO (매수/매도 결정 시), null (보유 결정 시)
     */
    @Transactional(readOnly = true)
    public OrderRequestDto decideTradingAction(String accountNo, String symbol) {
        log.info("자동 매매 결정 요청: accountNo={}, symbol={}", accountNo, symbol);
        
        // 거래 설정 조회
        TradingSetting setting = tradingSettingRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> new DomainException(
                        ErrorCode.SETTING_NOT_FOUND, 
                        "거래 설정을 찾을 수 없습니다: " + accountNo));
        
        if (!setting.getAutoTradingEnabled()) {
            log.debug("자동 매매가 비활성화되어 있습니다: accountNo={}", accountNo);
            return null;
        }
        
        // AI 분석 수행
        AnalysisResponseDto analysis = analysisService.analyze(
                com.investment.analysis.dto.AnalysisRequestDto.builder()
                        .symbol(symbol)
                        .periodDays(30)
                        .build());
        
        // 전략에 따른 매매 결정
        if ("BUY".equals(analysis.getRecommendation()) && 
            analysis.getConfidence().compareTo(new BigDecimal("0.7")) >= 0) {
            
            // 매수 결정
            BigDecimal investmentAmount = calculateInvestmentAmount(setting, analysis);
            if (investmentAmount.compareTo(setting.getMinInvestmentAmount()) < 0) {
                log.debug("최소 투자금액 미달: amount={}, min={}", 
                        investmentAmount, setting.getMinInvestmentAmount());
                return null;
            }
            
            // TODO: 현재가 조회 필요
            BigDecimal currentPrice = analysis.getCurrentPrice();
            Integer quantity = investmentAmount.divide(currentPrice, 0, BigDecimal.ROUND_DOWN).intValue();
            
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
                   analysis.getConfidence().compareTo(new BigDecimal("0.7")) >= 0) {
            
            // 매도 결정
            // TODO: 보유 수량 조회 필요
            // 임시로 null 반환
            log.debug("매도 신호이지만 보유 수량 확인 필요: symbol={}", symbol);
        }
        
        return null;
    }
    
    /**
     * 투자 금액 계산
     */
    private BigDecimal calculateInvestmentAmount(TradingSetting setting, AnalysisResponseDto analysis) {
        BigDecimal baseAmount = setting.getMaxInvestmentAmount();
        
        // 신뢰도에 따른 조정
        BigDecimal confidenceMultiplier = analysis.getConfidence();
        BigDecimal adjustedAmount = baseAmount.multiply(confidenceMultiplier);
        
        // 리스크 레벨에 따른 조정
        if (setting.getRiskLevel() != null) {
            adjustedAmount = adjustedAmount.multiply(setting.getRiskLevel());
        }
        
        // 최대 투자금액 초과 방지
        if (adjustedAmount.compareTo(setting.getMaxInvestmentAmount()) > 0) {
            adjustedAmount = setting.getMaxInvestmentAmount();
        }
        
        return adjustedAmount;
    }
    
    /**
     * 여러 종목에 대한 자동 매매 결정
     */
    public List<OrderRequestDto> decideTradingActionsForSymbols(String accountNo, List<String> symbols) {
        log.info("다중 종목 자동 매매 결정 요청: accountNo={}, symbols={}", accountNo, symbols);
        
        return symbols.stream()
                .map(symbol -> decideTradingAction(accountNo, symbol))
                .filter(order -> order != null)
                .collect(java.util.stream.Collectors.toList());
    }
}
