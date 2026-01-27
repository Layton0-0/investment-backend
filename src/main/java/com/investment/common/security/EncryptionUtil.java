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
     */
    public EncryptionUtil(@Value("${investment.security.encryption-key:}") String encryptionKey) {
        if (encryptionKey == null || encryptionKey.isEmpty()) {
            log.warn("암호화 키가 설정되지 않았습니다. 임시 키를 생성합니다. 프로덕션에서는 반드시 설정하세요.");
            this.secretKey = generateKey();
        } else {
            // Base64로 인코딩된 키를 디코딩
            byte[] decodedKey = Base64.getDecoder().decode(encryptionKey);
            this.secretKey = new SecretKeySpec(decodedKey, ALGORITHM);
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
        } catch (Exception e) {
            log.error("복호화 실패", e);
            throw new RuntimeException("복호화 실패", e);
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
