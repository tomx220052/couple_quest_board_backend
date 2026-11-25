package com.hannzer.cqb.goals;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 子任務進度：必須是 JSON 整數，且為 0／20／40／60／80／100。
 *
 * <p>欄位宣告為 Object，讓 Jackson 保留原始型別：字串 "20" 會是 String、20.5 會是 Double，都不通過，
 * 與前端的 schema 一致（不做型別自動轉換）。
 */
@Documented
@Constraint(validatedBy = ProgressValue.Validator.class)
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
@interface ProgressValue {

    String message() default "進度只能是 0、20、40、60、80、100";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ProgressValue, Object> {

        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            return value instanceof Integer progress && Progress.isAllowed(progress);
        }
    }
}
