package com.example.nextjsbackendadmin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Basic non-auth endpoints used for health checks and developer convenience.
 */
@RestController
@Tag(name = "System", description = "Basic endpoints for service health and discovery")
public class HelloController {

    @GetMapping("/")
    @Operation(summary = "Welcome endpoint", description = "Returns a welcome message.")
    public String hello() {
        return "Hello, Spring Boot! Welcome to nextjsbackendadmin";
    }

    @GetMapping("/docs")
    @Operation(
            summary = "API Documentation",
            description = "Redirects to Swagger UI, preserving original scheme/host/port (honors X-Forwarded-* headers).")
    public RedirectView docs(HttpServletRequest request) {
        // Note: fromHttpRequest is deprecated but acceptable here; it preserves the original request URL with forwarded headers.
        String target = UriComponentsBuilder
                .fromHttpRequest(new ServletServerHttpRequest(request))
                .replacePath("/swagger-ui.html")
                .replaceQuery(null)
                .build()
                .toUriString();

        RedirectView rv = new RedirectView(target);
        rv.setHttp10Compatible(false);
        return rv;
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Returns application health status.")
    public String health() {
        return "OK";
    }

    @GetMapping("/api/info")
    @Operation(summary = "Application info", description = "Returns application information.")
    public String info() {
        return "Spring Boot Application: nextjsbackendadmin";
    }
}
