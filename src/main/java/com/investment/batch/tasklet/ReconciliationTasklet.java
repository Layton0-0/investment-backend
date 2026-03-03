package com.investment.batch.tasklet;

import com.investment.alert.EmergencyAlertService;
import com.investment.common.security.LogMaskingUtil;
import com.investment.domain.entity.TradingSetting;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.ops.dto.ReconciliationResultDto;
import com.investment.ops.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 정기 포지션 정합성(Reconciliation) 배치.
 * 자동투자 ON 계좌별로 브로커 vs DB 비교 후 불일치 시 Discord 알림.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReconciliationTasklet implements Tasklet {

    private static final String COMPONENT_RECONCILIATION = "ReconciliationMismatch";

    @Value("${investment.reconciliation.enabled:true}")
    private boolean reconciliationEnabled;

    @Value("${investment.reconciliation.alert-on-mismatch:true}")
    private boolean alertOnMismatch;

    private final TradingSettingRepository tradingSettingRepository;
    private final ReconciliationService reconciliationService;
    private final EmergencyAlertService emergencyAlertService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        if (!reconciliationEnabled) {
            log.debug("Reconciliation 배치 비활성화됨");
            return RepeatStatus.FINISHED;
        }
        List<TradingSetting> settings = tradingSettingRepository.findAllByAutoTradingEnabledTrue();
        if (settings == null || settings.isEmpty()) {
            log.trace("Reconciliation: 자동투자 ON 계좌 없음");
            return RepeatStatus.FINISHED;
        }
        for (TradingSetting setting : settings) {
            try {
                String userId = setting.getUserId();
                String accountNo = setting.getAccountNo();
                if (userId == null || accountNo == null || accountNo.isBlank()) {
                    continue;
                }
                ReconciliationResultDto result = reconciliationService.reconcile(userId, accountNo);
                if (alertOnMismatch && result.getSummary().isHasDiscrepancy()) {
                    String message = buildAlertMessage(result);
                    emergencyAlertService.sendRiskEventAlert("WARNING", COMPONENT_RECONCILIATION, message);
                    log.warn("Reconciliation 불일치 알림 발송: accountNo={}", LogMaskingUtil.maskAccountNo(accountNo));
                }
            } catch (Exception e) {
                log.warn("Reconciliation 실패: accountNo={}, error={}",
                        LogMaskingUtil.maskAccountNo(setting.getAccountNo()), e.getMessage());
            }
        }
        return RepeatStatus.FINISHED;
    }

    private String buildAlertMessage(ReconciliationResultDto result) {
        ReconciliationResultDto.ReconciliationSummaryDto s = result.getSummary();
        StringBuilder sb = new StringBuilder();
        sb.append("** [정합성] 브로커-DB 포지션 불일치 **\n");
        sb.append("계좌: ").append(LogMaskingUtil.maskAccountNo(result.getAccountNo())).append("\n");
        sb.append("수량 불일치: ").append(s.getMismatchCount()).append("건, ");
        sb.append("DB전용: ").append(s.getOnlyInDbCount()).append("건, ");
        sb.append("브로커전용: ").append(s.getOnlyInBrokerCount()).append("건\n");
        if (!result.getMismatchItems().isEmpty()) {
            result.getMismatchItems().stream().limit(5).forEach(m ->
                    sb.append("- ").append(m.getSymbol()).append(" ").append(m.getMarket())
                            .append(" DB=").append(m.getDbQuantity()).append(" 브로커=").append(m.getBrokerQuantity()).append("\n"));
        }
        return sb.toString();
    }
}
