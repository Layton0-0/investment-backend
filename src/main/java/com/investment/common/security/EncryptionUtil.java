package com.investment.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM 암호화 유틸리티
 * 
 * 민감한 정보(API 키, 토큰 등)를 안전하게 암호화/복호화합니다.
 * GCM 모드는 인증된 암호화를 제공하여 무결성 검증도 함께 수행합니다.
 */
@Slf4j
@Component
public class EncryptionUtil {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 16; // 128 bits
    private static final int AES_KEY_SIZE = 256;
    
    private final SecretKey secretKey;
    
    /**
     * 생성자: application.yml에서 암호화 키를 읽어옵니다.
     * 키가 없으면 자동으로 생성하지만, 프로덕션에서는 반드시 설정해야 합니다.
     * 
     * 키 형식:
     * - Base64로 인코딩된 32바이트 키 (권장)
     * - UUID 형식 또는 임의의 문자열: SHA-256 해시를 통해 32바이트 키로 변환
     */
    public EncryptionUtil(@Value("${investment.security.encryption-key:}") String encryptionKey) {
        if (encryptionKey == null || encryptionKey.isEmpty()) {
            log.warn("암호화 키가 설정되지 않았습니다. 임시 키를 생성합니다. 프로덕션에서는 반드시 설정하세요.");
            this.secretKey = generateKey();
        } else {
            byte[] keyBytes = parseEncryptionKey(encryptionKey);
            this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
        }
    }
    
    /**
     * 암호화 키를 파싱합니다.
     * Base64 형식이면 디코딩하고, 그렇지 않으면 SHA-256 해시를 통해 32바이트 키로 변환합니다.
     * 
     * @param encryptionKey 암호화 키 문자열
     * @return 32바이트 키 바이트 배열
     */
    private byte[] parseEncryptionKey(String encryptionKey) {
        try {
            // Base64 디코딩 시도
            byte[] decodedKey = Base64.getDecoder().decode(encryptionKey);
            if (decodedKey.length == 32) {
                log.debug("Base64 형식의 암호화 키를 사용합니다.");
                return decodedKey;
            } else {
                log.warn("Base64 디코딩된 키의 길이가 32바이트가 아닙니다 ({}바이트). SHA-256 해시로 변환합니다.", decodedKey.length);
                return deriveKeyFromString(encryptionKey);
            }
        } catch (IllegalArgumentException e) {
            // Base64 디코딩 실패: 문자열을 SHA-256 해시로 변환
            log.debug("Base64 디코딩 실패. 입력 문자열을 SHA-256 해시하여 키로 변환합니다: {}", e.getMessage());
            return deriveKeyFromString(encryptionKey);
        }
    }
    
    /**
     * 문자열을 SHA-256 해시하여 32바이트 키로 변환합니다.
     * 
     * @param input 입력 문자열
     * @return 32바이트 키 바이트 배열
     */
    private byte[] deriveKeyFromString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            log.info("입력 문자열을 SHA-256 해시하여 암호화 키로 변환했습니다. (입력 길이: {}자)", input.length());
            return hash;
        } catch (Exception e) {
            log.error("키 변환 실패", e);
            throw new RuntimeException("암호화 키 변환 실패", e);
        }
    }
    
    /**
     * 임시 키 생성 (개발용)
     */
    private SecretKey generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(AES_KEY_SIZE);
            return keyGenerator.generateKey();
        } catch (Exception e) {
            log.error("키 생성 실패", e);
            throw new RuntimeException("암호화 키 생성 실패", e);
        }
    }
    
    /**
     * 평문을 암호화합니다.
     * 
     * @param plaintext 암호화할 평문
     * @return Base64로 인코딩된 암호문 (IV + 암호문)
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        
        try {
            // IV 생성 (매번 새로운 IV 사용)
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            
            // Cipher 초기화
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
            
            // 암호화
            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            
            // IV + 암호문을 결합하여 Base64 인코딩
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);
            byte[] encryptedData = byteBuffer.array();
            
            return Base64.getEncoder().encodeToString(encryptedData);
        } catch (Exception e) {
            log.error("암호화 실패", e);
            throw new RuntimeException("암호화 실패", e);
        }
    }
    
    /**
     * 암호문을 복호화합니다.
     * 
     * @param ciphertext Base64로 인코딩된 암호문 (IV + 암호문)
     * @return 복호화된 평문
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }
        
        try {
            // Base64 디코딩
            byte[] encryptedData = Base64.getDecoder().decode(ciphertext);
            
            // IV와 암호문 분리
            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedData);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);
            
            // Cipher 초기화
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
            
            // 복호화
            byte[] plaintext = cipher.doFinal(cipherText);
            
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (javax.crypto.AEADBadTagException e) {
            log.error("복호화 실패: 암호화 키 불일치 또는 데이터 손상. 암호화 키가 변경되었거나 다른 키로 암호화된 데이터일 수 있습니다.", e);
            throw new RuntimeException("복호화 실패: 암호화 키가 일치하지 않습니다. 환경 변수 INVESTMENT_ENCRYPTION_KEY를 확인하거나, 마이페이지에서 API 키를 다시 입력해주세요.", e);
        } catch (IllegalArgumentException e) {
            log.error("복호화 실패: 잘못된 Base64 형식", e);
            throw new RuntimeException("복호화 실패: 암호화된 데이터 형식이 올바르지 않습니다.", e);
        } catch (Exception e) {
            log.error("복호화 실패: 예상치 못한 오류", e);
            throw new RuntimeException("복호화 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 새로운 암호화 키를 생성하여 Base64로 인코딩된 문자열로 반환합니다.
     * 프로덕션 환경에서 application.yml에 설정할 키를 생성할 때 사용합니다.
     * 
     * @return Base64로 인코딩된 암호화 키
     */
    public static String generateEncryptionKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(AES_KEY_SIZE);
            SecretKey key = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("암호화 키 생성 실패", e);
        }
    }
}
