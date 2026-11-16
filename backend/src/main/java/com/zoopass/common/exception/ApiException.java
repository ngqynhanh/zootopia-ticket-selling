package com.zoopass.common.exception;

/**
 * Base class for expected errors thrown by services. The {@link GlobalExceptionHandler}
 * turns them into an {@link ApiError} with the status of their {@link ErrorCode}.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
