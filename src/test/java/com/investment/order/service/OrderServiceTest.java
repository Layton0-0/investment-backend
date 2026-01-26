package com.investment.order.service;

import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.domain.entity.Order;
import com.investment.domain.entity.TradingSetting;
import com.investment.domain.repository.OrderRepository;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.order.dto.OrderRequestDto;
import com.investment.order.dto.OrderResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private TradingSettingRepository tradingSettingRepository;
    
    @InjectMocks
    private OrderService orderService;
    
    private TradingSetting tradingSetting;
    private OrderRequestDto orderRequest;
    
    @BeforeEach
    void setUp() {
        tradingSetting = TradingSetting.builder()
                .accountNo("1234567890")
                .maxInvestmentAmount(new BigDecimal("1000000"))
                .minInvestmentAmount(new BigDecimal("10000"))
                .defaultCurrency("USD")
                .autoTradingEnabled(false)
                .build();
        
        orderRequest = OrderRequestDto.builder()
                .accountNo("1234567890")
                .symbol("SPY")
                .orderType(OrderRequestDto.OrderType.BUY)
                .quantity(10)
                .price(new BigDecimal("400.00"))
                .build();
    }
    
    @Test
    void 주문_실행_성공() {
        // given
        when(tradingSettingRepository.findByAccountNo("1234567890"))
                .thenReturn(Optional.of(tradingSetting));
        
        Order savedOrder = Order.builder()
                .accountNo(orderRequest.getAccountNo())
                .symbol(orderRequest.getSymbol())
                .orderType(Order.OrderType.BUY)
                .quantity(orderRequest.getQuantity())
                .price(orderRequest.getPrice())
                .status(Order.OrderStatus.PENDING)
                .build();
        
        // Reflection을 사용하여 id 설정
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
        verify(orderRepository, times(2)).save(any(Order.class));
    }
    
    @Test
    void 주문_실행_최대투자금액_초과() {
        // given
        orderRequest.setPrice(new BigDecimal("200000.00")); // 2,000,000원 초과
        
        when(tradingSettingRepository.findByAccountNo("1234567890"))
                .thenReturn(Optional.of(tradingSetting));
        
        // when & then
        DomainException exception = assertThrows(DomainException.class, 
                () -> orderService.executeOrder(orderRequest));
        
        assertEquals(ErrorCode.EXCEEDS_MAX_INVESTMENT, exception.getErrorCode());
        verify(orderRepository, never()).save(any(Order.class));
    }
    
    @Test
    void 주문_실행_최소투자금액_미만() {
        // given
        orderRequest.setPrice(new BigDecimal("500.00")); // 5,000원 미만
        
        when(tradingSettingRepository.findByAccountNo("1234567890"))
                .thenReturn(Optional.of(tradingSetting));
        
        // when & then
        DomainException exception = assertThrows(DomainException.class, 
                () -> orderService.executeOrder(orderRequest));
        
        assertEquals(ErrorCode.INVALID_ORDER_AMOUNT, exception.getErrorCode());
        verify(orderRepository, never()).save(any(Order.class));
    }
    
    @Test
    void 주문_조회_성공() {
        // given
        Order order = Order.builder()
                .accountNo("1234567890")
                .symbol("SPY")
                .orderType(Order.OrderType.BUY)
                .quantity(10)
                .price(new BigDecimal("400.00"))
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
        assertEquals("SPY", response.getSymbol());
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
}
