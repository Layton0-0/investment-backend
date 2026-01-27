package com.investment.domain.entity;

/**
 * 증권사 타입
 * 사용증권명을 코드화하여 저장합니다.
 */
public enum BrokerType {
    KOREA_INVESTMENT("KOREA_INVESTMENT", "한국투자증권"),
    KIWOOM("KIWOOM", "키움증권"),
    NH_INVESTMENT("NH_INVESTMENT", "NH투자증권"),
    SAMSUNG("SAMSUNG", "삼성증권"),
    DAISHIN("DAISHIN", "대신증권"),
    MIRAE_ASSET("MIRAE_ASSET", "미래에셋증권"),
    KB_SECURITIES("KB_SECURITIES", "KB증권"),
    SHINHAN("SHINHAN", "신한투자증권"),
    HANA("HANA", "하나증권"),
    OTHER("OTHER", "기타");
    
    private final String code;
    private final String name;
    
    BrokerType(String code, String name) {
        this.code = code;
        this.name = name;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    /**
     * 코드로 BrokerType 찾기
     */
    public static BrokerType fromCode(String code) {
        for (BrokerType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return OTHER;
    }
}
