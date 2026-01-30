package com.investment.web.controller;

import com.investment.auth.dto.MyPageResponseDto;
import com.investment.auth.service.AuthService;
import com.investment.news.dto.NewsItemPageResponseDto;
import com.investment.news.service.NewsItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Slf4j
@Controller
@RequestMapping("/news")
@RequiredArgsConstructor
public class NewsWebController {

    private final AuthService authService;
    private final NewsItemService newsItemService;

    @GetMapping
    public String news(@RequestParam(required = false) String market,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
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

        NewsItemPageResponseDto newsPage = newsItemService.getNewsItems(market, source, symbol, from, to, page, size);
        model.addAttribute("newsPage", newsPage);
        model.addAttribute("market", market);
        model.addAttribute("source", source);
        model.addAttribute("symbol", symbol);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        return "news";
    }
}
