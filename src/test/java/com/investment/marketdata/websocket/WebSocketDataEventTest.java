package com.investment.marketdata.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WebSocketDataEvent")
class WebSocketDataEventTest {

    @Nested
    @DisplayName("sessionKey 파싱")
    class SessionKeyParsingTests {

        @Test
        @DisplayName("정상적인 sessionKey에서 userId 추출")
        void getUserId_validSessionKey_returnsUserId() {
            WebSocketDataEvent event = new WebSocketDataEvent(
                    this, "user123|1", "H0STASP0", "005930", "data");
            
            assertThat(event.getUserId()).isEqualTo("user123");
        }

        @Test
        @DisplayName("정상적인 sessionKey에서 serverType 추출")
        void getServerType_validSessionKey_returnsServerType() {
            WebSocketDataEvent event = new WebSocketDataEvent(
                    this, "user123|0", "H0STASP0", "005930", "data");
            
            assertThat(event.getServerType()).isEqualTo("0");
        }

        @Test
        @DisplayName("sessionKey null일 때 빈 userId 반환")
        void getUserId_nullSessionKey_returnsEmpty() {
            WebSocketDataEvent event = new WebSocketDataEvent(
                    this, null, "H0STASP0", "005930", "data");
            
            assertThat(event.getUserId()).isEmpty();
        }

        @Test
        @DisplayName("sessionKey에 구분자 없을 때 기본 serverType 반환")
        void getServerType_noDelimiter_returnsDefault() {
            WebSocketDataEvent event = new WebSocketDataEvent(
                    this, "user123", "H0STASP0", "005930", "data");
            
            assertThat(event.getServerType()).isEqualTo("1");
        }
    }

    @Nested
    @DisplayName("TR_ID 구분")
    class TrIdTests {

        @Test
        @DisplayName("H0STASP0은 호가 데이터")
        void isQuoteData_H0STASP0_returnsTrue() {
            WebSocketDataEvent event = new WebSocketDataEvent(
                    this, "user|1", "H0STASP0", "005930", "data");
            
            assertThat(event.isQuoteData()).isTrue();
            assertThat(event.isExecutionData()).isFalse();
            assertThat(event.isCcnlNotice()).isFalse();
        }

        @Test
        @DisplayName("H0STCNT0은 체결 데이터")
        void isExecutionData_H0STCNT0_returnsTrue() {
            WebSocketDataEvent event = new WebSocketDataEvent(
                    this, "user|1", "H0STCNT0", "005930", "data");
            
            assertThat(event.isQuoteData()).isFalse();
            assertThat(event.isExecutionData()).isTrue();
            assertThat(event.isCcnlNotice()).isFalse();
        }

        @Test
        @DisplayName("H0STCNI0은 체결통보")
        void isCcnlNotice_H0STCNI0_returnsTrue() {
            WebSocketDataEvent event = new WebSocketDataEvent(
                    this, "user|1", "H0STCNI0", "", "data");
            
            assertThat(event.isQuoteData()).isFalse();
            assertThat(event.isExecutionData()).isFalse();
            assertThat(event.isCcnlNotice()).isTrue();
        }
    }

    @Nested
    @DisplayName("getter")
    class GetterTests {

        @Test
        @DisplayName("모든 필드 반환")
        void getters_returnCorrectValues() {
            WebSocketDataEvent event = new WebSocketDataEvent(
                    this, "user123|1", "H0STASP0", "005930", "test-data");
            
            assertThat(event.getSessionKey()).isEqualTo("user123|1");
            assertThat(event.getTrId()).isEqualTo("H0STASP0");
            assertThat(event.getTrKey()).isEqualTo("005930");
            assertThat(event.getData()).isEqualTo("test-data");
        }
    }
}
