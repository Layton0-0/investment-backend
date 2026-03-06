package com.investment.common.logging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

/**
 * 한국투자증권 Open API 호출·응답 공통 로깅.
 * <p>
 * 명세: docs/04-api/10-korea-investment-api-spec/05-logging.md
 * - 요청: DEBUG (path, trId, query 키 목록)
 * - 응답 성공: DEBUG (status, rt_cd, msg_cd, msg1, output 요약)
 * - 응답 비정상/에러: WARN (status, rt_cd, msg_cd, msg1, body 요약)
 * - 예외/실패: ERROR (apiName, 메시지), 스택은 DEBUG
 */
public final class KoreaInvestmentApiLogging {

    private static final Logger log = LoggerFactory.getLogger(KoreaInvestmentApiLogging.class);
    private static final int BODY_SUMMARY_MAX_LEN = 500;

    private KoreaInvestmentApiLogging() {
        throw new UnsupportedOperationException("utility");
    }

    /**
     * API 요청 로그 (DEBUG). path, trId, query 파라미터 키 목록만 기록.
     */
    public static void logRequest(String apiName, String path, String trId, Collection<String> queryParamKeys) {
        if (log.isDebugEnabled()) {
            String keys = queryParamKeys == null ? "[]" : queryParamKeys.toString();
            log.debug("[KIS-API] 요청 apiName={} path={} trId={} queryKeys={}", apiName, path, trId, keys);
        }
    }

    /**
     * API 요청 로그 (DEBUG). query 파라미터가 Map인 경우. 키와 값을 모두 기록.
     */
    public static void logRequest(String apiName, String path, String trId, Map<String, ?> queryParams) {
        if (log.isDebugEnabled()) {
            String paramSummary = queryParams == null ? "null" : queryParams.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            log.debug("[KIS-API] 요청 apiName={} path={} trId={} queryParams=[{}]", apiName, path, trId, paramSummary);
        }
    }

    /**
     * 응답 성공 로그 (DEBUG). rt_cd, msg_cd, msg1, output 요약.
     */
    public static void logResponseSuccess(String apiName, int status, String rtCd, String msgCd, String msg1,
                                         String outputSummary) {
        if (log.isDebugEnabled()) {
            log.debug("[KIS-API] 응답 apiName={} status={} rt_cd={} msg_cd={} msg1={} outputSummary={}",
                    apiName, status, rtCd, msgCd, msg1, outputSummary);
        }
    }

    /**
     * 응답 성공 로그 (DEBUG). Map 응답에서 rt_cd, msg_cd, msg1, output 키 요약 추출.
     */
    @SuppressWarnings("unchecked")
    public static void logResponseSuccessFromMap(String apiName, int status, Map<String, Object> responseMap) {
        if (!log.isDebugEnabled() || responseMap == null) {
            return;
        }
        String rtCd = stringOrNull(responseMap.get("rt_cd"));
        String msgCd = stringOrNull(responseMap.get("msg_cd"));
        String msg1 = stringOrNull(responseMap.get("msg1"));
        String outputSummary = summarizeOutput(responseMap.get("output"));
        if (outputSummary == null || "null".equals(outputSummary)) {
            Object output2 = responseMap.get("output2");
            if (output2 != null) {
                outputSummary = summarizeOutput(output2);
            }
        }
        logResponseSuccess(apiName, status, rtCd, msgCd, msg1, outputSummary);
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 응답 성공 로그 (DEBUG). JSON 문자열에서 rt_cd, msg_cd, msg1, output 요약 추출.
     */
    public static void logResponseSuccessFromJson(String apiName, int status, String responseJson) {
        if (!log.isDebugEnabled() || responseJson == null || responseJson.isEmpty()) {
            return;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(responseJson);
            String rtCd = root.has("rt_cd") ? root.get("rt_cd").asText(null) : null;
            String msgCd = root.has("msg_cd") ? root.get("msg_cd").asText(null) : null;
            String msg1 = root.has("msg1") ? root.get("msg1").asText(null) : null;
            JsonNode output = root.path("output");
            String outputSummary = output.isMissingNode() || output.isNull()
                    ? summarizeJsonNode(root.path("output2"))
                    : summarizeJsonNode(output);
            logResponseSuccess(apiName, status, rtCd, msgCd, msg1, outputSummary);
        } catch (Exception e) {
            log.debug("[KIS-API] 응답 로그 파싱 실패 apiName={} error={}", apiName, e.getMessage());
        }
    }

    private static String summarizeJsonNode(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "null";
        }
        if (node.isObject()) {
            return "keys=" + node.fieldNames().toString();
        }
        if (node.isArray()) {
            return "size=" + node.size();
        }
        return node.getNodeType().toString();
    }

    /**
     * 응답 비정상/오류 로그 (WARN). HTTP 4xx/5xx 또는 rt_cd != "0" 시.
     */
    public static void logResponseError(String apiName, int status, String rtCd, String msgCd, String msg1,
                                        String bodySummary) {
        log.warn("[KIS-API] 오류 apiName={} status={} rt_cd={} msg_cd={} msg1={} bodySummary={}",
                apiName, status, rtCd, msgCd, msg1, truncate(bodySummary, BODY_SUMMARY_MAX_LEN));
    }

    /**
     * HTTP 4xx/5xx 응답 body로 오류 로그 (WARN).
     */
    public static void logResponseError(String apiName, int status, String responseBody) {
        log.warn("[KIS-API] 오류 apiName={} status={} bodySummary={}",
                apiName, status, truncate(responseBody, BODY_SUMMARY_MAX_LEN));
    }

    /**
     * 예외/실패 로그 (ERROR). 상세 스택은 DEBUG에서만.
     */
    public static void logFailure(String apiName, Throwable throwable) {
        String message = throwable != null ? throwable.getMessage() : "null";
        log.error("[KIS-API] 실패 apiName={} error={}", apiName, message);
        if (log.isDebugEnabled() && throwable != null) {
            log.debug("[KIS-API] 실패 apiName={} 스택", apiName, throwable);
        }
    }

    private static String stringOrNull(Object o) {
        return o == null ? null : o.toString();
    }

    private static String summarizeOutput(Object output) {
        if (output == null) {
            return "null";
        }
        if (output instanceof Map) {
            return "keys=" + ((Map<?, ?>) output).keySet().toString();
        }
        if (output instanceof Collection) {
            return "size=" + ((Collection<?>) output).size();
        }
        if (output instanceof Object[]) {
            return "length=" + ((Object[]) output).length;
        }
        return output.getClass().getSimpleName();
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) {
            return null;
        }
        if (s.length() <= maxLen) {
            return s;
        }
        return s.substring(0, maxLen) + "...(truncated)";
    }
}
