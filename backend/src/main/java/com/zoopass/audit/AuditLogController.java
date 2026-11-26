package com.zoopass.audit;

import com.zoopass.common.dto.PageResponse;
import com.zoopass.common.web.PageRequests;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only audit trail for administrators. */
@RestController
@RequestMapping("/api/v1/audit-logs")
@Tag(name = "Audit log")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "action", "entityType");

    private final AuditLogRepository repository;

    public AuditLogController(AuditLogRepository repository) {
        this.repository = repository;
    }

    /**
     * @param search filters: actorId, action, entityType, entityId, from, to (all optional)
     */
    public record Search(Long actorId, String action, String entityType, Long entityId, Instant from, Instant to) {
    }

    public record AuditLogResponse(Long id, Long actorId, String actorEmail, String action, String entityType,
                                   Long entityId, Map<String, Object> oldValue, Map<String, Object> newValue,
                                   String ipAddress, String requestId, Instant createdAt) {

        static AuditLogResponse from(AuditLog log) {
            return new AuditLogResponse(log.getId(), log.getActorId(), log.getActorEmail(), log.getAction(),
                    log.getEntityType(), log.getEntityId(), log.getOldValue(), log.getNewValue(),
                    log.getIpAddress(), log.getRequestId(), log.getCreatedAt());
        }
    }

    @Operation(summary = "Search the audit log (admin)")
    @GetMapping
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(Search search,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size,
                                                 @RequestParam(required = false) List<String> sort) {
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search.actorId() != null) {
                predicates.add(cb.equal(root.get("actorId"), search.actorId()));
            }
            if (search.action() != null && !search.action().isBlank()) {
                predicates.add(cb.equal(root.get("action"), search.action().trim().toUpperCase()));
            }
            if (search.entityType() != null && !search.entityType().isBlank()) {
                predicates.add(cb.equal(root.get("entityType"), search.entityType().trim().toUpperCase()));
            }
            if (search.entityId() != null) {
                predicates.add(cb.equal(root.get("entityId"), search.entityId()));
            }
            if (search.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), search.from()));
            }
            if (search.to() != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), search.to()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        var pageable = PageRequests.of(page, size, sort, SORT_FIELDS, Sort.by(Sort.Order.desc("createdAt")));
        return PageResponse.of(repository.findAll(spec, pageable), AuditLogResponse::from);
    }
}
