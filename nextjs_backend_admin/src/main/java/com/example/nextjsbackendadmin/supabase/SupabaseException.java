package com.example.nextjsbackendadmin.supabase;

/**
 * Exception thrown when Supabase returns a non-2xx response.
 */
public class SupabaseException extends RuntimeException {
    private final int statusCode;

    public SupabaseException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }
}
