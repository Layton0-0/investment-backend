package com.investment.web.controller;

import com.investment.auth.dto.MyPageResponseDto;
import com.investment.auth.service.AuthService;
import com.investment.tradingportfolio.dto.TradingPortfolioDto;
import com.investment.tradingportfolio.service.TradingPortfolioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/**
 * 트레이딩 포트폴리오 웹 컨트롤러
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class TradingPortfolioWebController {

    private final TradingPortfolioService tradingPortfolioService;
    private final AuthService authService;

    /**
     * 포트폴리오 페이지
     */
    @GetMapping("/portfolio")
    public String portfolio(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication,
            Model model) {
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                MyPageResponseDto userInfo = authService.getMyPage(authentication.getName());
                model.addAttribute("userInfo", userInfo);
            } catch (Exception ignored) {
            }
        }
        try {
            TradingPortfolioDto portfolio;
            if (date != null) {
                portfolio = tradingPortfolioService.getPortfolioByDate(date);
            } else {
                portfolio = tradingPortfolioService.getTodayPortfolio();
            }
            model.addAttribute("portfolio", portfolio);
        } catch (Exception e) {
            log.error("포트폴리오 조회 실패", e);
            model.addAttribute("error", e.getMessage());
        }
        return "portfolio";
    }
}
