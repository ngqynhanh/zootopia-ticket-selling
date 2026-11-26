package com.zoopass.catalog;

import com.zoopass.catalog.dto.CatalogRequests;
import com.zoopass.catalog.dto.ComboResponse;
import com.zoopass.catalog.dto.TicketTypeResponse;
import com.zoopass.common.dto.PageResponse;
import com.zoopass.common.web.PageRequests;
import com.zoopass.security.CurrentUser;
import com.zoopass.user.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ticket types and combos. GET endpoints are public (guests see active items only; managers also
 * see deactivated ones); changes require MANAGER or ADMIN.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Catalog", description = "Ticket types and experience combos")
public class CatalogController {

    private static final String MANAGER = "hasAnyRole('MANAGER','ADMIN')";
    private static final Set<String> TICKET_SORT = Set.of("displayOrder", "name", "code", "price", "category",
            "createdAt");
    private static final Set<String> COMBO_SORT = Set.of("code", "name", "price", "durationMinutes", "createdAt");

    private final TicketTypeService ticketTypeService;
    private final ComboService comboService;

    public CatalogController(TicketTypeService ticketTypeService, ComboService comboService) {
        this.ticketTypeService = ticketTypeService;
        this.comboService = comboService;
    }

    // ----- ticket types ------------------------------------------------------------------------

    @Operation(summary = "Search ticket types. Filters: q, category, active (managers), minPrice, maxPrice, "
            + "heightCm. Sort example: sort=price:desc")
    @GetMapping("/ticket-types")
    public PageResponse<TicketTypeResponse> searchTicketTypes(
            CatalogRequests.TicketTypeSearch search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> sort,
            @AuthenticationPrincipal CurrentUser user) {
        return ticketTypeService.search(search, isManager(user),
                PageRequests.of(page, size, sort, TICKET_SORT, Sort.by("displayOrder", "id")));
    }

    @Operation(summary = "Get one ticket type")
    @GetMapping("/ticket-types/{id}")
    public TicketTypeResponse getTicketType(@PathVariable Long id, @AuthenticationPrincipal CurrentUser user) {
        return ticketTypeService.get(id, isManager(user));
    }

    @Operation(summary = "Create a ticket type (manager)")
    @PostMapping("/ticket-types")
    @PreAuthorize(MANAGER)
    public ResponseEntity<TicketTypeResponse> createTicketType(
            @Valid @RequestBody CatalogRequests.TicketTypeRequest request) {
        TicketTypeResponse created = ticketTypeService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/ticket-types/" + created.id())).body(created);
    }

    @Operation(summary = "Update a ticket type (manager)")
    @PutMapping("/ticket-types/{id}")
    @PreAuthorize(MANAGER)
    public TicketTypeResponse updateTicketType(@PathVariable Long id,
                                               @Valid @RequestBody CatalogRequests.TicketTypeRequest request) {
        return ticketTypeService.update(id, request);
    }

    @Operation(summary = "Soft-delete a ticket type (manager); existing orders keep it")
    @DeleteMapping("/ticket-types/{id}")
    @PreAuthorize(MANAGER)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTicketType(@PathVariable Long id) {
        ticketTypeService.delete(id);
    }

    @Operation(summary = "Restore a soft-deleted ticket type (manager)")
    @PatchMapping("/ticket-types/{id}/restore")
    @PreAuthorize(MANAGER)
    public TicketTypeResponse restoreTicketType(@PathVariable Long id) {
        return ticketTypeService.restore(id);
    }

    // ----- combos ------------------------------------------------------------------------------

    @Operation(summary = "Search combos. Filters: q, active (managers), minPrice, maxPrice, maxDuration, "
            + "groupSize, age. Sort example: sort=price:asc")
    @GetMapping("/combos")
    public PageResponse<ComboResponse> searchCombos(
            CatalogRequests.ComboSearch search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) List<String> sort,
            @AuthenticationPrincipal CurrentUser user) {
        return comboService.search(search, isManager(user),
                PageRequests.of(page, size, sort, COMBO_SORT, Sort.by("code", "id")));
    }

    @Operation(summary = "Get one combo")
    @GetMapping("/combos/{id}")
    public ComboResponse getCombo(@PathVariable Long id, @AuthenticationPrincipal CurrentUser user) {
        return comboService.get(id, isManager(user));
    }

    @Operation(summary = "Create a combo (manager)")
    @PostMapping("/combos")
    @PreAuthorize(MANAGER)
    public ResponseEntity<ComboResponse> createCombo(@Valid @RequestBody CatalogRequests.ComboRequest request) {
        ComboResponse created = comboService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/combos/" + created.id())).body(created);
    }

    @Operation(summary = "Update a combo (manager)")
    @PutMapping("/combos/{id}")
    @PreAuthorize(MANAGER)
    public ComboResponse updateCombo(@PathVariable Long id, @Valid @RequestBody CatalogRequests.ComboRequest request) {
        return comboService.update(id, request);
    }

    @Operation(summary = "Soft-delete a combo (manager)")
    @DeleteMapping("/combos/{id}")
    @PreAuthorize(MANAGER)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCombo(@PathVariable Long id) {
        comboService.delete(id);
    }

    @Operation(summary = "Restore a soft-deleted combo (manager)")
    @PatchMapping("/combos/{id}/restore")
    @PreAuthorize(MANAGER)
    public ComboResponse restoreCombo(@PathVariable Long id) {
        return comboService.restore(id);
    }

    private static boolean isManager(CurrentUser user) {
        return user != null && user.hasAnyRole(Role.MANAGER, Role.ADMIN);
    }
}
