package com.investment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 파이프라인 매매 허용 시간대 (퀀트 유리 구간만 실행).
 * 한국장: 09:00~10:00, 미국장: 23:30~01:00(KST) 등. 모든 시간 KST.
 */
@Component
@ConfigurationProperties(prefix = "investment.pipeline.trading-window")
@Getter
@Setter
public class PipelineTradingWindowProperties {

    /** 사용 여부. true면 허용 구간 밖에서는 해당 시장 run 스킵 */
    private boolean enabled = true;

    /** 변동성 구간(장 시작/마감) 신규 매수 회피. true면 9:00-9:10, 15:20-15:30(KR) 등에서 매수 지연 */
    private boolean avoidVolatileWindow = true;

    private Kr kr = new Kr();
    private Us us = new Us();

    @Getter
    @Setter
    public static class Kr {
        /** 허용 시작 (KST). 기본 09:00 */
        private String start = "09:00";
        /** 허용 종료 (KST). 기본 10:00 */
        private String end = "10:00";
        /** 두 번째 구간 시작 (KST, 선택). 예: 14:30 마감 1시간 */
        private String start2 = "14:30";
        /** 두 번째 구간 종료 (KST, 선택). 예: 15:30 */
        private String end2 = "15:30";
    }

    @Getter
    @Setter
    public static class Us {
        /** 허용 시작 (KST). 미 정규장 개장 직후 = 23:30 KST */
        private String start = "23:30";
        /** 허용 종료 (KST). 자정 넘김 가능. 기본 01:00 (다음날) */
        private String end = "01:00";
        /** 두 번째 구간 시작 (KST, 선택). 예: 05:00 마감 직전 */
        private String start2 = "05:00";
        /** 두 번째 구간 종료 (KST, 선택). 예: 06:00 */
        private String end2 = "06:00";
    }

    /** 세그먼트(시작·종료) — 다중 구간 판단용 */
    public static class Segment {
        private final LocalTime start;
        private final LocalTime end;

        public Segment(LocalTime start, LocalTime end) {
            this.start = start;
            this.end = end;
        }

        public LocalTime getStart() {
            return start;
        }

        public LocalTime getEnd() {
            return end;
        }
    }

    public LocalTime getKrStartTime() {
        return parseTime(kr.getStart(), LocalTime.of(9, 0));
    }

    public LocalTime getKrEndTime() {
        return parseTime(kr.getEnd(), LocalTime.of(10, 0));
    }

    public LocalTime getUsStartTime() {
        return parseTime(us.getStart(), LocalTime.of(23, 30));
    }

    public LocalTime getUsEndTime() {
        return parseTime(us.getEnd(), LocalTime.of(1, 0));
    }

    /**
     * 한국장 허용 구간 세그먼트 목록 (1구간 필수 + 2구간 있으면 추가).
     * start2/end2가 비어 있으면 1구간만 반환.
     */
    public List<Segment> getKrSegments() {
        List<Segment> list = new ArrayList<>();
        list.add(new Segment(getKrStartTime(), getKrEndTime()));
        if (isSegment2Configured(kr.getStart2(), kr.getEnd2())) {
            list.add(new Segment(parseTime(kr.getStart2(), LocalTime.of(14, 30)),
                    parseTime(kr.getEnd2(), LocalTime.of(15, 30))));
        }
        return Collections.unmodifiableList(list);
    }

    /**
     * 미국장 허용 구간 세그먼트 목록 (1구간 필수 + 2구간 있으면 추가).
     */
    public List<Segment> getUsSegments() {
        List<Segment> list = new ArrayList<>();
        list.add(new Segment(getUsStartTime(), getUsEndTime()));
        if (isSegment2Configured(us.getStart2(), us.getEnd2())) {
            list.add(new Segment(parseTime(us.getStart2(), LocalTime.of(5, 0)),
                    parseTime(us.getEnd2(), LocalTime.of(6, 0))));
        }
        return Collections.unmodifiableList(list);
    }

    private static boolean isSegment2Configured(String start2, String end2) {
        return start2 != null && !start2.isBlank() && end2 != null && !end2.isBlank();
    }

    private static LocalTime parseTime(String s, LocalTime defaultVal) {
        if (s == null || s.isBlank()) {
            return defaultVal;
        }
        try {
            String[] parts = s.trim().split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return LocalTime.of(hour % 24, minute % 60);
        } catch (Exception e) {
            return defaultVal;
        }
    }
}
