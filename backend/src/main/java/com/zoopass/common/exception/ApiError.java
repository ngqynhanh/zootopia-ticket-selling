package com.zoopass.common.exception;

import java.time.Instant;
import java.util.List;

/**
 * Body of every error response.
 *
 * @param timestamp when the error happened
 * @param status    HTTP status code
 * @param code      machine-readable {@link ErrorCode}
 * @param message   user-friendly message (Vietnamese)
 * @param errors    per-field validation errors, if any
 * @param path      request path
 * @param requestId correlation id, same as the X-Request-Id header and the log line
 * @param debug     exception detail, only when app.errors.include-debug-detail=true (dev)
 */
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        List<FieldErrorDetail> errors,
        String path,
        String requestId,
        String debug) {

    /**
     * One invalid field.
     *
     * @param field   field path, e.g. {@code items[1].quantity}
     * @param message what is wrong with it
     */
    public record FieldErrorDetail(String field, String message) {
    }
}
