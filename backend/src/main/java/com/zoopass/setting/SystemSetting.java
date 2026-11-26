package com.zoopass.setting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Key/value configuration editable by an admin at runtime
 * (opening hours, ticket sales cut-off, group size threshold...), so business rules
 * never hard-code these numbers.
 */
@Entity
@Table(name = "system_settings")
public class SystemSetting {

    @Id
    @Column(name = "setting_key", length = 60)
    private String key;

    @Column(name = "setting_value", nullable = false, length = 500)
    private String value;

    @Column(name = "value_type", nullable = false, length = 10)
    private String valueType;

    @Column(length = 300)
    private String description;

    /** Public settings may be shown to guests (e.g. opening hours). */
    @Column(name = "is_public", nullable = false)
    private boolean publicSetting;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    protected SystemSetting() {
        // for JPA
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public String getValueType() {
        return valueType;
    }

    public String getDescription() {
        return description;
    }

    public boolean isPublicSetting() {
        return publicSetting;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }
}
