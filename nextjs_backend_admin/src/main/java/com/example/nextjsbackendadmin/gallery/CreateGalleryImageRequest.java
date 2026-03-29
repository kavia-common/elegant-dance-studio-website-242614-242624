package com.example.nextjsbackendadmin.gallery;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body to create a gallery image record.
 */
public record CreateGalleryImageRequest(
        @NotBlank String imageUrl,
        String altText
) {}
