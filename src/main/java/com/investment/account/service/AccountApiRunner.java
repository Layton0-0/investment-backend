package com.investment.account.service;

import com.investment.account.client.KoreaInvestmentAccountClient;
import com.investment.account.dto.AccountPositionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
     * 해외 잔고 조회를 새 트랜잭션에서 실행. 실패 시 빈 목록 반환.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<AccountPositionDto> inquireOverseasBalanceInNewTx(String userId, String accountNo) {
        try {
            return accountClient.inquireOverseasBalance(userId, accountNo);
        } catch (Exception e) {
            log.debug("해외 잔고 조회 스킵: accountNo={}, error={}", accountNo, e.getMessage());
            return new ArrayList<>();
        }
    }
}
