package com.investment.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * 비밀번호 검증 어노테이션
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordValidator.class)
@Documented
public @interface ValidPassword {
    
    String message() default "비밀번호가 정책에 맞지 않습니다";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}
