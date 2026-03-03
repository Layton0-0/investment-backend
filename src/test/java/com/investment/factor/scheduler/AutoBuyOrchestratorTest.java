package com.investment.factor.scheduler;

import com.investment.backtest.robo.RoboRebalanceScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutoBuyOrchestrator")
class AutoBuyOrchestratorTest {

    @Mock
    private RoboRebalanceScheduler roboRebalanceScheduler;

    @Mock
    private PipelineExecutionScheduler pipelineExecutionScheduler;

    private AutoBuyOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new AutoBuyOrchestrator(roboRebalanceScheduler, pipelineExecutionScheduler);
    }

    @Test
    @DisplayName("run 호출 시 로보 먼저, 파이프라인 다음 순서로 실행 (실제 주문 여부는 각 스케줄러가 DB에서 조회)")
    void run_invokesRoboThenPipeline() {
        orchestrator.run();

        InOrder order = inOrder(roboRebalanceScheduler, pipelineExecutionScheduler);
        order.verify(roboRebalanceScheduler).runNow();
        order.verify(pipelineExecutionScheduler).runNow(null);
    }
}
