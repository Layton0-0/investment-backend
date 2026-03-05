package com.investment.api.controller;

import com.investment.account.dto.AccountBalanceDto;
import com.investment.account.dto.AccountPositionDto;
import com.investment.account.dto.BalanceRealizedProfitLossDto;
import com.investment.account.dto.BuyableAmountDto;
import com.investment.account.dto.SellableQuantityDto;
import com.investment.account.dto.CancelableOrderDto;
import com.investment.account.dto.OrderHistoryDto;
import com.investment.account.dto.AccountAssetDto;
import com.investment.account.dto.OverseasBalanceSummaryDto;
import com.investment.account.dto.PeriodProfitLossStatusDto;
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
            description = "특정 계좌의 보유 종목 목록을 조회합니다. market=KR(국내만), market=US(해외만), 미지정 시 국내+해외 병합."
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
            @PathVariable @NotBlank String accountNo,
            @Parameter(description = "시장 구분 (KR: 국내만, US: 해외만, 미지정: 전체)")
            @RequestParam(required = false) String market) {
        List<AccountPositionDto> positions = accountService.getPositions(accountNo, market);
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
            summary = "주식정정취소가능주문조회",
            description = "미체결 주문 목록(한국투자증권 API)을 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = CancelableOrderDto.class))),
            @ApiResponse(responseCode = "404", description = "계좌를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{accountNo}/cancelable-orders")
    public ResponseEntity<List<CancelableOrderDto>> getCancelableOrders(
            @Parameter(description = "계좌번호", required = true, example = "12345678")
            @PathVariable @NotBlank String accountNo) {
        List<CancelableOrderDto> list = accountService.getCancelableOrders(accountNo);
        return ResponseEntity.ok(list);
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
            summary = "해외(미국) 계좌 요약",
            description = "해외주식 체결기준현재잔고 API output2 기반 예수금·총자산 등. 대시보드 US 계좌 카드용."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = OverseasBalanceSummaryDto.class))),
            @ApiResponse(responseCode = "404", description = "해외 계좌 요약 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{accountNo}/overseas-summary")
    public ResponseEntity<OverseasBalanceSummaryDto> getOverseasSummary(
            @Parameter(description = "계좌번호", required = true, example = "12345678")
            @PathVariable @NotBlank String accountNo) {
        OverseasBalanceSummaryDto summary = accountService.getOverseasSummary(accountNo);
        if (summary == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(summary);
    }
    
    @Operation(
            summary = "기간별손익조회",
            description = "특정 기간의 손익 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ProfitLossDto.class))),
            @ApiResponse(responseCode = "400", description = "해당 환경에서 미지원(모의투자 계좌 등). code=API_NOT_SUPPORTED"),
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

    @Operation(
            summary = "주식잔고조회_실현손익",
            description = "계좌의 실현손익(주식잔고조회_실현손익 API)을 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = BalanceRealizedProfitLossDto.class))),
            @ApiResponse(responseCode = "404", description = "계좌를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{accountNo}/balance-rlz-pl")
    public ResponseEntity<BalanceRealizedProfitLossDto> getBalanceRealizedProfitLoss(
            @Parameter(description = "계좌번호", required = true, example = "12345678-01")
            @PathVariable @NotBlank String accountNo) {
        BalanceRealizedProfitLossDto dto = accountService.getBalanceRealizedProfitLoss(accountNo);
        return ResponseEntity.ok(dto);
    }

    @Operation(
            summary = "기간별매매손익현황조회",
            description = "기간별 매매손익 현황(TTTC8709R/VTTC8709R)을 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = PeriodProfitLossStatusDto.class))),
            @ApiResponse(responseCode = "404", description = "계좌를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{accountNo}/profit-loss-status")
    public ResponseEntity<PeriodProfitLossStatusDto> getPeriodProfitLossStatus(
            @Parameter(description = "계좌번호", required = true) @PathVariable @NotBlank String accountNo,
            @Parameter(description = "시작일", required = true) @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "종료일", required = true) @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        PeriodProfitLossStatusDto dto = accountService.getPeriodProfitLossStatus(accountNo, startDate, endDate);
        return ResponseEntity.ok(dto);
    }
}
