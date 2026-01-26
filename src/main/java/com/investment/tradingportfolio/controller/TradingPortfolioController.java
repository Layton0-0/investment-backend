package com.investment.tradingportfolio.controller;

import com.investment.tradingportfolio.dto.TradingPortfolioDto;
import com.investment.tradingportfolio.service.TradingPortfolioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 트레이딩 포트폴리오 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/trading-portfolios")
@RequiredArgsConstructor
public class TradingPortfolioController {
    
    private final TradingPortfolioService tradingPortfolioService;
    
    /**
     * 오늘의 트레이딩 포트폴리오 조회
     */
    @GetMapping("/today")
    public ResponseEntity<TradingPortfolioDto> getTodayPortfolio() {
        log.debug("오늘의 트레이딩 포트폴리오 조회");
        TradingPortfolioDto portfolio = tradingPortfolioService.getTodayPortfolio();
        return ResponseEntity.ok(portfolio);
    }
    
    /**
     * 특정 날짜의 트레이딩 포트폴리오 조회
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<TradingPortfolioDto> getPortfolioByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.debug("트레이딩 포트폴리오 조회: date={}", date);
        TradingPortfolioDto portfolio = tradingPortfolioService.getPortfolioByDate(date);
        return ResponseEntity.ok(portfolio);
    }
    
    /**
     * 최신 트레이딩 포트폴리오 목록 조회
     */
    @GetMapping("/latest")
    public ResponseEntity<List<TradingPortfolioDto>> getLatestPortfolios(
            @RequestParam(defaultValue = "10") int limit) {
        log.debug("최신 트레이딩 포트폴리오 목록 조회: limit={}", limit);
        List<TradingPortfolioDto> portfolios = tradingPortfolioService.getLatestPortfolios(limit);
        return ResponseEntity.ok(portfolios);
    }
    
    /**
     * 일별 트레이딩 포트폴리오 수동 생성 (관리자용)
     */
    @PostMapping("/generate")
    public ResponseEntity<TradingPortfolioDto> generatePortfolio(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        log.info("트레이딩 포트폴리오 수동 생성: date={}", targetDate);
        TradingPortfolioDto portfolio = tradingPortfolioService.generateDailyPortfolio(targetDate);
        return ResponseEntity.ok(portfolio);
    }
}
