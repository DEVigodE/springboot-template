package com.template.auth.presentation.shared;

import com.template.auth.domain.shared.DomainException;
import com.template.auth.domain.shared.exception.AccountLockedException;
import com.template.auth.domain.shared.exception.EmailAlreadyRegisteredException;
import com.template.auth.domain.shared.exception.FileAssetNotFoundException;
import com.template.auth.domain.shared.exception.InvalidCredentialsException;
import com.template.auth.domain.shared.exception.InvalidEmailException;
import com.template.auth.domain.shared.exception.InvalidEmailVerificationTokenException;
import com.template.auth.domain.shared.exception.InvalidFileTypeException;
import com.template.auth.domain.shared.exception.InvalidPasswordException;
import com.template.auth.domain.shared.exception.InvalidPasswordResetTokenException;
import com.template.auth.domain.shared.exception.InvalidRefreshTokenException;
import com.template.auth.domain.shared.exception.NotificationNotFoundException;
import com.template.auth.domain.shared.exception.PermissionAlreadyExistsException;
import com.template.auth.domain.shared.exception.PermissionNotFoundException;
import com.template.auth.domain.shared.exception.RefreshTokenNotFoundException;
import com.template.auth.domain.shared.exception.RoleAlreadyExistsException;
import com.template.auth.domain.shared.exception.RoleNotFoundException;
import com.template.auth.domain.shared.exception.UserInactiveException;
import com.template.auth.domain.shared.exception.UserNotFoundException;
import com.template.auth.presentation.shared.ErrorResponse.FieldError;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Maps every failure to the ErrorResponse shape. Domain exceptions get a stable
 * status + code each (the table below); framework exceptions (validation,
 * security, routing) are normalized the same way; anything else is a genuine
 * bug and becomes an opaque 500 with the details only in the logs.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private record ExceptionMeta(HttpStatus status, String code) {
    }

    private static final ExceptionMeta DEFAULT_DOMAIN_META = new ExceptionMeta(HttpStatus.BAD_REQUEST, "DOMAIN_ERROR");

    private static final Map<Class<? extends DomainException>, ExceptionMeta> DOMAIN_META = Map.ofEntries(
            Map.entry(EmailAlreadyRegisteredException.class, new ExceptionMeta(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED")),
            Map.entry(InvalidCredentialsException.class, new ExceptionMeta(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS")),
            Map.entry(AccountLockedException.class, new ExceptionMeta(HttpStatus.LOCKED, "ACCOUNT_LOCKED")),
            Map.entry(UserInactiveException.class, new ExceptionMeta(HttpStatus.FORBIDDEN, "USER_INACTIVE")),
            Map.entry(UserNotFoundException.class, new ExceptionMeta(HttpStatus.NOT_FOUND, "USER_NOT_FOUND")),
            Map.entry(RoleAlreadyExistsException.class, new ExceptionMeta(HttpStatus.CONFLICT, "ROLE_ALREADY_EXISTS")),
            Map.entry(RoleNotFoundException.class, new ExceptionMeta(HttpStatus.NOT_FOUND, "ROLE_NOT_FOUND")),
            Map.entry(PermissionAlreadyExistsException.class, new ExceptionMeta(HttpStatus.CONFLICT, "PERMISSION_ALREADY_EXISTS")),
            Map.entry(PermissionNotFoundException.class, new ExceptionMeta(HttpStatus.NOT_FOUND, "PERMISSION_NOT_FOUND")),
            Map.entry(InvalidRefreshTokenException.class, new ExceptionMeta(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN")),
            Map.entry(NotificationNotFoundException.class, new ExceptionMeta(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND")),
            Map.entry(RefreshTokenNotFoundException.class, new ExceptionMeta(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND")),
            Map.entry(FileAssetNotFoundException.class, new ExceptionMeta(HttpStatus.NOT_FOUND, "FILE_ASSET_NOT_FOUND")),
            Map.entry(InvalidPasswordResetTokenException.class, new ExceptionMeta(HttpStatus.UNAUTHORIZED, "INVALID_PASSWORD_RESET_TOKEN")),
            Map.entry(InvalidEmailVerificationTokenException.class, new ExceptionMeta(HttpStatus.UNAUTHORIZED, "INVALID_EMAIL_VERIFICATION_TOKEN")),
            Map.entry(InvalidFileTypeException.class, new ExceptionMeta(HttpStatus.BAD_REQUEST, "INVALID_FILE_TYPE")),
            Map.entry(InvalidPasswordException.class, new ExceptionMeta(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD")),
            Map.entry(InvalidEmailException.class, new ExceptionMeta(HttpStatus.BAD_REQUEST, "INVALID_EMAIL")));

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomain(DomainException ex) {
        ExceptionMeta meta = DOMAIN_META.getOrDefault(ex.getClass(), DEFAULT_DOMAIN_META);
        return respond(meta.status(), meta.code(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException ex) {
        List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        return validation(errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleParameterValidation(HandlerMethodValidationException ex) {
        List<FieldError> errors = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldError(result.getMethodParameter().getParameterName(),
                                message(error))))
                .toList();
        return validation(errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldError> errors = ex.getConstraintViolations().stream()
                .map(violation -> new FieldError(String.valueOf(violation.getPropertyPath()), violation.getMessage()))
                .toList();
        return validation(errors);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex) {
        return respond(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Malformed request");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        return respond(HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_TOO_LARGE", "Uploaded file exceeds the size limit");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(AuthenticationException ex) {
        return respond(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Unauthorized");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(AccessDeniedException ex) {
        return respond(HttpStatus.FORBIDDEN, "FORBIDDEN", "Forbidden");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException ex) {
        return respond(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return respond(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Method not allowed");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        return respond(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "Unsupported media type");
    }

    /**
     * Postgres/Redis unreachable (connection refused, timeouts). Reported as 503
     * rather than an opaque 500: it's an infrastructure outage, not a bug, and
     * a load balancer can act on it.
     */
    @ExceptionHandler({DataAccessResourceFailureException.class, CannotCreateTransactionException.class})
    public ResponseEntity<ErrorResponse> handleBackingServiceUnavailable(Exception ex) {
        log.error("Backing service unavailable: {}", ex.getMessage());
        return respond(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE",
                "A backing service is temporarily unavailable");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Internal server error");
    }

    private static ResponseEntity<ErrorResponse> validation(List<FieldError> errors) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.validation(HttpStatus.BAD_REQUEST.value(), "Validation failed", errors));
    }

    private static ResponseEntity<ErrorResponse> respond(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status.value(), code, message));
    }

    private static String message(MessageSourceResolvable error) {
        return error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage();
    }
}
