package com.template.auth.presentation.shared;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import org.slf4j.MDC;

/**
 * The one error shape every failure takes: a stable machine-readable
 * {@code code} frontends can branch on (instead of parsing messages) and the
 * {@code requestId} also echoed as X-Request-Id, to correlate against logs.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(int statusCode, String code, String message, List<FieldError> errors,
        String requestId) {

    public record FieldError(String field, String message) {
    }

    public static ErrorResponse of(int status, String code, String message) {
        return new ErrorResponse(status, code, message, null, MDC.get(RequestIdFilter.MDC_KEY));
    }

    public static ErrorResponse validation(int status, String message, List<FieldError> errors) {
        return new ErrorResponse(status, "VALIDATION_ERROR", message, errors, MDC.get(RequestIdFilter.MDC_KEY));
    }
}
