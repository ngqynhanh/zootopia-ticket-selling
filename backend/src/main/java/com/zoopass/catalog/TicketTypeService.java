package com.zoopass.catalog;

import static com.zoopass.common.web.SpecificationUtils.containsPattern;
import static com.zoopass.common.web.SpecificationUtils.hasText;

import com.zoopass.audit.AuditAction;
import com.zoopass.audit.AuditLogService;
import com.zoopass.catalog.dto.CatalogRequests.TicketTypeRequest;
import com.zoopass.catalog.dto.CatalogRequests.TicketTypeSearch;
import com.zoopass.catalog.dto.TicketTypeResponse;
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

/**
 * Ticket types: public search for guests (active only) and full CRUD for managers.
 * Delete is a soft delete because existing orders keep referring to the ticket type.
 */
@Service
public class TicketTypeService {

    private static final String ENTITY = "TICKET_TYPE";

    private final TicketTypeRepository repository;
    private final AuditLogService auditLog;

    public TicketTypeService(TicketTypeRepository repository, AuditLogService auditLog) {
        this.repository = repository;
        this.auditLog = auditLog;
    }

    @Transactional(readOnly = true)
    public PageResponse<TicketTypeResponse> search(TicketTypeSearch search, boolean manager, Pageable pageable) {
        if (search.minPrice() != null && search.maxPrice() != null
                && search.minPrice().compareTo(search.maxPrice()) > 0) {
            throw new FieldValidationException("minPrice", "Giá từ phải nhỏ hơn hoặc bằng giá đến");
        }
        // Guests only ever see active ticket types, whatever they ask for.
        Boolean active = manager ? search.active() : Boolean.TRUE;
        return PageResponse.of(repository.findAll(matching(search, active), pageable), TicketTypeResponse::from);
    }

    @Transactional(readOnly = true)
    public TicketTypeResponse get(Long id, boolean manager) {
        TicketType ticketType = find(id);
        if (!manager && !ticketType.isActive()) {
            throw notFound(id);
        }
        return TicketTypeResponse.from(ticketType);
    }

    @Transactional
    public TicketTypeResponse create(TicketTypeRequest request) {
        String code = normalizeCode(request.code());
        if (repository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("Mã loại vé '" + code + "' đã tồn tại");
        }
        TicketType ticketType = new TicketType(code, request.name().trim(), request.category(), request.price());
        apply(ticketType, request);
        repository.saveAndFlush(ticketType);
        TicketTypeResponse created = TicketTypeResponse.from(ticketType);
        auditLog.record(AuditAction.CREATED, ENTITY, ticketType.getId(), null, created);
        return created;
    }

    @Transactional
    public TicketTypeResponse update(Long id, TicketTypeRequest request) {
        TicketType ticketType = find(id);
        String code = normalizeCode(request.code());
        if (repository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new ConflictException("Mã loại vé '" + code + "' đã tồn tại");
        }
        TicketTypeResponse before = TicketTypeResponse.from(ticketType);
        ticketType.setCode(code);
        ticketType.setName(request.name().trim());
        ticketType.setCategory(request.category());
        ticketType.setPrice(request.price());
        apply(ticketType, request);
        repository.saveAndFlush(ticketType);
        TicketTypeResponse after = TicketTypeResponse.from(ticketType);
        auditLog.record(AuditAction.UPDATED, ENTITY, id, before, after);
        return after;
    }

    @Transactional
    public void delete(Long id) {
        TicketType ticketType = find(id);
        ticketType.softDelete();
        auditLog.record(AuditAction.DELETED, ENTITY, id, null, null);
    }

    @Transactional
    public TicketTypeResponse restore(Long id) {
        TicketType ticketType = find(id);
        ticketType.restore();
        auditLog.record(AuditAction.RESTORED, ENTITY, id, null, null);
        return TicketTypeResponse.from(ticketType);
    }

    private static void apply(TicketType ticketType, TicketTypeRequest request) {
        ticketType.setDescription(blankToNull(request.description()));
        ticketType.setMinHeightCm(toShort(request.minHeightCm()));
        ticketType.setMaxHeightCm(toShort(request.maxHeightCm()));
        ticketType.setMinQuantity(request.minQuantity().shortValue());
        ticketType.setEntriesPerUnit(request.entriesPerUnit().shortValue());
        ticketType.setValidityDays(request.validityDays().shortValue());
        ticketType.setDisplayOrder(request.displayOrder() == null ? 0 : request.displayOrder().shortValue());
    }

    private static Specification<TicketType> matching(TicketTypeSearch search, Boolean active) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (hasText(search.q())) {
                String pattern = containsPattern(search.q());
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), pattern, '\\'),
                        cb.like(cb.lower(root.get("name")), pattern, '\\')));
            }
            if (search.category() != null) {
                predicates.add(cb.equal(root.get("category"), search.category()));
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
            if (search.heightCm() != null) {
                // min <= height < max, where a missing bound means "no limit"
                Short height = search.heightCm().shortValue();
                predicates.add(cb.or(cb.isNull(root.get("minHeightCm")),
                        cb.lessThanOrEqualTo(root.get("minHeightCm"), height)));
                predicates.add(cb.or(cb.isNull(root.get("maxHeightCm")),
                        cb.greaterThan(root.get("maxHeightCm"), height)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private TicketType find(Long id) {
        return repository.findById(id).orElseThrow(() -> notFound(id));
    }

    private static ResourceNotFoundException notFound(Long id) {
        return new ResourceNotFoundException("loại vé", id);
    }

    private static String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private static Short toShort(Integer value) {
        return value == null ? null : value.shortValue();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
