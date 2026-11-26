package com.zoopass.catalog;

import static com.zoopass.common.web.SpecificationUtils.containsPattern;
import static com.zoopass.common.web.SpecificationUtils.hasText;

import com.zoopass.audit.AuditAction;
import com.zoopass.audit.AuditLogService;
import com.zoopass.catalog.dto.CatalogRequests.ComboRequest;
import com.zoopass.catalog.dto.CatalogRequests.ComboSearch;
import com.zoopass.catalog.dto.ComboResponse;
import com.zoopass.common.dto.PageResponse;
import com.zoopass.common.exception.ConflictException;
import com.zoopass.common.exception.FieldValidationException;
import com.zoopass.common.exception.ResourceNotFoundException;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Experience combos: public search (active only) and manager CRUD with soft delete. */
@Service
public class ComboService {

    private static final String ENTITY = "COMBO";

    private final ComboRepository repository;
    private final AuditLogService auditLog;

    public ComboService(ComboRepository repository, AuditLogService auditLog) {
        this.repository = repository;
        this.auditLog = auditLog;
    }

    @Transactional(readOnly = true)
    public PageResponse<ComboResponse> search(ComboSearch search, boolean manager, Pageable pageable) {
        if (search.minPrice() != null && search.maxPrice() != null
                && search.minPrice().compareTo(search.maxPrice()) > 0) {
            throw new FieldValidationException("minPrice", "Giá từ phải nhỏ hơn hoặc bằng giá đến");
        }
        Boolean active = manager ? search.active() : Boolean.TRUE;
        return PageResponse.of(repository.findAll(matching(search, active), pageable), ComboResponse::from);
    }

    @Transactional(readOnly = true)
    public ComboResponse get(Long id, boolean manager) {
        Combo combo = find(id);
        if (!manager && !combo.isActive()) {
            throw notFound(id);
        }
        return ComboResponse.from(combo);
    }

    @Transactional
    public ComboResponse create(ComboRequest request) {
        String code = normalizeCode(request.code());
        if (repository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("Mã combo '" + code + "' đã tồn tại");
        }
        Combo combo = new Combo(code, request.name().trim(), request.price(), request.durationMinutes().shortValue());
        apply(combo, request);
        repository.saveAndFlush(combo);
        ComboResponse created = ComboResponse.from(combo);
        auditLog.record(AuditAction.CREATED, ENTITY, combo.getId(), null, created);
        return created;
    }

    @Transactional
    public ComboResponse update(Long id, ComboRequest request) {
        Combo combo = find(id);
        String code = normalizeCode(request.code());
        if (repository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new ConflictException("Mã combo '" + code + "' đã tồn tại");
        }
        ComboResponse before = ComboResponse.from(combo);
        combo.setCode(code);
        combo.setName(request.name().trim());
        combo.setPrice(request.price());
        combo.setDurationMinutes(request.durationMinutes().shortValue());
        apply(combo, request);
        repository.saveAndFlush(combo);
        ComboResponse after = ComboResponse.from(combo);
        auditLog.record(AuditAction.UPDATED, ENTITY, id, before, after);
        return after;
    }

    @Transactional
    public void delete(Long id) {
        find(id).softDelete();
        auditLog.record(AuditAction.DELETED, ENTITY, id, null, null);
    }

    @Transactional
    public ComboResponse restore(Long id) {
        Combo combo = find(id);
        combo.restore();
        auditLog.record(AuditAction.RESTORED, ENTITY, id, null, null);
        return ComboResponse.from(combo);
    }

    private static void apply(Combo combo, ComboRequest request) {
        combo.setDescription(blankToNull(request.description()));
        combo.setMinParticipants(request.minParticipants().shortValue());
        combo.setMaxParticipants(request.maxParticipants() == null ? null : request.maxParticipants().shortValue());
        combo.setMinAge(request.minAge() == null ? null : request.minAge().shortValue());
        combo.setImageUrl(blankToNull(request.imageUrl()));
    }

    private static Specification<Combo> matching(ComboSearch search, Boolean active) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (hasText(search.q())) {
                String pattern = containsPattern(search.q());
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), pattern, '\\'),
                        cb.like(cb.lower(root.get("name")), pattern, '\\'),
                        cb.like(cb.lower(cb.coalesce(root.get("description"), "")), pattern, '\\')));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            if (search.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), search.minPrice()));
            }
            if (search.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), search.maxPrice()));
            }
            if (search.maxDuration() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("durationMinutes"), search.maxDuration().shortValue()));
            }
            if (search.groupSize() != null) {
                Short size = search.groupSize().shortValue();
                predicates.add(cb.lessThanOrEqualTo(root.get("minParticipants"), size));
                predicates.add(cb.or(cb.isNull(root.get("maxParticipants")),
                        cb.greaterThanOrEqualTo(root.get("maxParticipants"), size)));
            }
            if (search.age() != null) {
                predicates.add(cb.or(cb.isNull(root.get("minAge")),
                        cb.lessThanOrEqualTo(root.get("minAge"), search.age().shortValue())));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Combo find(Long id) {
        return repository.findById(id).orElseThrow(() -> notFound(id));
    }

    private static ResourceNotFoundException notFound(Long id) {
        return new ResourceNotFoundException("combo", id);
    }

    private static String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
