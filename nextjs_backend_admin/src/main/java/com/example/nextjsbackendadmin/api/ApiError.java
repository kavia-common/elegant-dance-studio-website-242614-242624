package com.example.nextjsbackendadmin.api;

import java.time.OffsetDateTime;

/**
 * Standard error payload returned by the API.
 */
public record ApiError(
        String message,
        String code,
        OffsetDateTime timestamp
) {
    public static ApiError of(String message, String code) {
        return new ApiError(message, code, OffsetDateTime.now());
    }
}
