package com.zoopass.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;

/**
 * Entity that is never physically deleted: "delete" marks it inactive and records when.
 * Used for master data that orders still reference (ticket types, combos, zones, users...).
 */
@MappedSuperclass
public abstract class SoftDeletableEntity extends BaseEntity {

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /** Marks the entity as deleted. Calling it twice keeps the first deletion time. */
    public void softDelete() {
        if (active) {
            active = false;
            deletedAt = Instant.now();
        }
    }

    /** Undoes {@link #softDelete()}. */
    public void restore() {
        active = true;
        deletedAt = null;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
