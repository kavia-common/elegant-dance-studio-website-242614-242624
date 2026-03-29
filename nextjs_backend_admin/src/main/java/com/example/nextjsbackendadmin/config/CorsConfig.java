package com.example.nextjsbackendadmin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Cross-Origin Resource Sharing (CORS) configuration for browser clients.
 *
 * <p>Why: The React frontend runs on a different origin (preview URL / localhost:3000) and calls
 * this backend's /api/** endpoints. Without explicit CORS headers, browsers will block these
 * requests.</p>
 *
 * <p>Configuration:
 * <ul>
 *   <li>Allowed origin is controlled via the FRONTEND_ORIGIN environment variable.</li>
 *   <li>Allows standard methods needed for the app flow: GET/POST/DELETE/OPTIONS.</li>
 *   <li>Allows Authorization header (admin JWT) and Content-Type (multipart upload).</li>
 *   <li>Does not allow credentials (cookies) because auth is bearer-token based.</li>
 * </ul>
 * </p>
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private static final String DEFAULT_FRONTEND_ORIGIN = "http://localhost:3000";

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String frontendOrigin = System.getenv("FRONTEND_ORIGIN");
        if (frontendOrigin == null || frontendOrigin.isBlank()) {
            frontendOrigin = DEFAULT_FRONTEND_ORIGIN;
        }

        // Apply CORS to API routes only (keeps surface area minimal).
        registry.addMapping("/api/**")
                .allowedOrigins(frontendOrigin)
                .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With")
                .exposedHeaders("Location")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
