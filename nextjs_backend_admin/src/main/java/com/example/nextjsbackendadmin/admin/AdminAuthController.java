package com.example.nextjsbackendadmin.admin;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.example.nextjsbackendadmin.config.AdminAuthProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * Admin login endpoint that issues a JWT used to access /api/admin/** routes.
 *
 * This is intentionally minimal. In production, prefer Supabase Auth / RLS and verify Supabase JWTs.
 */
@RestController
@RequestMapping(value = "/api/admin/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Admin Auth")
public class AdminAuthController {

    private final AdminAuthProperties authProps;

    public AdminAuthController(AdminAuthProperties authProps) {
        this.authProps = authProps;
    }

    public record LoginRequest(@NotBlank String password) {}
    public record LoginResponse(String token, long expiresInSeconds) {}

    @PostMapping("/login")
    @Operation(
            summary = "Admin login",
            description = "Returns a JWT token for subsequent admin API calls. Requires ADMIN_JWT_SECRET and ADMIN_PASSWORD.")
    public LoginResponse login(@Valid @RequestBody LoginRequest req, @RequestHeader Map<String, String> headers) {
        String secret = authProps.secret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("ADMIN_JWT_SECRET is not configured");
        }

        // Password must be supplied via env var; do not hardcode.
        String expected = System.getenv("ADMIN_PASSWORD");
        if (expected == null || expected.isBlank()) {
            throw new IllegalArgumentException("ADMIN_PASSWORD is not configured");
        }

        if (!expected.equals(req.password())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        Algorithm alg = Algorithm.HMAC256(secret.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        long expiresIn = 60L * 60L * 8L; // 8 hours
        String token = JWT.create()
                .withSubject("admin")
                .withIssuer(authProps.issuer() == null ? "nextjs_backend_admin" : authProps.issuer())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusSeconds(expiresIn)))
                .sign(alg);

        return new LoginResponse(token, expiresIn);
    }
}
