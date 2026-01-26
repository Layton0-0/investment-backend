package com.investment.strategy.scheduler;

import com.investment.domain.entity.Strategy;
import com.investment.domain.repository.StrategyRepository;
import com.investment.order.dto.OrderRequestDto;
import com.investment.order.service.OrderService;
import com.investment.strategy.domain.StrategyStatus;
import com.investment.strategy.domain.StrategyType;
import com.investment.strategy.service.StrategyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 전략 스케줄러
 * 활성화된 전략들을 주기적으로 실행합니다.
 * 중지된 전략은 절대 실행하지 않습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StrategyScheduler {
    
    private final StrategyRepository strategyRepository;
    private final StrategyService strategyService;
    private final OrderService orderService;
    
    /**
     * 단기 전략 실행 (매 1시간마다)
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void executeShortTermStrategies() {
        log.info("단기 전략 실행 시작");
        executeStrategies(StrategyType.SHORT_TERM);
    }
    
    /**
     * 중기 전략 실행 (매일 오전 9시)
     */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void executeMediumTermStrategies() {
        log.info("중기 전략 실행 시작");
        executeStrategies(StrategyType.MEDIUM_TERM);
    }
    
    /**
     * 장기 전략 실행 (매주 월요일 오전 9시)
     */
    @Scheduled(cron = "0 0 9 * * MON")
    @Transactional
    public void executeLongTermStrategies() {
        log.info("장기 전략 실행 시작");
        executeStrategies(StrategyType.LONG_TERM);
    }
    
    /**
     * 전략 실행
     */
    private void executeStrategies(StrategyType strategyType) {
        // 활성화된 전략만 조회 (중지된 전략은 제외)
        List<Strategy> strategies = strategyRepository.findByStatus(StrategyStatus.ACTIVE);
        
        strategies.stream()
                .filter(s -> s.getStrategyType() == strategyType)
                .forEach(strategy -> {
                    try {
                        executeStrategy(strategy);
                    } catch (Exception e) {
                        log.error("전략 실행 실패: accountNo={}, strategyType={}", 
                                strategy.getAccountNo(), strategy.getStrategyType(), e);
                        // 실패 기록
                        strategy.recordExecution(false, BigDecimal.ZERO);
                        strategyRepository.save(strategy);
                    }
                });
    }
    
    /**
     * 개별 전략 실행
     */
    private void executeStrategy(Strategy strategy) {
        log.info("전략 실행: accountNo={}, strategyType={}", 
                strategy.getAccountNo(), strategy.getStrategyType());
        
        // 중지된 전략은 실행하지 않음 (이중 체크)
        if (strategy.isStopped()) {
            log.warn("전략이 중지되어 있어 실행하지 않습니다: accountNo={}, strategyType={}", 
                    strategy.getAccountNo(), strategy.getStrategyType());
            return;
        }
        
        // TODO: 모니터링할 종목 목록 조회 (예: 설정된 종목 목록 또는 포트폴리오 종목)
        // 임시로 빈 리스트 사용
        List<String> symbols = List.of(); // 실제로는 종목 목록을 조회해야 함
        
        if (symbols.isEmpty()) {
            log.debug("실행할 종목이 없습니다: accountNo={}, strategyType={}", 
                    strategy.getAccountNo(), strategy.getStrategyType());
            return;
        }
        
        // 전략 서비스를 통해 매매 결정
        List<OrderRequestDto> orders = strategyService.decideTradingActionsForSymbols(
                strategy.getAccountNo(), symbols, strategy.getStrategyType());
        
        // 주문 실행 및 결과 기록
        BigDecimal totalProfitLoss = BigDecimal.ZERO;
        int successCount = 0;
        int failureCount = 0;
        
        for (OrderRequestDto order : orders) {
            try {
                orderService.executeOrder(order);
                successCount++;
                // TODO: 실제 손익 계산
                totalProfitLoss = totalProfitLoss.add(BigDecimal.ZERO);
            } catch (Exception e) {
                log.error("주문 실행 실패: order={}", order, e);
                failureCount++;
            }
        }
        
        // 전략 실행 결과 기록
        boolean success = successCount > 0;
        strategy.recordExecution(success, totalProfitLoss);
        strategyRepository.save(strategy);
        
        log.info("전략 실행 완료: accountNo={}, strategyType={}, success={}, totalOrders={}", 
                strategy.getAccountNo(), strategy.getStrategyType(), success, orders.size());
    }
}
