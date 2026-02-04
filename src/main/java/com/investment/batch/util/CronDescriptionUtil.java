package com.investment.batch.util;

/**
 * Spring 6필드 cron 표현식(초 분 시 일 월 요일)을 한국어 설명으로 변환.
 */
public final class CronDescriptionUtil {

    private CronDescriptionUtil() {
    }

    /**
     * cron 표현식을 한국어 실행 주기 설명으로 변환.
     *
     * @param cronExpression 6필드 cron (초 분 시 일 월 요일)
     * @return 한국어 설명. 매핑 없으면 "사용자 정의 스케줄" 또는 원문 반환
     */
    public static String toKoreanDescription(String cronExpression) {
        if (cronExpression == null || cronExpression.isBlank()) {
            return "";
        }
        String normalized = cronExpression.trim();

        // 매핑 (프로젝트에서 사용하는 패턴 기준)
        if ("0 0 9 * * *".equals(normalized))
            return "매일 09:00";
        if ("0 0 6 * * *".equals(normalized))
            return "매일 06:00";
        if ("0 0 8 * * *".equals(normalized))
            return "매일 08:00";
        if ("0 0 16 * * *".equals(normalized))
            return "매일 16:00";
        if ("0 0 17 * * *".equals(normalized))
            return "매일 17:00";
        if ("0 0 * * * *".equals(normalized))
            return "매시간 0분";
        if ("0 */10 * * * *".equals(normalized))
            return "10분마다";
        if ("0 */15 * * * *".equals(normalized))
            return "15분마다";
        if ("0 */5 * * * *".equals(normalized))
            return "5분마다";
        if ("0 10 9 * * *".equals(normalized))
            return "매일 09:10";
        if ("0 0 9 * * MON".equals(normalized))
            return "매주 월요일 09:00";
        if ("0 30 8 1 * *".equals(normalized))
            return "매월 1일 08:30";
        if ("0 * * * * *".equals(normalized))
            return "매분";
        if ("0 0 9 L * *".equals(normalized))
            return "매월 말일 09:00";
        if ("0 5 16 * * MON-FRI".equals(normalized))
            return "평일 16:05";
        if ("0 10,40 9 * * MON-FRI".equals(normalized))
            return "평일 09:10, 09:40";

        if (normalized.contains("*/5") && normalized.contains("9-15") && normalized.contains("MON-FRI")) {
            return "평일 09:00~15:00, 5분마다";
        }

        return "사용자 정의 스케줄";
    }
}
