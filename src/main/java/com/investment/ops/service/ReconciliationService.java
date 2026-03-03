package com.investment.ops.service;

import com.investment.account.dto.AccountPositionDto;
import com.investment.account.dto.BalanceAndPositionsDto;
import com.investment.account.service.AccountService;
import com.investment.common.security.LogMaskingUtil;
import com.investment.domain.entity.StrategyPosition;
import com.investment.domain.repository.StrategyPositionRepository;
import com.investment.ops.dto.ReconciliationResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 브로커 실잔고 vs TB_STRATEGY_POSITION(보유) 정합성 비교.
 * 불일치 시 리포트(감지 전용). Re-sync API·정기 배치에서 사용.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private static final String DEFAULT_MARKET = "KR";

    private final AccountService accountService;
    private final StrategyPositionRepository strategyPositionRepository;

    /**
     * 계좌별 브로커 포지션과 DB 보유 포지션을 비교해 불일치 목록을 반환.
     *
     * @param userId    사용자 ID (브로커 API 호출용)
     * @param accountNo 계좌번호 (암호화 해제된 값)
     * @return 불일치·DB전용·브로커전용 목록 및 요약
     */
    @Transactional(readOnly = true)
    public ReconciliationResultDto reconcile(String userId, String accountNo) {
        if (userId == null || accountNo == null || accountNo.isBlank()) {
            return ReconciliationResultDto.builder()
                    .userId(userId)
                    .accountNo(accountNo)
                    .mismatchItems(List.of())
                    .onlyInDb(List.of())
                    .onlyInBroker(List.of())
                    .summary(ReconciliationResultDto.ReconciliationSummaryDto.builder()
                            .mismatchCount(0)
                            .onlyInDbCount(0)
                            .onlyInBrokerCount(0)
                            .hasDiscrepancy(false)
                            .build())
                    .build();
        }

        Map<String, Integer> dbByKey = buildDbQuantityBySymbolMarket(accountNo);
        Map<String, Integer> brokerByKey = buildBrokerQuantityBySymbolMarket(userId, accountNo);

        List<ReconciliationResultDto.ReconciliationMismatchItemDto> mismatchItems = new ArrayList<>();
        List<ReconciliationResultDto.ReconciliationPositionItemDto> onlyInDb = new ArrayList<>();
        List<ReconciliationResultDto.ReconciliationPositionItemDto> onlyInBroker = new ArrayList<>();

        for (Map.Entry<String, Integer> e : dbByKey.entrySet()) {
            String key = e.getKey();
            int dbQty = e.getValue();
            int brokerQty = brokerByKey.getOrDefault(key, 0);
            String[] symMarket = key.split("\\|");
            String symbol = symMarket[0];
            String market = symMarket.length > 1 ? symMarket[1] : DEFAULT_MARKET;
            if (brokerQty == 0) {
                onlyInDb.add(ReconciliationResultDto.ReconciliationPositionItemDto.builder()
                        .symbol(symbol)
                        .market(market)
                        .quantity(dbQty)
                        .build());
            } else if (dbQty != brokerQty) {
                mismatchItems.add(ReconciliationResultDto.ReconciliationMismatchItemDto.builder()
                        .symbol(symbol)
                        .market(market)
                        .dbQuantity(dbQty)
                        .brokerQuantity(brokerQty)
                        .build());
            }
        }
        for (Map.Entry<String, Integer> e : brokerByKey.entrySet()) {
            String key = e.getKey();
            if (dbByKey.containsKey(key)) {
                continue;
            }
            int brokerQty = e.getValue();
            if (brokerQty <= 0) {
                continue;
            }
            String[] symMarket = key.split("\\|");
            String symbol = symMarket[0];
            String market = symMarket.length > 1 ? symMarket[1] : DEFAULT_MARKET;
            onlyInBroker.add(ReconciliationResultDto.ReconciliationPositionItemDto.builder()
                    .symbol(symbol)
                    .market(market)
                    .quantity(brokerQty)
                    .build());
        }

        int mismatchCount = mismatchItems.size();
        int onlyInDbCount = onlyInDb.size();
        int onlyInBrokerCount = onlyInBroker.size();
        boolean hasDiscrepancy = mismatchCount > 0 || onlyInDbCount > 0 || onlyInBrokerCount > 0;

        if (hasDiscrepancy) {
            log.warn("Reconciliation 불일치: accountNo={}, mismatch={}, onlyInDb={}, onlyInBroker={}",
                    LogMaskingUtil.maskAccountNo(accountNo), mismatchCount, onlyInDbCount, onlyInBrokerCount);
        }

        return ReconciliationResultDto.builder()
                .userId(userId)
                .accountNo(accountNo)
                .mismatchItems(mismatchItems)
                .onlyInDb(onlyInDb)
                .onlyInBroker(onlyInBroker)
                .summary(ReconciliationResultDto.ReconciliationSummaryDto.builder()
                        .mismatchCount(mismatchCount)
                        .onlyInDbCount(onlyInDbCount)
                        .onlyInBrokerCount(onlyInBrokerCount)
                        .hasDiscrepancy(hasDiscrepancy)
                        .build())
                .build();
    }

    private String key(String symbol, String market) {
        return symbol + "|" + (market != null && !market.isBlank() ? market : DEFAULT_MARKET);
    }

    private Map<String, Integer> buildDbQuantityBySymbolMarket(String accountNo) {
        List<StrategyPosition> positions = strategyPositionRepository
                .findByAccountNoAndExitDtIsNullOrderByEntryDtAsc(accountNo);
        Map<String, Integer> map = new HashMap<>();
        for (StrategyPosition p : positions) {
            String k = key(p.getSymbol(), p.getMarket());
            map.merge(k, p.getQuantity(), Integer::sum);
        }
        return map;
    }

    private Map<String, Integer> buildBrokerQuantityBySymbolMarket(String userId, String accountNo) {
        BalanceAndPositionsDto balanceAndPositions = accountService.getBalanceAndPositionsWithUserId(userId, accountNo);
        if (balanceAndPositions == null || balanceAndPositions.getPositions() == null) {
            return new HashMap<>();
        }
        Map<String, Integer> map = new HashMap<>();
        for (AccountPositionDto p : balanceAndPositions.getPositions()) {
            if (p.getSymbol() == null || p.getSymbol().isBlank()) {
                continue;
            }
            String market = (p.getMarket() != null && !p.getMarket().isBlank()) ? p.getMarket() : DEFAULT_MARKET;
            String k = key(p.getSymbol(), market);
            int qty = (p.getQuantity() != null && p.getQuantity() >= 0) ? p.getQuantity() : 0;
            if (qty > 0) {
                map.merge(k, qty, Integer::sum);
            }
        }
        return map;
    }
}
