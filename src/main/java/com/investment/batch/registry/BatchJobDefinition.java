package com.investment.batch.registry;

import lombok.Builder;
import lombok.Getter;

/**
 * 스케줄 Job 정의 (레지스트리 단일 소스).
 * Job 이름(id), 한글 이름, 설명, cron, timeZone, 수동 트리거 경로.
 */
@Getter
@Builder
public class BatchJobDefinition {

    /** Job 빈 이름 = Spring Batch Job 이름 */
    private final String id;
    private final String name;
    private final String description;
    private final String cronExpression;
    private final String timeZone;
    /** POST 트리거 경로. null이면 "지금 실행" 버튼 미노출 */
    private final String triggerPath;
}
