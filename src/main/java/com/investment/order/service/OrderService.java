package com.investment.order.service;

import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.config.CacheConfig;
import com.investment.domain.entity.Order;
import com.investment.domain.entity.TradingSetting;
import com.investment.domain.repository.OrderRepository;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.order.dto.OrderRequestDto;
import com.investment.order.dto.OrderResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 주문 서비스
 * 
 * <p>주문 생성, 조회, 취소 등의 주문 관련 비즈니스 로직을 처리합니다.
 * 키움 API 연동이 제거되어 주문은 데이터베이스에만 저장되며,
 * 실제 거래는 수동으로 처리해야 합니다.</p>
 * 
 * <p>주문 실행 시 계좌 잔액 및 보유 종목 캐시를 자동으로 무효화합니다.</p>
 * 
 * @author Investment System
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final TradingSettingRepository tradingSettingRepository;
    
    /**
     * 주문 실행
     * 
     * <p>주문 요청을 검증하고 주문 엔티티를 생성하여 저장합니다.
     * 거래 설정의 최소/최대 투자금액을 검증하며,
     * 주문 실행 후 계좌 관련 캐시를 무효화합니다.</p>
     * 
     * <p>현재는 키움 API 연동이 제거되어 주문 상태가 PENDING으로 유지되며,
     * 실제 거래는 수동으로 처리해야 합니다.</p>
     * 
     * @param request 주문 요청 정보 (계좌번호, 종목코드, 주문유형, 수량, 가격)
     * @return 생성된 주문 정보
     * @throws DomainException 거래 설정이 없거나, 투자금액이 범위를 벗어난 경우
     */
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_ACCOUNT, allEntries = true)
    public OrderResponseDto executeOrder(OrderRequestDto request) {
        log.info("주문 실행 요청: accountNo={}, symbol={}, type={}, quantity={}, price={}", 
                request.getAccountNo(), request.getSymbol(), request.getOrderType(), 
                request.getQuantity(), request.getPrice());
        
        // 거래 설정 조회 및 검증
        TradingSetting setting = tradingSettingRepository.findByAccountNo(request.getAccountNo())
                .orElseThrow(() -> new DomainException(
                        ErrorCode.SETTING_NOT_FOUND, 
                        "거래 설정을 찾을 수 없습니다: " + request.getAccountNo()));
        
        // 주문 금액 계산 및 검증
        BigDecimal orderAmount = request.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
        
        // 최대 투자금액 검증
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
        
        // 주문 엔티티 생성 및 저장
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
        // 주문 상태는 PENDING으로 유지되며, 실제 거래는 별도 프로세스에서 처리해야 함
        log.info("주문이 생성되었습니다 (수동 처리 필요): orderId={}, accountNo={}, symbol={}", 
                order.getId(), order.getAccountNo(), order.getSymbol());
        
        return convertToResponseDto(order);
    }
    
    /**
     * 주문 조회
     * 
     * <p>주문 ID와 계좌번호를 기반으로 특정 주문을 조회합니다.
     * 계좌번호 검증을 통해 다른 계좌의 주문에 대한 접근을 방지합니다.</p>
     * 
     * @param orderId 주문 ID
     * @param accountNo 계좌번호 (접근 권한 검증용)
     * @return 주문 정보
     * @throws DomainException 주문을 찾을 수 없는 경우
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
     * 
     * <p>특정 계좌의 모든 주문을 조회합니다.
     * 주문 목록은 주문 시간 순서로 정렬됩니다.</p>
     * 
     * @param accountNo 계좌번호
     * @return 주문 목록 (주문이 없는 경우 빈 리스트)
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
     * 
     * <p>대기 중인 주문을 취소합니다.
     * 이미 체결되거나 취소된 주문은 취소할 수 없습니다.</p>
     * 
     * <p>키움 API 연동이 제거되어 주문 취소는 데이터베이스 상태만 변경됩니다.</p>
     * 
     * @param orderId 주문 ID
     * @param accountNo 계좌번호 (접근 권한 검증용)
     * @throws DomainException 주문을 찾을 수 없거나, 이미 체결/취소된 주문인 경우
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
        
        log.info("주문이 취소되었습니다: orderId={}, accountNo={}", orderId, accountNo);
    }
    
    /**
     * DTO 주문 유형을 엔티티 주문 유형으로 변환
     * 
     * @param type DTO 주문 유형 (BUY/SELL)
     * @return 엔티티 주문 유형
     */
    private Order.OrderType convertOrderType(OrderRequestDto.OrderType type) {
        return type == OrderRequestDto.OrderType.BUY ? Order.OrderType.BUY : Order.OrderType.SELL;
    }
    
    /**
     * 주문 엔티티를 응답 DTO로 변환
     * 
     * @param order 주문 엔티티
     * @return 주문 응답 DTO
     */
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
    
    /**
     * 엔티티 주문 유형을 DTO 주문 유형으로 변환
     * 
     * @param type 엔티티 주문 유형
     * @return DTO 주문 유형
     */
    private OrderRequestDto.OrderType convertToDtoOrderType(Order.OrderType type) {
        return type == Order.OrderType.BUY ? OrderRequestDto.OrderType.BUY : OrderRequestDto.OrderType.SELL;
    }
    
    /**
     * 엔티티 주문 상태를 DTO 주문 상태로 변환
     * 
     * @param status 엔티티 주문 상태
     * @return DTO 주문 상태
     */
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
