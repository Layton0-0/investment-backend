package com.investment.factor.scheduler;

import com.investment.alert.EmergencyAlertService;
import com.investment.domain.entity.BrokerType;
import com.investment.domain.entity.Order;
import com.investment.domain.entity.UserAccount;
import com.investment.domain.repository.OrderRepository;
import com.investment.domain.repository.UserAccountRepository;
import com.investment.common.security.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 미체결 주문 확인 스케줄러.
 * PENDING 상태로 N분 이상 경과한 주문에 대해 Discord 긴급 알림 발송.
 * (향후 한투 API 주문체결/미체결 조회 연동 시 실제 미체결 수량 반영)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnfilledOrderCheckScheduler {

    private static final String BROKER_NAME = "한국투자증권";

    private final OrderRepository orderRepository;
    private final EmergencyAlertService emergencyAlertService;
    private final UserAccountRepository userAccountRepository;
    private final EncryptionUtil encryptionUtil;

    @Value("${investment.pipeline.unfilled-check-minutes:1}")
    private int unfilledCheckMinutes = 1;

    @Value("${investment.pipeline.alert-base-url:}")
    private String alertBaseUrl;

    @Value("${investment.pipeline.alert-discord-webhook-url:}")
    private String alertDiscordWebhookUrl;

    /** Spring Batch Job에서 호출. */
    @Transactional(readOnly = true)
    public void checkUnfilledOrders() {
        if (alertDiscordWebhookUrl == null || alertDiscordWebhookUrl.isBlank()) {
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(unfilledCheckMinutes);
        List<Order> pending = orderRepository.findByStatusAndOrderTimeBefore(Order.OrderStatus.PENDING, cutoff);
        if (pending.isEmpty()) {
            return;
        }
        for (Order order : pending) {
            try {
                String userId = order.getUserId();
                String accountNo = order.getAccountNo();
                String serverType = resolveServerType(userId, accountNo);
                int outstandingQty = order.getExecutedQuantity() != null
                        ? Math.max(0, order.getQuantity() - order.getExecutedQuantity())
                        : order.getQuantity();
                int elapsedMin = (int) java.time.Duration.between(order.getOrderTime(), LocalDateTime.now())
                        .toMinutes();
                emergencyAlertService.sendUnfilledAlert(
                        order.getId(),
                        order.getSymbol(),
                        outstandingQty,
                        elapsedMin,
                        userId,
                        accountNo,
                        serverType,
                        BROKER_NAME,
                        alertBaseUrl);
            } catch (Exception e) {
                log.warn("미체결 알림 발송 실패: orderId={}, error={}", order.getId(), e.getMessage());
            }
        }
    }

    private String resolveServerType(String userId, String accountNo) {
        if (userId == null || accountNo == null || accountNo.trim().isEmpty()) {
            return "1";
        }
        List<UserAccount> accounts = userAccountRepository.findByUserIdAndBrokerType(userId,
                BrokerType.KOREA_INVESTMENT);
        for (UserAccount account : accounts) {
            try {
                String decrypted = encryptionUtil.decrypt(account.getAccountNoEncrypted());
                if (accountNo.trim().equals(decrypted)) {
                    return account.getServerType() != null ? account.getServerType() : "1";
                }
            } catch (Exception e) {
                log.trace("계좌번호 복호화 스킵: accountId={}", account.getId());
            }
        }
        return "1";
    }
}
