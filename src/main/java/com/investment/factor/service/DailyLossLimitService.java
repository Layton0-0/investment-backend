package com.investment.factor.service;

import com.investment.account.dto.AccountBalanceDto;
import com.investment.account.dto.BalanceAndPositionsDto;
import com.investment.account.service.AccountService;
import com.investment.common.security.LogMaskingUtil;
import com.investment.config.RiskProperties;
import com.investment.domain.repository.TradingSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 일일 손실 한도 — 당일 시작 자산 대비 손실이 설정 % 초과 시 당일 신규 매수 중단 (전문 투자자 흐름 P0).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyLossLimitService {

    private final RiskProperties riskProperties;
    private final TradingSettingRepository tradingSettingRepository;
    private final AccountService accountService;

    /** 계좌별·일별 시초 평가액 (accountNo_date -> BigDecimal) */
    private final Map<String, BigDecimal> openingBalanceByAccountAndDate = new ConcurrentHashMap<>();

    /**
     * 당일 첫 파이프라인 실행 시 현재 평가액을 시초 평가액으로 기록.
     */
    public void recordOpeningBalanceIfAbsent(String accountNo, BigDecimal currentPortfolioValue) {
        if (accountNo == null || currentPortfolioValue == null) {
            return;
        }
        String key = key(accountNo, LocalDate.now());
        openingBalanceByAccountAndDate.putIfAbsent(key, currentPortfolioValue);
        log.debug("일일 손실 한도: 시초 평가액 기록 accountNo={}, value={}", LogMaskingUtil.maskAccountNo(accountNo),
                currentPortfolioValue);
    }

    /**
     * 당일 손실 한도 초과 여부. 초과 시 신규 매수 불가.
     * 시초 평가액 미기록 시 당일 첫 실행으로 간주하여 허용.
     *
     * @param accountNo 계좌번호
     * @return true면 신규 매수 허용, false면 당일 한도 초과로 스킵
     */
    public boolean isNewBuyAllowed(String accountNo) {
        if (accountNo == null) {
            return true;
        }
        BigDecimal limitPct = riskProperties.getDailyLossLimitPct();
        if (limitPct == null || limitPct.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        BigDecimal current = getCurrentPortfolioValue(accountNo);
        if (current == null || current.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        String key = key(accountNo, LocalDate.now());
        BigDecimal opening = openingBalanceByAccountAndDate.get(key);
        if (opening == null || opening.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        BigDecimal lossPct = opening.subtract(current).divide(opening, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        if (lossPct.compareTo(limitPct) >= 0) {
            log.warn("일일 손실 한도 초과: accountNo={}, opening={}, current={}, lossPct={}%, limit={}%",
                    LogMaskingUtil.maskAccountNo(accountNo), opening, current, lossPct, limitPct);
            return false;
        }
        return true;
    }

    /**
     * 계좌 현재 평가액 조회 (파이프라인용 userId로 API 호출).
     */
    public BigDecimal getCurrentPortfolioValue(String accountNo) {
        if (accountNo == null) {
            return null;
        }
        String userId = tradingSettingRepository.findByAccountNo(accountNo)
                .map(com.investment.domain.entity.TradingSetting::getUserId)
                .orElse(null);
        if (userId == null) {
            return null;
        }
        BalanceAndPositionsDto dto = accountService.getBalanceAndPositionsWithUserId(userId, accountNo);
        if (dto == null || dto.getBalance() == null) {
            return null;
        }
        AccountBalanceDto balance = dto.getBalance();
        if (balance.getTotalAssetValue() != null && balance.getTotalAssetValue().compareTo(BigDecimal.ZERO) >= 0) {
            return balance.getTotalAssetValue();
        }
        if (balance.getTotalBalance() != null && balance.getTotalBalance().compareTo(BigDecimal.ZERO) >= 0) {
            return balance.getTotalBalance();
        }
        return null;
    }

    /**
     * 계좌·일자 시초 평가액 조회 (리뷰·일일 PnL용).
     */
    public BigDecimal getOpeningBalance(String accountNo, LocalDate date) {
        if (accountNo == null || date == null) {
            return null;
        }
        return openingBalanceByAccountAndDate.get(key(accountNo, date));
    }

    private static String key(String accountNo, LocalDate date) {
        return accountNo + "_" + date.toString();
    }
}
