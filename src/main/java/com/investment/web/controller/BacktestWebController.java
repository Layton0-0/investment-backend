package com.investment.web.controller;

import com.investment.auth.dto.MyPageResponseDto;
import com.investment.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 백테스트 화면 — GET /backtest → backtest.html.
 * 실행은 POST /api/v1/backtest (BacktestController)에서 처리.
 */
@Controller
@RequestMapping("/backtest")
@RequiredArgsConstructor
public class BacktestWebController {

    private final AuthService authService;

    @GetMapping
    public String backtest(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                MyPageResponseDto userInfo = authService.getMyPage(authentication.getName());
                model.addAttribute("userInfo", userInfo);
            } catch (Exception ignored) {
            }
        }
        return "backtest";
    }
}
