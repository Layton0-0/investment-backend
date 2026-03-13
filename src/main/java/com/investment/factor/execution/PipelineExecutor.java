package com.investment.factor.execution;

import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.common.security.EncryptionUtil;
import com.investment.common.security.LogMaskingUtil;
import com.investment.domain.entity.BrokerType;
import com.investment.domain.entity.Order;
import com.investment.domain.entity.StrategyPosition;
import com.investment.domain.entity.UserAccount;
import com.investment.domain.repository.OrderRepository;
import com.investment.domain.repository.StrategyPositionRepository;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.domain.repository.UserAccountRepository;
import com.investment.factor.dto.PositionRecommendationDto;
import com.investment.factor.service.PositionSizingService;
import com.investment.factor.service.TradingWindowService;
import com.investment.ops.service.AuditLogService;
import com.investment.order.dto.OrderRequestDto;
import com.investment.order.dto.OrderResponseDto;
import com.investment.order.service.OrderService;
import com.investment.order.service.PipelineOrderExecutor;
import com.investment.setting.service.SystemSettingService;
import com.investment.strategy.domain.StrategyType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 4단계 파이프라인 실행 — 권장 포지션 주문 실행.
 * 실제 주문 여부는 호출부에서 전달하는 autoExecute(DB 시스템 설정·계정별 pipelineAutoExecute 반영값)에 따름.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineExecutor {

    private static final BigDecimal DEFAULT_ATR_MULTIPLIER = new BigDecimal("2.0");
    private static final int DEFAULT_TIME_CUT_DAYS = 5;
    private static final BigDecimal DEFAULT_TARGET_RETURN_PCT = new BigDecimal("3.0");

    private final PositionSizingService positionSizingService;
    private final OrderService orderService;
    @Autowired(required = false)
    private PipelineOrderExecutor pipelineOrderExecutor;
    private final StrategyPositionRepository strategyPositionRepository;
    private final OrderRepository orderRepository;
    private final TradingSettingRepository tradingSettingRepository;
    private final UserAccountRepository userAccountRepository;
    private final EncryptionUtil encryptionUtil;
    private final AuditLogService auditLogService;
    private final SystemSettingService systemSettingService;
    private final TradingWindowService tradingWindowService;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** 체결 확인 후 포지션 등록 여부 (true면 체결 확인 후, false면 주문 성공 시 즉시 등록) */
    @Value("${investment.pipeline.register-position-on-execution:false}")
    private boolean registerPositionOnExecution = false;

    /** KR 시초가/변동성 돌파 시 주문구분(ORD_DVSN). 02=최유리, 03=IOC. 빈값이면 지정가(00). KR+SHORT_TERM일 때만 적용 */
    @Value("${investment.pipeline.kr-opening-order-dvsn:}")
    private String krOpeningOrderDvsn = "";

    /** true 시 PipelineOrderExecutor(TWAP/VWAP 등) 사용, false 시 OrderService 단일 주문 직접 호출 */
    @Value("${investment.pipeline.use-algo-execution:false}")
    private boolean useAlgoExecution = false;

    /**
     * 계좌의 서버 타입 조회 (모의=1, 실전=0). userId·accountNo에 해당하는 UserAccount 기준.
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
     * 기준일·시장에 대해 권장 포지션 산출 후, 설정에 따라 주문 실행 (기본 SHORT_TERM).
     */
    @Transactional
    public PipelineRunResult run(LocalDate basDt, String market, String accountNo, BigDecimal totalCapital,
            boolean autoExecute) {
        return run(basDt, market, accountNo, StrategyType.SHORT_TERM, totalCapital, autoExecute, null);
    }

    /**
     * 기준일·시장·기간별 권장 포지션 산출 후, 설정에 따라 주문 실행.
     *
     * @param basDt            기준일
     * @param market           시장 (KR, US)
     * @param accountNo        계좌번호
     * @param strategyType     기간 (SHORT_TERM, MEDIUM_TERM, LONG_TERM)
     * @param allocatedCapital 해당 기간 배분 자산 (원)
     * @param autoExecute      true면 주문 실행(DB·계정별 pipelineAutoExecute 반영값), false면 권장 목록만 반환
     * @return 실행(또는 권장만) 결과 요약
     */
    @Transactional
    public PipelineRunResult run(LocalDate basDt, String market, String accountNo, StrategyType strategyType,
            BigDecimal allocatedCapital, boolean autoExecute) {
        return run(basDt, market, accountNo, strategyType, allocatedCapital, autoExecute, null);
    }

    /**
     * 기준일·시장·기간별 권장 포지션 산출 후, 설정에 따라 주문 실행. correlationId가 있으면 구조화 로그에 포함.
     *
     * @param correlationId    실행 추적용 ID (null 가능)
     */
    @Transactional
    public PipelineRunResult run(LocalDate basDt, String market, String accountNo, StrategyType strategyType,
            BigDecimal allocatedCapital, boolean autoExecute, String correlationId) {
        if (correlationId != null) {
            log.info("pipelineRunStart basDt={} market={} strategyType={} accountNo={} correlationId={}",
                    basDt, market, strategyType, LogMaskingUtil.maskAccountNo(accountNo), correlationId);
        } else {
            log.debug("파이프라인 실행 시작: market={}, strategyType={}, accountNo={}", market, strategyType, LogMaskingUtil.maskAccountNo(accountNo));
        }
        boolean serverAllowRealExecution = systemSettingService.getBoolean("pipeline.allowRealExecution");
        List<PositionRecommendationDto> recommendations = positionSizingService.getRecommendations(
                basDt, market, strategyType, allocatedCapital, accountNo);
        List<PipelineRunResult.OrderResult> orderResults = new ArrayList<>();
        boolean actuallyExecute = autoExecute;
        String userId = tradingSettingRepository.findByAccountNo(accountNo)
                .map(com.investment.domain.entity.TradingSetting::getUserId)
                .orElse(null);
        String strategyTypeStr = strategyType != null ? strategyType.name() : StrategyType.SHORT_TERM.name();

        for (PositionRecommendationDto rec : recommendations) {
            if (rec.getRecommendedQty() <= 0)
                continue;
            String orderDvsn = null;
            if ("KR".equalsIgnoreCase(market) && strategyType == StrategyType.SHORT_TERM
                    && krOpeningOrderDvsn != null && !krOpeningOrderDvsn.isBlank()) {
                orderDvsn = krOpeningOrderDvsn;
            }
            OrderRequestDto request = OrderRequestDto.builder()
                    .accountNo(accountNo)
                    .symbol(rec.getSymbol())
                    .orderType(OrderRequestDto.OrderType.BUY)
                    .quantity((int) rec.getRecommendedQty())
                    .price(rec.getEntryPrice())
                    .market(rec.getMarket() != null ? rec.getMarket() : market)
                    .orderDvsn(orderDvsn)
                    .signalType(rec.getMethod())
                    .build();
            if (actuallyExecute) {
                LocalTime nowKst = ZonedDateTime.now(KST).toLocalTime();
                if (tradingWindowService.isVolatilePeriod(market, nowKst)) {
                    log.info("변동성 구간으로 신규 매수 지연: market={}, symbol={}", market, rec.getSymbol());
                    auditLogService.logTradeDecision(userId, accountNo, "SKIP", rec.getSymbol(),
                            rec.getMarket() != null ? rec.getMarket() : market, strategyTypeStr,
                            rec.getMethod(), null, null, AuditLogService.RESULT_SUCCESS, "변동성 구간");
                    orderResults.add(PipelineRunResult.OrderResult.dryRun(rec.getSymbol(), rec.getRecommendedQty(),
                            rec.getEntryPrice()));
                    continue;
                }
                try {
                    // 스케줄러 등 인증 컨텍스트 없음: accountNo → userId 조회 후 파이프라인용 주문 실행
                    com.investment.domain.entity.TradingSetting setting = tradingSettingRepository
                            .findByAccountNo(accountNo)
                            .orElseThrow(() -> new DomainException(ErrorCode.SETTING_NOT_FOUND,
                                    "거래 설정을 찾을 수 없습니다: " + accountNo));
                    String pipelineUserId = setting.getUserId();
                    boolean effectiveAllowReal = setting.getPipelineAllowRealExecution() != null
                            ? setting.getPipelineAllowRealExecution()
                            : serverAllowRealExecution;
                    // 실전 계좌(serverType=0)는 allow-real-execution=false 시 주문 스킵
                    String serverType = resolveServerTypeForAccount(pipelineUserId, accountNo);
                    if ("0".equals(serverType) && !effectiveAllowReal) {
                        log.warn("실전 계좌 자동 실행 미허용(allow-real-execution=false), 주문 스킵: accountNo={}, symbol={}",
                                accountNo, rec.getSymbol());
                        auditLogService.record(AuditLogService.EVENT_REAL_ACCOUNT_GUARD_BLOCKED, pipelineUserId, accountNo,
                                "실전 계좌 자동 실행 미허용으로 주문 스킵 symbol=" + rec.getSymbol(),
                                AuditLogService.RESULT_SUCCESS, null);
                        auditLogService.logTradeDecision(pipelineUserId, accountNo, "SKIP", rec.getSymbol(),
                                rec.getMarket() != null ? rec.getMarket() : market, strategyTypeStr,
                                rec.getMethod(), null, "실계좌 미허용", AuditLogService.RESULT_SUCCESS, "실전 계좌 자동 실행 미허용");
                        orderResults.add(PipelineRunResult.OrderResult.dryRun(rec.getSymbol(), rec.getRecommendedQty(),
                                rec.getEntryPrice()));
                        continue;
                    }
                    OrderResponseDto orderResponse = (useAlgoExecution && pipelineOrderExecutor != null)
                            ? pipelineOrderExecutor.executeOrderForPipeline(request, pipelineUserId)
                            : orderService.executeOrderForPipeline(request, pipelineUserId);

                    // 포지션 등록: 체결 확인 후 등록 옵션에 따라 분기
                    if (registerPositionOnExecution) {
                        // 체결 확인 후 등록: 주문에 포지션 컨텍스트 저장, FillConfirmationScheduler에서 포지션 등록
                        orderRepository.findById(orderResponse.getOrderId()).ifPresent(order -> {
                            order.setPositionContext(basDt, market,
                                    strategyType != null ? strategyType.name() : StrategyType.SHORT_TERM.name());
                            orderRepository.save(order);
                        });
                        auditLogService.logTradeDecision(pipelineUserId, accountNo, "BUY", rec.getSymbol(),
                                rec.getMarket() != null ? rec.getMarket() : market, strategyTypeStr,
                                rec.getMethod(), null, null, AuditLogService.RESULT_SUCCESS, null);
                        log.debug("파이프라인 주문 성공 (체결 확인 후 포지션 등록 대기): orderId={}, symbol={}, qty={}, price={}",
                                orderResponse.getOrderId(), rec.getSymbol(), rec.getRecommendedQty(),
                                rec.getEntryPrice());
                        orderResults.add(PipelineRunResult.OrderResult.success(rec.getSymbol(), rec.getRecommendedQty(),
                                rec.getEntryPrice()));
                    } else {
                        // 주문 성공 시 즉시 등록 (기존 로직). Time-Cut은 SHORT_TERM 전용.
                        StrategyType st = strategyType != null ? strategyType : StrategyType.SHORT_TERM;
                        int timeCutDays = (st == StrategyType.SHORT_TERM) ? DEFAULT_TIME_CUT_DAYS : 0;
                        BigDecimal targetReturnPct = (st == StrategyType.SHORT_TERM) ? DEFAULT_TARGET_RETURN_PCT : null;
                        StrategyPosition position = StrategyPosition.builder()
                                .accountNo(accountNo)
                                .symbol(rec.getSymbol())
                                .market(rec.getMarket())
                                .strategyType(st)
                                .entryDt(basDt)
                                .entryPrice(rec.getEntryPrice())
                                .quantity((int) rec.getRecommendedQty())
                                .trailingHigh(rec.getEntryPrice())
                                .atrMultiplier(DEFAULT_ATR_MULTIPLIER)
                                .timeCutDays(timeCutDays)
                                .targetReturnPct(targetReturnPct)
                                .signalType(rec.getMethod())
                                .build();
                        strategyPositionRepository.save(position);
                        auditLogService.logTradeDecision(pipelineUserId, accountNo, "BUY", rec.getSymbol(),
                                rec.getMarket() != null ? rec.getMarket() : market, strategyTypeStr,
                                rec.getMethod(), null, null, AuditLogService.RESULT_SUCCESS, null);
                        log.debug("파이프라인 주문 성공 (포지션 즉시 등록): symbol={}, qty={}, price={}",
                                rec.getSymbol(), rec.getRecommendedQty(), rec.getEntryPrice());
                        orderResults.add(PipelineRunResult.OrderResult.success(rec.getSymbol(), rec.getRecommendedQty(),
                                rec.getEntryPrice()));
                    }
                } catch (Exception e) {
                    log.warn("파이프라인 주문 실패: symbol={}, error={}", rec.getSymbol(), e.getMessage());
                    auditLogService.logTradeDecision(userId, accountNo, "BUY", rec.getSymbol(),
                            rec.getMarket() != null ? rec.getMarket() : market, strategyTypeStr,
                            rec.getMethod(), null, null, AuditLogService.RESULT_FAILURE, e.getMessage());
                    orderResults.add(PipelineRunResult.OrderResult.failure(rec.getSymbol(), e.getMessage()));
                }
            } else {
                auditLogService.logTradeDecision(userId, accountNo, "DRY_RUN", rec.getSymbol(),
                        rec.getMarket() != null ? rec.getMarket() : market, strategyTypeStr,
                        rec.getMethod(), null, null, AuditLogService.RESULT_SUCCESS, "권장만(주문 미실행)");
                log.debug("파이프라인 권장만(주문 미실행): symbol={}, qty={}, price={}", rec.getSymbol(), rec.getRecommendedQty(),
                        rec.getEntryPrice());
                orderResults.add(PipelineRunResult.OrderResult.dryRun(rec.getSymbol(), rec.getRecommendedQty(),
                        rec.getEntryPrice()));
            }
        }
        PipelineRunResult result = PipelineRunResult.builder()
                .basDt(basDt)
                .market(market)
                .dryRun(!actuallyExecute)
                .recommendationCount(recommendations.size())
                .orderResults(orderResults)
                .build();
        if (correlationId != null) {
            log.info("pipelineRunEnd correlationId={} market={} strategyType={} recommendationCount={} orderCount={}",
                    correlationId, market, strategyType, result.getRecommendationCount(), result.getOrderResults().size());
        } else {
            log.debug("파이프라인 실행 완료: market={}, strategyType={}, accountNo={}", market, strategyType, LogMaskingUtil.maskAccountNo(accountNo));
        }
        return result;
    }

    /**
     * 체결 확인 후 포지션 등록 (체결 확인 스케줄러/리스너에서 호출).
     * 주문이 EXECUTED 상태이고 아직 포지션이 등록되지 않은 경우 포지션을 등록합니다.
     *
     * @param orderId 주문 ID
     * @param basDt   기준일
     * @param market  시장
     * @return 포지션 등록 성공 여부
     */
    @Transactional
    public boolean registerPositionOnExecution(String orderId, LocalDate basDt, String market) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("체결 확인 후 포지션 등록 실패: 주문 없음, orderId={}", orderId);
            return false;
        }

        // 매수 주문만 처리
        if (order.getOrderType() != Order.OrderType.BUY) {
            return false;
        }

        // 체결 완료 상태 확인
        if (order.getStatus() != Order.OrderStatus.EXECUTED) {
            log.debug("체결 확인 후 포지션 등록 스킵: 주문 미체결, orderId={}, status={}", orderId, order.getStatus());
            return false;
        }

        String posMarket = market != null ? market
                : (order.getPositionMarket() != null ? order.getPositionMarket() : "KR");
        // 이미 포지션이 등록되어 있는지 확인
        List<StrategyPosition> existing = strategyPositionRepository.findByAccountNoAndSymbolAndMarketAndExitDtIsNull(
                order.getAccountNo(), order.getSymbol(), posMarket);
        if (!existing.isEmpty()) {
            log.debug("체결 확인 후 포지션 등록 스킵: 이미 등록됨, orderId={}, symbol={}", orderId, order.getSymbol());
            return false;
        }

        // 실제 체결가·수량 사용
        BigDecimal entryPrice = order.getExecutedPrice() != null ? order.getExecutedPrice() : order.getPrice();
        int quantity = order.getExecutedQuantity() != null ? order.getExecutedQuantity() : order.getQuantity();
        LocalDate posBasDt = basDt != null ? basDt
                : (order.getPositionBasDt() != null ? order.getPositionBasDt()
                        : order.getExecutedTime() != null ? order.getExecutedTime().toLocalDate()
                                : order.getOrderTime().toLocalDate());
        StrategyType posStrategyType = parseStrategyType(order.getPositionStrategyType());
        // Time-Cut은 SHORT_TERM 전용
        int timeCutDays = (posStrategyType == StrategyType.SHORT_TERM) ? DEFAULT_TIME_CUT_DAYS : 0;
        BigDecimal targetReturnPct = (posStrategyType == StrategyType.SHORT_TERM) ? DEFAULT_TARGET_RETURN_PCT : null;

        StrategyPosition position = StrategyPosition.builder()
                .accountNo(order.getAccountNo())
                .symbol(order.getSymbol())
                .market(posMarket)
                .strategyType(posStrategyType)
                .entryDt(posBasDt)
                .entryPrice(entryPrice)
                .quantity(quantity)
                .trailingHigh(entryPrice)
                .atrMultiplier(DEFAULT_ATR_MULTIPLIER)
                .timeCutDays(timeCutDays)
                .targetReturnPct(targetReturnPct)
                .signalType(order.getSignalType())
                .build();
        strategyPositionRepository.save(position);

        log.info("체결 확인 후 포지션 등록 완료: orderId={}, symbol={}, qty={}, price={}",
                orderId, order.getSymbol(), quantity, entryPrice);
        return true;
    }

    private static StrategyType parseStrategyType(String value) {
        if (value == null || value.isBlank()) {
            return StrategyType.SHORT_TERM;
        }
        try {
            return StrategyType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return StrategyType.SHORT_TERM;
        }
    }

    @lombok.Getter
    @lombok.Builder
    @lombok.AllArgsConstructor
    public static class PipelineRunResult {
        private LocalDate basDt;
        private String market;
        private boolean dryRun;
        private int recommendationCount;
        private List<OrderResult> orderResults;

        @lombok.Getter
        @lombok.AllArgsConstructor
        public static class OrderResult {
            private String symbol;
            private long qty;
            private BigDecimal price;
            private boolean success;
            private String errorMessage;

            static OrderResult success(String symbol, long qty, BigDecimal price) {
                return new OrderResult(symbol, qty, price, true, null);
            }

            static OrderResult failure(String symbol, String errorMessage) {
                return new OrderResult(symbol, 0, null, false, errorMessage);
            }

            static OrderResult dryRun(String symbol, long qty, BigDecimal price) {
                return new OrderResult(symbol, qty, price, true, null);
            }
        }
    }
}
