package com.investment.web.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
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

    public MenuModelAdvice(@Qualifier("appMenuItems") List<MenuItem> menuItems) {
        this.menuItems = menuItems;
    }

    @ModelAttribute("menuItems")
    public List<MenuItem> addMenuItems() {
        return menuItems;
    }

    @ModelAttribute("activeMenuId")
    public String addActiveMenuId(HttpServletRequest request) {
        String path = Optional.ofNullable(request.getRequestURI()).orElse("");
        return menuItems.stream()
                .filter(m -> path.equals(m.getPath()) || (m.getPath().length() > 1 && path.startsWith(m.getPath())))
                .findFirst()
                .map(MenuItem::getId)
                .orElse("");
    }
}
