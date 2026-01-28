package com.investment.api.controller;

import com.investment.account.dto.AccountListResponseDto;
import com.investment.account.dto.MainAccountResponseDto;
import com.investment.account.service.AccountService;
import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 계좌 관리 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/user/accounts")
@RequiredArgsConstructor
@Tag(name = "계좌 관리", description = "사용자 계좌 조회 및 관리 API")
public class UserAccountController {

    private final AccountService accountService;

    /**
     * 사용자 계좌 목록 조회
     */
    @GetMapping
    @Operation(summary = "계좌 목록 조회", description = "사용자의 모든 계좌 목록을 조회합니다")
    public ResponseEntity<AccountListResponseDto> getUserAccounts(Authentication authentication) {
        String userId = getUserId(authentication);

        AccountListResponseDto response = accountService.getUserAccounts(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 메인 계좌 조회
     */
    @GetMapping("/main")
    @Operation(summary = "메인 계좌 조회", description = "사용자의 메인 계좌를 조회합니다")
    public ResponseEntity<MainAccountResponseDto> getMainAccount(Authentication authentication) {
        String userId = getUserId(authentication);

        try {
            MainAccountResponseDto response = accountService.getMainAccount(userId);
            return ResponseEntity.ok(response);
        } catch (DomainException e) {
            if (e.getErrorCode() == ErrorCode.ACCOUNT_NOT_FOUND) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            throw e;
        }
    }

    /**
     * 특정 계좌 조회
     */
    @GetMapping("/{accountId}")
    @Operation(summary = "특정 계좌 조회", description = "계좌 ID로 특정 계좌를 조회합니다")
    public ResponseEntity<MainAccountResponseDto> getAccount(
            @PathVariable String accountId,
            Authentication authentication) {
        String userId = getUserId(authentication);

        try {
            MainAccountResponseDto response = accountService.getAccountByAccountId(userId, accountId);
            return ResponseEntity.ok(response);
        } catch (DomainException e) {
            if (e.getErrorCode() == ErrorCode.ACCOUNT_NOT_FOUND) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            throw e;
        }
    }

    /**
     * 메인 계좌 변경
     */
    @PutMapping("/{accountId}/main")
    @Operation(summary = "메인 계좌 변경", description = "지정한 계좌를 메인 계좌로 설정합니다")
    public ResponseEntity<Void> setMainAccount(
            @PathVariable String accountId,
            Authentication authentication) {
        String userId = getUserId(authentication);

        try {
            accountService.setMainAccount(userId, accountId);
            return ResponseEntity.ok().build();
        } catch (DomainException e) {
            if (e.getErrorCode() == ErrorCode.ACCOUNT_NOT_FOUND) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            throw e;
        }
    }

    /**
     * 인증 정보에서 사용자 ID 추출
     */
    private String getUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new DomainException(ErrorCode.UNAUTHORIZED, "인증되지 않은 사용자입니다");
        }
        return authentication.getName();
    }
}
