package com.investment.strategy.engine;

import com.investment.config.RiskProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 거시경제 지표(VIX 등) 제공 — 설정된 URL에서 JSON 조회.
 * investment.risk.macro-indicator-url 미설정 시 Optional.empty() 반환.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultMacroIndicatorProvider implements MacroIndicatorProvider {

    private final RiskProperties riskProperties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public Optional<MacroEconomicStrategyEngine.MacroEconomicIndicators> getCurrentIndicators() {
        String url = riskProperties.getMacroIndicatorUrl();
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }
        try {
            String json = restTemplate.getForObject(url, String.class);
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(json);
            MacroEconomicStrategyEngine.MacroEconomicIndicators.MacroEconomicIndicatorsBuilder builder = MacroEconomicStrategyEngine.MacroEconomicIndicators
                    .builder();
            if (root.has("vix") && !root.get("vix").isNull()) {
                builder.vix(new BigDecimal(root.get("vix").asText()));
            }
            if (root.has("interestRate") && !root.get("interestRate").isNull()) {
                builder.interestRate(new BigDecimal(root.get("interestRate").asText()));
            }
            if (root.has("inflationRate") && !root.get("inflationRate").isNull()) {
                builder.inflationRate(new BigDecimal(root.get("inflationRate").asText()));
            }
            if (root.has("gdpGrowthRate") && !root.get("gdpGrowthRate").isNull()) {
                builder.gdpGrowthRate(new BigDecimal(root.get("gdpGrowthRate").asText()));
            }
            return Optional.of(builder.build());
        } catch (Exception e) {
            log.warn("거시경제 지표 조회 실패: url={}, error={}", url, e.getMessage());
            return Optional.empty();
        }
    }
}
