package com.example.nextjsbackendadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Admin JWT settings.
 *
 * If secret is blank, admin auth filter will reject all admin calls to avoid accidental exposure.
 */
@ConfigurationProperties(prefix = "admin.jwt")
public record AdminAuthProperties(
        String secret,
        String issuer
) {}
