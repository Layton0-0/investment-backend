package com.investment.api.controller;

import com.investment.account.dto.AccountBalanceDto;
import com.investment.account.dto.AccountPositionDto;
import com.investment.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 계좌 조회 REST API
 */
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {
    
    private final AccountService accountService;
    
    @GetMapping("/{accountNo}/balance")
    public ResponseEntity<AccountBalanceDto> getBalance(
            @PathVariable @NotBlank String accountNo) {
        AccountBalanceDto balance = accountService.getAccountBalance(accountNo);
        return ResponseEntity.ok(balance);
    }
    
    @GetMapping("/{accountNo}/positions")
    public ResponseEntity<List<AccountPositionDto>> getPositions(
            @PathVariable @NotBlank String accountNo) {
        List<AccountPositionDto> positions = accountService.getPositions(accountNo);
        return ResponseEntity.ok(positions);
    }
}
