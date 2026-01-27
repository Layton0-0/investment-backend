package com.investment.account.service;

import com.investment.account.dto.AccountBalanceDto;
import com.investment.account.dto.AccountPositionDto;
import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.config.CacheConfig;
import com.investment.domain.entity.Portfolio;
import com.investment.domain.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 계좌 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {
    
    private final PortfolioRepository portfolioRepository;
    
    /**
     * 계좌 잔고 조회
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_ACCOUNT, key = "'balance_' + #accountNo")
    public AccountBalanceDto getAccountBalance(String accountNo) {
        log.debug("계좌 잔고 조회: accountNo={}", accountNo);
        
        try {
            // 포트폴리오 총 가치 계산
            BigDecimal portfolioValue = portfolioRepository.getTotalPortfolioValue(accountNo);
            if (portfolioValue == null) {
                portfolioValue = BigDecimal.ZERO;
            }
            
            // DB에서 직접 조회하는 방식으로 변경 (키움 API 제거)
            return AccountBalanceDto.builder()
                    .accountNo(accountNo)
                    .totalBalance(portfolioValue)
                    .availableBalance(portfolioValue)
                    .investedAmount(portfolioValue)
                    .currency("USD")
                    .build();
                    
        } catch (Exception e) {
            log.error("계좌 잔고 조회 실패: accountNo={}", accountNo, e);
            throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND, 
                    "계좌 잔고 조회에 실패했습니다: " + e.getMessage(), e);
        }
    }
    
    /**
     * 보유 종목 조회
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_ACCOUNT, key = "'positions_' + #accountNo")
    public List<AccountPositionDto> getPositions(String accountNo) {
        log.debug("보유 종목 조회: accountNo={}", accountNo);
        
        try {
            // DB의 포트폴리오 정보 조회
            List<Portfolio> portfolios = portfolioRepository.findByAccountNo(accountNo);
            
            return portfolios.stream()
                    .map(portfolio -> {
                        BigDecimal currentPrice = portfolio.getCurrentPrice();
                        if (currentPrice == null) {
                            currentPrice = portfolio.getAveragePrice();
                        }
                        
                        BigDecimal totalValue = currentPrice.multiply(BigDecimal.valueOf(portfolio.getQuantity()));
                        BigDecimal profitLoss = totalValue.subtract(
                                portfolio.getAveragePrice().multiply(BigDecimal.valueOf(portfolio.getQuantity())));
                        BigDecimal profitLossRate = portfolio.getAveragePrice().compareTo(BigDecimal.ZERO) > 0 ?
                                profitLoss.divide(portfolio.getAveragePrice().multiply(BigDecimal.valueOf(portfolio.getQuantity())), 
                                        4, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100)) :
                                BigDecimal.ZERO;
                        
                        return AccountPositionDto.builder()
                                .symbol(portfolio.getSymbol())
                                .name(portfolio.getName())
                                .quantity(portfolio.getQuantity())
                                .averagePrice(portfolio.getAveragePrice())
                                .currentPrice(currentPrice)
                                .totalValue(totalValue)
                                .profitLoss(profitLoss)
                                .profitLossRate(profitLossRate)
                                .currency(portfolio.getCurrency())
                                .lastUpdated(portfolio.getLastUpdated())
                                .build();
                    })
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("보유 종목 조회 실패: accountNo={}", accountNo, e);
            throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND, 
                    "보유 종목 조회에 실패했습니다: " + e.getMessage(), e);
        }
    }
}
