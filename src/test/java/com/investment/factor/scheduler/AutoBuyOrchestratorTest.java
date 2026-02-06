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
    @DisplayName("run 호출 시 로보 먼저, 파이프라인 다음 순서로 실행")
    void run_invokesRoboThenPipeline() {
        orchestrator.run(true);

        InOrder order = inOrder(roboRebalanceScheduler, pipelineExecutionScheduler);
        order.verify(roboRebalanceScheduler).runNow(true);
        order.verify(pipelineExecutionScheduler).runNow(true);
    }

    @Test
    @DisplayName("run(false) 시 dryRun false로 양쪽 전달")
    void run_withFalse_passesFalseToBoth() {
        orchestrator.run(false);

        verify(roboRebalanceScheduler).runNow(false);
        verify(pipelineExecutionScheduler).runNow(false);
    }

    @Test
    @DisplayName("run(null) 시 로보는 true, 파이프라인은 null 전달")
    void run_withNull_passesNullToPipeline() {
        orchestrator.run(null);

        verify(roboRebalanceScheduler).runNow(true);
        verify(pipelineExecutionScheduler).runNow(null);
    }
}
