package com.investment.web.config;

import com.investment.account.dto.MainAccountResponseDto;
import com.investment.account.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;
import java.util.Optional;

/**
 * 모든 뷰에 메뉴 목록과 현재 활성 메뉴 ID를 주입
 */
@ControllerAdvice
public class MenuModelAdvice {

    private final List<MenuItem> menuItems;
    private final AccountService accountService;

    public MenuModelAdvice(@Qualifier("appMenuItems") List<MenuItem> menuItems,
            AccountService accountService) {
        this.menuItems = menuItems;
        this.accountService = accountService;
    }

    @ModelAttribute("menuItems")
    public List<MenuItem> addMenuItems() {
        return menuItems;
    }

    @ModelAttribute("activeMenuId")
    public String addActiveMenuId(HttpServletRequest request) {
        String path = Optional.ofNullable(request.getRequestURI()).orElse("");
        if ("/dashboard".equals(path)) {
            return "dashboard";
        }
        return menuItems.stream()
                .filter(m -> path.equals(m.getPath()) || (m.getPath().length() > 1 && path.startsWith(m.getPath())))
                .findFirst()
                .map(MenuItem::getId)
                .orElse("");
    }

    /**
     * 현재 계좌 타입 (모의=1, 실계좌=0). URL 쿼리 serverType 기준, 기본값 1.
     * 메뉴·계좌 탭 링크에 반영.
     */
    @ModelAttribute("currentServerType")
    public String addCurrentServerType(HttpServletRequest request) {
        String st = request.getParameter("serverType");
        return ("0".equals(st) || "1".equals(st)) ? st : "1";
    }

    /**
     * 현재 요청 URI (경로만, 쿼리 제외). 계좌 탭에서 serverType만 바꿔 이동할 때 사용.
     */
    @ModelAttribute("currentPath")
    public String addCurrentPath(HttpServletRequest request) {
        String uri = Optional.ofNullable(request.getRequestURI()).orElse("");
        return (uri != null && !uri.isEmpty()) ? uri : "/dashboard";
    }

    /**
     * 실계좌(serverType=0) 등록 여부. 로그인 사용자만 조회, 미등록 시 탭 영역에 안내 노출용.
     */
    @ModelAttribute("hasRealAccount")
    public boolean addHasRealAccount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            return false;
        }
        try {
            MainAccountResponseDto main = accountService.getMainAccount(auth.getName(), "0");
            return main != null && main.getAccountNo() != null && !main.getAccountNo().trim().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
