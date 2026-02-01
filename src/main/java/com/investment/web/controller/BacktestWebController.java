package com.investment.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 백테스트 화면 — GET /backtest → backtest.html.
 * 실행은 POST /api/v1/backtest (BacktestController)에서 처리.
 */
@Controller
@RequestMapping("/backtest")
public class BacktestWebController {

    @GetMapping
    public String backtest() {
        return "backtest";
    }
}
