package com.example.nextjsbackendadmin.gallery;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * Represents a row in the Supabase gallery_images table.
 */
public record GalleryImage(
        String id,
        @JsonProperty("image_url") String imageUrl,
        @JsonProperty("alt_text") String altText,
        @JsonProperty("created_at") OffsetDateTime createdAt
) {}
