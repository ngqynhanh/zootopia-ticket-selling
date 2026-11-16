package com.zoopass.catalog;

import com.zoopass.catalog.dto.ComboResponse;
import com.zoopass.catalog.dto.TicketTypeResponse;
import com.zoopass.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, read-only catalog endpoints used by the price page.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Catalog", description = "Ticket types and experience combos")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Operation(summary = "List active ticket types, optionally filtered by category")
    @GetMapping("/ticket-types")
    public List<TicketTypeResponse> listTicketTypes(@RequestParam(required = false) TicketCategory category) {
        return catalogService.listTicketTypes(category);
    }

    @Operation(summary = "Get one active ticket type")
    @GetMapping("/ticket-types/{id}")
    public TicketTypeResponse getTicketType(@PathVariable Long id) {
        return catalogService.getTicketType(id);
    }

    @Operation(summary = "List active combos (paginated)")
    @GetMapping("/combos")
    public PageResponse<ComboResponse> listCombos(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Số trang không được âm") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Kích thước trang tối thiểu là 1")
            @Max(value = 100, message = "Kích thước trang tối đa là 100") int size,
            @RequestParam(defaultValue = "code")
            @Pattern(regexp = "code|name|price|durationMinutes", message = "Chỉ sắp xếp theo code, name, price, durationMinutes")
            String sortBy,
            @RequestParam(defaultValue = "asc")
            @Pattern(regexp = "asc|desc", message = "Chiều sắp xếp phải là asc hoặc desc") String direction) {
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        return catalogService.listCombos(PageRequest.of(page, size, sort));
    }

    @Operation(summary = "Get one active combo")
    @GetMapping("/combos/{id}")
    public ComboResponse getCombo(@PathVariable Long id) {
        return catalogService.getCombo(id);
    }
}
