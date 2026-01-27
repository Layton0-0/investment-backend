package com.investment.web.controller;

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
     * 마이페이지
     */
    @GetMapping("/mypage")
    public String myPagePage(Authentication authentication, Model model) {
        // 인증 정보는 SecurityContext에서 자동으로 주입됨
        // 마이페이지 데이터는 JavaScript에서 API로 가져옴
        return "mypage";
    }
}
