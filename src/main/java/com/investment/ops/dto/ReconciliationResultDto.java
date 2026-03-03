package com.investment.ops.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 브로커–DB 포지션 정합성(Reconciliation) 결과.
 * TB_STRATEGY_POSITION(보유) vs 증권사 실잔고 비교 결과.
 */
@Getter
@Builder
public class ReconciliationResultDto {

    private final String userId;
    private final String accountNo;
    /** 종목별 수량 불일치 (동일 종목, DB 수량 ≠ 브로커 수량) */
    private final List<ReconciliationMismatchItemDto> mismatchItems;
    /** DB에만 있는 보유 (브로커에는 없음) */
    private final List<ReconciliationPositionItemDto> onlyInDb;
    /** 브로커에만 있는 보유 (DB에는 없음) */
    private final List<ReconciliationPositionItemDto> onlyInBroker;
    private final ReconciliationSummaryDto summary;

    @Getter
    @Builder
    public static class ReconciliationMismatchItemDto {
        private final String symbol;
        private final String market;
        private final int dbQuantity;
        private final int brokerQuantity;
    }

    @Getter
    @Builder
    public static class ReconciliationPositionItemDto {
        private final String symbol;
        private final String market;
        private final int quantity;
    }

    @Getter
    @Builder
    public static class ReconciliationSummaryDto {
        private final int mismatchCount;
        private final int onlyInDbCount;
        private final int onlyInBrokerCount;
        private final boolean hasDiscrepancy;
    }
}
