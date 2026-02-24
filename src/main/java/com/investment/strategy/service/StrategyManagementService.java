package com.investment.strategy.service;

import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.common.security.LogMaskingUtil;
import com.investment.domain.entity.Strategy;
import com.investment.domain.entity.TradingSetting;
import com.investment.domain.repository.StrategyRepository;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.strategy.domain.StrategyStatus;
import com.investment.strategy.domain.StrategyType;
import com.investment.strategy.dto.StrategyDto;
import com.investment.strategy.dto.StrategyStatusUpdateDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 전략 관리 서비스.
 * 계좌+시장별 조회 시 전략이 없으면 시스템 기본(단기/중기/장기 3건)을 자동 생성하여 조회 중심 UX를 보장한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyManagementService {

    private static final String DEFAULT_MARKET = "KR";
    private static final BigDecimal DEFAULT_SHORT_RATIO = new BigDecimal("0.2");
    private static final BigDecimal DEFAULT_MEDIUM_RATIO = new BigDecimal("0.4");
    private static final BigDecimal DEFAULT_LONG_RATIO = new BigDecimal("0.4");
    private static final BigDecimal DEFAULT_RISK_LEVEL = new BigDecimal("1.0");
    private static final BigDecimal DEFAULT_CONFIDENCE_THRESHOLD = new BigDecimal("0.7");

    private final StrategyRepository strategyRepository;
    private final TradingSettingRepository tradingSettingRepository;

    /**
     * 계좌의 모든 전략 조회 (시장 미지정 시 전체). 시장별 ensure는 적용하지 않음.
     */
    @Transactional(readOnly = true)
    public List<StrategyDto> getStrategies(String accountNo) {
        return getStrategies(accountNo, null);
    }

    /**
     * 계좌·시장별 전략 조회 (market null이면 전체).
     * 시장이 지정된 경우 해당 계좌+시장에 전략이 하나도 없으면 시스템 기본 3건(단기/중기/장기)을 생성한 뒤 반환한다.
     */
    @Transactional
    public List<StrategyDto> getStrategies(String accountNo, String market) {
        List<Strategy> strategies = (market != null && !market.isBlank())
                ? strategyRepository.findByAccountNoAndMarket(accountNo, market)
                : strategyRepository.findByAccountNo(accountNo);

        if (strategies.isEmpty() && market != null && !market.isBlank()) {
            ensureDefaultStrategies(accountNo, market);
            strategies = strategyRepository.findByAccountNoAndMarket(accountNo, market);
        }

        return strategies.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 계좌+시장에 대해 단기/중기/장기 시스템 기본 전략이 없으면 생성한다.
     * TradingSetting이 있으면 maxInvestmentAmount·비율로 배분 금액을 설정하고, 없으면 금액은 null(파이프라인 기본 사용).
     */
    private void ensureDefaultStrategies(String accountNo, String market) {
        String m = market != null && !market.isBlank() ? market : DEFAULT_MARKET;
        BigDecimal maxTotal = null;
        BigDecimal shortRatio = DEFAULT_SHORT_RATIO;
        BigDecimal mediumRatio = DEFAULT_MEDIUM_RATIO;
        BigDecimal longRatio = DEFAULT_LONG_RATIO;
        String userId = null;

        var settingOpt = tradingSettingRepository.findByAccountNo(accountNo);
        if (settingOpt.isPresent()) {
            TradingSetting setting = settingOpt.get();
            maxTotal = setting.getMaxInvestmentAmount();
            userId = setting.getUserId();
            if (setting.getShortTermRatio() != null) {
                shortRatio = setting.getShortTermRatio();
            }
            if (setting.getMediumTermRatio() != null) {
                mediumRatio = setting.getMediumTermRatio();
            }
            if (setting.getLongTermRatio() != null) {
                longRatio = setting.getLongTermRatio();
            }
        }

        for (StrategyType type : List.of(StrategyType.SHORT_TERM, StrategyType.MEDIUM_TERM, StrategyType.LONG_TERM)) {
            if (strategyRepository.findByAccountNoAndMarketAndStrategyType(accountNo, m, type).isPresent()) {
                continue;
            }
            BigDecimal ratio = type == StrategyType.SHORT_TERM ? shortRatio
                    : type == StrategyType.MEDIUM_TERM ? mediumRatio : longRatio;
            BigDecimal maxAmount = null;
            if (maxTotal != null && maxTotal.compareTo(BigDecimal.ZERO) > 0 && ratio != null) {
                maxAmount = maxTotal.multiply(ratio).setScale(0, RoundingMode.DOWN);
            }
            Strategy strategy = Strategy.builder()
                    .accountNo(accountNo)
                    .userId(userId)
                    .market(m)
                    .strategyType(type)
                    .status(StrategyStatus.ACTIVE)
                    .maxInvestmentAmount(maxAmount)
                    .minInvestmentAmount(BigDecimal.ZERO)
                    .riskLevel(DEFAULT_RISK_LEVEL)
                    .confidenceThreshold(DEFAULT_CONFIDENCE_THRESHOLD)
                    .build();
            strategyRepository.save(strategy);
            log.info("시스템 기본 전략 생성: accountNo={}, market={}, strategyType={}",
                    LogMaskingUtil.maskAccountNo(accountNo), m, type);
        }
    }

    /**
     * 특정 전략 조회 (market 미지정 시 KR)
     */
    @Transactional(readOnly = true)
    public StrategyDto getStrategy(String accountNo, StrategyType strategyType) {
        return getStrategy(accountNo, DEFAULT_MARKET, strategyType);
    }

    /**
     * 특정 전략 조회 (시장 지정, 미지정 시 KR)
     */
    @Transactional(readOnly = true)
    public StrategyDto getStrategy(String accountNo, String market, StrategyType strategyType) {
        String m = (market != null && !market.isBlank()) ? market : DEFAULT_MARKET;
        Strategy strategy = strategyRepository.findByAccountNoAndMarketAndStrategyType(accountNo, m, strategyType)
                .orElseThrow(() -> new DomainException(
                        ErrorCode.SETTING_NOT_FOUND,
                        String.format("전략을 찾을 수 없습니다: accountNo=%s, market=%s, strategyType=%s", accountNo, m,
                                strategyType)));
        return convertToDto(strategy);
    }

    /**
     * 전략 생성 또는 업데이트
     */
    @Transactional
    public StrategyDto saveStrategy(StrategyDto dto) {
        String market = (dto.getMarket() != null && !dto.getMarket().isBlank()) ? dto.getMarket() : DEFAULT_MARKET;
        log.info("전략 저장: accountNo={}, market={}, strategyType={}", LogMaskingUtil.maskAccountNo(dto.getAccountNo()),
                market, dto.getStrategyType());

        Strategy strategy = strategyRepository.findByAccountNoAndMarketAndStrategyType(
                dto.getAccountNo(), market, dto.getStrategyType())
                .orElse(Strategy.builder()
                        .accountNo(dto.getAccountNo())
                        .market(market)
                        .strategyType(dto.getStrategyType())
                        .status(dto.getStatus() != null ? dto.getStatus() : StrategyStatus.ACTIVE)
                        .maxInvestmentAmount(dto.getMaxInvestmentAmount())
                        .minInvestmentAmount(dto.getMinInvestmentAmount())
                        .riskLevel(dto.getRiskLevel())
                        .confidenceThreshold(dto.getConfidenceThreshold())
                        .build());

        if (strategy.getId() != null && !strategy.getId().isEmpty()) {
            // 업데이트
            if (dto.getMaxInvestmentAmount() != null) {
                strategy.updateMaxInvestmentAmount(dto.getMaxInvestmentAmount());
            }
            if (dto.getMinInvestmentAmount() != null) {
                strategy.updateMinInvestmentAmount(dto.getMinInvestmentAmount());
            }
            if (dto.getRiskLevel() != null) {
                strategy.updateRiskLevel(dto.getRiskLevel());
            }
            if (dto.getConfidenceThreshold() != null) {
                strategy.updateConfidenceThreshold(dto.getConfidenceThreshold());
            }
        }

        strategy = strategyRepository.save(strategy);
        return convertToDto(strategy);
    }

    /**
     * 전략 상태 업데이트 (market 미지정 시 KR)
     */
    @Transactional
    public StrategyDto updateStrategyStatus(String accountNo, String market, StrategyType strategyType,
            StrategyStatusUpdateDto dto) {
        String m = (market != null && !market.isBlank()) ? market : DEFAULT_MARKET;
        log.info("전략 상태 업데이트: accountNo={}, market={}, strategyType={}, status={}",
                accountNo, m, strategyType, dto.getStatus());

        Strategy strategy = strategyRepository.findByAccountNoAndMarketAndStrategyType(accountNo, m, strategyType)
                .orElseThrow(() -> new DomainException(
                        ErrorCode.SETTING_NOT_FOUND,
                        String.format("전략을 찾을 수 없습니다: accountNo=%s, market=%s, strategyType=%s", accountNo, m,
                                strategyType)));

        switch (dto.getStatus()) {
            case ACTIVE:
                strategy.activate();
                break;
            case STOPPED:
                strategy.stop();
                break;
            case PAUSED:
                strategy.pause();
                break;
        }

        strategy = strategyRepository.save(strategy);
        return convertToDto(strategy);
    }

    /**
     * 전략 활성화 (market 미지정 시 KR)
     */
    @Transactional
    public StrategyDto activateStrategy(String accountNo, String market, StrategyType strategyType) {
        String m = (market != null && !market.isBlank()) ? market : DEFAULT_MARKET;
        log.info("전략 활성화: accountNo={}, market={}, strategyType={}", LogMaskingUtil.maskAccountNo(accountNo), m,
                strategyType);

        Strategy strategy = strategyRepository.findByAccountNoAndMarketAndStrategyType(accountNo, m, strategyType)
                .orElseThrow(() -> new DomainException(
                        ErrorCode.SETTING_NOT_FOUND,
                        String.format("전략을 찾을 수 없습니다: accountNo=%s, market=%s, strategyType=%s", accountNo, m,
                                strategyType)));

        strategy.activate();
        strategy = strategyRepository.save(strategy);
        return convertToDto(strategy);
    }

    /**
     * 전략 중지 (market 미지정 시 KR)
     */
    @Transactional
    public StrategyDto stopStrategy(String accountNo, String market, StrategyType strategyType) {
        String m = (market != null && !market.isBlank()) ? market : DEFAULT_MARKET;
        log.info("전략 중지: accountNo={}, market={}, strategyType={}", accountNo, m, strategyType);

        Strategy strategy = strategyRepository.findByAccountNoAndMarketAndStrategyType(accountNo, m, strategyType)
                .orElseThrow(() -> new DomainException(
                        ErrorCode.SETTING_NOT_FOUND,
                        String.format("전략을 찾을 수 없습니다: accountNo=%s, market=%s, strategyType=%s", accountNo, m,
                                strategyType)));

        strategy.stop();
        strategy = strategyRepository.save(strategy);
        return convertToDto(strategy);
    }

    /**
     * 엔티티를 DTO로 변환
     */
    private StrategyDto convertToDto(Strategy strategy) {
        BigDecimal successRate = null;
        if (strategy.getTotalExecutions() != null && strategy.getTotalExecutions() > 0) {
            successRate = BigDecimal.valueOf(strategy.getSuccessCount())
                    .divide(BigDecimal.valueOf(strategy.getTotalExecutions()), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return StrategyDto.builder()
                .strategyId(strategy.getId())
                .accountNo(strategy.getAccountNo())
                .market(strategy.getMarket())
                .strategyType(strategy.getStrategyType())
                .status(strategy.getStatus())
                .maxInvestmentAmount(strategy.getMaxInvestmentAmount())
                .minInvestmentAmount(strategy.getMinInvestmentAmount())
                .riskLevel(strategy.getRiskLevel())
                .confidenceThreshold(strategy.getConfidenceThreshold())
                .lastExecutedAt(strategy.getLastExecutedAt())
                .totalExecutions(strategy.getTotalExecutions())
                .successCount(strategy.getSuccessCount())
                .failureCount(strategy.getFailureCount())
                .totalProfitLoss(strategy.getTotalProfitLoss())
                .successRate(successRate)
                .createdAt(strategy.getCreatedAt())
                .updatedAt(strategy.getUpdatedAt())
                .build();
    }
}
