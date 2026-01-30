package com.investment.auth.service;

import com.investment.auth.dto.AccountVerifyRequestDto;
import com.investment.auth.dto.AccountVerifyResponseDto;
import com.investment.marketdata.service.KoreaInvestmentTokenClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 회원가입 전 계좌인증 서비스.
 * API Key/Secret·서버타입이 해당 도메인(모의 29443 / 실전 9443)에서 유효한지
 * 접근 토큰 발급 성공 여부로만 확인합니다. (잔고조회 호출 없음)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountVerificationService {

    private final KoreaInvestmentTokenClient tokenClient;

    /**
     * 한국투자증권 계좌 인증: 접근 토큰 발급 성공 시 인증 완료로 간주.
     * API Key/Secret과 서버 타입(모의/실거래)이 일치하면 토큰이 발급되므로, 별도 잔고조회는 하지 않습니다.
     * 키·계좌번호는 로그에 남기지 않음.
     */
    public AccountVerifyResponseDto verifyAccount(AccountVerifyRequestDto request) {
        String appKey = request.getAppKey();
        String appSecret = request.getAppSecret();
        String serverType = request.getServerType();

        try {
            String accessToken = tokenClient.issueAccessToken(appKey, appSecret, serverType)
                    .block(Duration.ofSeconds(30));
            if (accessToken == null) {
                return AccountVerifyResponseDto.builder()
                        .success(false)
                        .message("API 인증에 실패했습니다. API Key·Secret과 서버 타입(모의/실거래)이 일치하는지 확인하세요.")
                        .build();
            }
            return AccountVerifyResponseDto.builder()
                    .success(true)
                    .message("계좌 인증이 완료되었습니다.")
                    .accessToken(accessToken)
                    .build();
        } catch (Exception e) {
            log.debug("계좌인증 중 오류: {}", e.getMessage());
            String message = e.getMessage() != null ? e.getMessage() : "계좌 인증 중 오류가 발생했습니다.";
            if (message.contains("403") || message.contains("Forbidden")) {
                message = "API Key·Secret 또는 서버 타입(모의/실거래)이 올바르지 않습니다.";
            } else if (message.contains("401")) {
                message = "API 인증에 실패했습니다. API Key·Secret을 확인하세요.";
            }
            return AccountVerifyResponseDto.builder()
                    .success(false)
                    .message(message)
                    .build();
        }
    }
}
