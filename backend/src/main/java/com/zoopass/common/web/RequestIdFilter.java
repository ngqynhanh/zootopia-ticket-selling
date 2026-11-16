package com.zoopass.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gives every request a correlation id and writes one access-log line per API call.
 *
 * <p>The id comes from the {@code X-Request-Id} header when the client sends a safe value,
 * otherwise a new UUID. It is put in the logging MDC (so every log line of the request shows it),
 * echoed in the response header and copied into {@code ApiError.requestId}, which lets us find
 * the server log for an error the user reports.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_REQUEST_ID = "requestId";

    private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);
    // Accept only short, harmless ids from clients (prevents log injection).
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9-]{8,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String incoming = request.getHeader(HEADER);
        String requestId = incoming != null && SAFE_ID.matcher(incoming).matches()
                ? incoming
                : UUID.randomUUID().toString();

        long start = System.nanoTime();
        MDC.put(MDC_REQUEST_ID, requestId);
        response.setHeader(HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            if (request.getRequestURI().startsWith("/api/")) {
                long millis = (System.nanoTime() - start) / 1_000_000;
                log.info("{} {} -> {} ({} ms)", request.getMethod(), request.getRequestURI(),
                        response.getStatus(), millis);
            }
            MDC.remove(MDC_REQUEST_ID);
        }
    }
}
