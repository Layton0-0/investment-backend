package com.investment.backtest.robo;

import com.investment.backtest.robo.dto.RoboBacktestResult;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 실행 전 백테스트 결과 저장소 (인메모리). 로보 리밸런싱 스케줄러가 결과를 기록하고, API/UI에서 조회.
 */
@Component
public class RoboPreExecutionResultStore {

    private final Map<String, StoredResult> byAccount = new ConcurrentHashMap<>();

    public void store(String accountNo, RoboBacktestResult result, boolean passed) {
        byAccount.put(accountNo, new StoredResult(result, passed, Instant.now()));
    }

    public StoredResult get(String accountNo) {
        return byAccount.get(accountNo);
    }

    @Getter
    @RequiredArgsConstructor
    public static class StoredResult {
        private final RoboBacktestResult result;
        private final boolean passed;
        private final Instant runAt;

        public BigDecimal getMddPct() {
            return result != null ? result.getMddPct() : null;
        }

        public BigDecimal getSharpeRatio() {
            return result != null ? result.getSharpeRatio() : null;
        }
    }
}
