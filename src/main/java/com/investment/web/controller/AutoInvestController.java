package com.investment.web.controller;

import com.investment.account.service.AccountService;
import com.investment.auth.dto.MyPageResponseDto;
import com.investment.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 자동투자 현황 웹 컨트롤러
 * 4단계 파이프라인(유니버스→시그널→자금관리→매매실행) 요약·시그널/체결 스텁
 */
@Slf4j
@Controller
@RequestMapping("/auto-invest")
@RequiredArgsConstructor
public class AutoInvestController {

    private final AuthService authService;
    private final AccountService accountService;

    @GetMapping
    @SuppressWarnings("deprecation")
    public String autoInvest(@RequestParam(required = false) String accountNo,
            Authentication authentication,
            Model model) {
        String userId = authentication != null ? authentication.getName() : null;

        try {
            if (userId != null) {
                MyPageResponseDto userInfo = authService.getMyPage(userId);
                model.addAttribute("userInfo", userInfo);
            }
        } catch (Exception e) {
            log.warn("사용자 정보 조회 실패: {}", e.getMessage());
        }

        if (accountNo == null || accountNo.trim().isEmpty() && userId != null) {
            try {
                accountNo = accountService.getMainAccount(userId).getAccountNo();
            } catch (Exception e) {
                accountNo = accountService.getUserAccountNo(userId);
            }
        }
        if (accountNo == null || accountNo.trim().isEmpty()) {
            accountNo = accountService.getDefaultAccountNo();
        }

        model.addAttribute("accountNo", accountNo);
        model.addAttribute("hasAccount", accountNo != null && !accountNo.trim().isEmpty());
        return "auto-invest";
    }
}
