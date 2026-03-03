package com.investment.setting.service;

import com.investment.common.exception.DomainException;
import com.investment.domain.entity.SystemSetting;
import com.investment.domain.repository.SystemSettingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SystemSettingService")
class SystemSettingServiceTest {

    @Mock
    private SystemSettingRepository systemSettingRepository;
    @Mock
    private Environment environment;

    @InjectMocks
    private SystemSettingService systemSettingService;

    @Test
    @DisplayName("getBoolean - DB에 값 있으면 DB 값 반환")
    void getBoolean_whenDbHasValue_returnsDbValue() {
        when(systemSettingRepository.findById("pipeline.autoExecute"))
                .thenReturn(Optional.of(SystemSetting.builder()
                        .key("pipeline.autoExecute")
                        .value("false")
                        .build()));
        Boolean result = systemSettingService.getBoolean("pipeline.autoExecute");
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("getBoolean - DB 없으면 Environment fallback")
    void getBoolean_whenDbEmpty_usesEnvironmentFallback() {
        when(systemSettingRepository.findById("pipeline.autoExecute")).thenReturn(Optional.empty());
        when(environment.getProperty(eq("investment.pipeline.auto-execute"), eq("false"))).thenReturn("true");
        Boolean result = systemSettingService.getBoolean("pipeline.autoExecute");
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("getBoolean - 허용되지 않은 키면 예외")
    void getBoolean_invalidKey_throws() {
        assertThatThrownBy(() -> systemSettingService.getBoolean("invalid.key"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("허용되지 않은 시스템 설정 키");
    }

    @Test
    @DisplayName("getBigDecimal - DB에 값 있으면 DB 값 반환")
    void getBigDecimal_whenDbHasValue_returnsDbValue() {
        when(systemSettingRepository.findById("pipeline.scheduler.defaultCapital"))
                .thenReturn(Optional.of(SystemSetting.builder()
                        .key("pipeline.scheduler.defaultCapital")
                        .value("100000000")
                        .build()));
        BigDecimal result = systemSettingService.getBigDecimal("pipeline.scheduler.defaultCapital");
        assertThat(result).isEqualByComparingTo(new BigDecimal("100000000"));
    }

    @Test
    @DisplayName("getBigDecimal - DB 없으면 Environment fallback")
    void getBigDecimal_whenDbEmpty_usesEnvironmentFallback() {
        when(systemSettingRepository.findById("pipeline.scheduler.defaultCapital")).thenReturn(Optional.empty());
        when(environment.getProperty("investment.pipeline.scheduler.default-capital", "0")).thenReturn("0");
        BigDecimal result = systemSettingService.getBigDecimal("pipeline.scheduler.defaultCapital");
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("listAll - 허용 키 목록 + DB/effective 반환")
    void listAll_returnsAllowedKeysWithDbAndEffective() {
        when(systemSettingRepository.findAllByKeyIn(any())).thenReturn(List.of(
                SystemSetting.builder().key("pipeline.autoExecute").value("true").build()
        ));
        when(environment.getProperty(anyString(), anyString())).thenAnswer(inv -> {
            String k = inv.getArgument(0);
            if (k != null && k.contains("auto-execute")) return "true";
            if (k != null && k.contains("allow-real")) return "false";
            if (k != null && k.contains("default-capital")) return "0";
            if (k != null && k.contains("trading-window")) return "true";
            if (k != null && k.contains("governance")) return inv.getArgument(1);
            if (k != null && k.contains("batch.failure")) return "false";
            if (k != null && k.contains("regime-gate")) return "false";
            if (k != null && k.contains("breakout-enabled")) return "false";
            return inv.getArgument(1);
        });
        var list = systemSettingService.listAll();
        assertThat(list).isNotEmpty();
        assertThat(list.stream().anyMatch(d -> "pipeline.autoExecute".equals(d.key()))).isTrue();
    }

    @Test
    @DisplayName("put - 허용 키면 저장")
    void put_allowedKey_saves() {
        when(systemSettingRepository.findById("pipeline.autoExecute")).thenReturn(Optional.empty());
        when(systemSettingRepository.save(any(SystemSetting.class))).thenAnswer(i -> i.getArgument(0));
        systemSettingService.put("pipeline.autoExecute", "true", "admin-1");
        verify(systemSettingRepository).save(any(SystemSetting.class));
    }

    @Test
    @DisplayName("put - 허용되지 않은 키면 예외")
    void put_invalidKey_throws() {
        assertThatThrownBy(() -> systemSettingService.put("invalid.key", "x", "admin-1"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("허용되지 않은 시스템 설정 키");
    }

    @Test
    @DisplayName("put - Boolean 타입에 잘못된 값이면 예외")
    void put_booleanInvalidValue_throws() {
        assertThatThrownBy(() -> systemSettingService.put("pipeline.autoExecute", "yes", "admin-1"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("true");
    }
}
