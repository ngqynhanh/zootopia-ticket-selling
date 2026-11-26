package com.zoopass.catalog;

import com.zoopass.common.entity.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * An experience combo (e.g. "Nhà khoa học tí hon"): a priced package of activities.
 * The zones it visits are in the {@code combo_zones} junction table (mapped in week 3).
 */
@Entity
@Table(name = "combos")
public class Combo extends SoftDeletableEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "duration_minutes", nullable = false)
    private short durationMinutes;

    @Column(name = "min_participants", nullable = false)
    private short minParticipants = 1;

    @Column(name = "max_participants")
    private Short maxParticipants;

    @Column(name = "min_age")
    private Short minAge;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    protected Combo() {
        // for JPA
    }

    public Combo(String code, String name, BigDecimal price, short durationMinutes) {
        this.code = code;
        this.name = name;
        this.price = price;
        this.durationMinutes = durationMinutes;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public short getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(short durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public short getMinParticipants() {
        return minParticipants;
    }

    public void setMinParticipants(short minParticipants) {
        this.minParticipants = minParticipants;
    }

    public Short getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(Short maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public Short getMinAge() {
        return minAge;
    }

    public void setMinAge(Short minAge) {
        this.minAge = minAge;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
