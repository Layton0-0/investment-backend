package com.investment.order.service;

import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.marketdata.config.MarketDataProperties;
import com.investment.order.dto.OrderRequestDto;
import com.investment.order.dto.OrderResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 주문 요청 큐 및 Throttling.
 * 한국투자증권 API 제한(실전 초당 2~10회, 모의 2회)을 준수하기 위해
 * 주문 요청을 단일 큐에 넣고, 단일 소비자 스레드가 RateLimiter 적용 후 순차 실행합니다.
 *
 * @see MarketDataProperties.KoreaInvestmentProperties.ThrottleProperties
 */
@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "investment.market-data.korea-investment.throttle",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class OrderRequestQueue {

    private final OrderExecutor orderExecutor;
    private final MarketDataProperties marketDataProperties;

    private volatile BlockingQueue<OrderQueueItem> queue;
    private volatile RateLimiter rateLimiter;
    private volatile Thread consumerThread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public OrderRequestQueue(OrderExecutor orderExecutor, MarketDataProperties marketDataProperties) {
        this.orderExecutor = orderExecutor;
        this.marketDataProperties = marketDataProperties;
    }

    public boolean isEnabled() {
        return marketDataProperties.getKoreaInvestment().getThrottle().isEnabled();
    }

    @PostConstruct
    public void start() {
        MarketDataProperties.KoreaInvestmentProperties.ThrottleProperties throttle = marketDataProperties.getKoreaInvestment().getThrottle();
        int capacity = Math.max(1, throttle.getQueueMaxSize());
        int ordersPerSecond = Math.max(1, Math.min(20, throttle.getOrdersPerSecond()));

        queue = new LinkedBlockingQueue<>(capacity);
        rateLimiter = RateLimiter.of("orderQueue", RateLimiterConfig.custom()
                .limitForPeriod(ordersPerSecond)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofSeconds(30))
                .build());

        running.set(true);
        consumerThread = new Thread(this::consume, "order-queue-consumer");
        consumerThread.setDaemon(false);
        consumerThread.start();
        log.info("Order request queue started: capacity={}, ordersPerSecond={}", capacity, ordersPerSecond);
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        if (consumerThread != null) {
            consumerThread.interrupt();
            try {
                consumerThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        log.info("Order request queue stopped");
    }

    /**
     * 주문 요청을 큐에 넣고 결과를 기다립니다.
     * 큐가 가득 찼고 rejectWhenFull이면 즉시 예외를 던집니다.
     */
    public OrderResponseDto submit(OrderRequestDto request, String userId) {
        MarketDataProperties.KoreaInvestmentProperties.ThrottleProperties throttle = marketDataProperties.getKoreaInvestment().getThrottle();
        CompletableFuture<OrderResponseDto> future = new CompletableFuture<>();
        OrderQueueItem item = new OrderQueueItem(request, userId, future);

        boolean offered = queue.offer(item);
        if (!offered && throttle.isRejectWhenFull()) {
            throw new DomainException(ErrorCode.ORDER_FAILED,
                    "주문 요청이 많습니다. 잠시 후 다시 시도해 주세요. (큐 가득 참)");
        }
        if (!offered) {
            try {
                queue.put(item);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new DomainException(ErrorCode.ORDER_FAILED, "주문 요청 대기 중 중단되었습니다.", e);
            }
        }

        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DomainException(ErrorCode.ORDER_FAILED, "주문 요청 대기 중 중단되었습니다.", e);
        } catch (Exception e) {
            if (e.getCause() instanceof DomainException) {
                throw (DomainException) e.getCause();
            }
            throw new DomainException(ErrorCode.ORDER_FAILED,
                    "주문 실행 중 오류가 발생했습니다: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()),
                    e.getCause() != null ? e.getCause() : e);
        }
    }

    private void consume() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                OrderQueueItem item = queue.take();
                try {
                    OrderResponseDto result = rateLimiter.executeSupplier(
                            () -> orderExecutor.execute(item.request, item.userId));
                    item.future.complete(result);
                } catch (Exception e) {
                    item.future.completeExceptionally(e);
                    log.debug("Order execution failed in queue: accountNo={}, symbol={}", 
                            item.request.getAccountNo(), item.request.getSymbol(), e);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Order queue consumer error", e);
            }
        }
    }

    private static class OrderQueueItem {
        final OrderRequestDto request;
        final String userId;
        final CompletableFuture<OrderResponseDto> future;

        OrderQueueItem(OrderRequestDto request, String userId, CompletableFuture<OrderResponseDto> future) {
            this.request = request;
            this.userId = userId;
            this.future = future;
        }
    }
}
