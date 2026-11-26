package com.zoopass.catalog.dto;

import com.zoopass.catalog.Combo;
import java.math.BigDecimal;
import java.time.Instant;

/** An experience combo as returned by the API. */
public record ComboResponse(
        Long id,
        String code,
        String name,
        String description,
        BigDecimal price,
        short durationMinutes,
        short minParticipants,
        Short maxParticipants,
        Short minAge,
        String imageUrl,
        boolean active,
        Instant updatedAt) {

    public static ComboResponse from(Combo combo) {
        return new ComboResponse(
                combo.getId(),
                combo.getCode(),
                combo.getName(),
                combo.getDescription(),
                combo.getPrice(),
                combo.getDurationMinutes(),
                combo.getMinParticipants(),
                combo.getMaxParticipants(),
                combo.getMinAge(),
                combo.getImageUrl(),
                combo.isActive(),
                combo.getUpdatedAt());
    }
}
