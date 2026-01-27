package com.investment.api.controller;

import com.investment.account.dto.AccountBalanceDto;
import com.investment.account.dto.AccountPositionDto;
import com.investment.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * 계좌 조회 REST API
 */
@Tag(name = "Account", description = "계좌 조회 API")
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {
    
    private final AccountService accountService;
    
    @Operation(
            summary = "계좌 잔고 조회",
            description = "특정 계좌의 잔고 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = AccountBalanceDto.class))),
            @ApiResponse(responseCode = "404", description = "계좌를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{accountNo}/balance")
    public ResponseEntity<AccountBalanceDto> getBalance(
            @Parameter(description = "계좌번호", required = true, example = "12345678")
            @PathVariable @NotBlank String accountNo) {
        AccountBalanceDto balance = accountService.getAccountBalance(accountNo);
        return ResponseEntity.ok(balance);
    }
    
    @Operation(
            summary = "보유 종목 조회",
            description = "특정 계좌의 보유 종목 목록을 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = AccountPositionDto.class))),
            @ApiResponse(responseCode = "404", description = "계좌를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{accountNo}/positions")
    public ResponseEntity<List<AccountPositionDto>> getPositions(
            @Parameter(description = "계좌번호", required = true, example = "12345678")
            @PathVariable @NotBlank String accountNo) {
        List<AccountPositionDto> positions = accountService.getPositions(accountNo);
        return ResponseEntity.ok(positions);
    }
}
