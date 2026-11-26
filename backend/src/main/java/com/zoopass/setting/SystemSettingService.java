package com.zoopass.setting;

import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Typed access to {@link SystemSetting} values for business rules,
 * e.g. {@code getInt("group.min_size")}.
 */
@Service
@Transactional(readOnly = true)
public class SystemSettingService {

    private final SystemSettingRepository repository;

    public SystemSettingService(SystemSettingRepository repository) {
        this.repository = repository;
    }

    /** Settings that guests may see, as an ordered key -> value map. */
    public Map<String, String> getPublicSettings() {
        Map<String, String> result = new LinkedHashMap<>();
        repository.findByPublicSettingTrueOrderByKeyAsc()
                .forEach(setting -> result.put(setting.getKey(), setting.getValue()));
        return result;
    }

    public String getString(String key) {
        return repository.findById(key)
                .map(SystemSetting::getValue)
                // A missing setting is a deployment error (migration not applied), not a user error.
                .orElseThrow(() -> new IllegalStateException("Missing system setting: " + key));
    }

    public int getInt(String key) {
        return Integer.parseInt(getString(key));
    }

    public LocalTime getTime(String key) {
        return LocalTime.parse(getString(key));
    }
}
