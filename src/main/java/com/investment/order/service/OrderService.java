package com.investment.order.service;

import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.domain.entity.Order;
import com.investment.domain.entity.TradingSetting;
import com.investment.domain.repository.OrderRepository;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.order.dto.OrderRequestDto;
import com.investment.order.dto.OrderResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 주문 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final TradingSettingRepository tradingSettingRepository;
    
    /**
     * 주문 실행
     */
    @Transactional
    public OrderResponseDto executeOrder(OrderRequestDto request) {
        log.info("주문 실행: accountNo={}, symbol={}, type={}, quantity={}, price={}", 
                request.getAccountNo(), request.getSymbol(), request.getOrderType(), 
                request.getQuantity(), request.getPrice());
        
        // 거래 설정 조회 및 검증
        TradingSetting setting = tradingSettingRepository.findByAccountNo(request.getAccountNo())
                .orElseThrow(() -> new DomainException(
                        ErrorCode.SETTING_NOT_FOUND, 
                        "거래 설정을 찾을 수 없습니다: " + request.getAccountNo()));
        
        // 최대 투자금액 검증
        BigDecimal orderAmount = request.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
        if (orderAmount.compareTo(setting.getMaxInvestmentAmount()) > 0) {
            throw new DomainException(ErrorCode.EXCEEDS_MAX_INVESTMENT, 
                    String.format("최대 투자금액(%s)을 초과합니다: %s", 
                            setting.getMaxInvestmentAmount(), orderAmount));
        }
        
        // 최소 투자금액 검증
        if (orderAmount.compareTo(setting.getMinInvestmentAmount()) < 0) {
            throw new DomainException(ErrorCode.INVALID_ORDER_AMOUNT, 
                    String.format("최소 투자금액(%s) 미만입니다: %s", 
                            setting.getMinInvestmentAmount(), orderAmount));
        }
        
        // 주문 엔티티 생성
        Order order = Order.builder()
                .accountNo(request.getAccountNo())
                .symbol(request.getSymbol())
                .orderType(convertOrderType(request.getOrderType()))
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .status(Order.OrderStatus.PENDING)
                .build();
        
        order = orderRepository.save(order);
        
        // 키움 API 제거로 인해 주문은 DB에만 저장 (실제 거래는 수동 처리)
        // 주문 상태는 PENDING으로 유지
        order.updateStatus(Order.OrderStatus.PENDING);
        order = orderRepository.save(order);
        
        log.info("주문이 생성되었습니다 (수동 처리 필요): orderId={}", order.getId());
        return convertToResponseDto(order);
    }
    
    /**
     * 주문 조회
     */
    @Transactional(readOnly = true)
    public OrderResponseDto getOrder(String orderId, String accountNo) {
        Order order = orderRepository.findByIdAndAccountNo(orderId, accountNo)
                .orElseThrow(() -> new DomainException(
                        ErrorCode.ORDER_NOT_FOUND, 
                        "주문을 찾을 수 없습니다: " + orderId));
        
        return convertToResponseDto(order);
    }
    
    /**
     * 계좌별 주문 목록 조회
     */
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getOrders(String accountNo) {
        List<Order> orders = orderRepository.findByAccountNo(accountNo);
        return orders.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }
    
    /**
     * 주문 취소
     */
    @Transactional
    public void cancelOrder(String orderId, String accountNo) {
        Order order = orderRepository.findByIdAndAccountNo(orderId, accountNo)
                .orElseThrow(() -> new DomainException(
                        ErrorCode.ORDER_NOT_FOUND, 
                        "주문을 찾을 수 없습니다: " + orderId));
        
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new DomainException(ErrorCode.ORDER_FAILED, 
                    "대기 중인 주문만 취소할 수 있습니다");
        }
        
        // 키움 API 제거로 인해 주문 취소는 DB 상태만 변경
        order.cancel("사용자 요청에 의한 취소");
        orderRepository.save(order);
        log.info("주문이 취소되었습니다: orderId={}", orderId);
    }
    
    private Order.OrderType convertOrderType(OrderRequestDto.OrderType type) {
        return type == OrderRequestDto.OrderType.BUY ? Order.OrderType.BUY : Order.OrderType.SELL;
    }
    
    private OrderResponseDto convertToResponseDto(Order order) {
        return OrderResponseDto.builder()
                .orderId(order.getId())
                .accountNo(order.getAccountNo())
                .symbol(order.getSymbol())
                .orderType(convertToDtoOrderType(order.getOrderType()))
                .quantity(order.getQuantity())
                .price(order.getPrice())
                .status(convertToDtoStatus(order.getStatus()))
                .orderTime(order.getOrderTime())
                .message(order.getMessage())
                .build();
    }
    
    private OrderRequestDto.OrderType convertToDtoOrderType(Order.OrderType type) {
        return type == Order.OrderType.BUY ? OrderRequestDto.OrderType.BUY : OrderRequestDto.OrderType.SELL;
    }
    
    private OrderResponseDto.OrderStatus convertToDtoStatus(Order.OrderStatus status) {
        switch (status) {
            case PENDING:
                return OrderResponseDto.OrderStatus.PENDING;
            case EXECUTED:
                return OrderResponseDto.OrderStatus.EXECUTED;
            case PARTIAL:
                return OrderResponseDto.OrderStatus.PARTIAL;
            case CANCELLED:
                return OrderResponseDto.OrderStatus.CANCELLED;
            case FAILED:
                return OrderResponseDto.OrderStatus.FAILED;
            default:
                return OrderResponseDto.OrderStatus.PENDING;
        }
    }
}
