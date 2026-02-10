package com.investment.web.controller;

import com.investment.account.dto.*;
import com.investment.account.service.AccountService;
import com.investment.auth.dto.MyPageResponseDto;
import com.investment.auth.service.AuthService;
import com.investment.order.dto.OrderResponseDto;
import com.investment.order.service.OrderService;
import com.investment.factor.dto.PipelineSummaryDto;
import com.investment.factor.service.PipelineSummaryService;
import com.investment.setting.dto.TradingSettingDto;
import com.investment.setting.service.TradingSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 대시보드 웹 컨트롤러 (Thymeleaf)
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final AccountService accountService;
    private final OrderService orderService;
    private final TradingSettingService tradingSettingService;
    private final PipelineSummaryService pipelineSummaryService;
    private final AuthService authService;

    /**
     * 루트 경로는 인증 상태에 따라 리다이렉트
     * - 인증된 사용자: /dashboard로 리다이렉트 (쿼리 파라미터 유지)
     * - 미인증 사용자: /login으로 리다이렉트
     */
    @GetMapping("/")
    public String index(Authentication authentication, HttpServletRequest request) {
        if (authentication != null && authentication.isAuthenticated()) {
            String query = request.getQueryString();
            return "redirect:/dashboard" + (query != null && !query.isEmpty() ? "?" + query : "");
        }
        return "redirect:/login";
    }

    /**
     * 대시보드 페이지
     * 모의계좌·실계좌를 각각 조회하여 한 화면에 두 구역으로 표시합니다.
     */
    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) String serverType,
            Authentication authentication,
            Model model) {
        String userId = authentication != null ? authentication.getName() : null;

        // 사용자 정보 조회
        try {
            if (userId != null) {
                MyPageResponseDto userInfo = authService.getMyPage(userId);
                model.addAttribute("userInfo", userInfo);
            }
        } catch (Exception e) {
            log.warn("사용자 정보 조회 실패: {}", e.getMessage());
        }

        // 계좌 목록 조회 (메뉴·설정 등에서 사용)
        try {
            if (userId != null) {
                AccountListResponseDto accountList = accountService.getUserAccounts(userId);
                model.addAttribute("accountList", accountList);
                model.addAttribute("accounts", accountList.getAccounts());
                model.addAttribute("mainAccountId", accountList.getMainAccountId());
            }
        } catch (Exception e) {
            log.warn("계좌 목록 조회 실패: {}", e.getMessage());
        }

        final Authentication auth = authentication;

        // 모의계좌(serverType=1) 메인 계좌 조회 및 데이터 로드
        try {
            if (userId != null) {
                MainAccountResponseDto mainVirtual = accountService.getMainAccount(userId, "1");
                if (mainVirtual != null && mainVirtual.getAccountNo() != null
                        && !mainVirtual.getAccountNo().trim().isEmpty()) {
                    String accNoVirtual = mainVirtual.getAccountNo();
                    model.addAttribute("accountNoVirtual", accNoVirtual);
                    model.addAttribute("hasVirtualAccount", true);

                    // userId 명시 전달로 병렬 스레드에서 SecurityContext 의존 없이 모의/실 계좌별 올바른 API 호출 보장
                    CompletableFuture<BalanceAndPositionsDto> balanceVirtualFuture = CompletableFuture.supplyAsync(
                            () -> accountService.getBalanceAndPositionsWithUserId(userId, accNoVirtual));
                    CompletableFuture<List<OrderResponseDto>> ordersVirtualFuture = CompletableFuture
                            .supplyAsync(() -> runWithAuth(auth, () -> orderService.getOrders(accNoVirtual)));
                    CompletableFuture<Optional<TradingSettingDto>> settingVirtualFuture = CompletableFuture.supplyAsync(
                            () -> tradingSettingService.getSettingOptional(accNoVirtual));

                    BalanceAndPositionsDto balanceVirtual = balanceVirtualFuture.join();
                    List<AccountPositionDto> positionsVirtual = balanceVirtual != null
                            ? balanceVirtual.getPositions() : List.of();
                    List<OrderResponseDto> ordersVirtual = ordersVirtualFuture.join();
                    Optional<TradingSettingDto> settingVirtualOpt = settingVirtualFuture.join();

                    model.addAttribute("balanceVirtual", balanceVirtual != null ? balanceVirtual.getBalance() : null);
                    model.addAttribute("positionsVirtual", positionsVirtual);
                    model.addAttribute("ordersVirtual", ordersVirtual);
                    settingVirtualOpt.ifPresent(dto -> model.addAttribute("settingVirtual", dto));

                    addStatisticsFor("Virtual", positionsVirtual, model);
                    addMarketSummaryFor("Virtual", positionsVirtual, model);
                    addPipelineSummaryFor("Virtual", accNoVirtual, model);
                } else {
                    model.addAttribute("hasVirtualAccount", false);
                }
            } else {
                model.addAttribute("hasVirtualAccount", false);
            }
        } catch (Exception e) {
            log.warn("모의계좌 데이터 조회 실패: {}", e.getMessage());
            model.addAttribute("hasVirtualAccount", false);
        }

        // 실계좌(serverType=0) 메인 계좌 조회 및 데이터 로드
        try {
            if (userId != null) {
                MainAccountResponseDto mainReal = accountService.getMainAccount(userId, "0");
                if (mainReal != null && mainReal.getAccountNo() != null && !mainReal.getAccountNo().trim().isEmpty()) {
                    String accNoReal = mainReal.getAccountNo();
                    model.addAttribute("accountNoReal", accNoReal);
                    model.addAttribute("hasRealAccount", true);

                    // userId 명시 전달로 병렬 스레드에서 SecurityContext 의존 없이 실계좌 API 호출 보장
                    CompletableFuture<BalanceAndPositionsDto> balanceRealFuture = CompletableFuture.supplyAsync(
                            () -> accountService.getBalanceAndPositionsWithUserId(userId, accNoReal));
                    CompletableFuture<List<OrderResponseDto>> ordersRealFuture = CompletableFuture
                            .supplyAsync(() -> runWithAuth(auth, () -> orderService.getOrders(accNoReal)));
                    CompletableFuture<Optional<TradingSettingDto>> settingRealFuture = CompletableFuture.supplyAsync(
                            () -> tradingSettingService.getSettingOptional(accNoReal));

                    BalanceAndPositionsDto balanceReal = balanceRealFuture.join();
                    List<AccountPositionDto> positionsReal = balanceReal != null
                            ? balanceReal.getPositions() : List.of();
                    List<OrderResponseDto> ordersReal = ordersRealFuture.join();
                    Optional<TradingSettingDto> settingRealOpt = settingRealFuture.join();

                    model.addAttribute("balanceReal", balanceReal != null ? balanceReal.getBalance() : null);
                    model.addAttribute("positionsReal", positionsReal);
                    model.addAttribute("ordersReal", ordersReal);
                    settingRealOpt.ifPresent(dto -> model.addAttribute("settingReal", dto));

                    addStatisticsFor("Real", positionsReal, model);
                    addMarketSummaryFor("Real", positionsReal, model);
                    addPipelineSummaryFor("Real", accNoReal, model);
                } else {
                    model.addAttribute("hasRealAccount", false);
                }
            } else {
                model.addAttribute("hasRealAccount", false);
            }
        } catch (Exception e) {
            log.warn("실계좌 데이터 조회 실패: {}", e.getMessage());
            model.addAttribute("hasRealAccount", false);
        }

        boolean hasVirtual = Boolean.TRUE.equals(model.getAttribute("hasVirtualAccount"));
        boolean hasReal = Boolean.TRUE.equals(model.getAttribute("hasRealAccount"));
        model.addAttribute("hasAccount", hasVirtual || hasReal);
        return "dashboard";
    }

    /**
     * 인증 컨텍스트를 설정한 뒤 작업을 실행 (병렬 스레드에서 계좌/주문 서비스 호출 시 사용)
     */
    private <T> T runWithAuth(Authentication authentication, java.util.function.Supplier<T> supplier) {
        if (authentication == null) {
            return supplier.get();
        }
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            return supplier.get();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void addStatisticsFor(String suffix, List<AccountPositionDto> positions, Model model) {
        String prefix = "positionCount" + suffix;
        String totalPl = "totalProfitLoss" + suffix;
        String totalPlRate = "totalProfitLossRate" + suffix;
        if (positions == null || positions.isEmpty()) {
            model.addAttribute(prefix, 0);
            model.addAttribute(totalPl, BigDecimal.ZERO);
            model.addAttribute(totalPlRate, BigDecimal.ZERO);
            return;
        }
        int positionCount = positions.size();
        BigDecimal totalProfitLoss = positions.stream()
                .map(AccountPositionDto::getProfitLoss)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalInvestedValue = positions.stream()
                .map(p -> p.getAveragePrice().multiply(BigDecimal.valueOf(p.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalProfitLossRate = BigDecimal.ZERO;
        if (totalInvestedValue.compareTo(BigDecimal.ZERO) > 0) {
            totalProfitLossRate = totalProfitLoss
                    .divide(totalInvestedValue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        model.addAttribute(prefix, positionCount);
        model.addAttribute(totalPl, totalProfitLoss);
        model.addAttribute(totalPlRate, totalProfitLossRate);
    }

    private void addMarketSummaryFor(String suffix, List<AccountPositionDto> positions, Model model) {
        String kr = "positionCountKr" + suffix;
        String us = "positionCountUs" + suffix;
        if (positions == null || positions.isEmpty()) {
            model.addAttribute(kr, 0);
            model.addAttribute(us, 0);
            return;
        }
        long countKr = positions.stream()
                .filter(p -> p.getMarket() == null || "KR".equals(p.getMarket()))
                .count();
        long countUs = positions.stream()
                .filter(p -> "US".equals(p.getMarket()))
                .count();
        model.addAttribute(kr, (int) countKr);
        model.addAttribute(us, (int) countUs);
    }

    private void addPipelineSummaryFor(String suffix, String accountNo, Model model) {
        try {
            PipelineSummaryDto summary = pipelineSummaryService.getSummary(LocalDate.now(), accountNo);
            model.addAttribute("universeCountKr" + suffix, summary.getUniverseCountKr());
            model.addAttribute("universeCountUs" + suffix, summary.getUniverseCountUs());
            model.addAttribute("signalCountKr" + suffix, summary.getSignalCountKr());
            model.addAttribute("signalCountUs" + suffix, summary.getSignalCountUs());
            model.addAttribute("openPositionCount" + suffix, summary.getOpenPositionCount());
        } catch (Exception e) {
            log.debug("파이프라인 요약 조회 실패(스킵): {}", e.getMessage());
        }
    }
}
