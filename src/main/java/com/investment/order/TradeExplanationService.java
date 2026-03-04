package com.investment.order;

import com.investment.order.dto.OrderRequestDto;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 매매 실행 시 초보자가 이해할 수 있는 한글 평문 설명을 생성합니다.
 * 템플릿: {symbol} {quantity}주 {side} - 이유: {factors}.
 */
@Service
public class TradeExplanationService {

    private static final int MAX_EXPLANATION_LENGTH = 500;
    private static final Map<String, String> SIGNAL_TYPE_LABELS = new LinkedHashMap<>();
    private static final Map<String, String> EXIT_RULE_LABELS = new LinkedHashMap<>();

    static {
        SIGNAL_TYPE_LABELS.put("VOLATILITY_BREAKOUT", "변동성 돌파 시그널");
        SIGNAL_TYPE_LABELS.put("DUAL_MOMENTUM", "듀얼 모멘텀 시그널");
        SIGNAL_TYPE_LABELS.put("ORDER_FLOW", "수급 강도");
        SIGNAL_TYPE_LABELS.put("QUALITY_GROWTH", "퀄리티·성장");
        SIGNAL_TYPE_LABELS.put("EARNINGS_SURPRISE", "실적 서프라이즈");
        SIGNAL_TYPE_LABELS.put("SECTOR_RS", "섹터 상대 강도");
        SIGNAL_TYPE_LABELS.put("MOMENTUM_RANK", "모멘텀 순위");
        EXIT_RULE_LABELS.put("ATR_TRAILING_STOP", "ATR 트레일링 스탑");
        EXIT_RULE_LABELS.put("TIME_CUT", "기간 만료");
        EXIT_RULE_LABELS.put("STOP_LOSS", "손절");
        EXIT_RULE_LABELS.put("PRIOR_LOW", "전저점 이탈");
        EXIT_RULE_LABELS.put("RSI_EXIT", "RSI 익절");
        EXIT_RULE_LABELS.put("MOMENTUM_RANK_DROP", "모멘텀 순위 하락");
    }

    /**
     * 주문(매수/매도)에 대한 한글 평문 설명을 생성합니다.
     *
     * @param symbol     종목 코드 또는 심볼
     * @param quantity   수량
     * @param orderType  주문 유형 (BUY/SELL)
     * @param signalType 진입 시그널 유형 (매수 시). null 가능
     * @param exitRuleType 청산 규칙 유형 (매도 시). null 가능
     * @return 500자 이내 한글 설명
     */
    public String buildExplanation(
            String symbol,
            Integer quantity,
            OrderRequestDto.OrderType orderType,
            String signalType,
            String exitRuleType) {
        String side = orderType == OrderRequestDto.OrderType.BUY ? "매수" : "매도";
        String reason = buildReasonPhrase(signalType, exitRuleType, orderType);
        String base = String.format("%s %d주 %s - 이유: %s", symbol, quantity != null ? quantity : 0, side, reason);
        if (base.length() > MAX_EXPLANATION_LENGTH) {
            return base.substring(0, MAX_EXPLANATION_LENGTH);
        }
        return base;
    }

    private String buildReasonPhrase(String signalType, String exitRuleType, OrderRequestDto.OrderType orderType) {
        if (orderType == OrderRequestDto.OrderType.BUY && signalType != null && !signalType.isBlank()) {
            return SIGNAL_TYPE_LABELS.getOrDefault(signalType, signalType);
        }
        if (orderType == OrderRequestDto.OrderType.SELL && exitRuleType != null && !exitRuleType.isBlank()) {
            return EXIT_RULE_LABELS.getOrDefault(exitRuleType, exitRuleType);
        }
        return "시그널 기반";
    }
}
