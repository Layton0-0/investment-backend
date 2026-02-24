package com.investment.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.investment.account.service.AccountService;
import com.investment.common.security.JwtAuthenticationFilter;
import com.investment.common.security.RateLimitFilter;
import com.investment.config.SecurityHeadersConfig;
import com.investment.order.dto.OrderRequestDto;
import com.investment.order.dto.OrderResponseDto;
import com.investment.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private OrderService orderService;

        @MockBean
        private AccountService accountService;

        @MockBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;
        @MockBean
        private RateLimitFilter rateLimitFilter;
        @MockBean
        private SecurityHeadersConfig securityHeadersConfig;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        void 주문_생성_성공() throws Exception {
                // given
                OrderRequestDto request = OrderRequestDto.builder()
                                .accountNo("1234567890")
                                .symbol("005930") // 삼성전자
                                .orderType(OrderRequestDto.OrderType.BUY)
                                .quantity(10)
                                .price(new BigDecimal("70000.00"))
                                .build();

                String orderId = java.util.UUID.randomUUID().toString();
                OrderResponseDto response = OrderResponseDto.builder()
                                .orderId(orderId)
                                .accountNo("1234567890")
                                .symbol("005930")
                                .orderType(OrderRequestDto.OrderType.BUY)
                                .quantity(10)
                                .price(new BigDecimal("70000.00"))
                                .status(OrderResponseDto.OrderStatus.PENDING)
                                .orderTime(LocalDateTime.now())
                                .build();

                when(orderService.executeOrder(any(OrderRequestDto.class))).thenReturn(response);

                // when & then
                mockMvc.perform(post("/api/v1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.orderId").value(orderId))
                                .andExpect(jsonPath("$.symbol").value("005930"))
                                .andExpect(jsonPath("$.orderType").value("BUY"));
        }

        @Test
        void 주문_조회_성공() throws Exception {
                // given
                String orderId = java.util.UUID.randomUUID().toString();
                OrderResponseDto response = OrderResponseDto.builder()
                                .orderId(orderId)
                                .accountNo("1234567890")
                                .symbol("005930") // 삼성전자
                                .orderType(OrderRequestDto.OrderType.BUY)
                                .quantity(10)
                                .price(new BigDecimal("70000.00"))
                                .status(OrderResponseDto.OrderStatus.EXECUTED)
                                .orderTime(LocalDateTime.now())
                                .build();

                when(orderService.getOrder(orderId, "1234567890")).thenReturn(response);

                // when & then
                mockMvc.perform(get("/api/v1/orders/" + orderId)
                                .param("accountNo", "1234567890"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.orderId").value(orderId))
                                .andExpect(jsonPath("$.status").value("EXECUTED"));
        }

        @Test
        void 미체결_전체_취소_성공() throws Exception {
                when(orderService.cancelAllPendingOrders("1234567890")).thenReturn(2);

                mockMvc.perform(post("/api/v1/orders/cancel-all-pending")
                                .param("accountNo", "1234567890"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accountNo").value("1234567890"))
                                .andExpect(jsonPath("$.cancelledCount").value(2));
        }

        @Test
        void 미체결_전체_취소_건수_0() throws Exception {
                when(orderService.cancelAllPendingOrders("1234567890")).thenReturn(0);

                mockMvc.perform(post("/api/v1/orders/cancel-all-pending")
                                .param("accountNo", "1234567890"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.cancelledCount").value(0));
        }

        @Test
        void 주문_목록_조회_성공_모의계좌() throws Exception {
                String accountNo = "50161075-01"; // 모의계좌 예시
                OrderResponseDto o1 = OrderResponseDto.builder()
                                .orderId(java.util.UUID.randomUUID().toString())
                                .accountNo(accountNo)
                                .symbol("005930")
                                .orderType(OrderRequestDto.OrderType.BUY)
                                .quantity(10)
                                .price(new BigDecimal("70000"))
                                .status(OrderResponseDto.OrderStatus.PENDING)
                                .orderTime(LocalDateTime.now())
                                .build();
                when(orderService.getOrders(accountNo)).thenReturn(List.of(o1));

                mockMvc.perform(get("/api/v1/orders").param("accountNo", accountNo))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1))
                                .andExpect(jsonPath("$[0].symbol").value("005930"))
                                .andExpect(jsonPath("$[0].status").value("PENDING"));
        }

        @Test
        void 주문_단건_취소_성공() throws Exception {
                String orderId = java.util.UUID.randomUUID().toString();
                String accountNo = "50161075-01";

                mockMvc.perform(delete("/api/v1/orders/" + orderId).param("accountNo", accountNo))
                                .andExpect(status().isNoContent());
                verify(orderService).cancelOrder(eq(orderId), eq(accountNo));
        }
}
