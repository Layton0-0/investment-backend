package com.investment.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.investment.order.dto.OrderRequestDto;
import com.investment.order.dto.OrderResponseDto;
import com.investment.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private OrderService orderService;
    
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
}
