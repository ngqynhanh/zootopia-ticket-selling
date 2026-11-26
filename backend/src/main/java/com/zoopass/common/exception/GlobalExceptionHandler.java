package com.zoopass.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Converts every exception thrown by a controller into a structured {@link ApiError}.
 *
 * <p>Rules: expected errors (4xx) are logged at WARN without a stack trace; unexpected errors (5xx)
 * are logged at ERROR with the stack trace and the client only gets a generic message.
 * Exception detail is added to the body only in dev ({@code app.errors.include-debug-detail}).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ApiErrorFactory errorFactory;

    public GlobalExceptionHandler(ApiErrorFactory errorFactory) {
        this.errorFactory = errorFactory;
    }

    /** Errors thrown on purpose by our services (not found, business rule, conflict...). */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex, HttpServletRequest request) {
        List<ApiError.FieldErrorDetail> errors = ex instanceof FieldValidationException fieldError
                ? fieldError.getErrors()
                : List.of();
        return build(ex.getErrorCode(), ex.getMessage(), errors, request, ex);
    }

    /** {@code @PreAuthorize} denied a logged-in user (wrong role). */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(ErrorCode.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này", List.of(), request, ex);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return build(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập", List.of(), request, ex);
    }

    /** {@code @Valid @RequestBody} failed. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBodyValidation(MethodArgumentNotValidException ex,
                                                         HttpServletRequest request) {
        List<ApiError.FieldErrorDetail> errors = ex.getBindingResult().getAllErrors().stream()
                .map(error -> new ApiError.FieldErrorDetail(
                        error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName(),
                        error.getDefaultMessage()))
                .toList();
        return build(ErrorCode.VALIDATION_FAILED, "Dữ liệu không hợp lệ", errors, request, ex);
    }

    /** Constraint annotations on {@code @RequestParam} / {@code @PathVariable} failed. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleMethodValidation(HandlerMethodValidationException ex,
                                                           HttpServletRequest request) {
        List<ApiError.FieldErrorDetail> errors = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new ApiError.FieldErrorDetail(
                                result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
                .toList();
        return build(ErrorCode.VALIDATION_FAILED, "Dữ liệu không hợp lệ", errors, request, ex);
    }

    /** Validation of service-layer method arguments ({@code @Validated} beans) failed. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex,
                                                              HttpServletRequest request) {
        List<ApiError.FieldErrorDetail> errors = ex.getConstraintViolations().stream()
                .map(violation -> new ApiError.FieldErrorDetail(
                        violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();
        return build(ErrorCode.VALIDATION_FAILED, "Dữ liệu không hợp lệ", errors, request, ex);
    }

    /** Wrong type in a query/path parameter, e.g. {@code ?page=abc}. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                       HttpServletRequest request) {
        var error = new ApiError.FieldErrorDetail(ex.getName(), "Giá trị '" + ex.getValue() + "' không đúng định dạng");
        return build(ErrorCode.VALIDATION_FAILED, "Dữ liệu không hợp lệ", List.of(error), request, ex);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException ex,
                                                           HttpServletRequest request) {
        var error = new ApiError.FieldErrorDetail(ex.getParameterName(), "Thiếu tham số bắt buộc");
        return build(ErrorCode.VALIDATION_FAILED, "Dữ liệu không hợp lệ", List.of(error), request, ex);
    }

    /** Body is not valid JSON or has a wrong type (e.g. text in a number field). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                         HttpServletRequest request) {
        return build(ErrorCode.MALFORMED_REQUEST, "Nội dung yêu cầu không đúng định dạng JSON", List.of(), request, ex);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return build(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy đường dẫn yêu cầu", List.of(), request, ex);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                             HttpServletRequest request) {
        return build(ErrorCode.METHOD_NOT_ALLOWED,
                "Phương thức " + ex.getMethod() + " không được hỗ trợ", List.of(), request, ex);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleUploadTooLarge(MaxUploadSizeExceededException ex,
                                                         HttpServletRequest request) {
        return build(ErrorCode.PAYLOAD_TOO_LARGE, "File vượt quá dung lượng cho phép (5 MB)", List.of(), request, ex);
    }

    /**
     * A database constraint rejected the change (unique, foreign key, check).
     * Services should validate first; this is the safety net, so the message stays generic.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex,
                                                        HttpServletRequest request) {
        return build(ErrorCode.CONFLICT,
                "Dữ liệu xung đột với ràng buộc của hệ thống (trùng lặp hoặc đang được sử dụng)",
                List.of(), request, ex);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(OptimisticLockingFailureException ex,
                                                         HttpServletRequest request) {
        return build(ErrorCode.CONFLICT,
                "Dữ liệu đã được người khác cập nhật. Vui lòng tải lại và thử lại", List.of(), request, ex);
    }

    /** Anything unexpected: log with stack trace, never expose details in production. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        return build(ErrorCode.INTERNAL_ERROR,
                "Đã có lỗi xảy ra. Vui lòng thử lại sau", List.of(), request, ex);
    }

    private ResponseEntity<ApiError> build(ErrorCode code, String message, List<ApiError.FieldErrorDetail> errors,
                                           HttpServletRequest request, Exception ex) {
        String path = request.getRequestURI();
        if (code.status().is5xxServerError()) {
            log.error("{} {} failed: {}", request.getMethod(), path, ex.getMessage(), ex);
        } else {
            // For validation errors log only the field names: exception messages contain the rejected
            // values, which may be passwords or personal data.
            String detail = errors.isEmpty()
                    ? ex.getMessage()
                    : "invalid fields " + errors.stream().map(ApiError.FieldErrorDetail::field).toList();
            log.warn("{} {} -> {} {}: {}", request.getMethod(), path, code.status().value(), code, detail);
        }
        return ResponseEntity.status(code.status()).body(errorFactory.create(code, message, errors, request, ex));
    }
}
