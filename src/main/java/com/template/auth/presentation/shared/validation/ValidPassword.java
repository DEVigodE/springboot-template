package com.template.auth.presentation.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Shared by register / reset / change-password requests. Same rules as the
 * domain's Password value object - the API boundary rejects early with a
 * field-level message, the domain enforces the invariant regardless of caller.
 * The max length also keeps bcrypt's 72-byte input limit comfortably away.
 */
@NotBlank(message = "Password is required")
@Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
@Pattern(regexp = ".*[a-z].*", message = "Password must contain at least one lowercase letter")
@Pattern(regexp = ".*[A-Z].*", message = "Password must contain at least one uppercase letter")
@Pattern(regexp = ".*\\d.*", message = "Password must contain at least one number")
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {

    String message() default "Invalid password";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
