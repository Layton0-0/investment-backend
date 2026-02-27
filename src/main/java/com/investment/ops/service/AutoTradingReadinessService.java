package com.investment.ops.service;

import com.investment.domain.repository.DailyStockRepository;
import com.investment.domain.repository.SignalScoreRepository;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.governance.GovernanceHaltService;
import com.investment.ops.dto.AutoTradingReadinessDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 자동매매 준비 상태 조회.
 * 09:10 자동매수 실행 전 관리자 점검용: 자동투자 ON 계좌 수, 전일 일봉·시그널 건수, 활성 halt 수.
 */
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
        long signalScoreRowCount = signalScoreRepository.countByBasDt(basDt);
        int activeGovernanceHaltCount = governanceHaltService.getActiveHalts().size();

        return AutoTradingReadinessDto.builder()
                .basDt(basDt)
                .autoTradingOnAccountCount(autoTradingOnAccountCount)
                .dailyStockRowCount(dailyStockRowCount)
                .signalScoreRowCount(signalScoreRowCount)
                .activeGovernanceHaltCount(activeGovernanceHaltCount)
                .build();
    }
}
