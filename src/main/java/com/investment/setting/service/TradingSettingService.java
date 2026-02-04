package com.investment.setting.service;

import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.common.security.LogMaskingUtil;
import com.investment.domain.entity.TradingSetting;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.setting.dto.TradingSettingDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 거래 설정 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradingSettingService {

    private final TradingSettingRepository tradingSettingRepository;

    /**
     * 거래 설정 조회 (없으면 예외). API용.
     */
    @Transactional(readOnly = true)
    public TradingSettingDto getSetting(String accountNo) {
        TradingSetting setting = tradingSettingRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> new DomainException(
                        ErrorCode.SETTING_NOT_FOUND,
                        "거래 설정을 찾을 수 없습니다: " + accountNo));

        return convertToDto(setting);
    }

    /**
     * 거래 설정 선택 조회 (없으면 empty). 대시보드 등 선택 표시용.
     */
    @Transactional(readOnly = true)
    public Optional<TradingSettingDto> getSettingOptional(String accountNo) {
        return tradingSettingRepository.findByAccountNo(accountNo)
                .map(this::convertToDto);
    }

    /**
     * 거래 설정 저장/업데이트
     */
    @Transactional
    public TradingSettingDto saveSetting(String accountNo, TradingSettingDto dto) {
        log.info("거래 설정 저장: accountNo={}, maxAmount={}, minAmount={}",
                LogMaskingUtil.maskAccountNo(accountNo), dto.getMaxInvestmentAmount(), dto.getMinInvestmentAmount());

        validateSetting(dto);

        TradingSetting setting = tradingSettingRepository.findByAccountNo(accountNo)
                .orElse(TradingSetting.builder()
                        .accountNo(accountNo)
                        .maxInvestmentAmount(dto.getMaxInvestmentAmount())
                        .minInvestmentAmount(dto.getMinInvestmentAmount())
                        .defaultCurrency(dto.getDefaultCurrency())
                        .autoTradingEnabled(dto.getAutoTradingEnabled() != null ? dto.getAutoTradingEnabled() : false)
                        .roboAdvisorEnabled(dto.getRoboAdvisorEnabled())
                        .riskLevel(dto.getRiskLevel())
                        .shortTermRatio(dto.getShortTermRatio())
                        .mediumTermRatio(dto.getMediumTermRatio())
                        .longTermRatio(dto.getLongTermRatio())
                        .build());

        if (setting.getId() != null && !setting.getId().isEmpty()) {
            setting.updateMaxInvestmentAmount(dto.getMaxInvestmentAmount());
            setting.updateMinInvestmentAmount(dto.getMinInvestmentAmount());
            setting.updateAutoTradingEnabled(dto.getAutoTradingEnabled() != null ? dto.getAutoTradingEnabled() : false);
            if (dto.getRoboAdvisorEnabled() != null) {
                setting.updateRoboAdvisorEnabled(dto.getRoboAdvisorEnabled());
            }
            if (dto.getRiskLevel() != null) {
                setting.updateRiskLevel(dto.getRiskLevel());
            }
            if (dto.getShortTermRatio() != null && dto.getMediumTermRatio() != null && dto.getLongTermRatio() != null) {
                setting.updateStrategyRatios(dto.getShortTermRatio(), dto.getMediumTermRatio(), dto.getLongTermRatio());
            }
        }

        setting = tradingSettingRepository.save(setting);
        return convertToDto(setting);
    }

    /**
     * 설정 검증
     */
    private void validateSetting(TradingSettingDto dto) {
        if (dto.getMaxInvestmentAmount().compareTo(dto.getMinInvestmentAmount()) < 0) {
            throw new DomainException(ErrorCode.INVALID_SETTING_VALUE,
                    "최대 투자금액은 최소 투자금액보다 크거나 같아야 합니다");
        }

        if (dto.getRiskLevel() != null) {
            if (dto.getRiskLevel().compareTo(BigDecimal.ZERO) < 0 ||
                    dto.getRiskLevel().compareTo(BigDecimal.ONE) > 0) {
                throw new DomainException(ErrorCode.INVALID_SETTING_VALUE,
                        "리스크 레벨은 0.0 ~ 1.0 사이의 값이어야 합니다");
            }
        }

        if (dto.getShortTermRatio() != null || dto.getMediumTermRatio() != null || dto.getLongTermRatio() != null) {
            BigDecimal s = dto.getShortTermRatio() != null ? dto.getShortTermRatio() : BigDecimal.ZERO;
            BigDecimal m = dto.getMediumTermRatio() != null ? dto.getMediumTermRatio() : BigDecimal.ZERO;
            BigDecimal l = dto.getLongTermRatio() != null ? dto.getLongTermRatio() : BigDecimal.ZERO;
            if (s.compareTo(BigDecimal.ZERO) < 0 || s.compareTo(BigDecimal.ONE) > 0
                    || m.compareTo(BigDecimal.ZERO) < 0 || m.compareTo(BigDecimal.ONE) > 0
                    || l.compareTo(BigDecimal.ZERO) < 0 || l.compareTo(BigDecimal.ONE) > 0) {
                throw new DomainException(ErrorCode.INVALID_SETTING_VALUE,
                        "단기/중기/장기 비율은 각각 0~1 사이여야 합니다");
            }
            BigDecimal sum = s.add(m).add(l);
            if (sum.compareTo(BigDecimal.ONE) != 0) {
                throw new DomainException(ErrorCode.INVALID_SETTING_VALUE,
                        "단기·중기·장기 비율의 합은 1이어야 합니다 (현재 합: " + sum + ")");
            }
        }
    }

    private TradingSettingDto convertToDto(TradingSetting setting) {
        return TradingSettingDto.builder()
                .maxInvestmentAmount(setting.getMaxInvestmentAmount())
                .minInvestmentAmount(setting.getMinInvestmentAmount())
                .defaultCurrency(setting.getDefaultCurrency())
                .autoTradingEnabled(setting.getAutoTradingEnabled())
                .roboAdvisorEnabled(setting.getRoboAdvisorEnabled())
                .riskLevel(setting.getRiskLevel())
                .shortTermRatio(setting.getShortTermRatio())
                .mediumTermRatio(setting.getMediumTermRatio())
                .longTermRatio(setting.getLongTermRatio())
                .build();
    }
}
