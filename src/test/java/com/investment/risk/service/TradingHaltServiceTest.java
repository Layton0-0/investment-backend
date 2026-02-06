package com.investment.risk.service;

import com.investment.domain.entity.TradingHalt;
import com.investment.domain.repository.TradingHaltRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TradingHaltService")
class TradingHaltServiceTest {

    @Mock
    private TradingHaltRepository tradingHaltRepository;

    @InjectMocks
    private TradingHaltService tradingHaltService;

    @Test
    @DisplayName("isHaltAllOrders: 엔티티 없으면 false")
    void isHaltAllOrders_whenEmpty_returnsFalse() {
        when(tradingHaltRepository.findById(1)).thenReturn(Optional.empty());

        assertFalse(tradingHaltService.isHaltAllOrders());
    }

    @Test
    @DisplayName("isHaltAllOrders: haltAllOrders true면 true")
    void isHaltAllOrders_whenTrue_returnsTrue() {
        TradingHalt entity = TradingHalt.createDefault();
        entity.setHaltAllOrders(true);
        when(tradingHaltRepository.findById(1)).thenReturn(Optional.of(entity));

        assertTrue(tradingHaltService.isHaltAllOrders());
    }

    @Test
    @DisplayName("setHaltAllOrders: 엔티티 없으면 생성 후 설정")
    void setHaltAllOrders_whenEmpty_createsAndSets() {
        when(tradingHaltRepository.findById(1)).thenReturn(Optional.empty());
        when(tradingHaltRepository.save(any(TradingHalt.class))).thenAnswer(inv -> inv.getArgument(0));

        tradingHaltService.setHaltAllOrders(true);

        verify(tradingHaltRepository, atLeastOnce()).save(argThat(h -> Boolean.TRUE.equals(h.getHaltAllOrders())));
    }

    @Test
    @DisplayName("setHaltAllOrders: 기존 엔티티 있으면 업데이트")
    void setHaltAllOrders_whenExists_updates() {
        TradingHalt entity = TradingHalt.createDefault();
        entity.setHaltAllOrders(false);
        when(tradingHaltRepository.findById(1)).thenReturn(Optional.of(entity));
        when(tradingHaltRepository.save(any(TradingHalt.class))).thenAnswer(inv -> inv.getArgument(0));

        tradingHaltService.setHaltAllOrders(true);

        verify(tradingHaltRepository).save(entity);
        assertTrue(entity.getHaltAllOrders());
    }
}
