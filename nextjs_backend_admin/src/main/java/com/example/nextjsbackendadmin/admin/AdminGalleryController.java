package com.example.nextjsbackendadmin.admin;

import com.example.nextjsbackendadmin.gallery.GalleryImage;
import com.example.nextjsbackendadmin.gallery.GalleryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Admin-only gallery operations.
 *
 * Protected by AdminAuthFilter (Authorization: Bearer <token>).
 */
@RestController
@RequestMapping(value = "/api/admin/gallery", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Admin Gallery")
public class AdminGalleryController {

    private final GalleryService galleryService;

    public AdminGalleryController(GalleryService galleryService) {
        this.galleryService = galleryService;
    }

    @GetMapping
    @Operation(summary = "List gallery images (admin)", description = "Lists the most recent gallery images from Supabase DB.")
    public Mono<List<GalleryImage>> list(
            @Parameter(description = "Maximum number of rows to return (1..200). Default 50.")
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit
    ) {
        return galleryService.listImages(limit);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload image (admin)",
            description = "Uploads an image to Supabase Storage, then inserts a row into the gallery table.")
    public Mono<GalleryImage> upload(
            @Parameter(description = "Image file to upload") @RequestPart("file") MultipartFile file,
            @Parameter(description = "Optional alt text for accessibility") @RequestPart(value = "altText", required = false) String altText
    ) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("Only image uploads are allowed");
        }
        return galleryService.uploadAndCreate(file.getOriginalFilename(), file.getContentType(), file.getBytes(), altText);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete image (admin)",
            description = "Deletes the image from Supabase Storage (best-effort) and deletes the DB row.")
    public Mono<Void> delete(@PathVariable String id) {
        return galleryService.deleteById(id);
    }
}
