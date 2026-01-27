package com.investment.config;

/**
 * 서버 시작 시 토큰 초기화
 * 
 * @deprecated 서버 시작 시 토큰 발급 방식에서 로그인 시 토큰 발급 방식으로 변경됨.
 *             토큰은 로그인 시점에 DB에 없으면 1회 발급됩니다.
 *             이 클래스는 더 이상 사용되지 않습니다.
 */
@Deprecated
public class TokenInitializationConfig {
    // 서버 시작 시 토큰 발급 로직 제거됨
    // 토큰은 로그인 시점에 AuthService.login()에서 처리됩니다.
}
