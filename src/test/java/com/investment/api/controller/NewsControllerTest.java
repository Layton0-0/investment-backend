package com.investment.api.controller;

import com.investment.common.security.JwtAuthenticationFilter;
import com.investment.common.security.RateLimitFilter;
import com.investment.config.SecurityHeadersConfig;
import com.investment.news.dto.NewsItemPageResponseDto;
import com.investment.news.service.NewsItemService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NewsController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("NewsController")
class NewsControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private NewsItemService newsItemService;
        @MockBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;
        @MockBean
        private RateLimitFilter rateLimitFilter;
        @MockBean
        private SecurityHeadersConfig securityHeadersConfig;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        @DisplayName("GET /api/v1/news 뉴스 목록 조회 성공")
        void getNews_returnsOk() throws Exception {
                NewsItemPageResponseDto response = NewsItemPageResponseDto.builder()
                                .content(Collections.emptyList())
                                .page(NewsItemPageResponseDto.PageMeta.builder()
                                                .number(0)
                                                .size(20)
                                                .totalElements(0)
                                                .totalPages(0)
                                                .build())
                                .build();
                when(newsItemService.getNewsItems(any(), any(), any(), any(), any(), eq(0), eq(20)))
                                .thenReturn(response);

                mockMvc.perform(get("/api/v1/news")
                                .param("page", "0")
                                .param("size", "20"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.page.number").value(0))
                                .andExpect(jsonPath("$.page.size").value(20));
        }

        @Test
        @DisplayName("GET /api/v1/news market 파라미터 전달 시 서비스에 전달한다")
        void getNews_withMarket_callsServiceWithMarket() throws Exception {
                NewsItemPageResponseDto response = NewsItemPageResponseDto.builder()
                                .content(Collections.emptyList())
                                .page(NewsItemPageResponseDto.PageMeta.builder().number(0).size(20).totalElements(0)
                                                .totalPages(0).build())
                                .build();
                when(newsItemService.getNewsItems(eq("KR"), any(), any(), any(), any(), anyInt(), anyInt()))
                                .thenReturn(response);

                mockMvc.perform(get("/api/v1/news").param("market", "KR"))
                                .andExpect(status().isOk());
        }
}
