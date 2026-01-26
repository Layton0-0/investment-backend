package com.investment.setting.service;

import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.domain.entity.TradingSetting;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.setting.dto.TradingSettingDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 거래 설정 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradingSettingService {
    
    private final TradingSettingRepository tradingSettingRepository;
    
    /**
     * 거래 설정 조회
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
     * 거래 설정 저장/업데이트
     */
    @Transactional
    public TradingSettingDto saveSetting(String accountNo, TradingSettingDto dto) {
        log.info("거래 설정 저장: accountNo={}, maxAmount={}, minAmount={}", 
                accountNo, dto.getMaxInvestmentAmount(), dto.getMinInvestmentAmount());
        
        validateSetting(dto);
        
        TradingSetting setting = tradingSettingRepository.findByAccountNo(accountNo)
                .orElse(TradingSetting.builder()
                        .accountNo(accountNo)
                        .maxInvestmentAmount(dto.getMaxInvestmentAmount())
                        .minInvestmentAmount(dto.getMinInvestmentAmount())
                        .defaultCurrency(dto.getDefaultCurrency())
                        .autoTradingEnabled(dto.getAutoTradingEnabled() != null ? dto.getAutoTradingEnabled() : false)
                        .riskLevel(dto.getRiskLevel())
                        .build());
        
        if (setting.getId() != null && !setting.getId().isEmpty()) {
            // 업데이트
            setting.updateMaxInvestmentAmount(dto.getMaxInvestmentAmount());
            setting.updateMinInvestmentAmount(dto.getMinInvestmentAmount());
            setting.updateAutoTradingEnabled(dto.getAutoTradingEnabled() != null ? dto.getAutoTradingEnabled() : false);
            if (dto.getRiskLevel() != null) {
                setting.updateRiskLevel(dto.getRiskLevel());
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
            if (dto.getRiskLevel().compareTo(java.math.BigDecimal.ZERO) < 0 || 
                dto.getRiskLevel().compareTo(java.math.BigDecimal.ONE) > 0) {
                throw new DomainException(ErrorCode.INVALID_SETTING_VALUE, 
                        "리스크 레벨은 0.0 ~ 1.0 사이의 값이어야 합니다");
            }
        }
    }
    
    private TradingSettingDto convertToDto(TradingSetting setting) {
        return TradingSettingDto.builder()
                .maxInvestmentAmount(setting.getMaxInvestmentAmount())
                .minInvestmentAmount(setting.getMinInvestmentAmount())
                .defaultCurrency(setting.getDefaultCurrency())
                .autoTradingEnabled(setting.getAutoTradingEnabled())
                .riskLevel(setting.getRiskLevel())
                .build();
    }
}
