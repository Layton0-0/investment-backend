package com.investment.web.controller;

import com.investment.auth.dto.MyPageResponseDto;
import com.investment.auth.service.AuthService;
import com.investment.domain.entity.BrokerType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 인증 웹 컨트롤러 (Thymeleaf)
 * 로그인, 회원가입, 마이페이지 페이지를 렌더링합니다.
 */
@Controller
@RequiredArgsConstructor
public class AuthWebController {

    private final AuthService authService;

    /**
     * 로그인 페이지
     */
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    /**
     * 회원가입 페이지
     */
    @GetMapping("/register")
    public String registerPage(Model model) {
        // 증권사 목록을 모델에 추가
        model.addAttribute("brokerTypes", BrokerType.values());
        return "register";
    }

    /**
     * 마이페이지 (비밀번호·사용자명 등 내 정보)
     */
    @GetMapping("/mypage")
    public String myPagePage(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                MyPageResponseDto userInfo = authService.getMyPage(authentication.getName());
                model.addAttribute("userInfo", userInfo);
            } catch (Exception ignored) {
            }
        }
        return "mypage";
    }

    /**
     * 설정 전용 화면 (계좌/API·거래 설정)
     */
    @GetMapping("/settings")
    public String settingsPage(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                MyPageResponseDto userInfo = authService.getMyPage(authentication.getName());
                model.addAttribute("userInfo", userInfo);
            } catch (Exception ignored) {
            }
        }
        return "settings";
    }
}
