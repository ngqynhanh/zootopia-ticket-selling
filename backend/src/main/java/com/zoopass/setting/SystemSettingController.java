package com.zoopass.setting;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes public settings (opening hours, group size...) to the frontend. */
@RestController
@RequestMapping("/api/v1/settings")
@Tag(name = "Settings")
public class SystemSettingController {

    private final SystemSettingService settingService;

    public SystemSettingController(SystemSettingService settingService) {
        this.settingService = settingService;
    }

    @Operation(summary = "Public system settings as key/value pairs")
    @GetMapping("/public")
    public Map<String, String> getPublicSettings() {
        return settingService.getPublicSettings();
    }
}
