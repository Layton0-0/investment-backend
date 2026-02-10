package com.investment.api.controller;

import com.investment.account.service.AccountService;
import com.investment.common.security.JwtAuthenticationFilter;
import com.investment.common.security.RateLimitFilter;
import com.investment.config.SecurityHeadersConfig;
import com.investment.ops.dto.AlertItemDto;
import com.investment.ops.dto.AlertListResponseDto;
import com.investment.ops.service.OpsAlertsService;
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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OpsAlertsController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("OpsAlertsController")
class OpsAlertsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OpsAlertsService opsAlertsService;
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
    @DisplayName("GET /api/v1/ops/alerts ADMIN 역할 시 200 및 페이징 본문 반환")
    @WithMockUser(username = "admin1", roles = "ADMIN")
    void getAlerts_withAdmin_returnsOk() throws Exception {
        AlertListResponseDto dto = AlertListResponseDto.builder()
                .items(List.of(
                        AlertItemDto.builder()
                                .id(1L)
                                .occurredAt("2026-02-10T12:00:00+09:00")
                                .level("WARNING")
                                .component("UnfilledOrder")
                                .message("미체결 알림")
                                .build()))
                .page(0)
                .size(20)
                .totalElements(1L)
                .totalPages(1)
                .build();
        when(opsAlertsService.getAlerts(anyInt(), anyInt(), any())).thenReturn(dto);

        mockMvc.perform(get("/api/v1/ops/alerts").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].level").value("WARNING"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
