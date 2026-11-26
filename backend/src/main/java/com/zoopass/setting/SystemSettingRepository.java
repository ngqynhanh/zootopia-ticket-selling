package com.zoopass.setting;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link SystemSetting}. */
public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {

    List<SystemSetting> findByPublicSettingTrueOrderByKeyAsc();
}
