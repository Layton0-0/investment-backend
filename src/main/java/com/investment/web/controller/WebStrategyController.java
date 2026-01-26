package com.investment.web.controller;

import com.investment.strategy.domain.StrategyType;
import com.investment.strategy.dto.StrategyDto;
import com.investment.strategy.service.StrategyManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 전략 관리 웹 컨트롤러 (Thymeleaf)
 */
@Controller
@RequestMapping("/strategies")
@RequiredArgsConstructor
public class WebStrategyController {
    
    private final StrategyManagementService strategyManagementService;
    
    @GetMapping
    public String strategies(@RequestParam(required = false, defaultValue = "") String accountNo, 
                            Model model) {
        if (accountNo != null && !accountNo.trim().isEmpty()) {
            try {
                List<StrategyDto> strategies = strategyManagementService.getStrategies(accountNo);
                
                // 전략 타입별로 분류
                StrategyDto shortTerm = strategies.stream()
                        .filter(s -> s.getStrategyType() == StrategyType.SHORT_TERM)
                        .findFirst()
                        .orElse(null);
                
                StrategyDto mediumTerm = strategies.stream()
                        .filter(s -> s.getStrategyType() == StrategyType.MEDIUM_TERM)
                        .findFirst()
                        .orElse(null);
                
                StrategyDto longTerm = strategies.stream()
                        .filter(s -> s.getStrategyType() == StrategyType.LONG_TERM)
                        .findFirst()
                        .orElse(null);
                
                model.addAttribute("shortTerm", shortTerm);
                model.addAttribute("mediumTerm", mediumTerm);
                model.addAttribute("longTerm", longTerm);
                model.addAttribute("strategies", strategies);
            } catch (Exception e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        
        model.addAttribute("accountNo", accountNo);
        return "strategies";
    }
    
    @PostMapping("/activate")
    public String activateStrategy(@RequestParam String accountNo, 
                                   @RequestParam StrategyType strategyType) {
        try {
            strategyManagementService.activateStrategy(accountNo, strategyType);
        } catch (Exception e) {
            // 에러 처리
        }
        return "redirect:/strategies?accountNo=" + accountNo;
    }
    
    @PostMapping("/stop")
    public String stopStrategy(@RequestParam String accountNo, 
                              @RequestParam StrategyType strategyType) {
        try {
            strategyManagementService.stopStrategy(accountNo, strategyType);
        } catch (Exception e) {
            // 에러 처리
        }
        return "redirect:/strategies?accountNo=" + accountNo;
    }
}
