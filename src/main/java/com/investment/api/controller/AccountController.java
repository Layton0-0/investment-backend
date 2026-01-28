package com.investment.api.controller;

import com.investment.account.dto.AccountBalanceDto;
import com.investment.account.dto.AccountPositionDto;
import com.investment.account.dto.BuyableAmountDto;
import com.investment.account.dto.SellableQuantityDto;
import com.investment.account.dto.OrderHistoryDto;
import com.investment.account.dto.AccountAssetDto;
import com.investment.account.dto.ProfitLossDto;
import com.investment.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
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
    
    @Operation(
            summary = "매수가능조회",
            description = "특정 종목의 매수 가능 금액 및 수량을 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = BuyableAmountDto.class))),
            @ApiResponse(responseCode = "404", description = "계좌를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{accountNo}/buyable-amount")
    public ResponseEntity<BuyableAmountDto> getBuyableAmount(
            @Parameter(description = "계좌번호", required = true, example = "12345678")
            @PathVariable @NotBlank String accountNo,
            @Parameter(description = "종목코드", required = true, example = "005930")
            @RequestParam @NotBlank String symbol,
            @Parameter(description = "주문가격", required = true, example = "75000")
            @RequestParam @NotNull @Positive BigDecimal price) {
        BuyableAmountDto buyableAmount = accountService.getBuyableAmount(accountNo, symbol, price);
        return ResponseEntity.ok(buyableAmount);
    }
    
    @Operation(
            summary = "매도가능수량조회",
            description = "특정 종목의 매도 가능 수량을 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = SellableQuantityDto.class))),
            @ApiResponse(responseCode = "404", description = "계좌를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{accountNo}/sellable-quantity")
    public ResponseEntity<SellableQuantityDto> getSellableQuantity(
            @Parameter(description = "계좌번호", required = true, example = "12345678")
            @PathVariable @NotBlank String accountNo,
            @Parameter(description = "종목코드", required = true, example = "005930")
            @RequestParam @NotBlank String symbol) {
        SellableQuantityDto sellableQuantity = accountService.getSellableQuantity(accountNo, symbol);
        return ResponseEntity.ok(sellableQuantity);
    }
    
    @Operation(
            summary = "주문체결조회",
            description = "특정 기간의 주문 체결 내역을 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = OrderHistoryDto.class))),
            @ApiResponse(responseCode = "404", description = "계좌를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{accountNo}/order-history")
    public ResponseEntity<List<OrderHistoryDto>> getOrderHistory(
            @Parameter(description = "계좌번호", required = true, example = "12345678")
            @PathVariable @NotBlank String accountNo,
            @Parameter(description = "시작일", required = true, example = "2026-01-01")
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "종료일", required = true, example = "2026-01-31")
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<OrderHistoryDto> orderHistory = accountService.getOrderHistory(accountNo, startDate, endDate);
        return ResponseEntity.ok(orderHistory);
    }
    
    @Operation(
            summary = "투자계좌자산현황조회",
            description = "계좌의 자산 현황을 종합 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = AccountAssetDto.class))),
            @ApiResponse(responseCode = "404", description = "계좌를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{accountNo}/assets")
    public ResponseEntity<AccountAssetDto> getAccountAssets(
            @Parameter(description = "계좌번호", required = true, example = "12345678")
            @PathVariable @NotBlank String accountNo) {
        AccountAssetDto assets = accountService.getAccountAssets(accountNo);
        return ResponseEntity.ok(assets);
    }
    
    @Operation(
            summary = "기간별손익조회",
            description = "특정 기간의 손익 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ProfitLossDto.class))),
            @ApiResponse(responseCode = "404", description = "계좌를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{accountNo}/profit-loss")
    public ResponseEntity<ProfitLossDto> getPeriodProfitLoss(
            @Parameter(description = "계좌번호", required = true, example = "12345678")
            @PathVariable @NotBlank String accountNo,
            @Parameter(description = "시작일", required = true, example = "2026-01-01")
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "종료일", required = true, example = "2026-01-31")
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        ProfitLossDto profitLoss = accountService.getPeriodProfitLoss(accountNo, startDate, endDate);
        return ResponseEntity.ok(profitLoss);
    }
}
