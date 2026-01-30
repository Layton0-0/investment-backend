package com.investment.web.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 메뉴 항목 (화면·메뉴 스펙 기반)
 */
@Getter
@AllArgsConstructor
public class MenuItem {
    private final String id;
    private final String label;
    private final String path;
    private final int order;
}
