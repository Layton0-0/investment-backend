package com.investment.common.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PasswordValidator")
class PasswordValidatorTest {

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("null 또는 빈 문자열은 유효하지 않다")
    void nullOrEmpty_invalid(String password) {
        PasswordValidator.ValidationResult result = PasswordValidator.validatePassword(password);
        assertFalse(result.isValid());
        assertTrue(result.getMessage() != null && result.getMessage().contains("필수"));
    }

    @Test
    @DisplayName("8자 미만은 유효하지 않다")
    void tooShort_invalid() {
        PasswordValidator.ValidationResult result = PasswordValidator.validatePassword("Ab1!");
        assertFalse(result.isValid());
        assertTrue(result.getMessage() != null && result.getMessage().contains("8자"));
    }

    @Test
    @DisplayName("100자 초과는 유효하지 않다")
    void tooLong_invalid() {
        String longPassword = "A" + "a1!".repeat(34); // 1 + 102 = 103자
        PasswordValidator.ValidationResult result = PasswordValidator.validatePassword(longPassword);
        assertFalse(result.isValid());
        assertTrue(result.getMessage() != null && result.getMessage().contains("100자"));
    }

    @Test
    @DisplayName("대소문자 숫자 특수문자 3종류 미만은 유효하지 않다")
    void lessThanThreeTypes_invalid() {
        // 소문자+숫자만 (2종류)
        PasswordValidator.ValidationResult r1 = PasswordValidator.validatePassword("abcdef12");
        assertFalse(r1.isValid());
        assertTrue(r1.getMessage() != null && r1.getMessage().contains("3종류"));
    }

    @Test
    @DisplayName("대문자 소문자 숫자 특수문자 3종류 이상이면 유효하다")
    void threeTypesOrMore_valid() {
        // 연속 영문(abc, cde 등) 패턴 없이 3종류 이상
        PasswordValidator.ValidationResult result = PasswordValidator.validatePassword("Xk9#mNp2");
        assertTrue(result.isValid());
        assertNull(result.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = { "password1!A", "Admin123!x", "12345678!aA", "qwerty1!Ab", "letmein1!Ab", "welcome1!Ab" })
    @DisplayName("약한 비밀번호 패턴은 유효하지 않다")
    void weakPattern_invalid(String password) {
        PasswordValidator.ValidationResult result = PasswordValidator.validatePassword(password);
        assertFalse(result.isValid());
        assertTrue(result.getMessage() != null &&
                (result.getMessage().contains("강력한") || result.getMessage().contains("3종류")));
    }

    @Test
    @DisplayName("같은 문자 4번 이상 반복은 유효하지 않다")
    void repeatedChars_invalid() {
        PasswordValidator.ValidationResult result = PasswordValidator.validatePassword("AAAAbc12!");
        assertFalse(result.isValid());
    }

    @Test
    @DisplayName("정상적인 강한 비밀번호는 유효하다")
    void strongPassword_valid() {
        assertTrue(PasswordValidator.validatePassword("MyP@ssw0rdX").isValid());
        assertTrue(PasswordValidator.validatePassword("Tr0ub4dor&3").isValid());
    }
}
