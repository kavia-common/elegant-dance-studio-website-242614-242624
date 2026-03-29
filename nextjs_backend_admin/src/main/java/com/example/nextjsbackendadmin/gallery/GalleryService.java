package com.example.nextjsbackendadmin.gallery;

import com.example.nextjsbackendadmin.config.AppProperties;
import com.example.nextjsbackendadmin.supabase.SupabaseClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Gallery domain service backed by Supabase (PostgREST + Storage).
 */
@Service
public class GalleryService {

    private final AppProperties props;
    private final SupabaseClient supabase;
    private final ObjectMapper mapper = new ObjectMapper();

    public GalleryService(AppProperties props, SupabaseClient supabase) {
        this.props = props;
        this.supabase = supabase;
    }

    public Mono<List<GalleryImage>> listImages(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        String table = props.db().galleryTable();
        // Order newest first
        String query = "?select=id,image_url,alt_text,created_at&order=created_at.desc&limit=" + safeLimit;
        return supabase.getGalleryRows(table, query).map(this::toGalleryImageList);
    }

    public Mono<GalleryImage> uploadAndCreate(String filename, String contentType, byte[] bytes, String altText) {
        if (bytes == null || bytes.length == 0) {
            return Mono.error(new IllegalArgumentException("File is required"));
        }
        if (bytes.length > 10L * 1024 * 1024) {
            return Mono.error(new IllegalArgumentException("File too large (max 10MB)"));
        }

        String ext = guessExtension(filename, contentType);
        String objectPath = "gallery/" + UUID.randomUUID() + (ext == null ? "" : ("." + ext));
        String bucket = props.storageBucket() == null || props.storageBucket().isBlank()
                ? "gallery"
                : props.storageBucket();

        return supabase.uploadToBucket(bucket, objectPath, bytes, contentType, false)
                .flatMap(ignore -> {
                    String publicUrl = supabase.publicObjectUrl(bucket, objectPath);

                    // Insert DB row
                    String table = props.db().galleryTable();
                    JsonNode payload = mapper.createObjectNode()
                            .put("image_url", publicUrl)
                            .put("alt_text", altText);

                    return supabase.insertRow(table, payload.toString())
                            .map(this::firstGalleryImageFromInsert);
                });
    }

    public Mono<Void> deleteById(String id) {
        if (id == null || id.isBlank()) {
            return Mono.error(new IllegalArgumentException("id is required"));
        }

        String table = props.db().galleryTable();
        // 1) Lookup row to find image_url so we can delete storage object
        String query = "?select=id,image_url&limit=1&id=eq." + urlEscape(id);
        return supabase.getGalleryRows(table, query)
                .flatMap(rows -> {
                    if (!rows.isArray() || rows.size() == 0) {
                        // Idempotent delete: nothing to do
                        return Mono.empty();
                    }
                    String imageUrl = rows.get(0).path("image_url").asText(null);
                    String objectPath = extractObjectPathFromPublicUrl(imageUrl);

                    Mono<Void> deleteStorage = Mono.empty();
                    if (objectPath != null) {
                        String bucket = props.storageBucket() == null || props.storageBucket().isBlank()
                                ? "gallery"
                                : props.storageBucket();
                        deleteStorage = supabase.deleteObject(bucket, objectPath);
                    }

                    // 2) Delete DB row
                    Mono<Void> deleteDb = supabase.deleteRows(table, "?id=eq." + urlEscape(id));
                    return deleteStorage.onErrorResume(e -> Mono.empty()).then(deleteDb);
                });
    }

    private List<GalleryImage> toGalleryImageList(JsonNode json) {
        List<GalleryImage> list = new ArrayList<>();
        if (json != null && json.isArray()) {
            for (JsonNode n : json) {
                list.add(new GalleryImage(
                        n.path("id").asText(null),
                        n.path("image_url").asText(null),
                        n.path("alt_text").asText(null),
                        parseOffsetDateTime(n.path("created_at").asText(null))
                ));
            }
        }
        return list;
    }

    private GalleryImage firstGalleryImageFromInsert(JsonNode json) {
        if (json != null && json.isArray() && json.size() > 0) {
            JsonNode n = json.get(0);
            return new GalleryImage(
                    n.path("id").asText(null),
                    n.path("image_url").asText(null),
                    n.path("alt_text").asText(null),
                    parseOffsetDateTime(n.path("created_at").asText(null))
            );
        }
        // If Supabase didn't return representation for some reason, still return a minimal object.
        return new GalleryImage(null, null, null, null);
    }

    private OffsetDateTime parseOffsetDateTime(String s) {
        try {
            return s == null ? null : OffsetDateTime.parse(s);
        } catch (Exception ex) {
            return null;
        }
    }

    private String extractObjectPathFromPublicUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return null;
        // Expected: {SUPABASE_URL}/storage/v1/object/public/{bucket}/{objectPath}
        String marker = "/storage/v1/object/public/";
        int idx = imageUrl.indexOf(marker);
        if (idx < 0) return null;
        String after = imageUrl.substring(idx + marker.length());
        // after = {bucket}/{objectPath}
        int slash = after.indexOf('/');
        if (slash < 0) return null;
        return after.substring(slash + 1);
    }

    private String guessExtension(String filename, String contentType) {
        String name = filename == null ? "" : filename.toLowerCase();
        if (name.endsWith(".png")) return "png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "jpg";
        if (name.endsWith(".webp")) return "webp";

        if (contentType == null) return null;
        return switch (contentType) {
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "image/webp" -> "webp";
            default -> null;
        };
    }

    private String urlEscape(String s) {
        return s.replace(" ", "%20");
    }
}
