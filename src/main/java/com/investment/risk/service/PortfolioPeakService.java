package com.investment.risk.service;

import com.investment.domain.entity.PortfolioPeak;
import com.investment.domain.repository.PortfolioPeakRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * 계좌별 평가액 피크 관리. MDD(최대 낙폭) 계산용.
 */
@Service
@RequiredArgsConstructor
public class PortfolioPeakService {

    private final PortfolioPeakRepository portfolioPeakRepository;

    /**
     * 현재 평가액으로 피크 갱신 후 MDD 반환.
     * MDD = (peak - current) / peak. peak가 0이면 0 반환.
     *
     * @param accountNo    계좌번호
     * @param currentValue 현재 총 평가액
     * @param asOfDate     기준일
     * @return MDD (0~1, 예: 0.15 = 15%)
     */
    @Transactional
    public BigDecimal getOrUpdatePeakAndComputeMdd(String accountNo, BigDecimal currentValue, LocalDate asOfDate) {
        if (currentValue == null || currentValue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        PortfolioPeak peak = portfolioPeakRepository.findById(accountNo)
                .orElseGet(() -> {
                    PortfolioPeak p = PortfolioPeak.of(accountNo, currentValue, asOfDate);
                    return portfolioPeakRepository.save(p);
                });
        peak.updateIfHigher(currentValue, asOfDate);
        portfolioPeakRepository.save(peak);

        BigDecimal peakVal = peak.getPeakValue();
        if (peakVal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        // MDD = (peak - current) / peak
        BigDecimal mdd = peakVal.subtract(currentValue).divide(peakVal, 6, RoundingMode.HALF_UP);
        return mdd.max(BigDecimal.ZERO);
    }
}
