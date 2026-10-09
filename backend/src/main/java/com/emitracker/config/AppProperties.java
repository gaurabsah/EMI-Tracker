package com.emitracker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Admin admin, Jwt jwt, boolean demoData) {

    public record Admin(String email, String password) {
    }

    public record Jwt(String secret, long expiryMinutes) {
    }
}
