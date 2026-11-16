package com.zoopass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point of the ZooPass REST API.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ZooPassApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZooPassApplication.class, args);
    }
}
