package com.example.nextjsbackendadmin;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entrypoint.
 */
@SpringBootApplication
@OpenAPIDefinition(
        info = @Info(
                title = "Dance Studio Admin API",
                version = "0.1.0",
                description = "Admin backend API for managing gallery images. Uses Supabase (DB + Storage)."),
        tags = {
                @Tag(name = "System", description = "Health and discovery endpoints"),
                @Tag(name = "Admin Auth", description = "Admin authentication endpoints"),
                @Tag(name = "Admin Gallery", description = "Admin-only gallery management endpoints")
        })
public class NextjsbackendadminApplication {

    public static void main(String[] args) {
        SpringApplication.run(NextjsbackendadminApplication.class, args);
    }
}
