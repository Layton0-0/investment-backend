package com.investment.web.controller;

import com.investment.account.dto.*;
import com.investment.common.security.LogMaskingUtil;
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
     * - 인증된 사용자: /dashboard로 리다이렉트
     * - 미인증 사용자: /login으로 리다이렉트
     */
    @GetMapping("/")
    public String index(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        return "redirect:/login";
    }

    /**
     * 대시보드 페이지
     * 계좌 선택을 지원하며, 계좌번호가 없으면 자동으로 메인 계좌를 조회합니다.
     */
    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) String accountId,
            @RequestParam(required = false, defaultValue = "") String accountNo,
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
            // 사용자 정보 조회 실패해도 계속 진행
        }

        // 계좌 목록 조회
        try {
            if (userId != null) {
                AccountListResponseDto accountList = accountService.getUserAccounts(userId);
                model.addAttribute("accountList", accountList);
                model.addAttribute("accounts", accountList.getAccounts());
                model.addAttribute("mainAccountId", accountList.getMainAccountId());
            }
        } catch (Exception e) {
            log.warn("계좌 목록 조회 실패: {}", e.getMessage());
            // 계좌 목록 조회 실패해도 계속 진행
        }

        // 계좌 선택 처리
        if (accountId != null && !accountId.trim().isEmpty() && userId != null) {
            try {
                // accountId로 계좌 조회
                MainAccountResponseDto account = accountService.getAccountByAccountId(userId, accountId);
                accountNo = account.getAccountNo();
                model.addAttribute("selectedAccountId", accountId);
            } catch (Exception e) {
                log.warn("계좌 조회 실패: accountId={}, error={}", accountId, e.getMessage());
            }
        }

        // 계좌번호가 없으면 메인 계좌 자동 조회
        if (accountNo == null || accountNo.trim().isEmpty()) {
            if (userId != null) {
                try {
                    MainAccountResponseDto mainAccount = accountService.getMainAccount(userId);
                    accountNo = mainAccount.getAccountNo();
                    model.addAttribute("selectedAccountId", mainAccount.getAccountId());
                } catch (Exception e) {
                    log.warn("메인 계좌 조회 실패: {}", e.getMessage());
                    // 메인 계좌 조회 실패 시 기존 방식 사용
                    accountNo = accountService.getUserAccountNo(userId);
                }
            } else {
                // 인증 정보가 없으면 기존 방식 사용 (하위 호환성)
                accountNo = accountService.getDefaultAccountNo();
            }
        }

        // 계좌번호가 있으면 데이터 조회 (잔고+보유 1회, 주문·설정 병렬 로딩으로 응답 시간 단축)
        if (accountNo != null && !accountNo.trim().isEmpty()) {
            try {
                final String accNo = accountNo;
                final Authentication auth = authentication;

                CompletableFuture<BalanceAndPositionsDto> balanceAndPositionsFuture =
                        CompletableFuture.supplyAsync(() -> runWithAuth(auth, () -> accountService.getBalanceAndPositions(accNo)));
                CompletableFuture<List<OrderResponseDto>> ordersFuture =
                        CompletableFuture.supplyAsync(() -> runWithAuth(auth, () -> orderService.getOrders(accNo)));
                CompletableFuture<Optional<TradingSettingDto>> settingFuture =
                        CompletableFuture.supplyAsync(() -> runWithAuth(auth, () -> tradingSettingService.getSettingOptional(accNo)));

                BalanceAndPositionsDto balanceAndPositions = balanceAndPositionsFuture.join();
                AccountBalanceDto balance = balanceAndPositions.getBalance();
                List<AccountPositionDto> positions = balanceAndPositions.getPositions();
                List<OrderResponseDto> orders = ordersFuture.join();
                Optional<TradingSettingDto> settingOpt = settingFuture.join();

                model.addAttribute("balance", balance);
                model.addAttribute("positions", positions);
                model.addAttribute("orders", orders);
                settingOpt.ifPresent(dto -> model.addAttribute("setting", dto));

                calculateStatistics(positions, model);
                addMarketSummary(positions, model);
                addPipelineSummary(accNo, model);
            } catch (Exception e) {
                log.error("계좌 데이터 조회 실패: accountNo={}", LogMaskingUtil.maskAccountNo(accountNo), e);
                model.addAttribute("error", "계좌 정보를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
            }
        }

        model.addAttribute("accountNo", accountNo);
        model.addAttribute("hasAccount", accountNo != null && !accountNo.trim().isEmpty());
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

    /**
     * 통계 정보 계산
     */
    private void calculateStatistics(List<AccountPositionDto> positions, Model model) {
        if (positions == null || positions.isEmpty()) {
            model.addAttribute("positionCount", 0);
            model.addAttribute("totalProfitLoss", BigDecimal.ZERO);
            model.addAttribute("totalProfitLossRate", BigDecimal.ZERO);
            return;
        }

        int positionCount = positions.size();
        BigDecimal totalProfitLoss = positions.stream()
                .map(AccountPositionDto::getProfitLoss)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 총 손익률 계산 (가중 평균)
        BigDecimal totalInvestedValue = positions.stream()
                .map(p -> p.getAveragePrice().multiply(BigDecimal.valueOf(p.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalProfitLossRate = BigDecimal.ZERO;
        if (totalInvestedValue.compareTo(BigDecimal.ZERO) > 0) {
            totalProfitLossRate = totalProfitLoss
                    .divide(totalInvestedValue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        model.addAttribute("positionCount", positionCount);
        model.addAttribute("totalProfitLoss", totalProfitLoss);
        model.addAttribute("totalProfitLossRate", totalProfitLossRate);
    }

    /**
     * 시장(KR/US)별 보유 종목 수 집계 (대시보드 계좌 요약 KR/US 구분용).
     */
    private void addMarketSummary(List<AccountPositionDto> positions, Model model) {
        if (positions == null || positions.isEmpty()) {
            model.addAttribute("positionCountKr", 0);
            model.addAttribute("positionCountUs", 0);
            return;
        }
        long countKr = positions.stream()
                .filter(p -> p.getMarket() == null || "KR".equals(p.getMarket()))
                .count();
        long countUs = positions.stream()
                .filter(p -> "US".equals(p.getMarket()))
                .count();
        model.addAttribute("positionCountKr", (int) countKr);
        model.addAttribute("positionCountUs", (int) countUs);
    }

    /**
     * 자동투자 파이프라인 요약 (유니버스·시그널·보유 포지션 수) 및 자동투자 ON/OFF.
     */
    private void addPipelineSummary(String accountNo, Model model) {
        try {
            PipelineSummaryDto summary = pipelineSummaryService.getSummary(LocalDate.now(), accountNo);
            model.addAttribute("pipelineSummary", summary);
            model.addAttribute("universeCountKr", summary.getUniverseCountKr());
            model.addAttribute("universeCountUs", summary.getUniverseCountUs());
            model.addAttribute("signalCountKr", summary.getSignalCountKr());
            model.addAttribute("signalCountUs", summary.getSignalCountUs());
            model.addAttribute("openPositionCount", summary.getOpenPositionCount());
        } catch (Exception e) {
            log.debug("파이프라인 요약 조회 실패(스킵): {}", e.getMessage());
            model.addAttribute("pipelineSummary", null);
        }
    }
}
