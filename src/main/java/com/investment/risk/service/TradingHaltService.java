package com.investment.risk.service;

import com.investment.domain.entity.TradingHalt;
import com.investment.domain.repository.TradingHaltRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kill Switch: 전체 주문 차단 상태 조회·설정.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradingHaltService {

    private static final int SINGLETON_ID = 1;

    private final TradingHaltRepository tradingHaltRepository;

    /**
     * 현재 Kill Switch 상태. true면 모든 주문 차단.
     */
    @Transactional(readOnly = true)
    public boolean isHaltAllOrders() {
        return tradingHaltRepository.findById(SINGLETON_ID)
                .map(TradingHalt::getHaltAllOrders)
                .orElse(false);
    }

    /**
     * Kill Switch 설정. true=차단, false=해제.
     */
    @Transactional
    public void setHaltAllOrders(boolean halt) {
        TradingHalt entity = tradingHaltRepository.findById(SINGLETON_ID)
                .orElseGet(() -> {
                    TradingHalt newEntity = TradingHalt.createDefault();
                    return tradingHaltRepository.save(newEntity);
                });
        entity.setHaltAllOrders(halt);
        tradingHaltRepository.save(entity);
        log.info("Kill Switch 설정 변경: haltAllOrders={}", halt);
    }
}
