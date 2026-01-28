package com.investment.marketdata.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

/**
 * 한국투자증권 API Hashkey 생성 유틸리티
 * 
 * 일부 API(주문 등)는 요청 바디의 무결성을 검증하기 위해 Hashkey가 필요합니다.
 * Hashkey는 요청 바디를 JSON 문자열로 변환한 후, appsecret을 키로 사용하여
 * HMAC SHA256 알고리즘으로 생성합니다.
 * 
 * 참고: 현재 사용하는 차트 조회 API에는 Hashkey가 필요하지 않지만,
 * 향후 주문 API 등에서 사용할 수 있도록 유틸리티로 제공합니다.
 */
@Slf4j
@Component
public class KoreaInvestmentHashkeyUtil {
    
    private static final String HMAC_SHA256 = "HmacSHA256";
    private final ObjectMapper objectMapper;
    
    public KoreaInvestmentHashkeyUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * 요청 바디를 기반으로 Hashkey 생성
     * 
     * @param requestBody 요청 바디 (Map 형태)
     * @param appSecret App Secret (HMAC 키로 사용)
     * @return Base64로 인코딩된 Hashkey
     * @throws IllegalStateException Hashkey 생성 실패 시
     */
    public String generateHashkey(Map<String, Object> requestBody, String appSecret) {
        try {
            // 요청 바디를 JSON 문자열로 변환
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            // HMAC SHA256으로 해시 생성
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    appSecret.getBytes(StandardCharsets.UTF_8), 
                    HMAC_SHA256
            );
            mac.init(secretKeySpec);
            
            byte[] hashBytes = mac.doFinal(jsonBody.getBytes(StandardCharsets.UTF_8));
            
            // Base64로 인코딩하여 반환
            return Base64.getEncoder().encodeToString(hashBytes);
            
        } catch (NoSuchAlgorithmException e) {
            log.error("HMAC SHA256 알고리즘을 찾을 수 없습니다", e);
            throw new IllegalStateException("Hashkey 생성 실패: 알고리즘을 찾을 수 없습니다", e);
        } catch (InvalidKeyException e) {
            log.error("잘못된 키입니다", e);
            throw new IllegalStateException("Hashkey 생성 실패: 잘못된 키", e);
        } catch (Exception e) {
            log.error("Hashkey 생성 중 오류 발생", e);
            throw new IllegalStateException("Hashkey 생성 실패", e);
        }
    }
    
    /**
     * 요청 바디를 기반으로 Hashkey 생성 (JSON 문자열 직접 전달)
     * 
     * @param jsonBody JSON 문자열로 변환된 요청 바디
     * @param appSecret App Secret (HMAC 키로 사용)
     * @return Base64로 인코딩된 Hashkey
     * @throws IllegalStateException Hashkey 생성 실패 시
     */
    public String generateHashkeyFromJson(String jsonBody, String appSecret) {
        try {
            // HMAC SHA256으로 해시 생성
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    appSecret.getBytes(StandardCharsets.UTF_8), 
                    HMAC_SHA256
            );
            mac.init(secretKeySpec);
            
            byte[] hashBytes = mac.doFinal(jsonBody.getBytes(StandardCharsets.UTF_8));
            
            // Base64로 인코딩하여 반환
            return Base64.getEncoder().encodeToString(hashBytes);
            
        } catch (NoSuchAlgorithmException e) {
            log.error("HMAC SHA256 알고리즘을 찾을 수 없습니다", e);
            throw new IllegalStateException("Hashkey 생성 실패: 알고리즘을 찾을 수 없습니다", e);
        } catch (InvalidKeyException e) {
            log.error("잘못된 키입니다", e);
            throw new IllegalStateException("Hashkey 생성 실패: 잘못된 키", e);
        } catch (Exception e) {
            log.error("Hashkey 생성 중 오류 발생", e);
            throw new IllegalStateException("Hashkey 생성 실패", e);
        }
    }
}
