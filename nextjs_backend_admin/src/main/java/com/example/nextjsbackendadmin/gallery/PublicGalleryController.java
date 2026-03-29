package com.example.nextjsbackendadmin.gallery;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Public gallery endpoints for the frontend website.
 */
@RestController
@RequestMapping(value = "/api/gallery", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Public Gallery", description = "Public endpoints for displaying gallery images")
public class PublicGalleryController {

    private final GalleryService galleryService;

    public PublicGalleryController(GalleryService galleryService) {
        this.galleryService = galleryService;
    }

    @GetMapping
    @Operation(summary = "List gallery images", description = "Lists the most recent gallery images from Supabase DB.")
    public Mono<List<GalleryImage>> list(@RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit) {
        return galleryService.listImages(limit);
    }
}
