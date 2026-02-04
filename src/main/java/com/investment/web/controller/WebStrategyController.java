package com.investment.web.controller;

import com.investment.account.dto.MainAccountResponseDto;
import com.investment.account.service.AccountService;
import com.investment.auth.dto.MyPageResponseDto;
import com.investment.auth.service.AuthService;
import com.investment.common.exception.DomainException;
import com.investment.strategy.domain.StrategyType;
import com.investment.strategy.dto.StrategyDto;
import com.investment.strategy.service.StrategyManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 전략 관리 웹 컨트롤러 (Thymeleaf)
 * 국내 전략(KR) = 모의계좌(serverType=1), 미국 전략(US) = 실계좌(serverType=0) 자동 사용.
 */
@Controller
@RequestMapping("/strategies")
@RequiredArgsConstructor
public class WebStrategyController {

    private final StrategyManagementService strategyManagementService;
    private final AccountService accountService;
    private final AuthService authService;

    @GetMapping
    public String strategies(@RequestParam(required = false, defaultValue = "") String accountNo,
            @RequestParam(required = false) String serverType,
            Authentication authentication, Model model) {
        return strategiesByMarket(accountNo, null, serverType, authentication, model);
    }

    @GetMapping("/kr")
    public String strategiesKr(@RequestParam(required = false, defaultValue = "") String accountNo,
            @RequestParam(required = false) String serverType,
            Authentication authentication, Model model) {
        return strategiesByMarket(accountNo, "KR", serverType, authentication, model);
    }

    @GetMapping("/us")
    public String strategiesUs(@RequestParam(required = false, defaultValue = "") String accountNo,
            @RequestParam(required = false) String serverType,
            Authentication authentication, Model model) {
        return strategiesByMarket(accountNo, "US", serverType, authentication, model);
    }

    private String strategiesByMarket(String accountNo, String market, String requestServerType,
            Authentication authentication, Model model) {
        String userId = authentication != null ? authentication.getName() : null;
        if (userId != null) {
            try {
                MyPageResponseDto userInfo = authService.getMyPage(userId);
                model.addAttribute("userInfo", userInfo);
            } catch (Exception ignored) {
            }
        }
        accountNo = accountNo != null ? accountNo.trim() : "";
        // URL serverType 우선, 없으면 시장별 기본 (KR=모의, US=실)
        String serverType = ("0".equals(requestServerType) || "1".equals(requestServerType))
                ? requestServerType
                : (market != null ? ("KR".equals(market) ? "1" : "0") : "1");

        // accountNo가 비어 있으면 serverType별 메인 계좌 자동 사용
        if (accountNo.isEmpty() && userId != null) {
            try {
                MainAccountResponseDto main = accountService.getMainAccount(userId, serverType);
                accountNo = main.getAccountNo();
            } catch (DomainException e) {
                String message = "1".equals(serverType)
                        ? "모의계좌를 등록해주세요. 설정에서 API 키와 계좌를 등록할 수 있습니다."
                        : "실계좌를 등록해주세요. 설정에서 API 키와 계좌를 등록할 수 있습니다.";
                model.addAttribute("accountMessage", message);
            }
        }

        if (accountNo != null && !accountNo.isEmpty()) {
            try {
                List<StrategyDto> strategies = strategyManagementService.getStrategies(accountNo, market);

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

        model.addAttribute("accountNo", accountNo != null ? accountNo : "");
        model.addAttribute("market", market != null ? market : "");
        model.addAttribute("marketLabel", "KR".equals(market) ? "국내" : ("US".equals(market) ? "미국" : "전체"));
        return "strategies";
    }

    @PostMapping("/activate")
    public String activateStrategy(@RequestParam String accountNo,
            @RequestParam StrategyType strategyType,
            @RequestParam(required = false) String market,
            @RequestParam(required = false) String serverType) {
        try {
            strategyManagementService.activateStrategy(accountNo, market, strategyType);
        } catch (Exception e) {
            // 에러 처리
        }
        String redirectPath = ("US".equals(market)) ? "/strategies/us" : "/strategies/kr";
        if (market == null || market.isEmpty()) {
            redirectPath = "/strategies";
        }
        String st = ("0".equals(serverType) || "1".equals(serverType)) ? serverType : null;
        return "redirect:" + redirectPath + (st != null ? "?serverType=" + st : "?accountNo=" + accountNo);
    }

    @PostMapping("/stop")
    public String stopStrategy(@RequestParam String accountNo,
            @RequestParam StrategyType strategyType,
            @RequestParam(required = false) String market,
            @RequestParam(required = false) String serverType) {
        try {
            strategyManagementService.stopStrategy(accountNo, market, strategyType);
        } catch (Exception e) {
            // 에러 처리
        }
        String redirectPath = ("US".equals(market)) ? "/strategies/us" : "/strategies/kr";
        if (market == null || market.isEmpty()) {
            redirectPath = "/strategies";
        }
        String st = ("0".equals(serverType) || "1".equals(serverType)) ? serverType : null;
        return "redirect:" + redirectPath + (st != null ? "?serverType=" + st : "?accountNo=" + accountNo);
    }
}
