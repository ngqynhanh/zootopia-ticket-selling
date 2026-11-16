package com.zoopass.catalog.dto;

import com.zoopass.catalog.Combo;
import java.math.BigDecimal;

/** Public view of an experience combo. */
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
        String imageUrl) {

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
                combo.getImageUrl());
    }
}
