package com.example.nextjsbackendadmin.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Application configuration loaded from environment variables / application.properties.
 */
@Validated
@ConfigurationProperties(prefix = "supabase")
public record AppProperties(
        @NotBlank String url,
        @NotBlank String anonKey,
        @NotBlank String serviceRoleKey,
        String storageBucket,
        Db db
) {
    public record Db(String galleryTable) {}
}
