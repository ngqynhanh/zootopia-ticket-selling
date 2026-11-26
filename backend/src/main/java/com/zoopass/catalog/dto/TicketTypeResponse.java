package com.zoopass.catalog.dto;

import com.zoopass.catalog.TicketCategory;
import com.zoopass.catalog.TicketType;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * A ticket type as returned by the API.
 *
 * @param minHeightCm inclusive lower height bound, null = none
 * @param maxHeightCm exclusive upper height bound, null = none
 * @param active      false when soft-deleted (only managers see those)
 */
public record TicketTypeResponse(
        Long id,
        String code,
        String name,
        TicketCategory category,
        String description,
        BigDecimal price,
        Short minHeightCm,
        Short maxHeightCm,
        short minQuantity,
        short entriesPerUnit,
        short validityDays,
        short displayOrder,
        boolean active,
        Instant updatedAt) {

    public static TicketTypeResponse from(TicketType ticketType) {
        return new TicketTypeResponse(
                ticketType.getId(),
                ticketType.getCode(),
                ticketType.getName(),
                ticketType.getCategory(),
                ticketType.getDescription(),
                ticketType.getPrice(),
                ticketType.getMinHeightCm(),
                ticketType.getMaxHeightCm(),
                ticketType.getMinQuantity(),
                ticketType.getEntriesPerUnit(),
                ticketType.getValidityDays(),
                ticketType.getDisplayOrder(),
                ticketType.isActive(),
                ticketType.getUpdatedAt());
    }
}
