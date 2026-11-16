package com.zoopass.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadata shown on Swagger UI ({@code /swagger-ui.html}).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI zooPassOpenApi() {
        return new OpenAPI().info(new Info()
                .title("ZooPass API")
                .version("v1")
                .description("Online ticketing and visit planning for Saigon Zoo (course project)."));
    }
}
