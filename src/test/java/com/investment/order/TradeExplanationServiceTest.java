package com.investment.order;

import com.investment.order.dto.OrderRequestDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TradeExplanationServiceTest {

    private final TradeExplanationService service = new TradeExplanationService();

    @Test
    void buildExplanation_buyWithSignalType_returnsKoreanPhrase() {
        String result = service.buildExplanation(
                "005930", 10, OrderRequestDto.OrderType.BUY,
                "DUAL_MOMENTUM", null);
        assertTrue(result.contains("005930"));
        assertTrue(result.contains("10주"));
        assertTrue(result.contains("매수"));
        assertTrue(result.contains("이유"));
        assertTrue(result.contains("듀얼 모멘텀 시그널"));
    }

    @Test
    void buildExplanation_sellWithExitRule_returnsKoreanPhrase() {
        String result = service.buildExplanation(
                "005930", 5, OrderRequestDto.OrderType.SELL,
                null, "ATR_TRAILING_STOP");
        assertTrue(result.contains("매도"));
        assertTrue(result.contains("이유"));
        assertTrue(result.contains("ATR 트레일링 스탑"));
    }

    @Test
    void buildExplanation_buyNoSignal_usesManualOrderReason() {
        String result = service.buildExplanation(
                "AAPL", 3, OrderRequestDto.OrderType.BUY,
                null, null);
        assertTrue(result.contains("매수"));
        assertTrue(result.contains("이유"));
        assertTrue(result.contains("수동 주문"));
    }

    @Test
    void buildExplanation_unknownSignalType_usesRawValue() {
        String result = service.buildExplanation(
                "005930", 1, OrderRequestDto.OrderType.BUY,
                "CUSTOM_SIGNAL", null);
        assertTrue(result.contains("CUSTOM_SIGNAL"));
    }

    @Test
    void buildExplanation_resultWithin500Chars() {
        String longSymbol = "A".repeat(100);
        String result = service.buildExplanation(
                longSymbol, 999, OrderRequestDto.OrderType.BUY,
                "VOLATILITY_BREAKOUT", null);
        assertNotNull(result);
        assertTrue(result.length() <= 500);
    }
}
