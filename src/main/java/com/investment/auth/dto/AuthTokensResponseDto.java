package com.investment.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 마이페이지 설정에서 조회하는 토큰 정보 응답 DTO.
 * 한국투자증권 Access Token 및 WebSocket(Approval Key) 토큰을 반환한다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthTokensResponseDto {

    /** 한국투자증권 REST API용 Access Token */
    private String accessToken;

    /** 한국투자증권 WebSocket 구독용 Approval Key (websocket token) */
    private String websocketToken;
}
