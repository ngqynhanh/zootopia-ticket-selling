package com.zoopass.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoopass.common.exception.ApiErrorFactory;
import com.zoopass.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * 401 / 403 responses produced by the security filter chain (before any controller runs),
 * written in the same {@code ApiError} JSON format as every other error.
 */
@Component
public class SecurityErrorHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(SecurityErrorHandlers.class);

    private final ApiErrorFactory errorFactory;
    private final ObjectMapper objectMapper;

    public SecurityErrorHandlers(ApiErrorFactory errorFactory, ObjectMapper objectMapper) {
        this.errorFactory = errorFactory;
        this.objectMapper = objectMapper;
    }

    /** Not logged in, or the token is missing / invalid / expired. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        write(request, response, ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập để tiếp tục", ex);
    }

    /** Logged in but the URL rule does not allow this role. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        write(request, response, ErrorCode.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này", ex);
    }

    private void write(HttpServletRequest request, HttpServletResponse response, ErrorCode code, String message,
                       Exception ex) throws IOException {
        log.warn("{} {} -> {} {}", request.getMethod(), request.getRequestURI(), code.status().value(), code);
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), errorFactory.create(code, message, List.of(), request, ex));
    }
}
