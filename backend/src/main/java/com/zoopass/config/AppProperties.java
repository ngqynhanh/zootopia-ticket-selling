package com.zoopass.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed view of the {@code app.*} settings in application.yml.
 *
 * @param cors   allowed frontend origins
 * @param errors error response options
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Cors cors, Errors errors) {

    /** @param allowedOrigins origins allowed to call the API from a browser */
    public record Cors(List<String> allowedOrigins) {
    }

    /** @param includeDebugDetail add exception detail to error bodies (dev only) */
    public record Errors(boolean includeDebugDetail) {
    }
}
