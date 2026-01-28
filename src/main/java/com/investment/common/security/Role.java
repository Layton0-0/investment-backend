package com.investment.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * 사용자 역할 정의
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
}
