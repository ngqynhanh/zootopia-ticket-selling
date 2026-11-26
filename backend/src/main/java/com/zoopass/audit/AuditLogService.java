package com.zoopass.audit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoopass.common.web.RequestIdFilter;
import com.zoopass.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Writes the audit trail: who did what to which record, with old and new values.
 *
 * <p>Each entry is saved in its own transaction ({@code REQUIRES_NEW}) so it is kept even when the
 * surrounding operation fails, for example a failed login attempt that ends with an exception.
 */
@Service
public class AuditLogService {

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {
    };

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /** Records an action by the currently logged-in user (or the system if anonymous). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditAction action, String entityType, Long entityId, Object oldValue, Object newValue) {
        CurrentUser actor = currentUser();
        save(actor == null ? null : actor.id(), actor == null ? null : actor.email(),
                action, entityType, entityId, oldValue, newValue);
    }

    /** Records an action whose actor is known but not logged in yet (login, registration). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFor(Long actorId, String actorEmail, AuditAction action, String entityType, Long entityId,
                          Object newValue) {
        save(actorId, actorEmail, action, entityType, entityId, null, newValue);
    }

    private void save(Long actorId, String actorEmail, AuditAction action, String entityType, Long entityId,
                      Object oldValue, Object newValue) {
        HttpServletRequest request = currentRequest();
        repository.save(new AuditLog(actorId, actorEmail, action.name(), entityType, entityId,
                toMap(oldValue), toMap(newValue),
                request == null ? null : request.getRemoteAddr(),
                MDC.get(RequestIdFilter.MDC_REQUEST_ID)));
    }

    private Map<String, Object> toMap(Object value) {
        return value == null ? null : objectMapper.convertValue(value, MAP);
    }

    private static CurrentUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof CurrentUser user ? user : null;
    }

    private static HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? attributes.getRequest()
                : null;
    }
}
