package com.investment.order.service;

import com.investment.marketdata.config.MarketDataProperties;
import com.investment.order.dto.OrderRequestDto;
import com.investment.order.dto.OrderResponseDto;
import org.junit.jupiter.api.BeforeEach;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * OrderRequestQueue 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderRequestQueue")
class OrderRequestQueueTest {

    @Mock
    private OrderExecutor orderExecutor;

    private MarketDataProperties marketDataProperties;
    private OrderRequestQueue queue;

    @BeforeEach
    void setUp() {
        marketDataProperties = new MarketDataProperties();
        MarketDataProperties.KoreaInvestmentProperties korea = marketDataProperties.getKoreaInvestment();
        MarketDataProperties.KoreaInvestmentProperties.ThrottleProperties throttle = korea.getThrottle();
        throttle.setEnabled(true);
        throttle.setOrdersPerSecond(10);
        throttle.setQueueMaxSize(10);
        throttle.setRejectWhenFull(true);
    }

    @Test
    @DisplayName("submit 시 executor가 호출되고 결과를 반환한다")
    void submit_callsExecutorAndReturnsResult() throws Exception {
        queue = new OrderRequestQueue(orderExecutor, marketDataProperties);
        queue.start();
        try {
            OrderRequestDto request = orderRequest("ACC1", "005930", 10, BigDecimal.valueOf(70000));
            OrderResponseDto expected = OrderResponseDto.builder()
                    .orderId("ord-1")
                    .accountNo("ACC1")
                    .symbol("005930")
                    .orderType(OrderRequestDto.OrderType.BUY)
                    .quantity(10)
                    .price(BigDecimal.valueOf(70000))
                    .status(OrderResponseDto.OrderStatus.EXECUTED)
                    .orderTime(LocalDateTime.now())
                    .build();
            when(orderExecutor.execute(any(OrderRequestDto.class), eq("user1"))).thenReturn(expected);

            OrderResponseDto result = queue.submit(request, "user1");

            assertThat(result).isNotNull();
            assertThat(result.getOrderId()).isEqualTo("ord-1");
            verify(orderExecutor, timeout(3000).times(1)).execute(any(OrderRequestDto.class), eq("user1"));
        } finally {
            queue.stop();
        }
    }

    private static OrderRequestDto orderRequest(String accountNo, String symbol, int qty, BigDecimal price) {
        return OrderRequestDto.builder()
                .accountNo(accountNo)
                .symbol(symbol)
                .quantity(qty)
                .price(price)
                .orderType(OrderRequestDto.OrderType.BUY)
                .market("KR")
                .build();
    }
}
