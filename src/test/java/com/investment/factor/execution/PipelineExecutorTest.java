package com.investment.factor.execution;

import com.investment.domain.entity.BrokerType;
import com.investment.domain.entity.Order;
import com.investment.domain.entity.StrategyPosition;
import com.investment.domain.entity.TradingSetting;
import com.investment.domain.entity.UserAccount;
import com.investment.domain.repository.OrderRepository;
import com.investment.domain.repository.StrategyPositionRepository;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.domain.repository.UserAccountRepository;
import com.investment.common.security.EncryptionUtil;
import com.investment.factor.dto.PositionRecommendationDto;
import com.investment.factor.service.PositionSizingService;
import com.investment.order.dto.OrderRequestDto;
import com.investment.order.dto.OrderResponseDto;
import com.investment.order.service.OrderService;
import com.investment.strategy.domain.StrategyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PipelineExecutor")
class PipelineExecutorTest {

    @Mock
    private PositionSizingService positionSizingService;
    @Mock
    private OrderService orderService;
    @Mock
    private StrategyPositionRepository strategyPositionRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private TradingSettingRepository tradingSettingRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private EncryptionUtil encryptionUtil;

    @InjectMocks
    private PipelineExecutor pipelineExecutor;

    @Captor
    private ArgumentCaptor<OrderRequestDto> orderRequestCaptor;
    @Captor
    private ArgumentCaptor<String> userIdCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(pipelineExecutor, "autoExecute", false);
        ReflectionTestUtils.setField(pipelineExecutor, "allowRealExecution", false);
        ReflectionTestUtils.setField(pipelineExecutor, "registerPositionOnExecution", false);
    }

    @Test
    @DisplayName("dry-run 모드 - 주문 실행 없이 권장 목록만 반환")
    void run_dryRun_returnsRecommendationsWithoutOrder() {
        // given
        LocalDate basDt = LocalDate.of(2026, 1, 30);
        String market = "KR";
        String accountNo = "1234567890";
        BigDecimal totalCapital = new BigDecimal("100000000");

        PositionRecommendationDto recommendation = PositionRecommendationDto.builder()
                .basDt(basDt)
                .symbol("005930")
                .market(market)
                .recommendedAmt(new BigDecimal("10000000"))
                .recommendedQty(100)
                .entryPrice(new BigDecimal("100000"))
                .stopLoss(new BigDecimal("95000"))
                .method("ATR")
                .build();

        when(positionSizingService.getRecommendations(eq(basDt), eq(market), eq(StrategyType.SHORT_TERM), eq(totalCapital)))
                .thenReturn(List.of(recommendation));

        // when
        PipelineExecutor.PipelineRunResult result = pipelineExecutor.run(
                basDt, market, accountNo, totalCapital, true);

        // then
        assertThat(result.isDryRun()).isTrue();
        assertThat(result.getRecommendationCount()).isEqualTo(1);
        assertThat(result.getOrderResults()).hasSize(1);
        assertThat(result.getOrderResults().get(0).getSymbol()).isEqualTo("005930");
        verify(orderService, never()).executeOrderForPipeline(any(), any());
        verify(strategyPositionRepository, never()).save(any());
    }

    @Test
    @DisplayName("auto-execute=true, registerPositionOnExecution=false - 스케줄러 경로 executeOrderForPipeline·userId 사용")
    void run_autoExecute_immediatePositionRegistration() {
        // given: 스케줄러에서 호출 시 인증 컨텍스트 없음 → accountNo로 userId 조회 후 executeOrderForPipeline 호출
        ReflectionTestUtils.setField(pipelineExecutor, "autoExecute", true);
        LocalDate basDt = LocalDate.of(2026, 1, 30);
        String market = "KR";
        String accountNo = "1234567890";
        String userId = "user-pipeline-1";
        BigDecimal totalCapital = new BigDecimal("100000000");

        TradingSetting setting = TradingSetting.builder()
                .accountNo(accountNo)
                .userId(userId)
                .maxInvestmentAmount(new BigDecimal("50000000"))
                .minInvestmentAmount(new BigDecimal("10000"))
                .defaultCurrency("KRW")
                .build();
        when(tradingSettingRepository.findByAccountNo(accountNo)).thenReturn(Optional.of(setting));

        // resolveServerTypeForAccount: 모의계좌(1) 반환 → 주문 실행 허용
        UserAccount userAccount = UserAccount.builder()
                .userId(userId)
                .userApiKeyId("key-1")
                .accountNoEncrypted("enc-1234567890")
                .brokerType(BrokerType.KOREA_INVESTMENT)
                .serverType("1")
                .accountName("테스트계좌")
                .isDefault(true)
                .isActive(true)
                .build();
        when(userAccountRepository.findByUserIdAndBrokerType(eq(userId), eq(BrokerType.KOREA_INVESTMENT)))
                .thenReturn(List.of(userAccount));
        when(encryptionUtil.decrypt("enc-1234567890")).thenReturn("1234567890");

        PositionRecommendationDto recommendation = PositionRecommendationDto.builder()
                .basDt(basDt)
                .symbol("005930")
                .market(market)
                .recommendedAmt(new BigDecimal("10000000"))
                .recommendedQty(100)
                .entryPrice(new BigDecimal("100000"))
                .stopLoss(new BigDecimal("95000"))
                .method("ATR")
                .build();

        when(positionSizingService.getRecommendations(eq(basDt), eq(market), eq(StrategyType.SHORT_TERM), eq(totalCapital)))
                .thenReturn(List.of(recommendation));

        OrderResponseDto orderResponse = OrderResponseDto.builder()
                .orderId("order-123")
                .accountNo(accountNo)
                .symbol("005930")
                .orderType(OrderRequestDto.OrderType.BUY)
                .quantity(100)
                .price(new BigDecimal("100000"))
                .status(OrderResponseDto.OrderStatus.PENDING)
                .orderTime(LocalDateTime.now())
                .build();

        when(orderService.executeOrderForPipeline(any(OrderRequestDto.class), eq(userId)))
                .thenReturn(orderResponse);

        // when
        PipelineExecutor.PipelineRunResult result = pipelineExecutor.run(
                basDt, market, accountNo, totalCapital, false);

        // then
        assertThat(result.isDryRun()).isFalse();
        verify(orderService).executeOrderForPipeline(orderRequestCaptor.capture(), userIdCaptor.capture());
        assertThat(orderRequestCaptor.getValue().getSymbol()).isEqualTo("005930");
        assertThat(userIdCaptor.getValue()).isEqualTo(userId);
        verify(strategyPositionRepository).save(any(StrategyPosition.class)); // 즉시 포지션 등록
    }

    @Test
    @DisplayName("체결 확인 후 포지션 등록 - registerPositionOnExecution=true")
    void registerPositionOnExecution_executedOrder_registersPosition() {
        // given
        String orderId = "order-123";
        LocalDate basDt = LocalDate.of(2026, 1, 30);
        String market = "KR";

        Order order = Order.builder()
                .accountNo("1234567890")
                .symbol("005930")
                .orderType(Order.OrderType.BUY)
                .quantity(100)
                .price(new BigDecimal("100000"))
                .status(Order.OrderStatus.EXECUTED)
                .build();
        // id는 @PrePersist에서 생성되므로 리플렉션으로 설정
        try {
            java.lang.reflect.Field idField = Order.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(order, orderId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        order.execute(100, new BigDecimal("99500")); // 실제 체결가

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(strategyPositionRepository.findByAccountNoAndSymbolAndMarketAndExitDtIsNull(
                eq("1234567890"), eq("005930"), eq(market)))
                .thenReturn(List.of()); // 기존 포지션 없음

        // when
        boolean registered = pipelineExecutor.registerPositionOnExecution(orderId, basDt, market);

        // then
        assertThat(registered).isTrue();
        verify(strategyPositionRepository).save(argThat(pos ->
                pos.getSymbol().equals("005930") &&
                pos.getEntryPrice().compareTo(new BigDecimal("99500")) == 0 && // 실제 체결가 사용
                pos.getQuantity() == 100
        ));
    }

    @Test
    @DisplayName("allow-real-execution=false, 실전 계좌(serverType=0) - 주문 스킵, dry-run 결과")
    void run_allowRealExecutionFalse_realAccount_skipsOrder() {
        ReflectionTestUtils.setField(pipelineExecutor, "autoExecute", true);
        ReflectionTestUtils.setField(pipelineExecutor, "allowRealExecution", false);

        LocalDate basDt = LocalDate.of(2026, 1, 30);
        String market = "KR";
        String accountNo = "1234567890";
        String userId = "user-real-1";
        BigDecimal totalCapital = new BigDecimal("100000000");

        TradingSetting setting = TradingSetting.builder()
                .accountNo(accountNo)
                .userId(userId)
                .maxInvestmentAmount(new BigDecimal("50000000"))
                .minInvestmentAmount(new BigDecimal("10000"))
                .defaultCurrency("KRW")
                .build();
        when(tradingSettingRepository.findByAccountNo(accountNo)).thenReturn(Optional.of(setting));

        // 실전 계좌(serverType=0) 반환 → allow-real-execution=false 이므로 주문 스킵
        UserAccount userAccount = UserAccount.builder()
                .userId(userId)
                .userApiKeyId("key-1")
                .accountNoEncrypted("enc-real")
                .brokerType(BrokerType.KOREA_INVESTMENT)
                .serverType("0")
                .accountName("실전계좌")
                .isDefault(true)
                .isActive(true)
                .build();
        when(userAccountRepository.findByUserIdAndBrokerType(eq(userId), eq(BrokerType.KOREA_INVESTMENT)))
                .thenReturn(List.of(userAccount));
        when(encryptionUtil.decrypt("enc-real")).thenReturn("1234567890");

        PositionRecommendationDto recommendation = PositionRecommendationDto.builder()
                .basDt(basDt)
                .symbol("005930")
                .market(market)
                .recommendedAmt(new BigDecimal("10000000"))
                .recommendedQty(100)
                .entryPrice(new BigDecimal("100000"))
                .stopLoss(new BigDecimal("95000"))
                .method("ATR")
                .build();
        when(positionSizingService.getRecommendations(eq(basDt), eq(market), eq(StrategyType.SHORT_TERM), eq(totalCapital)))
                .thenReturn(List.of(recommendation));

        PipelineExecutor.PipelineRunResult result = pipelineExecutor.run(
                basDt, market, accountNo, totalCapital, false);

        assertThat(result.isDryRun()).isFalse();
        assertThat(result.getOrderResults()).hasSize(1);
        // 실전 계좌라 주문 스킵 → dryRun 결과로 추가됨
        assertThat(result.getOrderResults().get(0).getSymbol()).isEqualTo("005930");
        verify(orderService, never()).executeOrderForPipeline(any(), any());
        verify(strategyPositionRepository, never()).save(any());
    }

    @Test
    @DisplayName("체결 확인 후 포지션 등록 - 주문 미체결 시 등록 안 함")
    void registerPositionOnExecution_pendingOrder_noRegistration() {
        // given
        String orderId = "order-123";
        Order order = Order.builder()
                .accountNo("1234567890")
                .symbol("005930")
                .orderType(Order.OrderType.BUY)
                .quantity(100)
                .price(new BigDecimal("100000"))
                .status(Order.OrderStatus.PENDING) // 미체결
                .build();
        // id는 @PrePersist에서 생성되므로 리플렉션으로 설정
        try {
            java.lang.reflect.Field idField = Order.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(order, orderId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // when
        boolean registered = pipelineExecutor.registerPositionOnExecution(orderId, LocalDate.now(), "KR");

        // then
        assertThat(registered).isFalse();
        verify(strategyPositionRepository, never()).save(any());
    }
}
