package com.investment.core.engine.execution;

import com.investment.config.FrictionCostProperties;
import com.investment.core.engine.execution.dto.TcaEstimateRequest;
import com.investment.core.engine.execution.dto.TcaReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * TCA (Transaction Cost Analysis) 구현체.
 * FrictionCostProperties를 재사용하여 KR/US 시장별 비용을 계산합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionCostAnalyzerImpl implements TransactionCostAnalyzer {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal DEFAULT_SPREAD_KR = new BigDecimal("0.001");
    private static final BigDecimal DEFAULT_SPREAD_US = new BigDecimal("0.0005");
    private static final BigDecimal DEFAULT_VOLATILITY = new BigDecimal("0.02");
    private static final BigDecimal MARKET_IMPACT_COEFFICIENT = new BigDecimal("0.1");

    private final FrictionCostProperties frictionCostProperties;

    @Override
    public TcaReport estimateCost(TcaEstimateRequest request) {
        if (request == null || request.getSymbol() == null) {
            return TcaReport.error("Invalid request: symbol is required");
        }
        if (request.getArrivalPrice() == null || request.getArrivalPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return TcaReport.error("Invalid arrival price");
        }
        if (request.getQuantity() <= 0) {
            return TcaReport.error("Invalid quantity");
        }

        try {
            String market = request.getMarket() != null ? request.getMarket().toUpperCase() : "KR";
            String assetType = request.getAssetType() != null ? request.getAssetType().toUpperCase() : "STOCK";
            String side = request.getSide() != null ? request.getSide().toUpperCase() : "BUY";
            BigDecimal notional = request.getArrivalPrice().multiply(new BigDecimal(request.getQuantity()));

            BigDecimal commissionCost;
            BigDecimal commissionPct;
            BigDecimal taxCost = BigDecimal.ZERO;
            BigDecimal taxPct = BigDecimal.ZERO;
            BigDecimal tafCost = BigDecimal.ZERO;

            if ("KR".equals(market)) {
                FrictionCostProperties.StockFee stockFee = frictionCostProperties.getKorea().getStock();
                FrictionCostProperties.EtfFee etfFee = frictionCostProperties.getKorea().getEtf();

                if ("ETF".equals(assetType)) {
                    commissionPct = etfFee.getCommission();
                    commissionCost = notional.multiply(commissionPct);
                } else {
                    commissionPct = stockFee.getCommission();
                    commissionCost = notional.multiply(commissionPct);
                    if ("SELL".equals(side)) {
                        taxPct = stockFee.getTax();
                        taxCost = notional.multiply(taxPct);
                    }
                }
            } else {
                FrictionCostProperties.UsaStockFee usFee = frictionCostProperties.getUsa().getStock();
                commissionPct = usFee.getCommission();
                commissionCost = notional.multiply(commissionPct);

                if ("SELL".equals(side)) {
                    taxPct = usFee.getSecFee();
                    taxCost = notional.multiply(taxPct);
                    tafCost = usFee.getTafPerShareUsd().multiply(new BigDecimal(request.getQuantity()));
                }
            }

            BigDecimal spreadPct = request.getAvgSpreadPct() != null
                    ? request.getAvgSpreadPct()
                    : ("KR".equals(market) ? DEFAULT_SPREAD_KR : DEFAULT_SPREAD_US);
            BigDecimal spreadCost = notional.multiply(spreadPct).divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP);

            BigDecimal slippagePct = getSlippagePct(market, assetType);
            BigDecimal slippageCost = notional.multiply(slippagePct);

            BigDecimal marketImpactPct = BigDecimal.ZERO;
            BigDecimal marketImpactCost = BigDecimal.ZERO;
            if (request.getAvgDailyVolume() != null && request.getAvgDailyVolume() > 0) {
                marketImpactPct = estimateMarketImpact(request.getQuantity(), request.getAvgDailyVolume(), DEFAULT_VOLATILITY);
                marketImpactCost = notional.multiply(marketImpactPct);
            }

            BigDecimal totalExplicitCost = commissionCost.add(taxCost).add(tafCost);
            BigDecimal totalExplicitPct = notional.compareTo(BigDecimal.ZERO) > 0
                    ? totalExplicitCost.divide(notional, 6, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal totalImplicitCost = spreadCost.add(slippageCost).add(marketImpactCost);
            BigDecimal totalImplicitPct = spreadPct.divide(new BigDecimal("2"), 6, RoundingMode.HALF_UP)
                    .add(slippagePct)
                    .add(marketImpactPct);

            BigDecimal totalEstimatedCost = totalExplicitCost.add(totalImplicitCost);
            BigDecimal totalEstimatedPct = totalExplicitPct.add(totalImplicitPct);

            log.debug("TCA 사전 분석: {} {} {} x {} @ {} → 총 비용 {} ({}%)",
                    market, request.getSymbol(), side, request.getQuantity(),
                    request.getArrivalPrice(), totalEstimatedCost.setScale(2, RoundingMode.HALF_UP),
                    totalEstimatedPct.multiply(HUNDRED).setScale(4, RoundingMode.HALF_UP));

            return TcaReport.builder()
                    .symbol(request.getSymbol())
                    .market(market)
                    .side(side)
                    .quantity(request.getQuantity())
                    .arrivalPrice(request.getArrivalPrice())
                    .commissionCost(commissionCost.setScale(2, RoundingMode.HALF_UP))
                    .commissionPct(commissionPct.multiply(HUNDRED).setScale(6, RoundingMode.HALF_UP))
                    .taxCost(taxCost.setScale(2, RoundingMode.HALF_UP))
                    .taxPct(taxPct.multiply(HUNDRED).setScale(6, RoundingMode.HALF_UP))
                    .tafCost(tafCost.setScale(4, RoundingMode.HALF_UP))
                    .totalExplicitCost(totalExplicitCost.setScale(2, RoundingMode.HALF_UP))
                    .totalExplicitPct(totalExplicitPct.multiply(HUNDRED).setScale(4, RoundingMode.HALF_UP))
                    .spreadCost(spreadCost.setScale(2, RoundingMode.HALF_UP))
                    .spreadPct(spreadPct.multiply(HUNDRED).divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP))
                    .slippageCost(slippageCost.setScale(2, RoundingMode.HALF_UP))
                    .slippagePct(slippagePct.multiply(HUNDRED).setScale(4, RoundingMode.HALF_UP))
                    .marketImpactCost(marketImpactCost.setScale(2, RoundingMode.HALF_UP))
                    .marketImpactPct(marketImpactPct.multiply(HUNDRED).setScale(4, RoundingMode.HALF_UP))
                    .totalImplicitCost(totalImplicitCost.setScale(2, RoundingMode.HALF_UP))
                    .totalImplicitPct(totalImplicitPct.multiply(HUNDRED).setScale(4, RoundingMode.HALF_UP))
                    .totalEstimatedCost(totalEstimatedCost.setScale(2, RoundingMode.HALF_UP))
                    .totalEstimatedPct(totalEstimatedPct.multiply(HUNDRED).setScale(4, RoundingMode.HALF_UP))
                    .analysisType(TcaReport.AnalysisType.PRE_TRADE)
                    .analyzedAt(Instant.now())
                    .valid(true)
                    .build();

        } catch (Exception e) {
            log.error("TCA 사전 분석 실패: {}", request.getSymbol(), e);
            return TcaReport.error("Analysis failed: " + e.getMessage());
        }
    }

    @Override
    public TcaReport analyzeCost(String symbol, String market, String side,
                                  int quantity, BigDecimal arrivalPrice,
                                  BigDecimal executionPrice, BigDecimal executionCost) {
        if (arrivalPrice == null || executionPrice == null) {
            return TcaReport.error("Arrival price and execution price are required");
        }

        try {
            BigDecimal notional = executionPrice.multiply(new BigDecimal(quantity));
            BigDecimal is = calculateImplementationShortfall(arrivalPrice, executionPrice, quantity, side, executionCost);
            BigDecimal isPct = notional.compareTo(BigDecimal.ZERO) > 0
                    ? is.divide(notional, 6, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            log.debug("TCA 사후 분석: {} {} {} x {} 도착가={} 체결가={} IS={}",
                    market, symbol, side, quantity, arrivalPrice, executionPrice, is);

            return TcaReport.builder()
                    .symbol(symbol)
                    .market(market != null ? market.toUpperCase() : "KR")
                    .side(side != null ? side.toUpperCase() : "BUY")
                    .quantity(quantity)
                    .arrivalPrice(arrivalPrice)
                    .executionPrice(executionPrice)
                    .totalEstimatedCost(executionCost != null ? executionCost : BigDecimal.ZERO)
                    .implementationShortfall(is.setScale(2, RoundingMode.HALF_UP))
                    .implementationShortfallPct(isPct.multiply(HUNDRED).setScale(4, RoundingMode.HALF_UP))
                    .analysisType(TcaReport.AnalysisType.POST_TRADE)
                    .analyzedAt(Instant.now())
                    .valid(true)
                    .build();

        } catch (Exception e) {
            log.error("TCA 사후 분석 실패: {}", symbol, e);
            return TcaReport.error("Post-trade analysis failed: " + e.getMessage());
        }
    }

    @Override
    public BigDecimal calculateImplementationShortfall(BigDecimal arrivalPrice,
                                                        BigDecimal executionPrice,
                                                        int quantity,
                                                        String side,
                                                        BigDecimal transactionCost) {
        BigDecimal priceDiff = executionPrice.subtract(arrivalPrice);

        if ("SELL".equals(side.toUpperCase())) {
            priceDiff = priceDiff.negate();
        }

        BigDecimal priceImpact = priceDiff.multiply(new BigDecimal(quantity));
        BigDecimal cost = transactionCost != null ? transactionCost : BigDecimal.ZERO;

        return priceImpact.add(cost);
    }

    @Override
    public BigDecimal estimateMarketImpact(int orderQuantity, long avgDailyVolume, BigDecimal volatility) {
        if (avgDailyVolume <= 0) {
            return BigDecimal.ZERO;
        }

        double participationRate = (double) orderQuantity / avgDailyVolume;
        double sqrtPR = Math.sqrt(participationRate);
        double vol = volatility != null ? volatility.doubleValue() : DEFAULT_VOLATILITY.doubleValue();
        double impact = MARKET_IMPACT_COEFFICIENT.doubleValue() * sqrtPR * vol;

        return BigDecimal.valueOf(impact).setScale(6, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateRoundTripCost(String market, String assetType,
                                              BigDecimal notional, int quantity) {
        if (notional == null || notional.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        String mkt = market != null ? market.toUpperCase() : "KR";
        String type = assetType != null ? assetType.toUpperCase() : "STOCK";

        BigDecimal buyCommission;
        BigDecimal sellCommission;
        BigDecimal sellTax;
        BigDecimal slippage;
        BigDecimal taf = BigDecimal.ZERO;

        if ("KR".equals(mkt)) {
            if ("ETF".equals(type)) {
                FrictionCostProperties.EtfFee fee = frictionCostProperties.getKorea().getEtf();
                buyCommission = notional.multiply(fee.getCommission());
                sellCommission = notional.multiply(fee.getCommission());
                sellTax = BigDecimal.ZERO;
                slippage = notional.multiply(fee.getSlippage()).multiply(new BigDecimal("2"));
            } else {
                FrictionCostProperties.StockFee fee = frictionCostProperties.getKorea().getStock();
                buyCommission = notional.multiply(fee.getCommission());
                sellCommission = notional.multiply(fee.getCommission());
                sellTax = notional.multiply(fee.getTax());
                slippage = notional.multiply(fee.getSlippage()).multiply(new BigDecimal("2"));
            }
        } else {
            FrictionCostProperties.UsaStockFee fee = frictionCostProperties.getUsa().getStock();
            buyCommission = notional.multiply(fee.getCommission());
            sellCommission = notional.multiply(fee.getCommission());
            sellTax = notional.multiply(fee.getSecFee());
            slippage = notional.multiply(fee.getSlippage()).multiply(new BigDecimal("2"));
            taf = fee.getTafPerShareUsd().multiply(new BigDecimal(quantity));

            BigDecimal exchangeSpread = frictionCostProperties.getUsa().getCurrency().getExchangeRateSpread();
            slippage = slippage.add(notional.multiply(exchangeSpread).multiply(new BigDecimal("2")));
        }

        return buyCommission.add(sellCommission).add(sellTax).add(slippage).add(taf)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal getSlippagePct(String market, String assetType) {
        if ("KR".equals(market)) {
            if ("ETF".equals(assetType)) {
                return frictionCostProperties.getKorea().getEtf().getSlippage();
            }
            return frictionCostProperties.getKorea().getStock().getSlippage();
        }
        return frictionCostProperties.getUsa().getStock().getSlippage();
    }
}
