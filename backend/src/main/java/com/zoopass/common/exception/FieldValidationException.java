package com.zoopass.common.exception;

import java.util.List;

/**
 * 400 - validation that can only be done in a service (e.g. "current password is wrong",
 * "cannot sort by this field"), reported with the same per-field shape as Bean Validation errors.
 */
public class FieldValidationException extends ApiException {

    private final List<ApiError.FieldErrorDetail> errors;

    public FieldValidationException(String field, String message) {
        super(ErrorCode.VALIDATION_FAILED, "Dữ liệu không hợp lệ");
        this.errors = List.of(new ApiError.FieldErrorDetail(field, message));
    }

    public List<ApiError.FieldErrorDetail> getErrors() {
        return errors;
    }
}
