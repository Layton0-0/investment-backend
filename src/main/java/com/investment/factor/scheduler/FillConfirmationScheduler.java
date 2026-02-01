package com.investment.factor.scheduler;

import com.investment.domain.entity.Order;
import com.investment.domain.repository.OrderRepository;
import com.investment.factor.execution.PipelineExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 체결 확인 후 포지션 등록 스케줄러.
 * EXECUTED 상태이면서 포지션 등록 대기 중인 주문에 대해 TB_STRATEGY_POSITION에 포지션을 등록하고
 * 주문의 포지션 컨텍스트를 초기화합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FillConfirmationScheduler {

    private final OrderRepository orderRepository;
    private final PipelineExecutor pipelineExecutor;

    @Scheduled(cron = "${investment.pipeline.fill-confirmation-cron:0 * * * * *}")
    @Transactional
    public void registerPositionsOnExecution() {
        List<Order> pending = orderRepository.findByStatusAndPositionBasDtIsNotNull(Order.OrderStatus.EXECUTED);
        if (pending.isEmpty()) {
            log.trace("체결 확인 후 포지션 등록: 대기 주문 없음");
            return;
        }

        for (Order order : pending) {
            try {
                boolean registered = pipelineExecutor.registerPositionOnExecution(
                        order.getId(),
                        order.getPositionBasDt(),
                        order.getPositionMarket());
                if (registered) {
                    order.clearPositionContext();
                    orderRepository.save(order);
                }
            } catch (Exception e) {
                log.warn("체결 확인 후 포지션 등록 실패: orderId={}, error={}", order.getId(), e.getMessage());
            }
        }
    }
}
