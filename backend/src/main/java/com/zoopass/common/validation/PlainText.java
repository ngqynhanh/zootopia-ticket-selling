package com.zoopass.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rejects text containing HTML/script markup ({@code <} or {@code >}).
 * Part of the XSS defence: names and descriptions never need markup, so refusing it at input is
 * simpler and safer than trying to clean it. React also escapes everything it renders.
 */
@Documented
@Constraint(validatedBy = PlainText.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface PlainText {

    String message() default "Không được chứa ký tự < hoặc >";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** null is valid (use @NotBlank to require a value). */
    class Validator implements ConstraintValidator<PlainText, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null || (value.indexOf('<') < 0 && value.indexOf('>') < 0);
        }
    }
}
