package com.zoopass.common.exception;

import com.zoopass.common.web.RequestIdFilter;
import com.zoopass.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Builds {@link ApiError} bodies. Shared by the MVC exception handler and the security
 * filter-chain handlers (401/403 happen before a controller is reached).
 */
@Component
public class ApiErrorFactory {

    private final boolean includeDebugDetail;

    public ApiErrorFactory(AppProperties properties) {
        this.includeDebugDetail = properties.errors() != null && properties.errors().includeDebugDetail();
    }

    public ApiError create(ErrorCode code, String message, List<ApiError.FieldErrorDetail> errors,
                           HttpServletRequest request, Exception ex) {
        return new ApiError(
                Instant.now(),
                code.status().value(),
                code.name(),
                message,
                errors == null || errors.isEmpty() ? null : errors,
                request.getRequestURI(),
                MDC.get(RequestIdFilter.MDC_REQUEST_ID),
                // Field errors already explain the problem; the exception text would echo rejected values.
                includeDebugDetail && ex != null && (errors == null || errors.isEmpty())
                        ? ex.getClass().getSimpleName() + ": " + ex.getMessage()
                        : null);
    }
}
