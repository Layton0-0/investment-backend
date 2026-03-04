package com.investment.setting.service;

import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.common.security.LogMaskingUtil;
import com.investment.config.TradingProperties;
import com.investment.domain.entity.TradingSetting;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.setting.dto.QuickStartRequestDto;
import com.investment.setting.dto.QuickStartResponseDto;
import com.investment.setting.dto.TradingSettingDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 거래 설정 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradingSettingService {

    private static final BigDecimal DEFAULT_SHORT = new BigDecimal("0.2");
    private static final BigDecimal DEFAULT_MEDIUM = new BigDecimal("0.4");
    private static final BigDecimal DEFAULT_LONG = new BigDecimal("0.4");

    private final TradingSettingRepository tradingSettingRepository;
    private final TradingProperties tradingProperties;

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
     * 거래 설정 저장/업데이트.
     * 최소 투자금액·전략 비중이 null이면 시스템 기본값 사용(퀀트 프로그램이 결정).
     */
    @Transactional
    public TradingSettingDto saveSetting(String accountNo, TradingSettingDto dto) {
        BigDecimal resolvedMin = dto.getMinInvestmentAmount() != null
                ? dto.getMinInvestmentAmount()
                : tradingProperties.getMinInvestmentAmount();
        BigDecimal shortR = dto.getShortTermRatio() != null ? dto.getShortTermRatio() : DEFAULT_SHORT;
        BigDecimal midR = dto.getMediumTermRatio() != null ? dto.getMediumTermRatio() : DEFAULT_MEDIUM;
        BigDecimal longR = dto.getLongTermRatio() != null ? dto.getLongTermRatio() : DEFAULT_LONG;

        log.info("거래 설정 저장: accountNo={}, maxAmount={}, minAmount={}",
                LogMaskingUtil.maskAccountNo(accountNo), dto.getMaxInvestmentAmount(), resolvedMin);

        validateRiskLevel(dto);
        validateSetting(dto.getMaxInvestmentAmount(), resolvedMin, shortR, midR, longR);

        TradingSetting setting = tradingSettingRepository.findByAccountNo(accountNo)
                .orElse(TradingSetting.builder()
                        .accountNo(accountNo)
                        .maxInvestmentAmount(dto.getMaxInvestmentAmount())
                        .minInvestmentAmount(resolvedMin)
                        .defaultCurrency(dto.getDefaultCurrency())
                        .autoTradingEnabled(dto.getAutoTradingEnabled() != null ? dto.getAutoTradingEnabled() : false)
                        .roboAdvisorEnabled(dto.getRoboAdvisorEnabled() != null && dto.getRoboAdvisorEnabled())
                        .riskLevel(dto.getRiskLevel())
                        .shortTermRatio(shortR)
                        .mediumTermRatio(midR)
                        .longTermRatio(longR)
                        .pipelineAutoExecute(dto.getPipelineAutoExecute() != null ? dto.getPipelineAutoExecute() : Boolean.FALSE)
                        .pipelineAllowRealExecution(dto.getPipelineAllowRealExecution() != null && dto.getPipelineAllowRealExecution())
                        .build());

        if (setting.getId() != null && !setting.getId().isEmpty()) {
            setting.updateMaxInvestmentAmount(dto.getMaxInvestmentAmount());
            if (dto.getMinInvestmentAmount() != null) {
                setting.updateMinInvestmentAmount(dto.getMinInvestmentAmount());
            }
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
            // null이면 기존 비율 유지(시스템 기본값은 스케줄러에서 적용)
            if (dto.getPipelineAutoExecute() != null) {
                setting.updatePipelineAutoExecute(dto.getPipelineAutoExecute());
            }
            if (dto.getPipelineAllowRealExecution() != null) {
                setting.updatePipelineAllowRealExecution(dto.getPipelineAllowRealExecution());
            }
        }

        setting = tradingSettingRepository.save(setting);
        return convertToDto(setting);
    }

    /**
     * 원클릭 자동투자 시작: 초보자 디폴트(autoTradingEnabled=true, pipelineAutoExecute=true, 균형 비율)로 설정 생성/갱신.
     * userId가 있으면 신규 생성 시 엔티티에 설정.
     */
    @Transactional
    public QuickStartResponseDto quickStart(String userId, QuickStartRequestDto request) {
        String accountNo = resolveAccountNoForQuickStart(userId, request.getAccountNo());
        TradingSettingDto dto = TradingSettingDto.builder()
                .maxInvestmentAmount(request.getMaxInvestmentAmount())
                .minInvestmentAmount(tradingProperties.getMinInvestmentAmount())
                .defaultCurrency("KRW")
                .autoTradingEnabled(true)
                .roboAdvisorEnabled(false)
                .shortTermRatio(DEFAULT_SHORT)
                .mediumTermRatio(DEFAULT_MEDIUM)
                .longTermRatio(DEFAULT_LONG)
                .pipelineAutoExecute(true)
                .pipelineAllowRealExecution(null)
                .build();
        TradingSettingDto saved = saveSettingWithUserId(accountNo, userId, dto);
        log.info("quick-start 완료: accountNo={}, maxAmount={}", LogMaskingUtil.maskAccountNo(accountNo), request.getMaxInvestmentAmount());
        return QuickStartResponseDto.builder()
                .success(true)
                .message("자동투자가 시작되었습니다.")
                .setting(saved)
                .build();
    }

    private String resolveAccountNoForQuickStart(String userId, String requestAccountNo) {
        if (requestAccountNo != null && !requestAccountNo.isBlank()) {
            tradingSettingRepository.findByAccountNo(requestAccountNo.trim())
                    .filter(s -> userId.equals(s.getUserId()))
                    .orElseThrow(() -> new DomainException(ErrorCode.SETTING_NOT_FOUND, "해당 계좌 설정을 찾을 수 없습니다"));
            return requestAccountNo.trim();
        }
        List<TradingSetting> list = tradingSettingRepository.findByUserIdOrderByAccountNo(userId);
        if (list.isEmpty()) {
            throw new DomainException(ErrorCode.SETTING_NOT_FOUND, "계좌를 먼저 연결해 주세요. 설정 화면에서 계좌를 등록한 뒤 다시 시도해 주세요.");
        }
        return list.get(0).getAccountNo();
    }

    @Transactional
    public TradingSettingDto saveSettingWithUserId(String accountNo, String userId, TradingSettingDto dto) {
        BigDecimal resolvedMin = dto.getMinInvestmentAmount() != null
                ? dto.getMinInvestmentAmount()
                : tradingProperties.getMinInvestmentAmount();
        BigDecimal shortR = dto.getShortTermRatio() != null ? dto.getShortTermRatio() : DEFAULT_SHORT;
        BigDecimal midR = dto.getMediumTermRatio() != null ? dto.getMediumTermRatio() : DEFAULT_MEDIUM;
        BigDecimal longR = dto.getLongTermRatio() != null ? dto.getLongTermRatio() : DEFAULT_LONG;
        validateSetting(dto.getMaxInvestmentAmount(), resolvedMin, shortR, midR, longR);

        TradingSetting setting = tradingSettingRepository.findByAccountNo(accountNo)
                .orElse(TradingSetting.builder()
                        .accountNo(accountNo)
                        .maxInvestmentAmount(dto.getMaxInvestmentAmount())
                        .minInvestmentAmount(resolvedMin)
                        .defaultCurrency(dto.getDefaultCurrency())
                        .autoTradingEnabled(dto.getAutoTradingEnabled() != null ? dto.getAutoTradingEnabled() : false)
                        .roboAdvisorEnabled(dto.getRoboAdvisorEnabled() != null && dto.getRoboAdvisorEnabled())
                        .riskLevel(dto.getRiskLevel())
                        .shortTermRatio(shortR)
                        .mediumTermRatio(midR)
                        .longTermRatio(longR)
                        .pipelineAutoExecute(dto.getPipelineAutoExecute() != null ? dto.getPipelineAutoExecute() : Boolean.FALSE)
                        .pipelineAllowRealExecution(dto.getPipelineAllowRealExecution() != null && dto.getPipelineAllowRealExecution())
                        .build());
        if (setting.getUserId() == null && userId != null) {
            setting.setUserId(userId);
        }
        if (setting.getId() != null && !setting.getId().isEmpty()) {
            setting.updateMaxInvestmentAmount(dto.getMaxInvestmentAmount());
            if (dto.getMinInvestmentAmount() != null) {
                setting.updateMinInvestmentAmount(dto.getMinInvestmentAmount());
            }
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
            if (dto.getPipelineAutoExecute() != null) {
                setting.updatePipelineAutoExecute(dto.getPipelineAutoExecute());
            }
            if (dto.getPipelineAllowRealExecution() != null) {
                setting.updatePipelineAllowRealExecution(dto.getPipelineAllowRealExecution());
            }
        }
        setting = tradingSettingRepository.save(setting);
        return convertToDto(setting);
    }

    private void validateRiskLevel(TradingSettingDto dto) {
        if (dto.getRiskLevel() != null) {
            if (dto.getRiskLevel().compareTo(BigDecimal.ZERO) < 0
                    || dto.getRiskLevel().compareTo(BigDecimal.ONE) > 0) {
                throw new DomainException(ErrorCode.INVALID_SETTING_VALUE,
                        "리스크 레벨은 0.0 ~ 1.0 사이의 값이어야 합니다");
            }
        }
    }

    /**
     * 설정 검증 (저장 시 사용하는 해소된 값 기준)
     */
    private void validateSetting(BigDecimal maxAmount, BigDecimal minAmount,
                                 BigDecimal shortR, BigDecimal midR, BigDecimal longR) {
        if (maxAmount.compareTo(minAmount) < 0) {
            throw new DomainException(ErrorCode.INVALID_SETTING_VALUE,
                    "최대 투자금액은 최소 투자금액보다 크거나 같아야 합니다");
        }
        BigDecimal sum = shortR.add(midR).add(longR);
        if (sum.compareTo(BigDecimal.ONE) != 0) {
            throw new DomainException(ErrorCode.INVALID_SETTING_VALUE,
                    "단기·중기·장기 비율의 합은 1이어야 합니다 (현재 합: " + sum + ")");
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
                .pipelineAutoExecute(setting.getPipelineAutoExecute())
                .pipelineAllowRealExecution(setting.getPipelineAllowRealExecution())
                .build();
    }
}
