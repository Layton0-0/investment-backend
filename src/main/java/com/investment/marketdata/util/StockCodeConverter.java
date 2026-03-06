package com.investment.marketdata.util;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 국내 주식 종목 코드 변환 유틸리티
 * 
 * 한국투자증권 API는 6자리 숫자 종목코드를 사용합니다.
 * - 코스피: 000000~099999
 * - 코스닥: 100000~999999
 * - ETF: 100000~999999
 */
@Slf4j
public class StockCodeConverter {
    
    /**
     * 주요 종목 코드 매핑 (종목명 -> 6자리 코드)
     */
    private static final Map<String, String> STOCK_NAME_TO_CODE = new HashMap<>();
    
    /**
     * 주요 종목 코드 매핑 (6자리 코드 -> 종목명)
     */
    private static final Map<String, String> STOCK_CODE_TO_NAME = new HashMap<>();
    
    static {
        // 코스피 대형주
        addMapping("005930", "삼성전자");
        addMapping("000660", "SK하이닉스");
        addMapping("035420", "NAVER");
        addMapping("051910", "LG화학");
        addMapping("006400", "삼성SDI");
        addMapping("035720", "카카오");
        addMapping("207940", "삼성바이오로직스");
        addMapping("005380", "현대차");
        addMapping("028260", "삼성물산");
        addMapping("105560", "KB금융");
        addMapping("055550", "신한지주");
        addMapping("034730", "SK");
        addMapping("032830", "삼성생명");
        addMapping("003670", "포스코홀딩스");
        addMapping("006800", "미래에셋증권");
        
        // 코스닥 대형주
        addMapping("035900", "JYP엔터테인먼트");
        addMapping("251270", "넷마블");
        addMapping("086790", "하나금융지주");
        addMapping("003550", "LG");
        addMapping("066570", "LG전자");
    }
    
    private static void addMapping(String code, String name) {
        STOCK_NAME_TO_CODE.put(name, code);
        STOCK_CODE_TO_NAME.put(code, name);
    }
    
    /**
     * 종목명 또는 코드를 6자리 종목코드로 변환
     * 
     * @param input 종목명 또는 종목코드
     * @return 6자리 종목코드
     */
    public static String toStockCode(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }
        
        String trimmed = input.trim();
        
        // 프론트/검색에서 오는 "005380-KR", "005380-US" 형식 → 6자리만 추출 (한국투자증권 API는 6자리만 사용)
        if (trimmed.contains("-")) {
            String codePart = trimmed.split("-")[0].trim();
            if (codePart.matches("^\\d{6}$")) {
                return codePart;
            }
        }
        
        // 이미 6자리 숫자 코드인 경우
        if (trimmed.matches("^\\d{6}$")) {
            return trimmed;
        }
        
        // 종목명으로 검색
        String code = STOCK_NAME_TO_CODE.get(trimmed);
        if (code != null) {
            return code;
        }
        
        // 매핑되지 않은 경우 원본 반환 (로그 경고)
        log.warn("종목 코드 매핑을 찾을 수 없습니다: input={}", input);
        return trimmed;
    }
    
    /**
     * 6자리 종목코드를 종목명으로 변환
     * 
     * @param code 6자리 종목코드
     * @return 종목명
     */
    public static String toStockName(String code) {
        if (code == null || code.trim().isEmpty()) {
            return code;
        }
        
        String trimmed = code.trim();
        
        // 6자리 숫자 코드인 경우 종목명 반환
        if (trimmed.matches("^\\d{6}$")) {
            String name = STOCK_CODE_TO_NAME.get(trimmed);
            if (name != null) {
                return name;
            }
        }
        
        // 매핑되지 않은 경우 원본 반환
        return trimmed;
    }
    
    /**
     * 종목 코드가 유효한 6자리 코드인지 확인
     * 
     * @param code 종목 코드
     * @return 유효 여부
     */
    public static boolean isValidStockCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        
        return code.trim().matches("^\\d{6}$");
    }
    
    /**
     * 시장 구분 (코스피/코스닥) 확인
     * 
     * @param code 6자리 종목코드
     * @return "KOSPI" 또는 "KOSDAQ" 또는 "UNKNOWN"
     */
    public static String getMarketType(String code) {
        if (!isValidStockCode(code)) {
            return "UNKNOWN";
        }
        
        int codeInt = Integer.parseInt(code);
        
        // 코스피: 000000~099999
        if (codeInt >= 0 && codeInt < 100000) {
            return "KOSPI";
        }
        
        // 코스닥: 100000~999999
        if (codeInt >= 100000 && codeInt < 1000000) {
            return "KOSDAQ";
        }
        
        return "UNKNOWN";
    }
    
    /**
     * 모든 매핑된 종목 코드 목록 반환
     * 
     * @return 종목 코드 목록
     */
    public static Map<String, String> getAllMappings() {
        return new HashMap<>(STOCK_CODE_TO_NAME);
    }
    
    /**
     * @deprecated 이 메서드는 하위 호환성을 위해 유지됩니다. {@link #toStockCode(String)}를 사용하세요.
     */
    @Deprecated
    public static String toKiwoomCode(String input) {
        return toStockCode(input);
    }
    
    /**
     * @deprecated 이 메서드는 하위 호환성을 위해 유지됩니다. {@link #isValidStockCode(String)}를 사용하세요.
     */
    @Deprecated
    public static boolean isValidKiwoomCode(String code) {
        return isValidStockCode(code);
    }
}
