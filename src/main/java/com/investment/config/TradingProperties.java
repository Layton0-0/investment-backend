package com.investment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 거래 설정 Properties
 */
@Component
@ConfigurationProperties(prefix = "investment.trading")
@Getter
@Setter
public class TradingProperties {
    
    private BigDecimal maxInvestmentAmount = new BigDecimal("1000000");
    private BigDecimal minInvestmentAmount = new BigDecimal("10000");
    private String defaultCurrency = "USD";
}
