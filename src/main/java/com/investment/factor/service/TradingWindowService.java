package com.investment.factor.service;

import com.investment.config.PipelineTradingWindowProperties;
import com.investment.setting.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * 퀀트 매매 유리 시간대 검사 (한국장 09:00~10:00, 미국장 23:30~01:00 KST 등).
 * 해당 구간에만 파이프라인 run 허용, 그 외는 스킵(치고 빠지기).
 * 활성 여부는 Admin 시스템 설정(pipeline.tradingWindowEnabled)에서 조회.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradingWindowService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final PipelineTradingWindowProperties tradingWindowProperties;
    private final SystemSettingService systemSettingService;

    private boolean isTradingWindowEnabled() {
        return Boolean.TRUE.equals(systemSettingService.getBoolean("pipeline.tradingWindowEnabled"));
    }

    /**
     * 현재 시각(KST)이 한국장 허용 구간 안이면 true.
     */
    public boolean isInKrWindow() {
        if (!isTradingWindowEnabled()) {
            return true;
        }
        LocalTime now = ZonedDateTime.now(KST).toLocalTime();
        LocalTime start = tradingWindowProperties.getKrStartTime();
        LocalTime end = tradingWindowProperties.getKrEndTime();
        return isInWindow(now, start, end);
    }

    /**
     * 현재 시각(KST)이 미국장 허용 구간 안이면 true.
     * (미 정규장 개장 23:30 KST ~ 01:00 등, 자정 넘김 구간 지원)
     */
    public boolean isInUsWindow() {
        if (!isTradingWindowEnabled()) {
            return true;
        }
        LocalTime now = ZonedDateTime.now(KST).toLocalTime();
        LocalTime start = tradingWindowProperties.getUsStartTime();
        LocalTime end = tradingWindowProperties.getUsEndTime();
        return isInWindow(now, start, end);
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
}
