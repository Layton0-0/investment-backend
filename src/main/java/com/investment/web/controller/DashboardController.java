package com.investment.web.controller;

import com.investment.account.dto.AccountBalanceDto;
import com.investment.account.dto.AccountPositionDto;
import com.investment.account.service.AccountService;
import com.investment.order.dto.OrderResponseDto;
import com.investment.order.service.OrderService;
import com.investment.setting.dto.TradingSettingDto;
import com.investment.setting.service.TradingSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 대시보드 웹 컨트롤러 (Thymeleaf)
 */
@Controller
@RequiredArgsConstructor
public class DashboardController {
    
    private final AccountService accountService;
    private final OrderService orderService;
    private final TradingSettingService tradingSettingService;
    
    @GetMapping("/")
    public String index(@RequestParam(required = false, defaultValue = "") String accountNo, 
                       Model model) {
        if (accountNo != null && !accountNo.trim().isEmpty()) {
            try {
                AccountBalanceDto balance = accountService.getAccountBalance(accountNo);
                List<AccountPositionDto> positions = accountService.getPositions(accountNo);
                List<OrderResponseDto> orders = orderService.getOrders(accountNo);
                TradingSettingDto setting = tradingSettingService.getSetting(accountNo);
                
                model.addAttribute("balance", balance);
                model.addAttribute("positions", positions);
                model.addAttribute("orders", orders);
                model.addAttribute("setting", setting);
            } catch (Exception e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        
        model.addAttribute("accountNo", accountNo);
        return "dashboard";
    }
}
