package com.zoopass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point of the ZooPass REST API.
 * The default in-memory user (random password printed at startup) is disabled: users live in the
 * database and log in through the JWT endpoints.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class ZooPassApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZooPassApplication.class, args);
    }
}
