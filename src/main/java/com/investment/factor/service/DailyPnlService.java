package com.investment.factor.service;

import com.investment.common.security.LogMaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * 일일 PnL 집계·리뷰 (전문 투자자 흐름 P3).
 * 당일 시초 평가액 대비 현재 평가액·손익률을 집계하고 로그로 기록.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyPnlService {

    private final DailyLossLimitService dailyLossLimitService;

    /**
     * 계좌별 당일 PnL 집계 후 로그 기록.
     * 시초 평가액은 DailyLossLimitService에 기록된 값 사용.
     *
     * @param accountNo 계좌번호
     * @param date      기준일
     * @return 일일 수익률(%) 또는 기록 실패 시 null
     */
    public BigDecimal recordDailyPnl(String accountNo, LocalDate date) {
        if (accountNo == null || date == null) {
            return null;
        }
        BigDecimal opening = dailyLossLimitService.getOpeningBalance(accountNo, date);
        BigDecimal closing = dailyLossLimitService.getCurrentPortfolioValue(accountNo);
        if (opening == null || opening.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("일일 PnL 스킵: 시초 평가액 없음, accountNo={}, date={}",
                    LogMaskingUtil.maskAccountNo(accountNo), date);
            return null;
        }
        if (closing == null) {
            log.debug("일일 PnL 스킵: 현재 평가액 조회 실패, accountNo={}, date={}",
                    LogMaskingUtil.maskAccountNo(accountNo), date);
            return null;
        }
        BigDecimal pnlPct = closing.subtract(opening).divide(opening, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        log.info("일일 PnL: accountNo={}, date={}, opening={}, closing={}, pnlPct={}%",
                LogMaskingUtil.maskAccountNo(accountNo), date, opening, closing, pnlPct);
        return pnlPct;
    }
}
