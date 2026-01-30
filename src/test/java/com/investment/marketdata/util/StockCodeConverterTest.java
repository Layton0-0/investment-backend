package com.investment.marketdata.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StockCodeConverter 테스트
 */
@DisplayName("StockCodeConverter 테스트")
class StockCodeConverterTest {

    @Test
    @DisplayName("6자리 종목코드를 그대로 반환")
    void toStockCode_이미_6자리_코드() {
        // given
        String code = "005930";

        // when
        String result = StockCodeConverter.toStockCode(code);

        // then
        assertEquals("005930", result);
    }

    @Test
    @DisplayName("종목명을 6자리 코드로 변환")
    void toStockCode_종목명_변환() {
        // given
        String name = "삼성전자";

        // when
        String result = StockCodeConverter.toStockCode(name);

        // then
        assertEquals("005930", result);
    }

    @Test
    @DisplayName("매핑되지 않은 종목명은 원본 반환")
    void toStockCode_매핑_없음() {
        // given
        String name = "알수없는종목";

        // when
        String result = StockCodeConverter.toStockCode(name);

        // then
        assertEquals("알수없는종목", result);
    }

    @Test
    @DisplayName("null 입력 처리")
    void toStockCode_null_입력() {
        // when
        String result = StockCodeConverter.toStockCode(null);

        // then
        assertNull(result);
    }

    @Test
    @DisplayName("빈 문자열 입력 처리")
    void toStockCode_빈_문자열() {
        // when
        String result = StockCodeConverter.toStockCode("   ");

        // then
        assertEquals("   ", result);
    }

    @Test
    @DisplayName("6자리 코드를 종목명으로 변환")
    void toStockName_코드_변환() {
        // given
        String code = "005930";

        // when
        String result = StockCodeConverter.toStockName(code);

        // then
        assertEquals("삼성전자", result);
    }

    @Test
    @DisplayName("매핑되지 않은 코드는 원본 반환")
    void toStockName_매핑_없음() {
        // given
        String code = "999999";

        // when
        String result = StockCodeConverter.toStockName(code);

        // then
        assertEquals("999999", result);
    }

    @Test
    @DisplayName("null 입력 처리")
    void toStockName_null_입력() {
        // when
        String result = StockCodeConverter.toStockName(null);

        // then
        assertNull(result);
    }

    @Test
    @DisplayName("유효한 6자리 코드 검증")
    void isValidStockCode_유효한_코드() {
        // given
        String code = "005930";

        // when
        boolean result = StockCodeConverter.isValidStockCode(code);

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("5자리 코드는 유효하지 않음")
    void isValidStockCode_5자리_코드() {
        // given
        String code = "05930";

        // when
        boolean result = StockCodeConverter.isValidStockCode(code);

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("7자리 코드는 유효하지 않음")
    void isValidStockCode_7자리_코드() {
        // given
        String code = "0059300";

        // when
        boolean result = StockCodeConverter.isValidStockCode(code);

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("문자가 포함된 코드는 유효하지 않음")
    void isValidStockCode_문자_포함() {
        // given
        String code = "00593A";

        // when
        boolean result = StockCodeConverter.isValidStockCode(code);

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("null 코드는 유효하지 않음")
    void isValidStockCode_null() {
        // when
        boolean result = StockCodeConverter.isValidStockCode(null);

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("코스피 종목 시장 구분")
    void getMarketType_코스피() {
        // given
        String code = "005930"; // 삼성전자

        // when
        String result = StockCodeConverter.getMarketType(code);

        // then
        assertEquals("KOSPI", result);
    }

    @Test
    @DisplayName("코스닥 종목 시장 구분")
    void getMarketType_코스닥() {
        // given: 100000~999999 구간 = 코스닥 (넷마블)
        String code = "251270";

        // when
        String result = StockCodeConverter.getMarketType(code);

        // then
        assertEquals("KOSDAQ", result);
    }

    @Test
    @DisplayName("유효하지 않은 코드는 UNKNOWN 반환")
    void getMarketType_유효하지_않은_코드() {
        // given
        String code = "9999999";

        // when
        String result = StockCodeConverter.getMarketType(code);

        // then
        assertEquals("UNKNOWN", result);
    }

    @Test
    @DisplayName("모든 매핑 조회")
    void getAllMappings() {
        // when
        Map<String, String> mappings = StockCodeConverter.getAllMappings();

        // then
        assertNotNull(mappings);
        assertFalse(mappings.isEmpty());
        assertEquals("삼성전자", mappings.get("005930"));
        assertEquals("005930", StockCodeConverter.toStockCode("삼성전자"));
    }

    @Test
    @DisplayName("하위 호환성: toKiwoomCode 메서드")
    void toKiwoomCode_하위_호환성() {
        // given
        String name = "삼성전자";

        // when
        String result = StockCodeConverter.toKiwoomCode(name);

        // then
        assertEquals("005930", result);
    }

    @Test
    @DisplayName("하위 호환성: isValidKiwoomCode 메서드")
    void isValidKiwoomCode_하위_호환성() {
        // given
        String code = "005930";

        // when
        boolean result = StockCodeConverter.isValidKiwoomCode(code);

        // then
        assertTrue(result);
    }
}
