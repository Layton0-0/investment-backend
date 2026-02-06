package com.investment.core.engine.portfolio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 현재 보유 vs 목표 비중 차이로 매매 리스트 생성.
 * Phase 2에서 구현.
 */
public interface Rebalancer {

    /**
     * 리밸런싱 매매 목록 산출 (userId 없으면 빈 목록 반환).
     *
     * @param asOfDate       기준일
     * @param accountNo     계좌번호
     * @param market        시장
     * @param targetWeights 목표 비중 (symbol -> weight)
     * @param totalValue    계좌 평가총액
     * @return 매수/매도 권장 목록 (심볼, 방향, 수량 또는 금액)
     */
    List<RebalanceItem> computeRebalanceList(
            LocalDate asOfDate,
            String accountNo,
            String market,
            Map<String, BigDecimal> targetWeights,
            BigDecimal totalValue);

    /**
     * 리밸런싱 매매 목록 산출 (userId로 잔고·포지션 조회).
     *
     * @param asOfDate       기준일
     * @param accountNo     계좌번호
     * @param market        시장
     * @param targetWeights 목표 비중 (symbol -> weight)
     * @param totalValue    계좌 평가총액
     * @param userId        사용자 ID (잔고·포지션 조회용)
     * @return 매수/매도 권장 목록
     */
    default List<RebalanceItem> computeRebalanceList(
            LocalDate asOfDate,
            String accountNo,
            String market,
            Map<String, BigDecimal> targetWeights,
            BigDecimal totalValue,
            String userId) {
        if (userId == null) {
            return computeRebalanceList(asOfDate, accountNo, market, targetWeights, totalValue);
        }
        return computeRebalanceList(asOfDate, accountNo, market, targetWeights, totalValue);
    }

    /** 리밸런싱 1건 (심볼, 매수/매도, 수량/금액 등). */
    record RebalanceItem(String symbol, String side, BigDecimal quantity, BigDecimal notional) {}
}
