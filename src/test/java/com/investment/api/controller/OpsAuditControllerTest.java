package com.investment.api.controller;

import com.investment.account.service.AccountService;
import com.investment.common.security.JwtAuthenticationFilter;
import com.investment.common.security.RateLimitFilter;
import com.investment.config.SecurityHeadersConfig;
import com.investment.ops.dto.AuditLogItemDto;
import com.investment.ops.dto.AuditLogListResponseDto;
import com.investment.ops.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OpsAuditController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("OpsAuditController")
class OpsAuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditLogService auditLogService;
    @MockBean
    private AccountService accountService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private RateLimitFilter rateLimitFilter;
    @MockBean
    private SecurityHeadersConfig securityHeadersConfig;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/v1/ops/audit ADMIN 역할 시 200 및 페이징 본문 반환")
    @WithMockUser(username = "admin1", roles = "ADMIN")
    void getAuditLogs_withAdmin_returnsOk() throws Exception {
        AuditLogListResponseDto dto = AuditLogListResponseDto.builder()
                .items(List.of(
                        AuditLogItemDto.builder()
                                .id(1L)
                                .occurredAt("2026-02-10T12:00:00+09:00")
                                .eventType("SETTING_CHANGE")
                                .userIdMasked("ab***")
                                .accountNoMasked("****-12")
                                .summary("거래 설정 저장")
                                .result("SUCCESS")
                                .build()))
                .page(0)
                .size(20)
                .totalElements(1L)
                .totalPages(1)
                .build();
        when(auditLogService.findPage(anyInt(), anyInt(), any(), any(), any())).thenReturn(dto);

        mockMvc.perform(get("/api/v1/ops/audit").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].eventType").value("SETTING_CHANGE"))
                .andExpect(jsonPath("$.items[0].userIdMasked").value("ab***"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/ops/audit eventType 쿼리 전달")
    @WithMockUser(username = "admin1", roles = "ADMIN")
    void getAuditLogs_withEventType_passesFilter() throws Exception {
        when(auditLogService.findPage(0, 20, "MANUAL_TRIGGER", null, null))
                .thenReturn(AuditLogListResponseDto.builder().items(List.of()).page(0).size(20).totalElements(0L).totalPages(0).build());

        mockMvc.perform(get("/api/v1/ops/audit")
                        .param("page", "0")
                        .param("size", "20")
                        .param("eventType", "MANUAL_TRIGGER"))
                .andExpect(status().isOk());
    }
}
