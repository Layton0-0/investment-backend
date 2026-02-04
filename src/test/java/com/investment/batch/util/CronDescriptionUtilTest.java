package com.investment.batch.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CronDescriptionUtil")
class CronDescriptionUtilTest {

    @Test
    @DisplayName("toKoreanDescription 주요 cron 패턴 매핑")
    void toKoreanDescription_mapsCommonPatterns() {
        assertEquals("매일 09:00", CronDescriptionUtil.toKoreanDescription("0 0 9 * * *"));
        assertEquals("10분마다", CronDescriptionUtil.toKoreanDescription("0 */10 * * * *"));
        assertEquals("매주 월요일 09:00", CronDescriptionUtil.toKoreanDescription("0 0 9 * * MON"));
        assertEquals("매월 1일 08:30", CronDescriptionUtil.toKoreanDescription("0 30 8 1 * *"));
        assertEquals("매분", CronDescriptionUtil.toKoreanDescription("0 * * * * *"));
        assertEquals("매월 말일 09:00", CronDescriptionUtil.toKoreanDescription("0 0 9 L * *"));
        assertEquals("평일 09:00~15:00, 5분마다", CronDescriptionUtil.toKoreanDescription("0 */5 9-15 * * MON-FRI"));
    }

    @Test
    @DisplayName("toKoreanDescription null/blank 빈 문자열 반환")
    void toKoreanDescription_nullOrBlank_returnsEmpty() {
        assertEquals("", CronDescriptionUtil.toKoreanDescription(null));
        assertEquals("", CronDescriptionUtil.toKoreanDescription(""));
        assertEquals("", CronDescriptionUtil.toKoreanDescription("   "));
    }

    @Test
    @DisplayName("toKoreanDescription 미매핑 시 사용자 정의 스케줄")
    void toKoreanDescription_unknown_returnsUserDefined() {
        assertEquals("사용자 정의 스케줄", CronDescriptionUtil.toKoreanDescription("0 0 12 15 * *"));
    }
}
