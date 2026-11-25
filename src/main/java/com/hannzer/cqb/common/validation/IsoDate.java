package com.hannzer.cqb.common.validation;

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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** 字串須為真實存在的日期，格式 YYYY-MM-DD（例如 2027-02-30 不通過）；null 視為通過 */
@Documented
@Constraint(validatedBy = IsoDate.Validator.class)
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface IsoDate {

    String message() default "日期格式應為 YYYY-MM-DD";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<IsoDate, String> {

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null || parse(value) != null;
        }

        /** ISO_LOCAL_DATE 為嚴格解析；格式或日期不正確時回傳 null */
        public static LocalDate parse(String value) {
            try {
                return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                return null;
            }
        }
    }
}
