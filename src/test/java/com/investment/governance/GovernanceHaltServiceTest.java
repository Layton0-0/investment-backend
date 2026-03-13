package com.investment.governance;

import com.investment.domain.entity.GovernanceCheckResult;
import com.investment.domain.entity.GovernanceHalt;
import com.investment.domain.repository.GovernanceCheckResultRepository;
import com.investment.domain.repository.GovernanceHaltRepository;
import com.investment.ops.dto.GovernanceCheckResultDto;
import com.investment.ops.dto.GovernanceHaltDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GovernanceHaltService")
class GovernanceHaltServiceTest {

    @Mock
    private GovernanceHaltRepository governanceHaltRepository;
    @Mock
    private GovernanceCheckResultRepository governanceCheckResultRepository;

    @InjectMocks
    private GovernanceHaltService governanceHaltService;

    @Test
    @DisplayName("isHalted 없으면 false")
    void isHalted_empty_returnsFalse() {
        when(governanceHaltRepository.findByMarketAndStrategyType("KR", "SHORT_TERM")).thenReturn(Optional.empty());

        boolean result = governanceHaltService.isHalted("KR", "SHORT_TERM");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isHalted cleared면 false")
    void isHalted_cleared_returnsFalse() {
        GovernanceHalt halt = GovernanceHalt.create("KR", "SHORT_TERM", "reason");
        halt.clear("admin");
        when(governanceHaltRepository.findByMarketAndStrategyType("KR", "SHORT_TERM")).thenReturn(Optional.of(halt));

        boolean result = governanceHaltService.isHalted("KR", "SHORT_TERM");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isHalted 활성 halt면 true")
    void isHalted_active_returnsTrue() {
        GovernanceHalt halt = GovernanceHalt.create("KR", "SHORT_TERM", "reason");
        when(governanceHaltRepository.findByMarketAndStrategyType("KR", "SHORT_TERM")).thenReturn(Optional.of(halt));

        boolean result = governanceHaltService.isHalted("KR", "SHORT_TERM");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("setHalt 없으면 저장")
    void setHalt_new_saves() {
        when(governanceHaltRepository.findByMarketAndStrategyType("KR", "MEDIUM_TERM")).thenReturn(Optional.empty());

        governanceHaltService.setHalt("KR", "MEDIUM_TERM", "MDD degraded");

        ArgumentCaptor<GovernanceHalt> captor = ArgumentCaptor.forClass(GovernanceHalt.class);
        verify(governanceHaltRepository).save(captor.capture());
        assertThat(captor.getValue().getMarket()).isEqualTo("KR");
        assertThat(captor.getValue().getStrategyType()).isEqualTo("MEDIUM_TERM");
        assertThat(captor.getValue().getReason()).isEqualTo("MDD degraded");
        assertThat(captor.getValue().isActive()).isTrue();
    }

    @Test
    @DisplayName("clearHalt 활성 halt면 clear 호출")
    void clearHalt_active_clears() {
        GovernanceHalt halt = GovernanceHalt.create("US", "SHORT_TERM", "reason");
        when(governanceHaltRepository.findByMarketAndStrategyType("US", "SHORT_TERM")).thenReturn(Optional.of(halt));

        governanceHaltService.clearHalt("US", "SHORT_TERM", "admin1");

        assertThat(halt.getClearedAt()).isNotNull();
        assertThat(halt.getClearedBy()).isEqualTo("admin1");
        verify(governanceHaltRepository).save(halt);
    }

    @Test
    @DisplayName("getActiveHalts CLEARED_AT IS NULL 목록 반환")
    void getActiveHalts_returnsList() {
        GovernanceHalt h = GovernanceHalt.create("KR", "SHORT_TERM", "r");
        when(governanceHaltRepository.findByClearedAtIsNull()).thenReturn(List.of(h));

        List<GovernanceHaltDto> result = governanceHaltService.getActiveHalts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMarket()).isEqualTo("KR");
        assertThat(result.get(0).getStrategyType()).isEqualTo("SHORT_TERM");
    }

    @Test
    @DisplayName("getRecentResults limit 적용 조회")
    void getRecentResults_returnsList() {
        GovernanceCheckResult r = GovernanceCheckResult.of(
                Instant.now(), "KR", "SHORT_TERM",
                new BigDecimal("-20"), new BigDecimal("0.5"), true,
                LocalDate.now().minusMonths(12), LocalDate.now().minusDays(1));
        when(governanceCheckResultRepository.findAllByOrderByRunAtDesc(PageRequest.of(0, 20)))
                .thenReturn(List.of(r));

        List<GovernanceCheckResultDto> result = governanceHaltService.getRecentResults(20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMarket()).isEqualTo("KR");
        assertThat(result.get(0).getStrategyType()).isEqualTo("SHORT_TERM");
        assertThat(result.get(0).isDegraded()).isTrue();
        assertThat(result.get(0).getPassed()).isFalse();
        assertThat(result.get(0).getMessage()).isEqualTo("Degraded");
    }

    @Test
    @DisplayName("getRecentResults limit 500 초과 시 500으로 캡")
    void getRecentResults_limitOver500_capsTo500() {
        when(governanceCheckResultRepository.findAllByOrderByRunAtDesc(PageRequest.of(0, 500)))
                .thenReturn(List.of());

        List<GovernanceCheckResultDto> result = governanceHaltService.getRecentResults(1000);

        assertThat(result).isEmpty();
        verify(governanceCheckResultRepository).findAllByOrderByRunAtDesc(PageRequest.of(0, 500));
    }

    @Test
    @DisplayName("clearHalt halt 없으면 no-op")
    void clearHalt_noHalt_doesNothing() {
        when(governanceHaltRepository.findByMarketAndStrategyType("KR", "LONG_TERM")).thenReturn(Optional.empty());

        governanceHaltService.clearHalt("KR", "LONG_TERM", "admin1");

        verify(governanceHaltRepository).findByMarketAndStrategyType("KR", "LONG_TERM");
        verify(governanceHaltRepository, never()).save(any());
    }

    @Test
    @DisplayName("clearHalt 이미 cleared면 no-op")
    void clearHalt_alreadyCleared_doesNothing() {
        GovernanceHalt halt = GovernanceHalt.create("US", "MEDIUM_TERM", "reason");
        halt.clear("admin0");
        when(governanceHaltRepository.findByMarketAndStrategyType("US", "MEDIUM_TERM")).thenReturn(Optional.of(halt));

        governanceHaltService.clearHalt("US", "MEDIUM_TERM", "admin1");

        verify(governanceHaltRepository).findByMarketAndStrategyType("US", "MEDIUM_TERM");
        verify(governanceHaltRepository, never()).save(any());
    }
}
