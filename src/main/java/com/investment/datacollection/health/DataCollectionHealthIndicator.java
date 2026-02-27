package com.investment.datacollection.health;

import com.investment.config.DataCollectionProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

/**
 * 데이터 수집 설정 검증용 헬스 인디케이터.
 * US collector-url 또는 yfinance 경로, KRX AUTH_KEY 존재 여부만 노출(값은 노출하지 않음).
 * 미설정 시 health detail에 warn 메시지를 넣어 운영자가 확인할 수 있게 한다.
 */
@Slf4j
@Component
public class DataCollectionHealthIndicator extends AbstractHealthIndicator {

    private final DataCollectionProperties dataCollectionProperties;

    public DataCollectionHealthIndicator(DataCollectionProperties dataCollectionProperties) {
        this.dataCollectionProperties = dataCollectionProperties;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        boolean usConfigured = isUsCollectorConfigured();
        boolean krxConfigured = isKrxAuthConfigured();

        builder.up()
                .withDetail("usCollectorConfigured", usConfigured)
                .withDetail("krxAuthConfigured", krxConfigured);

        if (!usConfigured) {
            builder.withDetail("usCollectorWarn",
                    "US 일별 수집 스킵 가능: investment.data.us.collector-url 또는 yfinance-script-path 설정 필요");
        }
        if (!krxConfigured) {
            builder.withDetail("krxAuthWarn",
                    "KRX 일별 수집 스킵: KRX_AUTH_KEY(또는 investment.data.krx.auth-key) 설정 필요");
        }
    }

    private boolean isUsCollectorConfigured() {
        String url = dataCollectionProperties.getUs().getCollectorUrl();
        if (url != null && !url.isBlank()) {
            return true;
        }
        String path = dataCollectionProperties.getUs().getYfinanceScriptPath();
        return path != null && !path.isBlank();
    }

    private boolean isKrxAuthConfigured() {
        String key = dataCollectionProperties.getKrx().getAuthKey();
        return key != null && !key.isBlank();
    }
}
