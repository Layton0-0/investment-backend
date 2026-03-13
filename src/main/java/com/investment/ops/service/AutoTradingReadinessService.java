package com.investment.ops.service;

import com.investment.domain.repository.DailyStockRepository;
import com.investment.domain.repository.SignalScoreRepository;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.governance.GovernanceHaltService;
import com.investment.ops.dto.AutoTradingReadinessDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 자동매매 준비 상태 조회.
 * 09:10 자동매수 실행 전 관리자 점검용: 자동투자 ON 계좌 수, 전일 일봉·시그널 건수, 활성 halt 수.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoTradingReadinessService {

    private final TradingSettingRepository tradingSettingRepository;
    private final DailyStockRepository dailyStockRepository;
    private final SignalScoreRepository signalScoreRepository;
    private final GovernanceHaltService governanceHaltService;

    /**
     * 기준일(basDt)은 파이프라인과 동일하게 전일.
     */
    @Transactional(readOnly = true)
    public AutoTradingReadinessDto getReadiness() {
        LocalDate basDt = LocalDate.now().minusDays(1);
        long autoTradingOnAccountCount = tradingSettingRepository.countByAutoTradingEnabledTrue();
        long dailyStockRowCount = dailyStockRepository.countByBasDt(basDt);
        long dailyStockRowCountKr = dailyStockRepository.countByBasDtAndMarket(basDt, "KR");
        long dailyStockRowCountUs = dailyStockRepository.countByBasDtAndMarket(basDt, "US");
        long signalScoreRowCount = signalScoreRepository.countByBasDt(basDt);
        long signalScoreRowCountKr = signalScoreRepository.countByBasDtAndMarket(basDt, "KR");
        long signalScoreRowCountUs = signalScoreRepository.countByBasDtAndMarket(basDt, "US");
        int activeGovernanceHaltCount = governanceHaltService.getActiveHalts().size();

        log.info("자동매매 준비상태 조회: basDt={}, autoTradingOn={}, dailyStock(KR={}, US={}), signalScore(KR={}, US={}), activeHalts={}",
                basDt, autoTradingOnAccountCount, dailyStockRowCountKr, dailyStockRowCountUs,
                signalScoreRowCountKr, signalScoreRowCountUs, activeGovernanceHaltCount);

        return AutoTradingReadinessDto.builder()
                .basDt(basDt)
                .autoTradingOnAccountCount(autoTradingOnAccountCount)
                .dailyStockRowCount(dailyStockRowCount)
                .dailyStockRowCountKr(dailyStockRowCountKr)
                .dailyStockRowCountUs(dailyStockRowCountUs)
                .signalScoreRowCount(signalScoreRowCount)
                .signalScoreRowCountKr(signalScoreRowCountKr)
                .signalScoreRowCountUs(signalScoreRowCountUs)
                .activeGovernanceHaltCount(activeGovernanceHaltCount)
                .build();
    }
}
