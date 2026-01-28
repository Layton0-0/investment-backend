package com.investment.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * 비밀번호 검증기
 * 
 * 강화된 비밀번호 정책:
 * - 최소 8자, 최대 100자
 * - 대문자, 소문자, 숫자, 특수문자 중 3종류 이상 포함
 * - 일반적인 비밀번호 패턴 금지
 */
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {
    
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 100;
    
    // 대문자, 소문자, 숫자, 특수문자 패턴
    private static final Pattern UPPER_CASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWER_CASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");
    
    // 일반적인 약한 비밀번호 패턴 (금지)
    private static final Pattern[] WEAK_PATTERNS = {
        Pattern.compile("(.)\\1{3,}"), // 같은 문자 4번 이상 반복
        Pattern.compile("(012|123|234|345|456|567|678|789|890)"), // 연속된 숫자
        Pattern.compile("(abc|bcd|cde|def|efg|fgh|ghi|hij|ijk|jkl|klm|lmn|mno|nop|opq|pqr|qrs|rst|stu|tuv|uvw|vwx|wxy|xyz)", Pattern.CASE_INSENSITIVE), // 연속된 영문
        Pattern.compile("^(password|admin|123456|qwerty|letmein|welcome|monkey|12345678|1234567890)", Pattern.CASE_INSENSITIVE) // 일반적인 약한 비밀번호
    };
    
    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        // 초기화 로직 (필요시)
    }
    
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isEmpty()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("비밀번호는 필수입니다")
                    .addConstraintViolation();
            return false;
        }
        
        // 길이 검증
        if (password.length() < MIN_LENGTH) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    String.format("비밀번호는 최소 %d자 이상이어야 합니다", MIN_LENGTH))
                    .addConstraintViolation();
            return false;
        }
        
        if (password.length() > MAX_LENGTH) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    String.format("비밀번호는 최대 %d자 이하여야 합니다", MAX_LENGTH))
                    .addConstraintViolation();
            return false;
        }
        
        // 문자 종류 검증 (대문자, 소문자, 숫자, 특수문자 중 3종류 이상)
        int typeCount = 0;
        if (UPPER_CASE.matcher(password).find()) typeCount++;
        if (LOWER_CASE.matcher(password).find()) typeCount++;
        if (DIGIT.matcher(password).find()) typeCount++;
        if (SPECIAL_CHAR.matcher(password).find()) typeCount++;
        
        if (typeCount < 3) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "비밀번호는 대문자, 소문자, 숫자, 특수문자 중 3종류 이상을 포함해야 합니다")
                    .addConstraintViolation();
            return false;
        }
        
        // 약한 비밀번호 패턴 검증
        for (Pattern weakPattern : WEAK_PATTERNS) {
            if (weakPattern.matcher(password).find()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        "보안을 위해 더 강력한 비밀번호를 사용해주세요")
                        .addConstraintViolation();
                return false;
            }
        }
        
        return true;
    }
}
