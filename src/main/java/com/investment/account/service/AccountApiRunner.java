package com.investment.account.service;

import com.investment.account.client.KoreaInvestmentAccountClient;
import com.investment.account.dto.AccountAssetDto;
import com.investment.account.dto.AccountPositionDto;
import com.investment.account.dto.BalanceRealizedProfitLossDto;
import com.investment.account.dto.BuyableAmountDto;
import com.investment.account.dto.CancelableOrderDto;
import com.investment.account.dto.OrderHistoryDto;
import com.investment.account.dto.PeriodProfitLossStatusDto;
import com.investment.account.dto.ProfitLossDto;
import com.investment.account.dto.SellableQuantityDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 한국투자증권 계좌 API 호출을 별도 트랜잭션(REQUIRES_NEW)에서 실행한다.
 * API/토큰 경로에서 예외가 나도 호출자 트랜잭션이 rollback-only로 오염되지 않도록 하여,
 * 호출자에서 catch 후 DB 폴백 시 UnexpectedRollbackException이 발생하지 않게 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountApiRunner {

    private final KoreaInvestmentAccountClient accountClient;

    /**
     * 주식잔고조회를 새 트랜잭션에서 실행. 실패 시 예외를 그대로 전파(호출자 트랜잭션에는 영향 없음).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public KoreaInvestmentAccountClient.BalanceAndPositionsResult inquireBalanceInNewTx(String userId, String accountNo) {
        return accountClient.inquireBalance(userId, accountNo);
    }

    /**
     * 해외 잔고 조회를 새 트랜잭션에서 실행. 실패 시 빈 목록·null 요약 반환.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public KoreaInvestmentAccountClient.OverseasBalanceResult inquireOverseasBalanceInNewTx(String userId, String accountNo) {
        try {
            return accountClient.inquireOverseasBalance(userId, accountNo);
        } catch (Exception e) {
            log.debug("해외 잔고 조회 스킵: accountNo={}, error={}", accountNo, e.getMessage());
            return new KoreaInvestmentAccountClient.OverseasBalanceResult(new ArrayList<>(), null);
        }
    }

    /**
     * 투자계좌자산현황조회를 새 트랜잭션에서 실행. 토큰/API 실패 시 예외 전파(호출자 트랜잭션에는 영향 없음).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public AccountAssetDto inquireAssetsInNewTx(String userId, String accountNo) {
        return accountClient.inquireAssets(userId, accountNo);
    }

    /**
     * 기간별손익조회를 새 트랜잭션에서 실행.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public ProfitLossDto inquirePeriodProfitLossInNewTx(String userId, String accountNo, LocalDate startDate, LocalDate endDate) {
        return accountClient.inquirePeriodProfitLoss(userId, accountNo, startDate, endDate);
    }

    /**
     * 기간별매매손익현황조회를 새 트랜잭션에서 실행.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public PeriodProfitLossStatusDto inquirePeriodProfitLossStatusInNewTx(String userId, String accountNo,
            LocalDate startDate, LocalDate endDate) {
        return accountClient.inquirePeriodProfitLossStatus(userId, accountNo, startDate, endDate);
    }

    /**
     * 매수가능조회를 새 트랜잭션에서 실행.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public BuyableAmountDto inquireBuyableAmountInNewTx(String userId, String accountNo, String symbol, BigDecimal price) {
        return accountClient.inquireBuyableAmount(userId, accountNo, symbol, price);
    }

    /**
     * 매도가능수량조회를 새 트랜잭션에서 실행.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public SellableQuantityDto inquireSellableQuantityInNewTx(String userId, String accountNo, String symbol) {
        return accountClient.inquireSellableQuantity(userId, accountNo, symbol);
    }

    /**
     * 주문체결조회를 새 트랜잭션에서 실행.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<OrderHistoryDto> inquireOrderHistoryInNewTx(String userId, String accountNo, LocalDate startDate, LocalDate endDate) {
        return accountClient.inquireOrderHistory(userId, accountNo, startDate, endDate);
    }

    /**
     * 주식정정취소가능주문조회를 새 트랜잭션에서 실행.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<CancelableOrderDto> inquireCancelableOrdersInNewTx(String userId, String accountNo) {
        return accountClient.inquireCancelableOrders(userId, accountNo);
    }

    /**
     * 주식잔고조회_실현손익을 새 트랜잭션에서 실행.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public BalanceRealizedProfitLossDto inquireBalanceRealizedProfitLossInNewTx(String userId, String accountNo) {
        return accountClient.inquireBalanceRealizedProfitLoss(userId, accountNo);
    }
}
