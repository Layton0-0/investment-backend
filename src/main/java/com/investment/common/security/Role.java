package com.investment.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;

/**
 * 사용자 역할 정의.
 * DB User.role 문자열("User", "Admin")과 Spring Security 권한(ROLE_*) 매핑.
 */
public enum Role {
    USER("ROLE_USER"),
    ADMIN("ROLE_ADMIN");

    private final String authority;

    Role(String authority) {
        this.authority = authority;
    }

    public String getAuthority() {
        return authority;
    }

    public GrantedAuthority toGrantedAuthority() {
        return new SimpleGrantedAuthority(authority);
    }

    /**
     * DB에 저장된 역할 문자열을 Spring Security 권한 목록으로 변환.
     * hasRole("ADMIN") / hasRole("USER")와 일치하도록 ROLE_ADMIN, ROLE_USER 부여.
     * 기존 DB에 "Ops"가 남아 있는 경우(마이그레이션 전) ADMIN으로 간주한다.
     *
     * @param dbRole TB_USERS.ROLE 값 (User, Admin)
     * @return ROLE_USER 또는 ROLE_ADMIN (최소 1개)
     */
    public static List<GrantedAuthority> fromDbRole(String dbRole) {
        if (dbRole == null || dbRole.isBlank()) {
            return Collections.singletonList(USER.toGrantedAuthority());
        }
        String normalized = dbRole.trim();
        for (Role r : values()) {
            if (r.name().equalsIgnoreCase(normalized) || r.authority.equals("ROLE_" + normalized)) {
                return Collections.singletonList(r.toGrantedAuthority());
            }
        }
        if ("Admin".equalsIgnoreCase(normalized)) {
            return Collections.singletonList(ADMIN.toGrantedAuthority());
        }
        if ("Ops".equalsIgnoreCase(normalized)) {
            return Collections.singletonList(ADMIN.toGrantedAuthority());
        }
        return Collections.singletonList(USER.toGrantedAuthority());
    }
}
