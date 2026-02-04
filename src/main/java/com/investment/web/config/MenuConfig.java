package com.investment.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 메뉴 설정 (09-planning/01-screen-menu-spec 기반)
 * 순서, 메뉴 ID, 표시명, 경로.
 */
@Configuration
public class MenuConfig {

    @Bean(name = "appMenuItems")
    public List<MenuItem> menuItems() {
        return List.of(
                new MenuItem("dashboard", "대시보드", "/", 1),
                new MenuItem("auto-invest", "자동투자 현황", "/auto-invest", 2),
                new MenuItem("strategy-kr", "국내 전략", "/strategies/kr", 3),
                new MenuItem("strategy-us", "미국 전략", "/strategies/us", 4),
                new MenuItem("news-events", "뉴스·이벤트", "/news", 5),
                new MenuItem("portfolio", "포트폴리오", "/portfolio", 6),
                new MenuItem("orders", "주문·체결", "/orders", 7),
                new MenuItem("schedule-status", "스케줄 현황", "/batch", 8),
                new MenuItem("backtest", "백테스트", "/backtest", 9),
                new MenuItem("settings", "설정", "/settings", 10));
    }
}
