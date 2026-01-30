package com.investment.datacollection.client;

import com.investment.config.DataCollectionProperties;
import com.investment.datacollection.dto.SecEdgarItemDto;
import com.investment.datacollection.dto.SecSubmissionsResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecEdgarApiClient")
class SecEdgarApiClientTest {

    @Mock
    private DataCollectionProperties dataCollectionProperties;

    @Mock
    private DataCollectionProperties.Sec secProperties;

    @Mock
    private RestTemplate restTemplate;

    @Test
    @DisplayName("apiKey가 비어 있으면 빈 리스트를 반환한다")
    void fetchRecentFilings_whenApiKeyBlank_returnsEmpty() {
        when(dataCollectionProperties.getSec()).thenReturn(secProperties);
        when(secProperties.getApiKey()).thenReturn("");

        SecEdgarApiClient client = new SecEdgarApiClient(dataCollectionProperties, restTemplate);
        List<SecEdgarItemDto> result = client.fetchRecentFilings(3);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(SecSubmissionsResponseDto.class));
    }

    @Test
    @DisplayName("apiKey가 null이면 빈 리스트를 반환한다")
    void fetchRecentFilings_whenApiKeyNull_returnsEmpty() {
        when(dataCollectionProperties.getSec()).thenReturn(secProperties);
        when(secProperties.getApiKey()).thenReturn(null);

        SecEdgarApiClient client = new SecEdgarApiClient(dataCollectionProperties, restTemplate);
        List<SecEdgarItemDto> result = client.fetchRecentFilings(3);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(SecSubmissionsResponseDto.class));
    }

    @Test
    @DisplayName("API 응답이 있으면 파싱된 항목을 반환한다")
    void fetchRecentFilings_whenApiReturnsData_returnsParsedItems() {
        when(dataCollectionProperties.getSec()).thenReturn(secProperties);
        when(secProperties.getApiKey()).thenReturn("test-key");
        when(secProperties.getBaseUrl()).thenReturn("https://data.sec.gov");

        SecSubmissionsResponseDto response = new SecSubmissionsResponseDto();
        response.setCik("0000320193");
        response.setName("Apple Inc.");
        SecSubmissionsResponseDto.RecentFilings recent = new SecSubmissionsResponseDto.RecentFilings();
        recent.setAccessionNumber(List.of("0000320193-26-000005"));
        recent.setForm(List.of("8-K"));
        recent.setFilingDate(List.of("2026-01-29"));
        recent.setPrimaryDocument(List.of("0000320193-26-000005.htm"));
        SecSubmissionsResponseDto.FilingsWrapper filings = new SecSubmissionsResponseDto.FilingsWrapper();
        filings.setRecent(recent);
        response.setFilings(filings);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(SecSubmissionsResponseDto.class)))
                .thenReturn(ResponseEntity.ok(response));

        SecEdgarApiClient client = new SecEdgarApiClient(dataCollectionProperties, restTemplate);
        List<SecEdgarItemDto> result = client.fetchRecentFilings(30);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        // Client calls 3 CIKs (Apple, Microsoft, Amazon); each returns 1 item with same stub
        assertTrue(result.size() >= 1);
        SecEdgarItemDto first = result.get(0);
        assertEquals("0000320193-26-000005", first.getAccessionNumber());
        assertEquals("8-K", first.getForm());
        assertEquals("Apple Inc.", first.getCompanyName());
        assertEquals("0000320193", first.getCik());
    }

    @Test
    @DisplayName("buildDocumentUrl은 올바른 SEC Archives URL을 반환한다")
    void buildDocumentUrl_returnsCorrectUrl() {
        String url = SecEdgarApiClient.buildDocumentUrl("0000320193", "0000320193-26-000005", "0000320193-26-000005.htm");
        assertTrue(url.contains("www.sec.gov"));
        assertTrue(url.contains("Archives/edgar/data"));
        assertTrue(url.contains("0000320193"));
        assertTrue(url.contains("000032019326000005"));
    }
}
