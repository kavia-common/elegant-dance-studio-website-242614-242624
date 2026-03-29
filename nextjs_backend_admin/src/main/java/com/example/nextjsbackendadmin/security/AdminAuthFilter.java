package com.example.nextjsbackendadmin.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.nextjsbackendadmin.config.AdminAuthProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Simple JWT-based guard for admin endpoints.
 *
 * Expected header:
 *   Authorization: Bearer <jwt>
 */
public class AdminAuthFilter extends OncePerRequestFilter {

    private final AdminAuthProperties props;

    public AdminAuthFilter(AdminAuthProperties props) {
        this.props = props;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Only protect admin routes
        return !path.startsWith("/api/admin/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Fail-closed: if secret is not configured, reject all admin routes.
        if (props.secret() == null || props.secret().isBlank()) {
            writeUnauthorized(response, "Admin auth is not configured");
            return;
        }

        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Bearer ")) {
            writeUnauthorized(response, "Missing bearer token");
            return;
        }

        String token = auth.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            writeUnauthorized(response, "Missing bearer token");
            return;
        }

        try {
            Algorithm alg = Algorithm.HMAC256(props.secret().getBytes(StandardCharsets.UTF_8));
            JWTVerifier.BaseVerification verification = (JWTVerifier.BaseVerification) JWT.require(alg);
            if (props.issuer() != null && !props.issuer().isBlank()) {
                verification.withIssuer(props.issuer());
            }
            JWTVerifier verifier = verification.build();
            DecodedJWT jwt = verifier.verify(token);

            // Optional: basic audience/claims checks could be added here if needed.
            request.setAttribute("admin.sub", jwt.getSubject());
        } catch (Exception ex) {
            writeUnauthorized(response, "Invalid token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"message\":\"" + message.replace("\"", "\\\"") + "\"}");
    }
}
