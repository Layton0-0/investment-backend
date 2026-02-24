package com.investment.tradingportfolio.service;

import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.domain.entity.TradingPortfolio;
import com.investment.domain.entity.TradingPortfolioItem;
import com.investment.domain.repository.TradingPortfolioRepository;
import com.investment.tradingportfolio.dto.TradingPortfolioDto;
import com.investment.tradingportfolio.dto.TradingPortfolioItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 트레이딩 포트폴리오 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradingPortfolioService {
    
    private final TradingPortfolioRepository tradingPortfolioRepository;
    private final ShortTermTradingStrategyService strategyService;
    
    /**
     * 오늘의 트레이딩 포트폴리오 조회 (없으면 Optional.empty, 400 미발생).
     */
    @Transactional(readOnly = true)
    public Optional<TradingPortfolioDto> getTodayPortfolioOptional() {
        LocalDate today = LocalDate.now();
        return tradingPortfolioRepository.findByTradingDate(today)
                .map(this::convertToDto);
    }

    /**
     * 오늘의 트레이딩 포트폴리오 조회 (없으면 예외. 배치/수동 생성용).
     */
    @Transactional(readOnly = true)
    public TradingPortfolioDto getTodayPortfolio() {
        return getTodayPortfolioOptional()
                .orElseThrow(() -> new DomainException(ErrorCode.PORTFOLIO_NOT_FOUND,
                        "오늘의 트레이딩 포트폴리오가 없습니다. 스케줄러를 실행해주세요."));
    }
    
    /**
     * 특정 날짜의 트레이딩 포트폴리오 조회
     */
    @Transactional(readOnly = true)
    public TradingPortfolioDto getPortfolioByDate(LocalDate date) {
        TradingPortfolio portfolio = tradingPortfolioRepository.findByTradingDate(date)
                .orElseThrow(() -> new DomainException(ErrorCode.PORTFOLIO_NOT_FOUND,
                        "해당 날짜의 트레이딩 포트폴리오가 없습니다: " + date));
        
        return convertToDto(portfolio);
    }
    
    /**
     * 최신 트레이딩 포트폴리오 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TradingPortfolioDto> getLatestPortfolios(int limit) {
        List<TradingPortfolio> portfolios = tradingPortfolioRepository.findLatestPortfolios();
        return portfolios.stream()
                .limit(limit)
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * 일별 트레이딩 포트폴리오 생성
     */
    @Transactional
    public TradingPortfolioDto generateDailyPortfolio(LocalDate tradingDate) {
        log.info("일별 트레이딩 포트폴리오 생성: tradingDate={}", tradingDate);
        
        // 이미 존재하는 경우 업데이트
        TradingPortfolio existing = tradingPortfolioRepository.findByTradingDate(tradingDate).orElse(null);
        
        TradingPortfolio portfolio;
        if (existing != null) {
            // 기존 포트폴리오 업데이트
            portfolio = existing;
            portfolio.getItems().clear(); // 기존 종목 제거
            
            TradingPortfolio newPortfolio = strategyService.generateDailyPortfolio(tradingDate);
            portfolio.updateMarketSummary(newPortfolio.getMarketSummary());
            portfolio.updateTopSectors(newPortfolio.getTopSector1(), 
                                     newPortfolio.getTopSector2(), 
                                     newPortfolio.getTopSector3());
            portfolio.updateRiskManagementStrategy(newPortfolio.getRiskManagementStrategy());
            
            newPortfolio.getItems().forEach(portfolio::addItem);
        } else {
            // 새 포트폴리오 생성
            portfolio = strategyService.generateDailyPortfolio(tradingDate);
        }
        
        portfolio = tradingPortfolioRepository.save(portfolio);
        log.info("일별 트레이딩 포트폴리오 생성 완료: tradingDate={}, id={}", tradingDate, portfolio.getId());
        
        return convertToDto(portfolio);
    }
    
    /**
     * 엔티티를 DTO로 변환
     */
    private TradingPortfolioDto convertToDto(TradingPortfolio portfolio) {
        List<TradingPortfolioItemDto> itemDtos = portfolio.getItems().stream()
                .map(this::convertItemToDto)
                .collect(Collectors.toList());
        
        return TradingPortfolioDto.builder()
                .id(portfolio.getId())
                .tradingDate(portfolio.getTradingDate())
                .marketSummary(portfolio.getMarketSummary())
                .topSector1(portfolio.getTopSector1())
                .topSector2(portfolio.getTopSector2())
                .topSector3(portfolio.getTopSector3())
                .riskManagementStrategy(portfolio.getRiskManagementStrategy())
                .positionSize(portfolio.getPositionSize())
                .items(itemDtos)
                .build();
    }
    
    /**
     * 종목 엔티티를 DTO로 변환
     */
    private TradingPortfolioItemDto convertItemToDto(TradingPortfolioItem item) {
        return TradingPortfolioItemDto.builder()
                .id(item.getId())
                .symbol(item.getSymbol())
                .name(item.getName())
                .entryPriceMin(item.getEntryPriceMin())
                .entryPriceMax(item.getEntryPriceMax())
                .stopLossPrice(item.getStopLossPrice())
                .targetPrice1(item.getTargetPrice1())
                .targetPrice2(item.getTargetPrice2())
                .expectedReturnRate(item.getExpectedReturnRate())
                .riskRewardRatio(item.getRiskRewardRatio())
                .technicalBasis(item.getTechnicalBasis())
                .supplyDemandBasis(item.getSupplyDemandBasis())
                .catalystFactor(item.getCatalystFactor())
                .buyTime(item.getBuyTime())
                .sellTime(item.getSellTime())
                .investmentAmount(item.getInvestmentAmount())
                .expectedProfit(item.getExpectedProfit())
                .ranking(item.getRanking())
                .build();
    }
}
