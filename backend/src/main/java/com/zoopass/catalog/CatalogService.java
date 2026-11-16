package com.zoopass.catalog;

import com.zoopass.catalog.dto.ComboResponse;
import com.zoopass.catalog.dto.TicketTypeResponse;
import com.zoopass.common.dto.PageResponse;
import com.zoopass.common.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read side of the catalog (public price page).
 * Week 2 adds create/update/soft-delete for managers.
 */
@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final TicketTypeRepository ticketTypeRepository;
    private final ComboRepository comboRepository;

    public CatalogService(TicketTypeRepository ticketTypeRepository, ComboRepository comboRepository) {
        this.ticketTypeRepository = ticketTypeRepository;
        this.comboRepository = comboRepository;
    }

    /** Active ticket types in display order, optionally only one category. */
    public List<TicketTypeResponse> listTicketTypes(TicketCategory category) {
        List<TicketType> ticketTypes = category == null
                ? ticketTypeRepository.findByActiveTrueOrderByDisplayOrderAsc()
                : ticketTypeRepository.findByActiveTrueAndCategoryOrderByDisplayOrderAsc(category);
        return ticketTypes.stream().map(TicketTypeResponse::from).toList();
    }

    public TicketTypeResponse getTicketType(Long id) {
        return ticketTypeRepository.findByIdAndActiveTrue(id)
                .map(TicketTypeResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("loại vé", id));
    }

    public PageResponse<ComboResponse> listCombos(Pageable pageable) {
        return PageResponse.of(comboRepository.findByActiveTrue(pageable), ComboResponse::from);
    }

    public ComboResponse getCombo(Long id) {
        return comboRepository.findByIdAndActiveTrue(id)
                .map(ComboResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("combo", id));
    }
}
