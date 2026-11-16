package com.zoopass.catalog;

import com.zoopass.common.entity.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * A sellable ticket (gate, train, tour, pass, service) with its current price and eligibility.
 */
@Entity
@Table(name = "ticket_types")
public class TicketType extends SoftDeletableEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketCategory category;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    /** Inclusive lower bound of visitor height, null = no lower bound. */
    @Column(name = "min_height_cm")
    private Short minHeightCm;

    /** Exclusive upper bound of visitor height, null = no upper bound. */
    @Column(name = "max_height_cm")
    private Short maxHeightCm;

    /** Minimum quantity per order (6 for electric-car tours). */
    @Column(name = "min_quantity", nullable = false)
    private short minQuantity = 1;

    /** Gate entries one unit gives (10 for a ticket book). */
    @Column(name = "entries_per_unit", nullable = false)
    private short entriesPerUnit = 1;

    @Column(name = "validity_days", nullable = false)
    private short validityDays = 30;

    @Column(name = "display_order", nullable = false)
    private short displayOrder;

    protected TicketType() {
        // for JPA
    }

    public TicketType(String code, String name, TicketCategory category, BigDecimal price) {
        this.code = code;
        this.name = name;
        this.category = category;
        this.price = price;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TicketCategory getCategory() {
        return category;
    }

    public void setCategory(TicketCategory category) {
        this.category = category;
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

    public Short getMinHeightCm() {
        return minHeightCm;
    }

    public void setMinHeightCm(Short minHeightCm) {
        this.minHeightCm = minHeightCm;
    }

    public Short getMaxHeightCm() {
        return maxHeightCm;
    }

    public void setMaxHeightCm(Short maxHeightCm) {
        this.maxHeightCm = maxHeightCm;
    }

    public short getMinQuantity() {
        return minQuantity;
    }

    public void setMinQuantity(short minQuantity) {
        this.minQuantity = minQuantity;
    }

    public short getEntriesPerUnit() {
        return entriesPerUnit;
    }

    public void setEntriesPerUnit(short entriesPerUnit) {
        this.entriesPerUnit = entriesPerUnit;
    }

    public short getValidityDays() {
        return validityDays;
    }

    public void setValidityDays(short validityDays) {
        this.validityDays = validityDays;
    }

    public short getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(short displayOrder) {
        this.displayOrder = displayOrder;
    }
}
