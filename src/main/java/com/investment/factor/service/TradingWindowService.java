package com.investment.factor.service;

import com.investment.config.PipelineTradingWindowProperties;
import com.investment.setting.service.SystemSettingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import com.investment.config.PipelineTradingWindowProperties.Segment;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 퀀트 매매 유리 시간대 검사 (한국장 09:00~10:00, 미국장 23:30~01:00 KST 등).
 * 해당 구간에만 파이프라인 run 허용, 그 외는 스킵(치고 빠지기).
 * 변동성 구간(장 시작/마감 직전) 검사: 신규 매수 회피용.
 * 활성 여부는 Admin 시스템 설정(pipeline.tradingWindowEnabled)에서 조회.
 */
@Slf4j
@Service
public class TradingWindowService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final PipelineTradingWindowProperties tradingWindowProperties;
    private final SystemSettingService systemSettingService;
    private final Clock clock;

    public TradingWindowService(PipelineTradingWindowProperties tradingWindowProperties,
            SystemSettingService systemSettingService,
            @Autowired(required = false) Clock clock) {
        this.tradingWindowProperties = tradingWindowProperties;
        this.systemSettingService = systemSettingService;
        this.clock = clock != null ? clock : Clock.system(KST);
    }

    /** KR 변동성 구간: 장 시작 9:00-9:10, 장 마감 15:20-15:30 (KST) */
    private static final LocalTime KR_VOL_START_BEGIN = LocalTime.of(9, 0);
    private static final LocalTime KR_VOL_START_END = LocalTime.of(9, 10);
    private static final LocalTime KR_VOL_END_BEGIN = LocalTime.of(15, 20);
    private static final LocalTime KR_VOL_END_END = LocalTime.of(15, 30);

    /** US 변동성 구간 (KST): 개장 23:30-23:40, 마감 05:50-06:00 */
    private static final LocalTime US_VOL_START_BEGIN = LocalTime.of(23, 30);
    private static final LocalTime US_VOL_START_END = LocalTime.of(23, 40);
    private static final LocalTime US_VOL_END_BEGIN = LocalTime.of(5, 50);
    private static final LocalTime US_VOL_END_END = LocalTime.of(6, 0);

    private boolean isTradingWindowEnabled() {
        return Boolean.TRUE.equals(systemSettingService.getBoolean("pipeline.tradingWindowEnabled"));
    }

    /**
     * 현재 시각(KST)이 한국장 허용 구간(1구간 09:00~10:00 또는 2구간 14:30~15:30 등) 안이면 true.
     */
    public boolean isInKrWindow() {
        return isInKrWindow(ZonedDateTime.now(clock).withZoneSameInstant(KST).toLocalTime());
    }

    /** 테스트용: 현재 시각(KST)을 지정하여 KR 윈도우 여부 판단 */
    public boolean isInKrWindow(LocalTime nowKst) {
        if (!isTradingWindowEnabled()) {
            return true;
        }
        LocalTime now = nowKst;
        List<Segment> segments = tradingWindowProperties.getKrSegments();
        for (Segment seg : segments) {
            if (isInWindow(now, seg.getStart(), seg.getEnd())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 현재 시각(KST)이 미국장 허용 구간(1구간 23:30~01:00 또는 2구간 05:00~06:00 등) 안이면 true.
     */
    public boolean isInUsWindow() {
        return isInUsWindow(ZonedDateTime.now(clock).withZoneSameInstant(KST).toLocalTime());
    }

    /** 테스트용: 현재 시각(KST)을 지정하여 US 윈도우 여부 판단 */
    public boolean isInUsWindow(LocalTime nowKst) {
        if (!isTradingWindowEnabled()) {
            return true;
        }
        LocalTime now = nowKst;
        List<Segment> segments = tradingWindowProperties.getUsSegments();
        for (Segment seg : segments) {
            if (isInWindow(now, seg.getStart(), seg.getEnd())) {
                return true;
            }
        }
        return false;
    }

    /**
     * start <= end: 일반 구간 (start <= now < end).
     * start > end: 자정 넘김 구간 (now >= start || now < end).
     */
    private boolean isInWindow(LocalTime now, LocalTime start, LocalTime end) {
        if (start.equals(end)) {
            return true;
        }
        if (!start.isAfter(end)) {
            return !now.isBefore(start) && now.isBefore(end);
        }
        return !now.isBefore(start) || now.isBefore(end);
    }

    /**
     * 변동성 구간 여부. 장 시작 직후(9:00-9:10) 및 장 마감 직전(15:20-15:30) KR과 동등한 US 구간.
     * avoidVolatileWindow 설정이 false면 항상 false.
     *
     * @param market KR 또는 US
     * @param nowKst 현재 시각 (KST)
     * @return 해당 시각이 변동성 구간이면 true
     */
    public boolean isVolatilePeriod(String market, LocalTime nowKst) {
        if (!tradingWindowProperties.isAvoidVolatileWindow()) {
            return false;
        }
        if (market == null) {
            return false;
        }
        if ("KR".equalsIgnoreCase(market)) {
            return isInRange(nowKst, KR_VOL_START_BEGIN, KR_VOL_START_END)
                    || isInRange(nowKst, KR_VOL_END_BEGIN, KR_VOL_END_END);
        }
        if ("US".equalsIgnoreCase(market)) {
            return isInRange(nowKst, US_VOL_START_BEGIN, US_VOL_START_END)
                    || isInRange(nowKst, US_VOL_END_BEGIN, US_VOL_END_END);
        }
        return false;
    }

    private static boolean isInRange(LocalTime now, LocalTime start, LocalTime end) {
        return !now.isBefore(start) && now.isBefore(end);
    }
}
