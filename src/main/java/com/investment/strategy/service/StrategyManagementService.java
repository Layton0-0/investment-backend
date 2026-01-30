package com.investment.strategy.service;

import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.common.security.LogMaskingUtil;
import com.investment.domain.entity.Strategy;
import com.investment.domain.repository.StrategyRepository;
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
 * 전략 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyManagementService {

    private final StrategyRepository strategyRepository;

    private static final String DEFAULT_MARKET = "KR";

    /**
     * 계좌의 모든 전략 조회 (시장 미지정 시 전체)
     */
    @Transactional(readOnly = true)
    public List<StrategyDto> getStrategies(String accountNo) {
        return getStrategies(accountNo, null);
    }

    /**
     * 계좌·시장별 전략 조회 (market null이면 전체)
     */
    @Transactional(readOnly = true)
    public List<StrategyDto> getStrategies(String accountNo, String market) {
        List<Strategy> strategies = (market != null && !market.isBlank())
                ? strategyRepository.findByAccountNoAndMarket(accountNo, market)
                : strategyRepository.findByAccountNo(accountNo);
        return strategies.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
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
