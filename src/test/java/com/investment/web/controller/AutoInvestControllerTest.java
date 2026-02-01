package com.investment.web.controller;

import com.investment.account.dto.MainAccountResponseDto;
import com.investment.account.service.AccountService;
import com.investment.auth.service.AuthService;
import com.investment.common.security.JwtAuthenticationFilter;
import com.investment.common.security.RateLimitFilter;
import com.investment.config.SecurityHeadersConfig;
import com.investment.factor.dto.PipelineSummaryDto;
import com.investment.factor.service.PipelineSummaryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AutoInvestController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AutoInvestController")
class AutoInvestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;
    @MockBean
    private AccountService accountService;
    @MockBean
    private PipelineSummaryService pipelineSummaryService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private RateLimitFilter rateLimitFilter;
    @MockBean
    private SecurityHeadersConfig securityHeadersConfig;

    @Test
    @DisplayName("GET /auto-invest 파이프라인 요약 조회 후 auto-invest 뷰 반환")
    void autoInvest_returnsViewWithPipelineSummary() throws Exception {
        when(accountService.getDefaultAccountNo()).thenReturn("12345678-01");
        LocalDate basDt = LocalDate.now().minusDays(1);
        PipelineSummaryDto summary = PipelineSummaryDto.builder()
                .basDt(basDt)
                .universeCountKr(120L)
                .universeCountUs(80L)
                .signalCountKr(15L)
                .signalCountUs(10L)
                .openPositionCount(2)
                .signalListKr(Collections.emptyList())
                .signalListUs(Collections.emptyList())
                .openPositionList(Collections.emptyList())
                .build();
        when(pipelineSummaryService.getSummary(any(LocalDate.class), eq("12345678-01"))).thenReturn(summary);

        mockMvc.perform(get("/auto-invest"))
                .andExpect(status().isOk())
                .andExpect(view().name("auto-invest"))
                .andExpect(model().attributeExists("signalBasDt", "universeCountKr", "universeCountUs",
                        "signalCountKr", "signalCountUs", "signalListKr", "signalListUs",
                        "openPositionCount", "openPositionList"));

        verify(pipelineSummaryService).getSummary(any(LocalDate.class), eq("12345678-01"));
    }

    @Test
    @DisplayName("GET /auto-invest accountNo 파라미터 있으면 해당 계좌로 요약 조회")
    @WithMockUser(username = "user1")
    void autoInvest_withAccountNo_usesGivenAccount() throws Exception {
        when(accountService.getMainAccount("user1")).thenReturn(
                MainAccountResponseDto.builder().accountNo("99999999-99").build());
        when(pipelineSummaryService.getSummary(any(LocalDate.class), eq("99999999-99")))
                .thenReturn(PipelineSummaryDto.builder()
                        .basDt(LocalDate.now().minusDays(1))
                        .universeCountKr(0L)
                        .universeCountUs(0L)
                        .signalCountKr(0L)
                        .signalCountUs(0L)
                        .openPositionCount(0)
                        .signalListKr(Collections.emptyList())
                        .signalListUs(Collections.emptyList())
                        .openPositionList(Collections.emptyList())
                        .build());

        mockMvc.perform(get("/auto-invest").param("accountNo", "99999999-99"))
                .andExpect(status().isOk())
                .andExpect(view().name("auto-invest"));

        verify(pipelineSummaryService).getSummary(any(LocalDate.class), eq("99999999-99"));
    }
}
