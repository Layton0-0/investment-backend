package com.investment.factor.scheduler;

import com.investment.common.security.EncryptionUtil;
import com.investment.domain.entity.BrokerType;
import com.investment.domain.entity.StrategyPosition;
import com.investment.domain.entity.TradingSetting;
import com.investment.domain.entity.UserAccount;
import com.investment.domain.repository.StrategyPositionRepository;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.domain.repository.UserAccountRepository;
import com.investment.factor.execution.ExitRuleService;
import com.investment.marketdata.dto.CurrentPriceDto;
import com.investment.ops.service.AuditLogService;
import com.investment.marketdata.service.RealtimeMarketDataService;
import com.investment.order.dto.OrderRequestDto;
import com.investment.order.service.OrderService;
import com.investment.setting.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 4단계 청산 스케줄러.
 * 장중 주기적으로 보유 포지션에 대해 청산 규칙(ATR Trailing Stop, -3%/-10%, Time-Cut)을 평가하고,
 * 실시간 현재가·당일 고가로 trailingHigh를 갱신한 뒤 매도 시그널 발생 시 주문 실행.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PipelineExitScheduler {

    private final StrategyPositionRepository strategyPositionRepository;
    private final TradingSettingRepository tradingSettingRepository;
    private final UserAccountRepository userAccountRepository;
    private final EncryptionUtil encryptionUtil;
    private final ExitRuleService exitRuleService;
    private final RealtimeMarketDataService realtimeMarketDataService;
    private final OrderService orderService;
    private final AuditLogService auditLogService;
    private final SystemSettingService systemSettingService;

    /**
     * 계좌의 서버 타입 조회 (모의=1, 실전=0).
     */
    private String resolveServerTypeForAccount(String userId, String accountNo) {
        if (userId == null || accountNo == null || accountNo.trim().isEmpty()) {
            return "1";
        }
        List<UserAccount> accounts = userAccountRepository.findByUserIdAndBrokerType(userId,
                BrokerType.KOREA_INVESTMENT);
        for (UserAccount account : accounts) {
            try {
                String decrypted = encryptionUtil.decrypt(account.getAccountNoEncrypted());
                if (accountNo.trim().equals(decrypted)) {
                    return account.getServerType() != null ? account.getServerType() : "1";
                }
            } catch (Exception e) {
                log.trace("계좌번호 복호화 스킵: accountId={}", account.getId());
            }
        }
        return "1";
    }

    /**
     * 보유 포지션이 있는 계좌별로 실시간 시세를 조회해 청산 시그널을 평가하고, 설정 시 매도 주문 실행. Spring Batch Job에서
     * 호출.
     */
    @Transactional
    public void evaluateAndExecuteExits() {
        List<String> accountNos = strategyPositionRepository.findDistinctAccountNosWithOpenPositions();
        if (accountNos.isEmpty()) {
            log.trace("청산 스케줄: 보유 포지션 없음");
            return;
        }
        boolean serverAutoExecute = systemSettingService.getBoolean("pipeline.autoExecute");
        boolean serverAllowRealExecution = systemSettingService.getBoolean("pipeline.allowRealExecution");
        for (String accountNo : accountNos) {
            try {
                evaluateAndExecuteExitsForAccount(accountNo, serverAutoExecute, serverAllowRealExecution);
            } catch (Exception e) {
                log.warn("청산 평가/실행 실패: accountNo={}, error={}", accountNo, e.getMessage(), e);
            }
        }
    }

    private void evaluateAndExecuteExitsForAccount(String accountNo, boolean autoExecute, boolean allowRealExecution) {
        List<StrategyPosition> openPositions = strategyPositionRepository
                .findByAccountNoAndExitDtIsNullOrderByEntryDtAsc(accountNo);
        if (openPositions.isEmpty()) {
            return;
        }

        List<String> symbols = openPositions.stream()
                .map(StrategyPosition::getSymbol)
                .distinct()
                .collect(Collectors.toList());

        List<CurrentPriceDto> priceDtos = realtimeMarketDataService.getCurrentPrices(symbols)
                .blockOptional()
                .orElse(List.of());

        Map<String, BigDecimal> currentPriceBySymbol = new HashMap<>();
        Map<String, BigDecimal> todayHighBySymbol = new HashMap<>();
        for (CurrentPriceDto dto : priceDtos) {
            if (dto.getSymbol() != null && dto.getCurrentPrice() != null
                    && dto.getCurrentPrice().compareTo(BigDecimal.ZERO) > 0) {
                currentPriceBySymbol.put(dto.getSymbol(), dto.getCurrentPrice());
            }
            if (dto.getSymbol() != null && dto.getHighPrice() != null
                    && dto.getHighPrice().compareTo(BigDecimal.ZERO) > 0) {
                todayHighBySymbol.put(dto.getSymbol(), dto.getHighPrice());
            }
        }

        if (currentPriceBySymbol.isEmpty()) {
            log.debug("청산 평가 스킵: 실시간 시세 없음, accountNo={}", accountNo);
            return;
        }

        List<ExitRuleService.ExitSignal> signals = exitRuleService.getSellSignals(
                accountNo, currentPriceBySymbol, todayHighBySymbol);
        if (signals.isEmpty()) {
            return;
        }

        String userId = tradingSettingRepository.findByAccountNo(accountNo)
                .map(TradingSetting::getUserId)
                .orElse(null);
        if (userId == null) {
            log.warn("청산 실행 스킵: 계좌에 대한 userId 없음, accountNo={}", accountNo);
            return;
        }

        if (!autoExecute) {
            log.info("청산 시그널 발생 (auto-execute=false, 주문 미실행): accountNo={}, count={}, reasons={}",
                    accountNo, signals.size(),
                    signals.stream().map(ExitRuleService.ExitSignal::getReason).distinct()
                            .collect(Collectors.joining(", ")));
            return;
        }

        // 실전 계좌는 allow-real-execution=false 시 매도 주문 스킵
        String serverType = resolveServerTypeForAccount(userId, accountNo);
        if ("0".equals(serverType) && !allowRealExecution) {
            log.warn("실전 계좌 자동 실행 미허용(allow-real-execution=false), 청산 주문 스킵: accountNo={}, count={}",
                    accountNo, signals.size());
            auditLogService.record(AuditLogService.EVENT_REAL_ACCOUNT_GUARD_BLOCKED, userId, accountNo,
                    "실전 계좌 자동 실행 미허용으로 청산 주문 스킵 count=" + signals.size(),
                    AuditLogService.RESULT_SUCCESS, null);
            return;
        }

        LocalDate today = LocalDate.now();
        for (ExitRuleService.ExitSignal signal : signals) {
            try {
                // US 포지션은 해외 현재가 미연동 시 시세 없을 수 있음 — 시세 없으면 해당 건 스킵
                if (signal.getCurrentPrice() == null || signal.getCurrentPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    log.debug("청산 스킵: 시세 없음, positionId={}, symbol={}, market={}",
                            signal.getPositionId(), signal.getSymbol(), signal.getMarket());
                    continue;
                }
                String positionMarket = (signal.getMarket() != null && !signal.getMarket().isBlank())
                        ? signal.getMarket()
                        : "KR";
                OrderRequestDto sellRequest = OrderRequestDto.builder()
                        .accountNo(accountNo)
                        .symbol(signal.getSymbol())
                        .orderType(OrderRequestDto.OrderType.SELL)
                        .quantity(signal.getQuantity())
                        .price(signal.getCurrentPrice())
                        .market(positionMarket)
                        .exitRuleType(signal.getReason())
                        .build();
                orderService.executeOrderForPipeline(sellRequest, userId);

                strategyPositionRepository.findById(signal.getPositionId()).ifPresent(pos -> {
                    pos.close(today, signal.getCurrentPrice());
                    pos.setExitRuleType(signal.getReason());
                    strategyPositionRepository.save(pos);
                });
                log.info("청산 주문 실행 및 포지션 마감: positionId={}, symbol={}, reason={}",
                        signal.getPositionId(), signal.getSymbol(), signal.getReason());
            } catch (Exception e) {
                log.warn("청산 주문 실패: positionId={}, symbol={}, error={}",
                        signal.getPositionId(), signal.getSymbol(), e.getMessage());
            }
        }
    }
}
