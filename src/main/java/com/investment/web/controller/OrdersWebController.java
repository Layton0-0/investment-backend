package com.investment.web.controller;

import com.investment.account.service.AccountService;
import com.investment.auth.dto.MyPageResponseDto;
import com.investment.auth.service.AuthService;
import com.investment.order.dto.OrderResponseDto;
import com.investment.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrdersWebController {

    private final AuthService authService;
    private final AccountService accountService;
    private final OrderService orderService;

    @GetMapping
    public String orders(@RequestParam(required = false) String accountNo,
            @RequestParam(required = false) String serverType,
            Authentication authentication,
            Model model) {
        String userId = authentication != null ? authentication.getName() : null;
        String st = ("0".equals(serverType) || "1".equals(serverType)) ? serverType : "1";

        try {
            if (userId != null) {
                MyPageResponseDto userInfo = authService.getMyPage(userId);
                model.addAttribute("userInfo", userInfo);
            }
        } catch (Exception e) {
            log.warn("사용자 정보 조회 실패: {}", e.getMessage());
        }

        if (accountNo == null || accountNo.trim().isEmpty()) {
            if (userId != null) {
                try {
                    accountNo = accountService.getMainAccount(userId, st).getAccountNo();
                } catch (Exception e) {
                    accountNo = accountService.getUserAccountNo(userId, st);
                }
            } else {
                accountNo = accountService.getDefaultAccountNo();
            }
        }

        if (accountNo != null && !accountNo.trim().isEmpty()) {
            try {
                List<OrderResponseDto> orders = orderService.getOrders(accountNo);
                model.addAttribute("orders", orders);
            } catch (Exception e) {
                log.warn("주문 목록 조회 실패: {}", e.getMessage());
                model.addAttribute("error", e.getMessage());
            }
        }

        model.addAttribute("accountNo", accountNo);
        model.addAttribute("hasAccount", accountNo != null && !accountNo.trim().isEmpty());
        return "orders";
    }
}
