package com.investment.order.service;

import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.core.engine.risk.ComplianceEngine;
import com.investment.core.engine.risk.ComplianceResult;
import com.investment.domain.entity.Order;
import com.investment.domain.entity.TradingSetting;
import com.investment.domain.repository.OrderRepository;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.order.TradeExplanationService;
import com.investment.order.client.KoreaInvestmentOrderClient;
import com.investment.order.dto.OrderRequestDto;
import com.investment.order.dto.OrderResponseDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;

import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

        @Mock
        private OrderRepository orderRepository;

        @Mock
        private TradingSettingRepository tradingSettingRepository;

        @Mock
        private KoreaInvestmentOrderClient orderClient;

        @Mock
        private ComplianceEngine complianceEngine;

        @Mock
        private TradeExplanationService tradeExplanationService;

        @InjectMocks
        private OrderService orderService;

        private TradingSetting tradingSetting;
        private OrderRequestDto orderRequest;
        private static final String TEST_USER_ID = "test-user-id";

        @BeforeEach
        void setUp() {
                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(TEST_USER_ID, null));

                tradingSetting = TradingSetting.builder()
                                .accountNo("1234567890")
                                .maxInvestmentAmount(new BigDecimal("1000000"))
                                .minInvestmentAmount(new BigDecimal("10000"))
                                .defaultCurrency("USD")
                                .autoTradingEnabled(false)
                                .build();

                orderRequest = OrderRequestDto.builder()
                                .accountNo("1234567890")
                                .symbol("005930") // 삼성전자
                                .orderType(OrderRequestDto.OrderType.BUY)
                                .quantity(10)
                                .price(new BigDecimal("70000.00"))
                                .build();

                lenient().when(tradeExplanationService.buildExplanation(anyString(), anyInt(), any(), any(), any()))
                                .thenReturn("테스트 설명");
        }

        @AfterEach
        void tearDown() {
                SecurityContextHolder.clearContext();
        }

        @Test
        void 주문_실행_성공() {
                // given: 컴플라이언스 승인, 한국투자증권 API 성공 응답
                when(complianceEngine.preTradeCheck(any(), eq(TEST_USER_ID))).thenReturn(ComplianceResult.approve());
                when(tradingSettingRepository.findByAccountNo("1234567890"))
                                .thenReturn(Optional.of(tradingSetting));
                when(orderClient.placeBuyOrder(eq(TEST_USER_ID), anyString(), anyString(), anyInt(), any(),
                                anyString()))
                                .thenReturn(Mono.just(KoreaInvestmentOrderClient.OrderResponse.builder()
                                                .orderNo("ORD123")
                                                .status("SUCCESS")
                                                .build()));

                Order savedOrder = Order.builder()
                                .accountNo(orderRequest.getAccountNo())
                                .symbol(orderRequest.getSymbol())
                                .orderType(Order.OrderType.BUY)
                                .quantity(orderRequest.getQuantity())
                                .price(orderRequest.getPrice())
                                .status(Order.OrderStatus.PENDING)
                                .build();
                try {
                        java.lang.reflect.Field idField = Order.class.getDeclaredField("id");
                        idField.setAccessible(true);
                        idField.set(savedOrder, java.util.UUID.randomUUID().toString());
                } catch (Exception e) {
                        throw new RuntimeException(e);
                }
                when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

                // when
                OrderResponseDto response = orderService.executeOrder(orderRequest);

                // then
                assertNotNull(response);
                assertEquals(OrderRequestDto.OrderType.BUY, response.getOrderType());
                verify(orderRepository, times(1)).save(any(Order.class));
        }

        @Test
        void 주문_실행_최대투자금액_초과() {
                // given: 금액 2,000,000원 초과 (10 * 200,000)
                OrderRequestDto overRequest = OrderRequestDto.builder()
                                .accountNo("1234567890")
                                .symbol("005930")
                                .orderType(OrderRequestDto.OrderType.BUY)
                                .quantity(10)
                                .price(new BigDecimal("200000.00"))
                                .build();
                when(complianceEngine.preTradeCheck(any(), eq(TEST_USER_ID))).thenReturn(ComplianceResult.approve());
                when(tradingSettingRepository.findByAccountNo("1234567890"))
                                .thenReturn(Optional.of(tradingSetting));

                // when & then
                DomainException exception = assertThrows(DomainException.class,
                                () -> orderService.executeOrder(overRequest));

                assertEquals(ErrorCode.EXCEEDS_MAX_INVESTMENT, exception.getErrorCode());
                verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        void 주문_실행_최소투자금액_미만() {
                // given: 금액 5,000원 미만
                OrderRequestDto underRequest = OrderRequestDto.builder()
                                .accountNo("1234567890")
                                .symbol("005930")
                                .orderType(OrderRequestDto.OrderType.BUY)
                                .quantity(1)
                                .price(new BigDecimal("500.00"))
                                .build();
                when(complianceEngine.preTradeCheck(any(), eq(TEST_USER_ID))).thenReturn(ComplianceResult.approve());
                when(tradingSettingRepository.findByAccountNo("1234567890"))
                                .thenReturn(Optional.of(tradingSetting));

                // when & then
                DomainException exception = assertThrows(DomainException.class,
                                () -> orderService.executeOrder(underRequest));

                assertEquals(ErrorCode.INVALID_ORDER_AMOUNT, exception.getErrorCode());
                verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        void 주문_조회_성공() {
                // given
                Order order = Order.builder()
                                .accountNo("1234567890")
                                .symbol("005930") // 삼성전자
                                .orderType(Order.OrderType.BUY)
                                .quantity(10)
                                .price(new BigDecimal("70000.00"))
                                .status(Order.OrderStatus.EXECUTED)
                                .build();

                String orderId = java.util.UUID.randomUUID().toString();

                // Reflection을 사용하여 id 설정
                try {
                        java.lang.reflect.Field idField = Order.class.getDeclaredField("id");
                        idField.setAccessible(true);
                        idField.set(order, orderId);
                } catch (Exception e) {
                        throw new RuntimeException(e);
                }

                when(orderRepository.findByIdAndAccountNo(orderId, "1234567890"))
                                .thenReturn(Optional.of(order));

                // when
                OrderResponseDto response = orderService.getOrder(orderId, "1234567890");

                // then
                assertNotNull(response);
                assertEquals(orderId, response.getOrderId());
                assertEquals("005930", response.getSymbol());
        }

        @Test
        void 주문_조회_실패_없는_주문() {
                // given
                String orderId = java.util.UUID.randomUUID().toString();
                when(orderRepository.findByIdAndAccountNo(orderId, "1234567890"))
                                .thenReturn(Optional.empty());

                // when & then
                DomainException exception = assertThrows(DomainException.class,
                                () -> orderService.getOrder(orderId, "1234567890"));

                assertEquals(ErrorCode.ORDER_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        void executeOrderForPipeline_marketKR_callsDomesticOrder() {
                // given: market KR (or null) → domestic placeBuyOrder
                when(complianceEngine.preTradeCheck(any(), eq(TEST_USER_ID))).thenReturn(ComplianceResult.approve());
                when(tradingSettingRepository.findByAccountNo("1234567890"))
                                .thenReturn(Optional.of(tradingSetting));
                when(orderClient.placeBuyOrder(eq(TEST_USER_ID), anyString(), eq("005930"), anyInt(), any(),
                                anyString()))
                                .thenReturn(Mono.just(KoreaInvestmentOrderClient.OrderResponse.builder()
                                                .orderNo("ORD-KR-1")
                                                .status("SUCCESS")
                                                .build()));
                Order savedOrder = Order.builder()
                                .accountNo(orderRequest.getAccountNo())
                                .symbol(orderRequest.getSymbol())
                                .orderType(Order.OrderType.BUY)
                                .quantity(orderRequest.getQuantity())
                                .price(orderRequest.getPrice())
                                .status(Order.OrderStatus.PENDING)
                                .build();
                try {
                        java.lang.reflect.Field idField = Order.class.getDeclaredField("id");
                        idField.setAccessible(true);
                        idField.set(savedOrder, java.util.UUID.randomUUID().toString());
                } catch (Exception e) {
                        throw new RuntimeException(e);
                }
                when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

                // when
                orderService.executeOrderForPipeline(orderRequest, TEST_USER_ID);

                // then
                verify(orderClient).placeBuyOrder(eq(TEST_USER_ID), eq("1234567890"), eq("005930"), eq(10), any(),
                                anyString());
                verify(orderClient, never()).placeOverseasBuyOrder(any(), any(), any(), anyInt(), any(), any());
        }

        @Test
        void executeOrderForPipeline_marketUS_callsOverseasOrder() {
                // given: market US → overseas placeOverseasBuyOrder (금액이 최소/최대 투자금액 내여야 함)
                OrderRequestDto usRequest = OrderRequestDto.builder()
                                .accountNo("1234567890")
                                .symbol("AAPL")
                                .orderType(OrderRequestDto.OrderType.BUY)
                                .quantity(100)
                                .price(new BigDecimal("150.00"))
                                .market("US")
                                .build();
                when(complianceEngine.preTradeCheck(any(), eq(TEST_USER_ID))).thenReturn(ComplianceResult.approve());
                when(tradingSettingRepository.findByAccountNo("1234567890"))
                                .thenReturn(Optional.of(tradingSetting));
                when(orderClient.placeOverseasBuyOrder(eq(TEST_USER_ID), anyString(), eq("AAPL"), eq(100), any(),
                                anyString()))
                                .thenReturn(Mono.just(KoreaInvestmentOrderClient.OrderResponse.builder()
                                                .orderNo("ORD-US-1")
                                                .status("SUCCESS")
                                                .build()));
                Order savedOrder = Order.builder()
                                .accountNo(usRequest.getAccountNo())
                                .symbol(usRequest.getSymbol())
                                .orderType(Order.OrderType.BUY)
                                .quantity(100)
                                .price(usRequest.getPrice())
                                .status(Order.OrderStatus.PENDING)
                                .build();
                try {
                        java.lang.reflect.Field idField = Order.class.getDeclaredField("id");
                        idField.setAccessible(true);
                        idField.set(savedOrder, java.util.UUID.randomUUID().toString());
                } catch (Exception e) {
                        throw new RuntimeException(e);
                }
                when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

                // when
                orderService.executeOrderForPipeline(usRequest, TEST_USER_ID);

                // then
                verify(orderClient).placeOverseasBuyOrder(eq(TEST_USER_ID), eq("1234567890"), eq("AAPL"), eq(100),
                                any(), anyString());
                verify(orderClient, never()).placeBuyOrder(any(), any(), any(), anyInt(), any(), any());
        }

        @Test
        void executeOrderForPipeline_orderDvsn_set_passesToClient() {
                // given: KR 주문에 orderDvsn "02"(최유리) 설정 시 해당 값이 placeBuyOrder에 전달됨
                OrderRequestDto krRequestWithDvsn = OrderRequestDto.builder()
                                .accountNo("1234567890")
                                .symbol("005930")
                                .orderType(OrderRequestDto.OrderType.BUY)
                                .quantity(10)
                                .price(new BigDecimal("70000.00"))
                                .market("KR")
                                .orderDvsn("02")
                                .build();
                when(complianceEngine.preTradeCheck(any(), eq(TEST_USER_ID))).thenReturn(ComplianceResult.approve());
                when(tradingSettingRepository.findByAccountNo("1234567890"))
                                .thenReturn(Optional.of(tradingSetting));
                when(orderClient.placeBuyOrder(eq(TEST_USER_ID), anyString(), eq("005930"), eq(10), any(), eq("02")))
                                .thenReturn(Mono.just(KoreaInvestmentOrderClient.OrderResponse.builder()
                                                .orderNo("ORD-KR-2")
                                                .status("SUCCESS")
                                                .build()));
                Order savedOrder = Order.builder()
                                .accountNo(krRequestWithDvsn.getAccountNo())
                                .symbol(krRequestWithDvsn.getSymbol())
                                .orderType(Order.OrderType.BUY)
                                .quantity(krRequestWithDvsn.getQuantity())
                                .price(krRequestWithDvsn.getPrice())
                                .status(Order.OrderStatus.PENDING)
                                .build();
                try {
                        java.lang.reflect.Field idField = Order.class.getDeclaredField("id");
                        idField.setAccessible(true);
                        idField.set(savedOrder, java.util.UUID.randomUUID().toString());
                } catch (Exception e) {
                        throw new RuntimeException(e);
                }
                when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

                // when
                orderService.executeOrderForPipeline(krRequestWithDvsn, TEST_USER_ID);

                // then: orderDvsn "02"가 placeBuyOrder의 orderType 인자로 전달됨
                verify(orderClient).placeBuyOrder(eq(TEST_USER_ID), eq("1234567890"), eq("005930"), eq(10), any(),
                                eq("02"));
        }

        @Test
        void 주문_실행_컴플라이언스_거부() {
                when(complianceEngine.preTradeCheck(any(), eq(TEST_USER_ID)))
                                .thenReturn(ComplianceResult.reject("Kill Switch 활성화"));

                DomainException exception = assertThrows(DomainException.class,
                                () -> orderService.executeOrder(orderRequest));

                assertEquals(ErrorCode.ORDER_REJECTED, exception.getErrorCode());
                assertTrue(exception.getMessage().contains("Kill Switch"));
                verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        void getOrder_includesSignalTypeAndExitRuleType() {
                String orderId = java.util.UUID.randomUUID().toString();
                Order order = Order.builder()
                                .accountNo("1234567890")
                                .symbol("005930")
                                .orderType(Order.OrderType.BUY)
                                .quantity(10)
                                .price(new BigDecimal("70000.00"))
                                .status(Order.OrderStatus.EXECUTED)
                                .build();
                try {
                        java.lang.reflect.Field idField = Order.class.getDeclaredField("id");
                        idField.setAccessible(true);
                        idField.set(order, orderId);
                        java.lang.reflect.Field orderTimeField = Order.class.getDeclaredField("orderTime");
                        orderTimeField.setAccessible(true);
                        orderTimeField.set(order, java.time.LocalDateTime.now());
                } catch (Exception e) {
                        throw new RuntimeException(e);
                }
                order.setSignalType("VOLATILITY_BREAKOUT");
                order.setExitRuleType("ATR_TRAILING_STOP");

                when(orderRepository.findByIdAndAccountNo(orderId, "1234567890")).thenReturn(Optional.of(order));

                OrderResponseDto response = orderService.getOrder(orderId, "1234567890");

                assertNotNull(response);
                assertEquals("VOLATILITY_BREAKOUT", response.getSignalType());
                assertEquals("ATR_TRAILING_STOP", response.getExitRuleType());
        }

        @Test
        void 미체결_전체_취소_성공() {
                String accountNo = "1234567890";
                Order o1 = Order.builder()
                                .accountNo(accountNo)
                                .symbol("005930")
                                .orderType(Order.OrderType.BUY)
                                .quantity(10)
                                .price(new BigDecimal("70000"))
                                .status(Order.OrderStatus.PENDING)
                                .build();
                Order o2 = Order.builder()
                                .accountNo(accountNo)
                                .symbol("000660")
                                .orderType(Order.OrderType.SELL)
                                .quantity(5)
                                .price(new BigDecimal("100000"))
                                .status(Order.OrderStatus.PENDING)
                                .build();
                try {
                        java.lang.reflect.Field idField = Order.class.getDeclaredField("id");
                        idField.setAccessible(true);
                        idField.set(o1, java.util.UUID.randomUUID().toString());
                        idField.set(o2, java.util.UUID.randomUUID().toString());
                } catch (Exception e) {
                        throw new RuntimeException(e);
                }

                when(orderRepository.findByAccountNoAndStatus(accountNo, Order.OrderStatus.PENDING))
                                .thenReturn(java.util.List.of(o1, o2));
                when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

                int count = orderService.cancelAllPendingOrders(accountNo);

                assertEquals(2, count);
                verify(orderRepository, times(2)).save(any(Order.class));
        }

        @Test
        void 미체결_전체_취소_대상없음() {
                String accountNo = "1234567890";
                when(orderRepository.findByAccountNoAndStatus(accountNo, Order.OrderStatus.PENDING))
                                .thenReturn(java.util.List.of());

                int count = orderService.cancelAllPendingOrders(accountNo);

                assertEquals(0, count);
                verify(orderRepository, never()).save(any(Order.class));
        }
}
