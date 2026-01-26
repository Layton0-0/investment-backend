package com.investment.tradingportfolio.service;

import com.investment.marketdata.config.MarketDataProperties;
import com.investment.domain.entity.TradingPortfolio;
import com.investment.domain.entity.TradingPortfolioItem;
import com.investment.taapi.dto.StockAnalysisDto;
import com.investment.taapi.service.StockAnalysisService;
import com.investment.taapi.service.StockScreeningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 단기 트레이딩 전략 서비스
 * 
 * 월가 헤지펀드 퀀트 트레이더 + 기술적 분석 전문가 + 매크로 트레이더 역할 수행
 * 감정적 판단 금지, 데이터 기반 확률 사고만 사용
 * 시장 데이터 API를 사용하여 실제 기술적 지표를 기반으로 분석합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShortTermTradingStrategyService {
    
    private final StockScreeningService stockScreeningService;
    private final StockAnalysisService stockAnalysisService;
    private final MarketDataProperties marketDataProperties;
    private java.util.Random random = new java.util.Random();
    
    /**
     * 일별 트레이딩 포트폴리오 생성
     */
    public TradingPortfolio generateDailyPortfolio(LocalDate tradingDate) {
        log.info("일별 트레이딩 포트폴리오 생성 시작: tradingDate={}", tradingDate);
        
        // 1. 시장 환경 분석
        String marketSummary = analyzeMarketEnvironment();
        
        // 2. 유망 섹터 분석
        String[] topSectors = analyzeTopSectors();
        
        // 3. 종목 필터링 및 분석
        List<TradingPortfolioItem> items = filterAndAnalyzeStocks();
        
        // 4. 리스크 관리 전략
        String riskManagementStrategy = generateRiskManagementStrategy();
        BigDecimal positionSize = new BigDecimal("10000"); // 기본 포지션 사이즈
        
        TradingPortfolio portfolio = TradingPortfolio.builder()
                .tradingDate(tradingDate)
                .marketSummary(marketSummary)
                .topSector1(topSectors[0])
                .topSector2(topSectors[1])
                .topSector3(topSectors[2])
                .riskManagementStrategy(riskManagementStrategy)
                .positionSize(positionSize)
                .build();
        
        // 종목 추가
        items.forEach(portfolio::addItem);
        
        log.info("일별 트레이딩 포트폴리오 생성 완료: tradingDate={}, items={}", tradingDate, items.size());
        return portfolio;
    }
    
    /**
     * 시장 환경 분석
     */
    private String analyzeMarketEnvironment() {
        // 실제로는 외부 API를 통해 나스닥, S&P500, 다우 지수, VIX, 금리, DXY 등을 조회해야 함
        // 현재는 모의 데이터 생성
        
        StringBuilder summary = new StringBuilder();
        summary.append("📌 시장 요약:\n");
        summary.append("현재 시장은 혼조세를 보이고 있습니다. ");
        summary.append("나스닥은 전일 대비 소폭 상승세를 보이며, ");
        summary.append("S&P500은 횡보 추세를 유지하고 있습니다. ");
        summary.append("VIX는 15-20 범위에서 안정적이며, ");
        summary.append("미국 10년물 국채 금리는 4.0-4.5% 범위를 유지하고 있습니다. ");
        summary.append("달러 인덱스(DXY)는 강세를 보이고 있어 ");
        summary.append("성장주보다 가치주에 유리한 환경입니다. ");
        summary.append("섹터 로테이션은 기술주에서 금융주, 에너지주로 이동하는 추세입니다.");
        
        return summary.toString();
    }
    
    /**
     * 유망 섹터 TOP3 분석
     */
    private String[] analyzeTopSectors() {
        // 실제로는 섹터별 성과, 자금 흐름, 상대 강도 등을 분석해야 함
        String[] sectors = {
            "반도체 (AI 반도체 수요 증가, 공급 부족 지속)",
            "에너지 (원유 가격 상승, 재생에너지 정책 지원)",
            "방산 (지정학적 리스크, 국방 예산 증가)"
        };
        return sectors;
    }
    
    /**
     * 종목 필터링 및 분석
     * 시장 데이터 API를 사용하여 실제 기술적 지표를 기반으로 분석합니다.
     */
    private List<TradingPortfolioItem> filterAndAnalyzeStocks() {
        List<TradingPortfolioItem> items = new ArrayList<>();
        
        // 모의 데이터 사용 옵션이 활성화된 경우
        if (marketDataProperties.isUseMockData()) {
            log.info("설정에 따라 모의 데이터를 사용합니다.");
            return createMockStocks();
        }
        
        try {
            // 시장 데이터 API를 사용하여 종목 스크리닝 (상위 5개)
            List<StockAnalysisDto> screenedStocks = stockScreeningService
                    .screenStocks("1h", 5)
                    .block(); // 동기 처리 (실제 운영에서는 비동기 처리 권장)
            
            if (screenedStocks == null || screenedStocks.isEmpty()) {
                log.warn("스크리닝된 종목이 없습니다. 모의 데이터를 사용합니다.");
                return createMockStocks();
            }
            
            // 각 종목에 대해 상세 분석 및 포트폴리오 아이템 생성
            for (int i = 0; i < screenedStocks.size(); i++) {
                StockAnalysisDto analysis = screenedStocks.get(i);
                
                // 현재가 조회 (VWAP 또는 EMA20 사용)
                BigDecimal currentPrice = analysis.getCurrentPrice();
                if (currentPrice == null) {
                    currentPrice = analysis.getVwap();
                }
                if (currentPrice == null) {
                    currentPrice = analysis.getEma20();
                }
                if (currentPrice == null) {
                    log.warn("종목 가격 정보가 없습니다: symbol={}", analysis.getSymbol());
                    continue;
                }
                
                // 진입가, 손절가, 목표가 계산
                PriceTargets targets = calculatePriceTargets(analysis, currentPrice);
                
                // 기대수익률 계산
                BigDecimal avgEntry = targets.entryMin.add(targets.entryMax)
                        .divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
                BigDecimal profit1 = targets.target1.subtract(avgEntry);
                BigDecimal expectedReturnRate = profit1.divide(avgEntry, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                
                // 투자금액 및 예상 수익 계산
                BigDecimal investmentAmount = new BigDecimal("10000");
                BigDecimal expectedProfit = investmentAmount.multiply(expectedReturnRate)
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                
                // 매수/매도 시간 (한국 시간 기준)
                // 매수 시간: 10:00, 10:30, 11:00, 11:30, 12:00 (분이 60을 초과하지 않도록 처리)
                int hour = 10 + (i * 30) / 60;
                int minute = (i * 30) % 60;
                LocalTime buyTime = LocalTime.of(hour, minute);
                LocalTime sellTime = buyTime.plusHours(2 + i); // 매수 후 2-6시간 후 매도
                
                TradingPortfolioItem item = TradingPortfolioItem.builder()
                        .symbol(analysis.getSymbol())
                        .name(analysis.getName())
                        .entryPriceMin(targets.entryMin)
                        .entryPriceMax(targets.entryMax)
                        .stopLossPrice(targets.stopLoss)
                        .targetPrice1(targets.target1)
                        .targetPrice2(targets.target2)
                        .expectedReturnRate(expectedReturnRate)
                        .riskRewardRatio(targets.riskRewardRatio)
                        .technicalBasis(generateTechnicalBasis(analysis))
                        .supplyDemandBasis(generateSupplyDemandBasis(analysis))
                        .catalystFactor(generateCatalystFactor(analysis.getSymbol()))
                        .buyTime(buyTime)
                        .sellTime(sellTime)
                        .investmentAmount(investmentAmount)
                        .expectedProfit(expectedProfit)
                        .ranking(i + 1)
                        .build();
                
                items.add(item);
            }
            
        } catch (Exception e) {
            log.error("종목 필터링 및 분석 실패", e);
            // 에러 발생 시 모의 데이터 사용
            return createMockStocks();
        }
        
        return items;
    }
    
    /**
     * 가격 목표 계산
     */
    private PriceTargets calculatePriceTargets(StockAnalysisDto analysis, BigDecimal currentPrice) {
        // 진입가: 현재가 ± 1%
        BigDecimal entryMin = currentPrice.multiply(new BigDecimal("0.99"));
        BigDecimal entryMax = currentPrice.multiply(new BigDecimal("1.01"));
        
        // 손절가: ATR 기반 또는 현재가의 3% 하락
        BigDecimal stopLoss;
        if (analysis.getAtr() != null && analysis.getAtr().compareTo(BigDecimal.ZERO) > 0) {
            stopLoss = currentPrice.subtract(analysis.getAtr().multiply(new BigDecimal("2")));
            if (stopLoss.compareTo(currentPrice.multiply(new BigDecimal("0.95"))) < 0) {
                stopLoss = currentPrice.multiply(new BigDecimal("0.97")); // 최소 3% 손절
            }
        } else {
            stopLoss = currentPrice.multiply(new BigDecimal("0.97"));
        }
        
        // 목표가 1: R:R 2:1 기준
        BigDecimal risk = currentPrice.subtract(stopLoss);
        BigDecimal target1 = currentPrice.add(risk.multiply(new BigDecimal("2")));
        
        // 목표가 2: R:R 3:1 기준
        BigDecimal target2 = currentPrice.add(risk.multiply(new BigDecimal("3")));
        
        // 리스크/리워드 비율 계산
        BigDecimal reward = target1.subtract(currentPrice);
        BigDecimal riskRewardRatio = risk.compareTo(BigDecimal.ZERO) > 0 ?
                reward.divide(risk, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        
        return new PriceTargets(entryMin, entryMax, stopLoss, target1, target2, riskRewardRatio);
    }
    
    /**
     * 가격 목표 내부 클래스
     */
    private static class PriceTargets {
        final BigDecimal entryMin;
        final BigDecimal entryMax;
        final BigDecimal stopLoss;
        final BigDecimal target1;
        final BigDecimal target2;
        final BigDecimal riskRewardRatio;
        
        PriceTargets(BigDecimal entryMin, BigDecimal entryMax, BigDecimal stopLoss,
                    BigDecimal target1, BigDecimal target2, BigDecimal riskRewardRatio) {
            this.entryMin = entryMin;
            this.entryMax = entryMax;
            this.stopLoss = stopLoss;
            this.target1 = target1;
            this.target2 = target2;
            this.riskRewardRatio = riskRewardRatio;
        }
    }
    
    /**
     * 모의 종목 데이터 생성 (API 실패 시 사용)
     */
    private List<TradingPortfolioItem> createMockStocks() {
        List<TradingPortfolioItem> items = new ArrayList<>();
        String[][] stockData = {
            {"NVDA", "NVIDIA Corporation", "480.00", "490.00", "470.00", "510.00", "530.00", "5.2", "2.5"},
            {"TSLA", "Tesla Inc.", "240.00", "245.00", "235.00", "255.00", "265.00", "6.3", "2.0"},
            {"AMD", "Advanced Micro Devices", "145.00", "150.00", "140.00", "160.00", "170.00", "8.6", "2.3"},
            {"AAPL", "Apple Inc.", "175.00", "178.00", "172.00", "182.00", "188.00", "4.5", "1.8"},
            {"MSFT", "Microsoft Corporation", "380.00", "385.00", "375.00", "395.00", "405.00", "4.2", "2.1"}
        };
        
        for (int i = 0; i < stockData.length; i++) {
            String[] data = stockData[i];
            BigDecimal entryMin = new BigDecimal(data[2]);
            BigDecimal entryMax = new BigDecimal(data[3]);
            BigDecimal stopLoss = new BigDecimal(data[4]);
            BigDecimal target1 = new BigDecimal(data[5]);
            BigDecimal target2 = new BigDecimal(data[6]);
            BigDecimal riskReward = new BigDecimal(data[8]);
            
            BigDecimal avgEntry = entryMin.add(entryMax).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
            BigDecimal profit1 = target1.subtract(avgEntry);
            BigDecimal expectedReturnRate = profit1.divide(avgEntry, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            
            BigDecimal investmentAmount = new BigDecimal("10000");
            BigDecimal expectedProfit = investmentAmount.multiply(expectedReturnRate)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            
            // 매수 시간: 10:00, 10:30, 11:00, 11:30, 12:00 (분이 60을 초과하지 않도록 처리)
            int hour = 10 + (i * 30) / 60;
            int minute = (i * 30) % 60;
            LocalTime buyTime = LocalTime.of(hour, minute);
            LocalTime sellTime = buyTime.plusHours(2 + i);
            
            TradingPortfolioItem item = TradingPortfolioItem.builder()
                    .symbol(data[0])
                    .name(data[1])
                    .entryPriceMin(entryMin)
                    .entryPriceMax(entryMax)
                    .stopLossPrice(stopLoss)
                    .targetPrice1(target1)
                    .targetPrice2(target2)
                    .expectedReturnRate(expectedReturnRate)
                    .riskRewardRatio(riskReward)
                    .technicalBasis("API 데이터 조회 실패로 모의 데이터 사용")
                    .supplyDemandBasis("API 데이터 조회 실패로 모의 데이터 사용")
                    .catalystFactor("API 데이터 조회 실패로 모의 데이터 사용")
                    .buyTime(buyTime)
                    .sellTime(sellTime)
                    .investmentAmount(investmentAmount)
                    .expectedProfit(expectedProfit)
                    .ranking(i + 1)
                    .build();
            
            items.add(item);
        }
        return items;
    }
    
    /**
     * 기술적 근거 생성 (실제 지표 기반)
     */
    private String generateTechnicalBasis(StockAnalysisDto analysis) {
        StringBuilder basis = new StringBuilder();
        
        if (analysis.getRsi() != null) {
            BigDecimal rsi = analysis.getRsi();
            if (rsi.compareTo(new BigDecimal("70")) > 0) {
                basis.append("RSI 과매수 구간(").append(rsi.setScale(1, RoundingMode.HALF_UP)).append("), ");
            } else if (rsi.compareTo(new BigDecimal("30")) < 0) {
                basis.append("RSI 과매도 구간(").append(rsi.setScale(1, RoundingMode.HALF_UP)).append(")에서 반등, ");
            } else {
                basis.append("RSI 중립 구간(").append(rsi.setScale(1, RoundingMode.HALF_UP)).append("), ");
            }
        }
        
        if (analysis.getMacdHist() != null && analysis.getMacdHist().compareTo(BigDecimal.ZERO) > 0) {
            basis.append("MACD 히스토그램 상승 전환, ");
        }
        
        if (analysis.isGoldenCross()) {
            basis.append("골든크로스(20일 EMA > 60일 EMA), ");
        }
        
        if (analysis.getVwap() != null && analysis.getCurrentPrice() != null) {
            if (analysis.getCurrentPrice().compareTo(analysis.getVwap()) > 0) {
                basis.append("VWAP 위에서 거래, ");
            }
        }
        
        if (analysis.isBreakout()) {
            basis.append("볼린저밴드 상단 돌파, ");
        }
        
        if (analysis.getEma20() != null && analysis.getEma60() != null && analysis.getEma120() != null) {
            if (analysis.getEma20().compareTo(analysis.getEma60()) > 0 && 
                analysis.getEma60().compareTo(analysis.getEma120()) > 0) {
                basis.append("이평선 정배열(20>60>120), ");
            }
        }
        
        String result = basis.toString();
        return result.isEmpty() ? "기술적 지표 분석 중" : result.substring(0, result.length() - 2);
    }
    
    /**
     * 수급 근거 생성 (실제 지표 기반)
     */
    private String generateSupplyDemandBasis(StockAnalysisDto analysis) {
        StringBuilder basis = new StringBuilder();
        
        if (analysis.getVolumeChange() != null) {
            BigDecimal volumeChange = analysis.getVolumeChange();
            if (volumeChange.compareTo(new BigDecimal("150")) > 0) {
                basis.append("거래량 ").append(volumeChange.setScale(1, RoundingMode.HALF_UP))
                        .append("% 급증, ");
            } else if (volumeChange.compareTo(new BigDecimal("120")) > 0) {
                basis.append("거래량 ").append(volumeChange.setScale(1, RoundingMode.HALF_UP))
                        .append("% 증가, ");
            }
        }
        
        if (analysis.getVwap() != null && analysis.getCurrentPrice() != null) {
            if (analysis.getCurrentPrice().compareTo(analysis.getVwap()) > 0) {
                basis.append("VWAP 위에서 거래(매수 압력), ");
            }
        }
        
        if (analysis.getMacdHist() != null && analysis.getMacdHist().compareTo(BigDecimal.ZERO) > 0) {
            basis.append("MACD 상승 모멘텀, ");
        }
        
        String result = basis.toString();
        return result.isEmpty() ? "수급 분석 중" : result.substring(0, result.length() - 2);
    }
    
    /**
     * 촉매 요인 생성
     */
    private String generateCatalystFactor(String symbol) {
        String[] catalysts = {
            "실적 발표 예정(다음주), AI 관련 호재, 반도체 수요 증가 전망",
            "전기차 판매 호조, 충전 인프라 확대, 정부 보조금 정책",
            "신제품 출시 예정, 파트너십 발표, 시장 점유율 확대",
            "배당락일 임박, 주식 분할 가능성, 신제품 라인업 발표",
            "클라우드 수요 증가, 엔터프라이즈 계약 확대, AI 서비스 성장"
        };
        return catalysts[random.nextInt(catalysts.length)];
    }
    
    /**
     * 리스크 관리 전략 생성
     */
    private String generateRiskManagementStrategy() {
        StringBuilder strategy = new StringBuilder();
        strategy.append("📌 리스크 관리 전략:\n");
        strategy.append("- 포지션 사이즈: 종목당 최대 $10,000 (총 자산의 10% 이하)\n");
        strategy.append("- 분할 진입 전략: 목표가의 50% 지점에서 1차 진입, 돌파 시 2차 진입\n");
        strategy.append("- 분할 청산 전략: 1차 목표가 도달 시 50% 청산, 2차 목표가 도달 시 전량 청산\n");
        strategy.append("- 손실 제한 규칙: 손절가 도달 시 즉시 전량 청산, 일일 최대 손실 한도 $2,000");
        return strategy.toString();
    }
}
